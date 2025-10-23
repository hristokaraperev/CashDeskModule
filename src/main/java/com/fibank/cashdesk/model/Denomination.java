package com.fibank.cashdesk.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a denomination with its count.
 * Immutable and thread-safe.
 */
public final class Denomination {
    private final int value;
    private final int count;

    /**
     * Create a denomination.
     * @param value The denomination value (10, 20, 50, etc.)
     * @param count The number of notes
     * @throws IllegalArgumentException if value or count is negative
     */
    public Denomination(int value, int count) {
        if (value <= 0) {
            throw new IllegalArgumentException("Denomination value must be positive");
        }
        if (count < 0) {
            throw new IllegalArgumentException("Denomination count cannot be negative");
        }
        this.value = value;
        this.count = count;
    }

    public int getValue() {
        return value;
    }

    public int getCount() {
        return count;
    }

    /**
     * Calculate the total amount for this denomination.
     * @return Total amount (value × count)
     */
    public BigDecimal calculateAmount() {
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(count));
    }

    /**
     * Create a new denomination with a different count.
     * @param newCount The new count
     * @return New denomination instance
     */
    public Denomination withCount(int newCount) {
        return new Denomination(value, newCount);
    }

    /**
     * Add counts from another denomination with the same value.
     * @param other Denomination to add
     * @return New denomination with combined count
     * @throws IllegalArgumentException if values don't match
     */
    public Denomination add(Denomination other) {
        if (this.value != other.value) {
            throw new IllegalArgumentException("Cannot add denominations with different values");
        }
        return new Denomination(value, this.count + other.count);
    }

    /**
     * Subtract counts from another denomination with the same value.
     * @param other Denomination to subtract
     * @return New denomination with reduced count
     * @throws IllegalArgumentException if values don't match or result is negative
     */
    public Denomination subtract(Denomination other) {
        if (this.value != other.value) {
            throw new IllegalArgumentException("Cannot subtract denominations with different values");
        }
        if (this.count < other.count) {
            throw new IllegalArgumentException(
                String.format("Insufficient %d notes. Available: %d, Required: %d",
                    value, this.count, other.count)
            );
        }
        return new Denomination(value, this.count - other.count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Denomination that = (Denomination) o;
        return value == that.value && count == that.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, count);
    }

    @Override
    public String toString() {
        return String.format("%dx%d", count, value);
    }
}
