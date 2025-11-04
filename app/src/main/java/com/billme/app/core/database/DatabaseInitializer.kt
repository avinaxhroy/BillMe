package com.billme.app.core.database

import android.content.Context
import android.util.Log
import com.billme.app.data.local.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database Initialization System
 * Handles app startup database setup with health checks and recovery
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val context: Context
) {
    companion object {
        private const val TAG = "DatabaseInitializer"
        private const val PREF_NAME = "database_init"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAST_VERSION = "last_db_version"
        private const val CURRENT_DB_VERSION = 8
    }
    
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * Initialize database on app startup
     * This should be called from Application.onCreate()
     */
    suspend fun initializeDatabase(): InitializationResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting database initialization")
        
        try {
            // Step 1: Check if this is first launch
            val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
            val lastVersion = prefs.getInt(KEY_LAST_VERSION, 0)
            
            // Step 2: Backup current database if it exists and version changed
            if (!isFirstLaunch && lastVersion < CURRENT_DB_VERSION) {
                Log.d(TAG, "Version upgrade detected ($lastVersion -> $CURRENT_DB_VERSION), creating backup")
                databaseManager.backupDatabase()
            }
            
            // Step 3: Get database instance (triggers migrations if needed)
            val database = databaseManager.getDatabase()
            
            // Step 4: Perform health check
            val health = databaseManager.checkDatabaseHealth()
            
            if (!health.isHealthy) {
                Log.w(TAG, "Database health check failed: ${health.message}")
                
                // Try to repair
                val repairResult = databaseManager.repairDatabase()
                if (!repairResult.isSuccess) {
                    return@withContext InitializationResult(
                        success = false,
                        isFirstLaunch = isFirstLaunch,
                        message = "Database repair failed: ${repairResult.exceptionOrNull()?.message}",
                        needsDataSeeding = false
                    )
                }
            }
            
            // Step 5: No demo data seeding for production
            // App starts clean - users add their own data
            val needsSeeding = false
            
            // Step 6: Update preferences
            prefs.edit()
                .putBoolean(KEY_FIRST_LAUNCH, false)
                .putInt(KEY_LAST_VERSION, CURRENT_DB_VERSION)
                .apply()
            
            // Step 7: Log final health status
            val finalHealth = databaseManager.checkDatabaseHealth()
            Log.i(TAG, "Database initialization complete - Health: ${finalHealth.message}")
            
            InitializationResult(
                success = true,
                isFirstLaunch = isFirstLaunch,
                message = "Database initialized successfully",
                needsDataSeeding = needsSeeding,
                productCount = finalHealth.productCount,
                databaseSize = databaseManager.getDatabaseSize()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Database initialization failed", e)
            InitializationResult(
                success = false,
                isFirstLaunch = false,
                message = "Initialization error: ${e.message}",
                error = e
            )
        }
    }
    
    /**
     * Reset database to fresh state
     */
    suspend fun resetDatabase(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Resetting database")
            
            // Backup first
            databaseManager.backupDatabase()
            
            // Clear all data
            val clearResult = databaseManager.clearAllData()
            if (!clearResult.isSuccess) {
                return@withContext Result.failure(
                    clearResult.exceptionOrNull() ?: Exception("Failed to clear data")
                )
            }
            
            // No demo data - production app resets to clean state
            
            // Mark as first launch
            prefs.edit()
                .putBoolean(KEY_FIRST_LAUNCH, true)
                .apply()
            
            Log.i(TAG, "Database reset complete")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Database reset failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if database is ready for use
     */
    suspend fun isDatabaseReady(): Boolean {
        return try {
            val health = databaseManager.checkDatabaseHealth()
            health.isHealthy
        } catch (e: Exception) {
            Log.e(TAG, "Database ready check failed", e)
            false
        }
    }
    
    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
        try {
            val health = databaseManager.checkDatabaseHealth()
            DatabaseStats(
                isHealthy = health.isHealthy,
                productCount = health.productCount,
                settingsCount = health.settingsCount,
                databaseSize = databaseManager.getDatabaseSize(),
                version = CURRENT_DB_VERSION
            )
        } catch (e: Exception) {
            DatabaseStats(
                isHealthy = false,
                error = e.message
            )
        }
    }
}

/**
 * Database initialization result
 */
data class InitializationResult(
    val success: Boolean,
    val isFirstLaunch: Boolean,
    val message: String,
    val needsDataSeeding: Boolean = false,
    val productCount: Int = 0,
    val databaseSize: Long = 0,
    val error: Throwable? = null
)

/**
 * Database statistics
 */
data class DatabaseStats(
    val isHealthy: Boolean,
    val productCount: Int = 0,
    val settingsCount: Int = 0,
    val databaseSize: Long = 0,
    val version: Int = 0,
    val error: String? = null
)
