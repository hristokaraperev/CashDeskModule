package com.fibank.cashdesk.dto.response;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO representing balance for a single currency.
 */
public class CurrencyBalanceDTO {
    private String currency;
    private BigDecimal total;
    private Map<Integer, Integer> denominations;

    public CurrencyBalanceDTO() {
    }

    public CurrencyBalanceDTO(String currency, BigDecimal total, Map<Integer, Integer> denominations) {
        this.currency = currency;
        this.total = total;
        this.denominations = denominations;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Map<Integer, Integer> getDenominations() {
        return denominations;
    }

    public void setDenominations(Map<Integer, Integer> denominations) {
        this.denominations = denominations;
    }
}
