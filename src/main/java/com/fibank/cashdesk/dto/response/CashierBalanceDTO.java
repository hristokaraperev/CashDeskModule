package com.fibank.cashdesk.dto.response;

import java.util.List;

/**
 * DTO representing balance for a single cashier.
 * Can represent either current balance or period summary depending on query parameters.
 */
public class CashierBalanceDTO {
    private String cashier;

    // For current balance queries (no date range)
    private List<CurrencyBalanceDTO> balances;

    // For period summary queries (with date range)
    private List<PeriodSummaryDTO> periodSummaries;

    public CashierBalanceDTO() {
    }

    public CashierBalanceDTO(String cashier, List<CurrencyBalanceDTO> balances) {
        this.cashier = cashier;
        this.balances = balances;
        this.periodSummaries = null;
    }

    public CashierBalanceDTO(String cashier, List<PeriodSummaryDTO> periodSummaries, boolean isPeriodSummary) {
        this.cashier = cashier;
        this.periodSummaries = periodSummaries;
        this.balances = null;
    }

    public String getCashier() {
        return cashier;
    }

    public void setCashier(String cashier) {
        this.cashier = cashier;
    }

    public List<CurrencyBalanceDTO> getBalances() {
        return balances;
    }

    public void setBalances(List<CurrencyBalanceDTO> balances) {
        this.balances = balances;
    }

    public List<PeriodSummaryDTO> getPeriodSummaries() {
        return periodSummaries;
    }

    public void setPeriodSummaries(List<PeriodSummaryDTO> periodSummaries) {
        this.periodSummaries = periodSummaries;
    }
}
