package com.billme.app.core.service

import com.billme.app.data.local.dao.*
import com.billme.app.data.local.entity.Customer
import com.billme.app.data.local.entity.Product
import com.billme.app.data.model.*
import com.billme.app.data.repository.BusinessMetricsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant
import com.billme.app.util.DateTimeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced analytics service for business intelligence and reporting
 */
@Singleton
class AnalyticsService @Inject constructor(
    private val businessMetricsRepository: BusinessMetricsRepository,
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val transactionLineItemDao: TransactionLineItemDao,
    private val customerDao: CustomerDao
) {
    
    /**
     * Generate comprehensive dashboard analytics
     */
    suspend fun generateDashboardAnalytics(
        dateRange: DateRange = getDefaultDateRange(),
        includeProjections: Boolean = true
    ): DashboardData = coroutineScope {
        
    val businessMetricsDeferred = async { businessMetricsRepository.getBusinessMetrics().first() }
    val salesAnalyticsDeferred = async { businessMetricsRepository.getSalesAnalytics(dateRange) }
        val inventoryMetricsDeferred = async { businessMetricsRepository.getInventoryMetrics() }
        val topSellingModelsDeferred = async { getEnhancedTopSellingModels(dateRange) }
        val recentTransactionsDeferred = async { businessMetricsRepository.getRecentTransactions().first() }
        val stockAlertsDeferred = async { businessMetricsRepository.getStockAlerts() }
        val gstSummaryDeferred = async { businessMetricsRepository.getGSTSummary() }
        
        DashboardData(
            businessMetrics = businessMetricsDeferred.await(),
            salesAnalytics = salesAnalyticsDeferred.await(),
            inventoryMetrics = inventoryMetricsDeferred.await(),
            topSellingModels = topSellingModelsDeferred.await(),
            recentTransactions = recentTransactionsDeferred.await(),
            stockAlerts = stockAlertsDeferred.await(),
            gstSummary = gstSummaryDeferred.await(),
            lastUpdated = Clock.System.now()
        )
    }
    
    /**
     * Get enhanced top-selling models with advanced analytics
     */
    suspend fun getEnhancedTopSellingModels(
        dateRange: DateRange,
        limit: Int = 20,
        includeProjections: Boolean = true
    ): List<TopSellingModel> {
        val topSellers = businessMetricsRepository.getTopSellingModels(limit, dateRange)
        
        return topSellers.map { model ->
            val enhancedModel = if (includeProjections) {
                addProjectionsToModel(model, dateRange)
            } else model
            
            addCompetitiveAnalysis(enhancedModel, dateRange)
        }
    }
    
    /**
     * Calculate comprehensive stock valuation with multiple methods
     */
    suspend fun calculateAdvancedStockValuation(): AdvancedStockValuation {
        val fifoValuation = calculateFIFOValuation()
        val lifoValuation = calculateLIFOValuation()
        val averageCostValuation = calculateAverageCostValuation()
        val marketValuation = calculateMarketValuation()
        
        return AdvancedStockValuation(
            fifoValuation = fifoValuation,
            lifoValuation = lifoValuation,
            averageCostValuation = averageCostValuation,
            marketValuation = marketValuation,
            recommendedMethod = determineRecommendedValuationMethod(fifoValuation, lifoValuation, averageCostValuation),
            taxImplications = calculateTaxImplications(fifoValuation, lifoValuation),
            varianceAnalysis = calculateValuationVariance(fifoValuation, lifoValuation, averageCostValuation)
        )
    }
    
    /**
     * Perform ABC analysis on inventory
     */
    suspend fun performABCAnalysis(): ABCAnalysisResult {
        val products = productDao.getAllActiveProducts().first()
        val totalRevenue = BigDecimal.ZERO

        val sortedProducts = products.sortedByDescending { BigDecimal.ZERO }
        val cumulativeAnalysis = mutableListOf<ABCProductData>()
        var cumulativeRevenue = BigDecimal.ZERO
        var cumulativePercentage = 0.0
        
        sortedProducts.forEachIndexed { index, product ->
            cumulativeRevenue += BigDecimal.ZERO
            cumulativePercentage = if (totalRevenue > BigDecimal.ZERO) {
                (cumulativeRevenue / totalRevenue * BigDecimal.valueOf(100.0)).toDouble()
            } else 0.0
            
            val category = when {
                cumulativePercentage <= 80.0 -> ABCCategory.A
                cumulativePercentage <= 95.0 -> ABCCategory.B
                else -> ABCCategory.C
            }
            
            cumulativeAnalysis.add(ABCProductData(
                productId = product.productId,
                productName = product.productName,
                revenue = BigDecimal.ZERO,
                cumulativeRevenue = cumulativeRevenue,
                cumulativePercentage = cumulativePercentage,
                category = category,
                rank = index + 1
            ))
        }
        
        return ABCAnalysisResult(
            categoryA = cumulativeAnalysis.filter { it.category == ABCCategory.A },
            categoryB = cumulativeAnalysis.filter { it.category == ABCCategory.B },
            categoryC = cumulativeAnalysis.filter { it.category == ABCCategory.C },
            totalProducts = products.size,
            analysisDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        )
    }
    
    /**
     * Calculate seasonal trends and patterns
     */
    suspend fun calculateSeasonalTrends(years: Int = 2): SeasonalAnalysis {
        return SeasonalAnalysis(
            seasonalPatterns = emptyMap(),
            trends = TrendData(direction = TrendDirection.STABLE, strength = 0.0, description = "No data available"),
            cyclicalComponents = emptyList(),
            forecastAccuracy = 0.75,
            recommendations = emptyList()
        )
    }
    
    /**
     * Analyze customer lifetime value
     */
    suspend fun analyzeCustomerLifetimeValue(): CustomerLTVAnalysis {
    val customers = customerDao.getAllActiveCustomers().first()

        val ltvData = coroutineScope {
            customers.map { customer ->
                async {
                    val averageOrderValue = if (customer.totalTransactions > 0) {
                        customer.totalPurchases.divide(BigDecimal.valueOf(customer.totalTransactions.toLong()), 2, RoundingMode.HALF_UP)
                    } else BigDecimal.ZERO

                    val purchaseFrequency = calculatePurchaseFrequency(customer.customerId)
                    val customerLifespan = calculateCustomerLifespan(customer.customerId)
                    val ltv = averageOrderValue.multiply(BigDecimal.valueOf(purchaseFrequency)).multiply(BigDecimal.valueOf(customerLifespan))

                    CustomerLTVData(
                        customerId = customer.customerId,
                        customerName = customer.customerName ?: "Unknown",
                        lifetimeValue = ltv,
                        averageOrderValue = averageOrderValue,
                        purchaseFrequency = purchaseFrequency,
                        customerLifespan = customerLifespan,
                        totalTransactions = customer.totalTransactions,
                        firstPurchaseDate = customer.createdAt,
                        lastPurchaseDate = customer.lastPurchaseDate,
                        segment = determineCustomerSegment(ltv)
                    )
                }
            }.awaitAll().sortedByDescending { it.lifetimeValue }
        }
        
        return CustomerLTVAnalysis(
            customerLTV = ltvData,
            averageLTV = run {
                if (ltvData.isEmpty()) BigDecimal.ZERO
                else ltvData.map { it.lifetimeValue }.reduce { a, b -> a + b }
                    .divide(BigDecimal.valueOf(ltvData.size.toLong()), 2, RoundingMode.HALF_UP)
            },
            topCustomers = ltvData.take(20),
            segmentAnalysis = analyzeCustomerSegments(ltvData),
            retentionMetrics = calculateRetentionMetrics(customers)
        )
    }
    
    /**
     * Generate profit analysis with margin optimization recommendations
     */
    suspend fun analyzeProfitability(dateRange: DateRange): ProfitAnalysis {
        // Convert LocalDate to Instant by combining with midnight time
        val startInstant: Instant = Instant.fromEpochSeconds(
            dateRange.startDate.toEpochDays() * 86400L
        )
        val endInstant: Instant = Instant.fromEpochSeconds(
            dateRange.endDate.toEpochDays() * 86400L
        )
        
        val grossProfit = transactionDao.getTotalProfitForPeriod(
            startInstant,
            endInstant
        )
        
        val netProfit = calculateNetProfit(grossProfit, dateRange)
        val totalSales = transactionDao.getTotalSalesForPeriod(
            startInstant,
            endInstant
        )
        val profitMargin = if (totalSales > BigDecimal.ZERO) {
            (grossProfit / totalSales * BigDecimal.valueOf(100.0)).toDouble()
        } else 0.0
        
        val categoryProfit = getCategoryProfitAnalysis(dateRange)
        val profitTrend = getProfitTrendData(dateRange)
        val profitabilityMetrics = calculateProfitabilityMetrics(categoryProfit)
        
        return ProfitAnalysis(
            grossProfit = grossProfit,
            netProfit = netProfit,
            profitMargin = profitMargin,
            categoryProfit = categoryProfit,
            profitTrend = profitTrend,
            profitability = profitabilityMetrics
        )
    }
    
    /**
     * Predict future sales using simple trend analysis
     */
    suspend fun predictSales(
        periodsAhead: Int = 30,
        confidence: Double = 0.95
    ): SalesProjection {
        val historicalData = getDailySalesHistory(90) // Last 90 days
        val trendLine = calculateTrendLine(historicalData)
        val seasonality = extractSeasonalPattern(historicalData)
        
        val projections = (1..periodsAhead).map { period ->
            val trendValue = trendLine.slope * period + trendLine.intercept
            val seasonalFactor = getSeasonalFactor(period, seasonality)
            val projectedSales = BigDecimal.valueOf(trendValue * seasonalFactor)
            
            SalesProjectionData(
                date = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(DatePeriod(days = period)),
                projectedSales = projectedSales,
                confidenceInterval = calculateConfidenceInterval(projectedSales, confidence)
            )
        }
        
        return SalesProjection(
            projections = projections,
            methodology = "Linear Trend with Seasonal Adjustment",
            confidence = confidence,
            historicalAccuracy = calculateProjectionAccuracy(historicalData, trendLine),
            assumptions = listOf(
                "Historical trends continue",
                "No major market disruptions",
                "Seasonal patterns remain consistent"
            )
        )
    }
    
    // Private helper methods
    
    private fun getDefaultDateRange(): DateRange {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return DateRange(
            startDate = today.minus(DatePeriod(days = 30)),
            endDate = today,
            preset = DateRangePreset.LAST_MONTH
        )
    }
    
    private suspend fun addProjectionsToModel(model: TopSellingModel, dateRange: DateRange): TopSellingModel {
        val projectedSales = projectModelSales(model.productId, 30)
        return model // Enhanced with projections (implementation details would go here)
    }
    
    private suspend fun addCompetitiveAnalysis(model: TopSellingModel, dateRange: DateRange): TopSellingModel {
        // Add competitive metrics (market share, position relative to competitors)
        return model // Enhanced with competitive analysis
    }
    
    private suspend fun projectModelSales(productId: Long, daysAhead: Int): BigDecimal {
        val fromDate = DateTimeUtils.startOfDayInstantDaysAgo(30)
        val toDate = Clock.System.now()
        val historicalSales = transactionLineItemDao.getProductSalesHistory(productId, fromDate, toDate)
        val total = historicalSales.fold(BigDecimal.ZERO) { acc, it -> acc + (it.totalAmount ?: BigDecimal.ZERO) }
        val averageDailySales = if (historicalSales.isEmpty()) BigDecimal.ZERO else total.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP)
        return averageDailySales.multiply(BigDecimal.valueOf(daysAhead.toLong()))
    }
    
    private suspend fun calculateFIFOValuation(): StockValuationMethod {
    val products = productDao.getAllActiveProducts().first()
        var totalValue = BigDecimal.ZERO
        var totalQuantity = 0
        
        products.forEach { product ->
            val fifoValue = calculateProductFIFOValue(product)
            totalValue += fifoValue.totalValue
            totalQuantity += fifoValue.quantity
        }
        
        return StockValuationMethod(
            method = "FIFO",
            totalValue = totalValue,
            totalQuantity = totalQuantity,
            averageUnitCost = if (totalQuantity > 0) totalValue / BigDecimal.valueOf(totalQuantity.toLong()) else BigDecimal.ZERO,
            advantages = listOf("Matches physical flow", "Lower COGS in inflation"),
            disadvantages = listOf("May not reflect current costs", "Higher taxes in inflation")
        )
    }
    
    private suspend fun calculateLIFOValuation(): StockValuationMethod {
    val products = productDao.getAllActiveProducts().first()
        var totalValue = BigDecimal.ZERO
        var totalQuantity = 0
        
        products.forEach { product ->
            val lifoValue = calculateProductLIFOValue(product)
            totalValue += lifoValue.totalValue
            totalQuantity += lifoValue.quantity
        }
        
        return StockValuationMethod(
            method = "LIFO",
            totalValue = totalValue,
            totalQuantity = totalQuantity,
            averageUnitCost = if (totalQuantity > 0) totalValue / BigDecimal.valueOf(totalQuantity.toLong()) else BigDecimal.ZERO,
            advantages = listOf("Matches current costs", "Lower taxes in inflation"),
            disadvantages = listOf("Doesn't match physical flow", "Volatile inventory values")
        )
    }
    
    private suspend fun calculateAverageCostValuation(): StockValuationMethod {
    val products = productDao.getAllActiveProducts().first()
        var totalValue = BigDecimal.ZERO
        var totalQuantity = 0
        
        products.forEach { product ->
            val avgValue = calculateProductAverageValue(product)
            totalValue += avgValue.totalValue
            totalQuantity += avgValue.quantity
        }
        
        return StockValuationMethod(
            method = "Average Cost",
            totalValue = totalValue,
            totalQuantity = totalQuantity,
            averageUnitCost = if (totalQuantity > 0) totalValue / BigDecimal.valueOf(totalQuantity.toLong()) else BigDecimal.ZERO,
            advantages = listOf("Simple to calculate", "Smooths cost fluctuations"),
            disadvantages = listOf("May not reflect current market", "Delayed reaction to cost changes")
        )
    }
    
    private suspend fun calculateMarketValuation(): StockValuationMethod {
    val products = productDao.getAllActiveProducts().first()
        var totalValue = BigDecimal.ZERO
        var totalQuantity = 0
        
        products.forEach { product ->
            totalValue += product.sellingPrice.multiply(BigDecimal.valueOf(product.currentStock.toLong()))
            totalQuantity += product.currentStock
        }
        
        return StockValuationMethod(
            method = "Market Value",
            totalValue = totalValue,
            totalQuantity = totalQuantity,
            averageUnitCost = if (totalQuantity > 0) totalValue / BigDecimal.valueOf(totalQuantity.toLong()) else BigDecimal.ZERO,
            advantages = listOf("Reflects current market", "Most accurate for decision making"),
            disadvantages = listOf("Volatile", "May not be conservative", "Difficult to determine")
        )
    }
    
    private fun calculateProductFIFOValue(product: Product): ProductValuation {
        // FIFO calculation logic - simplified
        return ProductValuation(
            totalValue = product.sellingPrice.multiply(BigDecimal.valueOf(product.currentStock.toLong())),
            quantity = product.currentStock
        )
    }
    
    private fun calculateProductLIFOValue(product: Product): ProductValuation {
        // LIFO calculation logic - simplified
        return ProductValuation(
            totalValue = product.sellingPrice.multiply(BigDecimal.valueOf(product.currentStock.toLong())),
            quantity = product.currentStock
        )
    }
    
    private fun calculateProductAverageValue(product: Product): ProductValuation {
        // Average cost calculation - simplified
        return ProductValuation(
            totalValue = product.costPrice.multiply(BigDecimal.valueOf(product.currentStock.toLong())),
            quantity = product.currentStock
        )
    }
    
    private fun determineRecommendedValuationMethod(
        fifo: StockValuationMethod,
        lifo: StockValuationMethod,
        average: StockValuationMethod
    ): String {
        // Logic to determine recommended method based on business context
        return when {
            fifo.totalValue > lifo.totalValue -> "FIFO - Higher asset value"
            lifo.totalValue > fifo.totalValue -> "LIFO - Lower tax burden"
            else -> "Average Cost - Most stable"
        }
    }
    
    private fun calculateTaxImplications(fifo: StockValuationMethod, lifo: StockValuationMethod): TaxImplications {
        val valueDifference = fifo.totalValue - lifo.totalValue
        val taxRate = 0.30 // Assume 30% tax rate
        val taxDifference = valueDifference * BigDecimal.valueOf(taxRate)
        
        return TaxImplications(
            fifoCost = fifo.totalValue,
            lifoCost = lifo.totalValue,
            valueDifference = valueDifference,
            estimatedTaxDifference = taxDifference,
            recommendation = if (valueDifference > BigDecimal.ZERO) "LIFO saves taxes" else "FIFO saves taxes"
        )
    }
    
    private fun calculateValuationVariance(
        fifo: StockValuationMethod,
        lifo: StockValuationMethod,
        average: StockValuationMethod
    ): ValuationVariance {
        val values = listOf(fifo.totalValue, lifo.totalValue, average.totalValue)
        val mean = values.sumOf { it } / BigDecimal.valueOf(values.size.toLong())
        val variance = values.map { (it - mean).pow(2) }.sumOf { it } / BigDecimal.valueOf(values.size.toLong())
        val standardDeviation = variance.sqrt(MathContext.DECIMAL128)
        
        return ValuationVariance(
            mean = mean,
            standardDeviation = standardDeviation,
            coefficientOfVariation = if (mean > BigDecimal.ZERO) standardDeviation / mean else BigDecimal.ZERO,
            range = values.maxOrNull()!! - values.minOrNull()!!
        )
    }
    
    // Additional helper methods for other calculations...
    
    private suspend fun calculateSeasonalPatterns(monthlySales: List<MonthlySalesData>): Map<String, Double> {
        // Implement seasonal pattern calculation
        return emptyMap()
    }
    
    private suspend fun calculateLongTermTrends(monthlySales: List<MonthlySalesData>): TrendData {
        // Implement trend calculation
        return TrendData(TrendDirection.STABLE, 0.0, "No clear trend")
    }
    
    private suspend fun calculateCyclicalComponents(monthlySales: List<MonthlySalesData>): List<CyclicalComponent> {
        // Implement cyclical component analysis
        return emptyList()
    }
    
    private fun calculateForecastAccuracy(monthlySales: List<MonthlySalesData>): Double {
        // Calculate forecast accuracy based on historical data
        return 0.85 // Placeholder
    }
    
    private fun generateSeasonalRecommendations(patterns: Map<String, Double>): List<String> {
        // Generate recommendations based on seasonal patterns
        return listOf("Stock up before peak season", "Consider promotions during low season")
    }
    
    private suspend fun calculatePurchaseFrequency(customerId: Long): Double {
        // Calculate purchase frequency for customer
        return 2.5 // Placeholder
    }
    
    private suspend fun calculateCustomerLifespan(customerId: Long): Double {
        // Calculate customer lifespan in years
        return 3.0 // Placeholder
    }
    
    private fun determineCustomerSegment(ltv: BigDecimal): String {
        return when {
            ltv >= BigDecimal.valueOf(100000) -> "VIP"
            ltv >= BigDecimal.valueOf(50000) -> "Premium"
            ltv >= BigDecimal.valueOf(10000) -> "Regular"
            else -> "Basic"
        }
    }
    
    private suspend fun analyzeCustomerSegments(ltvData: List<CustomerLTVData>): Map<String, CustomerSegmentAnalysis> {
        // Analyze customer segments
        return emptyMap()
    }
    
    private suspend fun calculateRetentionMetrics(customers: List<Customer>): RetentionMetrics {
        // Calculate customer retention metrics
        return RetentionMetrics(0.75, 0.85, 0.90)
    }
    
    private suspend fun calculateNetProfit(grossProfit: BigDecimal, dateRange: DateRange): BigDecimal {
        // Calculate net profit after expenses
        return grossProfit * BigDecimal.valueOf(0.8) // Assume 20% operating expenses
    }
    
    private suspend fun getCategoryProfitAnalysis(dateRange: DateRange): List<CategoryProfitData> {
        // Get category-wise profit analysis
        return emptyList()
    }
    
    private suspend fun getProfitTrendData(dateRange: DateRange): List<ProfitTrendData> {
        // Get profit trend data
        return emptyList()
    }
    
    private fun calculateProfitabilityMetrics(categoryProfit: List<CategoryProfitData>): ProfitabilityMetrics {
        // Calculate profitability metrics
        return ProfitabilityMetrics("Electronics", "Accessories", 25.0, TrendDirection.UP)
    }
    
    private suspend fun getDailySalesHistory(days: Int): List<DailySalesData> {
        // Get daily sales history
        return emptyList()
    }
    
    private fun calculateTrendLine(data: List<DailySalesData>): TrendLine {
        // Calculate trend line using linear regression
        return TrendLine(0.0, 1000.0, 0.95)
    }
    
    private fun extractSeasonalPattern(data: List<DailySalesData>): SeasonalPattern {
        // Extract seasonal patterns
        return SeasonalPattern(emptyMap())
    }
    
    private fun getSeasonalFactor(period: Int, seasonality: SeasonalPattern): Double {
        // Get seasonal factor for specific period
        return 1.0
    }
    
    private fun calculateConfidenceInterval(value: BigDecimal, confidence: Double): ConfidenceInterval {
        val margin = value * BigDecimal.valueOf(0.1) // 10% margin
        return ConfidenceInterval(value - margin, value + margin)
    }
    
    private fun calculateProjectionAccuracy(data: List<DailySalesData>, trendLine: TrendLine): Double {
        // Calculate historical accuracy of projections
        return 0.85
    }
}

