package com.fibank.cashdesk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.dto.response.BalanceQueryResponse;
import com.fibank.cashdesk.util.TestDataCleanup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CashDeskIntegrationTest {

    private static final String API_KEY = "f9Uie8nNf112hx8s";
    private static final String HEADER_NAME = "FIB-X-AUTH";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataCleanup testDataCleanup;

    @BeforeEach
    void setUp() {
        // Clean up test data before each test to ensure isolation
        testDataCleanup.cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        // Clean up test data after each test to prevent interference
        testDataCleanup.cleanupTestData();
    }

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
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
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
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
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
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
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
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
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
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
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
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
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

    @Test
    @DisplayName("Should return period summary with date range parameters")
    void shouldReturnPeriodSummaryWithDateRange() throws Exception {
        String cashier = "MARTINA";

        // Record start time
        java.time.Instant startTime = java.time.Instant.now().minusSeconds(1);

        // Perform multiple transactions
        // Transaction 1: Deposit 100 BGN
        Map<Integer, Integer> deposit1 = new HashMap<>();
        deposit1.put(10, 10);
        CashOperationRequest req1 = new CashOperationRequest(
            "DEPOSIT", cashier, "BGN", new BigDecimal("100.00"), deposit1
        );
        mockMvc.perform(post("/api/v1/cash-operation")
                .header(HEADER_NAME, API_KEY)
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
            .andExpect(status().isOk());

        // Transaction 2: Withdraw 50 BGN
        Map<Integer, Integer> withdrawal1 = new HashMap<>();
        withdrawal1.put(50, 1);
        CashOperationRequest req2 = new CashOperationRequest(
            "WITHDRAWAL", cashier, "BGN", new BigDecimal("50.00"), withdrawal1
        );
        mockMvc.perform(post("/api/v1/cash-operation")
                .header(HEADER_NAME, API_KEY)
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
            .andExpect(status().isOk());

        // Transaction 3: Deposit 200 EUR
        Map<Integer, Integer> deposit2 = new HashMap<>();
        deposit2.put(50, 4);  // 4x50 = 200 EUR
        CashOperationRequest req3 = new CashOperationRequest(
            "DEPOSIT", cashier, "EUR", new BigDecimal("200.00"), deposit2
        );
        mockMvc.perform(post("/api/v1/cash-operation")
                .header(HEADER_NAME, API_KEY)
                .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req3)))
            .andExpect(status().isOk());

        java.time.Instant endTime = java.time.Instant.now().plusSeconds(1);

        // Query with date range - should return period summary
        MvcResult result = mockMvc.perform(get("/api/v1/cash-balance")
                .header(HEADER_NAME, API_KEY)
                .param("cashier", cashier)
                .param("dateFrom", startTime.toString())
                .param("dateTo", endTime.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers.length()").value(1))
            .andExpect(jsonPath("$.cashiers[0].cashier").value(cashier))
            .andExpect(jsonPath("$.cashiers[0].periodSummaries").exists())
            .andExpect(jsonPath("$.cashiers[0].periodSummaries.length()").value(2))
            .andReturn();

        // Parse response
        String responseBody = result.getResponse().getContentAsString();
        BalanceQueryResponse response = objectMapper.readValue(responseBody, BalanceQueryResponse.class);

        // Verify BGN period summary
        var bgnSummary = response.getCashiers().get(0).getPeriodSummaries().stream()
            .filter(s -> s.getCurrency().equals("BGN"))
            .findFirst()
            .orElseThrow();

        // Starting balance: 1000 BGN (50x10 + 10x50)
        assertThat(bgnSummary.getStartingTotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
        // Net change: +100 - 50 = +50
        assertThat(bgnSummary.getNetChange()).isEqualByComparingTo(new BigDecimal("50.00"));
        // Ending balance: 1000 + 50 = 1050
        assertThat(bgnSummary.getEndingTotal()).isEqualByComparingTo(new BigDecimal("1050.00"));
        // Should have 2 transactions
        assertThat(bgnSummary.getTransactions()).hasSize(2);

        // Verify EUR period summary
        var eurSummary = response.getCashiers().get(0).getPeriodSummaries().stream()
            .filter(s -> s.getCurrency().equals("EUR"))
            .findFirst()
            .orElseThrow();

        // Starting balance: 2000 EUR (100x10 + 20x50)
        assertThat(eurSummary.getStartingTotal()).isEqualByComparingTo(new BigDecimal("2000.00"));
        // Net change: +200
        assertThat(eurSummary.getNetChange()).isEqualByComparingTo(new BigDecimal("200.00"));
        // Ending balance: 2000 + 200 = 2200
        assertThat(eurSummary.getEndingTotal()).isEqualByComparingTo(new BigDecimal("2200.00"));
        // Should have 1 transaction
        assertThat(eurSummary.getTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("Should return current balance when no date range specified")
    void shouldReturnCurrentBalanceWithoutDateRange() throws Exception {
        // Query without date range - should return current balance
        MvcResult result = mockMvc.perform(get("/api/v1/cash-balance")
                .header(HEADER_NAME, API_KEY)
                .param("cashier", "MARTINA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers.length()").value(1))
            .andExpect(jsonPath("$.cashiers[0].cashier").value("MARTINA"))
            .andExpect(jsonPath("$.cashiers[0].balances").exists())
            .andExpect(jsonPath("$.cashiers[0].periodSummaries").doesNotExist())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BalanceQueryResponse response = objectMapper.readValue(responseBody, BalanceQueryResponse.class);

        // Should have balances for both currencies
        assertThat(response.getCashiers().get(0).getBalances()).hasSize(2);
    }
}
