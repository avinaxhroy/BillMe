package com.billme.app.data.repository

import com.billme.app.data.local.dao.AppSettingDao
import com.billme.app.data.local.entity.AppSetting
import com.billme.app.data.local.entity.DefaultSettings
import com.billme.app.data.local.entity.SettingCategory
import com.billme.app.core.util.formatLocale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val appSettingDao: AppSettingDao
) {
    
    suspend fun initializeDefaultSettings() {
        // Check if settings are already initialized
        val existingSettings = appSettingDao.getAllSettings().first()
        if (existingSettings.isEmpty()) {
            // Insert default settings
            appSettingDao.insertSettings(DefaultSettings.defaults.values.toList())
        }
    }
    
    fun getAllSettings(): Flow<List<AppSetting>> = appSettingDao.getAllSettings()
    
    suspend fun getSettingByKey(key: String): AppSetting? = appSettingDao.getSettingByKey(key)
    
    suspend fun getSettingValue(key: String): String? = appSettingDao.getSettingValue(key)
    
    fun getSettingsByCategory(category: SettingCategory): Flow<List<AppSetting>> = 
        appSettingDao.getSettingsByCategory(category)
    
    suspend fun updateSettingValue(key: String, value: String) {
        appSettingDao.updateSettingValue(key, value, Clock.System.now().toEpochMilliseconds())
    }
    
    suspend fun insertSetting(setting: AppSetting) {
        appSettingDao.insertSetting(setting)
    }
    
    // Business settings convenience methods
    suspend fun getShopName(): String = 
        appSettingDao.getShopName() ?: "Mobile Shop Pro"
    
    suspend fun getShopAddress(): String = 
        getSettingValue("shop_address") ?: ""
    
    suspend fun getShopPhone(): String = 
        getSettingValue("shop_phone") ?: ""
    
    suspend fun isGstEnabled(): Boolean = 
        appSettingDao.isGstEnabled()?.toBoolean() ?: true
    
    suspend fun getDefaultGstRate(): Double = 
        appSettingDao.getDefaultGstRate()?.toDoubleOrNull() ?: 18.0
    
    suspend fun getCurrencyCode(): String = 
        appSettingDao.getCurrencyCode() ?: "INR"
    
    suspend fun getCurrencySymbol(): String {
        return when (getCurrencyCode()) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
    }
    
    // Printer settings
    suspend fun getPrinterModel(): String = 
        getSettingValue("printer_model") ?: ""
    
    suspend fun getPaperWidth(): Int = 
        getSettingValue("paper_width")?.toIntOrNull() ?: 58
    
    // App settings
    suspend fun isAutoBackupEnabled(): Boolean = 
        getSettingValue("auto_backup_enabled")?.toBoolean() ?: true
    
    suspend fun getBackupFrequency(): String = 
        getSettingValue("backup_frequency") ?: "DAILY"
    
    // Update specific settings
    suspend fun updateShopName(name: String) {
        updateSettingValue("shop_name", name)
    }
    
    suspend fun updateShopAddress(address: String) {
        updateSettingValue("shop_address", address)
    }
    
    suspend fun updateShopPhone(phone: String) {
        updateSettingValue("shop_phone", phone)
    }
    
    suspend fun updateGstEnabled(enabled: Boolean) {
        updateSettingValue("gst_enabled", enabled.toString())
    }
    
    suspend fun updateDefaultGstRate(rate: Double) {
        updateSettingValue("default_gst_rate", rate.toString())
    }
    
    suspend fun updatePrinterModel(model: String) {
        updateSettingValue("printer_model", model)
    }
    
    suspend fun updatePaperWidth(width: Int) {
        updateSettingValue("paper_width", width.toString())
    }
    
    suspend fun updateAutoBackupEnabled(enabled: Boolean) {
        updateSettingValue("auto_backup_enabled", enabled.toString())
    }
    
    suspend fun updateBackupFrequency(frequency: String) {
        updateSettingValue("backup_frequency", frequency)
    }
    
    // Format currency amount
    suspend fun formatCurrency(amount: Double): String {
        val symbol = getCurrencySymbol()
        return "$symbol${amount.formatLocale("%.2f")}"
    }
    
    suspend fun formatCurrency(amount: java.math.BigDecimal): String {
        val symbol = getCurrencySymbol()
        return "$symbol${amount.toDouble().formatLocale("%.2f")}"
    }
    
    // Validation
    suspend fun validateGstRate(rate: Double): Boolean {
        return rate in 0.0..100.0
    }
    
    suspend fun validatePaperWidth(width: Int): Boolean {
        return width in listOf(58, 80) // Common thermal printer widths
    }
}