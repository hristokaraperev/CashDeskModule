package com.fibank.cashdesk.exception;

/**
 * Exception thrown when cashier validation fails.
 */
public class InvalidCashierException extends RuntimeException {
    public InvalidCashierException(String message) {
        super(message);
    }
}
