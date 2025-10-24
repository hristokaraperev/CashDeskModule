package com.fibank.cashdesk.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing a single transaction in the balance query response.
 */
public class TransactionDTO {
    private UUID id;
    private Instant timestamp;
    private String operationType;
    private String currency;
    private BigDecimal amount;
    private Map<Integer, Integer> denominations;

    public TransactionDTO() {
    }

    public TransactionDTO(
        UUID id,
        Instant timestamp,
        String operationType,
        String currency,
        BigDecimal amount,
        Map<Integer, Integer> denominations
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.operationType = operationType;
        this.currency = currency;
        this.amount = amount;
        this.denominations = denominations;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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
}