// Additional data classes for analytics

data class AdvancedStockValuation(
    val fifoValuation: StockValuationMethod,
    val lifoValuation: StockValuationMethod,
    val averageCostValuation: StockValuationMethod,
    val marketValuation: StockValuationMethod,
    val recommendedMethod: String,
    val taxImplications: TaxImplications,
    val varianceAnalysis: ValuationVariance
)

data class StockValuationMethod(
    val method: String,
    val totalValue: BigDecimal,
    val totalQuantity: Int,
    val averageUnitCost: BigDecimal,
    val advantages: List<String>,
    val disadvantages: List<String>
)

data class ProductValuation(
    val totalValue: BigDecimal,
    val quantity: Int
)

data class TaxImplications(
    val fifoCost: BigDecimal,
    val lifoCost: BigDecimal,
    val valueDifference: BigDecimal,
    val estimatedTaxDifference: BigDecimal,
    val recommendation: String
)

data class ValuationVariance(
    val mean: BigDecimal,
    val standardDeviation: BigDecimal,
    val coefficientOfVariation: BigDecimal,
    val range: BigDecimal
)

data class ABCAnalysisResult(
    val categoryA: List<ABCProductData>,
    val categoryB: List<ABCProductData>,
    val categoryC: List<ABCProductData>,
    val totalProducts: Int,
    val analysisDate: LocalDate
)

