package com.billme.app.data.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

/**
 * Data models for dashboard analytics and business metrics
 */

/**
 * Complete dashboard data container
 */
data class DashboardData(
    val businessMetrics: BusinessMetrics,
    val salesAnalytics: SalesAnalytics,
    val inventoryMetrics: InventoryMetrics,
    val topSellingModels: List<TopSellingModel>,
    val recentTransactions: List<RecentTransaction>,
    val stockAlerts: List<StockAlert>,
    val gstSummary: GSTSummary,
    val lastUpdated: Instant
)

/**
 * Core business metrics
 */
data class BusinessMetrics(
    val todaysSales: BigDecimal,
    val todaysProfit: BigDecimal,
    val todaysTransactions: Int,
    val monthlySales: BigDecimal,
    val monthlyProfit: BigDecimal,
    val monthlyTransactions: Int,
    val averageOrderValue: BigDecimal,
    val totalCustomers: Int,
    val activeCustomers: Int,
    val pendingPayments: BigDecimal,
    val lowStockItems: Int,
    val outOfStockItems: Int
)

/**
 * Sales analytics with trends
 */
data class SalesAnalytics(
    val dailySales: List<DashboardDailySalesData>,
    val monthlySales: List<DashboardMonthlySalesData>,
    val salesByCategory: List<CategorySalesData>,
    val salesTrend: SalesTrend,
    val topPaymentMethods: List<DashboardPaymentMethodData>,
    val peakSalesHours: List<DashboardHourlySalesData>
)

/**
 * Inventory and stock metrics
 */
data class InventoryMetrics(
    val totalStockValue: BigDecimal,
    val totalStockQuantity: Int,
    val categoryWiseStock: List<CategoryStockData>,
    val stockTurnoverRate: Double,
    val averageStockAge: Int, // in days
    val deadStock: List<DashboardDeadStockItem>,
    val fastMovingItems: List<DashboardFastMovingItem>,
    val stockValuation: StockValuation
)

/**
 * Top selling model analytics
 */
data class TopSellingModel(
    val productId: Long,
    val productName: String,
    val brand: String,
    val model: String,
    val category: String,
    val unitsSold: Int,
    val revenue: BigDecimal,
    val profit: BigDecimal,
    val profitMargin: Double,
    val averageSellingPrice: BigDecimal,
    val stockLevel: Int,
    val salesTrend: TrendDirection,
    val rank: Int,
    val previousRank: Int?
)

/**
 * Recent transaction summary
 */
data class RecentTransaction(
    val transactionId: Long,
    val customerName: String?,
    val customerPhone: String?,
    val amount: BigDecimal,
    val profit: BigDecimal,
    val itemCount: Int,
    val paymentMethod: String,
    val transactionTime: Instant,
    val status: TransactionStatus
)

/**
 * Stock alerts and notifications
 */
data class StockAlert(
    val productId: Long,
    val productName: String,
    val currentStock: Int,
    val reorderLevel: Int,
    val alertType: StockAlertType,
    val severity: AlertSeverity,
    val daysToStockout: Int?,
    val recommendedOrder: Int?
)

/**
 * GST summary for dashboard
 */
data class GSTSummary(
    val todayGSTCollected: BigDecimal,
    val monthlyGSTCollected: BigDecimal,
    val cgstAmount: BigDecimal,
    val sgstAmount: BigDecimal,
    val igstAmount: BigDecimal,
    val cessAmount: BigDecimal,
    val interstateTransactions: Int,
    val intrastateTransactions: Int,
    val gstReturn: GSTReturnSummary?
)

/**
 * Daily sales data point for dashboard
 */
data class DashboardDailySalesData(
    val date: LocalDate,
    val sales: BigDecimal,
    val profit: BigDecimal,
    val transactions: Int,
    val averageOrderValue: BigDecimal
)

/**
 * Monthly sales data point for dashboard
 */
data class DashboardMonthlySalesData(
    val month: String,
    val year: Int,
    val sales: BigDecimal,
    val profit: BigDecimal,
    val transactions: Int,
    val growthRate: Double
)

/**
 * Category-wise sales data
 */
data class CategorySalesData(
    val category: String,
    val sales: BigDecimal,
    val profit: BigDecimal,
    val units: Int,
    val percentage: Double,
    val trend: TrendDirection
)

/**
 * Sales trend analysis
 */
data class SalesTrend(
    val direction: TrendDirection,
    val percentage: Double,
    val period: String,
    val description: String
)

/**
 * Payment method analytics for dashboard
 */
data class DashboardPaymentMethodData(
    val method: String,
    val amount: BigDecimal,
    val transactions: Int,
    val percentage: Double
)

/**
 * Hourly sales pattern for dashboard
 */
data class DashboardHourlySalesData(
    val hour: Int,
    val sales: BigDecimal,
    val transactions: Int,
    val averageValue: BigDecimal
)

/**
 * Category stock information
 */
data class CategoryStockData(
    val category: String,
    val totalValue: BigDecimal,
    val totalQuantity: Int,
    val averagePrice: BigDecimal,
    val lowStockItems: Int,
    val outOfStockItems: Int
)

/**
 * Dead stock item for dashboard
 */
