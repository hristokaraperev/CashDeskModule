package com.fibank.cashdesk.service.impl;

import com.fibank.cashdesk.dto.response.BalanceQueryResponse;
import com.fibank.cashdesk.dto.response.CashierBalanceDTO;
import com.fibank.cashdesk.dto.response.CurrencyBalanceDTO;
import com.fibank.cashdesk.exception.InvalidDateRangeException;
import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.BalanceRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import com.fibank.cashdesk.service.BalanceQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of BalanceQueryService.
 * Handles balance queries with optional date range and cashier filters.
 */
@Service
public class BalanceQueryServiceImpl implements BalanceQueryService {

    private static final Logger log = LoggerFactory.getLogger(BalanceQueryServiceImpl.class);

    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;

    @Value("${cashdesk.cashiers.names}")
    private List<String> allCashiers;

    public BalanceQueryServiceImpl(
        BalanceRepository balanceRepository,
        TransactionRepository transactionRepository
    ) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public BalanceQueryResponse queryBalance(Instant dateFrom, Instant dateTo, String cashier) {
        // Validate date range
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new InvalidDateRangeException("dateFrom must be before or equal to dateTo");
        }

        // Determine which cashiers to query
        List<String> cashiersToQuery = (cashier != null)
            ? List.of(cashier.toUpperCase())
            : allCashiers;

        List<CashierBalanceDTO> cashierBalances = new ArrayList<>();

        for (String cashierName : cashiersToQuery) {
            Map<Currency, CashBalance> balances;

            // If date range specified, calculate historical balance
            if (dateFrom != null || dateTo != null) {
                balances = calculateHistoricalBalance(cashierName, dateFrom, dateTo);
            } else {
                // Otherwise, use current balance
                balances = balanceRepository.findByCashier(cashierName);
            }

            // Convert to DTOs
            List<CurrencyBalanceDTO> currencyBalances = balances.entrySet().stream()
                .map(entry -> new CurrencyBalanceDTO(
                    entry.getKey().name(),
                    entry.getValue().calculateTotal(),
                    entry.getValue().getDenominations()
                ))
                .sorted(Comparator.comparing(CurrencyBalanceDTO::getCurrency))
                .collect(Collectors.toList());

            cashierBalances.add(new CashierBalanceDTO(cashierName, currencyBalances));
        }

        log.info("Balance query for cashier={}, dateFrom={}, dateTo={}", cashier, dateFrom, dateTo);

        return new BalanceQueryResponse(cashierBalances);
    }

    private Map<Currency, CashBalance> calculateHistoricalBalance(String cashier, Instant from, Instant to) {
        // Get transactions within date range for the cashier
        List<Transaction> transactions = transactionRepository.findByCashierAndDateRange(cashier, from, to);

        // Initialize balances with zeros
        Map<Currency, CashBalance> balances = new HashMap<>();
        for (Currency currency : Currency.values()) {
            balances.put(currency, new CashBalance(currency));
        }

        // Replay transactions to calculate balance
        for (Transaction txn : transactions) {
            CashBalance balance = balances.get(txn.getCurrency());

            switch (txn.getOperationType()) {
                case DEPOSIT:
                    balance.addDenominations(txn.getDenominations());
                    break;
                case WITHDRAWAL:
                    // For historical queries, we just subtract if possible
                    // (we don't enforce sufficient funds for past transactions)
                    if (balance.hasSufficientDenominations(txn.getDenominations())) {
                        balance.removeDenominations(txn.getDenominations());
                    }
                    break;
            }
        }

        return balances;
    }
}
