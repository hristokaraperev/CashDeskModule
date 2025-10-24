package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.response.CashOperationResponse;

import java.util.Optional;

/**
 * Service interface for managing idempotency of cash operations.
 * Prevents duplicate processing of requests by caching responses based on idempotency keys.
 */
public interface IdempotencyService {

    /**
     * Check if an idempotency key has been previously processed.
     *
     * @param idempotencyKey The unique idempotency key
     * @return Optional containing the cached response if found, empty otherwise
     */
    Optional<CashOperationResponse> getCachedResponse(String idempotencyKey);

    /**
     * Store a response with its idempotency key for future duplicate detection.
     *
     * @param idempotencyKey The unique idempotency key
     * @param response The response to cache
     */
    void cacheResponse(String idempotencyKey, CashOperationResponse response);

    /**
     * Check if an idempotency key exists in the cache.
     *
     * @param idempotencyKey The unique idempotency key
     * @return true if the key exists, false otherwise
     */
    boolean contains(String idempotencyKey);

    /**
     * Remove a cached response (for testing or manual cleanup).
     *
     * @param idempotencyKey The unique idempotency key
     */
    void evict(String idempotencyKey);

    /**
     * Clear all cached responses (for testing or manual cleanup).
     */
    void clear();

    /**
     * Get the current size of the cache.
     *
     * @return Number of cached responses
     */
    int size();
}
