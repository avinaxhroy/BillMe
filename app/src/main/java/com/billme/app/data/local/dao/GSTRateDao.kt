package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.GSTRate
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * DAO for GST rate management
 */
@Dao
interface GSTRateDao {
    
    /**
     * Get all GST rates
     */
    @Query("SELECT * FROM gst_rates ORDER BY gst_rate ASC")
    suspend fun getAllRates(): List<GSTRate>
    
    /**
     * Get all GST rates as Flow
     */
    @Query("SELECT * FROM gst_rates ORDER BY gst_rate ASC")
    fun getAllRatesFlow(): Flow<List<GSTRate>>
    
    /**
     * Get active GST rates only
     */
    @Query("SELECT * FROM gst_rates WHERE is_active = 1 ORDER BY gst_rate ASC")
    suspend fun getActiveRates(): List<GSTRate>
    
    /**
     * Get active GST rates as Flow
     */
    @Query("SELECT * FROM gst_rates WHERE is_active = 1 ORDER BY gst_rate ASC")
    fun getActiveRatesFlow(): Flow<List<GSTRate>>
    
    /**
     * Insert new GST rate
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: GSTRate): Long
    
    /**
     * Insert multiple GST rates
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<GSTRate>): List<Long>
    
    /**
     * Update existing GST rate
     */
    @Update
    suspend fun updateRate(rate: GSTRate)
    
    /**
     * Delete GST rate
     */
    @Delete
    suspend fun deleteRate(rate: GSTRate)
    
    /**
     * Delete GST rate by ID
     */
    @Query("DELETE FROM gst_rates WHERE rate_id = :rateId")
    suspend fun deleteGSTRateById(rateId: Long)
    
    /**
     * Get GST rate by ID
     */
    @Query("SELECT * FROM gst_rates WHERE rate_id = :rateId")
    suspend fun getRateById(rateId: Long): GSTRate?
    
    /**
     * Get GST rate by percentage
     */
    @Query("SELECT * FROM gst_rates WHERE gst_rate = :rate AND is_active = 1 LIMIT 1")
    suspend fun getRateByPercentage(rate: Double): GSTRate?
    
    /**
     * Get GST rates by category
     */
    @Query("SELECT * FROM gst_rates WHERE category = :category AND is_active = 1")
    suspend fun getRatesByCategory(category: String): List<GSTRate>
    
    /**
     * Search GST rates by description
     */
    @Query("SELECT * FROM gst_rates WHERE description LIKE '%' || :query || '%' AND is_active = 1")
    suspend fun searchRatesByDescription(query: String): List<GSTRate>
    
    /**
     * Get GST rates in range
     */
    @Query("SELECT * FROM gst_rates WHERE gst_rate BETWEEN :minRate AND :maxRate AND is_active = 1 ORDER BY gst_rate ASC")
    suspend fun getRatesInRange(minRate: Double, maxRate: Double): List<GSTRate>
    
    /**
     * Delete rate by ID
     */
    @Query("DELETE FROM gst_rates WHERE rate_id = :rateId")
    suspend fun deleteRateById(rateId: Long)
    
    /**
     * Update rate status
     */
    @Query("UPDATE gst_rates SET is_active = :isActive WHERE rate_id = :rateId")
    suspend fun updateRateStatus(rateId: Long, isActive: Boolean)
    
    /**
     * Get rate count
     */
    @Query("SELECT COUNT(*) FROM gst_rates WHERE is_active = 1")
    suspend fun getActiveRateCount(): Int
    
    /**
     * Check if rate exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM gst_rates WHERE gst_rate = :rate AND rate_id != :excludeRateId)")
    suspend fun isRateExists(rate: Double, excludeRateId: Long = 0): Boolean
    
    // Convenience methods for repository compatibility
    
    fun getAllActiveGSTRates(): Flow<List<GSTRate>> = getActiveRatesFlow()
    
    fun getGSTRatesByCategory(category: String): Flow<List<GSTRate>> {
        return kotlinx.coroutines.flow.flow {
            emit(getRatesByCategory(category))
        }
    }
    
    suspend fun getCurrentGSTRateByHSN(hsnCode: String): GSTRate? = getRateByPercentage(0.0) // Placeholder
    
    fun getGSTRatesByGSTCategory(category: com.billme.app.data.local.entity.GSTRateCategory): Flow<List<GSTRate>> {
        return kotlinx.coroutines.flow.flow {
            emit(getRatesByCategory(category.name))
        }
    }
    
    suspend fun getEffectiveGSTRate(
        category: String,
        subcategory: String? = null,
        currentTime: kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now()
    ): GSTRate? = getRateByPercentage(18.0) // Default to 18% GST
    
    fun getAllCategories(): Flow<List<String>> {
        return kotlinx.coroutines.flow.flow {
            emit(listOf("STANDARD", "EXEMPT", "ZERO_RATED"))
        }
    }
    
    fun getAllUniqueGSTRates(): Flow<List<Double>> {
        return kotlinx.coroutines.flow.flow {
            val rates = getAllRates().map { it.gstRate }.distinct().sorted()
            emit(rates)
        }
    }
    
    fun getAllHSNCodes(): Flow<List<String>> {
        return kotlinx.coroutines.flow.flow {
            emit(listOf())  // Empty placeholder
        }
    }
    
    suspend fun insertGSTRate(rate: GSTRate): Long = insertRate(rate)
    
    suspend fun updateGSTRate(rate: GSTRate) = updateRate(rate)
    
    suspend fun insertGSTRates(rates: List<GSTRate>): List<Long> = insertRates(rates)
}