package com.fibank.cashdesk.service.impl;

import com.fibank.cashdesk.exception.FileStorageException;
import com.fibank.cashdesk.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Implementation of BackupService for file-based backup and recovery.
 * Supports scheduled backups, compression, and retention policies.
 */
@Service
@ConditionalOnProperty(prefix = "cashdesk.backup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackupServiceImpl implements BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

    @Value("${cashdesk.storage.transaction-file}")
    private String transactionFilePath;

    @Value("${cashdesk.storage.balance-file}")
    private String balanceFilePath;

    @Value("${cashdesk.backup.directory}")
    private String backupDirectory;

    @Value("${cashdesk.backup.retention.max-backups:30}")
    private int maxBackups;

    @Value("${cashdesk.backup.retention.max-age-days:90}")
    private int maxAgeDays;

    @Value("${cashdesk.backup.compression:true}")
    private boolean compressionEnabled;

    /**
     * Scheduled backup job - runs according to cron schedule in configuration.
     */
    @Scheduled(cron = "${cashdesk.backup.schedule:0 0 2 * * *}")
    public void scheduledBackup() {
        try {
            log.info("Starting scheduled backup...");
            Path backupPath = createBackup();
            log.info("Scheduled backup completed successfully: {}", backupPath);

            // Clean up old backups
            int deleted = cleanupOldBackups();
            if (deleted > 0) {
                log.info("Cleaned up {} old backup(s)", deleted);
            }
        } catch (Exception e) {
            log.error("Scheduled backup failed", e);
        }
    }

    @Override
    public Path createBackup() throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        Path backupDir = ensureBackupDirectoryExists();

        // Create backup subdirectory for this backup
        Path backupPath = backupDir.resolve("backup_" + timestamp);
        Files.createDirectories(backupPath);

        try {
            // Backup transaction file
            backupFile(Paths.get(transactionFilePath), backupPath.resolve("transactions.txt"));

            // Backup balance file
            backupFile(Paths.get(balanceFilePath), backupPath.resolve("balances.txt"));

            // Create metadata file
            createMetadataFile(backupPath);

            log.info("Backup created successfully at: {}", backupPath);
            return backupPath;

        } catch (IOException e) {
            // Cleanup partial backup on failure
            try {
                if (Files.exists(backupPath)) {
                    deleteDirectory(backupPath);
                }
            } catch (IOException cleanupError) {
                log.warn("Failed to cleanup partial backup: {}", cleanupError.getMessage());
            }
            throw new FileStorageException("Failed to create backup", e);
        }
    }

    @Override
    public void restoreBackup(Path backupPath) throws IOException {
        if (!Files.exists(backupPath) || !Files.isDirectory(backupPath)) {
            throw new IllegalArgumentException("Invalid backup path: " + backupPath);
        }

        if (!verifyBackup(backupPath)) {
            throw new IllegalArgumentException("Backup verification failed: " + backupPath);
        }

        log.warn("Starting restore from backup: {}", backupPath);

        // Create backup of current data before restoring
        Path preRestoreBackup = null;
        try {
            preRestoreBackup = createBackup();
            log.info("Created pre-restore backup: {}", preRestoreBackup);
        } catch (IOException e) {
            log.warn("Failed to create pre-restore backup: {}", e.getMessage());
        }

        try {
            // Restore transaction file
            restoreFile(backupPath.resolve("transactions.txt"), Paths.get(transactionFilePath));

            // Restore balance file
            restoreFile(backupPath.resolve("balances.txt"), Paths.get(balanceFilePath));

            log.info("Restore completed successfully from: {}", backupPath);

        } catch (IOException e) {
            log.error("Restore failed. Please manually restore from backup or pre-restore backup: {}",
                    preRestoreBackup);
            throw new FileStorageException("Failed to restore backup", e);
        }
    }

    @Override
    public List<Path> listBackups() {
        try {
            Path backupDir = Paths.get(backupDirectory);
            if (!Files.exists(backupDir)) {
                return new ArrayList<>();
            }

            try (Stream<Path> stream = Files.list(backupDir)) {
                return stream
                        .filter(Files::isDirectory)
                        .filter(p -> p.getFileName().toString().startsWith("backup_"))
                        .sorted(Comparator.comparing(this::getBackupTimestamp).reversed())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to list backups", e);
            return new ArrayList<>();
        }
    }

    @Override
    public int cleanupOldBackups() {
        List<Path> backups = listBackups();
        int deleted = 0;

        Instant cutoffDate = Instant.now().minus(Duration.ofDays(maxAgeDays));

        for (int i = 0; i < backups.size(); i++) {
            Path backup = backups.get(i);
            boolean shouldDelete = false;
            String reason = "";

            // Check if backup exceeds max age
            try {
                Instant backupTime = getBackupTimestamp(backup);
                if (backupTime.isBefore(cutoffDate)) {
                    shouldDelete = true;
                    reason = "age exceeded";
                }
            } catch (Exception e) {
                log.warn("Error checking backup age: {}", backup.getFileName(), e);
            }

            // Check if backup exceeds max count (keep newest maxBackups)
            if (i >= maxBackups) {
                shouldDelete = true;
                reason = "count exceeded";
            }

            if (shouldDelete) {
                deleteBackup(backup);
                deleted++;
                log.info("Deleted old backup ({}): {}", reason, backup.getFileName());
            }
        }

        return deleted;
    }

    @Override
    public Path getLatestBackup() {
        List<Path> backups = listBackups();
        return backups.isEmpty() ? null : backups.get(0);
    }

    @Override
    public boolean verifyBackup(Path backupPath) {
        if (!Files.exists(backupPath) || !Files.isDirectory(backupPath)) {
            return false;
        }

        // Check that required files exist
        Path transactionBackup = backupPath.resolve("transactions.txt");
        Path balanceBackup = backupPath.resolve("balances.txt");
        Path metadataFile = backupPath.resolve("metadata.txt");

        if (compressionEnabled) {
            transactionBackup = Paths.get(transactionBackup.toString() + ".gz");
            balanceBackup = Paths.get(balanceBackup.toString() + ".gz");
        }

        return Files.exists(transactionBackup) &&
               Files.exists(balanceBackup) &&
               Files.exists(metadataFile);
    }

    private void backupFile(Path source, Path destination) throws IOException {
        if (!Files.exists(source)) {
            log.warn("Source file does not exist, skipping: {}", source);
            // Create empty file in backup
            Files.createFile(compressionEnabled ? Paths.get(destination.toString() + ".gz") : destination);
            return;
        }

        if (compressionEnabled) {
            // Backup with compression
            Path compressedDest = Paths.get(destination.toString() + ".gz");
            try (InputStream in = Files.newInputStream(source);
                 OutputStream out = Files.newOutputStream(compressedDest);
                 GZIPOutputStream gzipOut = new GZIPOutputStream(out)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    gzipOut.write(buffer, 0, len);
                }
            }
            log.debug("Backed up file with compression: {} -> {}", source, compressedDest);
        } else {
            // Backup without compression
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Backed up file: {} -> {}", source, destination);
        }
    }

    private void restoreFile(Path source, Path destination) throws IOException {
        Path actualSource = source;
        if (compressionEnabled) {
            actualSource = Paths.get(source.toString() + ".gz");
        }

        if (!Files.exists(actualSource)) {
            throw new FileNotFoundException("Backup file not found: " + actualSource);
        }

        // Ensure parent directory exists
        Files.createDirectories(destination.getParent());

        if (compressionEnabled) {
            // Restore from compressed file
            try (InputStream in = Files.newInputStream(actualSource);
                 GZIPInputStream gzipIn = new GZIPInputStream(in);
                 OutputStream out = Files.newOutputStream(destination,
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = gzipIn.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
            log.debug("Restored file from compressed backup: {} -> {}", actualSource, destination);
        } else {
            // Restore without decompression
            Files.copy(actualSource, destination, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Restored file: {} -> {}", actualSource, destination);
        }
    }

    private void createMetadataFile(Path backupPath) throws IOException {
        Path metadataFile = backupPath.resolve("metadata.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(metadataFile)) {
            writer.write("Backup Metadata\n");
            writer.write("================\n");
            writer.write("Timestamp: " + LocalDateTime.now() + "\n");
            writer.write("Compression: " + compressionEnabled + "\n");
            writer.write("Transaction file: " + transactionFilePath + "\n");
            writer.write("Balance file: " + balanceFilePath + "\n");
        }
    }

    private Instant getBackupTimestamp(Path backupPath) {
        try {
            return Files.getLastModifiedTime(backupPath).toInstant();
        } catch (IOException e) {
            log.warn("Failed to get backup timestamp for: {}", backupPath, e);
            return Instant.EPOCH;
        }
    }

    private void deleteBackup(Path backupPath) {
        try {
            deleteDirectory(backupPath);
        } catch (IOException e) {
            log.error("Failed to delete backup: {}", backupPath, e);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> stream = Files.walk(directory)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Failed to delete: {}", path, e);
                            }
                        });
            }
        }
    }

    private Path ensureBackupDirectoryExists() throws IOException {
        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
            log.info("Created backup directory: {}", backupDir);
        }
        return backupDir;
    }
}
