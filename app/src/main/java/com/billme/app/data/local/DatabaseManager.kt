package com.billme.app.data.local

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Robust Database Manager
 * Handles database initialization, migration, backup, and recovery
 */
@Singleton
class DatabaseManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "DatabaseManager"
        private const val DATABASE_NAME = "billme_database.db"
        private const val BACKUP_SUFFIX = ".backup"
        
        @Volatile
        private var INSTANCE: BillMeDatabase? = null
        
        // Flag to prevent destructive operations after restore
        @Volatile
        private var justRestored = false
    }
    
    // Callback DISABLED - PRAGMA commands in onOpen() may cause SQLITE_OK errors
    // WAL mode is set by Room automatically with setJournalMode()
    private val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d(TAG, "Database created successfully")
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d(TAG, "Database opened successfully")
            // REMOVED: PRAGMA commands that may conflict with Room
            // db.execSQL("PRAGMA journal_mode=WAL")
            // db.execSQL("PRAGMA synchronous=NORMAL")
        }
        
        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            Log.w(TAG, "Destructive migration performed - all data lost")
        }
    }
    
    /**
     * Get database instance with robust error handling
     */
    fun getDatabase(): BillMeDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase().also { INSTANCE = it }
        }
    }
    
    /**
     * Clear database instance - used after restore to force reconnection
     */
    fun clearInstance() {
        synchronized(this) {
            try {
                INSTANCE?.close()
                Log.d(TAG, "Database instance closed and cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing database instance", e)
            } finally {
                INSTANCE = null
                justRestored = true // Mark that we just restored
            }
        }
    }
    
    /**
     * Build database with all safety features
     */
    private fun buildDatabase(): BillMeDatabase {
        return try {
            Log.d(TAG, "Building database, justRestored=$justRestored")
            
            Room.databaseBuilder(
                context.applicationContext,
                BillMeDatabase::class.java,
                DATABASE_NAME
            )
            .addCallback(callback)
            .addMigrations(
                BillMeDatabase.MIGRATION_1_2,
                BillMeDatabase.MIGRATION_2_3,
                BillMeDatabase.MIGRATION_3_4,
                BillMeDatabase.MIGRATION_4_5,
                BillMeDatabase.MIGRATION_5_6,
                BillMeDatabase.MIGRATION_6_7,
                BillMeDatabase.MIGRATION_7_8,
                BillMeDatabase.MIGRATION_8_9,
                BillMeDatabase.MIGRATION_9_10,
                BillMeDatabase.MIGRATION_10_11
            )
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
            .also {
                justRestored = false // Reset flag after successful build
                Log.d(TAG, "Database built successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building database", e)
            
            // DO NOT create fresh database if we just restored
            if (justRestored) {
                Log.e(TAG, "Database error after restore - NOT deleting data!")
                justRestored = false
                throw e // Re-throw to prevent data loss
            }
            
            // Try to recover from backup
            tryRecoverFromBackup()
            // If recovery fails, create fresh database
            createFreshDatabase()
        }
    }
    
    /**
     * Create a fresh database (last resort)
     */
    private fun createFreshDatabase(): BillMeDatabase {
        Log.w(TAG, "Creating fresh database")
        // Delete corrupted database
        deleteDatabaseFiles()
        
        return Room.databaseBuilder(
            context.applicationContext,
            BillMeDatabase::class.java,
            DATABASE_NAME
        )
        .addCallback(callback)
        .fallbackToDestructiveMigration()
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()
    }
    
    /**
     * Backup database
     */
    suspend fun backupDatabase(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found"))
            }
            
            val backupFile = File(dbFile.parent, "$DATABASE_NAME$BACKUP_SUFFIX")
            dbFile.copyTo(backupFile, overwrite = true)
            
            Log.d(TAG, "Database backed up to: ${backupFile.absolutePath}")
            Result.success(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restore database from backup
     */
    suspend fun restoreFromBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val backupFile = File(dbFile.parent, "$DATABASE_NAME$BACKUP_SUFFIX")
            
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }
            
            // Close database before restore
            INSTANCE?.close()
            INSTANCE = null
            
            // Restore from backup
            backupFile.copyTo(dbFile, overwrite = true)
            
            Log.d(TAG, "Database restored from backup")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from backup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Try to recover from backup (internal use)
     */
    private fun tryRecoverFromBackup() {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val backupFile = File(dbFile.parent, "$DATABASE_NAME$BACKUP_SUFFIX")
            
            if (backupFile.exists()) {
                Log.d(TAG, "Attempting to recover from backup")
                backupFile.copyTo(dbFile, overwrite = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recover from backup", e)
        }
    }
    
    /**
     * Check database health
     */
    suspend fun checkDatabaseHealth(): DatabaseHealth = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase()
            
            // Try to query each table
            val productCount = db.productDao().getProductCount()
            val settingsCount = db.appSettingDao().getSettingsCount()
            
            DatabaseHealth(
                isHealthy = true,
                canRead = true,
                canWrite = true,
                productCount = productCount,
                settingsCount = settingsCount,
                message = "Database is healthy"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Database health check failed", e)
            DatabaseHealth(
                isHealthy = false,
                canRead = false,
                canWrite = false,
                message = "Database error: ${e.message}",
                error = e
            )
        }
    }
    
    /**
     * Repair database
     */
    suspend fun repairDatabase(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting database repair")
            
            // Step 1: Try backup first
            backupDatabase()
            
            // Step 2: Close current instance
            INSTANCE?.close()
            INSTANCE = null
            
            // Step 3: Try to open and verify
            val db = getDatabase()
            val health = checkDatabaseHealth()
            
            if (health.isHealthy) {
                Log.d(TAG, "Database repaired successfully")
                Result.success(true)
            } else {
                // Step 4: If still broken, restore from backup
                restoreFromBackup()
                val newHealth = checkDatabaseHealth()
                Result.success(newHealth.isHealthy)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database repair failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clear all data (for testing/reset)
     */
    suspend fun clearAllData(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase()
            db.clearAllTables()
            Log.d(TAG, "All data cleared")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete database files
     */
    private fun deleteDatabaseFiles() {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val walFile = File(dbFile.parent, "$DATABASE_NAME-wal")
            val shmFile = File(dbFile.parent, "$DATABASE_NAME-shm")
            
            dbFile.delete()
            walFile.delete()
            shmFile.delete()
            
            Log.d(TAG, "Database files deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting database files", e)
        }
    }
    
    /**
     * Get database size
     */
    fun getDatabaseSize(): Long {
        return try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            dbFile.length()
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * Database health status
 */
data class DatabaseHealth(
    val isHealthy: Boolean,
    val canRead: Boolean,
    val canWrite: Boolean,
    val productCount: Int = 0,
    val settingsCount: Int = 0,
    val message: String,
    val error: Throwable? = null
)
