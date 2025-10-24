package com.fibank.cashdesk.util;

import com.fibank.cashdesk.repository.FileBalanceRepository;
import com.fibank.cashdesk.repository.FileTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for cleaning up test data files.
 * Ensures test data is isolated from production data and properly cleaned up.
 */
@Component
public class TestDataCleanup {

    @Value("${cashdesk.storage.data-dir}")
    private String dataDir;

    @Value("${cashdesk.storage.transaction-file}")
    private String transactionFile;

    @Value("${cashdesk.storage.balance-file}")
    private String balanceFile;

    @Autowired
    private FileBalanceRepository balanceRepository;

    @Autowired
    private FileTransactionRepository transactionRepository;

    /**
     * Deletes all test data files and directories, then reinitializes repositories.
     * Only works in test profile to prevent accidental deletion of production data.
     */
    public void cleanupTestData() {
        // Safety check: Only cleanup if we're in test directory
        if (!dataDir.contains("test") && !dataDir.contains("tmp")) {
            throw new IllegalStateException(
                "Refusing to cleanup data directory that doesn't look like a test directory: " + dataDir
            );
        }

        try {
            // Delete transaction file
            deleteFileIfExists(transactionFile);

            // Delete balance file
            deleteFileIfExists(balanceFile);

            // Delete the entire test data directory
            Path dataDirPath = Paths.get(dataDir);
            if (Files.exists(dataDirPath)) {
                deleteDirectoryRecursively(dataDirPath.toFile());
            }

            // CRITICAL: Reinitialize repositories to start with fresh data
            // This ensures each test starts with the initial balances
            balanceRepository.initialize();
            transactionRepository.initialize();
        } catch (IOException e) {
            // Log but don't fail - cleanup is best effort
            System.err.println("Warning: Failed to cleanup test data: " + e.getMessage());
        }
    }

    /**
     * Deletes a file if it exists.
     */
    private void deleteFileIfExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private void deleteDirectoryRecursively(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectoryRecursively(file);
                }
            }
        }
        Files.deleteIfExists(directory.toPath());
    }
}
