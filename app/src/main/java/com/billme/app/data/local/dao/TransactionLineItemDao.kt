package com.billme.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.billme.app.data.local.entity.TransactionLineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionLineItemDao {
    
    @Query("SELECT * FROM transaction_line_items WHERE transaction_id = :transactionId ORDER BY line_item_id")
    fun getLineItemsByTransactionId(transactionId: Long): Flow<List<TransactionLineItem>>
    
    @Query("SELECT * FROM transaction_line_items WHERE transaction_id = :transactionId")
    suspend fun getLineItemsByTransactionIdSync(transactionId: Long): List<TransactionLineItem>
    
    @Query("SELECT * FROM transaction_line_items WHERE product_id = :productId ORDER BY line_item_id DESC")
    fun getLineItemsByProductId(productId: Long): Flow<List<TransactionLineItem>>
    
    @Query("SELECT * FROM transaction_line_items WHERE imei_sold = :imei")
    suspend fun getLineItemByImei(imei: String): TransactionLineItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItem(lineItem: TransactionLineItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(lineItems: List<TransactionLineItem>)
    
    @Update
    suspend fun updateLineItem(lineItem: TransactionLineItem)
    
    @Delete
    suspend fun deleteLineItem(lineItem: TransactionLineItem)
    
    @Query("DELETE FROM transaction_line_items WHERE transaction_id = :transactionId")
    suspend fun deleteLineItemsByTransactionId(transactionId: Long)
    
    // Get product sales summary
    @Query("""
        SELECT product_id, product_name, SUM(quantity) as total_sold, SUM(line_total) as total_revenue, SUM(line_profit) as total_profit
        FROM transaction_line_items 
        GROUP BY product_id 
        ORDER BY total_sold DESC
    """)
    suspend fun getProductSalesSummary(): List<ProductSalesSummary>
    
    // Analytics methods for BusinessMetricsRepository
    @Query("""
        SELECT tli.product_id, tli.product_name, p.brand, p.model, p.category, 
               SUM(tli.quantity) as totalQuantity, SUM(tli.line_total) as totalRevenue, 
               SUM(tli.line_profit) as totalProfit, AVG(tli.unit_selling_price) as averagePrice,
               p.current_stock as currentStock
        FROM transaction_line_items tli 
        LEFT JOIN products p ON tli.product_id = p.product_id
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        WHERE t.created_at BETWEEN :startDate AND :endDate
        GROUP BY tli.product_id 
        ORDER BY totalQuantity DESC 
        LIMIT :limit
    """)
    suspend fun getTopSellingProducts(limit: Int, startDate: kotlinx.datetime.Instant, endDate: kotlinx.datetime.Instant): List<TopSellingProductData>
    
    @Query("SELECT COUNT(*) FROM transaction_line_items WHERE transaction_id = :transactionId")
    suspend fun getItemCountForTransaction(transactionId: Long): Int
    
    @Query("""
        SELECT p.category, SUM(tli.line_total) as totalSales, COUNT(*) as transactionCount
        FROM transaction_line_items tli
        LEFT JOIN products p ON tli.product_id = p.product_id
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        WHERE t.created_at BETWEEN :startDate AND :endDate
        GROUP BY p.category
        ORDER BY totalSales DESC
    """)
    suspend fun getCategorySalesData(startDate: kotlinx.datetime.Instant, endDate: kotlinx.datetime.Instant): List<CategorySalesData>
    
    @Query("""
        SELECT SUM(tli.line_total) as totalSales
        FROM transaction_line_items tli
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        WHERE tli.product_id = :productId AND t.created_at BETWEEN :startDate AND :endDate
    """)
    suspend fun getProductSalesForPeriod(productId: Long, startDate: kotlinx.datetime.Instant, endDate: kotlinx.datetime.Instant): java.math.BigDecimal
    
    suspend fun getProductSalesVelocity(productId: Long, days: Int): Double {
        // Use DateTimeUtils to compute start-of-day instant for (today - days).
        val fromDate = com.billme.app.util.DateTimeUtils.startOfDayInstantDaysAgo(days)
        return getProductSalesVelocityFromDate(productId, fromDate)
    }
    
    @Query("""
        SELECT AVG(tli.quantity) as velocity
        FROM transaction_line_items tli
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        WHERE tli.product_id = :productId AND t.created_at >= :fromDate
    """)
    suspend fun getProductSalesVelocityFromDate(productId: Long, fromDate: kotlinx.datetime.Instant): Double
    
    // Additional methods for other repositories
    @Query("SELECT COUNT(*) > 0 FROM transaction_line_items WHERE imei_sold = :imei")
    suspend fun checkIMEIExists(imei: String): Boolean
    
    @Query("""
        SELECT tli.line_item_id, tli.transaction_id, tli.product_name, t.customer_name, t.created_at, t.grand_total as totalAmount
        FROM transaction_line_items tli
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        WHERE tli.imei_sold = :imei
        ORDER BY t.created_at DESC
    """)
    suspend fun getTransactionsByIMEI(imei: String): List<TransactionWithLineItem>
    
    @Query("""
        SELECT tli.imei_sold as imei, tli.product_name, t.created_at as saleDate, 
               tli.line_total as salePrice, t.customer_name
        FROM transaction_line_items tli
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        WHERE t.created_at BETWEEN :startDate AND :endDate AND tli.imei_sold IS NOT NULL
        ORDER BY t.created_at DESC
    """)
    suspend fun getDeviceSalesInRange(startDate: kotlinx.datetime.Instant, endDate: kotlinx.datetime.Instant): List<DeviceSaleData>
    
    @Query("""
        SELECT tli.line_item_id, tli.quantity, tli.unit_selling_price, tli.line_total, t.created_at as saleDate, t.customer_name,
               (tli.line_total - (tli.quantity * CAST(p.cost_price as DECIMAL))) as profit
        FROM transaction_line_items tli
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        LEFT JOIN products p ON tli.product_id = p.product_id
        WHERE tli.product_id = :productId AND t.created_at BETWEEN :fromDate AND :toDate
        ORDER BY t.created_at DESC
    """)
    suspend fun getProductSalesHistory(productId: Long, fromDate: kotlinx.datetime.Instant, toDate: kotlinx.datetime.Instant): List<ProductSalesHistoryData>
    
    @Query("""
        SELECT p.category, SUM(tli.line_total) as totalSales, SUM(tli.quantity) as quantity,
               AVG(tli.unit_selling_price) as averagePrice
        FROM transaction_line_items tli
        LEFT JOIN products p ON tli.product_id = p.product_id
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        WHERE t.created_at BETWEEN :startDate AND :endDate
        GROUP BY p.category
        ORDER BY totalSales DESC
    """)
    suspend fun getSalesByCategory(startDate: kotlinx.datetime.Instant, endDate: kotlinx.datetime.Instant): List<CategorySalesInfo>
    
    @Query("""
        SELECT tli.product_id, tli.product_name, SUM(tli.line_total) as totalSales,
               SUM(tli.quantity) as quantitySold, AVG(tli.unit_selling_price) as averagePrice
        FROM transaction_line_items tli
        LEFT JOIN transactions t ON tli.transaction_id = t.transaction_id
        WHERE t.created_at BETWEEN :startDate AND :endDate
        GROUP BY tli.product_id
        ORDER BY totalSales DESC
        LIMIT :limit
    """)
    suspend fun getBestSellingProducts(limit: Int, startDate: kotlinx.datetime.Instant, endDate: kotlinx.datetime.Instant): List<ProductSalesInfo>
}

data class ProductSalesSummary(
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "total_sold") val totalSold: Int,
    @ColumnInfo(name = "total_revenue") val totalRevenue: Double,
    @ColumnInfo(name = "total_profit") val totalProfit: Double
)

