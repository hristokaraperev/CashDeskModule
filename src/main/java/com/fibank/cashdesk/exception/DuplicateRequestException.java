package com.fibank.cashdesk.exception;

/**
 * Exception thrown when a duplicate request is detected based on idempotency key.
 * This is not necessarily an error condition, but indicates that the request
 * has already been processed and the cached response should be returned.
 */
public class DuplicateRequestException extends CashDeskException {

    private final String idempotencyKey;

    public DuplicateRequestException(String idempotencyKey) {
        super("Duplicate request detected with idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }

    public DuplicateRequestException(String idempotencyKey, String message) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
