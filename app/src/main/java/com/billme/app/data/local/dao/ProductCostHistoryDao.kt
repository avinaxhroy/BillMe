package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.ProductCostHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.math.BigDecimal

@Dao
interface ProductCostHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostHistory(costHistory: ProductCostHistory): Long
    
    @Update
    suspend fun updateCostHistory(costHistory: ProductCostHistory)
    
    @Delete
    suspend fun deleteCostHistory(costHistory: ProductCostHistory)
    
    /**
     * Get all cost history for a product
     */
    @Query("SELECT * FROM product_cost_history WHERE product_id = :productId ORDER BY purchase_date DESC")
    fun getCostHistoryForProduct(productId: Long): Flow<List<ProductCostHistory>>
    
    /**
     * Get current cost price for a product
     */
    @Query("SELECT * FROM product_cost_history WHERE product_id = :productId AND is_current = 1 ORDER BY purchase_date DESC LIMIT 1")
    suspend fun getCurrentCostPrice(productId: Long): ProductCostHistory?
    
    /**
     * Get average cost price for a product
     */
    @Query("SELECT AVG(cost_price) FROM product_cost_history WHERE product_id = :productId")
    suspend fun getAverageCostPrice(productId: Long): BigDecimal?
    
    /**
     * Get weighted average cost price (based on quantity)
     */
    @Query("""
        SELECT SUM(cost_price * quantity_purchased) / SUM(quantity_purchased) 
        FROM product_cost_history 
        WHERE product_id = :productId
    """)
    suspend fun getWeightedAverageCostPrice(productId: Long): BigDecimal?
    
    /**
     * Get cost history for a date range
     */
    @Query("""
        SELECT * FROM product_cost_history 
        WHERE product_id = :productId 
        AND purchase_date BETWEEN :startDate AND :endDate 
        ORDER BY purchase_date DESC
    """)
    fun getCostHistoryForDateRange(
        productId: Long,
        startDate: Instant,
        endDate: Instant
    ): Flow<List<ProductCostHistory>>
    
    /**
     * Mark all previous cost prices as not current
     */
    @Query("UPDATE product_cost_history SET is_current = 0 WHERE product_id = :productId")
    suspend fun markAllAsNotCurrent(productId: Long)
    
    /**
     * Get latest cost price entry
     */
    @Query("SELECT * FROM product_cost_history WHERE product_id = :productId ORDER BY purchase_date DESC LIMIT 1")
    suspend fun getLatestCostPrice(productId: Long): ProductCostHistory?
    
    /**
     * Get min and max cost prices for a product
     */
    @Query("""
        SELECT MIN(cost_price) as minPrice, MAX(cost_price) as maxPrice 
        FROM product_cost_history 
        WHERE product_id = :productId
    """)
    suspend fun getMinMaxCostPrice(productId: Long): MinMaxPrice?
    
    /**
     * Get total purchase value for a product
     */
    @Query("""
        SELECT SUM(cost_price * quantity_purchased) 
        FROM product_cost_history 
        WHERE product_id = :productId
    """)
    suspend fun getTotalPurchaseValue(productId: Long): BigDecimal?
    
    /**
     * Delete old cost history (keep only recent entries)
     */
    @Query("""
        DELETE FROM product_cost_history 
        WHERE product_id = :productId 
        AND cost_history_id NOT IN (
            SELECT cost_history_id FROM product_cost_history 
            WHERE product_id = :productId 
            ORDER BY purchase_date DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun cleanupOldHistory(productId: Long, keepCount: Int = 10)
}

/**
 * Data class for min/max cost prices
 */
data class MinMaxPrice(
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?
)