// Additional data classes for analytics
data class TopSellingProductData(
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "brand") val brand: String?,
    @ColumnInfo(name = "model") val model: String?,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "totalQuantity") val totalQuantity: java.math.BigDecimal,
    @ColumnInfo(name = "totalRevenue") val totalRevenue: java.math.BigDecimal,
    @ColumnInfo(name = "totalProfit") val totalProfit: java.math.BigDecimal,
    @ColumnInfo(name = "averagePrice") val averagePrice: java.math.BigDecimal,
    @ColumnInfo(name = "currentStock") val currentStock: Int
)

data class CategorySalesData(
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "totalSales") val totalSales: java.math.BigDecimal,
    @ColumnInfo(name = "transactionCount") val transactionCount: Int
)

data class TransactionWithLineItem(
    @ColumnInfo(name = "line_item_id") val lineItemId: Long,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "customer_name") val customerName: String?,
    @ColumnInfo(name = "created_at") val createdAt: kotlinx.datetime.Instant,
    @ColumnInfo(name = "totalAmount") val totalAmount: java.math.BigDecimal
)

data class DeviceSaleData(
    @ColumnInfo(name = "imei") val imei: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "saleDate") val saleDate: kotlinx.datetime.Instant,
    @ColumnInfo(name = "salePrice") val salePrice: java.math.BigDecimal,
    @ColumnInfo(name = "customer_name") val customerName: String?
)

data class ProductSalesHistoryData(
    @ColumnInfo(name = "line_item_id") val lineItemId: Long,
    @ColumnInfo(name = "quantity") val quantity: java.math.BigDecimal,
    @ColumnInfo(name = "unit_selling_price") val unitPrice: java.math.BigDecimal,
    @ColumnInfo(name = "line_total") val totalAmount: java.math.BigDecimal,
    @ColumnInfo(name = "saleDate") val saleDate: kotlinx.datetime.Instant,
    @ColumnInfo(name = "customer_name") val customerName: String?,
    @ColumnInfo(name = "profit") val profit: java.math.BigDecimal?
)

data class CategorySalesInfo(
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "totalSales") val totalSales: java.math.BigDecimal,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "averagePrice") val averagePrice: java.math.BigDecimal
)

data class ProductSalesInfo(
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "totalSales") val totalSales: java.math.BigDecimal,
    @ColumnInfo(name = "quantitySold") val quantitySold: Int,
    @ColumnInfo(name = "averagePrice") val averagePrice: java.math.BigDecimal
)
