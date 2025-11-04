package com.billme.app.data.model

import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Comprehensive data models for Reports Module
 */

// Core Report Models

/**
 * Main report container for all report types
 */
data class ReportData(
    val reportId: String,
    val reportType: ReportType,
    val title: String,
    val dateRange: DateRange,
    val filters: ReportFilters,
    val generatedAt: LocalDateTime,
    val salesReport: SalesReport? = null,
    val profitReport: ProfitReport? = null,
    val stockAgingReport: StockAgingReport? = null,
    val taxReport: TaxReport? = null,
    val customReport: CustomReport? = null,
    val summary: ReportSummary,
    val exportFormats: List<ExportFormat> = listOf(ExportFormat.PDF, ExportFormat.CSV)
)

/**
 * Sales Report with detailed breakdowns
 */
data class SalesReport(
    val totalSales: BigDecimal,
    val totalUnits: Int,
    val averageOrderValue: BigDecimal,
    val transactionCount: Int,
    val dailySales: List<DailySalesData>,
    val monthlySales: List<MonthlySalesData>,
    val categoryWiseSales: List<CategorySalesData>,
    val brandWiseSales: List<BrandSalesData>,
    val paymentMethodBreakdown: List<PaymentMethodData>,
    val hourlyDistribution: List<HourlySalesData>,
    val topSellingProducts: List<ProductSalesData>,
    val salesTrend: SalesTrendAnalysis,
    val performance: SalesPerformanceMetrics
)

/**
 * Profit Report with margin analysis
 */
data class ProfitReport(
    val totalRevenue: BigDecimal,
    val totalCost: BigDecimal,
    val totalProfit: BigDecimal,
    val profitMargin: Double,
    val dailyProfit: List<DailyProfitData>,
    val monthlyProfit: List<MonthlyProfitData>,
    val categoryWiseProfit: List<CategoryProfitData>,
    val productWiseProfit: List<ProductProfitData>,
    val profitByPaymentMethod: List<PaymentMethodProfitData>,
    val profitTrend: ProfitTrendAnalysis,
    val marginAnalysis: MarginAnalysis,
    val costBreakdown: CostBreakdown
)

/**
 * Stock Aging Report for inventory management
 */
data class StockAgingReport(
    val totalStockValue: BigDecimal,
    val totalItems: Int,
    val averageAge: Double, // in days
    val agingCategories: List<StockAgingCategory>,
    val productAging: List<ProductAgingData>,
    val categoryAging: List<CategoryAgingData>,
    val deadStockItems: List<DeadStockItem>,
    val fastMovingItems: List<FastMovingItem>,
    val slowMovingItems: List<SlowMovingItem>,
    val reorderRecommendations: List<ReorderRecommendation>,
    val stockTurnoverAnalysis: StockTurnoverAnalysis
)

/**
 * Tax Report with GST breakdown
 */
data class TaxReport(
    val totalTaxableRevenue: BigDecimal,
    val totalNonTaxableRevenue: BigDecimal,
    val totalTaxCollected: BigDecimal,
    val taxableTransactions: Int,
    val nonTaxableTransactions: Int,
    val gstBreakdown: GSTBreakdown,
    val taxRateWiseBreakdown: List<TaxRateBreakdown>,
    val dailyTaxCollection: List<DailyTaxData>,
    val monthlyTaxCollection: List<MonthlyTaxData>,
    val taxExemptItems: List<TaxExemptItemData>,
    val interstate: InterstateTransactionData,
    val intrastate: IntrastateTransactionData
)

/**
 * Custom Report for flexible reporting
 */
data class CustomReport(
    val reportName: String,
    val metrics: List<CustomMetric>,
    val chartData: List<ChartDataSet>,
    val tables: List<TableData>,
    val kpis: List<KPIData>
)

// Supporting Data Classes

/**
 * Daily sales data point
 */
data class DailySalesData(
    val date: LocalDate,
    val sales: BigDecimal,
    val units: Int,
    val transactions: Int,
    val averageOrderValue: BigDecimal
)

/**
 * Monthly sales data point
 */
data class MonthlySalesData(
    val month: Int,
    val year: Int,
    val sales: BigDecimal,
    val units: Int,
    val transactions: Int,
    val growth: Double // percentage
)

/**
 * Brand-wise sales data
 */
data class BrandSalesData(
    val brand: String,
    val sales: BigDecimal,
    val units: Int,
    val transactions: Int,
    val percentage: Double,
    val trend: TrendDirection
)

