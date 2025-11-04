package com.billme.app.data.model

import java.time.LocalDateTime
import java.util.*

/**
 * Price suggestion data model with intelligent recommendations
 */
data class PriceSuggestion(
    val suggestionId: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val category: String,
    val brand: String,
    val currentCostPrice: Double,
    val currentSellingPrice: Double?,
    val suggestedPrice: Double,
    val suggestedPriceRange: PriceRange,
    val confidence: Float,
    val reasoning: PricingReasoning,
    val factors: List<PricingFactor>,
    val marketData: MarketData?,
    val competitorData: List<CompetitorPrice>,
    val profitMargin: Double,
    val suggestedProfitMargin: Double,
    val demandScore: Float,
    val validityPeriod: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val status: SuggestionStatus = SuggestionStatus.ACTIVE
)

/**
 * Price range for suggestions
 */
data class PriceRange(
    val minPrice: Double,
    val maxPrice: Double,
    val optimalPrice: Double,
    val conservativePrice: Double,
    val aggressivePrice: Double
)

/**
 * Pricing reasoning and explanation
 */
data class PricingReasoning(
    val primaryReason: String,
    val detailedExplanation: String,
    val marketTrend: MarketTrend,
    val seasonalFactor: Float,
    val demandForecast: DemandForecast,
    val riskLevel: RiskLevel
)

/**
 * Factors affecting price suggestions
 */
data class PricingFactor(
    val factorType: PricingFactorType,
    val weight: Float,
    val impact: Float, // Positive or negative impact on price
    val description: String,
    val confidence: Float
)

/**
 * Types of pricing factors
 */
enum class PricingFactorType {
    MARKET_DEMAND,
    COMPETITION,
    SEASONALITY,
    HISTORICAL_SALES,
    STOCK_LEVEL,
    BRAND_PREMIUM,
    PRODUCT_AGE,
    COST_FLUCTUATION,
    PROMOTIONAL_ACTIVITY,
    CUSTOMER_PREFERENCE,
    PROFIT_TARGET,
    CLEARANCE_NEED
}

/**
 * Market data for pricing decisions
 */
data class MarketData(
    val averageMarketPrice: Double,
    val marketPriceRange: PriceRange,
    val marketTrend: MarketTrend,
    val dataSource: String,
    val lastUpdated: LocalDateTime,
    val reliability: Float,
    val marketShare: Float?,
    val regionalVariation: List<RegionalPrice>
)

/**
 * Competitor pricing information
 */
data class CompetitorPrice(
    val competitorName: String,
    val price: Double,
    val availability: StockStatus,
    val lastSeen: LocalDateTime,
    val source: String,
    val reliability: Float,
    val distance: Double?, // Physical distance in km
    val marketReputation: Float
)

/**
 * Regional pricing data
 */
data class RegionalPrice(
    val region: String,
    val averagePrice: Double,
    val priceRange: PriceRange,
    val demandLevel: DemandLevel
)

/**
 * Market trend indicators
 */
enum class MarketTrend {
    RISING, STABLE, DECLINING, VOLATILE, SEASONAL_HIGH, SEASONAL_LOW
}

/**
 * Demand forecast data
 */
data class DemandForecast(
    val predictedDemand: DemandLevel,
    val confidence: Float,
    val forecastPeriod: Int, // Days
    val seasonalPattern: SeasonalPattern?,
    val trendDirection: TrendDirectionAutomation
)

/**
 * Demand levels
 */
enum class DemandLevel {
    VERY_LOW, LOW, MODERATE, HIGH, VERY_HIGH, PEAK
}

/**
 * Seasonal patterns
 */
data class SeasonalPattern(
    val patternType: SeasonalType,
    val peakMonths: List<Int>,
    val lowMonths: List<Int>,
    val variationPercentage: Float
)

/**
 * Seasonal pattern types
 */
