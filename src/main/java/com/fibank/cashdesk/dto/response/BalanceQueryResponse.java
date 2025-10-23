package com.fibank.cashdesk.dto.response;

import java.util.List;

/**
 * Response DTO for balance queries.
 */
public class BalanceQueryResponse {
    private List<CashierBalanceDTO> cashiers;

    public BalanceQueryResponse() {
    }

    public BalanceQueryResponse(List<CashierBalanceDTO> cashiers) {
        this.cashiers = cashiers;
    }

    public List<CashierBalanceDTO> getCashiers() {
        return cashiers;
    }

    public void setCashiers(List<CashierBalanceDTO> cashiers) {
        this.cashiers = cashiers;
    }
}