/**
 * Payment method breakdown
 */
data class PaymentMethodData(
    val method: String,
    val amount: BigDecimal,
    val transactions: Int,
    val percentage: Double,
    val averageAmount: BigDecimal
)

/**
 * Hourly sales distribution
 */
data class HourlySalesData(
    val hour: Int,
    val sales: BigDecimal,
    val transactions: Int,
    val percentage: Double
)

/**
 * Product-wise sales data
 */
data class ProductSalesData(
    val productId: String,
    val productName: String,
    val brand: String,
    val category: String,
    val sales: BigDecimal,
    val units: Int,
    val rank: Int,
    val contribution: Double
)

/**
 * Sales trend analysis
 */
data class SalesTrendAnalysis(
    val overallTrend: TrendDirection,
    val growthRate: Double,
    val seasonality: SeasonalityData,
    val peakPeriods: List<PeakPeriod>,
    val forecast: List<ForecastData>
)

/**
 * Sales performance metrics
 */
data class SalesPerformanceMetrics(
    val conversionRate: Double,
    val customerRetention: Double,
    val averageOrderSize: BigDecimal,
    val salesVelocity: Double,
    val marketShare: Double
)

/**
 * Daily profit data
 */
data class DailyProfitData(
    val date: LocalDate,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val profit: BigDecimal,
    val margin: Double
)

/**
 * Monthly profit data
 */
data class MonthlyProfitData(
    val month: Int,
    val year: Int,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val profit: BigDecimal,
    val margin: Double,
    val growth: Double
)

/**
 * Category-wise profit analysis
 */
data class CategoryProfitData(
    val category: String,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val profit: BigDecimal,
    val margin: Double,
    val rank: Int
)

/**
 * Product-wise profit analysis
 */
data class ProductProfitData(
    val productId: String,
    val productName: String,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val profit: BigDecimal,
    val margin: Double,
    val units: Int
)

/**
 * Payment method profit data
 */
data class PaymentMethodProfitData(
    val method: String,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val profit: BigDecimal,
    val margin: Double
)

/**
 * Profit trend analysis
 */
data class ProfitTrendAnalysis(
    val trend: TrendDirection,
    val growthRate: Double,
    val marginTrend: TrendDirection,
    val bestPerformingPeriod: LocalDate,
    val worstPerformingPeriod: LocalDate
)

/**
 * Margin analysis breakdown
 */
data class MarginAnalysis(
    val averageMargin: Double,
    val highMarginProducts: List<ProductProfitData>,
    val lowMarginProducts: List<ProductProfitData>,
    val marginDistribution: List<MarginRangeData>
)

/**
 * Cost breakdown analysis
 */
data class CostBreakdown(
    val directCosts: BigDecimal,
    val indirectCosts: BigDecimal,
    val operationalCosts: BigDecimal,
    val costByCategory: List<CategoryCostData>,
    val costTrend: List<CostTrendData>
)

/**
 * Stock aging categories
 */
data class StockAgingCategory(
    val category: StockAgeCategory,
    val itemCount: Int,
    val stockValue: BigDecimal,
    val percentage: Double
)

/**
 * Product aging information
 */
data class ProductAgingData(
    val productId: String,
    val productName: String,
    val brand: String,
    val category: String,
    val ageInDays: Int,
    val stockQuantity: Int,
    val stockValue: BigDecimal,
    val lastSoldDate: LocalDate?,
    val ageCategory: StockAgeCategory
)

/**
 * Category aging analysis
 */
data class CategoryAgingData(
    val category: String,
    val averageAge: Double,
    val totalValue: BigDecimal,
    val itemCount: Int,
    val agingDistribution: Map<StockAgeCategory, Int>
)

/**
 * Dead stock items
 */
data class DeadStockItem(
    val productId: String,
    val productName: String,
    val daysWithoutSale: Int,
    val stockValue: BigDecimal,
    val recommendedAction: StockAction
)

/**
 * Fast moving items
 */
data class FastMovingItem(
    val productId: String,
    val productName: String,
    val turnoverRate: Double,
    val salesVelocity: Double,
    val stockLevel: Int,
    val reorderPoint: Int
)

/**
 * Slow moving items
 */
data class SlowMovingItem(
    val productId: String,
    val productName: String,
    val turnoverRate: Double,
    val daysToSell: Int,
    val stockValue: BigDecimal,
    val recommendedAction: StockAction
)