enum class SeasonalType {
    FESTIVE, BACK_TO_SCHOOL, WEDDING_SEASON, NEW_YEAR, SUMMER, MONSOON, WINTER
}

/**
 * Trend directions
 */
enum class TrendDirectionAutomation {
    UPWARD, DOWNWARD, STABLE, FLUCTUATING
}

/**
 * Risk levels for pricing decisions
 */
enum class RiskLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

/**
 * Suggestion status
 */
enum class SuggestionStatus {
    ACTIVE, APPLIED, REJECTED, EXPIRED, UNDER_REVIEW
}

/**
 * IMEI scan result with validation and device information
 */
data class IMEIScanResult(
    val scanId: String = UUID.randomUUID().toString(),
    val imei1: String,
    val imei2: String?,
    val scanMethod: ScanMethod,
    val validationResult: IMEIValidation,
    val deviceInfo: DeviceInfo?,
    val duplicateCheck: ImeiDuplicateCheckResult,
    val qualityScore: Float,
    val scanTime: LocalDateTime = LocalDateTime.now(),
    val location: String?,
    val scannedBy: String?,
    val notes: String?
)

/**
 * IMEI scanning methods
 */
enum class ScanMethod {
    CAMERA_OCR,
    BARCODE_SCANNER,
    MANUAL_ENTRY,
    NFC_SCAN,
    BULK_IMPORT
}

/**
 * IMEI validation result
 */
data class IMEIValidation(
    val isValid: Boolean,
    val validationChecks: List<ValidationCheck>,
    val errors: List<ValidationError>,
    val warnings: List<ValidationWarning>,
    val checkDigitValid: Boolean,
    val formatValid: Boolean,
    val lengthValid: Boolean,
    val blacklistStatus: BlacklistStatus
)

/**
 * Validation checks performed
 */
data class ValidationCheck(
    val checkType: ValidationCheckType,
    val passed: Boolean,
    val details: String
)

/**
 * Types of IMEI validation checks
 */
enum class ValidationCheckType {
    LUHN_ALGORITHM,
    FORMAT_CHECK,
    LENGTH_CHECK,
    BLACKLIST_CHECK,
    DUPLICATE_CHECK,
    MANUFACTURER_CHECK,
    TAC_VALIDATION
}

/**
 * Validation errors
 */
data class ValidationError(
    val errorType: ValidationErrorType,
    val message: String,
    val severity: ErrorSeverity
)

/**
 * Validation error types
 */
enum class ValidationErrorType {
    INVALID_FORMAT,
    INVALID_CHECK_DIGIT,
    BLACKLISTED_DEVICE,
    DUPLICATE_IMEI,
    UNKNOWN_MANUFACTURER,
    INVALID_TAC
}

/**
 * Validation warnings
 */
data class ValidationWarning(
    val warningType: ValidationWarningType,
    val message: String
)

/**
 * Validation warning types
 */
enum class ValidationWarningType {
    OLDER_DEVICE,
    UNCOMMON_MANUFACTURER,
    REGIONAL_VARIANT,
    LIMITED_INFO
}

/**
 * Error severity levels
 */
enum class ErrorSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

/**
 * Blacklist status
 */
enum class BlacklistStatus {
    CLEAN, BLACKLISTED, GREY_LISTED, UNKNOWN
}

/**
 * Device information extracted from IMEI
 */
data class DeviceInfo(
    val tac: String, // Type Allocation Code
    val manufacturer: String,
    val brandName: String,
    val modelName: String,
    val deviceType: DeviceType,
    val operatingSystem: String?,
    val releaseDate: LocalDateTime?,
    val marketName: String?,
    val specifications: DeviceSpecs?,
    val marketValue: MarketValue?,
    val colorVariants: List<String>,
    val storageVariants: List<String>,
    val regionInfo: RegionInfo?
)

/**
 * Device types
 */
enum class DeviceType {
    SMARTPHONE, TABLET, FEATURE_PHONE, SMARTWATCH, MODEM, IOT_DEVICE, UNKNOWN
}

