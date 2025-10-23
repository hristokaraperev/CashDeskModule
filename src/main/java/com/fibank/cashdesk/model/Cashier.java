package com.fibank.cashdesk.model;

import com.fibank.cashdesk.exception.InvalidCashierException;

import java.util.Objects;
import java.util.Set;

/**
 * Value object representing a cashier.
 * Immutable and thread-safe.
 */
public final class Cashier {
    private static final Set<String> VALID_CASHIERS = Set.of("MARTINA", "PETER", "LINDA");

    private final String name;

    /**
     * Create a cashier.
     * @param name Cashier name (must be in valid set)
     * @throws InvalidCashierException if name is invalid
     */
    public Cashier(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidCashierException("Cashier name cannot be null or blank");
        }

        String normalized = name.trim().toUpperCase();
        if (!VALID_CASHIERS.contains(normalized)) {
            throw new InvalidCashierException(
                "Invalid cashier name: " + name + ". Must be one of: " + VALID_CASHIERS
            );
        }

        this.name = normalized;
    }

    public String getName() {
        return name;
    }

    /**
     * Get all valid cashier names.
     * @return Immutable set of valid cashier names
     */
    public static Set<String> getValidCashiers() {
        return VALID_CASHIERS;
    }

    /**
     * Check if a name is valid.
     * @param name Name to check
     * @return true if valid
     */
    public static boolean isValid(String name) {
        if (name == null) return false;
        return VALID_CASHIERS.contains(name.trim().toUpperCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cashier cashier = (Cashier) o;
        return Objects.equals(name, cashier.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