data class DashboardDeadStockItem(
    val productId: Long,
    val productName: String,
    val stockQuantity: Int,
    val stockValue: BigDecimal,
    val lastSoldDate: LocalDate?,
    val daysWithoutSale: Int,
    val recommendedAction: String
)

/**
 * Fast moving item for dashboard
 */
data class DashboardFastMovingItem(
    val productId: Long,
    val productName: String,
    val salesVelocity: Double, // units per day
    val stockLevel: Int,
    val daysToStockout: Int,
    val reorderRecommended: Boolean
)

/**
 * Stock valuation breakdown
 */
data class StockValuation(
    val totalCostValue: BigDecimal,
    val totalSellingValue: BigDecimal,
    val potentialProfit: BigDecimal,
    val averageMarkup: Double,
    val categoryBreakdown: List<CategoryValuation>
)

/**
 * Category valuation
 */
data class CategoryValuation(
    val category: String,
    val costValue: BigDecimal,
    val sellingValue: BigDecimal,
    val profit: BigDecimal,
    val markup: Double,
    val percentage: Double
)

/**
 * GST return summary
 */
data class GSTReturnSummary(
    val period: String,
    val dueDate: LocalDate,
    val status: GSTReturnStatus,
    val totalTaxableAmount: BigDecimal,
    val totalGSTAmount: BigDecimal,
    val isOverdue: Boolean,
    val daysRemaining: Int
)

/**
 * Performance comparison data
 */
data class PerformanceComparison(
    val currentPeriod: PerformanceData,
    val previousPeriod: PerformanceData,
    val growthMetrics: GrowthMetrics
)

/**
 * Performance data for a period
 */
data class PerformanceData(
    val periodName: String,
    val sales: BigDecimal,
    val profit: BigDecimal,
    val transactions: Int,
    val customers: Int,
    val averageOrderValue: BigDecimal
)

/**
 * Growth metrics
 */
data class GrowthMetrics(
    val salesGrowth: Double,
    val profitGrowth: Double,
    val transactionGrowth: Double,
    val customerGrowth: Double,
    val aovGrowth: Double
)

/**
 * Customer analytics
 */
data class CustomerAnalytics(
    val totalCustomers: Int,
    val newCustomers: Int,
    val returningCustomers: Int,
    val topCustomers: List<TopCustomer>,
    val customerSegments: List<CustomerSegment>,
    val averageCustomerValue: BigDecimal,
    val customerRetentionRate: Double
)

/**
 * Top customer data
 */
data class TopCustomer(
    val customerId: Long,
    val customerName: String?,
    val phoneNumber: String,
    val totalPurchases: BigDecimal,
    val totalTransactions: Int,
    val lastPurchaseDate: LocalDate,
    val averageOrderValue: BigDecimal,
    val segment: String
)

/**
 * Customer segment data
 */
data class CustomerSegment(
    val segmentName: String,
    val customerCount: Int,
    val totalSales: BigDecimal,
    val averageSpend: BigDecimal,
    val percentage: Double
)

/**
 * Profit analysis
 */
data class ProfitAnalysis(
    val grossProfit: BigDecimal,
    val netProfit: BigDecimal,
    val profitMargin: Double,
    val categoryProfit: List<CategoryProfitData>,
    val profitTrend: List<ProfitTrendData>,
    val profitability: ProfitabilityMetrics
)

/**
 * Category profit data for dashboard
 */
data class DashboardCategoryProfitData(
    val category: String,
    val grossProfit: BigDecimal,
    val profitMargin: Double,
    val contribution: Double
)

/**
 * Profit trend data
 */
data class ProfitTrendData(
    val date: LocalDate,
    val grossProfit: BigDecimal,
    val netProfit: BigDecimal,
    val margin: Double
)

/**
 * Profitability metrics
 */
data class ProfitabilityMetrics(
    val mostProfitableCategory: String,
    val leastProfitableCategory: String,
    val averageMargin: Double,
    val marginTrend: TrendDirection
)

/**
 * Enums for dashboard data
 */

enum class TrendDirection {
    UP, DOWN, STABLE, UNKNOWN
}

enum class StockAlertType {
    LOW_STOCK, OUT_OF_STOCK, OVERSTOCK, EXPIRING, DEAD_STOCK
}

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class TransactionStatus {
    COMPLETED, PENDING, CANCELLED, REFUNDED
}

enum class GSTReturnStatus {
    NOT_DUE, PENDING, FILED, OVERDUE
}

/**
 * Dashboard filter options
 */
data class DashboardFilter(
    val dateRange: DateRange,
    val categories: List<String>? = null,
    val brands: List<String>? = null,
    val paymentMethods: List<String>? = null,
    val customerSegments: List<String>? = null,
    val includeReturns: Boolean = true
)

/**
 * Date range for filtering
 */
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val preset: DateRangePreset? = null
)

enum class DateRangePreset {
    TODAY, YESTERDAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH, 
    THIS_QUARTER, LAST_QUARTER, THIS_YEAR, LAST_YEAR, CUSTOM
}

/**
 * Dashboard refresh state
 */
data class DashboardRefreshState(
    val isRefreshing: Boolean,
    val lastRefresh: Instant?,
    val autoRefreshEnabled: Boolean,
    val refreshIntervalMinutes: Int,
    val error: String? = null
)