/**
 * Device specifications
 */
data class DeviceSpecs(
    val displaySize: String?,
    val resolution: String?,
    val processor: String?,
    val ram: String?,
    val storage: String?,
    val camera: String?,
    val battery: String?,
    val connectivity: List<String>,
    val sensors: List<String>
)

/**
 * Market value information
 */
data class MarketValue(
    val currentValue: Double?,
    val launchPrice: Double?,
    val depreciation: Float?,
    val resaleValue: Double?,
    val demandRating: Float,
    val availabilityStatus: AvailabilityStatus
)

/**
 * Availability status
 */
enum class AvailabilityStatus {
    AVAILABLE, LIMITED, DISCONTINUED, PRE_ORDER, COMING_SOON
}

/**
 * Region information
 */
data class RegionInfo(
    val supportedRegions: List<String>,
    val primaryMarket: String,
    val certifications: List<String>,
    val networkSupport: List<String>
)

/**
 * Duplicate check result
 */
data class DuplicateCheckResult(
    val hasDuplicates: Boolean,
    val duplicateEntries: List<DuplicateEntry>,
    val riskScore: Float,
    val recommendation: DuplicateRecommendation
)

/**
 * Duplicate entry information
 */
data class DuplicateEntry(
    val entryId: String,
    val imei: String,
    val foundInDatabase: String,
    val lastSeen: LocalDateTime,
    val status: String,
    val owner: String?,
    val notes: String?
)

/**
 * Duplicate handling recommendations
 */
enum class DuplicateRecommendation {
    ACCEPT, INVESTIGATE, REJECT, FLAG_FOR_REVIEW
}

/**
 * Stock notification data
 */
data class StockNotification(
    val notificationId: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val currentStock: Int,
    val threshold: Int,
    val notificationType: NotificationType,
    val priority: NotificationPriority,
    val message: String,
    val suggestedAction: String?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val acknowledged: Boolean = false,
    val acknowledgedAt: LocalDateTime? = null,
    val acknowledgedBy: String? = null
)

/**
 * Notification types
 */
enum class NotificationType {
    LOW_STOCK, OUT_OF_STOCK, RESTOCK_REMINDER, OVERSTOCK_WARNING, EXPIRY_ALERT
}

/**
 * Notification priority levels
 */
enum class NotificationPriority {
    LOW, MEDIUM, HIGH, URGENT
}

/**
 * Automation rule configuration
 */
data class AutomationRule(
    val ruleId: String = UUID.randomUUID().toString(),
    val ruleName: String,
    val description: String,
    val enabled: Boolean = true,
    val triggers: List<AutomationTrigger>,
    val conditions: List<AutomationCondition>,
    val actions: List<AutomationAction>,
    val schedule: AutomationSchedule?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastExecuted: LocalDateTime? = null,
    val executionCount: Int = 0
)

/**
 * IMEI scan result for smart scanner
 */
data class ImeiScanResult(
    val sessionId: String,
    val scannedImeis: List<ScannedImei>,
    val scanStatus: ScanStatus,
    val errorMessage: String? = null,
    val totalScanTime: Long
)

/**
 * Scanned IMEI information
 */
data class ScannedImei(
    val imeiNumber: String,
    val confidence: Float,
    val validation: ImeiValidation,
    val deviceInfo: DeviceInfo? = null,
    val duplicateCheck: ImeiDuplicateCheckResult? = null,
    val scanTimestamp: Long
)

/**
 * IMEI validation result
 */
data class ImeiValidation(
    val isValid: Boolean,
    val validationType: ValidationTypeSmartAutomation,
    val tacInfo: TacInfo? = null,
    val fraudCheck: FraudCheckResult? = null,
    val confidence: Float,
    val errorMessage: String? = null
)

/**
 * Duplicate check result for IMEI scanning
 */
