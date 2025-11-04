package com.billme.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.billme.app.core.database.DatabaseInitializer
import com.billme.app.data.local.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BillMeApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder
    
    @Inject
    lateinit var databaseInitializer: DatabaseInitializer
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        // Set up global exception handler to log crashes
        setupExceptionHandler()
        
        Log.d("BillMeApplication", "Application onCreate started")

        // Initialize database asynchronously with proper error handling
        applicationScope.launch {
            try {
                Log.d("BillMeApplication", "Starting robust database initialization")
                val result = databaseInitializer.initializeDatabase()
                
                if (result.success) {
                    Log.i("BillMeApplication", "✅ Database initialized successfully")
                    Log.i("BillMeApplication", "   - First launch: ${result.isFirstLaunch}")
                    Log.i("BillMeApplication", "   - Products: ${result.productCount}")
                    Log.i("BillMeApplication", "   - Size: ${result.databaseSize / 1024}KB")
                } else {
                    Log.e("BillMeApplication", "❌ Database initialization failed: ${result.message}")
                    result.error?.let { 
                        Log.e("BillMeApplication", "Error details:", it)
                    }
                }
            } catch (e: Exception) {
                Log.e("BillMeApplication", "Critical error during database initialization", e)
            }
        }
        
        Log.d("BillMeApplication", "Application onCreate completed")
    }
    
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("BillMeApplication", "Uncaught exception in thread ${thread.name}", throwable)
            
            // Detect database-related crashes
            val isDatabaseError = throwable.message?.let { msg ->
                msg.contains("Room", ignoreCase = true) ||
                msg.contains("Migration", ignoreCase = true) ||
                msg.contains("integrity", ignoreCase = true) ||
                msg.contains("database", ignoreCase = true) ||
                msg.contains("SQLite", ignoreCase = true)
            } ?: false
            
            if (isDatabaseError) {
                Log.e("BillMeApplication", """
                    ╔═══════════════════════════════════════════════════════════╗
                    ║ DATABASE ERROR DETECTED                                    ║
                    ║ The database recovery system should handle this            ║
                    ║ If app keeps crashing, go to Settings > Database > Reset  ║
                    ╚═══════════════════════════════════════════════════════════╝
                """.trimIndent())
            }
            
            // Call the default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

