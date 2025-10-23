package com.fibank.cashdesk.repository;

import com.fibank.cashdesk.exception.FileStorageException;
import com.fibank.cashdesk.model.CashBalance;
import com.fibank.cashdesk.model.Currency;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for FileBalanceRepository.
 * Tests file I/O operations, concurrency, and error handling.
 */
@DisplayName("FileBalanceRepository Tests")
class FileBalanceRepositoryTest {

    @TempDir
    Path tempDir;

    private FileBalanceRepository repository;
    private String balanceFilePath;

    @BeforeEach
    void setUp() {
        balanceFilePath = tempDir.resolve("test-balances.txt").toString();
        repository = new FileBalanceRepository();
        ReflectionTestUtils.setField(repository, "balanceFilePath", balanceFilePath);
        ReflectionTestUtils.setField(repository, "cashierNames", List.of("MARTINA", "PETER", "LINDA"));
    }

    // ===================== Initialization Tests =====================

    @Test
    @DisplayName("Should initialize with default balances when file does not exist")
    void shouldInitializeWithDefaultBalances() {
        repository.initialize();

        Map<Currency, CashBalance> martinaBalances = repository.findByCashier("MARTINA");
        assertThat(martinaBalances).isNotNull();
        assertThat(martinaBalances).containsKeys(Currency.BGN, Currency.EUR);
    }

    @Test
    @DisplayName("Should create balance file with initial data")
    void shouldCreateBalanceFileWithInitialData() {
        repository.initialize();

        File file = new File(balanceFilePath);
        assertThat(file).exists().isFile();
    }

    @Test
    @DisplayName("Should load existing balances from file")
    void shouldLoadExistingBalancesFromFile() throws IOException {
        // Create file with sample balance data (format: cashier|currency|denomination|count)
        String content = "MARTINA|BGN|10|50\n" +
                        "MARTINA|BGN|50|10\n" +
                        "MARTINA|EUR|10|100\n" +
                        "MARTINA|EUR|50|20\n" +
                        "PETER|BGN|10|50\n" +
                        "PETER|BGN|50|10\n";
        Files.writeString(Path.of(balanceFilePath), content);

        repository.initialize();

        Map<Currency, CashBalance> martinaBalances = repository.findByCashier("MARTINA");
        assertThat(martinaBalances).hasSize(2);
        assertThat(martinaBalances.get(Currency.BGN).getDenominationCount(10)).isEqualTo(50);
        assertThat(martinaBalances.get(Currency.BGN).getDenominationCount(50)).isEqualTo(10);
    }

    @Test
    @DisplayName("Should skip empty lines when loading balances")
    void shouldSkipEmptyLinesWhenLoading() throws IOException {
        String content = "MARTINA|BGN|10|50\n\n\nPETER|BGN|10|50\n";
        Files.writeString(Path.of(balanceFilePath), content);

        repository.initialize();

        Map<Currency, CashBalance> martinaBalances = repository.findByCashier("MARTINA");
        Map<Currency, CashBalance> peterBalances = repository.findByCashier("PETER");

        assertThat(martinaBalances).isNotEmpty();
        assertThat(peterBalances).isNotEmpty();
    }

    @Test
    @DisplayName("Should continue loading when encountering corrupted line")
    void shouldContinueLoadingWithCorruptedLine() throws IOException {
        String content = "MARTINA|BGN|10|50\n" +
                        "CORRUPTED_LINE\n" +
                        "PETER|BGN|10|50\n";
        Files.writeString(Path.of(balanceFilePath), content);

        repository.initialize();

        Map<Currency, CashBalance> martinaBalances = repository.findByCashier("MARTINA");
        Map<Currency, CashBalance> peterBalances = repository.findByCashier("PETER");

        assertThat(martinaBalances).isNotEmpty();
        assertThat(peterBalances).isNotEmpty();
    }

    @Test
    @DisplayName("Should throw FileStorageException for invalid file location")
    void shouldThrowFileStorageExceptionForInvalidLocation() {
        ReflectionTestUtils.setField(repository, "balanceFilePath", "/invalid/path/balances.txt");

        assertThatThrownBy(() -> repository.initialize())
            .isInstanceOf(FileStorageException.class)
            .hasMessageContaining("Failed to");
    }

    // ===================== Find Operations Tests =====================

