package com.fibank.cashdesk.service.impl;

import com.fibank.cashdesk.dto.response.*;
import com.fibank.cashdesk.exception.InvalidDateRangeException;
import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.OperationType;
import com.fibank.cashdesk.model.Transaction;
import com.fibank.cashdesk.repository.BalanceRepository;
import com.fibank.cashdesk.repository.TransactionRepository;
import com.fibank.cashdesk.service.BalanceQueryService;
import com.fibank.cashdesk.util.MdcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of BalanceQueryService.
 * Handles balance queries with optional date range and cashier filters.
 *
 * When no date range is provided: Returns current balances.
 * When date range is provided: Returns period summary with starting balance, ending balance, net change, and transactions.
 */
@Service
public class BalanceQueryServiceImpl implements BalanceQueryService {

    private static final Logger log = LoggerFactory.getLogger(BalanceQueryServiceImpl.class);

    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;

    @Value("#{'${cashdesk.cashiers.names}'.split(',')}")
    private List<String> allCashiers;

    @Value("${cashdesk.initial.bgn:1000}")
    private BigDecimal initialBgn;

    @Value("${cashdesk.initial.eur:2000}")
    private BigDecimal initialEur;

    public BalanceQueryServiceImpl(
        BalanceRepository balanceRepository,
        TransactionRepository transactionRepository
    ) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public BalanceQueryResponse queryBalance(Instant dateFrom, Instant dateTo, String cashier) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new InvalidDateRangeException("dateFrom must be before or equal to dateTo");
        }

        if (cashier != null) {
            MdcUtil.setCashier(cashier.toUpperCase());
        }

        List<String> cashiersToQuery = (cashier != null)
            ? List.of(cashier.toUpperCase())
            : allCashiers;

        List<CashierBalanceDTO> cashierBalances = new ArrayList<>();

        for (String cashierName : cashiersToQuery) {
            if (dateFrom != null || dateTo != null) {
                List<PeriodSummaryDTO> periodSummaries = calculatePeriodSummary(cashierName, dateFrom, dateTo);
                cashierBalances.add(new CashierBalanceDTO(cashierName, periodSummaries, true));
            } else {
                Map<Currency, CashBalance> balances = balanceRepository.findByCashier(cashierName);
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
        }

        log.info("Balance query completed: {} cashier(s), dateFrom={}, dateTo={}",
            cashiersToQuery.size(), dateFrom, dateTo);

        return new BalanceQueryResponse(cashierBalances);
    }

    /**
     * Calculate period summary with starting balance, ending balance, net change, and transactions.
     *
     * @param cashier Cashier name
     * @param from Start date (inclusive), or null for system start
     * @param to End date (inclusive), or null for current time
     * @return Period summaries for each currency
     */
    private List<PeriodSummaryDTO> calculatePeriodSummary(String cashier, Instant from, Instant to) {
        List<PeriodSummaryDTO> summaries = new ArrayList<>();

        log.debug("calculatePeriodSummary: cashier={}, from={}, to={}", cashier, from, to);

        for (Currency currency : Currency.values()) {
            Map<Integer, Integer> startingDenominations = calculateBalanceAtDate(cashier, currency, from);
            BigDecimal startingTotal = calculateTotalFromDenominations(startingDenominations);

            List<Transaction> periodTransactions = transactionRepository
                .findByCashierAndDateRange(cashier, from, to)
                .stream()
                .filter(txn -> txn.getCurrency() == currency)
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(Collectors.toList());

            log.debug("Currency {}: found {} transactions in period", currency, periodTransactions.size());
            periodTransactions.forEach(txn -> log.debug("  Transaction: {}", txn));

            Map<Integer, Integer> endingDenominations = new HashMap<>(startingDenominations);
            BigDecimal netChange = BigDecimal.ZERO;

            for (Transaction txn : periodTransactions) {
                Map<Integer, Integer> txnDenoms = txn.getDenominations();
                if (txn.getOperationType() == OperationType.DEPOSIT) {
                    addDenominations(endingDenominations, txnDenoms);
                    netChange = netChange.add(txn.getAmount());
                } else {
                    subtractDenominations(endingDenominations, txnDenoms);
                    netChange = netChange.subtract(txn.getAmount());
                }
            }

            BigDecimal endingTotal = calculateTotalFromDenominations(endingDenominations);

            List<TransactionDTO> transactionDTOs = periodTransactions.stream()
                .map(txn -> new TransactionDTO(
                    txn.getId(),
                    txn.getTimestamp(),
                    txn.getOperationType().name(),
                    txn.getCurrency().name(),
                    txn.getAmount(),
                    txn.getDenominations()
                ))
                .collect(Collectors.toList());

            summaries.add(new PeriodSummaryDTO(
                currency.name(),
                startingTotal,
                startingDenominations,
                endingTotal,
                endingDenominations,
                netChange,
                transactionDTOs
            ));
        }

        return summaries.stream()
            .sorted(Comparator.comparing(PeriodSummaryDTO::getCurrency))
            .collect(Collectors.toList());
    }

    /**
     * Calculate balance strictly BEFORE a specific date by replaying transactions from system start.
     * This is used to get the starting balance for a period query.
     * Transactions AT the specified date are NOT included (they are part of the period).
     * If date is null, returns initial balance.
     *
     * @param cashier Cashier name
     * @param currency Currency
     * @param date Date to calculate balance before (or null for initial)
     * @return Denominations strictly before the specified date
     */
    private Map<Integer, Integer> calculateBalanceAtDate(String cashier, Currency currency, Instant date) {
        if (date == null) {
            log.debug("calculateBalanceAtDate: date is null, returning initial balance for {}", currency);
            return getInitialDenominations(currency);
        }

        Map<Integer, Integer> balance = new HashMap<>(getInitialDenominations(currency));

        List<Transaction> transactionsBeforeDate = transactionRepository
            .findByCashierAndDateRange(cashier, null, null)
            .stream()
            .filter(txn -> txn.getCurrency() == currency)
            .filter(txn -> txn.getTimestamp().isBefore(date))
            .sorted(Comparator.comparing(Transaction::getTimestamp))
            .collect(Collectors.toList());

        log.debug("calculateBalanceAtDate: cashier={}, currency={}, date={}, found {} txns before date",
            cashier, currency, date, transactionsBeforeDate.size());

        for (Transaction txn : transactionsBeforeDate) {
            if (txn.getOperationType() == OperationType.DEPOSIT) {
                addDenominations(balance, txn.getDenominations());
            } else {
                subtractDenominations(balance, txn.getDenominations());
            }
        }

        return balance;
    }

    /**
     * Get initial denominations for a currency based on CLAUDE.md specifications.
     */
    private Map<Integer, Integer> getInitialDenominations(Currency currency) {
        Map<Integer, Integer> initial = new HashMap<>();

        if (currency == Currency.BGN) {
            initial.put(10, 50);
            initial.put(50, 10);
        } else {
            initial.put(10, 100);
            initial.put(20, 0);
            initial.put(50, 20);
        }

        return initial;
    }

    private void addDenominations(Map<Integer, Integer> balance, Map<Integer, Integer> toAdd) {
        for (Map.Entry<Integer, Integer> entry : toAdd.entrySet()) {
            balance.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private void subtractDenominations(Map<Integer, Integer> balance, Map<Integer, Integer> toSubtract) {
        for (Map.Entry<Integer, Integer> entry : toSubtract.entrySet()) {
            int current = balance.getOrDefault(entry.getKey(), 0);
            balance.put(entry.getKey(), current - entry.getValue());
        }
    }

    private BigDecimal calculateTotalFromDenominations(Map<Integer, Integer> denominations) {
        return denominations.entrySet().stream()
            .map(entry -> BigDecimal.valueOf(entry.getKey())
                .multiply(BigDecimal.valueOf(entry.getValue())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
