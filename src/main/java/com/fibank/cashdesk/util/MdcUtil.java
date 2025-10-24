package com.fibank.cashdesk.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for managing MDC (Mapped Diagnostic Context) for structured logging.
 * Provides correlation IDs and context tracking across the application.
 */
public class MdcUtil {

    // MDC key constants
    public static final String CORRELATION_ID = "correlationId";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String CASHIER = "cashier";
    public static final String OPERATION_TYPE = "operationType";
    public static final String CURRENCY = "currency";
    public static final String AMOUNT = "amount";

    private MdcUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generate and set a new correlation ID for the current thread.
     * Used for tracking requests across the system.
     *
     * @return The generated correlation ID
     */
    public static String generateCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID, correlationId);
        return correlationId;
    }

    /**
     * Set correlation ID for the current thread.
     *
     * @param correlationId The correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(CORRELATION_ID, correlationId);
        }
    }

    /**
     * Set transaction ID for the current thread.
     *
     * @param transactionId The transaction ID to set
     */
    public static void setTransactionId(String transactionId) {
        if (transactionId != null && !transactionId.isBlank()) {
            MDC.put(TRANSACTION_ID, transactionId);
        }
    }

    /**
     * Set transaction ID from UUID for the current thread.
     *
     * @param transactionId The transaction ID UUID to set
     */
    public static void setTransactionId(UUID transactionId) {
        if (transactionId != null) {
            MDC.put(TRANSACTION_ID, transactionId.toString());
        }
    }

    /**
     * Set cashier name for the current thread.
     *
     * @param cashier The cashier name to set
     */
    public static void setCashier(String cashier) {
        if (cashier != null && !cashier.isBlank()) {
            MDC.put(CASHIER, cashier);
        }
    }

    /**
     * Set operation type for the current thread.
     *
     * @param operationType The operation type to set
     */
    public static void setOperationType(String operationType) {
        if (operationType != null && !operationType.isBlank()) {
            MDC.put(OPERATION_TYPE, operationType);
        }
    }

    /**
     * Set currency for the current thread.
     *
     * @param currency The currency to set
     */
    public static void setCurrency(String currency) {
        if (currency != null && !currency.isBlank()) {
            MDC.put(CURRENCY, currency);
        }
    }

    /**
     * Set amount for the current thread.
     *
     * @param amount The amount to set
     */
    public static void setAmount(String amount) {
        if (amount != null && !amount.isBlank()) {
            MDC.put(AMOUNT, amount);
        }
    }

    /**
     * Get correlation ID for the current thread.
     *
     * @return The correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }

    /**
     * Get transaction ID for the current thread.
     *
     * @return The transaction ID, or null if not set
     */
    public static String getTransactionId() {
        return MDC.get(TRANSACTION_ID);
    }

    /**
     * Clear all MDC context for the current thread.
     * Should be called after request processing to prevent memory leaks.
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * Clear specific MDC key for the current thread.
     *
     * @param key The MDC key to clear
     */
    public static void clear(String key) {
        if (key != null) {
            MDC.remove(key);
        }
    }
}
