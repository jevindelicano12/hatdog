package com.coffeeshop.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Backup manager for data files.
 * Creates automatic backups to prevent data loss.
 */
public final class BackupManager {
    
    private static final Logger logger = Logger.getLogger(BackupManager.class);
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int MAX_BACKUPS = 10;
    
    private BackupManager() {
        // Prevent instantiation
    }
    
    /**
     * Create a backup of all data files
     * @return true if backup was successful
     */
    public static boolean createBackup() {
        try {
            File dataDir = new File(Constants.DATA_DIR);
            if (!dataDir.exists()) {
                logger.warn("Data directory does not exist: %s", Constants.DATA_DIR);
                return false;
            }
            
            // Create backup directory with timestamp
            String timestamp = LocalDateTime.now().format(BACKUP_FORMAT);
            File backupDir = new File(Constants.BACKUP_DIR, "backup_" + timestamp);
            
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // Files to backup
            String[] filesToBackup = {
                "products.json",
                "inventory.json",
                "categories.json",
                "accounts.json",
                "addons.json",
                "special_requests.json",
                "pending_orders.txt",
                "receipts_database.txt",
                "orders_database.txt",
                "complaints_database.txt",
                "returns_database.txt",
                "cash_transactions.txt"
            };
            
            int backedUp = 0;
            for (String fileName : filesToBackup) {
                File sourceFile = new File(dataDir, fileName);
                if (sourceFile.exists()) {
                    File destFile = new File(backupDir, fileName);
                    try {
                        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        backedUp++;
                    } catch (IOException e) {
                        logger.warn("Failed to backup file: %s - %s", fileName, e.getMessage());
                    }
                }
            }
            
            logger.info("Backup created: %s (%d files)", backupDir.getName(), backedUp);
            
            // Cleanup old backups
            cleanupOldBackups();
            
            return backedUp > 0;
            
        } catch (Exception e) {
            logger.error("Failed to create backup", e);
            return false;
        }
    }
    
    /**
     * Create a backup of a specific file
     */
    public static boolean backupFile(String fileName) {
        try {
            File sourceFile = new File(Constants.DATA_DIR, fileName);
            if (!sourceFile.exists()) {
                return false;
            }
            
            File backupDir = new File(Constants.BACKUP_DIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            String timestamp = LocalDateTime.now().format(BACKUP_FORMAT);
            String backupFileName = fileName.replace(".", "_" + timestamp + ".");
            File destFile = new File(backupDir, backupFileName);
            
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("File backed up: %s -> %s", fileName, backupFileName);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to backup file: " + fileName, e);
            return false;
        }
    }
    
    /**
     * Remove old backups keeping only the most recent ones
     */
    public static void cleanupOldBackups() {
        try {
            File backupDir = new File(Constants.BACKUP_DIR);
            if (!backupDir.exists()) return;
            
            File[] backups = backupDir.listFiles(f -> f.isDirectory() && f.getName().startsWith("backup_"));
            if (backups == null || backups.length <= MAX_BACKUPS) return;
            
            // Sort by name (which includes timestamp) descending
            Arrays.sort(backups, Comparator.comparing(File::getName).reversed());
            
            // Delete oldest backups beyond MAX_BACKUPS
            for (int i = MAX_BACKUPS; i < backups.length; i++) {
                deleteDirectory(backups[i]);
                logger.info("Deleted old backup: %s", backups[i].getName());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to cleanup old backups: %s", e.getMessage());
        }
    }
    
    /**
     * Restore data from a backup directory
     * @param backupName the name of the backup folder (e.g., "backup_2025-01-01_12-00-00")
     * @return true if restore was successful
     */
    public static boolean restoreBackup(String backupName) {
        try {
            File backupDir = new File(Constants.BACKUP_DIR, backupName);
            if (!backupDir.exists() || !backupDir.isDirectory()) {
                logger.error("Backup not found: %s", backupName);
                return false;
            }
            
            File dataDir = new File(Constants.DATA_DIR);
            
            // First, create a backup of current data before restoring
            createBackup();
            
            // Restore each file
            File[] files = backupDir.listFiles();
            if (files == null) return false;
            
            int restored = 0;
            for (File file : files) {
                if (file.isFile()) {
                    File destFile = new File(dataDir, file.getName());
                    Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    restored++;
                }
            }
            
            logger.info("Backup restored: %s (%d files)", backupName, restored);
            return restored > 0;
            
        } catch (Exception e) {
            logger.error("Failed to restore backup: " + backupName, e);
            return false;
        }
    }
    
    /**
     * List available backups
     */
    public static String[] listBackups() {
        File backupDir = new File(Constants.BACKUP_DIR);
        if (!backupDir.exists()) return new String[0];
        
        File[] backups = backupDir.listFiles(f -> f.isDirectory() && f.getName().startsWith("backup_"));
        if (backups == null) return new String[0];
        
        // Sort by name descending (newest first)
        Arrays.sort(backups, Comparator.comparing(File::getName).reversed());
        
        String[] names = new String[backups.length];
        for (int i = 0; i < backups.length; i++) {
            names[i] = backups[i].getName();
        }
        return names;
    }
    
    /**
     * Delete a directory and all its contents
     */
    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
    
    /**
     * Get the size of a backup in bytes
     */
    public static long getBackupSize(String backupName) {
        File backupDir = new File(Constants.BACKUP_DIR, backupName);
        if (!backupDir.exists()) return 0;
        return calculateDirectorySize(backupDir);
    }
    
    private static long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                }
            }
        }
        return size;
    }
    
    /**
     * Format file size for display
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
