package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.dto.response.BalanceQueryResponse;
import com.fibank.cashdesk.dto.response.CashierBalanceDTO;
import com.fibank.cashdesk.dto.response.CurrencyBalanceDTO;
import com.fibank.cashdesk.exception.InvalidCashierException;
import com.fibank.cashdesk.exception.InvalidDateRangeException;
import com.fibank.cashdesk.service.BalanceQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for BalanceController.
 * Tests balance query API with various filter combinations.
 */
@WebMvcTest(BalanceController.class)
@DisplayName("BalanceController Tests")
class BalanceControllerTest {

    private static final String API_KEY = "f9Uie8nNf112hx8s";
    private static final String HEADER_NAME = "FIB-X-AUTH";
    private static final String ENDPOINT = "/api/v1/cash-balance";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BalanceQueryService balanceQueryService;

    private BalanceQueryResponse mockBalanceResponse;

    @BeforeEach
    void setUp() {
        // Create mock balance response with all three cashiers
        List<CashierBalanceDTO> cashiers = new ArrayList<>();

        // MARTINA balances
        Map<Integer, Integer> martinaBgnDenoms = new HashMap<>();
        martinaBgnDenoms.put(10, 50);
        martinaBgnDenoms.put(50, 10);

        Map<Integer, Integer> martinaEurDenoms = new HashMap<>();
        martinaEurDenoms.put(10, 100);
        martinaEurDenoms.put(50, 20);

        List<CurrencyBalanceDTO> martinaBalances = new ArrayList<>();
        martinaBalances.add(new CurrencyBalanceDTO("BGN", new BigDecimal("1000.00"), martinaBgnDenoms));
        martinaBalances.add(new CurrencyBalanceDTO("EUR", new BigDecimal("2000.00"), martinaEurDenoms));

        cashiers.add(new CashierBalanceDTO("MARTINA", martinaBalances));

        // PETER balances (same as MARTINA initially)
        cashiers.add(new CashierBalanceDTO("PETER", new ArrayList<>(martinaBalances)));

        // LINDA balances (same as MARTINA initially)
        cashiers.add(new CashierBalanceDTO("LINDA", new ArrayList<>(martinaBalances)));

        mockBalanceResponse = new BalanceQueryResponse(cashiers);
    }

    // ===================== Authentication Tests =====================

    @Test
    @DisplayName("Should return 401 when authentication header is missing")
    void shouldReturn401WhenAuthHeaderMissing() throws Exception {
        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when authentication header is invalid")
    void shouldReturn401WhenAuthHeaderInvalid() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, "invalid-api-key"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 with valid authentication header")
    void shouldReturn200WithValidAuthHeader() throws Exception {
        when(balanceQueryService.queryBalance(any(), any(), anyString())).thenReturn(mockBalanceResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY))
            .andExpect(status().isOk());
    }

    // ===================== Query Without Filters Tests =====================

    @Test
    @DisplayName("Should return all cashier balances when no filters provided")
    void shouldReturnAllBalancesWithoutFilters() throws Exception {
        when(balanceQueryService.queryBalance(null, null, null)).thenReturn(mockBalanceResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers").isArray())
            .andExpect(jsonPath("$.cashiers.length()").value(3))
            .andExpect(jsonPath("$.cashiers[0].cashier").value("MARTINA"))
            .andExpect(jsonPath("$.cashiers[1].cashier").value("PETER"))
            .andExpect(jsonPath("$.cashiers[2].cashier").value("LINDA"));
    }

    // ===================== Cashier Filter Tests =====================

    @Test
    @DisplayName("Should return specific cashier balance when cashier filter provided")
    void shouldReturnSpecificCashierBalance() throws Exception {
        // Create response with only MARTINA
        List<CashierBalanceDTO> singleCashier = new ArrayList<>();
        singleCashier.add(mockBalanceResponse.getCashiers().get(0));
        BalanceQueryResponse martinaResponse = new BalanceQueryResponse(singleCashier);

        when(balanceQueryService.queryBalance(null, null, "MARTINA")).thenReturn(martinaResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("cashier", "MARTINA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers.length()").value(1))
            .andExpect(jsonPath("$.cashiers[0].cashier").value("MARTINA"));
    }

    @Test
    @DisplayName("Should return balance for PETER when specified")
    void shouldReturnPeterBalance() throws Exception {
        List<CashierBalanceDTO> singleCashier = new ArrayList<>();
        singleCashier.add(mockBalanceResponse.getCashiers().get(1));
        BalanceQueryResponse peterResponse = new BalanceQueryResponse(singleCashier);

        when(balanceQueryService.queryBalance(null, null, "PETER")).thenReturn(peterResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("cashier", "PETER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers[0].cashier").value("PETER"));
    }

    @Test
    @DisplayName("Should return balance for LINDA when specified")
    void shouldReturnLindaBalance() throws Exception {
        List<CashierBalanceDTO> singleCashier = new ArrayList<>();
        singleCashier.add(mockBalanceResponse.getCashiers().get(2));
        BalanceQueryResponse lindaResponse = new BalanceQueryResponse(singleCashier);

        when(balanceQueryService.queryBalance(null, null, "LINDA")).thenReturn(lindaResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("cashier", "LINDA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers[0].cashier").value("LINDA"));
    }

    @Test
    @DisplayName("Should return error when invalid cashier name provided")
    void shouldReturnErrorWhenInvalidCashierName() throws Exception {
        when(balanceQueryService.queryBalance(any(), any(), anyString()))
            .thenThrow(new InvalidCashierException("Invalid cashier: JOHN"));

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("cashier", "JOHN"))
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.error").exists());
    }

