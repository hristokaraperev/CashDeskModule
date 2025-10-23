package com.fibank.cashdesk.exception;

/**
 * Exception thrown when authentication fails.
 */
public class UnauthorizedException extends CashDeskException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
