package com.fibank.cashdesk.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for cash operation results.
 */
public class CashOperationResponse {
    private String transactionId;
    private Instant timestamp;
    private String cashier;
    private String operationType;
    private String currency;
    private BigDecimal amount;
    private Map<Integer, Integer> denominations;
    private String message;

    public CashOperationResponse() {
    }

    public CashOperationResponse(String transactionId, Instant timestamp, String cashier,
                                 String operationType, String currency, BigDecimal amount,
                                 Map<Integer, Integer> denominations, String message) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.cashier = cashier;
        this.operationType = operationType;
        this.currency = currency;
        this.amount = amount;
        this.denominations = denominations;
        this.message = message;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getCashier() {
        return cashier;
    }

    public void setCashier(String cashier) {
        this.cashier = cashier;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Map<Integer, Integer> getDenominations() {
        return denominations;
    }

    public void setDenominations(Map<Integer, Integer> denominations) {
        this.denominations = denominations;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
