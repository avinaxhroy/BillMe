package com.billme.app.ui.screen.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.backup.EnhancedBackupService
import com.billme.app.core.backup.GoogleDriveService
import com.billme.app.core.backup.DriveDownloadResult
import com.billme.app.core.util.GSTValidator
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.AppSetting
import com.billme.app.data.local.entity.GSTConfiguration
import com.billme.app.data.local.entity.GSTMode
import com.billme.app.data.local.entity.GSTType
import com.billme.app.data.local.entity.SettingCategory
import com.billme.app.data.local.entity.SettingValueType
import com.billme.app.data.repository.SetupPreferencesRepository
import com.billme.app.data.repository.GSTPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

data class SetupWizardUiState(
    // Wizard state
    val currentStep: SetupStep = SetupStep.RESTORE_OPTION,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val completionProgress: Float = 0f,
    
    // Step 0: Restore option
    val hasChosenRestoreOrNew: Boolean = false,
    val isRestoring: Boolean = false,
    val restoreProgress: String? = null,
    val needsGoogleSignIn: Boolean = false,
    
    // Step 1: Business Information
    val businessName: String = "",
    val businessAddress: String = "",
    val businessPhone: String = "",
    val businessEmail: String = "",
    
    // Step 2: GST Configuration
    val gstNumber: String = "",
    val gstEnabled: Boolean = true,
    val gstMode: GSTMode = GSTMode.FULL_GST,
    val defaultGstRate: Double = 18.0,
    val stateCode: String = "",
    
    // Step 3: Pricing Settings
    val priceIncludesGst: Boolean = false,
    val allowPriceEdit: Boolean = true,
    val trackCostHistory: Boolean = true,
    val showGstBreakdown: Boolean = true,
    
    // Validation state
    val businessNameError: String? = null,
    val gstNumberError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null
)

enum class SetupStep(val stepNumber: Int, val title: String) {
    RESTORE_OPTION(0, "Restore or Start Fresh"),
    BUSINESS_INFO(1, "Business Information"),
    GST_CONFIG(2, "Tax Configuration"),
    PRICING_SETTINGS(3, "Pricing Settings"),
    REVIEW(4, "Review & Confirm");
    
    fun getProgress(): Float = stepNumber / 5f
    
    fun next(): SetupStep? = when (this) {
        RESTORE_OPTION -> BUSINESS_INFO
        BUSINESS_INFO -> GST_CONFIG
        GST_CONFIG -> PRICING_SETTINGS
        PRICING_SETTINGS -> REVIEW
        REVIEW -> null
    }
    
