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
class DatabaseHealthMonitor @Inject constructor(
    private val database: BillMeDatabase,
    private val context: Context
) {
    companion object {
        private const val TAG = "DatabaseHealthMonitor"
    }

    suspend fun performHealthCheck(): HealthStatus = withContext(Dispatchers.IO) {
        try {
            val productCount = database.productDao().getProductCount()
            val settingsCount = database.appSettingDao().getSettingsCount()
            val dbFile = context.getDatabasePath("billme_database")
            val dbSize = if (dbFile.exists()) dbFile.length() else 0L

            HealthStatus(
                isHealthy = true,
                productCount = productCount,
                settingsCount = settingsCount,
                databaseSize = dbSize,
                message = "Database is healthy"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            HealthStatus(
                isHealthy = false,
                message = "Error: ${e.message}"
            )
        }
    }

    data class HealthStatus(
        val isHealthy: Boolean,
        val productCount: Int = 0,
        val settingsCount: Int = 0,
        val databaseSize: Long = 0,
        val message: String
    )
}
