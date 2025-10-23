package com.fibank.cashdesk.model;

import com.fibank.cashdesk.exception.InvalidDenominationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Currency enum.
 */
@DisplayName("Currency Model Tests")
class CurrencyTest {

    @Test
    @DisplayName("BGN should have valid denominations 10 and 50")
    void bgnShouldHaveValidDenominations() {
        assertThat(Currency.BGN.getValidDenominations()).containsExactlyInAnyOrder(10, 50);
        assertThat(Currency.BGN.isValidDenomination(10)).isTrue();
        assertThat(Currency.BGN.isValidDenomination(50)).isTrue();
        assertThat(Currency.BGN.isValidDenomination(20)).isFalse();
    }

    @Test
    @DisplayName("EUR should have valid denominations 10, 20, and 50")
    void eurShouldHaveValidDenominations() {
        assertThat(Currency.EUR.getValidDenominations()).containsExactlyInAnyOrder(10, 20, 50);
        assertThat(Currency.EUR.isValidDenomination(10)).isTrue();
        assertThat(Currency.EUR.isValidDenomination(20)).isTrue();
        assertThat(Currency.EUR.isValidDenomination(50)).isTrue();
        assertThat(Currency.EUR.isValidDenomination(100)).isFalse();
    }

    @Test
    @DisplayName("Should validate correct BGN denominations")
    void shouldValidateCorrectBgnDenominations() {
        Map<Integer, Integer> validDenoms = new HashMap<>();
        validDenoms.put(10, 50);
        validDenoms.put(50, 10);

        assertThatNoException().isThrownBy(() -> Currency.BGN.validateDenominations(validDenoms));
    }

    @Test
    @DisplayName("Should validate correct EUR denominations")
    void shouldValidateCorrectEurDenominations() {
        Map<Integer, Integer> validDenoms = new HashMap<>();
        validDenoms.put(10, 100);
        validDenoms.put(20, 50);
        validDenoms.put(50, 20);

        assertThatNoException().isThrownBy(() -> Currency.EUR.validateDenominations(validDenoms));
    }

    @Test
    @DisplayName("Should throw exception for invalid BGN denomination")
    void shouldThrowExceptionForInvalidBgnDenomination() {
        Map<Integer, Integer> invalidDenoms = new HashMap<>();
        invalidDenoms.put(20, 5); // 20 is not valid for BGN

        assertThatThrownBy(() -> Currency.BGN.validateDenominations(invalidDenoms))
            .isInstanceOf(InvalidDenominationException.class)
            .hasMessageContaining("Invalid denomination 20 for currency BGN");
    }

    @Test
    @DisplayName("Should throw exception for invalid EUR denomination")
    void shouldThrowExceptionForInvalidEurDenomination() {
        Map<Integer, Integer> invalidDenoms = new HashMap<>();
        invalidDenoms.put(100, 5); // 100 is not valid for EUR

        assertThatThrownBy(() -> Currency.EUR.validateDenominations(invalidDenoms))
            .isInstanceOf(InvalidDenominationException.class)
            .hasMessageContaining("Invalid denomination 100 for currency EUR");
    }
}
