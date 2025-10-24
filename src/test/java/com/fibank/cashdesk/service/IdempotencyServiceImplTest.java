package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.response.CashOperationResponse;
import com.fibank.cashdesk.service.impl.IdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IdempotencyServiceImpl.
 * Tests caching, expiration, and concurrency behavior.
 */
class IdempotencyServiceImplTest {

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        // Use a short TTL for testing (1 hour)
        idempotencyService = new IdempotencyServiceImpl(1);
    }

    @Test
    @DisplayName("Should return empty Optional when idempotency key is null")
    void getCachedResponse_NullKey_ReturnsEmpty() {
        // Act
        Optional<CashOperationResponse> result = idempotencyService.getCachedResponse(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional when idempotency key is blank")
    void getCachedResponse_BlankKey_ReturnsEmpty() {
        // Act
        Optional<CashOperationResponse> result = idempotencyService.getCachedResponse("   ");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional when key does not exist in cache")
    void getCachedResponse_NonExistentKey_ReturnsEmpty() {
        // Act
        Optional<CashOperationResponse> result = idempotencyService.getCachedResponse("non-existent-key");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should cache and retrieve response successfully")
    void cacheAndRetrieveResponse_Success() {
        // Arrange
        String idempotencyKey = "test-key-123";
        CashOperationResponse response = createTestResponse();

        // Act
        idempotencyService.cacheResponse(idempotencyKey, response);
        Optional<CashOperationResponse> retrievedResponse = idempotencyService.getCachedResponse(idempotencyKey);

        // Assert
        assertThat(retrievedResponse).isPresent();
        assertThat(retrievedResponse.get().getTransactionId()).isEqualTo(response.getTransactionId());
        assertThat(retrievedResponse.get().getCashier()).isEqualTo(response.getCashier());
        assertThat(retrievedResponse.get().getAmount()).isEqualByComparingTo(response.getAmount());
    }

    @Test
    @DisplayName("Should not cache response with null idempotency key")
    void cacheResponse_NullKey_DoesNotCache() {
        // Arrange
        CashOperationResponse response = createTestResponse();

        // Act
        idempotencyService.cacheResponse(null, response);

        // Assert - cache should remain empty
        assertThat(idempotencyService.size()).isZero();
    }

    @Test
    @DisplayName("Should not cache response with blank idempotency key")
    void cacheResponse_BlankKey_DoesNotCache() {
        // Arrange
        CashOperationResponse response = createTestResponse();

        // Act
        idempotencyService.cacheResponse("   ", response);

        // Assert - cache should remain empty
        assertThat(idempotencyService.size()).isZero();
    }

    @Test
    @DisplayName("Should not cache null response")
    void cacheResponse_NullResponse_DoesNotCache() {
        // Arrange
        String idempotencyKey = "test-key-123";

        // Act
        idempotencyService.cacheResponse(idempotencyKey, null);

        // Assert - cache should remain empty
        assertThat(idempotencyService.size()).isZero();
    }

    @Test
    @DisplayName("Should overwrite existing cached response with same key")
    void cacheResponse_DuplicateKey_Overwrites() {
        // Arrange
        String idempotencyKey = "test-key-123";
        CashOperationResponse response1 = createTestResponse("txn-001");
        CashOperationResponse response2 = createTestResponse("txn-002");

        // Act
        idempotencyService.cacheResponse(idempotencyKey, response1);
        idempotencyService.cacheResponse(idempotencyKey, response2);

        // Assert - should have the second response
        Optional<CashOperationResponse> retrievedResponse = idempotencyService.getCachedResponse(idempotencyKey);
        assertThat(retrievedResponse).isPresent();
        assertThat(retrievedResponse.get().getTransactionId()).isEqualTo("txn-002");
        assertThat(idempotencyService.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return true when key exists in cache")
    void contains_ExistingKey_ReturnsTrue() {
        // Arrange
        String idempotencyKey = "test-key-123";
        CashOperationResponse response = createTestResponse();
        idempotencyService.cacheResponse(idempotencyKey, response);

        // Act & Assert
        assertThat(idempotencyService.contains(idempotencyKey)).isTrue();
    }

    @Test
    @DisplayName("Should return false when key does not exist in cache")
    void contains_NonExistentKey_ReturnsFalse() {
        // Act & Assert
        assertThat(idempotencyService.contains("non-existent-key")).isFalse();
    }

    @Test
    @DisplayName("Should return false when key is null")
    void contains_NullKey_ReturnsFalse() {
        // Act & Assert
        assertThat(idempotencyService.contains(null)).isFalse();
    }

    @Test
    @DisplayName("Should return false when key is blank")
    void contains_BlankKey_ReturnsFalse() {
        // Act & Assert
        assertThat(idempotencyService.contains("   ")).isFalse();
    }

    @Test
    @DisplayName("Should evict cached response successfully")
    void evict_ExistingKey_RemovesFromCache() {
        // Arrange
        String idempotencyKey = "test-key-123";
        CashOperationResponse response = createTestResponse();
        idempotencyService.cacheResponse(idempotencyKey, response);

        // Act
        idempotencyService.evict(idempotencyKey);

        // Assert
        assertThat(idempotencyService.contains(idempotencyKey)).isFalse();
        assertThat(idempotencyService.size()).isZero();
    }

    @Test
    @DisplayName("Should handle eviction of non-existent key gracefully")
    void evict_NonExistentKey_NoError() {
        // Act & Assert - should not throw exception
        idempotencyService.evict("non-existent-key");
        assertThat(idempotencyService.size()).isZero();
    }

    @Test
    @DisplayName("Should handle eviction with null key gracefully")
    void evict_NullKey_NoError() {
        // Act & Assert - should not throw exception
        idempotencyService.evict(null);
    }

    @Test
    @DisplayName("Should clear all cached responses")
    void clear_RemovesAllEntries() {
        // Arrange
        idempotencyService.cacheResponse("key-1", createTestResponse("txn-001"));
        idempotencyService.cacheResponse("key-2", createTestResponse("txn-002"));
        idempotencyService.cacheResponse("key-3", createTestResponse("txn-003"));

        // Act
        idempotencyService.clear();

        // Assert
        assertThat(idempotencyService.size()).isZero();
        assertThat(idempotencyService.contains("key-1")).isFalse();
        assertThat(idempotencyService.contains("key-2")).isFalse();
        assertThat(idempotencyService.contains("key-3")).isFalse();
    }

    @Test
    @DisplayName("Should return correct cache size")
    void size_ReturnsCorrectCount() {
        // Arrange
        assertThat(idempotencyService.size()).isZero();

        // Act - add entries
        idempotencyService.cacheResponse("key-1", createTestResponse("txn-001"));
        assertThat(idempotencyService.size()).isEqualTo(1);

        idempotencyService.cacheResponse("key-2", createTestResponse("txn-002"));
        assertThat(idempotencyService.size()).isEqualTo(2);

        // Act - evict entry
        idempotencyService.evict("key-1");
        assertThat(idempotencyService.size()).isEqualTo(1);

        // Act - clear all
        idempotencyService.clear();
        assertThat(idempotencyService.size()).isZero();
    }

    @Test
    @DisplayName("Should handle multiple different keys independently")
    void cacheMultipleKeys_IndependentStorage() {
        // Arrange
        CashOperationResponse response1 = createTestResponse("txn-001");
        CashOperationResponse response2 = createTestResponse("txn-002");
        CashOperationResponse response3 = createTestResponse("txn-003");

        // Act
        idempotencyService.cacheResponse("key-1", response1);
        idempotencyService.cacheResponse("key-2", response2);
        idempotencyService.cacheResponse("key-3", response3);

        // Assert
        assertThat(idempotencyService.size()).isEqualTo(3);

        Optional<CashOperationResponse> retrieved1 = idempotencyService.getCachedResponse("key-1");
        Optional<CashOperationResponse> retrieved2 = idempotencyService.getCachedResponse("key-2");
        Optional<CashOperationResponse> retrieved3 = idempotencyService.getCachedResponse("key-3");

        assertThat(retrieved1).isPresent();
        assertThat(retrieved1.get().getTransactionId()).isEqualTo("txn-001");

        assertThat(retrieved2).isPresent();
        assertThat(retrieved2.get().getTransactionId()).isEqualTo("txn-002");

        assertThat(retrieved3).isPresent();
        assertThat(retrieved3.get().getTransactionId()).isEqualTo("txn-003");
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent cache operations")
    void concurrentCaching_ThreadSafe() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // Act - create multiple threads that cache responses
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String key = "key-" + index;
                CashOperationResponse response = createTestResponse("txn-" + index);
                idempotencyService.cacheResponse(key, response);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert - all entries should be cached
        assertThat(idempotencyService.size()).isEqualTo(threadCount);
        for (int i = 0; i < threadCount; i++) {
            assertThat(idempotencyService.contains("key-" + i)).isTrue();
        }
    }

    // Helper methods

    private CashOperationResponse createTestResponse() {
        return createTestResponse("test-txn-123");
    }

    private CashOperationResponse createTestResponse(String transactionId) {
        return new CashOperationResponse(
                transactionId,
                Instant.now(),
                "MARTINA",
                "DEPOSIT",
                "BGN",
                new BigDecimal("600.00"),
                Map.of(10, 10, 50, 10),
                "Deposit successful"
        );
    }
}
