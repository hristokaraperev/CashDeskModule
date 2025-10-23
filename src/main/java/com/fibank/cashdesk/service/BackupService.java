package com.fibank.cashdesk.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for backup and recovery operations.
 * Provides functionality to backup, restore, and manage data files.
 */
public interface BackupService {

    /**
     * Create a backup of all data files.
     *
     * @return Path to the created backup
     * @throws IOException if backup creation fails
     */
    Path createBackup() throws IOException;

    /**
     * Restore data from a specific backup.
     *
     * @param backupPath Path to the backup to restore
     * @throws IOException if restore operation fails
     */
    void restoreBackup(Path backupPath) throws IOException;

    /**
     * List all available backups, sorted by creation time (newest first).
     *
     * @return List of backup paths
     */
    List<Path> listBackups();

    /**
     * Clean up old backups based on retention policy.
     * Removes backups that exceed max count or max age.
     *
     * @return Number of backups deleted
     */
    int cleanupOldBackups();

    /**
     * Get the most recent backup.
     *
     * @return Path to the most recent backup, or null if no backups exist
     */
    Path getLatestBackup();

    /**
     * Verify integrity of a backup.
     *
     * @param backupPath Path to the backup to verify
     * @return true if backup is valid, false otherwise
     */
    boolean verifyBackup(Path backupPath);
}
