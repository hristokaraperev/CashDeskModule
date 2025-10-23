package com.fibank.cashdesk.exception;

/**
 * Exception thrown when denomination validation fails.
 */
public class InvalidDenominationException extends RuntimeException {
    public InvalidDenominationException(String message) {
        super(message);
    }
}
