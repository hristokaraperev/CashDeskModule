package com.fibank.cashdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.dto.response.CashOperationResponse;
import com.fibank.cashdesk.exception.InsufficientFundsException;
import com.fibank.cashdesk.exception.InvalidDenominationException;
import com.fibank.cashdesk.service.CashOperationService;
import com.fibank.cashdesk.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CashOperationController.
 * Tests API layer with MockMvc, verifying request validation and response handling.
 */
@WebMvcTest(CashOperationController.class)
@DisplayName("CashOperationController Tests")
class CashOperationControllerTest {

    private static final String API_KEY = "f9Uie8nNf112hx8s";
    private static final String HEADER_NAME = "FIB-X-AUTH";
    private static final String ENDPOINT = "/api/v1/cash-operation";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CashOperationService cashOperationService;

    @MockitoBean
    private IdempotencyService idempotencyService;

    private CashOperationRequest validDepositRequest;
    private CashOperationRequest validWithdrawalRequest;
    private CashOperationResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Valid deposit request
        Map<Integer, Integer> depositDenominations = new HashMap<>();
        depositDenominations.put(10, 10);
        depositDenominations.put(50, 10);
        validDepositRequest = new CashOperationRequest(
            "DEPOSIT",
            "MARTINA",
            "BGN",
            new BigDecimal("600.00"),
            depositDenominations
        );

        // Valid withdrawal request
        Map<Integer, Integer> withdrawalDenominations = new HashMap<>();
        withdrawalDenominations.put(10, 5);
        withdrawalDenominations.put(50, 1);
        validWithdrawalRequest = new CashOperationRequest(
            "WITHDRAWAL",
            "PETER",
            "BGN",
            new BigDecimal("100.00"),
            withdrawalDenominations
        );

