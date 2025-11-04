package com.billme.app.data.model

import java.time.LocalDateTime
import java.util.*

/**
 * Performance metrics and optimization data models
 */

/**
 * Database performance metrics
 */
data class DatabasePerformanceMetrics(
    val totalQueries: Long = 0,
    val cachedQueries: Long = 0,
    val averageQueryTime: Long = 0,
    val slowQueries: List<SlowQueryInfo> = emptyList(),
    val cacheHitRatio: Float = 0f,
    val cacheSize: Int = 0,
    val activeConnections: Int = 0,
    val maxConnections: Int = 0,
    val optimizedQueries: Long = 0,
    val missingIndices: List<IndexSuggestion> = emptyList(),
    val indexUsage: Map<String, IndexUsageStats> = emptyMap()
)

/**
 * Memory performance metrics
 */
data class MemoryPerformanceMetrics(
    val totalMemoryMB: Int,
    val usedMemoryMB: Int,
    val freeMemoryMB: Int,
    val memoryUsagePercentage: Float,
    val availableMemoryMB: Int,
    val memoryPressure: MemoryPressure = MemoryPressure.NORMAL,
    val cacheMetrics: CacheMetrics = CacheMetrics(),
    val resourceMetrics: ResourceMetrics = ResourceMetrics(),
    val gcMetrics: GCMetrics = GCMetrics(),
    val memoryLeaks: List<MemoryLeak> = emptyList(),
    val gcPressure: Float = 0f,
    val allocationRate: Float = 0f,
    val deallocationRate: Float = 0f
)

/**
 * Lazy loading performance metrics
 */
data class LazyLoadingPerformanceMetrics(
    val loadingEfficiency: Float = 0f,
    val cacheHitRatio: Float = 0f,
    val averageLoadTime: Long = 0,
    val prefetchHitRatio: Float = 0f,
    val memoryFootprint: Int = 0
)

/**
 * System performance metrics
 */
data class SystemPerformanceMetrics(
    val cpuUsage: Float = 0f,
    val availableMemoryMB: Int = 0,
    val totalMemoryMB: Int = 0,
    val batteryUsage: Float = 0f,
    val networkLatency: Long = 0,
    val diskIORate: Float = 0f
)

/**
 * Overall performance metrics container
 */
data class PerformanceMetrics(
    val timestamp: Long = System.currentTimeMillis(),
    val databaseMetrics: DatabasePerformanceMetrics? = null,
    val memoryMetrics: MemoryPerformanceMetrics? = null,
    val lazyLoadingMetrics: LazyLoadingPerformanceMetrics? = null,
    val systemMetrics: SystemPerformanceMetrics? = null,
    val overallScore: Float = 0f,
    val error: String? = null
)

/**
 * Query optimization results
 */