data class ImeiDuplicateCheckResult(
    val isDuplicate: Boolean,
    val confidence: Float,
    val duplicateCount: Int = 0,
    val existingEntries: List<String> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Scan status
 */
enum class ScanStatus {
    ACTIVE, COMPLETED, INVALID, FAILED, PARTIAL, CANCELLED
}

/**
 * Validation types
 */
enum class ValidationTypeSmartAutomation {
    FORMAT_CHECK, LUHN_CHECK, COMPREHENSIVE
}

/**
 * TAC information
 */
data class TacInfo(
    val tacCode: String,
    val manufacturer: String,
    val model: String,
    val approvalDate: String?,
    val countryCode: String?
)

/**
 * Fraud check result
 */
data class FraudCheckResult(
    val isFraudulent: Boolean,
    val riskLevel: RiskLevel,
    val reasons: List<String> = emptyList(),
    val confidence: Float = 0f
)

/**
 * Database performance analysis
 */
data class DatabasePerformanceAnalysis(
    val metrics: DatabasePerformanceMetrics,
    val bottlenecks: List<PerformanceBottleneck>,
    val recommendations: List<PerformanceRecommendation>,
    val overallHealth: DatabaseHealth,
    val analysisTimestamp: Long,
    val error: String? = null
)

/**
 * Leak fix recommendation
 */
data class LeakFixRecommendation(
    val leakType: LeakType,
    val priority: RecommendationPriority,
    val description: String,
    val affectedObjectCount: Int,
    val estimatedMemorySavingMB: Int
)

/**
 * Threshold settings for stock notifications
 */
data class ThresholdSettings(
    val lowStockThreshold: Int,
    val criticalStockThreshold: Int,
    val outOfStockThreshold: Int,
    val reorderPoint: Int,
    val maxStockLevel: Int,
    val safetyStock: Int,
    val dynamicThresholds: Boolean,
    val seasonalAdjustments: List<SeasonalAdjustment>
)

/**
 * Seasonal adjustments for thresholds
 */
data class SeasonalAdjustment(
    val season: SeasonalType,
    val adjustmentFactor: Float,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)

/**
 * Notification levels
 */
enum class NotificationLevel {
    INFO, WARNING, URGENT, CRITICAL, EMERGENCY
}

/**
 * Supplier information
 */
data class SupplierInfo(
    val supplierId: String,
    val supplierName: String,
    val contactInfo: ContactInfo,
    val leadTime: Int, // Days
    val minimumOrderQuantity: Int,
    val unitPrice: Double,
    val reliability: Float,
    val lastOrderDate: LocalDateTime?,
    val paymentTerms: String,
    val deliveryTerms: String
)

/**
 * Contact information
 */
data class ContactInfo(
    val phone: String?,
    val email: String?,
    val address: String?,
    val contactPerson: String?,
    val alternateContact: String?
)

/**
 * Automation rules for stock management
 */
data class StockAutomationRule(
    val ruleId: String = UUID.randomUUID().toString(),
    val ruleName: String,
    val ruleType: AutomationRuleType,
    val trigger: AutomationTrigger,
    val conditions: List<AutomationCondition>,
    val actions: List<AutomationAction>,
    val priority: Int,
    val isActive: Boolean,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastExecuted: LocalDateTime?,
    val executionCount: Int = 0
)

/**
 * Types of automation rules
 */
enum class AutomationRuleType {
    STOCK_REORDER,
    PRICE_ADJUSTMENT,
    NOTIFICATION_ESCALATION,
    SUPPLIER_SELECTION,
    DEMAND_FORECASTING,
    PROMOTIONAL_TRIGGER
}

/**
 * Automation triggers
 */
data class AutomationTrigger(
    val triggerType: TriggerType,
    val parameters: Map<String, Any>,
    val schedule: TriggerSchedule?
)

/**
 * Trigger types
 */
enum class TriggerType {
    STOCK_LEVEL,
    TIME_BASED,
    SALES_VELOCITY,
    SEASONAL_EVENT,
    PRICE_CHANGE,
    COMPETITOR_ACTION,
    DEMAND_SPIKE,
    MANUAL_TRIGGER
}

/**
 * Trigger schedule
 */
data class TriggerSchedule(
    val scheduleType: ScheduleType,
    val frequency: Int,
    val timeUnit: TimeUnit,
    val specificTimes: List<String>?,
    val daysOfWeek: List<Int>?,
    val daysOfMonth: List<Int>?
)

/**
 * Schedule types
 */
enum class ScheduleType {
    ONCE, RECURRING, CRON_BASED, EVENT_DRIVEN
}

/**
 * Time units
 */
enum class TimeUnit {
    MINUTES, HOURS, DAYS, WEEKS, MONTHS
}

/**
 * Automation conditions
 */
data class AutomationCondition(
    val conditionType: ConditionType,
    val operator: ConditionOperator,
    val value: Any,
    val logicalOperator: LogicalOperator?
)

/**
 * Condition types
 */
enum class ConditionType {
    STOCK_QUANTITY,
    SALES_VOLUME,
    PROFIT_MARGIN,
    TIME_PERIOD,
    COMPETITOR_PRICE,
    DEMAND_LEVEL,
    SEASONAL_FACTOR
}

/**
 * Condition operators
 */
enum class ConditionOperator {
    EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL, CONTAINS, IN_RANGE
}

/**
 * Logical operators
 */
enum class LogicalOperator {
    AND, OR, NOT
}

/**
 * Automation actions
 */
data class AutomationAction(
    val actionType: ActionType,
    val parameters: Map<String, Any>,
    val priority: Int,
    val executionOrder: Int
)

/**
 * Action types
 */
enum class ActionType {
    SEND_NOTIFICATION,
    CREATE_PURCHASE_ORDER,
    ADJUST_PRICE,
    UPDATE_THRESHOLD,
    ESCALATE_TO_MANAGER,
    CONTACT_SUPPLIER,
    UPDATE_PRODUCT_STATUS,
    GENERATE_REPORT
}

/**
 * Notification channels
 */
data class NotificationChannel(
    val channelType: ChannelType,
    val recipient: String,
    val priority: Int,
    val isEnabled: Boolean,
    val settings: Map<String, Any>
)

/**
 * Channel types
 */
enum class ChannelType {
    EMAIL, SMS, PUSH_NOTIFICATION, SLACK, WHATSAPP, TELEGRAM, WEBHOOK, IN_APP
}

/**
 * Notification status
 */
enum class NotificationStatus {
    ACTIVE, PAUSED, TRIGGERED, RESOLVED, EXPIRED
}

/**
 * Stock status
 */
enum class StockStatus {
    IN_STOCK, LOW_STOCK, CRITICAL_STOCK, OUT_OF_STOCK, DISCONTINUED, BACK_ORDER
}

/**
 * Performance optimization configuration
 */
data class PerformanceConfig(
    val configId: String = UUID.randomUUID().toString(),
    val databaseOptimization: DatabaseOptimizationConfig,
    val memoryManagement: MemoryManagementConfig,
    val cachingStrategy: CachingStrategyConfig,
    val lazyLoading: LazyLoadingConfig,
    val queryOptimization: QueryOptimizationConfig,
    val resourceMonitoring: ResourceMonitoringConfig,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true
)

/**
 * Database optimization configuration
 */
data class DatabaseOptimizationConfig(
    val enableQueryOptimization: Boolean = true,
    val enableIndexOptimization: Boolean = true,
    val enableConnectionPooling: Boolean = true,
    val connectionPoolSize: Int = 10,
    val queryTimeout: Long = 30000, // milliseconds
    val enablePreparedStatements: Boolean = true,
    val enableBatchOperations: Boolean = true,
    val batchSize: Int = 100,
    val enableVacuum: Boolean = true,
    val vacuumFrequency: VacuumFrequency = VacuumFrequency.WEEKLY
)

/**
 * Vacuum frequency options
 */
enum class VacuumFrequency {
    DAILY, WEEKLY, MONTHLY, ON_DEMAND
}

/**
 * Memory management configuration
 */
data class MemoryManagementConfig(
    val enableMemoryMonitoring: Boolean = true,
    val memoryThreshold: Float = 0.8f, // 80% of available memory
    val enableGarbageCollection: Boolean = true,
    val gcFrequency: GCFrequency = GCFrequency.ADAPTIVE,
    val enableLeakDetection: Boolean = true,
    val enableImageCaching: Boolean = true,
    val maxImageCacheSize: Long = 50 * 1024 * 1024, // 50MB
    val enableResourceCleaning: Boolean = true
)

/**
 * Garbage collection frequency
 */
enum class GCFrequency {
    AGGRESSIVE, NORMAL, CONSERVATIVE, ADAPTIVE
}

/**
 * Caching strategy configuration
 */
data class CachingStrategyConfig(
    val enableL1Cache: Boolean = true,
    val enableL2Cache: Boolean = true,
    val l1CacheSize: Int = 100,
    val l2CacheSize: Int = 1000,
    val cacheEvictionPolicy: EvictionPolicy = EvictionPolicy.LRU,
    val cacheTTL: Long = 300000, // 5 minutes
    val enableCachePreloading: Boolean = true,
    val preloadStrategies: List<PreloadStrategy>
)

/**
 * Cache eviction policies
 */
enum class EvictionPolicy {
    LRU, LFU, FIFO, RANDOM, TTL_BASED
}

/**
 * Preload strategies
 */
enum class PreloadStrategy {
    POPULAR_ITEMS, RECENT_ITEMS, CATEGORY_BASED, USER_PREFERENCE_BASED
}

/**
 * Lazy loading configuration
 */
data class LazyLoadingConfig(
    val enableLazyLoading: Boolean = true,
    val lazyLoadingStrategies: List<LazyLoadingStrategy>,
    val batchLoadSize: Int = 20,
    val enablePredictiveLoading: Boolean = true,
    val prefetchDistance: Int = 3, // items
    val enableImageLazyLoading: Boolean = true,
    val imagePlaceholder: String? = null
)

/**
 * Lazy loading strategies
 */
enum class LazyLoadingStrategy {
    ON_DEMAND, VIEWPORT_BASED, SCROLL_BASED, TIME_BASED, PROXIMITY_BASED
}

/**
 * Query optimization configuration
 */
data class QueryOptimizationConfig(
    val enableQueryCaching: Boolean = true,
    val queryCacheSize: Int = 500,
    val enableQueryAnalysis: Boolean = true,
    val enableIndexSuggestions: Boolean = true,
    val slowQueryThreshold: Long = 1000, // milliseconds
    val enableQueryProfiling: Boolean = true,
    val optimizationLevel: OptimizationLevel = OptimizationLevel.BALANCED
)

/**
 * Optimization levels
 */
enum class OptimizationLevel {
    CONSERVATIVE, BALANCED, AGGRESSIVE
}

/**
 * Resource monitoring configuration
 */
data class ResourceMonitoringConfig(
    val enableCPUMonitoring: Boolean = true,
    val enableMemoryMonitoring: Boolean = true,
    val enableNetworkMonitoring: Boolean = true,
    val enableStorageMonitoring: Boolean = true,
    val monitoringInterval: Long = 30000, // milliseconds
    val alertThresholds: ResourceThresholds,
    val enablePerformanceLogging: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO
)

/**
 * Resource thresholds
 */
data class ResourceThresholds(
    val cpuThreshold: Float = 80f,
    val memoryThreshold: Float = 85f,
    val networkLatencyThreshold: Long = 2000, // milliseconds
    val storageThreshold: Float = 90f
)

/**
 * Log levels
 */
enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR, CRITICAL
}

