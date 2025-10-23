package com.fibank.cashdesk.exception;

/**
 * Exception thrown when date range validation fails.
 */
public class InvalidDateRangeException extends CashDeskException {
    public InvalidDateRangeException(String message) {
        super(message);
    }
}
