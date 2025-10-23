package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for backup management operations.
 * Provides endpoints for creating, listing, and restoring backups.
 */
@RestController
@RequestMapping("/api/v1/admin/backup")
@ConditionalOnProperty(prefix = "cashdesk.backup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackupController {

    private static final Logger log = LoggerFactory.getLogger(BackupController.class);

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Create a manual backup.
     *
     * @return Backup information
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBackup() {
        log.info("Manual backup requested");

        try {
            Path backupPath = backupService.createBackup();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Backup created successfully");
            response.put("backupPath", backupPath.toString());
            response.put("backupName", backupPath.getFileName().toString());

            log.info("Manual backup completed: {}", backupPath);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Manual backup failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Backup failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * List all available backups.
     *
     * @return List of backups
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listBackups() {
        log.info("Listing all backups");

        List<Path> backups = backupService.listBackups();

        List<Map<String, Object>> backupList = backups.stream()
                .map(path -> {
                    Map<String, Object> backupInfo = new HashMap<>();
                    backupInfo.put("name", path.getFileName().toString());
                    backupInfo.put("path", path.toString());
                    backupInfo.put("valid", backupService.verifyBackup(path));
                    return backupInfo;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", backupList.size());
        response.put("backups", backupList);

        Path latest = backupService.getLatestBackup();
        if (latest != null) {
            response.put("latestBackup", latest.getFileName().toString());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Restore from a specific backup.
     *
     * @param backupName Name of the backup to restore
     * @return Restore result
     */
    @PostMapping("/restore/{backupName}")
    public ResponseEntity<Map<String, Object>> restoreBackup(@PathVariable String backupName) {
        log.warn("Restore requested for backup: {}", backupName);

        try {
            // Find backup by name
            List<Path> backups = backupService.listBackups();
            Path backupPath = backups.stream()
                    .filter(p -> p.getFileName().toString().equals(backupName))
                    .findFirst()
                    .orElse(null);

            if (backupPath == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Backup not found: " + backupName);
                return ResponseEntity.notFound().build();
            }

            backupService.restoreBackup(backupPath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Restore completed successfully");
            response.put("restoredFrom", backupName);
            response.put("warning", "Application should be restarted to reload data from restored files");

            log.warn("Restore completed from backup: {}. Application restart recommended.", backupName);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid backup for restore: {}", backupName, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid backup: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            log.error("Restore failed for backup: {}", backupName, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Restore failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Verify a specific backup.
     *
     * @param backupName Name of the backup to verify
     * @return Verification result
     */
    @GetMapping("/verify/{backupName}")
    public ResponseEntity<Map<String, Object>> verifyBackup(@PathVariable String backupName) {
        log.info("Verifying backup: {}", backupName);

        // Find backup by name
        List<Path> backups = backupService.listBackups();
        Path backupPath = backups.stream()
                .filter(p -> p.getFileName().toString().equals(backupName))
                .findFirst()
                .orElse(null);

        if (backupPath == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("valid", false);
            response.put("message", "Backup not found: " + backupName);
            return ResponseEntity.status(404).body(response);
        }

        boolean valid = backupService.verifyBackup(backupPath);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("backupName", backupName);
        response.put("valid", valid);
        response.put("message", valid ? "Backup is valid" : "Backup verification failed");

        return ResponseEntity.ok(response);
    }

    /**
     * Clean up old backups based on retention policy.
     *
     * @return Cleanup result
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupBackups() {
        log.info("Manual backup cleanup requested");

        int deleted = backupService.cleanupOldBackups();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("deletedCount", deleted);
        response.put("message", deleted > 0 ?
                "Cleaned up " + deleted + " old backup(s)" :
                "No backups needed cleanup");

        log.info("Manual cleanup completed. Deleted {} backup(s)", deleted);
        return ResponseEntity.ok(response);
    }
}
