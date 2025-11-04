package com.billme.app.core.performance

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.billme.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring service
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MONITORING_INTERVAL_MS = 5000L // 5 seconds
    }
    
    private val isMonitoring = AtomicBoolean(false)
    private var monitoringJob: Job? = null
    
    private val _performanceMetrics = MutableSharedFlow<PerformanceMetrics>()
    val performanceMetrics: SharedFlow<PerformanceMetrics> = _performanceMetrics.asSharedFlow()
    
    private val activityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }
    
    /**
     * Initialize performance monitor
     */
    suspend fun initialize(config: PMMonitoringConfig) {
        Log.i(TAG, "Initializing performance monitor")
    }
    
    /**
     * Start monitoring performance
     */
    fun startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            Log.i(TAG, "Starting performance monitoring")
            
            monitoringJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isMonitoring.get()) {
                    try {
                        val metrics = collectMetrics()
                        _performanceMetrics.emit(metrics)
                        delay(MONITORING_INTERVAL_MS)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting performance metrics", e)
                    }
                }
            }
        }
    }
    
    /**
     * Stop monitoring performance
     */
    fun stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            Log.i(TAG, "Stopping performance monitoring")
            monitoringJob?.cancel()
            monitoringJob = null
        }
    }
    
    /**
     * Collect current performance metrics
     */
    private suspend fun collectMetrics(): PerformanceMetrics {
        val runtime = Runtime.getRuntime()
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return PerformanceMetrics(
            timestamp = System.currentTimeMillis(),
            memoryUsage = MemoryUsage(
                totalMemory = runtime.totalMemory(),
                freeMemory = runtime.freeMemory(),
                maxMemory = runtime.maxMemory(),
                usedMemory = runtime.totalMemory() - runtime.freeMemory()
            ),
            systemMemory = SystemMemory(
                availableMemory = memoryInfo.availMem,
                totalMemory = memoryInfo.totalMem,
                threshold = memoryInfo.threshold,
                lowMemory = memoryInfo.lowMemory
            ),
            cpuUsage = getCpuUsage(),
            batteryLevel = getBatteryLevel(),
            networkLatency = getNetworkLatency()
        )
    }
    
    /**
     * Get CPU usage percentage
     */
    private fun getCpuUsage(): Double {
        // This is a simplified CPU usage calculation
        return 0.0 // Would implement actual CPU monitoring
    }
    
    /**
     * Get battery level
     */
    private fun getBatteryLevel(): Int {
        // Would implement battery level monitoring
        return 100
    }
    
    /**
     * Get network latency
     */
    private fun getNetworkLatency(): Long {
        // Would implement network latency monitoring
        return 0L
    }
}

/**
 * Configuration for performance monitoring
 */
data class PMMonitoringConfig(
    val enabled: Boolean = true,
    val intervalMs: Long = 5000L,
    val collectCpuUsage: Boolean = true,
    val collectMemoryUsage: Boolean = true,
    val collectBatteryInfo: Boolean = true,
    val collectNetworkInfo: Boolean = true
)

/**
 * Performance metrics data classes
 */
data class PerformanceMetrics(
    val timestamp: Long,
    val memoryUsage: MemoryUsage,
    val systemMemory: SystemMemory,
    val cpuUsage: Double,
    val batteryLevel: Int,
    val networkLatency: Long
)

data class MemoryUsage(
    val totalMemory: Long,
    val freeMemory: Long,
    val maxMemory: Long,
    val usedMemory: Long
) {
    val usagePercentage: Double
        get() = (usedMemory.toDouble() / maxMemory.toDouble()) * 100.0
}

data class SystemMemory(
    val availableMemory: Long,
    val totalMemory: Long,
    val threshold: Long,
    val lowMemory: Boolean
)