/**
 * Performance metrics
 */
data class AutomationPerformanceMetrics(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val cpuUsage: Float,
    val memoryUsage: Long,
    val availableMemory: Long,
    val networkLatency: Long,
    val queryExecutionTimes: List<QueryMetric>,
    val cacheHitRate: Float,
    val databaseConnections: Int,
    val activeThreads: Int,
    val responseTime: Long
)

/**
 * Query performance metrics
 */
data class QueryMetric(
    val queryId: String,
    val query: String,
    val executionTime: Long,
    val rowsAffected: Int,
    val cacheUsed: Boolean
)

/**
 * Automation analytics
 */
data class AutomationAnalytics(
    val analyticsId: String = UUID.randomUUID().toString(),
    val period: AnalyticsPeriod,
    val pricingSuggestionStats: PricingSuggestionStats,
    val imeiScanStats: IMEIScanStats,
    val stockNotificationStats: StockNotificationStats,
    val performanceStats: PerformanceStats,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Analytics periods
 */
enum class AnalyticsPeriod {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
}

/**
 * Pricing suggestion statistics
 */
data class PricingSuggestionStats(
    val totalSuggestions: Int,
    val acceptedSuggestions: Int,
    val rejectedSuggestions: Int,
    val averageAccuracy: Float,
    val profitImpact: Double,
    val topFactors: List<PricingFactorType>
)

/**
 * IMEI scan statistics
 */
data class IMEIScanStats(
    val totalScans: Int,
    val successfulScans: Int,
    val duplicatesFound: Int,
    val blacklistedDevices: Int,
    val averageQualityScore: Float,
    val topDeviceBrands: List<String>
)

/**
 * Stock notification statistics
 */
data class StockNotificationStats(
    val totalNotifications: Int,
    val criticalNotifications: Int,
    val resolvedNotifications: Int,
    val averageResolutionTime: Long,
    val stockoutsPrevented: Int,
    val automationEfficiency: Float
)

/**
 * Performance statistics
 */
data class PerformanceStats(
    val averageResponseTime: Long,
    val averageQueryTime: Long,
    val cacheEfficiency: Float,
    val memoryUsageStats: MemoryStats,
    val optimizationImpact: Float
)

/**
 * Memory usage statistics
 */
data class MemoryStats(
    val averageUsage: Long,
    val peakUsage: Long,
    val garbageCollections: Int,
    val memoryLeaksDetected: Int
)

/**
 * Automation Schedule for recurring automations
 */
data class AutomationSchedule(
    val scheduleId: String = UUID.randomUUID().toString(),
    val automationId: String,
    val frequency: ScheduleFrequency,
    val daysOfWeek: List<Int>? = null,
    val timeOfDay: String? = null,
    val nextRunTime: LocalDateTime? = null,
    val lastRunTime: LocalDateTime? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Schedule frequency enum
 */
enum class ScheduleFrequency {
    ONCE, DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM
}

/**
 * Performance Recommendation for optimization
 */
data class PerformanceRecommendation(
    val recommendationId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: RecommendationCategory,
    val priority: RecommendationPriority,
    val estimatedImpact: Double, // percentage improvement
    val difficulty: DifficultyLevel,
    val estimatedTimeToImplement: Long, // in minutes
    val actions: List<String>,
    val relatedMetrics: List<String>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val status: RecommendationStatus = RecommendationStatus.PENDING
)

/**
 * Recommendation category enum
 */
enum class RecommendationCategory {
    PERFORMANCE, COST, QUALITY, SECURITY, RELIABILITY, OTHER
}

/**
 * Difficulty level enum
 */
enum class DifficultyLevel {
    EASY, MEDIUM, HARD, CRITICAL
}

/**
 * Recommendation status enum
 */
enum class RecommendationStatus {
    PENDING, IMPLEMENTED, DISMISSED, COMPLETED
}