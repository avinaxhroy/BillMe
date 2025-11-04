package com.billme.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.billme.app.data.local.entity.AppSetting
import com.billme.app.data.local.entity.SettingCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
    
    @Query("SELECT * FROM app_settings ORDER BY category, setting_key")
    fun getAllSettings(): Flow<List<AppSetting>>
    
    @Query("SELECT COUNT(*) FROM app_settings")
    suspend fun getSettingsCount(): Int
    
    @Query("SELECT * FROM app_settings WHERE setting_key = :key")
    suspend fun getSettingByKey(key: String): AppSetting?
    
    @Query("SELECT * FROM app_settings WHERE category = :category ORDER BY setting_key")
    fun getSettingsByCategory(category: SettingCategory): Flow<List<AppSetting>>
    
    @Query("SELECT setting_value FROM app_settings WHERE setting_key = :key")
    suspend fun getSettingValue(key: String): String?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<AppSetting>)
    
    @Update
    suspend fun updateSetting(setting: AppSetting)
    
    @Query("UPDATE app_settings SET setting_value = :value, updated_at = :updatedAt WHERE setting_key = :key")
    suspend fun updateSettingValue(key: String, value: String, updatedAt: Long)
    
    @Query("DELETE FROM app_settings WHERE setting_key = :key AND is_system = 0")
    suspend fun deleteSetting(key: String)
    
    // Convenience methods for common settings
    @Query("SELECT setting_value FROM app_settings WHERE setting_key = 'shop_name'")
    suspend fun getShopName(): String?
    
    @Query("SELECT setting_value FROM app_settings WHERE setting_key = 'gst_enabled'")
    suspend fun isGstEnabled(): String?
    
    @Query("SELECT setting_value FROM app_settings WHERE setting_key = 'default_gst_rate'")
    suspend fun getDefaultGstRate(): String?
    
    @Query("SELECT setting_value FROM app_settings WHERE setting_key = 'currency_code'")
    suspend fun getCurrencyCode(): String?
    
    /**
     * Get total settings count (for backup)
     */
    @Query("SELECT COUNT(*) FROM app_settings")
    suspend fun getAllSettingsCount(): Int
}
