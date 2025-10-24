package com.fibank.cashdesk.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Custom health indicator for the transaction file.
 * Checks:
 * - File existence and accessibility
 * - File readability and writability
 * - Transaction count and file integrity
 * - File size (for detecting potential corruption)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionFileHealthIndicator implements HealthIndicator {

    @Value("${cashdesk.storage.transaction-file}")
    private String transactionFilePath;

    @Value("${cashdesk.storage.data-dir}")
    private String dataDir;

    private static final long MAX_REASONABLE_FILE_SIZE_MB = 500;
    private static final int MAX_TRANSACTION_COUNT_FOR_QUICK_CHECK = 10000;

    @Override
    public Health health() {
        try {
            Path transactionPath = Paths.get(transactionFilePath);
            Path dataDirPath = Paths.get(dataDir);
            File transactionFile = transactionPath.toFile();
            File dataDirFile = dataDirPath.toFile();

            if (!dataDirFile.exists()) {
                log.warn("Health check: Data directory does not exist: {}", dataDir);
                return Health.down()
                        .withDetail("transactionFile", transactionFilePath)
                        .withDetail("error", "Data directory does not exist")
                        .withDetail("dataDir", dataDir)
                        .build();
            }

            if (!dataDirFile.isDirectory()) {
                log.error("Health check: Data directory path exists but is not a directory: {}", dataDir);
                return Health.down()
                        .withDetail("transactionFile", transactionFilePath)
                        .withDetail("error", "Data directory path is not a directory")
                        .withDetail("dataDir", dataDir)
                        .build();
            }

            if (!transactionFile.exists()) {
                log.warn("Health check: Transaction file does not exist: {}", transactionFilePath);
                return Health.down()
                        .withDetail("transactionFile", transactionFilePath)
                        .withDetail("error", "File does not exist")
                        .build();
            }

            if (!transactionFile.canRead()) {
                log.error("Health check: Transaction file is not readable: {}", transactionFilePath);
                return Health.down()
                        .withDetail("transactionFile", transactionFilePath)
                        .withDetail("error", "File is not readable")
                        .build();
            }

            if (!transactionFile.canWrite()) {
                log.error("Health check: Transaction file is not writable: {}", transactionFilePath);
                return Health.down()
                        .withDetail("transactionFile", transactionFilePath)
                        .withDetail("error", "File is not writable")
                        .build();
            }

            long fileSizeMB = transactionFile.length() / (1024 * 1024);
            if (fileSizeMB > MAX_REASONABLE_FILE_SIZE_MB) {
                log.warn("Health check: Transaction file size is unusually large: {} MB", fileSizeMB);
                return Health.up()
                        .withDetail("transactionFile", transactionFilePath)
                        .withDetail("fileSizeMB", fileSizeMB)
                        .withDetail("warning", "File size is unusually large, consider archiving old transactions")
                        .build();
            }

            int transactionCount = countTransactions(transactionFile);

            log.debug("Health check: Transaction file is healthy with {} transactions", transactionCount);
            return Health.up()
                    .withDetail("transactionFile", transactionFilePath)
                    .withDetail("fileSizeBytes", transactionFile.length())
                    .withDetail("transactionCount", transactionCount)
                    .withDetail("readable", true)
                    .withDetail("writable", true)
                    .build();

        } catch (Exception e) {
            log.error("Health check: Unexpected error checking transaction file health", e);
            return Health.down()
                    .withDetail("transactionFile", transactionFilePath)
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }

    /**
     * Counts the number of transactions in the file.
     * This is a quick check for file integrity.
     */
    private int countTransactions(File file) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("#")) {
                    count++;
                }
                if (count >= MAX_TRANSACTION_COUNT_FOR_QUICK_CHECK) {
                    log.debug("Transaction count exceeded quick check limit, stopping count at {}", count);
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to count transactions in health check: {}", e.getMessage());
            return -1;
        }
        return count;
    }
}
