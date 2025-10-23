package com.fibank.cashdesk.model;

import com.fibank.cashdesk.exception.InvalidDenominationException;

import java.util.Map;
import java.util.Set;

/**
 * Supported currencies with their valid denominations.
 * Immutable and thread-safe.
 */
public enum Currency {
    /**
     * Bulgarian Lev with 10 and 50 unit notes
     */
    BGN(Set.of(10, 50)),

    /**
     * Euro with 10, 20, and 50 unit notes
     */
    EUR(Set.of(10, 20, 50));

    private final Set<Integer> validDenominations;

    Currency(Set<Integer> validDenominations) {
        this.validDenominations = Set.copyOf(validDenominations); // Immutable
    }

    /**
     * Get valid denominations for this currency.
     * @return Immutable set of valid denominations
     */
    public Set<Integer> getValidDenominations() {
        return validDenominations;
    }

    /**
     * Check if a denomination is valid for this currency.
     * @param denomination The denomination to check
     * @return true if valid, false otherwise
     */
    public boolean isValidDenomination(int denomination) {
        return validDenominations.contains(denomination);
    }

    /**
     * Validate that all provided denominations are valid for this currency.
     * @param denominations Map of denominations to validate
     * @throws InvalidDenominationException if any denomination is invalid
     */
    public void validateDenominations(Map<Integer, Integer> denominations) {
        for (Integer denom : denominations.keySet()) {
            if (!isValidDenomination(denom)) {
                throw new InvalidDenominationException(
                    String.format("Invalid denomination %d for currency %s", denom, this)
                );
            }
        }
    }
}
