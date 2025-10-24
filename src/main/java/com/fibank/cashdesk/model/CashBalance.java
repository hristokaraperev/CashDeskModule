package com.fibank.cashdesk.model;

import com.fibank.cashdesk.exception.InsufficientFundsException;
import com.fibank.cashdesk.exception.InvalidDenominationException;

import java.math.BigDecimal;
import java.util.*;

/**
 * Entity representing cash balance with denominations for a specific currency.
 * Thread-safe through external synchronization (repository layer).
 */
public class CashBalance {
    private final Currency currency;
    private final Map<Integer, Integer> denominations; // denomination → count

    /**
     * Create a cash balance with initial denominations.
     * @param currency The currency for this balance
     * @param initialDenominations Initial denomination counts (copied)
     */
    public CashBalance(Currency currency, Map<Integer, Integer> initialDenominations) {
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.denominations = new HashMap<>(initialDenominations);

        currency.validateDenominations(this.denominations);

        for (Integer validDenom : currency.getValidDenominations()) {
            denominations.putIfAbsent(validDenom, 0);
        }
    }

    /**
     * Create a cash balance with zero denominations.
     * @param currency The currency for this balance
     */
    public CashBalance(Currency currency) {
        this(currency, Collections.emptyMap());
    }

    public Currency getCurrency() {
        return currency;
    }

    /**
     * Get denomination counts (defensive copy).
     * @return Copy of denomination map
     */
    public Map<Integer, Integer> getDenominations() {
        return new HashMap<>(denominations);
    }

    /**
     * Get count for a specific denomination.
     * @param denomination The denomination value
     * @return Count of notes, or 0 if not present
     */
    public int getDenominationCount(int denomination) {
        return denominations.getOrDefault(denomination, 0);
    }

    /**
     * Set count for a specific denomination.
     * @param denomination The denomination value
     * @param count The new count
     * @throws InvalidDenominationException if denomination is invalid for currency
     */
    public void setDenominationCount(int denomination, int count) {
        if (!currency.isValidDenomination(denomination)) {
            throw new InvalidDenominationException(
                String.format("Invalid denomination %d for currency %s", denomination, currency)
            );
        }
        if (count < 0) {
            throw new IllegalArgumentException("Denomination count cannot be negative");
        }
        denominations.put(denomination, count);
    }

    /**
     * Add denominations to this balance (for deposits).
     * @param toAdd Map of denominations to add
     * @throws InvalidDenominationException if any denomination is invalid
     */
    public void addDenominations(Map<Integer, Integer> toAdd) {
        currency.validateDenominations(toAdd);

        for (Map.Entry<Integer, Integer> entry : toAdd.entrySet()) {
            int denomination = entry.getKey();
            int countToAdd = entry.getValue();

            if (countToAdd < 0) {
                throw new IllegalArgumentException("Cannot add negative count");
            }

            int currentCount = denominations.getOrDefault(denomination, 0);
            denominations.put(denomination, currentCount + countToAdd);
        }
    }

    /**
     * Remove denominations from this balance (for withdrawals).
     * @param toRemove Map of denominations to remove
     * @throws InsufficientFundsException if insufficient denominations available
     * @throws InvalidDenominationException if any denomination is invalid
     */
    public void removeDenominations(Map<Integer, Integer> toRemove) {
        currency.validateDenominations(toRemove);

        if (!hasSufficientDenominations(toRemove)) {
            throw new InsufficientFundsException(
                buildInsufficientDenominationsMessage(toRemove)
            );
        }

        for (Map.Entry<Integer, Integer> entry : toRemove.entrySet()) {
            int denomination = entry.getKey();
            int countToRemove = entry.getValue();

            if (countToRemove < 0) {
                throw new IllegalArgumentException("Cannot remove negative count");
            }

            int currentCount = denominations.getOrDefault(denomination, 0);
            denominations.put(denomination, currentCount - countToRemove);
        }
    }

    /**
     * Check if sufficient denominations are available.
     * @param required Map of required denominations
     * @return true if all required denominations are available
     */
    public boolean hasSufficientDenominations(Map<Integer, Integer> required) {
        for (Map.Entry<Integer, Integer> entry : required.entrySet()) {
            int denomination = entry.getKey();
            int requiredCount = entry.getValue();
            int availableCount = denominations.getOrDefault(denomination, 0);

            if (availableCount < requiredCount) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculate total amount from all denominations.
     * @return Total amount in this currency
     */
    public BigDecimal calculateTotal() {
        return denominations.entrySet().stream()
            .map(entry -> BigDecimal.valueOf(entry.getKey())
                .multiply(BigDecimal.valueOf(entry.getValue())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validate that provided denominations sum to expected amount.
     * @param denominations Map of denominations
     * @param expectedAmount Expected total amount
     * @throws InvalidDenominationException if sum doesn't match
     */
    public static void validateDenominationSum(
        Map<Integer, Integer> denominations,
        BigDecimal expectedAmount
    ) {
        BigDecimal actualSum = denominations.entrySet().stream()
            .map(entry -> BigDecimal.valueOf(entry.getKey())
                .multiply(BigDecimal.valueOf(entry.getValue())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (actualSum.compareTo(expectedAmount) != 0) {
            throw new InvalidDenominationException(
                String.format("Denominations sum (%.2f) does not match amount (%.2f)",
                    actualSum, expectedAmount)
            );
        }
    }

    private String buildInsufficientDenominationsMessage(Map<Integer, Integer> required) {
        StringBuilder sb = new StringBuilder("Insufficient denominations:\n");
        for (Map.Entry<Integer, Integer> entry : required.entrySet()) {
            int denomination = entry.getKey();
            int requiredCount = entry.getValue();
            int availableCount = denominations.getOrDefault(denomination, 0);

            if (availableCount < requiredCount) {
                sb.append(String.format("  %d notes: available=%d, required=%d, shortfall=%d\n",
                    denomination, availableCount, requiredCount, requiredCount - availableCount));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CashBalance that = (CashBalance) o;
        return currency == that.currency &&
               Objects.equals(denominations, that.denominations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, denominations);
    }

    @Override
    public String toString() {
        return String.format("CashBalance{currency=%s, total=%.2f, denominations=%s}",
            currency, calculateTotal(), denominations);
    }
}
