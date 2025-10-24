package com.fibank.cashdesk.exception;

/**
 * Exception thrown when an invalid or missing idempotency key is provided.
 * This is a critical security requirement for banking operations.
 */
public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException(String message) {
        super(message);
    }

    public InvalidIdempotencyKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
