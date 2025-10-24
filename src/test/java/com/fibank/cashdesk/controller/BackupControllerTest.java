package com.fibank.cashdesk.controller;

import com.fibank.cashdesk.config.AuthenticationInterceptor;
import com.fibank.cashdesk.config.WebConfig;
import com.fibank.cashdesk.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BackupController.
 */
@WebMvcTest(
    controllers = BackupController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {AuthenticationInterceptor.class, WebConfig.class}
    )
)
@TestPropertySource(properties = {
    "cashdesk.backup.enabled=true"
})
class BackupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BackupService backupService;

    private static final String API_KEY = "f9Uie8nNf112hx8s";
    private static final String AUTH_HEADER = "FIB-X-AUTH";

    @Test
    void testCreateBackup_Success() throws Exception {
        // Given
        Path mockBackupPath = Paths.get("/backups/backup_20241024_100000");
        when(backupService.createBackup()).thenReturn(mockBackupPath);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/backup")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Backup created successfully"))
                .andExpect(jsonPath("$.backupPath").exists())
                .andExpect(jsonPath("$.backupName").value("backup_20241024_100000"));

        verify(backupService, times(1)).createBackup();
    }

    @Test
    void testCreateBackup_Failure() throws Exception {
        // Given
        when(backupService.createBackup()).thenThrow(new IOException("Disk full"));

        // When & Then
        mockMvc.perform(post("/api/v1/admin/backup")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Backup failed: Disk full"));

        verify(backupService, times(1)).createBackup();
    }

    // Note: Authorization tests are excluded because we exclude AuthenticationInterceptor
    // to simplify unit testing of the controller logic. Authorization is tested separately
    // in integration tests.

    @Test
    void testListBackups_Empty() throws Exception {
        // Given
        when(backupService.listBackups()).thenReturn(Collections.emptyList());
        when(backupService.getLatestBackup()).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/backup")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.backups").isArray())
                .andExpect(jsonPath("$.backups").isEmpty())
                .andExpect(jsonPath("$.latestBackup").doesNotExist());

        verify(backupService, times(1)).listBackups();
        verify(backupService, times(1)).getLatestBackup();
    }

    @Test
    void testListBackups_WithBackups() throws Exception {
        // Given
        Path backup1 = Paths.get("/backups/backup_20241024_100000");
        Path backup2 = Paths.get("/backups/backup_20241024_110000");
        Path backup3 = Paths.get("/backups/backup_20241024_120000");

        List<Path> backups = Arrays.asList(backup3, backup2, backup1); // Newest first

        when(backupService.listBackups()).thenReturn(backups);
        when(backupService.getLatestBackup()).thenReturn(backup3);
        when(backupService.verifyBackup(any())).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/backup")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.backups").isArray())
                .andExpect(jsonPath("$.backups.length()").value(3))
                .andExpect(jsonPath("$.backups[0].name").value("backup_20241024_120000"))
                .andExpect(jsonPath("$.backups[0].valid").value(true))
                .andExpect(jsonPath("$.latestBackup").value("backup_20241024_120000"));

        verify(backupService, times(1)).listBackups();
        verify(backupService, times(3)).verifyBackup(any());
    }

    @Test
    void testRestoreBackup_Success() throws Exception {
        // Given
        Path backupPath = Paths.get("/backups/backup_20241024_100000");
        when(backupService.listBackups()).thenReturn(Collections.singletonList(backupPath));
        doNothing().when(backupService).restoreBackup(backupPath);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/backup/restore/backup_20241024_100000")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Restore completed successfully"))
                .andExpect(jsonPath("$.restoredFrom").value("backup_20241024_100000"))
                .andExpect(jsonPath("$.warning").exists());

        verify(backupService, times(1)).listBackups();
        verify(backupService, times(1)).restoreBackup(backupPath);
    }

    @Test
    void testRestoreBackup_NotFound() throws Exception {
        // Given
        when(backupService.listBackups()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/v1/admin/backup/restore/non_existent_backup")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isNotFound());

        verify(backupService, times(1)).listBackups();
        verify(backupService, never()).restoreBackup(any());
    }

    @Test
    void testRestoreBackup_InvalidBackup() throws Exception {
        // Given
        Path backupPath = Paths.get("/backups/backup_20241024_100000");
        when(backupService.listBackups()).thenReturn(Collections.singletonList(backupPath));
        doThrow(new IllegalArgumentException("Backup verification failed"))
                .when(backupService).restoreBackup(backupPath);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/backup/restore/backup_20241024_100000")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid backup: Backup verification failed"));

        verify(backupService, times(1)).restoreBackup(backupPath);
    }

    @Test
    void testRestoreBackup_RestoreFailure() throws Exception {
        // Given
        Path backupPath = Paths.get("/backups/backup_20241024_100000");
        when(backupService.listBackups()).thenReturn(Collections.singletonList(backupPath));
        doThrow(new IOException("File read error"))
                .when(backupService).restoreBackup(backupPath);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/backup/restore/backup_20241024_100000")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Restore failed: File read error"));

        verify(backupService, times(1)).restoreBackup(backupPath);
    }

    @Test
    void testVerifyBackup_Valid() throws Exception {
        // Given
        Path backupPath = Paths.get("/backups/backup_20241024_100000");
        when(backupService.listBackups()).thenReturn(Collections.singletonList(backupPath));
        when(backupService.verifyBackup(backupPath)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/backup/verify/backup_20241024_100000")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.backupName").value("backup_20241024_100000"))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Backup is valid"));

        verify(backupService, times(1)).verifyBackup(backupPath);
    }

    @Test
    void testVerifyBackup_Invalid() throws Exception {
        // Given
        Path backupPath = Paths.get("/backups/backup_20241024_100000");
        when(backupService.listBackups()).thenReturn(Collections.singletonList(backupPath));
        when(backupService.verifyBackup(backupPath)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/backup/verify/backup_20241024_100000")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Backup verification failed"));

        verify(backupService, times(1)).verifyBackup(backupPath);
    }

    @Test
    void testVerifyBackup_NotFound() throws Exception {
        // Given
        when(backupService.listBackups()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/admin/backup/verify/non_existent_backup")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.valid").value(false));

        verify(backupService, never()).verifyBackup(any());
    }

    @Test
    void testCleanupBackups_Success() throws Exception {
        // Given
        when(backupService.cleanupOldBackups()).thenReturn(3);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/backup/cleanup")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedCount").value(3))
                .andExpect(jsonPath("$.message").value("Cleaned up 3 old backup(s)"));

        verify(backupService, times(1)).cleanupOldBackups();
    }

    @Test
    void testCleanupBackups_NothingToCleanup() throws Exception {
        // Given
        when(backupService.cleanupOldBackups()).thenReturn(0);

        // When & Then
        mockMvc.perform(post("/api/v1/admin/backup/cleanup")
                        .header(AUTH_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedCount").value(0))
                .andExpect(jsonPath("$.message").value("No backups needed cleanup"));

        verify(backupService, times(1)).cleanupOldBackups();
    }
}
