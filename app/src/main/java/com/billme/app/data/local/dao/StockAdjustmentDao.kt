package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.StockAdjustment
import com.billme.app.data.local.entity.StockAdjustmentReason
import com.billme.app.data.local.entity.StockAdjustmentWithProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface StockAdjustmentDao {
    
    @Insert
    suspend fun insertAdjustment(adjustment: StockAdjustment): Long
    
    @Insert
    suspend fun insertAdjustments(adjustments: List<StockAdjustment>)
    
    @Query("""
        SELECT * FROM stock_adjustments 
        WHERE product_id = :productId 
        ORDER BY adjustment_date DESC
    """)
    fun getAdjustmentsByProduct(productId: Long): Flow<List<StockAdjustment>>
    
    @Query("""
        SELECT sa.*, p.product_name as productName, p.brand, p.model, p.imei1
        FROM stock_adjustments sa
        INNER JOIN products p ON sa.product_id = p.product_id
        WHERE sa.product_id = :productId
        ORDER BY sa.adjustment_date DESC
    """)
    fun getAdjustmentsWithProductDetails(productId: Long): Flow<List<StockAdjustmentWithProduct>>
    
    @Query("""
        SELECT sa.*, p.product_name as productName, p.brand, p.model, p.imei1
        FROM stock_adjustments sa
        INNER JOIN products p ON sa.product_id = p.product_id
        WHERE sa.adjustment_date BETWEEN :startDate AND :endDate
        ORDER BY sa.adjustment_date DESC
    """)
    fun getAdjustmentsInDateRange(
        startDate: Instant,
        endDate: Instant
    ): Flow<List<StockAdjustmentWithProduct>>
    
    @Query("""
        SELECT * FROM stock_adjustments 
        WHERE reason = :reason 
        ORDER BY adjustment_date DESC
    """)
    fun getAdjustmentsByReason(reason: StockAdjustmentReason): Flow<List<StockAdjustment>>
    
    @Query("""
        SELECT sa.*, p.product_name as productName, p.brand, p.model, p.imei1
        FROM stock_adjustments sa
        INNER JOIN products p ON sa.product_id = p.product_id
        ORDER BY sa.adjustment_date DESC
        LIMIT :limit
    """)
    fun getRecentAdjustments(limit: Int = 50): Flow<List<StockAdjustmentWithProduct>>
    
    @Query("""
        SELECT SUM(ABS(adjustment_quantity)) 
        FROM stock_adjustments 
        WHERE adjustment_date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalAdjustmentsInRange(startDate: Instant, endDate: Instant): Int?
    
    @Query("""
        SELECT COUNT(*) FROM stock_adjustments 
        WHERE product_id = :productId
    """)
    suspend fun getAdjustmentCountForProduct(productId: Long): Int
    
    @Query("DELETE FROM stock_adjustments WHERE adjustment_id = :adjustmentId")
    suspend fun deleteAdjustment(adjustmentId: Long)
    
    @Query("DELETE FROM stock_adjustments WHERE product_id = :productId")
    suspend fun deleteAdjustmentsForProduct(productId: Long)
}