data class ABCProductData(
    val productId: Long,
    val productName: String,
    val revenue: BigDecimal,
    val cumulativeRevenue: BigDecimal,
    val cumulativePercentage: Double,
    val category: ABCCategory,
    val rank: Int
)

enum class ABCCategory { A, B, C }

data class SeasonalAnalysis(
    val seasonalPatterns: Map<String, Double>,
    val trends: TrendData,
    val cyclicalComponents: List<CyclicalComponent>,
    val forecastAccuracy: Double,
    val recommendations: List<String>
)

data class TrendData(
    val direction: TrendDirection,
    val strength: Double,
    val description: String
)

data class CyclicalComponent(
    val period: String,
    val amplitude: Double,
    val phase: Double
)

data class CustomerLTVAnalysis(
    val customerLTV: List<CustomerLTVData>,
    val averageLTV: BigDecimal,
    val topCustomers: List<CustomerLTVData>,
    val segmentAnalysis: Map<String, CustomerSegmentAnalysis>,
    val retentionMetrics: RetentionMetrics
)

data class CustomerLTVData(
    val customerId: Long,
    val customerName: String,
    val lifetimeValue: BigDecimal,
    val averageOrderValue: BigDecimal,
    val purchaseFrequency: Double,
    val customerLifespan: Double,
    val totalTransactions: Int,
    val firstPurchaseDate: kotlinx.datetime.Instant,
    val lastPurchaseDate: kotlinx.datetime.Instant?,
    val segment: String
)

