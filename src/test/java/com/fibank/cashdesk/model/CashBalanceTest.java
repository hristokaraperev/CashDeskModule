package com.fibank.cashdesk.model;

import com.fibank.cashdesk.exception.InvalidDenominationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CashBalance model class.
 */
@DisplayName("CashBalance Model Tests")
class CashBalanceTest {

    @Test
    @DisplayName("Should create cash balance with denominations")
    void shouldCreateCashBalanceWithDenominations() {
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 50);
        denoms.put(50, 10);

        CashBalance balance = new CashBalance(Currency.BGN, denoms);

        assertThat(balance.getCurrency()).isEqualTo(Currency.BGN);
        assertThat(balance.getDenominationCount(10)).isEqualTo(50);
        assertThat(balance.getDenominationCount(50)).isEqualTo(10);
    }

    @Test
    @DisplayName("Should create empty cash balance")
    void shouldCreateEmptyCashBalance() {
        CashBalance balance = new CashBalance(Currency.EUR);

        assertThat(balance.getCurrency()).isEqualTo(Currency.EUR);
        assertThat(balance.getDenominationCount(10)).isEqualTo(0);
        assertThat(balance.calculateTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should calculate total correctly")
    void shouldCalculateTotalCorrectly() {
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 50);  // 500
        denoms.put(50, 10);  // 500
        CashBalance balance = new CashBalance(Currency.BGN, denoms);

        BigDecimal total = balance.calculateTotal();

        assertThat(total).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should add denominations correctly")
    void shouldAddDenominationsCorrectly() {
        CashBalance balance = new CashBalance(Currency.BGN);
        balance.setDenominationCount(10, 50);

        Map<Integer, Integer> toAdd = new HashMap<>();
        toAdd.put(10, 10);
        toAdd.put(50, 5);

        balance.addDenominations(toAdd);

        assertThat(balance.getDenominationCount(10)).isEqualTo(60);
        assertThat(balance.getDenominationCount(50)).isEqualTo(5);
    }

    @Test
    @DisplayName("Should remove denominations correctly")
    void shouldRemoveDenominationsCorrectly() {
        Map<Integer, Integer> initial = new HashMap<>();
        initial.put(10, 50);
        initial.put(50, 10);
        CashBalance balance = new CashBalance(Currency.BGN, initial);

        Map<Integer, Integer> toRemove = new HashMap<>();
        toRemove.put(10, 5);
        toRemove.put(50, 1);

        balance.removeDenominations(toRemove);

        assertThat(balance.getDenominationCount(10)).isEqualTo(45);
        assertThat(balance.getDenominationCount(50)).isEqualTo(9);
    }

    @Test
    @DisplayName("Should check if has sufficient denominations")
    void shouldCheckHasSufficientDenominations() {
        Map<Integer, Integer> initial = new HashMap<>();
        initial.put(10, 50);
        initial.put(50, 10);
        CashBalance balance = new CashBalance(Currency.BGN, initial);

        Map<Integer, Integer> required = new HashMap<>();
        required.put(10, 5);
        required.put(50, 1);

        assertThat(balance.hasSufficientDenominations(required)).isTrue();
    }

    @Test
    @DisplayName("Should return false when insufficient denominations")
    void shouldReturnFalseWhenInsufficientDenominations() {
        Map<Integer, Integer> initial = new HashMap<>();
        initial.put(10, 5);
        initial.put(50, 1);
        CashBalance balance = new CashBalance(Currency.BGN, initial);

        Map<Integer, Integer> required = new HashMap<>();
        required.put(10, 50);

        assertThat(balance.hasSufficientDenominations(required)).isFalse();
    }

    @Test
    @DisplayName("Should validate denomination sum matches amount")
    void shouldValidateDenominationSum() {
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 10);
        denoms.put(50, 10);

        assertThatNoException()
            .isThrownBy(() -> CashBalance.validateDenominationSum(denoms, new BigDecimal("600.00")));
    }

    @Test
    @DisplayName("Should throw exception when denomination sum mismatch")
    void shouldThrowExceptionWhenDenominationSumMismatch() {
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 10);  // Only 100

        assertThatThrownBy(() -> CashBalance.validateDenominationSum(denoms, new BigDecimal("200.00")))
            .isInstanceOf(InvalidDenominationException.class)
            .hasMessageContaining("does not match amount");
    }

    @Test
    @DisplayName("Should set denomination count")
    void shouldSetDenominationCount() {
        CashBalance balance = new CashBalance(Currency.BGN);

        balance.setDenominationCount(10, 50);

        assertThat(balance.getDenominationCount(10)).isEqualTo(50);
    }

    @Test
    @DisplayName("Should throw exception for negative denomination count")
    void shouldThrowExceptionForNegativeDenominationCount() {
        CashBalance balance = new CashBalance(Currency.BGN);

        assertThatThrownBy(() -> balance.setDenominationCount(10, -5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should get denomination count returns zero for non-existent denomination")
    void shouldGetDenominationCountReturnsZero() {
        CashBalance balance = new CashBalance(Currency.BGN);

        assertThat(balance.getDenominationCount(100)).isEqualTo(0);
    }
}
