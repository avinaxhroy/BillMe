package com.billme.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.billme.app.data.local.entity.GSTMode
import com.billme.app.data.local.entity.GSTType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gstPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "gst_preferences")

/**
 * Repository for managing GST settings using DataStore
 * This ensures settings are persisted and available even before database initialization
 */
@Singleton
class GSTPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.gstPreferencesDataStore
    
    companion object {
        private val GST_ENABLED_KEY = booleanPreferencesKey("gst_enabled")
        private val GST_MODE_KEY = stringPreferencesKey("gst_mode")
        private val GST_TYPE_KEY = stringPreferencesKey("gst_type")
        private val GST_NUMBER_KEY = stringPreferencesKey("gst_number")
        private val BUSINESS_NAME_KEY = stringPreferencesKey("business_name")
        private val DEFAULT_GST_RATE_KEY = doublePreferencesKey("default_gst_rate")
        private val STATE_CODE_KEY = stringPreferencesKey("state_code")
        private val SHOW_GST_BREAKDOWN_KEY = booleanPreferencesKey("show_gst_breakdown")
        private val PRICE_INCLUDES_GST_KEY = booleanPreferencesKey("price_includes_gst")
        private val PRINT_GST_ON_RECEIPT_KEY = booleanPreferencesKey("print_gst_on_receipt")
        private val USE_HSN_CODES_KEY = booleanPreferencesKey("use_hsn_codes")
        private val DEFAULT_HSN_CODE_KEY = stringPreferencesKey("default_hsn_code")
    }
    
    /**
     * Flow of current GST settings
     */
    val gstSettings: Flow<GSTSettings> = dataStore.data.map { preferences ->
        GSTSettings(
            gstEnabled = preferences[GST_ENABLED_KEY] ?: true,
            gstMode = try {
                GSTMode.valueOf(preferences[GST_MODE_KEY] ?: "FULL_GST")
            } catch (e: Exception) {
                GSTMode.FULL_GST
            },
            gstType = try {
                GSTType.valueOf(preferences[GST_TYPE_KEY] ?: "CGST_SGST")
            } catch (e: Exception) {
                GSTType.CGST_SGST
            },
            gstNumber = preferences[GST_NUMBER_KEY] ?: "",
            businessName = preferences[BUSINESS_NAME_KEY] ?: "",
            defaultGstRate = preferences[DEFAULT_GST_RATE_KEY] ?: 18.0,
            stateCode = preferences[STATE_CODE_KEY] ?: "",
            showGSTBreakdown = preferences[SHOW_GST_BREAKDOWN_KEY] ?: true,
            priceIncludesGST = preferences[PRICE_INCLUDES_GST_KEY] ?: false,
            printGSTOnReceipt = preferences[PRINT_GST_ON_RECEIPT_KEY] ?: true,
            useHSNCodes = preferences[USE_HSN_CODES_KEY] ?: false,
            defaultHSNCode = preferences[DEFAULT_HSN_CODE_KEY] ?: ""
        )
    }
    
    /**
     * Get GST settings synchronously (for immediate access)
     */
    suspend fun getGSTSettings(): GSTSettings {
        return gstSettings.first()
    }
    
    /**
     * Update GST enabled status
     */
    suspend fun setGSTEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[GST_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Update GST mode
     */
    suspend fun setGSTMode(mode: GSTMode) {
        dataStore.edit { preferences ->
            preferences[GST_MODE_KEY] = mode.name
        }
    }
    
    /**
     * Update GST type
     */
    suspend fun setGSTType(type: GSTType) {
        dataStore.edit { preferences ->
            preferences[GST_TYPE_KEY] = type.name
        }
    }
    
    /**
     * Update GST number
     */
    suspend fun setGSTNumber(gstNumber: String) {
        dataStore.edit { preferences ->
            preferences[GST_NUMBER_KEY] = gstNumber
        }
    }
    
    /**
     * Update business name
     */
    suspend fun setBusinessName(name: String) {
        dataStore.edit { preferences ->
            preferences[BUSINESS_NAME_KEY] = name
        }
    }
    
    /**
     * Update default GST rate
     */
    suspend fun setDefaultGSTRate(rate: Double) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_GST_RATE_KEY] = rate
        }
    }
    
    /**
     * Update state code
     */
    suspend fun setStateCode(code: String) {
        dataStore.edit { preferences ->
            preferences[STATE_CODE_KEY] = code
        }
    }
    
    /**
     * Update show GST breakdown
     */
    suspend fun setShowGSTBreakdown(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_GST_BREAKDOWN_KEY] = show
        }
    }
    
    /**
     * Update price includes GST
     */
    suspend fun setPriceIncludesGST(includes: Boolean) {
        dataStore.edit { preferences ->
            preferences[PRICE_INCLUDES_GST_KEY] = includes
        }
    }
    
    /**
     * Update print GST on receipt
     */
    suspend fun setPrintGSTOnReceipt(print: Boolean) {
        dataStore.edit { preferences ->
            preferences[PRINT_GST_ON_RECEIPT_KEY] = print
        }
    }
    
    /**
     * Update use HSN codes
     */
    suspend fun setUseHSNCodes(use: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_HSN_CODES_KEY] = use
        }
    }
    
    /**
     * Update default HSN code
     */
    suspend fun setDefaultHSNCode(code: String) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_HSN_CODE_KEY] = code
        }
    }
    
    /**
     * Update all GST settings at once
     */
    suspend fun updateGSTSettings(settings: GSTSettings) {
        dataStore.edit { preferences ->
            preferences[GST_ENABLED_KEY] = settings.gstEnabled
            preferences[GST_MODE_KEY] = settings.gstMode.name
            preferences[GST_TYPE_KEY] = settings.gstType.name
            preferences[GST_NUMBER_KEY] = settings.gstNumber
            preferences[BUSINESS_NAME_KEY] = settings.businessName
            preferences[DEFAULT_GST_RATE_KEY] = settings.defaultGstRate
            preferences[STATE_CODE_KEY] = settings.stateCode
            preferences[SHOW_GST_BREAKDOWN_KEY] = settings.showGSTBreakdown
            preferences[PRICE_INCLUDES_GST_KEY] = settings.priceIncludesGST
            preferences[PRINT_GST_ON_RECEIPT_KEY] = settings.printGSTOnReceipt
            preferences[USE_HSN_CODES_KEY] = settings.useHSNCodes
            preferences[DEFAULT_HSN_CODE_KEY] = settings.defaultHSNCode
        }
    }
    
    /**
     * Clear all GST settings (reset to defaults)
     */
    suspend fun clearSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

/**
 * Data class representing GST settings
 */
data class GSTSettings(
    val gstEnabled: Boolean = true,
    val gstMode: GSTMode = GSTMode.FULL_GST,
    val gstType: GSTType = GSTType.CGST_SGST,
    val gstNumber: String = "",
    val businessName: String = "",
    val defaultGstRate: Double = 18.0,
    val stateCode: String = "",
    val showGSTBreakdown: Boolean = true,
    val priceIncludesGST: Boolean = false,
    val printGSTOnReceipt: Boolean = true,
    val useHSNCodes: Boolean = false,
    val defaultHSNCode: String = ""
)
