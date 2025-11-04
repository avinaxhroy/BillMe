package com.billme.app.data.repository

import com.billme.app.data.local.dao.*
import com.billme.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import java.time.ZoneId
import java.time.LocalDate as JLocalDate
import java.time.Duration as JDuration
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for real-time business metrics and analytics
 */
@Singleton
class BusinessMetricsRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionLineItemDao: TransactionLineItemDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val gstRepository: GSTRepository
) {
    
    /**
     * Get real-time business metrics
     */
    fun getBusinessMetrics(): Flow<BusinessMetrics> = flow {
        // Use java.time LocalDate for calculations and convert to kotlinx Instant when calling DAOs
    val zone = ZoneId.systemDefault()
    val today = JLocalDate.now(zone)
    val todayStartKx = com.billme.app.util.DateTimeUtils.fromJavaInstant(today.atStartOfDay(zone).toInstant())
    val todayEndKx = com.billme.app.util.DateTimeUtils.addDays(todayStartKx, 1)

    val monthStartDate = today.withDayOfMonth(1)
    val monthStartKx = com.billme.app.util.DateTimeUtils.fromJavaInstant(monthStartDate.atStartOfDay(zone).toInstant())
        
        // Today's metrics
    val todaysSales = transactionDao.getTotalSalesForPeriod(todayStartKx, todayEndKx)
    val todaysProfit = transactionDao.getTotalProfitForPeriod(todayStartKx, todayEndKx)
    val todaysTransactions = transactionDao.getTransactionCountForPeriod(todayStartKx, todayEndKx)
        
        // Monthly metrics
    val monthlySales = transactionDao.getTotalSalesForPeriod(monthStartKx, todayEndKx)
    val monthlyProfit = transactionDao.getTotalProfitForPeriod(monthStartKx, todayEndKx)
    val monthlyTransactions = transactionDao.getTransactionCountForPeriod(monthStartKx, todayEndKx)
        
        // Other metrics
        val averageOrderValue = if (todaysTransactions > 0) {
            todaysSales.divide(BigDecimal.valueOf(todaysTransactions.toLong()), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        
    val totalCustomers = customerDao.getTotalCustomerCount()
    val activeCustomers = customerDao.getActiveCustomerCount(monthStartKx)
        val pendingPayments = transactionDao.getPendingPayments()
        val lowStockItems = productDao.getLowStockItemCount()
        val outOfStockItems = productDao.getOutOfStockItemCount()
        
        emit(BusinessMetrics(
            todaysSales = todaysSales,
            todaysProfit = todaysProfit,
            todaysTransactions = todaysTransactions,
            monthlySales = monthlySales,
            monthlyProfit = monthlyProfit,
            monthlyTransactions = monthlyTransactions,
            averageOrderValue = averageOrderValue,
            totalCustomers = totalCustomers,
            activeCustomers = activeCustomers,
            pendingPayments = pendingPayments,
            lowStockItems = lowStockItems,
            outOfStockItems = outOfStockItems
        ))
    }
    
    /**
     * Get sales analytics with trends
     */
    suspend fun getSalesAnalytics(dateRange: DateRange): SalesAnalytics {
        val dailySales = getDailySalesData(dateRange)
        val monthlySales = getMonthlySalesData(dateRange)
        val salesByCategory = getCategorySalesData(dateRange)
        val salesTrend = calculateSalesTrend(dailySales)
        val topPaymentMethods = getPaymentMethodData(dateRange)
        val peakSalesHours = getHourlySalesData(dateRange)
        
        return SalesAnalytics(
            dailySales = dailySales,
            monthlySales = monthlySales,
            salesByCategory = salesByCategory,
            salesTrend = salesTrend,
            topPaymentMethods = topPaymentMethods,
            peakSalesHours = peakSalesHours
        )
    }
    
    /**
     * Get inventory metrics and stock analysis
     */
    suspend fun getInventoryMetrics(): InventoryMetrics {
        val totalStockValue = productDao.getTotalStockValue()
        val totalStockQuantity = productDao.getTotalStockQuantity()
        val categoryWiseStock = emptyList<CategoryStockData>() // TODO: implement
        val stockTurnoverRate = 0.0 // TODO: implement
        val averageStockAge = 0 // TODO: implement
        val deadStock = emptyList<DashboardDeadStockItem>() // TODO: implement
        val fastMovingItems = emptyList<DashboardFastMovingItem>() // TODO: implement
        val stockValuation = StockValuation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, emptyList()) // TODO: implement
        
        return InventoryMetrics(
            totalStockValue = totalStockValue,
            totalStockQuantity = totalStockQuantity,
            categoryWiseStock = categoryWiseStock,
            stockTurnoverRate = stockTurnoverRate,
            averageStockAge = averageStockAge,
            deadStock = deadStock,
            fastMovingItems = fastMovingItems,
            stockValuation = stockValuation
        )
    }
    
    /**
     * Get top selling models with analytics
     */
    suspend fun getTopSellingModels(limit: Int = 20, dateRange: DateRange): List<TopSellingModel> {
        return transactionLineItemDao.getTopSellingProducts(
            limit = limit,
            startDate = com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dateRange.startDate),
            endDate = com.billme.app.util.DateTimeUtils.addDays(com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dateRange.endDate), 1)
        ).mapIndexed { index, item ->
            val previousRank = getPreviousRank(item.productId, dateRange)
            val salesTrend = calculateProductSalesTrend(item.productId, dateRange)
            
            TopSellingModel(
                productId = item.productId,
                productName = item.productName,
                brand = item.brand ?: "Unknown",
                model = item.model ?: "Unknown",
                category = item.category ?: "Unknown",
                unitsSold = item.totalQuantity.toInt(),
                revenue = item.totalRevenue,
                profit = item.totalProfit,
                profitMargin = calculateProfitMargin(item.totalProfit, item.totalRevenue),
                averageSellingPrice = item.averagePrice,
                stockLevel = item.currentStock,
                salesTrend = salesTrend,
                rank = index + 1,
                previousRank = previousRank
            )
        }
    }
    
    /**
     * Get recent transactions
     */
    fun getRecentTransactions(limit: Int = 10): Flow<List<RecentTransaction>> = flow {
        val transactions = transactionDao.getRecentTransactions(limit).first()
        
        val recentTransactions = transactions.map { transaction ->
            val itemCount = transactionLineItemDao.getItemCountForTransaction(transaction.transactionId)
            
            RecentTransaction(
                transactionId = transaction.transactionId,
                customerName = transaction.customerName,
                customerPhone = transaction.customerPhone,
                amount = transaction.grandTotal,
                profit = transaction.profitAmount,
                itemCount = itemCount,
                paymentMethod = transaction.paymentMethod.name,
                transactionTime = transaction.createdAt,
                status = when (transaction.paymentStatus) {
                    com.billme.app.data.local.entity.PaymentStatus.PAID -> TransactionStatus.COMPLETED
                    com.billme.app.data.local.entity.PaymentStatus.PENDING -> TransactionStatus.PENDING
                    com.billme.app.data.local.entity.PaymentStatus.PARTIALLY_PAID -> TransactionStatus.PENDING
                }
            )
        }
        
        emit(recentTransactions)
    }
    
    /**
     * Get stock alerts
     */
    suspend fun getStockAlerts(): List<StockAlert> {
        val lowStockProducts = productDao.getLowStockProducts().first()
        val outOfStockProducts = productDao.getOutOfStockProducts()
        val deadStockProducts = emptyList<com.billme.app.data.model.DashboardDeadStockItem>() // TODO: implement
        
        val alerts = mutableListOf<StockAlert>()
        
        // Low stock alerts
        for (product in lowStockProducts) {
            val daysToStockout = calculateDaysToStockout(product.productId, product.currentStock)
            alerts.add(StockAlert(
                productId = product.productId,
                productName = product.productName,
                currentStock = product.currentStock,
                reorderLevel = product.minStockLevel,
                alertType = StockAlertType.LOW_STOCK,
                severity = when {
                    product.currentStock <= 5 -> AlertSeverity.CRITICAL
                    product.currentStock <= 10 -> AlertSeverity.HIGH
                    else -> AlertSeverity.MEDIUM
                },
                daysToStockout = daysToStockout,
                recommendedOrder = calculateRecommendedOrder(product.productId)
            ))
        }
        
        // Out of stock alerts
        for (product in outOfStockProducts) {
            alerts.add(StockAlert(
                productId = product.productId,
                productName = product.productName,
                currentStock = 0,
                reorderLevel = product.minStockLevel,
                alertType = StockAlertType.OUT_OF_STOCK,
                severity = AlertSeverity.CRITICAL,
                daysToStockout = 0,
                recommendedOrder = calculateRecommendedOrder(product.productId)
            ))
        }
        
        // Dead stock alerts
        for (deadStock in deadStockProducts) {
            alerts.add(StockAlert(
                productId = deadStock.productId,
                productName = deadStock.productName,
                currentStock = deadStock.stockQuantity,
                reorderLevel = 0,
                alertType = StockAlertType.DEAD_STOCK,
                severity = AlertSeverity.LOW,
                daysToStockout = null,
                recommendedOrder = null
            ))
        }
        
        return alerts.sortedByDescending { it.severity }
    }
    
    /**
     * Get GST summary for dashboard
     */
    suspend fun getGSTSummary(): com.billme.app.data.model.GSTSummary {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    val todayStart = com.billme.app.util.DateTimeUtils.fromJavaInstant(today.atStartOfDay(zone).toInstant())
    val todayEnd = com.billme.app.util.DateTimeUtils.addDays(todayStart, 1)

    val monthStartDate = today.withDayOfMonth(1)
    val monthStart = com.billme.app.util.DateTimeUtils.fromJavaInstant(monthStartDate.atStartOfDay(zone).toInstant())
        
        val gstConfig = gstRepository.getCurrentGSTConfigurationSync()
        val shopGSTIN = gstConfig?.shopGSTIN ?: ""
        
        val todayGST = gstRepository.getTotalGSTForPeriod(shopGSTIN, todayStart, todayEnd)
        val monthlyGST = gstRepository.getTotalGSTForPeriod(shopGSTIN, monthStart, todayEnd)
        val gstBreakdown = gstRepository.getGSTBreakdownForPeriod(shopGSTIN, monthStart, todayEnd)
        val transactionCounts = gstRepository.getGSTTransactionCountForPeriod(monthStart, todayEnd)
        
        return com.billme.app.data.model.GSTSummary(
            todayGSTCollected = BigDecimal.valueOf(todayGST),
            monthlyGSTCollected = BigDecimal.valueOf(monthlyGST),
            cgstAmount = gstBreakdown?.let { BigDecimal.valueOf(it.totalCgst) } ?: BigDecimal.ZERO,
            sgstAmount = gstBreakdown?.let { BigDecimal.valueOf(it.totalSgst) } ?: BigDecimal.ZERO,
            igstAmount = gstBreakdown?.let { BigDecimal.valueOf(it.totalIgst) } ?: BigDecimal.ZERO,
            cessAmount = gstBreakdown?.let { BigDecimal.valueOf(it.totalCess) } ?: BigDecimal.ZERO,
            interstateTransactions = 0, // TODO: calculate
            intrastateTransactions = transactionCounts,
            gstReturn = getGSTReturnSummary()
        )
    }
    
    /**
     * Get customer analytics
     */
    suspend fun getCustomerAnalytics(dateRange: DateRange): CustomerAnalytics {
        val totalCustomers = customerDao.getTotalCustomerCount()
        val newCustomers = customerDao.getNewCustomersInPeriod(
            com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dateRange.startDate),
            com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dateRange.endDate)
        )
        val returningCustomers = customerDao.getReturningCustomersInPeriod(
            com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dateRange.startDate),
            com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dateRange.endDate)
        )
        val topCustomers = getTopCustomers(limit = 10)
        val customerSegments = getCustomerSegments()
        val averageCustomerValue = calculateAverageCustomerValue()
        val retentionRate = calculateCustomerRetentionRate()
        
        return CustomerAnalytics(
            totalCustomers = totalCustomers,
            newCustomers = newCustomers,
            returningCustomers = returningCustomers,
            topCustomers = topCustomers,
            customerSegments = customerSegments,
            averageCustomerValue = averageCustomerValue,
            customerRetentionRate = retentionRate
        )
    }
    
    // Private helper methods
    
    private suspend fun getDailySalesData(dateRange: DateRange): List<com.billme.app.data.model.DashboardDailySalesData> {
        val result = mutableListOf<com.billme.app.data.model.DashboardDailySalesData>()
        var currentDate = dateRange.startDate
        
        while (currentDate <= dateRange.endDate) {
            val dateInstant = com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(currentDate)
            val sales = transactionDao.getDailySales(dateInstant)
            val profit = transactionDao.getDailyProfit(dateInstant)
            val transactions = transactionDao.getDailyTransactionCount(dateInstant)
            val averageOrderValue = if (transactions > 0) sales / BigDecimal(transactions) else BigDecimal.ZERO
            
            result.add(com.billme.app.data.model.DashboardDailySalesData(
                date = currentDate,
                sales = sales,
                profit = profit,
                transactions = transactions,
                averageOrderValue = averageOrderValue
            ))
            
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        return result
    }
    
    private suspend fun getMonthlySalesData(dateRange: DateRange): List<DashboardMonthlySalesData> {
        val result = mutableListOf<DashboardMonthlySalesData>()
        var currentDate = dateRange.startDate
        
        // Group by month
        val monthlyData = mutableMapOf<Pair<Int, Int>, MutableList<LocalDate>>()
        
        while (currentDate <= dateRange.endDate) {
            val key = Pair(currentDate.year, currentDate.monthNumber)
            monthlyData.getOrPut(key) { mutableListOf() }.add(currentDate)
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        for ((monthYear, dates) in monthlyData) {
            val (year, month) = monthYear
            val monthStart = com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dates.first())
            val monthEnd = com.billme.app.util.DateTimeUtils.addDays(com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dates.last()), 1)
            
            val sales = transactionDao.getTotalSalesForPeriod(monthStart, monthEnd)
            val profit = transactionDao.getTotalProfitForPeriod(monthStart, monthEnd)
            val transactions = transactionDao.getTransactionCountForPeriod(monthStart, monthEnd)
            
            result.add(DashboardMonthlySalesData(
                month = kotlinx.datetime.Month(month).name,
                year = year,
                sales = sales,
                profit = profit,
                transactions = transactions,
                growthRate = 0.0 // TODO: calculate growth rate
            ))
        }
        
        return result.sortedBy { it.year * 12 + kotlinx.datetime.Month.valueOf(it.month).ordinal }
    }
    
    private suspend fun getCategorySalesData(dateRange: DateRange): List<com.billme.app.data.model.CategorySalesData> {
        val daoData = transactionLineItemDao.getCategorySalesData(
            com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dateRange.startDate),
            com.billme.app.util.DateTimeUtils.fromKxLocalDateStartOfDay(dateRange.endDate)
        )
        
        return daoData.map { daoItem ->
            com.billme.app.data.model.CategorySalesData(
                category = daoItem.category ?: "Uncategorized",
                sales = daoItem.totalSales,
                profit = BigDecimal.ZERO, // TODO: calculate profit
                units = 0, // TODO: calculate units
                percentage = 0.0, // TODO: calculate percentage
                trend = TrendDirection.STABLE
            )
        }
    }
    
    private fun calculateSalesTrend(dailySales: List<com.billme.app.data.model.DashboardDailySalesData>): SalesTrend {
        if (dailySales.size < 2) {
            return SalesTrend(TrendDirection.STABLE, 0.0, "Insufficient data", "Not enough data to determine trend")
        }
        
        val recentSales = dailySales.takeLast(7).sumOf { it.sales }
        val previousSales = dailySales.dropLast(7).takeLast(7).sumOf { it.sales }
        
        return when {
            previousSales == BigDecimal.ZERO -> SalesTrend(TrendDirection.STABLE, 0.0, "7 days", "No previous data for comparison")
            else -> {
                val growthRate = ((recentSales - previousSales) / previousSales * BigDecimal.valueOf(100.0)).toDouble()
                val direction = when {
                    growthRate > 5.0 -> TrendDirection.UP
                    growthRate < -5.0 -> TrendDirection.DOWN
                    else -> TrendDirection.STABLE
                }
                SalesTrend(direction, growthRate, "7 days", "Compared to previous 7 days")
            }
        }
    }
    
    private suspend fun getPaymentMethodData(dateRange: DateRange): List<DashboardPaymentMethodData> {
        // TODO: Implement payment method breakdown
        return emptyList()
    }
    
    private suspend fun getHourlySalesData(dateRange: DateRange): List<DashboardHourlySalesData> {
        // TODO: Implement hourly sales data
        return emptyList()
    }
    
    private suspend fun getPreviousRank(productId: Long, dateRange: DateRange): Int? {
        // Calculate rank for previous period of same length
        val periodLength = (dateRange.endDate.toEpochDays() - dateRange.startDate.toEpochDays()).toInt()
        val previousStart = dateRange.startDate.plus(DatePeriod(days = -periodLength))
        val previousEnd = dateRange.startDate.plus(DatePeriod(days = -1))
        
        val previousPeriodRange = DateRange(previousStart, previousEnd)
        val previousTopSellers = getTopSellingModels(50, previousPeriodRange)
        
        return previousTopSellers.find { it.productId == productId }?.rank
    }
    
    private suspend fun calculateProductSalesTrend(productId: Long, dateRange: DateRange): TrendDirection {
        // Convert LocalDate to Instant directly
        val currentStartInstant: Instant = Instant.fromEpochSeconds(
            dateRange.startDate.toEpochDays() * 86400L
        )
        val currentEndInstant: Instant = Instant.fromEpochSeconds(
            dateRange.endDate.toEpochDays() * 86400L
        )
        val currentSales = transactionLineItemDao.getProductSalesForPeriod(productId, 
            currentStartInstant,
            currentEndInstant
        )
        
        val periodLength = (dateRange.endDate.toEpochDays() - dateRange.startDate.toEpochDays()).toInt()
        val previousStart = dateRange.startDate.plus(DatePeriod(days = -periodLength))
        val previousStartInstant: Instant = Instant.fromEpochSeconds(
            previousStart.toEpochDays() * 86400L
        )
        val previousEndInstant: Instant = Instant.fromEpochSeconds(
            dateRange.startDate.plus(DatePeriod(days = -1)).toEpochDays() * 86400L
        )
        val previousSales = transactionLineItemDao.getProductSalesForPeriod(productId,
            previousStartInstant,
            previousEndInstant
        )
        
        return when {
            previousSales == BigDecimal.ZERO && currentSales > BigDecimal.ZERO -> TrendDirection.UP
            currentSales > previousSales -> TrendDirection.UP
            currentSales < previousSales -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }
    }
    
    private fun calculateProfitMargin(profit: BigDecimal, revenue: BigDecimal): Double {
        return if (revenue > BigDecimal.ZERO) {
            (profit / revenue * BigDecimal.valueOf(100.0)).toDouble()
        } else 0.0
    }
    
    private suspend fun calculateDaysToStockout(productId: Long, currentStock: Int): Int? {
        val salesVelocity = transactionLineItemDao.getProductSalesVelocity(productId, 30) // Last 30 days
        return if (salesVelocity > 0) {
            (currentStock / salesVelocity).toInt()
        } else null
    }
    
    private suspend fun calculateRecommendedOrder(productId: Long): Int {
        val salesVelocity = transactionLineItemDao.getProductSalesVelocity(productId, 30)
        val leadTime = 7 // Assume 7 days lead time
        return (salesVelocity * leadTime * 1.2).toInt() // 20% safety stock
    }
    
    private suspend fun getGSTReturnSummary(): GSTReturnSummary? {
        // This would depend on GST return filing requirements
        // For now, return a placeholder
        return null
    }
    
    private suspend fun getTopCustomers(limit: Int): List<com.billme.app.data.model.TopCustomer> {
        val daoCustomers = customerDao.getTopCustomersData(limit)
        return daoCustomers.map { daoCustomer ->
            com.billme.app.data.model.TopCustomer(
                customerId = daoCustomer.customerId,
                customerName = daoCustomer.customerName,
                phoneNumber = daoCustomer.phoneNumber ?: "",
                totalPurchases = daoCustomer.totalPurchases,
                totalTransactions = daoCustomer.totalTransactions,
                lastPurchaseDate = daoCustomer.lastPurchaseDate?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
                    ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                averageOrderValue = if (daoCustomer.totalTransactions > 0) {
                    daoCustomer.totalPurchases.divide(BigDecimal(daoCustomer.totalTransactions), 2, RoundingMode.HALF_UP)
                } else BigDecimal.ZERO,
                segment = "Top Customer" // TODO: determine segment based on purchase history
            )
        }
    }
    
    private suspend fun getCustomerSegments(): List<com.billme.app.data.model.CustomerSegment> {
        return customerDao.getCustomerSegments().map { daoSegment ->
            com.billme.app.data.model.CustomerSegment(
                segmentName = daoSegment.segment,
                customerCount = daoSegment.count,
                totalSales = BigDecimal.ZERO, // TODO: calculate total sales per segment
                averageSpend = BigDecimal.ZERO, // TODO: calculate average spend per segment
                percentage = 0.0 // TODO: calculate percentage
            )
        }
    }
    
    private suspend fun calculateAverageCustomerValue(): BigDecimal {
        return customerDao.getAverageCustomerValue()
    }
    
    private suspend fun calculateCustomerRetentionRate(): Double {
        val targetDate = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.plus(DatePeriod(days = -365))
        val oneYearAgoDt: Instant = Instant.fromEpochSeconds(
            targetDate.toEpochDays() * 86400L
        )
        return customerDao.getCustomerRetentionRate(oneYearAgoDt)
    }
}
