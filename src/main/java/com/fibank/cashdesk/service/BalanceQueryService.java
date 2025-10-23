package com.fibank.cashdesk.service;

import com.fibank.cashdesk.dto.response.BalanceQueryResponse;

import java.time.Instant;

/**
 * Service interface for querying balances.
 */
public interface BalanceQueryService {

    /**
     * Query balances with optional filters.
     * @param dateFrom Start date (inclusive), or null for no lower bound
     * @param dateTo End date (inclusive), or null for no upper bound
     * @param cashier Cashier name, or null for all cashiers
     * @return Balance query response
     */
    BalanceQueryResponse queryBalance(Instant dateFrom, Instant dateTo, String cashier);
}