        // Mock response
        Map<Integer, Integer> responseDenominations = new HashMap<>();
        responseDenominations.put(10, 10);
        responseDenominations.put(50, 10);
        mockResponse = new CashOperationResponse(
            "TXN-001",
            Instant.now(),
            "MARTINA",
            "DEPOSIT",
            "BGN",
            new BigDecimal("600.00"),
            responseDenominations,
            "Operation successful"
        );
    }

    // ===================== Authentication Tests =====================

    @Test
    @DisplayName("Should return 401 when authentication header is missing")
    void shouldReturn401WhenAuthHeaderMissing() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when authentication header is invalid")
    void shouldReturn401WhenAuthHeaderInvalid() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, "invalid-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 with valid authentication header")
    void shouldReturn200WithValidAuthHeader() throws Exception {
        when(cashOperationService.processOperation(any())).thenReturn(mockResponse);

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
            .andExpect(status().isOk());
    }

    // ===================== Valid Request Tests =====================

    @Test
    @DisplayName("Should process valid deposit request successfully")
    void shouldProcessValidDepositRequest() throws Exception {
        when(cashOperationService.processOperation(any())).thenReturn(mockResponse);

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Operation successful"))
            .andExpect(jsonPath("$.operationType").value("DEPOSIT"))
            .andExpect(jsonPath("$.cashier").value("MARTINA"))
            .andExpect(jsonPath("$.currency").value("BGN"))
            .andExpect(jsonPath("$.amount").value(600.00));
    }

    @Test
    @DisplayName("Should process valid withdrawal request successfully")
    void shouldProcessValidWithdrawalRequest() throws Exception {
        Map<Integer, Integer> withdrawalDenoms = new HashMap<>();
        withdrawalDenoms.put(10, 5);
        withdrawalDenoms.put(50, 1);
        CashOperationResponse withdrawalResponse = new CashOperationResponse(
            "TXN-002",
            Instant.now(),
            "PETER",
            "WITHDRAWAL",
            "BGN",
            new BigDecimal("100.00"),
            withdrawalDenoms,
            "Operation successful"
        );
        when(cashOperationService.processOperation(any())).thenReturn(withdrawalResponse);

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWithdrawalRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operationType").value("WITHDRAWAL"))
            .andExpect(jsonPath("$.cashier").value("PETER"))
            .andExpect(jsonPath("$.amount").value(100.00));
    }

    // ===================== Validation Tests - Missing Fields =====================

    @Test
    @DisplayName("Should return 400 when operation type is missing")
    void shouldReturn400WhenOperationTypeMissing() throws Exception {
        CashOperationRequest request = new CashOperationRequest(
            null, "MARTINA", "BGN", new BigDecimal("100.00"), new HashMap<>()
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when cashier is missing")
    void shouldReturn400WhenCashierMissing() throws Exception {
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", null, "BGN", new BigDecimal("100.00"), new HashMap<>()
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when currency is missing")
    void shouldReturn400WhenCurrencyMissing() throws Exception {
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", "MARTINA", null, new BigDecimal("100.00"), new HashMap<>()
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when amount is missing")
    void shouldReturn400WhenAmountMissing() throws Exception {
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", "MARTINA", "BGN", null, new HashMap<>()
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when denominations are missing")
    void shouldReturn400WhenDenominationsMissing() throws Exception {
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", "MARTINA", "BGN", new BigDecimal("100.00"), null
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ===================== Validation Tests - Invalid Values =====================

    @Test
    @DisplayName("Should return 400 when operation type is invalid")
    void shouldReturn400WhenOperationTypeInvalid() throws Exception {
        CashOperationRequest request = new CashOperationRequest(
            "INVALID_TYPE", "MARTINA", "BGN", new BigDecimal("100.00"), new HashMap<>()
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when cashier name is invalid")
    void shouldReturn400WhenCashierNameInvalid() throws Exception {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 10);
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", "JOHN", "BGN", new BigDecimal("100.00"), denominations
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when currency is invalid")
    void shouldReturn400WhenCurrencyInvalid() throws Exception {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 10);
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", "MARTINA", "USD", new BigDecimal("100.00"), denominations
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when amount is zero or negative")
    void shouldReturn400WhenAmountIsZeroOrNegative() throws Exception {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 0);
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", "MARTINA", "BGN", BigDecimal.ZERO, denominations
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when denominations are empty")
    void shouldReturn400WhenDenominationsEmpty() throws Exception {
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", "MARTINA", "BGN", new BigDecimal("100.00"), new HashMap<>()
        );

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ===================== Business Logic Error Tests =====================

    @Test
    @DisplayName("Should return 400 when insufficient funds for withdrawal")
    void shouldReturn400WhenInsufficientFunds() throws Exception {
        when(cashOperationService.processOperation(any()))
            .thenThrow(new InsufficientFundsException("Insufficient funds for withdrawal"));

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWithdrawalRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Should return 400 when denominations sum mismatch")
    void shouldReturn400WhenDenominationsSumMismatch() throws Exception {
        when(cashOperationService.processOperation(any()))
            .thenThrow(new InvalidDenominationException("Denominations sum does not match amount"));

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    // ===================== EUR Currency Tests =====================

    @Test
    @DisplayName("Should process EUR deposit successfully")
    void shouldProcessEurDepositSuccessfully() throws Exception {
        Map<Integer, Integer> eurDenominations = new HashMap<>();
        eurDenominations.put(20, 5);
        eurDenominations.put(50, 2);
        CashOperationRequest eurRequest = new CashOperationRequest(
            "DEPOSIT",
            "LINDA",
            "EUR",
            new BigDecimal("200.00"),
            eurDenominations
        );

        CashOperationResponse eurResponse = new CashOperationResponse(
            "TXN-003",
            Instant.now(),
            "LINDA",
            "DEPOSIT",
            "EUR",
            new BigDecimal("200.00"),
            eurDenominations,
            "Operation successful"
        );
        when(cashOperationService.processOperation(any())).thenReturn(eurResponse);

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eurRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.amount").value(200.00));
    }

    @Test
    @DisplayName("Should process EUR withdrawal successfully")
    void shouldProcessEurWithdrawalSuccessfully() throws Exception {
        Map<Integer, Integer> eurDenominations = new HashMap<>();
        eurDenominations.put(50, 10);
        CashOperationRequest eurRequest = new CashOperationRequest(
            "WITHDRAWAL",
            "LINDA",
            "EUR",
            new BigDecimal("500.00"),
            eurDenominations
        );

        CashOperationResponse eurResponse = new CashOperationResponse(
            "TXN-004",
            Instant.now(),
            "LINDA",
            "WITHDRAWAL",
            "EUR",
            new BigDecimal("500.00"),
            eurDenominations,
            "Operation successful"
        );
        when(cashOperationService.processOperation(any())).thenReturn(eurResponse);

        mockMvc.perform(post(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eurRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operationType").value("WITHDRAWAL"))
            .andExpect(jsonPath("$.currency").value("EUR"));
    }
}