/**
 * Reorder recommendations
 */
data class ReorderRecommendation(
    val productId: String,
    val productName: String,
    val currentStock: Int,
    val reorderPoint: Int,
    val recommendedQuantity: Int,
    val urgency: ReorderUrgency,
    val estimatedDaysToStockout: Int
)

/**
 * Stock turnover analysis
 */
data class StockTurnoverAnalysis(
    val averageTurnover: Double,
    val bestTurnoverCategory: String,
    val worstTurnoverCategory: String,
    val turnoverTrend: TrendDirection,
    val categoryTurnover: List<CategoryTurnoverData>
)

/**
 * GST breakdown details
 */
data class GSTBreakdown(
    val cgst: BigDecimal,
    val sgst: BigDecimal,
    val igst: BigDecimal,
    val cess: BigDecimal,
    val total: BigDecimal
) {
    /**
     * Convenience property for total GST (same as total)
     */
    val totalGST: BigDecimal
        get() = total
    
    /**
     * Check if this is an inter-state transaction (IGST)
     */
    fun hasIGST(): Boolean = igst > BigDecimal.ZERO
    
    /**
     * Check if this is an intra-state transaction (CGST+SGST)
     */
    fun hasCGSTSGST(): Boolean = cgst > BigDecimal.ZERO || sgst > BigDecimal.ZERO
}

/**
 * Tax rate wise breakdown
 */
data class TaxRateBreakdown(
    val taxRate: Double,
    val taxableAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val transactionCount: Int,
    val percentage: Double
)

/**
 * Daily tax collection data
 */
data class DailyTaxData(
    val date: LocalDate,
    val taxableRevenue: BigDecimal,
    val taxAmount: BigDecimal,
    val taxableTransactions: Int,
    val nonTaxableTransactions: Int
)

/**
 * Monthly tax collection data
 */
data class MonthlyTaxData(
    val month: Int,
    val year: Int,
    val taxableRevenue: BigDecimal,
    val taxAmount: BigDecimal,
    val taxableTransactions: Int,
    val nonTaxableTransactions: Int
)

/**
 * Tax exempt items
 */
data class TaxExemptItemData(
    val productId: String,
    val productName: String,
    val revenue: BigDecimal,
    val units: Int,
    val exemptionReason: String
)

/**
 * Interstate transaction data
 */
data class InterstateTransactionData(
    val totalRevenue: BigDecimal,
    val totalTax: BigDecimal,
    val transactionCount: Int,
    val stateWiseBreakdown: List<StateWiseTaxData>
)

/**
 * Intrastate transaction data
 */
data class IntrastateTransactionData(
    val totalRevenue: BigDecimal,
    val totalCGST: BigDecimal,
    val totalSGST: BigDecimal,
    val transactionCount: Int
)

// Report Configuration and Filters

/**
 * Report filters for customization
 */
data class ReportFilters(
    val dateRange: DateRange,
    val categories: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val products: List<String> = emptyList(),
    val paymentMethods: List<String> = emptyList(),
    val customers: List<String> = emptyList(),
    val priceRange: ReportPriceRange? = null,
    val stockRange: StockRange? = null,
    val taxable: TaxableFilter = TaxableFilter.ALL,
    val sortBy: SortBy = SortBy.DATE,
    val sortOrder: SortOrder = SortOrder.DESC,
    val groupBy: GroupBy = GroupBy.DAY,
    val includeZeroSales: Boolean = false
)

/**
 * Report summary information
 */
data class ReportSummary(
    val totalRecords: Int,
    val totalRevenue: BigDecimal,
    val totalProfit: BigDecimal,
    val totalTax: BigDecimal,
    val averageOrderValue: BigDecimal,
    val profitMargin: Double,
    val keyInsights: List<String>,
    val recommendations: List<String>
)

/**
 * Chart data sets for visualizations
 */
data class ChartDataSet(
    val chartType: ChartType,
    val title: String,
    val xAxisLabel: String,
    val yAxisLabel: String,
    val series: List<ChartSeries>,
    val colors: List<String> = emptyList()
)

/**
 * Chart series data
 */
data class ChartSeries(
    val name: String,
    val data: List<ChartDataPoint>,
    val color: String? = null
)

/**
 * Chart data point
 */
data class ChartDataPoint(
    val label: String,
    val value: Double,
    val timestamp: LocalDateTime? = null
)

