package com.fibank.cashdesk.exception;

/**
 * Exception thrown when data file parsing fails.
 */
public class DataCorruptionException extends CashDeskException {
    public DataCorruptionException(String message) {
        super(message);
    }

    public DataCorruptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
