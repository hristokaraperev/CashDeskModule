package com.fibank.cashdesk.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for cash operations (deposits and withdrawals).
 */
public class CashOperationRequest {

    @NotBlank(message = "Operation type is required")
    @Pattern(regexp = "DEPOSIT|WITHDRAWAL", message = "Operation type must be DEPOSIT or WITHDRAWAL")
    private String operationType;

    @NotBlank(message = "Cashier name is required")
    @Pattern(regexp = "MARTINA|PETER|LINDA", message = "Cashier must be MARTINA, PETER, or LINDA")
    private String cashier;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "BGN|EUR", message = "Currency must be BGN or EUR")
    private String currency;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Denominations are required")
    @Size(min = 1, message = "At least one denomination is required")
    private Map<Integer, Integer> denominations;

    // Constructors
    public CashOperationRequest() {
    }

    public CashOperationRequest(String operationType, String cashier, String currency,
                                BigDecimal amount, Map<Integer, Integer> denominations) {
        this.operationType = operationType;
        this.cashier = cashier;
        this.currency = currency;
        this.amount = amount;
        this.denominations = denominations;
    }

    // Getters and Setters
    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getCashier() {
        return cashier;
    }

    public void setCashier(String cashier) {
        this.cashier = cashier;
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