    fun previous(): SetupStep? = when (this) {
        RESTORE_OPTION -> null
        BUSINESS_INFO -> RESTORE_OPTION
        GST_CONFIG -> BUSINESS_INFO
        PRICING_SETTINGS -> GST_CONFIG
        REVIEW -> PRICING_SETTINGS
    }
}

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val database: BillMeDatabase,
    private val setupPreferencesRepository: SetupPreferencesRepository,
    private val gstPreferencesRepository: GSTPreferencesRepository,
    private val backupService: EnhancedBackupService,
    private val googleDriveService: GoogleDriveService,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()
    
    init {
        // Set initial progress
        _uiState.update { it.copy(completionProgress = SetupStep.RESTORE_OPTION.getProgress()) }
        
        // Observe backup progress during restore
        viewModelScope.launch {
            backupService.backupProgress.collect { progress ->
                when (progress) {
                    is EnhancedBackupService.BackupProgress.InProgress -> {
                        _uiState.update {
                            it.copy(
                                isRestoring = true,
                                restoreProgress = progress.message
                            )
                        }
                    }
                    is EnhancedBackupService.BackupProgress.Success -> {
                        _uiState.update {
                            it.copy(
                                isRestoring = false,
                                restoreProgress = "Restore completed successfully!",
                                errorMessage = null
                            )
                        }
                    }
                    is EnhancedBackupService.BackupProgress.Error -> {
                        _uiState.update {
                            it.copy(
                                isRestoring = false,
                                restoreProgress = null,
                                errorMessage = progress.message
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    // ============================================
    // Restore/New Setup Methods
    // ============================================
    
    fun chooseStartFresh() {
        _uiState.update { it.copy(hasChosenRestoreOrNew = true) }
        goToNextStep()
    }
    
    fun restoreFromBackup(backupFile: java.io.File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoring = true,
                    restoreProgress = "Starting restore..."
                )
            }
            
            val result = backupService.restoreBackup(backupFile)
            
            when (result) {
                is com.billme.app.core.backup.RestoreResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            restoreProgress = "Restored successfully!",
                            errorMessage = null
                        )
                    }
                    // Mark setup as complete since we restored everything
                    setupPreferencesRepository.markSetupCompleted()
                }
                is com.billme.app.core.backup.RestoreResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            restoreProgress = null,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Get Google Sign-In intent for Drive restore
     */
    fun getGoogleSignInIntent() = googleDriveService.getSignInIntent()
    
    /**
     * Handle Google Sign-In result for Drive restore
     */
    suspend fun handleGoogleSignInResult(account: GoogleSignInAccount?): Boolean {
        if (account == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Sign-in cancelled or failed",
                    needsGoogleSignIn = false
                )
            }
            return false
        }
        
        // Initialize Drive service with signed-in account
        return googleDriveService.initializeDriveService(account)
    }
    
    /**
     * Restore from Google Drive (latest backup)
     * Automatically handles sign-in if needed
     */
    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRestoring = true,
                    restoreProgress = "Checking Google Drive access..."
                )
            }
            
            // Check if already signed in
            val signedInAccount = googleDriveService.getSignedInAccount()
            if (signedInAccount == null) {
                // Need to sign in first
                _uiState.update {
                    it.copy(
                        needsGoogleSignIn = true,
                        isRestoring = false,
                        restoreProgress = "Please sign in to Google Drive"
                    )
                }
                return@launch
            }
            
            // Already signed in, initialize Drive service
            _uiState.update {
                it.copy(restoreProgress = "Connecting to Google Drive...")
            }
            
            val initialized = googleDriveService.initializeDriveService(signedInAccount)
            if (!initialized) {
                val error = googleDriveService.getLastError() ?: "Failed to connect to Google Drive"
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        restoreProgress = null,
                        errorMessage = error,
                        needsGoogleSignIn = true
                    )
                }
                return@launch
            }
            
            _uiState.update {
                it.copy(restoreProgress = "Downloading latest backup...")
            }
            
            // Create temp file for download
            val tempFile = java.io.File(context.cacheDir, "drive_restore_${System.currentTimeMillis()}.zip")
            
            // Download and restore
            when (val result = googleDriveService.downloadLatestBackup(tempFile)) {
                is DriveDownloadResult.Success -> {
                    _uiState.update {
                        it.copy(restoreProgress = "Restoring data...")
                    }
                    
                    // Restore from downloaded file
                    val backupFile = java.io.File(result.filePath)
                    val restoreResult = backupService.restoreBackup(backupFile)
                    when (restoreResult) {
                        is com.billme.app.core.backup.RestoreResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isRestoring = false,
                                    restoreProgress = "Restored successfully! Restarting app...",
                                    errorMessage = null,
                                    needsGoogleSignIn = false
                                )
                            }
                            // Mark setup as complete since we restored everything
                            setupPreferencesRepository.markSetupCompleted()
                            // Clean up temp file
                            backupFile.delete()
                            
                            // If restore needs app restart, restart automatically
                            if (restoreResult.needsAppRestart) {
                                kotlinx.coroutines.delay(1500)
                                backupService.restartApp()
                            }
                        }
                        is com.billme.app.core.backup.RestoreResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isRestoring = false,
                                    restoreProgress = null,
                                    errorMessage = "Failed to restore: ${restoreResult.message}"
                                )
                            }
                            tempFile.delete()
                        }
                    }
                }
                is DriveDownloadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            restoreProgress = null,
                            errorMessage = if (result.message.contains("No backups found", ignoreCase = true)) {
                                "No backups found in Google Drive"
                            } else {
                                "Failed to download from Drive: ${result.message}"
                            }
                        )
                    }
                    tempFile.delete()
                }
            }
        }
    }
    
    /**
     * Continue restore after signing in
     */
    fun continueRestoreAfterSignIn(account: GoogleSignInAccount) {
        viewModelScope.launch {
            val initialized = handleGoogleSignInResult(account)
            if (initialized) {
                _uiState.update {
                    it.copy(needsGoogleSignIn = false)
                }
                // Now proceed with restore
                restoreFromGoogleDrive()
            }
        }
    }
    
    // ============================================
    // Business Information Methods
    // ============================================
    
    fun onBusinessNameChange(value: String) {
        _uiState.update {
            it.copy(
                businessName = value,
                businessNameError = null
            )
        }
    }
    
    fun onBusinessAddressChange(value: String) {
        _uiState.update { it.copy(businessAddress = value) }
    }
    
    fun onBusinessPhoneChange(value: String) {
        _uiState.update {
            it.copy(
                businessPhone = value,
                phoneError = null
            )
        }
    }
    
    fun onBusinessEmailChange(value: String) {
        _uiState.update {
            it.copy(
                businessEmail = value,
                emailError = null
            )
        }
    }
    
    // ============================================
    // GST Configuration Methods
    // ============================================
    
    fun onGstNumberChange(value: String) {
        val formatted = value.uppercase().take(15)
        _uiState.update {
            it.copy(
                gstNumber = formatted,
                gstNumberError = null
            )
        }
    }
    
    fun onGstEnabledChange(enabled: Boolean) {
        _uiState.update { it.copy(gstEnabled = enabled) }
    }
    
    fun onGstModeChange(mode: GSTMode) {
        _uiState.update { it.copy(gstMode = mode) }
    }
    
    fun onDefaultGstRateChange(rate: Double) {
        _uiState.update { it.copy(defaultGstRate = rate) }
    }
    
    fun onStateCodeChange(code: String) {
        _uiState.update { it.copy(stateCode = code.take(2)) }
    }
    
    // ============================================
    // Pricing Settings Methods
    // ============================================
    
    fun onPriceIncludesGstChange(includes: Boolean) {
        _uiState.update { it.copy(priceIncludesGst = includes) }
    }
    
    fun onAllowPriceEditChange(allow: Boolean) {
        _uiState.update { it.copy(allowPriceEdit = allow) }
    }
    
    fun onTrackCostHistoryChange(track: Boolean) {
        _uiState.update { it.copy(trackCostHistory = track) }
    }
    
    fun onShowGstBreakdownChange(show: Boolean) {
        _uiState.update { it.copy(showGstBreakdown = show) }
    }
    
    // ============================================
    // Navigation Methods
    // ============================================
    
    fun goToNextStep() {
        val currentState = _uiState.value
        
        // Validate current step before proceeding
        val isValid = when (currentState.currentStep) {
            SetupStep.RESTORE_OPTION -> currentState.hasChosenRestoreOrNew
            SetupStep.BUSINESS_INFO -> validateBusinessInfo()
            SetupStep.GST_CONFIG -> validateGstConfig()
            SetupStep.PRICING_SETTINGS -> validatePricingSettings()
            SetupStep.REVIEW -> true
        }
        
        if (!isValid) return
        
        currentState.currentStep.next()?.let { nextStep ->
            _uiState.update {
                it.copy(
                    currentStep = nextStep,
                    completionProgress = nextStep.getProgress(),
                    errorMessage = null
                )
            }
        }
    }
    
    fun goToPreviousStep() {
        val currentState = _uiState.value
        currentState.currentStep.previous()?.let { prevStep ->
            _uiState.update {
                it.copy(
                    currentStep = prevStep,
                    completionProgress = prevStep.getProgress(),
                    errorMessage = null
                )
            }
        }
    }
    
    fun goToStep(step: SetupStep) {
        _uiState.update {
            it.copy(
                currentStep = step,
                completionProgress = step.getProgress(),
                errorMessage = null
            )
        }
    }
    
    // ============================================
    // Validation Methods
    // ============================================
    
    private fun validateBusinessInfo(): Boolean {
        val state = _uiState.value
        var isValid = true
        
        // Business name is required
        if (state.businessName.isBlank()) {
            _uiState.update { it.copy(businessNameError = "Business name is required") }
            isValid = false
        }
        
        // Validate phone if provided
        if (state.businessPhone.isNotBlank() && !isValidPhone(state.businessPhone)) {
            _uiState.update { it.copy(phoneError = "Invalid phone number format") }
            isValid = false
        }
        
        // Validate email if provided
        if (state.businessEmail.isNotBlank() && !isValidEmail(state.businessEmail)) {
            _uiState.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }
        
        return isValid
    }
    
    private fun validateGstConfig(): Boolean {
        val state = _uiState.value
        
        // If GST is enabled and GST number is provided, validate it
        if (state.gstEnabled && state.gstNumber.isNotBlank()) {
            if (!GSTValidator.isValidGSTIN(state.gstNumber)) {
                _uiState.update {
                    it.copy(gstNumberError = "Invalid GST number format (15 characters required)")
                }
                return false
            }
        }
        
        return true
    }
    
    private fun validatePricingSettings(): Boolean {
        // No specific validation needed for pricing settings
        return true
    }
    
    private fun isValidPhone(phone: String): Boolean {
        // Basic phone validation: 10 digits
        return phone.replace(Regex("[^0-9]"), "").length == 10
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    // ============================================
    // Setup Completion
    // ============================================
    
    fun completeSetup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            completeSetupInternal(onSuccess)
        }
    }
    
    private suspend fun completeSetupInternal(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        try {
            val state = _uiState.value
            
            // 1. Save business settings to database
            saveBusinessSettings(state)
            
            // 2. Save GST configuration
            saveGstConfiguration(state)
            
            // 3. Save pricing preferences
            savePricingSettings(state)
            
            // 4. Mark setup as completed
            setupPreferencesRepository.markSetupCompleted()
            
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
            
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Failed to save settings: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun saveBusinessSettings(state: SetupWizardUiState) {
        val settingsDao = database.appSettingDao()
        val now = Clock.System.now()
        
        val settings = listOf(
            AppSetting(
                settingKey = "shop_name",
                settingValue = state.businessName,
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "Shop display name",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "shop_address",
                settingValue = state.businessAddress,
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "Business address",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "shop_phone",
                settingValue = state.businessPhone,
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "Contact number",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "shop_email",
                settingValue = state.businessEmail,
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "Business email",
                isSystem = false,
                updatedAt = now
            )
        )
        
        settingsDao.insertSettings(settings)
    }
    
    private suspend fun saveGstConfiguration(state: SetupWizardUiState) {
        val gstDao = database.gstConfigurationDao()
        val settingsDao = database.appSettingDao()
        val now = Clock.System.now()
        
        // Deactivate any existing configurations
        gstDao.deactivateAllConfigurations()
        
        // Create new GST configuration if enabled
        if (state.gstEnabled && state.gstNumber.isNotBlank()) {
            val gstConfig = GSTConfiguration(
                shopGSTIN = state.gstNumber,
                shopLegalName = state.businessName,
                shopTradeName = state.businessName,
                shopStateCode = state.stateCode,
                defaultGSTMode = state.gstMode,
                defaultGSTRate = state.defaultGstRate,
                showGSTSummary = state.showGstBreakdown,
                includeGSTInPrice = state.priceIncludesGst,
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
            
            gstDao.insertConfiguration(gstConfig)
        }
        
        // Save GST settings to app_settings table
        val gstSettings = listOf(
            AppSetting(
                settingKey = "gst_enabled",
                settingValue = state.gstEnabled.toString(),
                valueType = SettingValueType.BOOLEAN,
                category = SettingCategory.BUSINESS,
                description = "Whether GST is enabled",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "gst_number",
                settingValue = state.gstNumber,
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "GST identification number",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "gst_mode",
                settingValue = state.gstMode.name,
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "GST calculation mode",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "default_gst_rate",
                settingValue = state.defaultGstRate.toString(),
                valueType = SettingValueType.NUMBER,
                category = SettingCategory.BUSINESS,
                description = "Default GST rate percentage",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "state_code",
                settingValue = state.stateCode,
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "State code for GST",
                isSystem = false,
                updatedAt = now
            )
        )
        
        settingsDao.insertSettings(gstSettings)
        
        // ALSO save to DataStore to ensure GSTSettingsViewModel loads correct values
        gstPreferencesRepository.setGSTEnabled(state.gstEnabled)
        gstPreferencesRepository.setGSTMode(state.gstMode)
        gstPreferencesRepository.setGSTNumber(state.gstNumber)
        gstPreferencesRepository.setDefaultGSTRate(state.defaultGstRate)
        gstPreferencesRepository.setStateCode(state.stateCode)
    }
    
    private suspend fun savePricingSettings(state: SetupWizardUiState) {
        val settingsDao = database.appSettingDao()
        val now = Clock.System.now()
        
        val pricingSettings = listOf(
            AppSetting(
                settingKey = "price_includes_gst",
                settingValue = state.priceIncludesGst.toString(),
                valueType = SettingValueType.BOOLEAN,
                category = SettingCategory.BUSINESS,
                description = "Whether selling price includes GST",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "allow_price_edit",
                settingValue = state.allowPriceEdit.toString(),
                valueType = SettingValueType.BOOLEAN,
                category = SettingCategory.APP,
                description = "Allow price editing during billing",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "track_cost_history",
                settingValue = state.trackCostHistory.toString(),
                valueType = SettingValueType.BOOLEAN,
                category = SettingCategory.BUSINESS,
                description = "Track product cost price history",
                isSystem = false,
                updatedAt = now
            ),
            AppSetting(
                settingKey = "show_gst_breakdown",
                settingValue = state.showGstBreakdown.toString(),
                valueType = SettingValueType.BOOLEAN,
                category = SettingCategory.BUSINESS,
                description = "Show GST breakdown on invoices",
                isSystem = false,
                updatedAt = now
            )
        )
        
        settingsDao.insertSettings(pricingSettings)
    }
}
