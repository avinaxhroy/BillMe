package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.GSTConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * DAO for GST configuration management
 */
@Dao
interface GSTConfigurationDao {
    
    /**
     * Get current GST configuration
     */
    @Query("SELECT * FROM gst_configuration WHERE is_active = 1 LIMIT 1")
    suspend fun getCurrentConfiguration(): GSTConfiguration?
    
    /**
     * Get current GST configuration as Flow
     */
    @Query("SELECT * FROM gst_configuration WHERE is_active = 1 LIMIT 1")
    fun getCurrentConfigurationFlow(): Flow<GSTConfiguration?>
    
    /**
     * Insert new GST configuration
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfiguration(configuration: GSTConfiguration): Long
    
    /**
     * Update existing GST configuration
     */
    @Update
    suspend fun updateConfiguration(configuration: GSTConfiguration)
    
    /**
     * Delete GST configuration
     */
    @Delete
    suspend fun deleteConfiguration(configuration: GSTConfiguration)
    
    /**
     * Get all GST configurations
     */
    @Query("SELECT * FROM gst_configuration ORDER BY created_at DESC")
    suspend fun getAllConfigurations(): List<GSTConfiguration>
    
    /**
     * Get all GST configurations as Flow
     */
    @Query("SELECT * FROM gst_configuration ORDER BY created_at DESC")
    fun getAllConfigurationsFlow(): Flow<List<GSTConfiguration>>
    
    /**
     * Deactivate all configurations
     */
    @Query("UPDATE gst_configuration SET is_active = 0")
    suspend fun deactivateAllConfigurations()
    
    /**
     * Activate specific configuration
     */
    @Query("UPDATE gst_configuration SET is_active = 1 WHERE config_id = :configId")
    suspend fun activateConfiguration(configId: Long)
    
    /**
     * Get configuration by ID
     */
    @Query("SELECT * FROM gst_configuration WHERE config_id = :configId")
    suspend fun getConfigurationById(configId: Long): GSTConfiguration?
    
    /**
     * Search configurations by business name
     */
    @Query("SELECT * FROM gst_configuration WHERE shop_legal_name LIKE '%' || :query || '%' OR shop_trade_name LIKE '%' || :query || '%'")
    suspend fun searchConfigurationsByBusinessName(query: String): List<GSTConfiguration>
    
    /**
     * Get configurations by GST number
     */
    @Query("SELECT * FROM gst_configuration WHERE shop_gstin = :gstNumber")
    suspend fun getConfigurationByGSTNumber(gstNumber: String): GSTConfiguration?
    
    /**
     * Delete configuration by ID
     */
    @Query("DELETE FROM gst_configuration WHERE config_id = :configId")
    suspend fun deleteConfigurationById(configId: Long)
    
    /**
     * Get active configuration count
     */
    @Query("SELECT COUNT(*) FROM gst_configuration WHERE is_active = 1")
    suspend fun getActiveConfigurationCount(): Int
    
    /**
     * Check if GST number exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM gst_configuration WHERE shop_gstin = :gstNumber AND config_id != :excludeConfigId)")
    suspend fun isGSTNumberExists(gstNumber: String, excludeConfigId: Long = 0): Boolean
    
    // Convenience methods for repository compatibility
    
    suspend fun getCurrentGSTConfiguration(): GSTConfiguration? = getCurrentConfiguration()
    
    fun getCurrentGSTConfigurationFlow(): Flow<GSTConfiguration?> = getCurrentConfigurationFlow()
    
    suspend fun getGSTConfigurationById(configId: Long): GSTConfiguration? = getConfigurationById(configId)
    
    suspend fun getGSTConfigurationByGSTIN(gstin: String): GSTConfiguration? = getConfigurationByGSTNumber(gstin)
    
    fun getAllGSTConfigurations(): Flow<List<GSTConfiguration>> = getAllConfigurationsFlow()
    
    suspend fun setActiveGSTConfiguration(configuration: GSTConfiguration): Long {
        deactivateAllConfigurations()
        return insertConfiguration(configuration)
    }
}