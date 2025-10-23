package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.exception.FileStorageException;
import com.fibank.cashdesk.model.Currency;
import com.fibank.cashdesk.model.OperationType;
import com.fibank.cashdesk.model.Transaction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for FileTransactionRepository.
 * Tests file I/O operations, error handling, and edge cases.
 */
@DisplayName("FileTransactionRepository Tests")
class FileTransactionRepositoryTest {

    @TempDir
    Path tempDir;

    private FileTransactionRepository repository;
    private String transactionFilePath;

    @BeforeEach
    void setUp() {
        transactionFilePath = tempDir.resolve("test-transactions.txt").toString();
        repository = new FileTransactionRepository();
        ReflectionTestUtils.setField(repository, "transactionFilePath", transactionFilePath);
    }

    // ===================== Initialization Tests =====================

    @Test
    @DisplayName("Should initialize repository and create file if not exists")
    void shouldInitializeAndCreateFile() {
        repository.initialize();

        File file = new File(transactionFilePath);
        assertThat(file).exists().isFile();
    }

    @Test
    @DisplayName("Should load existing transactions on initialization")
    void shouldLoadExistingTransactions() throws IOException {
        // Create file with sample transactions
        String content = String.format(
            "%s|MARTINA|DEPOSIT|BGN|100.00|10:10%n%s|PETER|WITHDRAWAL|EUR|50.00|50:1%n",
            Instant.now(),
            Instant.now()
        );
        Files.writeString(Path.of(transactionFilePath), content);

        repository.initialize();

        List<Transaction> transactions = repository.findAll();
        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).getCashier()).isEqualTo("MARTINA");
        assertThat(transactions.get(1).getCashier()).isEqualTo("PETER");
    }

    @Test
    @DisplayName("Should skip empty lines when loading transactions")
    void shouldSkipEmptyLines() throws IOException {
        String content = String.format(
            "%s|MARTINA|DEPOSIT|BGN|100.00|10:10%n%n%n%s|PETER|WITHDRAWAL|EUR|50.00|50:1%n",
            Instant.now(),
            Instant.now()
        );
        Files.writeString(Path.of(transactionFilePath), content);

        repository.initialize();

        List<Transaction> transactions = repository.findAll();
        assertThat(transactions).hasSize(2);
    }

    @Test
    @DisplayName("Should continue loading when encountering corrupted line")
    void shouldContinueLoadingWithCorruptedLine() throws IOException {
        Instant now = Instant.now();
        String content = String.format(
            "%s|MARTINA|DEPOSIT|BGN|100.00|10:10%n" +
            "CORRUPTED_LINE_WITH_INVALID_FORMAT%n" +
            "%s|PETER|WITHDRAWAL|EUR|50.00|50:1%n",
            now, now
        );
        Files.writeString(Path.of(transactionFilePath), content);

        repository.initialize();

        // Should load the valid transactions and skip the corrupted one
        List<Transaction> transactions = repository.findAll();
        assertThat(transactions).hasSize(2);
    }

    @Test
    @DisplayName("Should throw FileStorageException when file is in invalid location")
    void shouldThrowFileStorageExceptionForInvalidLocation() {
        ReflectionTestUtils.setField(repository, "transactionFilePath", "/invalid/path/file.txt");

        assertThatThrownBy(() -> repository.initialize())
            .isInstanceOf(FileStorageException.class)
            .hasMessageContaining("Failed to create transaction file");
    }

    // ===================== Save Transaction Tests =====================

    @Test
    @DisplayName("Should save transaction successfully")
    void shouldSaveTransactionSuccessfully() {
        repository.initialize();

        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 10);
        denoms.put(50, 10);

        Transaction transaction = Transaction.create(
            "MARTINA",
            OperationType.DEPOSIT,
            Currency.BGN,
            new BigDecimal("600.00"),
            denoms
        );

        repository.save(transaction);

        List<Transaction> transactions = repository.findAll();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getCashier()).isEqualTo("MARTINA");
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    @DisplayName("Should append multiple transactions to file")
    void shouldAppendMultipleTransactions() {
        repository.initialize();

        Transaction tx1 = Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10));
        Transaction tx2 = Transaction.create("PETER", OperationType.WITHDRAWAL,
            Currency.EUR, new BigDecimal("50.00"), Map.of(50, 1));
        Transaction tx3 = Transaction.create("LINDA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("200.00"), Map.of(10, 20));

        repository.save(tx1);
        repository.save(tx2);
        repository.save(tx3);

        List<Transaction> transactions = repository.findAll();
        assertThat(transactions).hasSize(3);
    }

    @Test
    @DisplayName("Should persist transactions across repository instances")
    void shouldPersistAcrossInstances() {
        repository.initialize();

        Transaction tx = Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10));
        repository.save(tx);

        // Create new repository instance pointing to same file
        FileTransactionRepository newRepo = new FileTransactionRepository();
        ReflectionTestUtils.setField(newRepo, "transactionFilePath", transactionFilePath);
        newRepo.initialize();

        List<Transaction> transactions = newRepo.findAll();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getCashier()).isEqualTo("MARTINA");
    }

    // ===================== Find Operations Tests =====================

    @Test
    @DisplayName("Should find all transactions")
    void shouldFindAllTransactions() {
        repository.initialize();

        Transaction tx1 = Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10));
        Transaction tx2 = Transaction.create("PETER", OperationType.WITHDRAWAL,
            Currency.EUR, new BigDecimal("50.00"), Map.of(50, 1));

        repository.save(tx1);
        repository.save(tx2);

        List<Transaction> all = repository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("Should find transactions by cashier")
    void shouldFindTransactionsByCashier() {
        repository.initialize();

        repository.save(Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10)));
        repository.save(Transaction.create("PETER", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("50.00"), Map.of(10, 5)));
        repository.save(Transaction.create("MARTINA", OperationType.WITHDRAWAL,
            Currency.BGN, new BigDecimal("30.00"), Map.of(10, 3)));

        List<Transaction> martinaTransactions = repository.findByCashier("MARTINA");

        assertThat(martinaTransactions).hasSize(2);
        assertThat(martinaTransactions).allMatch(tx -> tx.getCashier().equals("MARTINA"));
    }

    @Test
    @DisplayName("Should find transactions by cashier case-insensitively")
    void shouldFindTransactionsByCashierCaseInsensitive() {
        repository.initialize();

        repository.save(Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10)));

        List<Transaction> transactions = repository.findByCashier("martina");
        assertThat(transactions).hasSize(1);

        transactions = repository.findByCashier("Martina");
        assertThat(transactions).hasSize(1);
    }

    @Test
    @DisplayName("Should find transactions by date range")
    void shouldFindTransactionsByDateRange() {
        repository.initialize();

        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);

        repository.save(Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10)));

        List<Transaction> inRange = repository.findByDateRange(yesterday, tomorrow);
        assertThat(inRange).hasSize(1);

        List<Transaction> outOfRange = repository.findByDateRange(
            yesterday.minus(7, ChronoUnit.DAYS),
            yesterday
        );
        assertThat(outOfRange).isEmpty();
    }

    @Test
    @DisplayName("Should find transactions by cashier and date range")
    void shouldFindTransactionsByCashierAndDateRange() {
        repository.initialize();

        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);

        repository.save(Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10)));
        repository.save(Transaction.create("PETER", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("50.00"), Map.of(10, 5)));

        List<Transaction> martinaInRange = repository.findByCashierAndDateRange("MARTINA", yesterday, tomorrow);
        assertThat(martinaInRange).hasSize(1);
        assertThat(martinaInRange.get(0).getCashier()).isEqualTo("MARTINA");

        List<Transaction> peterInRange = repository.findByCashierAndDateRange("PETER", yesterday, tomorrow);
        assertThat(peterInRange).hasSize(1);
        assertThat(peterInRange.get(0).getCashier()).isEqualTo("PETER");
    }

    @Test
    @DisplayName("Should find all cashier transactions when cashier is null")
    void shouldFindAllWhenCashierIsNull() {
        repository.initialize();

        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);

        repository.save(Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("100.00"), Map.of(10, 10)));
        repository.save(Transaction.create("PETER", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("50.00"), Map.of(10, 5)));

        List<Transaction> allInRange = repository.findByCashierAndDateRange(null, yesterday, tomorrow);
        assertThat(allInRange).hasSize(2);
    }

    // ===================== Edge Cases and Error Handling =====================

    @Test
    @DisplayName("Should handle empty repository")
    void shouldHandleEmptyRepository() {
        repository.initialize();

        List<Transaction> all = repository.findAll();
        assertThat(all).isEmpty();

        List<Transaction> byCashier = repository.findByCashier("MARTINA");
        assertThat(byCashier).isEmpty();
    }

    @Test
    @DisplayName("Should handle transaction with multiple denominations")
    void shouldHandleMultipleDenominations() {
        repository.initialize();

        Map<Integer, Integer> denoms = new HashMap<>();
        denoms.put(10, 10);
        denoms.put(20, 5);
        denoms.put(50, 4);

        Transaction tx = Transaction.create("LINDA", OperationType.DEPOSIT,
            Currency.EUR, new BigDecimal("400.00"), denoms);

        repository.save(tx);

        List<Transaction> transactions = repository.findAll();
        assertThat(transactions.get(0).getDenominations()).hasSize(3);
        assertThat(transactions.get(0).getDenominations()).containsEntry(10, 10);
        assertThat(transactions.get(0).getDenominations()).containsEntry(20, 5);
        assertThat(transactions.get(0).getDenominations()).containsEntry(50, 4);
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent saves")
    void shouldBeThreadSafeForConcurrentSaves() throws InterruptedException {
        repository.initialize();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                repository.save(Transaction.create("MARTINA", OperationType.DEPOSIT,
                    Currency.BGN, new BigDecimal("10.00"), Map.of(10, 1)));
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                repository.save(Transaction.create("PETER", OperationType.DEPOSIT,
                    Currency.BGN, new BigDecimal("10.00"), Map.of(10, 1)));
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        List<Transaction> all = repository.findAll();
        assertThat(all).hasSize(20);
    }

    @Test
    @DisplayName("Should handle large amounts correctly")
    void shouldHandleLargeAmounts() {
        repository.initialize();

        // 99950.00 = 1999 x 50 BGN notes
        Transaction tx = Transaction.create("MARTINA", OperationType.DEPOSIT,
            Currency.BGN, new BigDecimal("99950.00"), Map.of(50, 1999));

        repository.save(tx);

        List<Transaction> transactions = repository.findAll();
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("99950.00"));
    }
}
