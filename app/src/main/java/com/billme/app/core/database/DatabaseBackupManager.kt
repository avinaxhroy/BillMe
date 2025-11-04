package com.billme.app.core.database

import android.content.Context
import android.util.Log
import com.billme.app.data.local.BillMeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseBackupManager @Inject constructor(
    private val context: Context,
    private val database: BillMeDatabase
) {
    companion object {
        private const val TAG = "DatabaseBackupManager"
    }

    suspend fun createBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            database.close()
            
            val dbFile = context.getDatabasePath("billme_database")
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found"))
            }

            val backupDir = File(context.filesDir, "backups")
            backupDir.mkdirs()
            
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "backup_$timestamp.db")
            
            dbFile.copyTo(backupFile, overwrite = true)
            
            Log.d(TAG, "Backup created: ${backupFile.absolutePath}")
            Result.success(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure(e)
        }
    }

    suspend fun listBackups(): List<File> = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups")
        backupDir.listFiles()?.filter { it.name.startsWith("backup_") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
