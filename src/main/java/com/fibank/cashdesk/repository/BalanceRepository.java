package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;

import java.util.Map;

/**
 * Repository interface for balance persistence.
 */
public interface BalanceRepository {

    /**
     * Save balances for a specific cashier.
     * @param cashier Cashier name
     * @param balances Map of currency to balance
     */
    void save(String cashier, Map<Currency, CashBalance> balances);

    /**
     * Save all cashier balances.
     * @param allBalances Map of cashier to currency balances
     */
    void saveAll(Map<String, Map<Currency, CashBalance>> allBalances);

    /**
     * Find balances for a specific cashier.
     * @param cashier Cashier name
     * @return Map of currency to balance for the cashier
     */
    Map<Currency, CashBalance> findByCashier(String cashier);

    /**
     * Find all cashier balances.
     * @return Map of cashier to currency balances
     */
    Map<String, Map<Currency, CashBalance>> findAll();
}
