package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.exception.DataCorruptionException;
import com.fibank.cashdesk.exception.FileStorageException;
import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File-based implementation of BalanceRepository.
 * Uses per-cashier read-write locks for concurrency control.
 */
@Repository
public class FileBalanceRepository implements BalanceRepository {

    private static final Logger log = LoggerFactory.getLogger(FileBalanceRepository.class);

    @Value("${cashdesk.storage.balance-file}")
    private String balanceFilePath;

    @Value("#{'${cashdesk.cashiers.names}'.split(',')}")
    private List<String> cashierNames;

    private final Map<String, Map<Currency, CashBalance>> balancesCache = new ConcurrentHashMap<>();
    private final Map<String, ReadWriteLock> cashierLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        for (String cashier : cashierNames) {
            cashierLocks.put(cashier, new ReentrantReadWriteLock());
        }

        loadBalances();
    }

    private void loadBalances() {
        File file = getBalanceFile();

        if (!file.exists()) {
            log.info("Balance file not found, creating with initial balances: {}", balanceFilePath);
            initializeWithDefaultBalances();
            saveAll(balancesCache);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.trim().isEmpty()) {
                    try {
                        parseBalanceLine(line);
                    } catch (Exception e) {
                        log.error("Failed to parse balance at line {}: {}", lineNumber, line, e);
                    }
                }
            }
            log.info("Loaded balances for {} cashiers from {}", balancesCache.size(), balanceFilePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to load balances from file", e);
        }

        for (String cashier : cashierNames) {
            balancesCache.putIfAbsent(cashier, new HashMap<>());
            for (Currency currency : Currency.values()) {
                balancesCache.get(cashier).putIfAbsent(currency, new CashBalance(currency));
            }
        }
    }

    private void initializeWithDefaultBalances() {
        for (String cashier : cashierNames) {
            Map<Currency, CashBalance> cashierBalances = new HashMap<>();

            // BGN: 1000 BGN = 50x10 + 10x50
            Map<Integer, Integer> bgnDenoms = new LinkedHashMap<>();
            bgnDenoms.put(10, 50);
            bgnDenoms.put(50, 10);
            cashierBalances.put(Currency.BGN, new CashBalance(Currency.BGN, bgnDenoms));

            // EUR: 2000 EUR = 100x10 + 0x20 + 20x50
            Map<Integer, Integer> eurDenoms = new LinkedHashMap<>();
            eurDenoms.put(10, 100);
            eurDenoms.put(20, 0);
            eurDenoms.put(50, 20);
            cashierBalances.put(Currency.EUR, new CashBalance(Currency.EUR, eurDenoms));

            balancesCache.put(cashier, cashierBalances);
        }
        log.info("Initialized default balances for {} cashiers", cashierNames.size());
    }

    @Override
    public void save(String cashier, Map<Currency, CashBalance> balances) {
        ReadWriteLock lock = cashierLocks.get(cashier);
        if (lock == null) {
            throw new IllegalArgumentException("Invalid cashier: " + cashier);
        }

        lock.writeLock().lock();
        try {
            balancesCache.put(cashier, new HashMap<>(balances));
            saveAll(balancesCache);
            log.debug("Saved balances for cashier: {}", cashier);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public synchronized void saveAll(Map<String, Map<Currency, CashBalance>> allBalances) {
        File file = getBalanceFile();
        File tempFile = new File(file.getParentFile(), "balances.tmp");

        try {
            Files.createDirectories(file.getParentFile().toPath());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                for (String cashier : allBalances.keySet()) {
                    Map<Currency, CashBalance> cashierBalances = allBalances.get(cashier);
                    for (Currency currency : Currency.values()) {
                        CashBalance balance = cashierBalances.get(currency);
                        if (balance != null) {
                            for (Map.Entry<Integer, Integer> entry : balance.getDenominations().entrySet()) {
                                String line = String.format("%s|%s|%d|%d",
                                    cashier,
                                    currency,
                                    entry.getKey(),
                                    entry.getValue()
                                );
                                writer.write(line);
                                writer.newLine();
                            }
                        }
                    }
                }
            }

            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.debug("Saved all balances to file");

        } catch (IOException e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new FileStorageException("Failed to save balances to file", e);
        }
    }

    @Override
    public Map<Currency, CashBalance> findByCashier(String cashier) {
        ReadWriteLock lock = cashierLocks.get(cashier);
        if (lock == null) {
            throw new IllegalArgumentException("Invalid cashier: " + cashier);
        }

        lock.readLock().lock();
        try {
            Map<Currency, CashBalance> balances = balancesCache.get(cashier);
            if (balances == null) {
                return new HashMap<>();
            }

            Map<Currency, CashBalance> copy = new HashMap<>();
            for (Map.Entry<Currency, CashBalance> entry : balances.entrySet()) {
                copy.put(entry.getKey(), new CashBalance(entry.getKey(), entry.getValue().getDenominations()));
            }
            return copy;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<String, Map<Currency, CashBalance>> findAll() {
        Map<String, Map<Currency, CashBalance>> result = new HashMap<>();

        for (String cashier : cashierNames) {
            result.put(cashier, findByCashier(cashier));
        }

        return result;
    }

    private void parseBalanceLine(String line) {
        String[] parts = line.split("\\|");

        if (parts.length != 4) {
            throw new DataCorruptionException("Invalid balance format: expected 4 fields, got " + parts.length);
        }

        try {
            String cashier = parts[0];
            Currency currency = Currency.valueOf(parts[1]);
            int denomination = Integer.parseInt(parts[2]);
            int count = Integer.parseInt(parts[3]);

            balancesCache
                .computeIfAbsent(cashier, k -> new HashMap<>())
                .computeIfAbsent(currency, k -> new CashBalance(currency))
                .setDenominationCount(denomination, count);

        } catch (Exception e) {
            throw new DataCorruptionException("Failed to parse balance line: " + line, e);
        }
    }

    private File getBalanceFile() {
        return new File(balanceFilePath);
    }
}