    @Test
    @DisplayName("Should find balances by cashier")
    void shouldFindBalancesByCashier() {
        repository.initialize();

        Map<Currency, CashBalance> balances = repository.findByCashier("MARTINA");

        assertThat(balances).isNotNull();
        assertThat(balances).containsKeys(Currency.BGN, Currency.EUR);
    }

    @Test
    @DisplayName("Should throw exception for unknown cashier")
    void shouldThrowExceptionForUnknownCashier() {
        repository.initialize();

        assertThatThrownBy(() -> repository.findByCashier("UNKNOWN"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cashier");
    }

    @Test
    @DisplayName("Should only accept uppercase cashier names")
    void shouldOnlyAcceptUppercaseCashierNames() {
        repository.initialize();

        // Uppercase should work
        Map<Currency, CashBalance> balances = repository.findByCashier("MARTINA");
        assertThat(balances).isNotNull();

        // Lowercase should throw exception
        assertThatThrownBy(() -> repository.findByCashier("martina"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cashier");

        // Mixed case should throw exception
        assertThatThrownBy(() -> repository.findByCashier("Martina"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cashier");
    }

    // ===================== Save Operations Tests =====================

    @Test
    @DisplayName("Should save cashier balances successfully")
    void shouldSaveCashierBalancesSuccessfully() {
        repository.initialize();

        Map<Currency, CashBalance> newBalances = new HashMap<>();
        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 100);
        bgnBalance.setDenominationCount(50, 20);
        newBalances.put(Currency.BGN, bgnBalance);

        repository.save("MARTINA", newBalances);

        Map<Currency, CashBalance> retrieved = repository.findByCashier("MARTINA");
        assertThat(retrieved.get(Currency.BGN).getDenominationCount(10)).isEqualTo(100);
        assertThat(retrieved.get(Currency.BGN).getDenominationCount(50)).isEqualTo(20);
    }

    @Test
    @DisplayName("Should persist balances to file")
    void shouldPersistBalancesToFile() {
        repository.initialize();

        Map<Currency, CashBalance> newBalances = new HashMap<>();
        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 75);
        newBalances.put(Currency.BGN, bgnBalance);

        repository.save("PETER", newBalances);

        // Create new repository instance to verify persistence
        FileBalanceRepository newRepo = new FileBalanceRepository();
        ReflectionTestUtils.setField(newRepo, "balanceFilePath", balanceFilePath);
        ReflectionTestUtils.setField(newRepo, "cashierNames", List.of("MARTINA", "PETER", "LINDA"));
        newRepo.initialize();

        Map<Currency, CashBalance> retrieved = newRepo.findByCashier("PETER");
        assertThat(retrieved.get(Currency.BGN).getDenominationCount(10)).isEqualTo(75);
    }

    @Test
    @DisplayName("Should handle multiple currency updates")
    void shouldHandleMultipleCurrencyUpdates() {
        repository.initialize();

        Map<Currency, CashBalance> balances = new HashMap<>();

        CashBalance bgnBalance = new CashBalance(Currency.BGN);
        bgnBalance.setDenominationCount(10, 50);
        bgnBalance.setDenominationCount(50, 10);
        balances.put(Currency.BGN, bgnBalance);

        CashBalance eurBalance = new CashBalance(Currency.EUR);
        eurBalance.setDenominationCount(10, 100);
        eurBalance.setDenominationCount(20, 50);
        eurBalance.setDenominationCount(50, 20);
        balances.put(Currency.EUR, eurBalance);

        repository.save("LINDA", balances);

        Map<Currency, CashBalance> retrieved = repository.findByCashier("LINDA");
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(Currency.BGN).getDenominationCount(50)).isEqualTo(10);
        assertThat(retrieved.get(Currency.EUR).getDenominationCount(20)).isEqualTo(50);
    }

    // ===================== Concurrency Tests =====================

    @Test
    @DisplayName("Should handle concurrent reads safely")
    void shouldHandleConcurrentReadsSafely() throws InterruptedException {
        repository.initialize();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                repository.findByCashier("MARTINA");
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                repository.findByCashier("MARTINA");
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Should complete without errors
        Map<Currency, CashBalance> balances = repository.findByCashier("MARTINA");
        assertThat(balances).isNotNull();
    }

    @Test
    @DisplayName("Should handle concurrent writes to different cashiers")
    void shouldHandleConcurrentWritesToDifferentCashiers() throws InterruptedException {
        repository.initialize();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                Map<Currency, CashBalance> balances = new HashMap<>();
                CashBalance balance = new CashBalance(Currency.BGN);
                balance.setDenominationCount(10, 50 + i);
                balances.put(Currency.BGN, balance);
                repository.save("MARTINA", balances);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                Map<Currency, CashBalance> balances = new HashMap<>();
                CashBalance balance = new CashBalance(Currency.BGN);
                balance.setDenominationCount(10, 50 + i);
                balances.put(Currency.BGN, balance);
                repository.save("PETER", balances);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Both cashiers should have their balances updated
        Map<Currency, CashBalance> martinaBalances = repository.findByCashier("MARTINA");
        Map<Currency, CashBalance> peterBalances = repository.findByCashier("PETER");

        assertThat(martinaBalances).isNotEmpty();
        assertThat(peterBalances).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle concurrent writes to same cashier")
    void shouldHandleConcurrentWritesToSameCashier() throws InterruptedException {
        repository.initialize();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                Map<Currency, CashBalance> balances = new HashMap<>();
                CashBalance balance = new CashBalance(Currency.BGN);
                balance.setDenominationCount(10, 100 + i);
                balances.put(Currency.BGN, balance);
                repository.save("MARTINA", balances);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                Map<Currency, CashBalance> balances = new HashMap<>();
                CashBalance balance = new CashBalance(Currency.BGN);
                balance.setDenominationCount(50, 20 + i);
                balances.put(Currency.BGN, balance);
                repository.save("MARTINA", balances);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Should complete without corruption
        Map<Currency, CashBalance> balances = repository.findByCashier("MARTINA");
        assertThat(balances).isNotNull();
        assertThat(balances.get(Currency.BGN)).isNotNull();
    }

    // ===================== Edge Cases =====================

    @Test
    @DisplayName("Should handle empty denominations")
    void shouldHandleEmptyDenominations() {
        repository.initialize();

        Map<Currency, CashBalance> balances = new HashMap<>();
        CashBalance emptyBalance = new CashBalance(Currency.BGN);
        balances.put(Currency.BGN, emptyBalance);

        repository.save("LINDA", balances);

        Map<Currency, CashBalance> retrieved = repository.findByCashier("LINDA");
        assertThat(retrieved.get(Currency.BGN)).isNotNull();
    }

    @Test
    @DisplayName("Should handle large denomination counts")
    void shouldHandleLargeDenominationCounts() {
        repository.initialize();

        Map<Currency, CashBalance> balances = new HashMap<>();
        CashBalance balance = new CashBalance(Currency.BGN);
        balance.setDenominationCount(10, 10000);
        balance.setDenominationCount(50, 5000);
        balances.put(Currency.BGN, balance);

        repository.save("PETER", balances);

        Map<Currency, CashBalance> retrieved = repository.findByCashier("PETER");
        assertThat(retrieved.get(Currency.BGN).getDenominationCount(10)).isEqualTo(10000);
        assertThat(retrieved.get(Currency.BGN).getDenominationCount(50)).isEqualTo(5000);
    }

    @Test
    @DisplayName("Should handle all three cashiers correctly")
    void shouldHandleAllThreeCashiers() {
        repository.initialize();

        Map<Currency, CashBalance> martinaBalances = repository.findByCashier("MARTINA");
        Map<Currency, CashBalance> peterBalances = repository.findByCashier("PETER");
        Map<Currency, CashBalance> lindaBalances = repository.findByCashier("LINDA");

        assertThat(martinaBalances).isNotEmpty();
        assertThat(peterBalances).isNotEmpty();
        assertThat(lindaBalances).isNotEmpty();
    }

    @Test
    @DisplayName("Should maintain data integrity across multiple operations")
    void shouldMaintainDataIntegrityAcrossOperations() {
        repository.initialize();

        // Perform multiple operations
        for (int i = 0; i < 10; i++) {
            Map<Currency, CashBalance> balances = repository.findByCashier("MARTINA");
            CashBalance bgnBalance = balances.get(Currency.BGN);
            bgnBalance.setDenominationCount(10, 50 + i);
            repository.save("MARTINA", balances);
        }

        Map<Currency, CashBalance> finalBalances = repository.findByCashier("MARTINA");
        assertThat(finalBalances.get(Currency.BGN).getDenominationCount(10)).isEqualTo(59);
    }
}
