package com.fibank.cashdesk.dto.response;

import java.util.List;

/**
 * DTO representing balance for a single cashier.
 */
public class CashierBalanceDTO {
    private String cashier;
    private List<CurrencyBalanceDTO> balances;

    public CashierBalanceDTO() {
    }

    public CashierBalanceDTO(String cashier, List<CurrencyBalanceDTO> balances) {
        this.cashier = cashier;
        this.balances = balances;
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
}
