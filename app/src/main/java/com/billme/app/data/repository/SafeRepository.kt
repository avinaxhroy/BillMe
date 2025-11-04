package com.billme.app.data.repository

import android.util.Log
import com.billme.app.data.local.DatabaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Safe Repository Base
 * Provides error handling and recovery for all database operations
 */
@Singleton
class SafeRepository @Inject constructor(
    private val databaseManager: DatabaseManager
) {
    companion object {
        private const val TAG = "SafeRepository"
    }
    
    /**
     * Execute a database operation safely with error handling
     */
    suspend fun <T> safeDbCall(
        operation: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            val result = block()
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Database operation failed: $operation", e)
            handleDatabaseError(e)
            Result.failure(e)
        }
    }
    
    /**
     * Execute a Flow operation safely with error handling
     */
    fun <T> safeFlowCall(
        operation: String,
        block: () -> Flow<T>
    ): Flow<Result<T>> = flow {
        block()
            .onStart {
                Log.d(TAG, "Flow operation started: $operation")
            }
            .catch { e ->
                Log.e(TAG, "Flow operation failed: $operation", e)
                handleDatabaseError(e)
                emit(Result.failure(e))
            }
            .collect { data ->
                emit(Result.success(data))
            }
    }
    
    /**
     * Handle database errors with recovery attempts
     */
    private suspend fun handleDatabaseError(error: Throwable) {
        when {
            isDatabaseCorrupted(error) -> {
                Log.w(TAG, "Database corruption detected, attempting repair")
                val repairResult = databaseManager.repairDatabase()
                if (repairResult.isSuccess) {
                    Log.i(TAG, "Database repaired successfully")
                } else {
                    Log.e(TAG, "Database repair failed")
                }
            }
            isDiskFull(error) -> {
                Log.e(TAG, "Disk full error detected")
                // Could trigger cleanup operations here
            }
            isConnectionError(error) -> {
                Log.e(TAG, "Database connection error")
                // Could trigger reconnection here
            }
            else -> {
                Log.e(TAG, "General database error: ${error.message}")
            }
        }
    }
    
    /**
     * Check if error indicates database corruption
     */
    private fun isDatabaseCorrupted(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("corrupt") ||
               message.contains("malformed") ||
               message.contains("sqlite_corrupt") ||
               message.contains("database disk image is malformed")
    }
    
    /**
     * Check if error indicates disk full
     */
    private fun isDiskFull(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("disk") ||
               message.contains("space") ||
               message.contains("sqlite_full")
    }
    
    /**
     * Check if error indicates connection issue
     */
    private fun isConnectionError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("connection") ||
               message.contains("closed") ||
               message.contains("not open")
    }
    
    /**
     * Get database health status
     */
    suspend fun getDatabaseHealth() = databaseManager.checkDatabaseHealth()
    
    /**
     * Backup database
     */
    suspend fun backupDatabase() = databaseManager.backupDatabase()
    
    /**
     * Restore database from backup
     */
    suspend fun restoreDatabase() = databaseManager.restoreFromBackup()
}

/**
 * Extension function to safely collect Flow with error handling
 */
fun <T> Flow<T>.safeCollect(): Flow<Result<T>> = flow {
    this@safeCollect
        .catch { e ->
            Log.e("SafeFlow", "Flow error", e)
            emit(Result.failure(e))
        }
        .collect { data ->
            emit(Result.success(data))
        }
}
