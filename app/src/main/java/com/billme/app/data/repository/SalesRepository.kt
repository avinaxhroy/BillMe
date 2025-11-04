package com.billme.app.data.repository

import com.billme.app.data.local.dao.*
import com.billme.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Duration
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for sales operations and analytics
 */
@Singleton
class SalesRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionLineItemDao: TransactionLineItemDao,
    private val productDao: ProductDao
) {
    
    /**
     * Get sales history for a specific product
     */
    suspend fun getSalesHistory(
        productId: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<SaleHistoryItem> {
        // Convert java.time.LocalDateTime to kotlinx.datetime.Instant via java.time Instant
        fun toKxInstant(ldt: LocalDateTime): Instant {
            val zone = ZoneId.systemDefault()
            val zdt = ldt.atZone(zone)
            return com.billme.app.util.DateTimeUtils.fromJavaInstant(zdt.toInstant())
        }

        val fromInstant = toKxInstant(fromDate)
        val toInstant = toKxInstant(toDate)
        
        // Get sales data for the product within date range
        val salesData = transactionLineItemDao.getProductSalesHistory(
            productId.toLongOrNull() ?: return emptyList(),
            fromInstant,
            toInstant
        )
        
        return salesData.map { item ->
            SaleHistoryItem(
                date = item.saleDate,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalAmount = item.totalAmount,
                profit = item.profit ?: BigDecimal.ZERO,
                // DAO exposes customerName; repository model expects a customerType string (legacy).
                // Use the customer name as a proxy for now, defaulting to "Regular" when absent.
                customerType = item.customerName ?: "Regular"
            )
        }
    }
    
    /**
     * Get total sales for a date range
     */
    suspend fun getTotalSales(startDate: Instant, endDate: Instant): BigDecimal {
        return transactionDao.getTotalSalesForPeriod(startDate, endDate)
    }
    
    /**
     * Get sales by category
     */
    suspend fun getSalesByCategory(startDate: Instant, endDate: Instant): List<CategorySales> {
        // Map DAO DTOs to repository models
        return transactionLineItemDao.getSalesByCategory(startDate, endDate).map { dao ->
            CategorySales(
                category = dao.category ?: "Uncategorized",
                totalSales = dao.totalSales,
                quantity = dao.quantity,
                averagePrice = dao.averagePrice
            )
        }
    }
    
    /**
     * Get best selling products
     */
    suspend fun getBestSellingProducts(limit: Int, startDate: Instant, endDate: Instant): List<ProductSales> {
        return transactionLineItemDao.getBestSellingProducts(limit, startDate, endDate).map { dao ->
            ProductSales(
                productId = dao.productId,
                productName = dao.productName,
                totalSales = dao.totalSales,
                quantitySold = dao.quantitySold,
                averagePrice = dao.averagePrice
            )
        }
    }
    
    /**
     * Get sales trend analysis
     */
    fun getSalesTrend(days: Int): Flow<List<DailySales>> {
        // Use java.time.Instant.now() and convert to kotlinx Instant for DAO calls
        val nowJava = java.time.Instant.now()
        val endDate = com.billme.app.util.DateTimeUtils.fromJavaInstant(nowJava)
        val startDate = com.billme.app.util.DateTimeUtils.fromJavaInstant(nowJava.minus(Duration.ofDays(days.toLong())))

        return transactionDao.getDailySalesForPeriod(startDate, endDate).map { sales ->
            sales.map { daily ->
                DailySales(
                    date = com.billme.app.util.DateTimeUtils.fromJavaInstant(java.time.Instant.parse(daily.date.toString())),
                    totalSales = daily.totalAmount,
                    transactionCount = daily.transactionCount,
                    averageOrderValue = if (daily.transactionCount > 0) {
                        daily.totalAmount.divide(BigDecimal.valueOf(daily.transactionCount.toLong()), 2, java.math.RoundingMode.HALF_UP)
                    } else BigDecimal.ZERO
                )
            }
        }
    }
    
    /**
     * Get sales performance metrics
     */
    suspend fun getSalesPerformance(startDate: Instant, endDate: Instant): SalesPerformance {
        val totalSales = getTotalSales(startDate, endDate)
        val transactionCount = transactionDao.getTransactionCountForPeriod(startDate, endDate)
        val averageOrderValue = if (transactionCount > 0) {
            totalSales.divide(BigDecimal.valueOf(transactionCount.toLong()), 2, java.math.RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        
        // Calculate growth compared to previous period
        val periodDuration = endDate.epochSeconds - startDate.epochSeconds
        val previousStartDate = Instant.fromEpochSeconds(startDate.epochSeconds - periodDuration)
        val previousEndDate = startDate
        
        val previousSales = getTotalSales(previousStartDate, previousEndDate)
        val growthPercentage = if (previousSales > BigDecimal.ZERO) {
            ((totalSales - previousSales) / previousSales * BigDecimal(100)).toDouble()
        } else 0.0
        
        return SalesPerformance(
            totalSales = totalSales,
            transactionCount = transactionCount,
            averageOrderValue = averageOrderValue,
            growthPercentage = growthPercentage
        )
    }
    
    /**
     * Get product sales history for the last N days - convenience method for pricing engine
     */
    suspend fun getProductSalesHistory(productId: Long, days: Int = 30): List<SaleHistoryItem> {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val fromDate = now.minusDays(days.toLong())
        return getSalesHistory(productId.toString(), fromDate, now)
    }
}

/**
 * Data classes for sales operations
 */
data class SaleHistoryItem(
    val date: Instant,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalAmount: BigDecimal,
    val profit: BigDecimal,
    val customerType: String
)

data class CategorySales(
    val category: String,
    val totalSales: BigDecimal,
    val quantity: Int,
    val averagePrice: BigDecimal
)

data class ProductSales(
    val productId: Long,
    val productName: String,
    val totalSales: BigDecimal,
    val quantitySold: Int,
    val averagePrice: BigDecimal
)

data class DailySales(
    val date: Instant,
    val totalSales: BigDecimal,
    val transactionCount: Int,
    val averageOrderValue: BigDecimal
)

data class SalesPerformance(
    val totalSales: BigDecimal,
    val transactionCount: Int,
    val averageOrderValue: BigDecimal,
    val growthPercentage: Double
)