package com.fibank.cashdesk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OperationType enum.
 */
@DisplayName("OperationType Model Tests")
class OperationTypeTest {

    @Test
    @DisplayName("Should parse DEPOSIT from string")
    void shouldParseDepositFromString() {
        OperationType type = OperationType.fromString("DEPOSIT");
        assertThat(type).isEqualTo(OperationType.DEPOSIT);
    }

    @Test
    @DisplayName("Should parse WITHDRAWAL from string")
    void shouldParseWithdrawalFromString() {
        OperationType type = OperationType.fromString("WITHDRAWAL");
        assertThat(type).isEqualTo(OperationType.WITHDRAWAL);
    }

    @Test
    @DisplayName("Should parse lowercase deposit")
    void shouldParseLowercaseDeposit() {
        OperationType type = OperationType.fromString("deposit");
        assertThat(type).isEqualTo(OperationType.DEPOSIT);
    }

    @Test
    @DisplayName("Should parse lowercase withdrawal")
    void shouldParseLowercaseWithdrawal() {
        OperationType type = OperationType.fromString("withdrawal");
        assertThat(type).isEqualTo(OperationType.WITHDRAWAL);
    }

    @Test
    @DisplayName("Should throw exception for invalid operation type")
    void shouldThrowExceptionForInvalidType() {
        assertThatThrownBy(() -> OperationType.fromString("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid operation type");
    }

    @Test
    @DisplayName("Should throw exception for null operation type")
    void shouldThrowExceptionForNullType() {
        assertThatThrownBy(() -> OperationType.fromString(null))
            .isInstanceOf(Exception.class); // NPE or IllegalArgumentException
    }
}
