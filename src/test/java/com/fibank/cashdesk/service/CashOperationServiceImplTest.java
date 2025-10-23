package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.request.CashOperationRequest;
import com.fibank.cashdesk.dto.response.CashOperationResponse;
import com.fibank.cashdesk.exception.InsufficientFundsException;
import com.fibank.cashdesk.exception.InvalidCashierException;
import com.fibank.cashdesk.exception.InvalidDenominationException;
import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.BalanceRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import com.fibank.cashdesk.service.handler.CashOperationHandler;
import com.fibank.cashdesk.service.handler.DepositOperationHandler;
import com.fibank.cashdesk.service.handler.WithdrawalOperationHandler;
import com.fibank.cashdesk.service.impl.CashOperationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CashOperationServiceImpl.
 * Tests business logic for deposits and withdrawals.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CashOperationService Tests")
class CashOperationServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<Map<Currency, CashBalance>> balanceCaptor;

    private CashOperationService cashOperationService;
    private DepositOperationHandler depositHandler;
    private WithdrawalOperationHandler withdrawalHandler;

    @BeforeEach
    void setUp() {
        depositHandler = new DepositOperationHandler();
        withdrawalHandler = new WithdrawalOperationHandler();

        List<CashOperationHandler> handlers = List.of(depositHandler, withdrawalHandler);

        cashOperationService = new CashOperationServiceImpl(
            transactionRepository,
            balanceRepository,
            handlers
        );
    }

    // ===================== Deposit Tests =====================

    @Test
    @DisplayName("Should process valid BGN deposit successfully")
    void shouldProcessValidBgnDeposit() {
        // Arrange
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 10); // 100 BGN
        denominations.put(50, 10); // 500 BGN

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "MARTINA",
            "BGN",
            new BigDecimal("600.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 50);
        bgnBalance.setDenominationCount(50, 10);
        existingBalances.put(Currency.BGN, bgnBalance);

        when(balanceRepository.findByCashier("MARTINA")).thenReturn(existingBalances);

        // Act
        CashOperationResponse response = cashOperationService.processOperation(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOperationType()).isEqualTo("DEPOSIT");
        assertThat(response.getCashier()).isEqualTo("MARTINA");
        assertThat(response.getCurrency()).isEqualTo("BGN");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(response.getTransactionId()).isNotNull();
        assertThat(response.getTimestamp()).isNotNull();

        // Verify transaction was saved
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertThat(savedTransaction.getCashier()).isEqualTo("MARTINA");
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("600.00"));

        // Verify balance was updated
        verify(balanceRepository, times(1)).save(eq("MARTINA"), balanceCaptor.capture());
        Map<Currency, CashBalance> updatedBalances = balanceCaptor.getValue();
        CashBalance updatedBgnBalance = updatedBalances.get(Currency.BGN);
        assertThat(updatedBgnBalance.getDenominationCount(10)).isEqualTo(60); // 50 + 10
        assertThat(updatedBgnBalance.getDenominationCount(50)).isEqualTo(20); // 10 + 10
    }

    @Test
    @DisplayName("Should process valid EUR deposit successfully")
    void shouldProcessValidEurDeposit() {
        // Arrange
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(20, 5);  // 100 EUR
        denominations.put(50, 2);  // 100 EUR

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "PETER",
            "EUR",
            new BigDecimal("200.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        CashBalance eurBalance = new CashBalance(Currency.EUR);
        eurBalance.setDenominationCount(10, 100);
        eurBalance.setDenominationCount(50, 20);
        existingBalances.put(Currency.EUR, eurBalance);

        when(balanceRepository.findByCashier("PETER")).thenReturn(existingBalances);

        // Act
        CashOperationResponse response = cashOperationService.processOperation(request);

        // Assert
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));

        verify(balanceRepository).save(eq("PETER"), any());
    }

    // ===================== Withdrawal Tests =====================

    @Test
    @DisplayName("Should process valid BGN withdrawal successfully")
    void shouldProcessValidBgnWithdrawal() {
        // Arrange
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 5);  // 50 BGN
        denominations.put(50, 1);  // 50 BGN

        CashOperationRequest request = new CashOperationRequest(
            "WITHDRAWAL",
            "LINDA",
            "BGN",
            new BigDecimal("100.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 50);
        bgnBalance.setDenominationCount(50, 10);
        existingBalances.put(Currency.BGN, bgnBalance);

        when(balanceRepository.findByCashier("LINDA")).thenReturn(existingBalances);

        // Act
        CashOperationResponse response = cashOperationService.processOperation(request);

        // Assert
        assertThat(response.getOperationType()).isEqualTo("WITHDRAWAL");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        verify(balanceRepository).save(eq("LINDA"), balanceCaptor.capture());
        Map<Currency, CashBalance> updatedBalances = balanceCaptor.getValue();
        CashBalance updatedBgnBalance = updatedBalances.get(Currency.BGN);
        assertThat(updatedBgnBalance.getDenominationCount(10)).isEqualTo(45); // 50 - 5
        assertThat(updatedBgnBalance.getDenominationCount(50)).isEqualTo(9);  // 10 - 1
    }

    @Test
    @DisplayName("Should process valid EUR withdrawal successfully")
    void shouldProcessValidEurWithdrawal() {
        // Arrange
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(50, 10); // 500 EUR

        CashOperationRequest request = new CashOperationRequest(
            "WITHDRAWAL",
            "MARTINA",
            "EUR",
            new BigDecimal("500.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        CashBalance eurBalance = new CashBalance(Currency.EUR);
        eurBalance.setDenominationCount(10, 100);
        eurBalance.setDenominationCount(50, 20);
        existingBalances.put(Currency.EUR, eurBalance);

        when(balanceRepository.findByCashier("MARTINA")).thenReturn(existingBalances);

        // Act
        CashOperationResponse response = cashOperationService.processOperation(request);

        // Assert
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(balanceRepository).save(eq("MARTINA"), balanceCaptor.capture());
        CashBalance updatedEurBalance = balanceCaptor.getValue().get(Currency.EUR);
        assertThat(updatedEurBalance.getDenominationCount(50)).isEqualTo(10); // 20 - 10
    }

    // ===================== Validation Tests =====================

    @Test
    @DisplayName("Should throw exception for invalid cashier name")
    void shouldThrowExceptionForInvalidCashier() {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 10);

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "INVALID_CASHIER",
            "BGN",
            new BigDecimal("100.00"),
            denominations
        );

        assertThatThrownBy(() -> cashOperationService.processOperation(request))
            .isInstanceOf(InvalidCashierException.class)
            .hasMessageContaining("Invalid cashier");

        verify(balanceRepository, never()).save(anyString(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when denominations sum mismatch")
    void shouldThrowExceptionForDenominationSumMismatch() {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 5);  // Only 50 BGN

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "MARTINA",
            "BGN",
            new BigDecimal("100.00"), // But amount says 100
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        existingBalances.put(Currency.BGN, new CashBalance(Currency.BGN));
        when(balanceRepository.findByCashier("MARTINA")).thenReturn(existingBalances);

        assertThatThrownBy(() -> cashOperationService.processOperation(request))
            .isInstanceOf(InvalidDenominationException.class);

        verify(balanceRepository, never()).save(anyString(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for withdrawal with insufficient funds")
    void shouldThrowExceptionForInsufficientFunds() {
        Map<Integer, Integer> withdrawalDenominations = new HashMap<>();
        withdrawalDenominations.put(50, 30); // Need 1500 BGN

        CashOperationRequest request = new CashOperationRequest(
            "WITHDRAWAL",
            "PETER",
            "BGN",
            new BigDecimal("1500.00"),
            withdrawalDenominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 50);  // 500 BGN
        bgnBalance.setDenominationCount(50, 10);  // 500 BGN (total 1000 BGN only)
        existingBalances.put(Currency.BGN, bgnBalance);

        when(balanceRepository.findByCashier("PETER")).thenReturn(existingBalances);

        assertThatThrownBy(() -> cashOperationService.processOperation(request))
            .isInstanceOf(InsufficientFundsException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when insufficient specific denomination")
    void shouldThrowExceptionForInsufficientDenomination() {
        Map<Integer, Integer> withdrawalDenominations = new HashMap<>();
        withdrawalDenominations.put(50, 15); // Need 15x 50 BGN notes

        CashOperationRequest request = new CashOperationRequest(
            "WITHDRAWAL",
            "LINDA",
            "BGN",
            new BigDecimal("750.00"),
            withdrawalDenominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 100); // 1000 BGN in 10s
        bgnBalance.setDenominationCount(50, 10);  // Only 10x 50 BGN notes (not enough!)
        existingBalances.put(Currency.BGN, bgnBalance);

        when(balanceRepository.findByCashier("LINDA")).thenReturn(existingBalances);

        assertThatThrownBy(() -> cashOperationService.processOperation(request))
            .isInstanceOf(InsufficientFundsException.class)
            .hasMessageContaining("Insufficient");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid denomination for BGN")
    void shouldThrowExceptionForInvalidBgnDenomination() {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(25, 4); // 25 is not a valid BGN denomination

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "MARTINA",
            "BGN",
            new BigDecimal("100.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        existingBalances.put(Currency.BGN, new CashBalance(Currency.BGN));
        when(balanceRepository.findByCashier("MARTINA")).thenReturn(existingBalances);

        assertThatThrownBy(() -> cashOperationService.processOperation(request))
            .isInstanceOf(InvalidDenominationException.class)
            .hasMessageContaining("Invalid denomination");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid denomination for EUR")
    void shouldThrowExceptionForInvalidEurDenomination() {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(15, 10); // 15 is not a valid EUR denomination

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "PETER",
            "EUR",
            new BigDecimal("150.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        existingBalances.put(Currency.EUR, new CashBalance(Currency.EUR));
        when(balanceRepository.findByCashier("PETER")).thenReturn(existingBalances);

        assertThatThrownBy(() -> cashOperationService.processOperation(request))
            .isInstanceOf(InvalidDenominationException.class);

        verify(transactionRepository, never()).save(any());
    }

    // ===================== Case Insensitivity Tests =====================

    @Test
    @DisplayName("Should handle lowercase cashier name")
    void shouldHandleLowercaseCashierName() {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 10);

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "martina", // lowercase
            "BGN",
            new BigDecimal("100.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        existingBalances.put(Currency.BGN, new CashBalance(Currency.BGN));
        when(balanceRepository.findByCashier("MARTINA")).thenReturn(existingBalances);

        CashOperationResponse response = cashOperationService.processOperation(request);

        assertThat(response.getCashier()).isEqualTo("MARTINA");
        verify(balanceRepository).findByCashier("MARTINA");
    }

    @Test
    @DisplayName("Should handle lowercase operation type")
    void shouldHandleLowercaseOperationType() {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 10);

        CashOperationRequest request = new CashOperationRequest(
            "deposit", // lowercase
            "PETER",
            "BGN",
            new BigDecimal("100.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        existingBalances.put(Currency.BGN, new CashBalance(Currency.BGN));
        when(balanceRepository.findByCashier("PETER")).thenReturn(existingBalances);

        CashOperationResponse response = cashOperationService.processOperation(request);

        assertThat(response.getOperationType()).isEqualTo("DEPOSIT");
    }

    // ===================== Edge Case Tests =====================

    @Test
    @DisplayName("Should handle multiple denomination types in single deposit")
    void shouldHandleMultipleDenominationsInDeposit() {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 20);  // 200 BGN
        denominations.put(50, 12);  // 600 BGN
        // Total: 800 BGN

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "LINDA",
            "BGN",
            new BigDecimal("800.00"),
            denominations
        );

        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        existingBalances.put(Currency.BGN, new CashBalance(Currency.BGN));
        when(balanceRepository.findByCashier("LINDA")).thenReturn(existingBalances);

        CashOperationResponse response = cashOperationService.processOperation(request);

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(response.getDenominations()).hasSize(2);
    }

    @Test
    @DisplayName("Should create balance if not exists for currency")
    void shouldCreateBalanceIfNotExists() {
        Map<Integer, Integer> denominations = new HashMap<>();
        denominations.put(10, 10);

        CashOperationRequest request = new CashOperationRequest(
            "DEPOSIT",
            "MARTINA",
            "BGN",
            new BigDecimal("100.00"),
            denominations
        );

        // Return empty balances map (no BGN balance yet)
        Map<Currency, CashBalance> existingBalances = new HashMap<>();
        when(balanceRepository.findByCashier("MARTINA")).thenReturn(existingBalances);

        CashOperationResponse response = cashOperationService.processOperation(request);

        assertThat(response).isNotNull();
        verify(balanceRepository).save(eq("MARTINA"), balanceCaptor.capture());

        Map<Currency, CashBalance> savedBalances = balanceCaptor.getValue();
        assertThat(savedBalances).containsKey(Currency.BGN);
        assertThat(savedBalances.get(Currency.BGN).getDenominationCount(10)).isEqualTo(10);
    }
}
