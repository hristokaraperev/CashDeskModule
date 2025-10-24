package com.fibank.cashdesk.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Custom health indicator for the balance file.
 * Checks:
 * - File existence and accessibility
 * - File readability and writability
 * - Disk space availability
 * - File size (for detecting potential corruption)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceFileHealthIndicator implements HealthIndicator {

    @Value("${cashdesk.storage.balance-file}")
    private String balanceFilePath;

    @Value("${cashdesk.storage.data-dir}")
    private String dataDir;

    private static final long MIN_FREE_DISK_SPACE_MB = 10;
    private static final long MAX_REASONABLE_FILE_SIZE_MB = 100;

    @Override
    public Health health() {
        try {
            Path balancePath = Paths.get(balanceFilePath);
            Path dataDirPath = Paths.get(dataDir);
            File balanceFile = balancePath.toFile();
            File dataDirFile = dataDirPath.toFile();

            // Check data directory exists
            if (!dataDirFile.exists()) {
                log.warn("Health check: Data directory does not exist: {}", dataDir);
                return Health.down()
                        .withDetail("balanceFile", balanceFilePath)
                        .withDetail("error", "Data directory does not exist")
                        .withDetail("dataDir", dataDir)
                        .build();
            }

            // Check data directory is actually a directory
            if (!dataDirFile.isDirectory()) {
                log.error("Health check: Data directory path exists but is not a directory: {}", dataDir);
                return Health.down()
                        .withDetail("balanceFile", balanceFilePath)
                        .withDetail("error", "Data directory path is not a directory")
                        .withDetail("dataDir", dataDir)
                        .build();
            }

            // Check file accessibility
            if (!balanceFile.exists()) {
                log.warn("Health check: Balance file does not exist: {}", balanceFilePath);
                return Health.down()
                        .withDetail("balanceFile", balanceFilePath)
                        .withDetail("error", "File does not exist")
                        .build();
            }

            // Check file is readable
            if (!balanceFile.canRead()) {
                log.error("Health check: Balance file is not readable: {}", balanceFilePath);
                return Health.down()
                        .withDetail("balanceFile", balanceFilePath)
                        .withDetail("error", "File is not readable")
                        .build();
            }

            // Check file is writable
            if (!balanceFile.canWrite()) {
                log.error("Health check: Balance file is not writable: {}", balanceFilePath);
                return Health.down()
                        .withDetail("balanceFile", balanceFilePath)
                        .withDetail("error", "File is not writable")
                        .build();
            }

            // Check disk space
            long freeSpaceMB = dataDirFile.getUsableSpace() / (1024 * 1024);
            if (freeSpaceMB < MIN_FREE_DISK_SPACE_MB) {
                log.error("Health check: Low disk space: {} MB available, minimum required: {} MB",
                        freeSpaceMB, MIN_FREE_DISK_SPACE_MB);
                return Health.down()
                        .withDetail("balanceFile", balanceFilePath)
                        .withDetail("diskSpaceMB", freeSpaceMB)
                        .withDetail("minRequiredMB", MIN_FREE_DISK_SPACE_MB)
                        .withDetail("error", "Insufficient disk space")
                        .build();
            }

            // Check file size is reasonable (potential corruption detection)
            long fileSizeMB = balanceFile.length() / (1024 * 1024);
            if (fileSizeMB > MAX_REASONABLE_FILE_SIZE_MB) {
                log.warn("Health check: Balance file size is unusually large: {} MB", fileSizeMB);
                return Health.up()
                        .withDetail("balanceFile", balanceFilePath)
                        .withDetail("fileSizeMB", fileSizeMB)
                        .withDetail("diskSpaceMB", freeSpaceMB)
                        .withDetail("warning", "File size is unusually large, possible corruption")
                        .build();
            }

            // All checks passed
            log.debug("Health check: Balance file is healthy");
            return Health.up()
                    .withDetail("balanceFile", balanceFilePath)
                    .withDetail("fileSizeBytes", balanceFile.length())
                    .withDetail("diskSpaceMB", freeSpaceMB)
                    .withDetail("readable", true)
                    .withDetail("writable", true)
                    .build();

        } catch (Exception e) {
            log.error("Health check: Unexpected error checking balance file health", e);
            return Health.down()
                    .withDetail("balanceFile", balanceFilePath)
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
