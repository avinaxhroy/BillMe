package com.billme.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.AppSetting
import com.billme.app.data.local.entity.SettingCategory
import com.billme.app.data.local.entity.SettingValueType
import com.billme.app.data.repository.SafeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val shopName: String = "",
    val shopAddress: String = "",
    val shopPhone: String = "",
    val shopEmail: String = "",
    val gstNumber: String = "",
    val printerEnabled: Boolean = false,
    val printerName: String = "",
    val autoPrintBill: Boolean = false,
    val showLowStockAlerts: Boolean = true,
    val lowStockThreshold: String = "5",
    val currency: String = "₹",
    val taxRate: String = "18.0",
    val enableNotifications: Boolean = true,
    val darkMode: Boolean = false,
    
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    
    val gstError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null
) {
    val isValid: Boolean
        get() = shopName.isNotBlank() &&
                gstError == null &&
                phoneError == null &&
                emailError == null
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: BillMeDatabase,
    private val safeRepository: SafeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = safeRepository.safeDbCall("loadSettings") {
                val settingsDao = database.appSettingDao()
                
                mapOf(
                    "shop_name" to (settingsDao.getSettingValue("shop_name") ?: "My Mobile Shop"),
                    "shop_address" to (settingsDao.getSettingValue("shop_address") ?: ""),
                    "shop_phone" to (settingsDao.getSettingValue("shop_phone") ?: ""),
                    "shop_email" to (settingsDao.getSettingValue("shop_email") ?: ""),
                    "gst_number" to (settingsDao.getSettingValue("gst_number") ?: ""),
                    "printer_enabled" to (settingsDao.getSettingValue("printer_enabled") ?: "false"),
                    "printer_name" to (settingsDao.getSettingValue("printer_name") ?: ""),
                    "auto_print_bill" to (settingsDao.getSettingValue("auto_print_bill") ?: "false"),
                    "show_low_stock_alerts" to (settingsDao.getSettingValue("show_low_stock_alerts") ?: "true"),
                    "low_stock_threshold" to (settingsDao.getSettingValue("low_stock_threshold") ?: "5"),
                    "currency" to (settingsDao.getSettingValue("currency") ?: "₹"),
                    "tax_rate" to (settingsDao.getSettingValue("tax_rate") ?: "18.0"),
                    "enable_notifications" to (settingsDao.getSettingValue("enable_notifications") ?: "true"),
                    "dark_mode" to (settingsDao.getSettingValue("dark_mode") ?: "false")
                )
            }
            
            result.onSuccess { settings ->
                _uiState.update { 
                    it.copy(
                        shopName = settings["shop_name"] ?: "My Mobile Shop",
                        shopAddress = settings["shop_address"] ?: "",
                        shopPhone = settings["shop_phone"] ?: "",
                        shopEmail = settings["shop_email"] ?: "",
                        gstNumber = settings["gst_number"] ?: "",
                        printerEnabled = settings["printer_enabled"]?.toBoolean() ?: false,
                        printerName = settings["printer_name"] ?: "",
                        autoPrintBill = settings["auto_print_bill"]?.toBoolean() ?: false,
                        showLowStockAlerts = settings["show_low_stock_alerts"]?.toBoolean() ?: true,
                        lowStockThreshold = settings["low_stock_threshold"] ?: "5",
                        currency = settings["currency"] ?: "₹",
                        taxRate = settings["tax_rate"] ?: "18.0",
                        enableNotifications = settings["enable_notifications"]?.toBoolean() ?: true,
                        darkMode = settings["dark_mode"]?.toBoolean() ?: false,
                        isLoading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load settings. Using defaults. Error: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun onShopNameChange(value: String) {
        _uiState.update { it.copy(shopName = value, errorMessage = null) }
    }
    
    fun onShopAddressChange(value: String) {
        _uiState.update { it.copy(shopAddress = value, errorMessage = null) }
    }
    
    fun onShopPhoneChange(value: String) {
        _uiState.update { 
            it.copy(
                shopPhone = value,
                phoneError = validatePhone(value),
                errorMessage = null
            ) 
        }
    }
    
    fun onShopEmailChange(value: String) {
        _uiState.update { 
            it.copy(
                shopEmail = value,
                emailError = validateEmail(value),
                errorMessage = null
            ) 
        }
    }
    
    fun onGstNumberChange(value: String) {
        _uiState.update { 
            it.copy(
                gstNumber = value.uppercase(),
                gstError = validateGst(value),
                errorMessage = null
            ) 
        }
    }
    
    fun onPrinterEnabledChange(enabled: Boolean) {
        _uiState.update { it.copy(printerEnabled = enabled) }
    }
    
    fun onPrinterNameChange(value: String) {
        _uiState.update { it.copy(printerName = value) }
    }
    
    fun onAutoPrintBillChange(enabled: Boolean) {
        _uiState.update { it.copy(autoPrintBill = enabled) }
    }
    
    fun onShowLowStockAlertsChange(enabled: Boolean) {
        _uiState.update { it.copy(showLowStockAlerts = enabled) }
    }
    
    fun onLowStockThresholdChange(value: String) {
        if (value.isEmpty() || value.toIntOrNull() != null) {
            _uiState.update { it.copy(lowStockThreshold = value) }
        }
    }
    
    fun onCurrencyChange(value: String) {
        _uiState.update { it.copy(currency = value) }
    }
    
    fun onTaxRateChange(value: String) {
        if (value.isEmpty() || value.toDoubleOrNull() != null) {
            _uiState.update { it.copy(taxRate = value) }
        }
    }
    
    fun onEnableNotificationsChange(enabled: Boolean) {
        _uiState.update { it.copy(enableNotifications = enabled) }
    }
    
    fun onDarkModeChange(enabled: Boolean) {
        _uiState.update { it.copy(darkMode = enabled) }
    }
    
    private fun validatePhone(phone: String): String? {
        if (phone.isBlank()) return null
        return when {
            phone.length != 10 -> "Phone number must be 10 digits"
            !phone.all { it.isDigit() } -> "Phone number must contain only digits"
            else -> null
        }
    }
    
    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return null
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return if (!email.matches(emailRegex)) "Invalid email format" else null
    }
    
    private fun validateGst(gst: String): String? {
        if (gst.isBlank()) return null
        val gstRegex = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}\$".toRegex()
        return when {
            gst.length != 15 -> "GST number must be 15 characters"
            !gst.matches(gstRegex) -> "Invalid GST format"
            else -> null
        }
    }
    
    fun saveSettings(onSuccess: () -> Unit) {
        val state = _uiState.value
        
        if (!state.isValid) {
            _uiState.update { it.copy(errorMessage = "Please correct all errors before saving") }
            return
        }
        
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            val result = safeRepository.safeDbCall("saveSettings") {
                val settingsDao = database.appSettingDao()
                val now = kotlinx.datetime.Clock.System.now()
                
                // Save all settings
                val settings = listOf(
                    AppSetting("shop_name", state.shopName, SettingValueType.STRING, SettingCategory.BUSINESS, "Shop Name", false, now),
                    AppSetting("shop_address", state.shopAddress, SettingValueType.STRING, SettingCategory.BUSINESS, "Shop Address", false, now),
                    AppSetting("shop_phone", state.shopPhone, SettingValueType.STRING, SettingCategory.BUSINESS, "Shop Phone", false, now),
                    AppSetting("shop_email", state.shopEmail, SettingValueType.STRING, SettingCategory.BUSINESS, "Shop Email", false, now),
                    AppSetting("gst_number", state.gstNumber, SettingValueType.STRING, SettingCategory.BUSINESS, "GST Number", false, now),
                    AppSetting("printer_enabled", state.printerEnabled.toString(), SettingValueType.BOOLEAN, SettingCategory.PRINTER, "Printer Enabled", false, now),
                    AppSetting("printer_name", state.printerName, SettingValueType.STRING, SettingCategory.PRINTER, "Printer Name", false, now),
                    AppSetting("auto_print_bill", state.autoPrintBill.toString(), SettingValueType.BOOLEAN, SettingCategory.PRINTER, "Auto Print Bill", false, now),
                    AppSetting("show_low_stock_alerts", state.showLowStockAlerts.toString(), SettingValueType.BOOLEAN, SettingCategory.APP, "Show Low Stock Alerts", false, now),
                    AppSetting("low_stock_threshold", state.lowStockThreshold, SettingValueType.STRING, SettingCategory.APP, "Low Stock Threshold", false, now),
                    AppSetting("currency", state.currency, SettingValueType.STRING, SettingCategory.BUSINESS, "Currency", false, now),
                    AppSetting("tax_rate", state.taxRate, SettingValueType.STRING, SettingCategory.BUSINESS, "Tax Rate", false, now),
                    AppSetting("enable_notifications", state.enableNotifications.toString(), SettingValueType.BOOLEAN, SettingCategory.APP, "Enable Notifications", false, now),
                    AppSetting("dark_mode", state.darkMode.toString(), SettingValueType.BOOLEAN, SettingCategory.APP, "Dark Mode", false, now)
                )
                
                settingsDao.insertSettings(settings)
            }
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = "Settings saved successfully!"
                    )
                }
                
                onSuccess()
                
                // Clear success message after delay
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(successMessage = null) }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save settings: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun resetToDefaults() {
        _uiState.update { 
            SettingsUiState(
                shopName = "",
                currency = "₹",
                taxRate = "18.0",
                lowStockThreshold = "5",
                showLowStockAlerts = true,
                enableNotifications = true,
                darkMode = false
            )
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
