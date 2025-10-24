package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.response.BalanceQueryResponse;
import com.fibank.cashdesk.dto.response.CashierBalanceDTO;
import com.fibank.cashdesk.exception.InvalidDateRangeException;
import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.OperationType;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.BalanceRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import com.fibank.cashdesk.service.impl.BalanceQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BalanceQueryServiceImpl.
 * Tests balance query functionality with various filters.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceQueryService Tests")
class BalanceQueryServiceImplTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private BalanceQueryService balanceQueryService;

    @BeforeEach
    void setUp() {
        balanceQueryService = new BalanceQueryServiceImpl(balanceRepository, transactionRepository);

        // Set cashier names using reflection (since it's a @Value field)
        ReflectionTestUtils.setField(balanceQueryService, "allCashiers",
            List.of("MARTINA", "PETER", "LINDA"));
    }

    // ===================== Query Without Filters Tests =====================

    @Test
    @DisplayName("Should return all cashier balances when no filters provided")
    void shouldReturnAllBalancesWithoutFilters() {
        // Arrange - Setup balances for all cashiers
        setupMockBalance("MARTINA");
        setupMockBalance("PETER");
        setupMockBalance("LINDA");

        // Act
        BalanceQueryResponse response = balanceQueryService.queryBalance(null, null, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCashiers()).hasSize(3);
        assertThat(response.getCashiers())
            .extracting(CashierBalanceDTO::getCashier)
            .containsExactlyInAnyOrder("MARTINA", "PETER", "LINDA");
    }

    @Test
    @DisplayName("Should return correct denomination details for each currency")
    void shouldReturnCorrectDenominationDetails() {
        // Arrange
        Map<Currency, CashBalance> martinaBalances = new HashMap<>();
        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 50);
        bgnBalance.setDenominationCount(50, 10);
        martinaBalances.put(Currency.BGN, bgnBalance);

        CashBalance eurBalance = new CashBalance(Currency.EUR);
        eurBalance.setDenominationCount(10, 100);
        eurBalance.setDenominationCount(50, 20);
        martinaBalances.put(Currency.EUR, eurBalance);

        when(balanceRepository.findByCashier("MARTINA")).thenReturn(martinaBalances);

        // Act
        BalanceQueryResponse response = balanceQueryService.queryBalance(null, null, "MARTINA");

        // Assert
        assertThat(response.getCashiers()).hasSize(1);
        CashierBalanceDTO cashierBalance = response.getCashiers().get(0);
        assertThat(cashierBalance.getCashier()).isEqualTo("MARTINA");
        assertThat(cashierBalance.getBalances()).hasSize(2);

        // Verify BGN balance
        assertThat(cashierBalance.getBalances())
            .filteredOn(b -> b.getCurrency().equals("BGN"))
            .first()
            .satisfies(bgn -> {
                assertThat(bgn.getTotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
                assertThat(bgn.getDenominations().get(10)).isEqualTo(50);
                assertThat(bgn.getDenominations().get(50)).isEqualTo(10);
            });

        // Verify EUR balance
        assertThat(cashierBalance.getBalances())
            .filteredOn(b -> b.getCurrency().equals("EUR"))
            .first()
            .satisfies(eur -> {
                assertThat(eur.getTotal()).isEqualByComparingTo(new BigDecimal("2000.00"));
                assertThat(eur.getDenominations().get(10)).isEqualTo(100);
                assertThat(eur.getDenominations().get(50)).isEqualTo(20);
            });
    }

    // ===================== Cashier Filter Tests =====================

    @Test
    @DisplayName("Should return balance for specific cashier only")
    void shouldReturnBalanceForSpecificCashier() {
        // Arrange
        setupMockBalance("PETER");

        // Act
        BalanceQueryResponse response = balanceQueryService.queryBalance(null, null, "PETER");

        // Assert
        assertThat(response.getCashiers()).hasSize(1);
        assertThat(response.getCashiers().get(0).getCashier()).isEqualTo("PETER");
    }

    @Test
    @DisplayName("Should handle lowercase cashier name")
    void shouldHandleLowercaseCashierName() {
        // Arrange
        setupMockBalance("LINDA");

        // Act
        BalanceQueryResponse response = balanceQueryService.queryBalance(null, null, "linda");

        // Assert
        assertThat(response.getCashiers()).hasSize(1);
        assertThat(response.getCashiers().get(0).getCashier()).isEqualTo("LINDA");
    }

    // ===================== Date Range Filter Tests =====================

    @Test
    @DisplayName("Should throw exception when dateFrom is after dateTo")
    void shouldThrowExceptionWhenDateFromAfterDateTo() {
        Instant dateFrom = Instant.now();
        Instant dateTo = dateFrom.minus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> balanceQueryService.queryBalance(dateFrom, dateTo, null))
            .isInstanceOf(InvalidDateRangeException.class)
            .hasMessageContaining("dateFrom must be before or equal to dateTo");
    }

    @Test
    @DisplayName("Should accept valid date range with dateFrom before dateTo")
    void shouldAcceptValidDateRange() {
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant dateTo = Instant.now();

        when(transactionRepository.findByCashierAndDateRange(any(), any(), any()))
            .thenReturn(Collections.emptyList());

        BalanceQueryResponse response = balanceQueryService.queryBalance(dateFrom, dateTo, "MARTINA");

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should accept equal dateFrom and dateTo")
    void shouldAcceptEqualDates() {
        Instant date = Instant.now();

        when(transactionRepository.findByCashierAndDateRange(any(), any(), any()))
            .thenReturn(Collections.emptyList());

        BalanceQueryResponse response = balanceQueryService.queryBalance(date, date, "PETER");

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should calculate historical balance from transactions")
    void shouldCalculateHistoricalBalanceFromTransactions() {
        // Arrange
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant dateTo = Instant.now();

        List<Transaction> transactions = new ArrayList<>();

        // Deposit transaction
        Map<Integer, Integer> depositDenoms = new HashMap<>();
        depositDenoms.put(10, 10);
        depositDenoms.put(50, 10);
        Transaction deposit = Transaction.create(
            "MARTINA", OperationType.DEPOSIT, Currency.BGN, new BigDecimal("600.00"), depositDenoms
        );
        transactions.add(deposit);

        // Withdrawal transaction
        Map<Integer, Integer> withdrawalDenoms = new HashMap<>();
        withdrawalDenoms.put(10, 5);
        withdrawalDenoms.put(50, 1);
        Transaction withdrawal = Transaction.create(
            "MARTINA", OperationType.WITHDRAWAL, Currency.BGN, new BigDecimal("100.00"), withdrawalDenoms
        );
        transactions.add(withdrawal);

        when(transactionRepository.findByCashierAndDateRange(eq("MARTINA"), any(), any()))
            .thenReturn(transactions);

        // Act
        BalanceQueryResponse response = balanceQueryService.queryBalance(dateFrom, dateTo, "MARTINA");

        // Assert
        assertThat(response.getCashiers()).hasSize(1);
        CashierBalanceDTO cashierBalance = response.getCashiers().get(0);

        // Should have period summaries with deposited 600 and withdrawn 100 = 500 net
        assertThat(cashierBalance.getPeriodSummaries())
            .filteredOn(s -> s.getCurrency().equals("BGN"))
            .first()
            .satisfies(bgn -> {
                // Starting balance should be 1000 (initial BGN balance)
                assertThat(bgn.getStartingTotal()).isEqualByComparingTo(new BigDecimal("1000"));
                // Net change: +600 - 100 = +500
                assertThat(bgn.getNetChange()).isEqualByComparingTo(new BigDecimal("500.00"));
                // Ending balance: 1000 + 500 = 1500
                assertThat(bgn.getEndingTotal()).isEqualByComparingTo(new BigDecimal("1500.00"));
                // Ending denominations: initial(50x10 + 10x50) + deposited 10x10 + 10x50 - withdrawn 5x10 - 1x50
                // 10: 50 + 10 - 5 = 55
                // 50: 10 + 10 - 1 = 19
                assertThat(bgn.getEndingDenominations().get(10)).isEqualTo(55);
                assertThat(bgn.getEndingDenominations().get(50)).isEqualTo(19);
            });
    }

    @Test
    @DisplayName("Should return zero balance when no transactions in date range")
    void shouldReturnZeroBalanceWhenNoTransactions() {
        Instant dateFrom = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant dateTo = dateFrom.plus(1, ChronoUnit.DAYS);

        // Stub for calculating starting balance (null, null)
        when(transactionRepository.findByCashierAndDateRange(eq("LINDA"), any(), any()))
            .thenReturn(Collections.emptyList());

        BalanceQueryResponse response = balanceQueryService.queryBalance(dateFrom, dateTo, "LINDA");

        assertThat(response.getCashiers()).hasSize(1);
        CashierBalanceDTO cashierBalance = response.getCashiers().get(0);

        // Should have period summaries (not regular balances)
        // No transactions, so starting balance equals ending balance (initial balances)
        assertThat(cashierBalance.getPeriodSummaries()).isNotNull();
        assertThat(cashierBalance.getPeriodSummaries()).hasSize(2); // BGN and EUR

        // Check BGN - initial 1000 BGN
        assertThat(cashierBalance.getPeriodSummaries())
            .filteredOn(s -> s.getCurrency().equals("BGN"))
            .first()
            .satisfies(bgn -> {
                assertThat(bgn.getStartingTotal()).isEqualByComparingTo(new BigDecimal("1000"));
                assertThat(bgn.getEndingTotal()).isEqualByComparingTo(new BigDecimal("1000"));
                assertThat(bgn.getNetChange()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(bgn.getTransactions()).isEmpty();
            });

        // Check EUR - initial 2000 EUR
        assertThat(cashierBalance.getPeriodSummaries())
            .filteredOn(s -> s.getCurrency().equals("EUR"))
            .first()
            .satisfies(eur -> {
                assertThat(eur.getStartingTotal()).isEqualByComparingTo(new BigDecimal("2000"));
                assertThat(eur.getEndingTotal()).isEqualByComparingTo(new BigDecimal("2000"));
                assertThat(eur.getNetChange()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(eur.getTransactions()).isEmpty();
            });
    }

    @Test
    @DisplayName("Should handle only dateFrom parameter")
    void shouldHandleOnlyDateFrom() {
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);

        when(transactionRepository.findByCashierAndDateRange(eq("PETER"), any(), any()))
            .thenReturn(Collections.emptyList());

        BalanceQueryResponse response = balanceQueryService.queryBalance(dateFrom, null, "PETER");

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should handle only dateTo parameter")
    void shouldHandleOnlyDateTo() {
        Instant dateTo = Instant.now();

        when(transactionRepository.findByCashierAndDateRange(eq("MARTINA"), eq(null), eq(dateTo)))
            .thenReturn(Collections.emptyList());

        BalanceQueryResponse response = balanceQueryService.queryBalance(null, dateTo, "MARTINA");

        assertThat(response).isNotNull();
    }

    // ===================== Combined Filters Tests =====================

    @Test
    @DisplayName("Should handle combined cashier and date range filters")
    void shouldHandleCombinedFilters() {
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant dateTo = Instant.now();

        when(transactionRepository.findByCashierAndDateRange(eq("LINDA"), any(), any()))
            .thenReturn(Collections.emptyList());

        BalanceQueryResponse response = balanceQueryService.queryBalance(dateFrom, dateTo, "LINDA");

        assertThat(response.getCashiers()).hasSize(1);
        assertThat(response.getCashiers().get(0).getCashier()).isEqualTo("LINDA");
    }

    // ===================== Multiple Transactions Tests =====================

    @Test
    @DisplayName("Should correctly calculate balance with multiple deposits")
    void shouldCalculateBalanceWithMultipleDeposits() {
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant dateTo = Instant.now();

        List<Transaction> transactions = new ArrayList<>();

        // First deposit
        Map<Integer, Integer> deposit1 = new HashMap<>();
        deposit1.put(10, 10);
        transactions.add(Transaction.create(
            "PETER", OperationType.DEPOSIT, Currency.BGN, new BigDecimal("100.00"), deposit1
        ));

        // Second deposit
        Map<Integer, Integer> deposit2 = new HashMap<>();
        deposit2.put(10, 20);
        transactions.add(Transaction.create(
            "PETER", OperationType.DEPOSIT, Currency.BGN, new BigDecimal("200.00"), deposit2
        ));

        when(transactionRepository.findByCashierAndDateRange(eq("PETER"), any(), any()))
            .thenReturn(transactions);

        BalanceQueryResponse response = balanceQueryService.queryBalance(dateFrom, dateTo, "PETER");

        assertThat(response.getCashiers().get(0).getPeriodSummaries())
            .filteredOn(s -> s.getCurrency().equals("BGN"))
            .first()
            .satisfies(bgn -> {
                // Starting: 1000 BGN (initial), Deposited: 100 + 200 = 300, Ending: 1300
                assertThat(bgn.getStartingTotal()).isEqualByComparingTo(new BigDecimal("1000"));
                assertThat(bgn.getEndingTotal()).isEqualByComparingTo(new BigDecimal("1300.00"));
                // Initial: 50x10, Deposited: 10x10 + 20x10 = 30x10, Total: 80x10
                assertThat(bgn.getEndingDenominations().get(10)).isEqualTo(80);
            });
    }

    @Test
    @DisplayName("Should handle EUR transactions in date range")
    void shouldHandleEurTransactions() {
        Instant dateFrom = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant dateTo = Instant.now();

        List<Transaction> transactions = new ArrayList<>();

        Map<Integer, Integer> eurDenoms = new HashMap<>();
        eurDenoms.put(20, 5);
        eurDenoms.put(50, 2);
        transactions.add(Transaction.create(
            "LINDA", OperationType.DEPOSIT, Currency.EUR, new BigDecimal("200.00"), eurDenoms
        ));

        when(transactionRepository.findByCashierAndDateRange(eq("LINDA"), any(), any()))
            .thenReturn(transactions);

        BalanceQueryResponse response = balanceQueryService.queryBalance(dateFrom, dateTo, "LINDA");

        assertThat(response.getCashiers().get(0).getPeriodSummaries())
            .filteredOn(s -> s.getCurrency().equals("EUR"))
            .first()
            .satisfies(eur -> {
                // Starting: 2000 EUR (initial), Deposited: 200, Ending: 2200
                assertThat(eur.getStartingTotal()).isEqualByComparingTo(new BigDecimal("2000"));
                assertThat(eur.getEndingTotal()).isEqualByComparingTo(new BigDecimal("2200.00"));
                // Initial: 0x20, Deposited: 5x20, Total: 5x20
                assertThat(eur.getEndingDenominations().get(20)).isEqualTo(5);
                // Initial: 20x50, Deposited: 2x50, Total: 22x50
                assertThat(eur.getEndingDenominations().get(50)).isEqualTo(22);
            });
    }

    // ===================== Helper Methods =====================

    private void setupMockBalance(String cashierName) {
        Map<Currency, CashBalance> balances = new HashMap<>();

        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 50);
        bgnBalance.setDenominationCount(50, 10);
        balances.put(Currency.BGN, bgnBalance);

        CashBalance eurBalance = new CashBalance(Currency.EUR);
        eurBalance.setDenominationCount(10, 100);
        eurBalance.setDenominationCount(50, 20);
        balances.put(Currency.EUR, eurBalance);

        when(balanceRepository.findByCashier(cashierName)).thenReturn(balances);
    }
}
