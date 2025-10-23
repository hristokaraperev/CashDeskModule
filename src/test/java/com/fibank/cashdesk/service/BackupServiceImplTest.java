package com.fibank.cashdesk.service;

import com.fibank.cashdesk.exception.FileStorageException;
import com.fibank.cashdesk.service.impl.BackupServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackupServiceImpl.
 */
class BackupServiceImplTest {

    @TempDir
    Path tempDir;

    private BackupServiceImpl backupService;
    private Path dataDir;
    private Path backupDir;
    private Path transactionFile;
    private Path balanceFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create test directories
        dataDir = tempDir.resolve("data");
        backupDir = tempDir.resolve("backups");
        Files.createDirectories(dataDir);
        Files.createDirectories(backupDir);

        // Create test data files
        transactionFile = dataDir.resolve("transactions.txt");
        balanceFile = dataDir.resolve("balances.txt");

        Files.writeString(transactionFile, "2024-10-24T10:00:00Z|MARTINA|DEPOSIT|BGN|100.00|10:10\n");
        Files.writeString(balanceFile, "MARTINA|BGN|10|10\n");

        // Initialize service
        backupService = new BackupServiceImpl();
        ReflectionTestUtils.setField(backupService, "transactionFilePath", transactionFile.toString());
        ReflectionTestUtils.setField(backupService, "balanceFilePath", balanceFile.toString());
        ReflectionTestUtils.setField(backupService, "backupDirectory", backupDir.toString());
        ReflectionTestUtils.setField(backupService, "maxBackups", 5);
        ReflectionTestUtils.setField(backupService, "maxAgeDays", 30);
        ReflectionTestUtils.setField(backupService, "compressionEnabled", false);
    }

    @AfterEach
    void tearDown() {
        // Cleanup is handled by @TempDir
    }

    @Test
    void testCreateBackup_Success() throws IOException {
        // When
        Path backupPath = backupService.createBackup();

        // Then
        assertNotNull(backupPath);
        assertTrue(Files.exists(backupPath));
        assertTrue(Files.isDirectory(backupPath));

        // Verify backup contains required files
        assertTrue(Files.exists(backupPath.resolve("transactions.txt")));
        assertTrue(Files.exists(backupPath.resolve("balances.txt")));
        assertTrue(Files.exists(backupPath.resolve("metadata.txt")));

        // Verify content
        String transactionBackupContent = Files.readString(backupPath.resolve("transactions.txt"));
        assertTrue(transactionBackupContent.contains("MARTINA"));

        String balanceBackupContent = Files.readString(backupPath.resolve("balances.txt"));
        assertTrue(balanceBackupContent.contains("MARTINA|BGN|10|10"));
    }

    @Test
    void testCreateBackup_WithCompression() throws IOException {
        // Given
        ReflectionTestUtils.setField(backupService, "compressionEnabled", true);

        // When
        Path backupPath = backupService.createBackup();

        // Then
        assertNotNull(backupPath);
        assertTrue(Files.exists(backupPath));

        // Verify compressed files exist
        assertTrue(Files.exists(backupPath.resolve("transactions.txt.gz")));
        assertTrue(Files.exists(backupPath.resolve("balances.txt.gz")));
        assertTrue(Files.exists(backupPath.resolve("metadata.txt")));
    }

    @Test
    void testCreateBackup_MissingSourceFiles() throws IOException {
        // Given - delete source files
        Files.delete(transactionFile);
        Files.delete(balanceFile);

        // When
        Path backupPath = backupService.createBackup();

        // Then - should still create backup with empty files
        assertNotNull(backupPath);
        assertTrue(Files.exists(backupPath));
        assertTrue(Files.exists(backupPath.resolve("transactions.txt")));
        assertTrue(Files.exists(backupPath.resolve("balances.txt")));
    }

    @Test
    void testListBackups_Empty() {
        // When
        List<Path> backups = backupService.listBackups();

        // Then
        assertNotNull(backups);
        assertTrue(backups.isEmpty());
    }

    @Test
    void testListBackups_MultipleBackups() throws IOException, InterruptedException {
        // Given - create multiple backups
        backupService.createBackup();
        Thread.sleep(100); // Ensure different timestamps
        backupService.createBackup();
        Thread.sleep(100);
        backupService.createBackup();

        // When
        List<Path> backups = backupService.listBackups();

        // Then
        assertNotNull(backups);
        assertEquals(3, backups.size());

        // Verify sorted by timestamp (newest first)
        for (int i = 0; i < backups.size() - 1; i++) {
            assertTrue(
                Files.getLastModifiedTime(backups.get(i)).compareTo(
                    Files.getLastModifiedTime(backups.get(i + 1))
                ) >= 0,
                "Backups should be sorted newest first"
            );
        }
    }

    @Test
    void testGetLatestBackup_NoBackups() {
        // When
        Path latest = backupService.getLatestBackup();

        // Then
        assertNull(latest);
    }

    @Test
    void testGetLatestBackup_WithBackups() throws IOException, InterruptedException {
        // Given
        backupService.createBackup();
        Thread.sleep(100);
        Path secondBackup = backupService.createBackup();
        Thread.sleep(100);
        backupService.createBackup();

        // When
        Path latest = backupService.getLatestBackup();

        // Then
        assertNotNull(latest);
        // Latest should be the third backup (most recent)
        List<Path> backups = backupService.listBackups();
        assertEquals(backups.get(0), latest);
    }

    @Test
    void testVerifyBackup_Valid() throws IOException {
        // Given
        Path backupPath = backupService.createBackup();

        // When
        boolean valid = backupService.verifyBackup(backupPath);

        // Then
        assertTrue(valid);
    }

    @Test
    void testVerifyBackup_InvalidPath() {
        // Given
        Path nonExistentPath = tempDir.resolve("non-existent");

        // When
        boolean valid = backupService.verifyBackup(nonExistentPath);

        // Then
        assertFalse(valid);
    }

    @Test
    void testVerifyBackup_MissingFiles() throws IOException {
        // Given
        Path backupPath = backupService.createBackup();
        Files.delete(backupPath.resolve("transactions.txt"));

        // When
        boolean valid = backupService.verifyBackup(backupPath);

        // Then
        assertFalse(valid);
    }

    @Test
    void testRestoreBackup_Success() throws IOException, InterruptedException {
        // Given
        Path backupPath = backupService.createBackup();

        // Verify backup exists and is valid
        assertTrue(backupService.verifyBackup(backupPath), "Backup should be valid");
        Thread.sleep(10); // Ensure file system operations complete

        // Modify original files
        Files.writeString(transactionFile, "MODIFIED CONTENT\n");
        Files.writeString(balanceFile, "MODIFIED CONTENT\n");

        // Verify files were modified
        assertTrue(Files.readString(transactionFile).contains("MODIFIED"));
        assertTrue(Files.readString(balanceFile).contains("MODIFIED"));

        // When
        backupService.restoreBackup(backupPath);
        Thread.sleep(10); // Ensure restore completes

        // Then
        String restoredTransaction = Files.readString(transactionFile);
        String restoredBalance = Files.readString(balanceFile);

        assertFalse(restoredTransaction.contains("MODIFIED"), "Should not contain modified content after restore");
        assertFalse(restoredBalance.contains("MODIFIED"), "Should not contain modified content after restore");
        assertTrue(restoredTransaction.contains("MARTINA"), "Should contain original transaction data");
        assertTrue(restoredBalance.contains("MARTINA|BGN|10|10"), "Should contain original balance data");
    }

    @Test
    void testRestoreBackup_WithCompression() throws IOException, InterruptedException {
        // Given
        ReflectionTestUtils.setField(backupService, "compressionEnabled", true);
        Path backupPath = backupService.createBackup();

        // Verify backup was created with compressed files
        assertTrue(Files.exists(backupPath.resolve("transactions.txt.gz")), "Compressed transaction file should exist");
        assertTrue(Files.exists(backupPath.resolve("balances.txt.gz")), "Compressed balance file should exist");
        assertTrue(backupService.verifyBackup(backupPath), "Backup should be valid");
        Thread.sleep(10); // Ensure file system operations complete

        // Modify original files
        Files.writeString(transactionFile, "MODIFIED CONTENT\n");
        Files.writeString(balanceFile, "MODIFIED CONTENT\n");

        // Verify files were modified
        assertTrue(Files.readString(transactionFile).contains("MODIFIED"), "Files should contain modified content before restore");
        assertTrue(Files.readString(balanceFile).contains("MODIFIED"), "Files should contain modified content before restore");

        // When
        backupService.restoreBackup(backupPath);
        Thread.sleep(10); // Ensure restore completes

        // Then
        String restoredTransaction = Files.readString(transactionFile);
        String restoredBalance = Files.readString(balanceFile);

        assertFalse(restoredTransaction.contains("MODIFIED"), "Should not contain modified content after restore");
        assertFalse(restoredBalance.contains("MODIFIED"), "Should not contain modified content after restore");
        assertTrue(restoredTransaction.contains("MARTINA"), "Should contain original MARTINA transaction data");
        assertTrue(restoredBalance.contains("MARTINA|BGN|10|10"), "Should contain original MARTINA balance data");
    }

    @Test
    void testRestoreBackup_InvalidPath() {
        // Given
        Path nonExistentPath = tempDir.resolve("non-existent");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            backupService.restoreBackup(nonExistentPath);
        });
    }

    @Test
    void testRestoreBackup_InvalidBackup() throws IOException {
        // Given
        Path backupPath = backupService.createBackup();
        Files.delete(backupPath.resolve("transactions.txt"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            backupService.restoreBackup(backupPath);
        });
    }

    @Test
    void testCleanupOldBackups_MaxCountExceeded() throws IOException, InterruptedException {
        // Given - create more backups than maxBackups (5)
        for (int i = 0; i < 7; i++) {
            backupService.createBackup();
            Thread.sleep(50); // Ensure different timestamps
        }

        // When
        int deleted = backupService.cleanupOldBackups();

        // Then
        assertEquals(2, deleted, "Should delete 2 backups (7 - 5)");

        List<Path> remainingBackups = backupService.listBackups();
        assertEquals(5, remainingBackups.size());
    }

    @Test
    void testCleanupOldBackups_NoCleanupNeeded() throws IOException, InterruptedException {
        // Given - create fewer backups than maxBackups
        backupService.createBackup();
        Thread.sleep(10); // Ensure different timestamps
        backupService.createBackup();

        // When
        int deleted = backupService.cleanupOldBackups();

        // Then
        assertEquals(0, deleted);

        List<Path> backups = backupService.listBackups();
        assertEquals(2, backups.size());
    }

    @Test
    void testCleanupOldBackups_NoBackups() {
        // When
        int deleted = backupService.cleanupOldBackups();

        // Then
        assertEquals(0, deleted);
    }

    @Test
    void testMultipleBackupsAndRestore() throws IOException, InterruptedException {
        // Create first backup
        Path backup1 = backupService.createBackup();
        Thread.sleep(100);

        // Modify data
        Files.writeString(transactionFile, "2024-10-24T11:00:00Z|PETER|WITHDRAWAL|EUR|50.00|50:1\n");
        Files.writeString(balanceFile, "PETER|EUR|50|1\n");

        // Create second backup
        Path backup2 = backupService.createBackup();
        Thread.sleep(100);

        // Modify data again
        Files.writeString(transactionFile, "2024-10-24T12:00:00Z|LINDA|DEPOSIT|BGN|200.00|10:20\n");
        Files.writeString(balanceFile, "LINDA|BGN|10|20\n");

        // Restore from first backup
        backupService.restoreBackup(backup1);

        // Verify
        String restoredTransaction = Files.readString(transactionFile);
        assertTrue(restoredTransaction.contains("MARTINA"));
        assertFalse(restoredTransaction.contains("PETER"));
        assertFalse(restoredTransaction.contains("LINDA"));

        // Restore from second backup
        backupService.restoreBackup(backup2);

        // Verify
        restoredTransaction = Files.readString(transactionFile);
        assertTrue(restoredTransaction.contains("PETER"));
        assertFalse(restoredTransaction.contains("LINDA"));
    }
}
