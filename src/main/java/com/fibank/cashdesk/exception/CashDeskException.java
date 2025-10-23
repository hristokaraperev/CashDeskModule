package com.fibank.cashdesk.exception;

/**
 * Base exception for all Cash Desk Module exceptions.
 * Extends RuntimeException for unchecked exception handling.
 */
public class CashDeskException extends RuntimeException {

    public CashDeskException(String message) {
        super(message);
    }

    public CashDeskException(String message, Throwable cause) {
        super(message, cause);
    }
}
