package com.fibank.cashdesk.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable transaction record.
 * Once created, cannot be modified (audit trail requirement).
 */
public final class Transaction {
    private final UUID id;
    private final Instant timestamp;
    private final String cashier;
    private final OperationType operationType;
    private final Currency currency;
    private final BigDecimal amount;
    private final Map<Integer, Integer> denominations; // Immutable copy

    /**
     * Create a new transaction.
     * @param id Unique transaction ID
     * @param timestamp Transaction timestamp
     * @param cashier Cashier name
     * @param operationType DEPOSIT or WITHDRAWAL
     * @param currency Transaction currency
     * @param amount Transaction amount
     * @param denominations Denominations used (will be copied)
     */
    public Transaction(
        UUID id,
        Instant timestamp,
        String cashier,
        OperationType operationType,
        Currency currency,
        BigDecimal amount,
        Map<Integer, Integer> denominations
    ) {
        this.id = Objects.requireNonNull(id, "Transaction ID cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.cashier = Objects.requireNonNull(cashier, "Cashier cannot be null");
        this.operationType = Objects.requireNonNull(operationType, "Operation type cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.denominations = Map.copyOf(denominations); // Immutable copy

        // Validate amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Validate denominations
        currency.validateDenominations(denominations);
        CashBalance.validateDenominationSum(denominations, amount);
    }

    /**
     * Create a new transaction with auto-generated ID and current timestamp.
     */
    public static Transaction create(
        String cashier,
        OperationType operationType,
        Currency currency,
        BigDecimal amount,
        Map<Integer, Integer> denominations
    ) {
        return new Transaction(
            UUID.randomUUID(),
            Instant.now(),
            cashier,
            operationType,
            currency,
            amount,
            denominations
        );
    }

    // Getters only (no setters - immutable)

    public UUID getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCashier() {
        return cashier;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Get denominations (immutable map).
     * @return Immutable copy of denominations
     */
    public Map<Integer, Integer> getDenominations() {
        return denominations; // Already immutable from constructor
    }

    /**
     * Check if this transaction occurred within a date range.
     * @param startDate Start date (inclusive), or null for no lower bound
     * @param endDate End date (inclusive), or null for no upper bound
     * @return true if transaction is within range
     */
    public boolean isWithinDateRange(Instant startDate, Instant endDate) {
        if (startDate != null && timestamp.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && timestamp.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id); // Identity by ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Transaction{id=%s, timestamp=%s, cashier=%s, type=%s, currency=%s, amount=%.2f}",
            id, timestamp, cashier, operationType, currency, amount);
    }
}