/**
 * Custom metrics for flexible reporting
 */
data class CustomMetric(
    val name: String,
    val value: BigDecimal,
    val unit: String,
    val trend: TrendDirection,
    val comparison: Double? = null
)

/**
 * Table data for reports
 */
data class TableData(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val totalRow: List<String>? = null
)

/**
 * KPI data for dashboards
 */
data class KPIData(
    val name: String,
    val value: String,
    val target: String? = null,
    val achievement: Double? = null,
    val trend: TrendDirection
)

// Supporting Data Types

/**
 * Price range filter
 */
data class ReportPriceRange(
    val min: BigDecimal,
    val max: BigDecimal
)

/**
 * Stock range filter
 */
data class StockRange(
    val min: Int,
    val max: Int
)

/**
 * Seasonality data
 */
data class SeasonalityData(
    val pattern: ReportSeasonalPattern,
    val peakMonths: List<Int>,
    val lowMonths: List<Int>,
    val seasonalityStrength: Double
)

/**
 * Peak period information
 */
data class PeakPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val sales: BigDecimal,
    val description: String
)

/**
 * Forecast data
 */
data class ForecastData(
    val date: LocalDate,
    val predictedSales: BigDecimal,
    val confidence: Double
)

/**
 * Margin range data
 */
data class MarginRangeData(
    val range: String,
    val productCount: Int,
    val revenue: BigDecimal,
    val percentage: Double
)

/**
 * Category cost data
 */
data class CategoryCostData(
    val category: String,
    val cost: BigDecimal,
    val percentage: Double
)

/**
 * Cost trend data
 */
data class CostTrendData(
    val date: LocalDate,
    val cost: BigDecimal,
    val trend: TrendDirection
)

/**
 * Category turnover data
 */
data class CategoryTurnoverData(
    val category: String,
    val turnoverRate: Double,
    val rank: Int
)

/**
 * State wise tax data
 */
data class StateWiseTaxData(
    val state: String,
    val revenue: BigDecimal,
    val taxAmount: BigDecimal,
    val transactionCount: Int
)

// Enums

/**
 * Report types available
 */
enum class ReportType {
    SALES,
    PROFIT,
    STOCK_AGING,
    TAX,
    INVENTORY,
    CUSTOMER,
    CUSTOM,
    COMBINED
}

/**
 * Export formats supported
 */
enum class ExportFormat {
    PDF,
    CSV,
    EXCEL,
    JSON
}

/**
 * Chart types for visualization
 */
enum class ChartType {
    LINE,
    BAR,
    PIE,
    DONUT,
    AREA,
    COLUMN,
    SCATTER,
    HEATMAP
}

/**
 * Stock age categories
 */
enum class StockAgeCategory {
    FRESH(0, 30),           // 0-30 days
    MODERATE(31, 60),       // 31-60 days
    AGING(61, 90),          // 61-90 days
    OLD(91, 180),           // 91-180 days
    DEAD(181, Int.MAX_VALUE); // 180+ days

    val minDays: Int
    val maxDays: Int

    constructor(minDays: Int, maxDays: Int) {
        this.minDays = minDays
        this.maxDays = maxDays
    }
}

/**
 * Stock actions for recommendations
 */
enum class StockAction {
    REORDER,
    DISCOUNT,
    RETURN,
    LIQUIDATE,
    MONITOR,
    NONE
}

/**
 * Reorder urgency levels
 */
enum class ReorderUrgency {
    CRITICAL,   // Stock out in 1-3 days
    HIGH,       // Stock out in 4-7 days
    MEDIUM,     // Stock out in 8-14 days
    LOW         // Stock out in 15+ days
}

/**
 * Taxable filter options
 */
enum class TaxableFilter {
    ALL,
    TAXABLE_ONLY,
    NON_TAXABLE_ONLY
}

/**
 * Sorting options
 */
enum class SortBy {
    DATE,
    AMOUNT,
    QUANTITY,
    PROFIT,
    NAME,
    CATEGORY,
    BRAND
}

/**
 * Sort order
 */
enum class SortOrder {
    ASC,
    DESC
}

/**
 * Grouping options
 */
enum class GroupBy {
    DAY,
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
    CATEGORY,
    BRAND,
    PRODUCT
}

/**
 * Seasonal patterns
 */
enum class ReportSeasonalPattern {
    NONE,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}