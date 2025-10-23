package com.fibank.cashdesk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Transaction model class.
 */
@DisplayName("Transaction Model Tests")
class TransactionTest {

    @Test
    @DisplayName("Should create transaction with factory method")
    void shouldCreateTransactionWithFactoryMethod() {
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 10);
        denoms.put(50, 10);

        Transaction transaction = Transaction.create(
            "MARTINA",
            OperationType.DEPOSIT,
            Currency.BGN,
            new BigDecimal("600.00"),
            denoms
        );

        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getTimestamp()).isNotNull();
        assertThat(transaction.getCashier()).isEqualTo("MARTINA");
        assertThat(transaction.getOperationType()).isEqualTo(OperationType.DEPOSIT);
        assertThat(transaction.getCurrency()).isEqualTo(Currency.BGN);
        assertThat(transaction.getAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(transaction.getDenominations()).containsEntry(10, 10);
    }

    @Test
    @DisplayName("Should create transaction with constructor")
    void shouldCreateTransactionWithConstructor() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.now();
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(50, 10);

        Transaction transaction = new Transaction(
            id,
            timestamp,
            "PETER",
            OperationType.WITHDRAWAL,
            Currency.EUR,
            new BigDecimal("500.00"),
            denoms
        );

        assertThat(transaction.getId()).isEqualTo(id);
        assertThat(transaction.getTimestamp()).isEqualTo(timestamp);
        assertThat(transaction.getCashier()).isEqualTo("PETER");
        assertThat(transaction.getOperationType()).isEqualTo(OperationType.WITHDRAWAL);
        assertThat(transaction.getCurrency()).isEqualTo(Currency.EUR);
    }

    @Test
    @DisplayName("Should check if within date range - both bounds")
    void shouldCheckIfWithinDateRangeBothBounds() {
        Instant now = Instant.now();
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 10);

        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            now,
            "LINDA",
            OperationType.DEPOSIT,
            Currency.BGN,
            new BigDecimal("100.00"),
            denoms
        );

        Instant before = now.minus(1, ChronoUnit.HOURS);
        Instant after = now.plus(1, ChronoUnit.HOURS);

        assertThat(transaction.isWithinDateRange(before, after)).isTrue();
    }

    @Test
    @DisplayName("Should check if within date range - only dateFrom")
    void shouldCheckIfWithinDateRangeOnlyDateFrom() {
        Instant now = Instant.now();
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 10);

        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            now,
            "MARTINA",
            OperationType.DEPOSIT,
            Currency.BGN,
            new BigDecimal("100.00"),
            denoms
        );

        Instant before = now.minus(1, ChronoUnit.HOURS);

        assertThat(transaction.isWithinDateRange(before, null)).isTrue();
    }

    @Test
    @DisplayName("Should check if within date range - only dateTo")
    void shouldCheckIfWithinDateRangeOnlyDateTo() {
        Instant now = Instant.now();
        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 10);

        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            now,
            "PETER",
            OperationType.DEPOSIT,
            Currency.BGN,
            new BigDecimal("100.00"),
            denoms
        );

        Instant after = now.plus(1, ChronoUnit.HOURS);

        assertThat(transaction.isWithinDateRange(null, after)).isTrue();
    }

    @Test
    @DisplayName("Should return true when both date bounds are null")
    void shouldReturnTrueWhenBothDateBoundsNull() {
        Transaction transaction = Transaction.create(
            "LINDA",
            OperationType.DEPOSIT,
            Currency.EUR,
            new BigDecimal("100.00"),
            Map.of(10, 10)
        );

        assertThat(transaction.isWithinDateRange(null, null)).isTrue();
    }

    @Test
    @DisplayName("Should return false when before dateFrom")
    void shouldReturnFalseWhenBeforeDateFrom() {
        Instant now = Instant.now();
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            now,
            "MARTINA",
            OperationType.DEPOSIT,
            Currency.BGN,
            new BigDecimal("100.00"),
            Map.of(10, 10)
        );

        Instant after = now.plus(1, ChronoUnit.HOURS);

        assertThat(transaction.isWithinDateRange(after, null)).isFalse();
    }

    @Test
    @DisplayName("Should return false when after dateTo")
    void shouldReturnFalseWhenAfterDateTo() {
        Instant now = Instant.now();
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            now,
            "PETER",
            OperationType.DEPOSIT,
            Currency.BGN,
            new BigDecimal("100.00"),
            Map.of(10, 10)
        );

        Instant before = now.minus(1, ChronoUnit.HOURS);

        assertThat(transaction.isWithinDateRange(null, before)).isFalse();
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.now();
        Map<Integer, Integer> denoms = Map.of(10, 10);

        Transaction tx1 = new Transaction(id, timestamp, "MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), denoms);
        Transaction tx2 = new Transaction(id, timestamp, "MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), denoms);

        assertThat(tx1).isEqualTo(tx2);
        assertThat(tx1.hashCode()).isEqualTo(tx2.hashCode());
    }

    @Test
    @DisplayName("Should implement equals for different objects")
    void shouldImplementEqualsForDifferentObjects() {
        Transaction tx1 = Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10));
        Transaction tx2 = Transaction.create("PETER", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10));

        assertThat(tx1).isNotEqualTo(tx2);
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        Transaction transaction = Transaction.create(
            "LINDA",
            OperationType.WITHDRAWAL,
            Currency.EUR,
            new BigDecimal("200.00"),
            Map.of(50, 4)
        );

        String str = transaction.toString();

        assertThat(str).contains("Transaction");
        assertThat(str).contains("LINDA");
        assertThat(str).contains("WITHDRAWAL");
    }
}
