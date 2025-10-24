package com.fibank.cashdesk.service.impl;

import com.fibank.cashdesk.dto.response.CashOperationResponse;
import com.fibank.cashdesk.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of IdempotencyService using an in-memory cache with TTL (Time-To-Live).
 * Uses ConcurrentHashMap for thread-safe caching of responses.
 */
@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private final Duration ttl;

    /**
     * Cached response wrapper with expiration timestamp.
     */
    private static class CachedResponse {
        private final CashOperationResponse response;
        private final Instant expiresAt;

        public CachedResponse(CashOperationResponse response, Instant expiresAt) {
            this.response = response;
            this.expiresAt = expiresAt;
        }

        public CashOperationResponse getResponse() {
            return response;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public IdempotencyServiceImpl(
            @Value("${cashdesk.idempotency.ttl-hours:24}") int ttlHours
    ) {
        this.ttl = Duration.ofHours(ttlHours);
        log.info("IdempotencyService initialized with TTL: {} hours", ttlHours);
    }

    @Override
    public Optional<CashOperationResponse> getCachedResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        CachedResponse cached = cache.get(idempotencyKey);

        if (cached == null) {
            return Optional.empty();
        }

        if (cached.isExpired()) {
            log.debug("Cached response for key {} has expired, removing from cache", idempotencyKey);
            cache.remove(idempotencyKey);
            return Optional.empty();
        }

        log.info("Found cached response for idempotency key: {}", idempotencyKey);
        return Optional.of(cached.getResponse());
    }

    @Override
    public void cacheResponse(String idempotencyKey, CashOperationResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("Cannot cache response with null or blank idempotency key");
            return;
        }

        if (response == null) {
            log.warn("Cannot cache null response for idempotency key: {}", idempotencyKey);
            return;
        }

        Instant expiresAt = Instant.now().plus(ttl);
        cache.put(idempotencyKey, new CachedResponse(response, expiresAt));

        log.info("Cached response for idempotency key: {} (expires at: {})",
                idempotencyKey, expiresAt);

        // Cleanup expired entries opportunistically
        cleanupExpired();
    }

    @Override
    public boolean contains(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }

        CachedResponse cached = cache.get(idempotencyKey);

        if (cached == null) {
            return false;
        }

        if (cached.isExpired()) {
            cache.remove(idempotencyKey);
            return false;
        }

        return true;
    }

    @Override
    public void evict(String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            cache.remove(idempotencyKey);
            log.debug("Evicted cached response for idempotency key: {}", idempotencyKey);
        }
    }

    @Override
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("Cleared idempotency cache ({} entries removed)", size);
    }

    @Override
    public int size() {
        return cache.size();
    }

    /**
     * Remove expired entries from the cache.
     * Called opportunistically during cache operations.
     */
    private void cleanupExpired() {
        // Only perform cleanup if cache is getting large
        if (cache.size() < 100) {
            return;
        }

        int removedCount = 0;
        for (Map.Entry<String, CachedResponse> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.debug("Cleaned up {} expired entries from idempotency cache", removedCount);
        }
    }
}
