package com.fibank.cashdesk.exception;

/**
 * Exception thrown when file I/O operations fail.
 */
public class FileStorageException extends CashDeskException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
