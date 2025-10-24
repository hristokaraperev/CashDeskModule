package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.exception.DataCorruptionException;
import com.fibank.cashdesk.exception.FileStorageException;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.OperationType;
import com.fibank.cashdesk.model.Transaction;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * File-based implementation of TransactionRepository.
 * Uses append-only transaction log with in-memory caching.
 */
@Repository
public class FileTransactionRepository implements TransactionRepository {

    private static final Logger log = LoggerFactory.getLogger(FileTransactionRepository.class);

    @Value("${cashdesk.storage.transaction-file}")
    private String transactionFilePath;

    private final List<Transaction> transactionsCache = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void initialize() {
        loadTransactions();
    }

    private void loadTransactions() {
        File file = getTransactionFile();

        if (!file.exists()) {
            log.info("Transaction file not found, creating new file: {}", transactionFilePath);
            try {
                Files.createDirectories(file.getParentFile().toPath());
                file.createNewFile();
            } catch (IOException e) {
                throw new FileStorageException("Failed to create transaction file", e);
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.trim().isEmpty()) {
                    try {
                        Transaction transaction = parseTransaction(line);
                        transactionsCache.add(transaction);
                    } catch (Exception e) {
                        log.error("Failed to parse transaction at line {}: {}", lineNumber, line, e);
                        // Continue processing - don't fail on corrupted lines
                    }
                }
            }
            log.info("Loaded {} transactions from {}", transactionsCache.size(), transactionFilePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to load transactions from file", e);
        }
    }

    @Override
    public synchronized void save(Transaction transaction) {
        File file = getTransactionFile();
        String line = formatTransaction(transaction);

        try {
            Files.writeString(
                file.toPath(),
                line + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
            transactionsCache.add(transaction);
            log.debug("Saved transaction: {}", transaction.getId());
        } catch (IOException e) {
            throw new FileStorageException("Failed to append transaction to file", e);
        }
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(transactionsCache);
    }

    @Override
    public List<Transaction> findByDateRange(Instant from, Instant to) {
        return transactionsCache.stream()
            .filter(txn -> txn.isWithinDateRange(from, to))
            .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByCashier(String cashier) {
        return transactionsCache.stream()
            .filter(txn -> txn.getCashier().equalsIgnoreCase(cashier))
            .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByCashierAndDateRange(String cashier, Instant from, Instant to) {
        return transactionsCache.stream()
            .filter(txn -> cashier == null || txn.getCashier().equalsIgnoreCase(cashier))
            .filter(txn -> txn.isWithinDateRange(from, to))
            .collect(Collectors.toList());
    }

    private Transaction parseTransaction(String line) {
        String[] parts = line.split("\\|");

        // Support both old format (6 fields without UUID) and new format (7 fields with UUID)
        if (parts.length != 6 && parts.length != 7) {
            throw new DataCorruptionException("Invalid transaction format: expected 6 or 7 fields, got " + parts.length);
        }

        try {
            UUID id;
            int offset;

            if (parts.length == 7) {
                // New format with UUID as first field
                id = UUID.fromString(parts[0]);
                offset = 1;
            } else {
                // Old format without UUID - generate new one for backward compatibility
                id = UUID.randomUUID();
                offset = 0;
                log.warn("Loading transaction in old format (without UUID): {}", line);
            }

            Instant timestamp = Instant.parse(parts[offset]);
            String cashier = parts[offset + 1];
            OperationType operationType = OperationType.valueOf(parts[offset + 2]);
            Currency currency = Currency.valueOf(parts[offset + 3]);
            BigDecimal amount = new BigDecimal(parts[offset + 4]);
            Map<Integer, Integer> denominations = parseDenominations(parts[offset + 5]);

            return new Transaction(
                id,
                timestamp,
                cashier,
                operationType,
                currency,
                amount,
                denominations
            );
        } catch (Exception e) {
            throw new DataCorruptionException("Failed to parse transaction: " + line, e);
        }
    }

    private Map<Integer, Integer> parseDenominations(String denominationString) {
        Map<Integer, Integer> result = new LinkedHashMap<>();

        if (denominationString == null || denominationString.isEmpty()) {
            return result;
        }

        String[] pairs = denominationString.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length != 2) {
                throw new DataCorruptionException("Invalid denomination format: " + pair);
            }
            int denomination = Integer.parseInt(parts[0]);
            int count = Integer.parseInt(parts[1]);
            result.put(denomination, count);
        }

        return result;
    }

    private String formatTransaction(Transaction transaction) {
        String denominationsStr = transaction.getDenominations().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining(","));

        return String.format("%s|%s|%s|%s|%s|%s|%s",
            transaction.getId().toString(),
            transaction.getTimestamp().toString(),
            transaction.getCashier(),
            transaction.getOperationType(),
            transaction.getCurrency(),
            transaction.getAmount().toPlainString(),
            denominationsStr
        );
    }

    private File getTransactionFile() {
        return new File(transactionFilePath);
    }
}
