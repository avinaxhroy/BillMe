package com.billme.app.ui.screen.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.AppSetting
import com.billme.app.data.local.entity.GSTMode
import com.billme.app.data.local.entity.GSTType
import com.billme.app.data.local.entity.SettingCategory
import com.billme.app.data.local.entity.SettingValueType
import com.billme.app.data.repository.GSTPreferencesRepository
import com.billme.app.data.repository.GSTSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GSTSettingsUiState(
    val gstEnabled: Boolean = true,
    val gstMode: GSTMode = GSTMode.FULL_GST,
    val gstType: GSTType = GSTType.CGST_SGST,
    val gstNumber: String = "",
    val businessName: String = "",
    val defaultGstRate: String = "18",
    val stateCode: String = "",
    val showGSTBreakdown: Boolean = true,
    val priceIncludesGST: Boolean = false,
    val printGSTOnReceipt: Boolean = true,
    val useHSNCodes: Boolean = false,
    val defaultHSNCode: String = "",
    val isLoading: Boolean = false,
    val isModified: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class GSTSettingsViewModel @Inject constructor(
    private val database: BillMeDatabase,
    private val gstPreferencesRepository: GSTPreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GSTSettingsUiState())
    val uiState: StateFlow<GSTSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Load from DataStore preferences first (faster and safer)
                val settings = gstPreferencesRepository.getGSTSettings()
                
                _uiState.update {
                    it.copy(
                        gstEnabled = settings.gstEnabled,
                        gstMode = settings.gstMode,
                        gstType = settings.gstType,
                        gstNumber = settings.gstNumber,
                        businessName = settings.businessName,
                        defaultGstRate = settings.defaultGstRate.toString(),
                        stateCode = settings.stateCode,
                        showGSTBreakdown = settings.showGSTBreakdown,
                        priceIncludesGST = settings.priceIncludesGST,
                        printGSTOnReceipt = settings.printGSTOnReceipt,
                        useHSNCodes = settings.useHSNCodes,
                        defaultHSNCode = settings.defaultHSNCode,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("Room") == true || 
                    e.message?.contains("Migration") == true ||
                    e.message?.contains("integrity") == true -> {
                        "Database error: Please uninstall and reinstall the app. ${e.message}"
                    }
                    else -> "Failed to load settings: ${e.message}"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                }
            }
        }
    }
    
    fun updateGSTEnabled(enabled: Boolean) {
        _uiState.update { it.copy(gstEnabled = enabled, isModified = true) }
    }
    
    fun updateGSTMode(mode: GSTMode) {
        _uiState.update { it.copy(gstMode = mode, isModified = true) }
    }
    
    fun updateGSTType(type: GSTType) {
        _uiState.update { it.copy(gstType = type, isModified = true) }
    }
    
    fun updateGSTNumber(number: String) {
        _uiState.update { it.copy(gstNumber = number, isModified = true) }
    }
    
    fun updateBusinessName(name: String) {
        _uiState.update { it.copy(businessName = name, isModified = true) }
    }
    
    fun updateDefaultGSTRate(rate: String) {
        _uiState.update { it.copy(defaultGstRate = rate, isModified = true) }
    }
    
    fun updateStateCode(code: String) {
        _uiState.update { it.copy(stateCode = code, isModified = true) }
    }
    
    fun updateShowGSTBreakdown(show: Boolean) {
        _uiState.update { it.copy(showGSTBreakdown = show, isModified = true) }
    }
    
    fun updatePriceIncludesGST(includes: Boolean) {
        _uiState.update { it.copy(priceIncludesGST = includes, isModified = true) }
    }
    
    fun updatePrintGSTOnReceipt(print: Boolean) {
        _uiState.update { it.copy(printGSTOnReceipt = print, isModified = true) }
    }
    
    fun updateUseHSNCodes(use: Boolean) {
        _uiState.update { it.copy(useHSNCodes = use, isModified = true) }
    }
    
    fun updateDefaultHSNCode(code: String) {
        _uiState.update { it.copy(defaultHSNCode = code, isModified = true) }
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            
            try {
                val state = _uiState.value
                val now = kotlinx.datetime.Clock.System.now()
                
                // Save to DataStore first (fast and crash-safe)
                val gstSettings = GSTSettings(
                    gstEnabled = state.gstEnabled,
                    gstMode = state.gstMode,
                    gstType = state.gstType,
                    gstNumber = state.gstNumber,
                    businessName = state.businessName,
                    defaultGstRate = state.defaultGstRate.toDoubleOrNull() ?: 18.0,
                    stateCode = state.stateCode,
                    showGSTBreakdown = state.showGSTBreakdown,
                    priceIncludesGST = state.priceIncludesGST,
                    printGSTOnReceipt = state.printGSTOnReceipt,
                    useHSNCodes = state.useHSNCodes,
                    defaultHSNCode = state.defaultHSNCode
                )
                gstPreferencesRepository.updateGSTSettings(gstSettings)
                
                // Also save to database for consistency
                try {
                    val settingsDao = database.appSettingDao()
                    settingsDao.insertSetting(AppSetting("gst_enabled", state.gstEnabled.toString(), SettingValueType.BOOLEAN, SettingCategory.BUSINESS, "Enable GST", false, now))
                    settingsDao.insertSetting(AppSetting("gst_mode", state.gstMode.name, SettingValueType.STRING, SettingCategory.BUSINESS, "GST Mode", false, now))
                    settingsDao.insertSetting(AppSetting("gst_type", state.gstType.name, SettingValueType.STRING, SettingCategory.BUSINESS, "GST Type", false, now))
                    settingsDao.insertSetting(AppSetting("gst_number", state.gstNumber, SettingValueType.STRING, SettingCategory.BUSINESS, "GST Number", false, now))
                    settingsDao.insertSetting(AppSetting("business_name", state.businessName, SettingValueType.STRING, SettingCategory.BUSINESS, "Business Name", false, now))
                    settingsDao.insertSetting(AppSetting("default_gst_rate", state.defaultGstRate, SettingValueType.STRING, SettingCategory.BUSINESS, "Default GST Rate", false, now))
                    settingsDao.insertSetting(AppSetting("state_code", state.stateCode, SettingValueType.STRING, SettingCategory.BUSINESS, "State Code", false, now))
                    settingsDao.insertSetting(AppSetting("show_gst_breakdown", state.showGSTBreakdown.toString(), SettingValueType.BOOLEAN, SettingCategory.BUSINESS, "Show GST Breakdown", false, now))
                    settingsDao.insertSetting(AppSetting("price_includes_gst", state.priceIncludesGST.toString(), SettingValueType.BOOLEAN, SettingCategory.BUSINESS, "Price Includes GST", false, now))
                    settingsDao.insertSetting(AppSetting("print_gst_on_receipt", state.printGSTOnReceipt.toString(), SettingValueType.BOOLEAN, SettingCategory.BUSINESS, "Print GST on Receipt", false, now))
                    settingsDao.insertSetting(AppSetting("use_hsn_codes", state.useHSNCodes.toString(), SettingValueType.BOOLEAN, SettingCategory.BUSINESS, "Use HSN Codes", false, now))
                    settingsDao.insertSetting(AppSetting("default_hsn_code", state.defaultHSNCode, SettingValueType.STRING, SettingCategory.BUSINESS, "Default HSN Code", false, now))
                    
                    // Also update gst_configuration table to keep it in sync for flexible GST system
                    val gstConfigDao = database.gstConfigurationDao()
                    gstConfigDao.deactivateAllConfigurations()
                    if (state.gstEnabled) {
                        val gstConfig = com.billme.app.data.local.entity.GSTConfiguration(
                            shopGSTIN = state.gstNumber.ifBlank { null },
                            shopLegalName = state.businessName,
                            shopTradeName = state.businessName,
                            shopStateCode = state.stateCode,
                            defaultGSTMode = state.gstMode,
                            defaultGSTRate = state.defaultGstRate.toDoubleOrNull() ?: 18.0,
                            showGSTSummary = state.showGSTBreakdown,
                            includeGSTInPrice = state.priceIncludesGST,
                            isActive = true,
                            createdAt = now,
                            updatedAt = now
                        )
                        gstConfigDao.insertConfiguration(gstConfig)
                    }
                } catch (dbError: Exception) {
                    // Log but don't fail if database save fails - DataStore is primary source
                    android.util.Log.w("GSTSettingsViewModel", "Database save failed: ${dbError.message}")
                }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isModified = false,
                        successMessage = "GST settings saved successfully!"
                    )
                }
                
                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(successMessage = null) }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save settings: ${e.message}"
                    )
                }
            }
        }
    }
}