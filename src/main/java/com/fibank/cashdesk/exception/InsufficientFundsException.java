package com.fibank.cashdesk.exception;

/**
 * Exception thrown when attempting to withdraw more than available balance.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
