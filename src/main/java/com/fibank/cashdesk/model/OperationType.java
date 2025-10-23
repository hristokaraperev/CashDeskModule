package com.fibank.cashdesk.model;

/**
 * Types of cash operations supported by the system.
 */
public enum OperationType {
    /**
     * Cash deposit - adds funds to cashier balance
     */
    DEPOSIT,

    /**
     * Cash withdrawal - removes funds from cashier balance
     */
    WITHDRAWAL;

    /**
     * Parse operation type from string (case-insensitive).
     * @param value String value to parse
     * @return OperationType enum value
     * @throws IllegalArgumentException if value is invalid
     */
    public static OperationType fromString(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid operation type: " + value + ". Must be DEPOSIT or WITHDRAWAL"
            );
        }
    }
}