    // ===================== Date Range Filter Tests =====================

    @Test
    @DisplayName("Should accept date range filter with both dateFrom and dateTo")
    void shouldAcceptDateRangeWithBothDates() throws Exception {
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant dateTo = Instant.now();

        when(balanceQueryService.queryBalance(any(), any(), any())).thenReturn(mockBalanceResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("dateFrom", dateFrom.toString())
                .param("dateTo", dateTo.toString()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should accept filter with only dateFrom")
    void shouldAcceptOnlyDateFrom() throws Exception {
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);

        when(balanceQueryService.queryBalance(any(), any(), any())).thenReturn(mockBalanceResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("dateFrom", dateFrom.toString()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should accept filter with only dateTo")
    void shouldAcceptOnlyDateTo() throws Exception {
        Instant dateTo = Instant.now();

        when(balanceQueryService.queryBalance(any(), any(), any())).thenReturn(mockBalanceResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("dateTo", dateTo.toString()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 when dateFrom is after dateTo")
    void shouldReturn400WhenDateFromAfterDateTo() throws Exception {
        Instant dateFrom = Instant.now();
        Instant dateTo = dateFrom.minus(7, ChronoUnit.DAYS);

        when(balanceQueryService.queryBalance(any(), any(), any()))
            .thenThrow(new InvalidDateRangeException("dateFrom must be before or equal to dateTo"));

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("dateFrom", dateFrom.toString())
                .param("dateTo", dateTo.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Should return error when date format is invalid")
    void shouldReturnErrorWhenDateFormatInvalid() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("dateFrom", "invalid-date-format"))
            .andExpect(status().is5xxServerError()); // Date parsing error returns 500
    }

    // ===================== Combined Filters Tests =====================

    @Test
    @DisplayName("Should accept combined cashier and date range filters")
    void shouldAcceptCombinedFilters() throws Exception {
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant dateTo = Instant.now();

        List<CashierBalanceDTO> singleCashier = new ArrayList<>();
        singleCashier.add(mockBalanceResponse.getCashiers().get(0));
        BalanceQueryResponse filteredResponse = new BalanceQueryResponse(singleCashier);

        when(balanceQueryService.queryBalance(any(), any(), anyString())).thenReturn(filteredResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("cashier", "MARTINA")
                .param("dateFrom", dateFrom.toString())
                .param("dateTo", dateTo.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers.length()").value(1))
            .andExpect(jsonPath("$.cashiers[0].cashier").value("MARTINA"));
    }

    // ===================== Response Structure Tests =====================

    @Test
    @DisplayName("Should return correct response structure with denominations")
    void shouldReturnCorrectResponseStructure() throws Exception {
        when(balanceQueryService.queryBalance(null, null, null)).thenReturn(mockBalanceResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers").isArray())
            .andExpect(jsonPath("$.cashiers[0].cashier").exists())
            .andExpect(jsonPath("$.cashiers[0].balances").isArray())
            .andExpect(jsonPath("$.cashiers[0].balances[0].currency").exists())
            .andExpect(jsonPath("$.cashiers[0].balances[0].total").exists())
            .andExpect(jsonPath("$.cashiers[0].balances[0].denominations").exists());
    }

    @Test
    @DisplayName("Should return BGN and EUR balances for each cashier")
    void shouldReturnBothCurrenciesForEachCashier() throws Exception {
        when(balanceQueryService.queryBalance(null, null, null)).thenReturn(mockBalanceResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers[0].balances.length()").value(2))
            .andExpect(jsonPath("$.cashiers[0].balances[0].currency").value("BGN"))
            .andExpect(jsonPath("$.cashiers[0].balances[0].total").value(1000.00))
            .andExpect(jsonPath("$.cashiers[0].balances[1].currency").value("EUR"))
            .andExpect(jsonPath("$.cashiers[0].balances[1].total").value(2000.00));
    }

    @Test
    @DisplayName("Should return correct denomination details")
    void shouldReturnCorrectDenominationDetails() throws Exception {
        when(balanceQueryService.queryBalance(null, null, null)).thenReturn(mockBalanceResponse);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers[0].balances[0].denominations.10").value(50))
            .andExpect(jsonPath("$.cashiers[0].balances[0].denominations.50").value(10))
            .andExpect(jsonPath("$.cashiers[0].balances[1].denominations.10").value(100))
            .andExpect(jsonPath("$.cashiers[0].balances[1].denominations.50").value(20));
    }

    // ===================== Empty Results Tests =====================

    @Test
    @DisplayName("Should return empty cashiers array when no transactions in date range")
    void shouldReturnEmptyResultsWhenNoTransactionsInRange() throws Exception {
        BalanceQueryResponse emptyResponse = new BalanceQueryResponse(new ArrayList<>());

        when(balanceQueryService.queryBalance(any(), any(), any())).thenReturn(emptyResponse);

        Instant dateFrom = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant dateTo = dateFrom.plus(1, ChronoUnit.DAYS);

        mockMvc.perform(get(ENDPOINT)
                .header(HEADER_NAME, API_KEY)
                .param("dateFrom", dateFrom.toString())
                .param("dateTo", dateTo.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashiers").isEmpty());
    }
}