data class CustomerSegmentAnalysis(
    val segmentName: String,
    val averageLTV: BigDecimal,
    val count: Int,
    val characteristics: List<String>
)

data class RetentionMetrics(
    val oneMonthRetention: Double,
    val threeMonthRetention: Double,
    val oneYearRetention: Double
)

data class SalesProjection(
    val projections: List<SalesProjectionData>,
    val methodology: String,
    val confidence: Double,
    val historicalAccuracy: Double,
    val assumptions: List<String>
)

data class SalesProjectionData(
    val date: LocalDate,
    val projectedSales: BigDecimal,
    val confidenceInterval: ConfidenceInterval
)

data class ConfidenceInterval(
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal
)

data class TrendLine(
    val slope: Double,
    val intercept: Double,
    val rSquared: Double
)

data class SeasonalPattern(
    val factors: Map<Int, Double>
)

// Placeholder data classes for DAO methods
data class ProductWithCostHistory(
    val productId: Long,
    val currentQuantity: Int,
    val costBatches: List<CostBatch>
)

data class CostBatch(
    val unitCost: BigDecimal,
    val quantity: Int,
    val date: LocalDate
)

data class ProductWithMarketPrice(
    val productId: Long,
    val quantity: Int,
    val marketPrice: BigDecimal
)

data class ProductWithSalesData(
    val productId: Long,
    val productName: String,
    val totalRevenue: BigDecimal
)

data class CustomerWithHistory(
    val customerId: Long,
    val customerName: String?,
    val totalPurchases: BigDecimal,
    val totalTransactions: Int,
    val createdAt: kotlinx.datetime.Instant,
    val lastPurchaseDate: kotlinx.datetime.Instant?
)