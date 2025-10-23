package com.fibank.cashdesk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.dto.response.BalanceQueryResponse;
import com.fibank.cashdesk.dto.response.CashOperationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Cash Desk Module.
 * Tests end-to-end scenarios from CLAUDE.md requirements.
 * Uses test profile to avoid interfering with production data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Cash Desk Integration Tests")
@org.springframework.test.context.ActiveProfiles("test")
class CashDeskIntegrationTest {

    private static final String API_KEY = "f9Uie8nNf112hx8s";
    private static final String HEADER_NAME = "FIB-X-AUTH";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test deposit and withdrawal operations.
     */
    @Test
    @DisplayName("Should execute deposit and withdrawal operations successfully")
    void shouldExecuteDepositAndWithdrawalOperations() throws Exception {
        String cashier = "PETER"; // Use PETER to avoid state conflicts

        // Step 1: Deposit 600 BGN
        Map<Integer, Integer> deposit600Bgn = new HashMap<>();
        deposit600Bgn.put(10, 10);
        deposit600Bgn.put(50, 10);

        CashOperationRequest deposit1 = new CashOperationRequest(
            "DEPOSIT", cashier, "BGN", new BigDecimal("600.00"), deposit600Bgn
        );

        mockMvc.perform(post("/api/v1/cash-operation")
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deposit1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operationType").value("DEPOSIT"))
            .andExpect(jsonPath("$.amount").value(600.00));

        Map<Integer, Integer> deposit200Eur = new HashMap<>();
        deposit200Eur.put(20, 5);
        deposit200Eur.put(50, 2);

        CashOperationRequest deposit2 = new CashOperationRequest(
            "DEPOSIT", cashier, "EUR", new BigDecimal("200.00"), deposit200Eur
        );

        mockMvc.perform(post("/api/v1/cash-operation")
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deposit2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operationType").value("DEPOSIT"))
            .andExpect(jsonPath("$.amount").value(200.00));

        // Step 3: Perform a withdrawal
        Map<Integer, Integer> withdraw100Bgn = new HashMap<>();
        withdraw100Bgn.put(10, 10); // Withdraw 100 BGN

        CashOperationRequest withdrawal = new CashOperationRequest(
            "WITHDRAWAL", cashier, "BGN", new BigDecimal("100.00"), withdraw100Bgn
        );

        mockMvc.perform(post("/api/v1/cash-operation")
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operationType").value("WITHDRAWAL"))
            .andExpect(jsonPath("$.amount").value(100.00));

        // Verify balance query works
        mockMvc.perform(get("/api/v1/cash-balance")
                .header(HEADER_NAME, API_KEY)
                .param("cashier", cashier))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers.length()").value(1))
            .andExpect(jsonPath("$.cashiers[0].cashier").value(cashier));
    }

    @Test
    @DisplayName("Should reject unauthorized requests")
    void shouldRejectUnauthorizedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/cash-balance"))
            .andExpect(status().isUnauthorized());

        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 10);
        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT", "MARTINA", "BGN", new BigDecimal("100.00"), denominations
        );

        mockMvc.perform(post("/api/v1/cash-operation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle multiple cashiers independently")
    void shouldHandleMultipleCashiersIndependently() throws Exception {
        // Deposit for PETER
        Map<Integer, Integer> peterDeposit = new HashMap<>();
        peterDeposit.put(10, 10);
        CashOperationRequest request1 = new CashOperationRequest(
            "DEPOSIT", "PETER", "BGN", new BigDecimal("100.00"), peterDeposit
        );

        mockMvc.perform(post("/api/v1/cash-operation")
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isOk());

        // Deposit for LINDA
        Map<Integer, Integer> lindaDeposit = new HashMap<>();
        lindaDeposit.put(50, 2);
        CashOperationRequest request2 = new CashOperationRequest(
            "DEPOSIT", "LINDA", "BGN", new BigDecimal("100.00"), lindaDeposit
        );

        mockMvc.perform(post("/api/v1/cash-operation")
                .header(HEADER_NAME, API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isOk());

        // Query all balances
        mockMvc.perform(get("/api/v1/cash-balance")
                .header(HEADER_NAME, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers.length()").value(3))
            .andExpect(jsonPath("$.cashiers[*].cashier").value(org.hamcrest.Matchers.hasItems("MARTINA", "PETER", "LINDA")));
    }
}
