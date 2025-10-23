package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.model.Transaction;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for transaction persistence.
 */
public interface TransactionRepository {

    /**
     * Save a transaction to the repository.
     * @param transaction Transaction to save
     */
    void save(Transaction transaction);

    /**
     * Find all transactions.
     * @return List of all transactions
     */
    List<Transaction> findAll();

    /**
     * Find transactions within a date range.
     * @param from Start date (inclusive), or null for no lower bound
     * @param to End date (inclusive), or null for no upper bound
     * @return List of transactions within range
     */
    List<Transaction> findByDateRange(Instant from, Instant to);

    /**
     * Find transactions for a specific cashier.
     * @param cashier Cashier name
     * @return List of transactions for the cashier
     */
    List<Transaction> findByCashier(String cashier);

    /**
     * Find transactions by cashier and date range.
     * @param cashier Cashier name (or null for all)
     * @param from Start date (inclusive), or null for no lower bound
     * @param to End date (inclusive), or null for no upper bound
     * @return Filtered list of transactions
     */
    List<Transaction> findByCashierAndDateRange(String cashier, Instant from, Instant to);
}
