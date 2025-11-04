package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.IMEIStatus
import com.billme.app.data.local.entity.ProductIMEI
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductIMEIDao {
    
    /**
     * Insert new IMEI
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIMEI(imei: ProductIMEI): Long
    
    /**
     * Insert multiple IMEIs
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIMEIs(imeis: List<ProductIMEI>): List<Long>
    
    /**
     * Update IMEI
     */
    @Update
    suspend fun updateIMEI(imei: ProductIMEI)
    
    /**
     * Delete IMEI
     */
    @Delete
    suspend fun deleteIMEI(imei: ProductIMEI)
    
    /**
     * Get all IMEIs for a product
     */
    @Query("SELECT * FROM product_imeis WHERE product_id = :productId ORDER BY created_at DESC")
    fun getIMEIsByProductId(productId: Long): Flow<List<ProductIMEI>>
    
    /**
     * Get all IMEIs for a product (sync)
     */
    @Query("SELECT * FROM product_imeis WHERE product_id = :productId ORDER BY created_at DESC")
    suspend fun getIMEIsByProductIdSync(productId: Long): List<ProductIMEI>
    
    /**
     * Get available IMEIs for a product
     */
    @Query("SELECT * FROM product_imeis WHERE product_id = :productId AND status = 'AVAILABLE' ORDER BY created_at ASC")
    suspend fun getAvailableIMEIs(productId: Long): List<ProductIMEI>
    
    /**
     * Get available IMEIs for a product (Flow)
     */
    @Query("SELECT * FROM product_imeis WHERE product_id = :productId AND status = 'AVAILABLE' ORDER BY created_at ASC")
    fun getAvailableIMEIsFlow(productId: Long): Flow<List<ProductIMEI>>
    
    /**
     * Get IMEI by ID
     */
    @Query("SELECT * FROM product_imeis WHERE imei_id = :imeiId")
    suspend fun getIMEIById(imeiId: Long): ProductIMEI?
    
    /**
     * Get IMEI by IMEI number
     */
    @Query("SELECT * FROM product_imeis WHERE imei_number = :imeiNumber")
    suspend fun getIMEIByNumber(imeiNumber: String): ProductIMEI?
    
    /**
     * Check if IMEI exists
     */
    @Query("SELECT COUNT(*) FROM product_imeis WHERE imei_number = :imeiNumber")
    suspend fun imeiExists(imeiNumber: String): Int
    
    /**
     * Get available count for a product
     */
    @Query("SELECT COUNT(*) FROM product_imeis WHERE product_id = :productId AND status = 'AVAILABLE'")
    suspend fun getAvailableCount(productId: Long): Int
    
    /**
     * Get available count for a product (Flow)
     */
    @Query("SELECT COUNT(*) FROM product_imeis WHERE product_id = :productId AND status = 'AVAILABLE'")
    fun getAvailableCountFlow(productId: Long): Flow<Int>
    
    /**
     * Mark IMEI as sold
     */
    @Query("""
        UPDATE product_imeis 
        SET status = 'SOLD', 
            sold_date = :soldDate, 
            sold_price = :soldPrice,
            sold_in_transaction_id = :transactionId,
            updated_at = :updatedAt
        WHERE imei_id = :imeiId
    """)
    suspend fun markAsSold(
        imeiId: Long,
        soldDate: kotlinx.datetime.Instant,
        soldPrice: java.math.BigDecimal,
        transactionId: Long,
        updatedAt: kotlinx.datetime.Instant
    )
    
    /**
     * Mark IMEI as available (for returns)
     */
    @Query("""
        UPDATE product_imeis 
        SET status = 'AVAILABLE',
            sold_date = NULL,
            sold_price = NULL,
            sold_in_transaction_id = NULL,
            updated_at = :updatedAt
        WHERE imei_id = :imeiId
    """)
    suspend fun markAsAvailable(imeiId: Long, updatedAt: kotlinx.datetime.Instant)
    
    /**
     * Get IMEIs sold in a transaction
     */
    @Query("SELECT * FROM product_imeis WHERE sold_in_transaction_id = :transactionId")
    suspend fun getIMEIsByTransaction(transactionId: Long): List<ProductIMEI>
    
    /**
     * Get all sold IMEIs with product details
     */
    @Query("""
        SELECT pi.* FROM product_imeis pi
        WHERE pi.status = 'SOLD'
        ORDER BY pi.sold_date DESC
    """)
    fun getSoldIMEIs(): Flow<List<ProductIMEI>>
    
    /**
     * Get total value of available inventory by product
     */
    @Query("""
        SELECT SUM(purchase_price) FROM product_imeis 
        WHERE product_id = :productId AND status = 'AVAILABLE'
    """)
    suspend fun getTotalInventoryValue(productId: Long): java.math.BigDecimal?
    
    /**
     * Delete all IMEIs for a product
     */
    @Query("DELETE FROM product_imeis WHERE product_id = :productId")
    suspend fun deleteIMEIsByProductId(productId: Long)
    
    /**
     * Get all sold IMEIs with date range
     */
    @Query("""
        SELECT * FROM product_imeis 
        WHERE status = 'SOLD' 
        AND sold_date >= :startDate 
        AND sold_date <= :endDate
        ORDER BY sold_date DESC
    """)
    fun getSoldIMEIsByDateRange(
        startDate: kotlinx.datetime.Instant,
        endDate: kotlinx.datetime.Instant
    ): Flow<List<ProductIMEI>>
    
    /**
     * Get total inventory value (all available IMEIs)
     */
    @Query("""
        SELECT SUM(purchase_price) FROM product_imeis 
        WHERE status = 'AVAILABLE'
    """)
    suspend fun getTotalAvailableInventoryValue(): java.math.BigDecimal?
    
    /**
     * Get inventory value by status
     */
    @Query("""
        SELECT 
            status,
            COUNT(*) as count,
            SUM(purchase_price) as total_value,
            AVG(purchase_price) as avg_value
        FROM product_imeis 
        GROUP BY status
    """)
    suspend fun getInventoryValueByStatus(): List<InventoryValueByStatus>
    
    /**
     * Get all available IMEIs with product details for valuation
     */
    @Query("""
        SELECT pi.* FROM product_imeis pi
        WHERE pi.status = 'AVAILABLE'
        ORDER BY pi.product_id, pi.created_at ASC
    """)
    fun getAvailableIMEIsForValuation(): Flow<List<ProductIMEI>>
    
    /**
     * Get total count of all IMEIs
     */
    @Query("SELECT COUNT(*) FROM product_imeis")
    suspend fun getTotalIMEICount(): Int
}

data class InventoryValueByStatus(
    val status: IMEIStatus,
    val count: Int,
    val total_value: java.math.BigDecimal?,
    val avg_value: java.math.BigDecimal?
)
