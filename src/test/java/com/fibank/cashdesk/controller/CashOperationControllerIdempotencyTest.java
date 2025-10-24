package com.fibank.cashdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.util.TestDataCleanup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for idempotency support in CashOperationController.
 * Tests the complete flow of duplicate request prevention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CashOperationControllerIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${cashdesk.security.test-api-key}")
    private String apiKey;

    @Value("${cashdesk.security.header-name}")
    private String authHeaderName;

    @Autowired
    private TestDataCleanup testDataCleanup;

    @BeforeEach
    void setUp() {
        // Clean up test data before each test to ensure isolation
        testDataCleanup.cleanupTestData();
        // Note: We're not clearing the idempotency cache between tests
        // because each test uses a unique idempotency key
    }

    @AfterEach
    void tearDown() {
        // Clean up test data after each test to prevent interference
        testDataCleanup.cleanupTestData();
    }

    @Test
    @DisplayName("Should return 400 when idempotency key is missing")
    void processOperation_WithoutIdempotencyKey_Returns400() throws Exception {
        // Arrange
        CashOperationRequest request = createDepositRequest();

        // Act & Assert
        mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Should cache response when idempotency key is provided")
    void processOperation_WithIdempotencyKey_CachesResponse() throws Exception {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        CashOperationRequest request = createDepositRequest();

        // Act - First request
        MvcResult result1 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andReturn();

        String firstResponse = result1.getResponse().getContentAsString();

        // Act - Second request with same idempotency key
        MvcResult result2 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andReturn();

        String secondResponse = result2.getResponse().getContentAsString();

        // Assert - responses should be identical
        assertThat(secondResponse).isEqualTo(firstResponse);
    }

    @Test
    @DisplayName("Should return same transaction ID for duplicate requests")
    void processOperation_DuplicateRequest_ReturnsSameTransactionId() throws Exception {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        CashOperationRequest request = createDepositRequest();

        // Act - First request
        MvcResult result1 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String txnId1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("transactionId").asText();

        // Act - Second request with same idempotency key
        MvcResult result2 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String txnId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("transactionId").asText();

        // Assert - transaction IDs should be identical
        assertThat(txnId2).isEqualTo(txnId1);
    }

    @Test
    @DisplayName("Should treat different idempotency keys as separate requests")
    void processOperation_DifferentKeys_ProcessesSeparately() throws Exception {
        // Arrange
        String idempotencyKey1 = UUID.randomUUID().toString();
        String idempotencyKey2 = UUID.randomUUID().toString();
        CashOperationRequest request = createDepositRequest();

        // Act - First request
        MvcResult result1 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String txnId1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("transactionId").asText();

        // Act - Second request with different idempotency key
        MvcResult result2 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String txnId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("transactionId").asText();

        // Assert - transaction IDs should be different
        assertThat(txnId2).isNotEqualTo(txnId1);
    }

    @Test
    @DisplayName("Should reject blank idempotency key")
    void processOperation_BlankIdempotencyKey_ProcessesNormally() throws Exception {
        // Arrange
        CashOperationRequest request = createDepositRequest();

        // Act & Assert - Request with blank idempotency key should be rejected
        mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required for all cash operations"));
    }

    @Test
    @DisplayName("Should work correctly for withdrawal operations with idempotency")
    void processOperation_Withdrawal_WithIdempotency_Success() throws Exception {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        CashOperationRequest depositRequest = createDepositRequest();
        CashOperationRequest withdrawalRequest = createWithdrawalRequest();

        // Act - First deposit to ensure balance
        mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Act - First withdrawal request
        MvcResult result1 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("WITHDRAWAL"))
                .andReturn();

        String txnId1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("transactionId").asText();

        // Act - Second withdrawal request with same idempotency key
        MvcResult result2 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("WITHDRAWAL"))
                .andReturn();

        String txnId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("transactionId").asText();

        // Assert - should return cached response with same transaction ID
        assertThat(txnId2).isEqualTo(txnId1);
    }

    @Test
    @DisplayName("Should handle sequential duplicate requests with same idempotency key")
    void processOperation_SequentialDuplicates_ReturnsCachedResponse() throws Exception {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        CashOperationRequest request = createDepositRequest();
        String requestJson = objectMapper.writeValueAsString(request);

        // Act - Make first request
        MvcResult result1 = mockMvc.perform(post("/api/v1/cash-operation")
                        .header(authHeaderName, apiKey)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        String txnId1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("transactionId").asText();

        // Act - Make multiple subsequent requests with the same idempotency key
        for (int i = 0; i < 5; i++) {
            MvcResult result = mockMvc.perform(post("/api/v1/cash-operation")
                            .header(authHeaderName, apiKey)
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String txnId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("transactionId").asText();

            // Assert - all subsequent requests should return the same transaction ID
            assertThat(txnId).isEqualTo(txnId1);
        }
    }

    // Helper methods

    private CashOperationRequest createDepositRequest() {
        CashOperationRequest request = new CashOperationRequest();
        request.setCashier("MARTINA");
        request.setOperationType("DEPOSIT");
        request.setCurrency("BGN");
        request.setAmount(new BigDecimal("600.00"));
        request.setDenominations(Map.of(10, 10, 50, 10));
        return request;
    }

    private CashOperationRequest createWithdrawalRequest() {
        CashOperationRequest request = new CashOperationRequest();
        request.setCashier("MARTINA");
        request.setOperationType("WITHDRAWAL");
        request.setCurrency("BGN");
        request.setAmount(new BigDecimal("100.00"));
        request.setDenominations(Map.of(10, 5, 50, 1));
        return request;
    }
}
