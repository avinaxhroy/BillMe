package com.billme.app.core.performance

import android.util.Log
import com.billme.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for lazy loading optimization
 */
@Singleton
class LazyLoadingManager @Inject constructor() {
    
    companion object {
        private const val TAG = "LazyLoadingManager"
        private const val DEFAULT_PAGE_SIZE = 20
        private const val CACHE_EXPIRY_MS = 300000L // 5 minutes
    }
    
    private val isEnabled = AtomicBoolean(true)
    private val loadingCache = ConcurrentHashMap<String, Any>()
    private val loadingJobs = ConcurrentHashMap<String, Job>()
    
    /**
     * Initialize lazy loading manager
     */
    suspend fun initialize(config: LazyLoadingConfig) {
        Log.i(TAG, "Initializing lazy loading manager")
        isEnabled.set(config.enableLazyLoading)
    }
    
    /**
     * Load data with lazy loading strategy
     */
    suspend fun <T> lazyLoad(
        key: String,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        loader: suspend (page: Int, size: Int) -> List<T>
    ): Flow<List<T>> = flow {
        if (!isEnabled.get()) {
            // Load all data at once if lazy loading is disabled
            emit(loader(0, Int.MAX_VALUE))
            return@flow
        }
        
        var page = 0
        val allData = mutableListOf<T>()
        
        do {
            val data = loader(page, pageSize)
            allData.addAll(data)
            emit(allData.toList())
            page++
        } while (data.size == pageSize)
    }
    
    /**
     * Cache data for lazy loading
     */
    fun <T> cacheData(key: String, data: T) {
        if (isEnabled.get()) {
            loadingCache[key] = data as Any
        }
    }
    
    /**
     * Get cached data
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCachedData(key: String): T? {
        return loadingCache[key] as? T
    }
    
    /**
     * Clear cache
     */
    fun clearCache() {
        loadingCache.clear()
    }
    
    /**
     * Set enabled state
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
        if (!enabled) {
            clearCache()
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        loadingJobs.values.forEach { it.cancel() }
        loadingJobs.clear()
        clearCache()
        Log.i(TAG, "Lazy loading manager cleaned up")
    }
}