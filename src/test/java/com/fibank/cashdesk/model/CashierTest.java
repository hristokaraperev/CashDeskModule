package com.fibank.cashdesk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Cashier model class.
 */
@DisplayName("Cashier Model Tests")
class CashierTest {

    @Test
    @DisplayName("Should validate MARTINA as valid cashier")
    void shouldValidateMartina() {
        assertThat(Cashier.isValid("MARTINA")).isTrue();
    }

    @Test
    @DisplayName("Should validate PETER as valid cashier")
    void shouldValidatePeter() {
        assertThat(Cashier.isValid("PETER")).isTrue();
    }

    @Test
    @DisplayName("Should validate LINDA as valid cashier")
    void shouldValidateLinda() {
        assertThat(Cashier.isValid("LINDA")).isTrue();
    }

    @Test
    @DisplayName("Should invalidate unknown cashier")
    void shouldInvalidateUnknownCashier() {
        assertThat(Cashier.isValid("JOHN")).isFalse();
        assertThat(Cashier.isValid("UNKNOWN")).isFalse();
        assertThat(Cashier.isValid("")).isFalse();
    }

    @Test
    @DisplayName("Should handle null cashier name")
    void shouldHandleNullCashierName() {
        assertThat(Cashier.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("Should be case insensitive")
    void shouldBeCaseInsensitive() {
        assertThat(Cashier.isValid("MARTINA")).isTrue();
        assertThat(Cashier.isValid("martina")).isTrue();
        assertThat(Cashier.isValid("Martina")).isTrue();
        assertThat(Cashier.isValid("PETER")).isTrue();
        assertThat(Cashier.isValid("peter")).isTrue();
    }
}