data class QueryOptimizationResult(
    val success: Boolean,
    val optimizedCount: Int = 0,
    val optimizationTime: Long = 0,
    val optimizations: List<QueryOptimizationInfo> = emptyList(),
    val slowQueriesFound: Int = 0,
    val performanceGain: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Query optimization information
 */
data class QueryOptimizationInfo(
    val queryId: String,
    val originalQuery: String,
    val optimizedQuery: String,
    val optimizationType: OptimizationType,
    val performanceGain: Double,
    val success: Boolean,
    val executionTimeBefore: Long = 0,
    val executionTimeAfter: Long = 0
)

/**
 * Index optimization result
 */
data class IndexOptimizationResult(
    val success: Boolean,
    val optimizationTime: Long = 0,
    val existingIndices: List<DatabaseIndex> = emptyList(),
    val unusedIndicesRemoved: Int = 0,
    val newIndicesCreated: Int = 0,
    val suggestedIndices: List<IndexSuggestion> = emptyList(),
    val performanceImpact: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Cache cleanup result
 */
data class CacheCleanupResult(
    val success: Boolean,
    val cleanupTime: Long = 0,
    val initialSizeMB: Int = 0,
    val finalSizeMB: Int = 0,
    val spaceSavedMB: Int = 0,
    val entriesRemoved: Int = 0,
    val initialEntryCount: Int = 0,
    val finalEntryCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Memory leak detection result
 */
data class MemoryLeakDetectionResult(
    val success: Boolean,
    val detectionTime: Long = 0,
    val leaksFound: Int = 0,
    val memoryLeaks: List<MemoryLeak> = emptyList(),
    val recommendations: List<LeakFixRecommendation> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Cache optimization result
 */
data class CacheOptimizationResult(
    val success: Boolean,
    val optimizationTime: Long = 0,
    val initialCacheSizeMB: Int = 0,
    val finalCacheSizeMB: Int = 0,
    val memorySavedMB: Int = 0,
    val entriesRemoved: Int = 0,
    val imageCacheOptimization: CacheOptimizationInfo = CacheOptimizationInfo(),
    val dataCacheOptimization: CacheOptimizationInfo = CacheOptimizationInfo(),
    val diskCacheOptimization: CacheOptimizationInfo = CacheOptimizationInfo(),
    val memoryCacheOptimization: CacheOptimizationInfo = CacheOptimizationInfo(),
    val expiredEntriesCleared: Int = 0,
    val compressionResult: CompressionResult = CompressionResult(),
    val errorMessage: String? = null
)

/**
 * Resource cleanup result
 */
data class ResourceCleanupResult(
    val success: Boolean,
    val cleanupTime: Long = 0,
    val initialResourceCount: Int = 0,
    val finalResourceCount: Int = 0,
    val resourcesCleaned: Int = 0,
    val bitmapsCleaned: Int = 0,
    val drawablesCleaned: Int = 0,
    val cursorsCleaned: Int = 0,
    val streamsCleaned: Int = 0,
    val connectionsCleaned: Int = 0,
    val threadsCleaned: Int = 0,
    val weakReferencesCleaned: Int = 0,
    val errorMessage: String? = null
)

/**
 * GC optimization result
 */
data class GCOptimizationResult(
    val success: Boolean,
    val optimizationTime: Long = 0,
    val memoryFreedMB: Int = 0,
    val preOptimizationMemory: MemoryInfo = MemoryInfo(),
    val postOptimizationMemory: MemoryInfo = MemoryInfo(),
    val strategyOptimization: GCStrategyOptimization = GCStrategyOptimization(),
    val gcTriggerResult: GCTriggerResult = GCTriggerResult(),
    val allocationOptimization: AllocationOptimization = AllocationOptimization(),
    val errorMessage: String? = null
)

/**
 * Memory cleanup result
 */
data class MemoryCleanupResult(
    val success: Boolean,
    val cleanupTime: Long = 0,
    val memoryFreedMB: Int = 0,
    val preCleanupMemory: MemoryInfo = MemoryInfo(),
    val postCleanupMemory: MemoryInfo = MemoryInfo(),
    val leakDetectionResult: MemoryLeakDetectionResult = MemoryLeakDetectionResult(false),
    val cacheOptimizationResult: CacheOptimizationResult = CacheOptimizationResult(false),
    val resourceCleanupResult: ResourceCleanupResult = ResourceCleanupResult(false),
    val gcOptimizationResult: GCOptimizationResult = GCOptimizationResult(false),
    val errorMessage: String? = null
)

/**
 * Lazy loading optimization result
 */
data class LazyLoadingOptimizationResult(
    val success: Boolean,
    val optimizationTime: Long = 0,
    val strategyOptimization: LoadingStrategyOptimization = LoadingStrategyOptimization(),
    val prefetchOptimization: PrefetchOptimization = PrefetchOptimization(),
    val loaderCleanup: LoaderCleanup = LoaderCleanup(),
    val errorMessage: String? = null
)

/**
 * Database optimization result
 */
data class DatabaseOptimizationResult(
    val success: Boolean,
    val optimizationTime: Long = 0,
    val queryOptimization: QueryOptimizationResult = QueryOptimizationResult(false),
    val indexOptimization: IndexOptimizationResult = IndexOptimizationResult(false),
    val cacheCleanup: CacheCleanupResult = CacheCleanupResult(false),
    val performanceAnalysis: DatabasePerformanceAnalysis = DatabasePerformanceAnalysis(
        DatabasePerformanceMetrics(), emptyList(), emptyList(), DatabaseHealth.UNKNOWN, 0L
    ),
    val errorMessage: String? = null
)

/**
 * Memory optimization result
 */
data class MemoryOptimizationResult(
    val success: Boolean,
    val optimizationTime: Long = 0,
    val leakDetection: MemoryLeakDetectionResult = MemoryLeakDetectionResult(false),
    val cacheOptimization: CacheOptimizationResult = CacheOptimizationResult(false),
    val resourceCleanup: ResourceCleanupResult = ResourceCleanupResult(false),
    val gcOptimization: GCOptimizationResult = GCOptimizationResult(false),
    val errorMessage: String? = null
)

// Supporting data classes

/**
 * Slow query information
 */
data class SlowQueryInfo(
    val queryId: String,
    val query: String,
    val averageExecutionTime: Long,
    val executionCount: Int,
    val lastExecutionTime: Long
)

/**
 * Database query result
 */
data class QueryResult(
    val success: Boolean,
    val data: List<Any> = emptyList(),
    val executionTime: Long = 0,
    val errorMessage: String? = null
)

/**
 * Database index information
 */
data class DatabaseIndex(
    val name: String,
    val tableName: String,
    val columnName: String,
    val isUnique: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Index suggestion
 */
data class IndexSuggestion(
    val indexName: String,
    val tableName: String,
    val columnName: String,
    val expectedImpact: Float,
    val reason: String,
    val isComposite: Boolean = false,
    val compositeColumns: List<String> = emptyList()
)

/**
 * Index usage statistics
 */
data class IndexUsageStats(
    val indexName: String,
    val usageCount: Long,
    val usageRatio: Float,
    val lastUsed: LocalDateTime?
)

/**
 * Query execution plan
 */
data class QueryExecutionPlan(
    val queryId: String,
    val query: String,
    val executionPlan: String,
    val estimatedCost: Float,
    val actualExecutionTime: Long
)

/**
 * Performance bottleneck
 */
data class PerformanceBottleneck(
    val type: BottleneckType,
    val severity: BottleneckSeverity,
    val description: String,
    val impact: Float,
    val affectedQueries: List<String> = emptyList()
)

/**
 * Bottleneck type enum
 */
enum class BottleneckType {
    SLOW_QUERY, MISSING_INDEX, CACHE_MISS, MEMORY_LEAK, CPU_INTENSIVE, IO_BOUND, LOCK_CONTENTION, OTHER
}

/**
 * Bottleneck severity
 */
enum class BottleneckSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Memory information
 */
data class MemoryInfo(
    val totalMemoryMB: Int = 0,
    val usedMemoryMB: Int = 0,
    val freeMemoryMB: Int = 0,
    val availableMemoryMB: Int = 0,
    val usagePercentage: Float = 0f,
    val lowMemory: Boolean = false,
    val threshold: Int = 0
)

/**
 * Memory leak information
 */
data class MemoryLeak(
    val objectType: String,
    val type: LeakType,
    val severity: LeakSeverity,
    val estimatedSizeMB: Int,
    val description: String,
    val stackTrace: String? = null,
    val detectedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Memory leak types
 */
enum class LeakType {
    CONTEXT_LEAK, BITMAP_LEAK, LISTENER_LEAK, THREAD_LEAK, COLLECTION_LEAK, VIEW_LEAK, OTHER
}

/**
 * Memory leak severity
 */
enum class LeakSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Cache metrics
 */
data class CacheMetrics(
    val hitRatio: Float = 0f,
    val missRatio: Float = 0f,
    val evictionCount: Long = 0,
    val requestCount: Long = 0,
    val totalSizeMB: Int = 0,
    val entryCount: Int = 0
)

/**
 * Resource metrics
 */
data class ResourceMetrics(
    val totalResources: Int = 0,
    val bitmapCount: Int = 0,
    val drawableCount: Int = 0,
    val cursorCount: Int = 0,
    val streamCount: Int = 0,
    val connectionCount: Int = 0,
    val threadCount: Int = 0,
    val weakReferenceCount: Int = 0
)

/**
 * GC metrics
 */
data class GCMetrics(
    val gcCount: Long = 0,
    val gcTime: Long = 0,
    val youngGenGCCount: Long = 0,
    val oldGenGCCount: Long = 0,
    val averageGCTime: Float = 0f
)

/**
 * Cache optimization information
 */
data class CacheOptimizationInfo(
    val optimizationType: String = "",
    val itemsRemoved: Int = 0,
    val spaceSavedMB: Int = 0,
    val success: Boolean = true
)

/**
 * Compression result
 */
data class CompressionResult(
    val originalSizeMB: Int = 0,
    val compressedSizeMB: Int = 0,
    val compressionRatio: Float = 0f,
    val success: Boolean = true
)

/**
 * GC strategy optimization
 */
data class GCStrategyOptimization(
    val strategyUsed: String = "",
    val improvement: Float = 0f,
    val success: Boolean = true
)

/**
 * GC trigger result
 */
data class GCTriggerResult(
    val triggered: Boolean = false,
    val memoryFreedMB: Int = 0,
    val gcTime: Long = 0
)

/**
 * Allocation optimization
 */
data class AllocationOptimization(
    val optimizationType: String = "",
    val reduction: Float = 0f,
    val success: Boolean = true
)

/**
 * Loading strategy optimization
 */
data class LoadingStrategyOptimization(
    val strategyType: String = "",
    val improvement: Float = 0f,
    val success: Boolean = true
)

/**
 * Prefetch optimization
 */
data class PrefetchOptimization(
    val hitRatioImprovement: Float = 0f,
    val itemsPrefetched: Int = 0,
    val success: Boolean = true
)

/**
 * Loader cleanup
 */
data class LoaderCleanup(
    val loadersRemoved: Int = 0,
    val memoryFreedMB: Int = 0,
    val success: Boolean = true
)

/**
 * Optimization configuration classes
 */
data class CacheOptimizationConfig(
    val maxSizeMB: Int = 50,
    val compressionEnabled: Boolean = true,
    val autoCleanup: Boolean = true
)

data class LeakDetectionConfig(
    val enabled: Boolean = true,
    val detectionIntervalMs: Long = 60000,
    val reportCriticalLeaks: Boolean = true
)

data class ResourceTrackingConfig(
    val trackBitmaps: Boolean = true,
    val trackCursors: Boolean = true,
    val trackStreams: Boolean = true,
    val cleanupThreshold: Int = 100
)

data class GCOptimizationConfig(
    val strategy: String = "adaptive",
    val aggressiveCleanup: Boolean = false,
    val triggerThreshold: Float = 0.85f
)

data class LazyLoadingOptimizationConfig(
    val prefetchEnabled: Boolean = true,
    val cacheSize: Int = 100,
    val preloadDistance: Int = 5
)

data class PerformanceMonitoringConfig(
    val monitoringEnabled: Boolean = true,
    val reportingIntervalMs: Long = 30000,
    val detailedMetrics: Boolean = true
)

/**
 * Optimization types
 */
enum class OptimizationType {
    QUERY_REWRITE, INDEX_HINT, PARAMETER_OPTIMIZATION, PLAN_OPTIMIZATION
}

/**
 * Memory pressure enum
 */
enum class MemoryPressure {
    NORMAL, MEDIUM, HIGH, CRITICAL
}

/**
 * Database health enum
 */
enum class DatabaseHealth {
    EXCELLENT, GOOD, FAIR, POOR, CRITICAL, UNKNOWN
}

/**
 * Recommendation priority
 */
enum class RecommendationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Stock status enum for supporting models
 */
enum class PerformanceStockStatus {
    IN_STOCK, LOW_STOCK, OUT_OF_STOCK, DISCONTINUED
}