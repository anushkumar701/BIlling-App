package com.fruitbilling.app.data.backup

import android.content.Context
import android.util.Log
import com.fruitbilling.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Offline-first Daily Backup Manager (§17).
 * Creates automatic daily snapshots of the Room database.
 * Works silently, never interrupts billing, and safely supports restore.
 */
object BackupManager {
    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "fruit_backup_prefs"
    private const val KEY_LAST_BACKUP = "key_last_backup_time"
    private const val BACKUP_DIR = "database_backups"
    private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

    suspend fun performDailyBackupIfDue(context: Context, database: AppDatabase) {
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastBackup = prefs.getLong(KEY_LAST_BACKUP, 0L)
                val now = System.currentTimeMillis()

                if (now - lastBackup >= ONE_DAY_MILLIS) {
                    val success = createBackupSnapshot(context)
                    if (success) {
                        prefs.edit().putLong(KEY_LAST_BACKUP, now).apply()
                        Log.d(TAG, "Daily backup snapshot completed successfully at $now")
                    }
                }
                Unit
            } catch (e: Exception) {
                // Backup failures must NEVER interrupt normal app usage (§17)
                Log.e(TAG, "Silent backup error (non-fatal): ${e.message}")
            }
        }
    }

    fun createBackupSnapshot(context: Context): Boolean {
        return try {
            val dbFile = context.getDatabasePath("fruit_billing_database")
            if (!dbFile.exists()) return false

            val backupFolder = File(context.filesDir, BACKUP_DIR)
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }

            val targetFile = File(backupFolder, "fruit_billing_backup.db")
            copyFile(dbFile, targetFile)

            // Also copy WAL and SHM files if present
            val walFile = File(dbFile.path + "-wal")
            if (walFile.exists()) {
                copyFile(walFile, File(backupFolder, "fruit_billing_backup.db-wal"))
            }
            val shmFile = File(dbFile.path + "-shm")
            if (shmFile.exists()) {
                copyFile(shmFile, File(backupFolder, "fruit_billing_backup.db-shm"))
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup snapshot: ${e.message}")
            false
        }
    }

    fun restoreBackupSnapshot(context: Context): Boolean {
        return try {
            val backupFolder = File(context.filesDir, BACKUP_DIR)
            val backupFile = File(backupFolder, "fruit_billing_backup.db")
            if (!backupFile.exists()) return false

            val dbFile = context.getDatabasePath("fruit_billing_database")
            copyFile(backupFile, dbFile)

            val backupWal = File(backupFolder, "fruit_billing_backup.db-wal")
            if (backupWal.exists()) {
                copyFile(backupWal, File(dbFile.path + "-wal"))
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore backup snapshot: ${e.message}")
            false
        }
    }

    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }
}
