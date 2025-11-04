package com.billme.app.ui.screen.backup

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.backup.BackupInfo
import com.billme.app.core.backup.BackupScheduler
import com.billme.app.core.backup.EnhancedBackupService
import com.billme.app.core.backup.GoogleDriveService
import com.billme.app.core.backup.GoogleDriveBackupService
import com.billme.app.core.backup.DriveUploadResult
import com.billme.app.core.backup.DriveDownloadResult
import com.billme.app.core.backup.RestoreResult
import com.billme.app.data.model.CloudBackupResult
import com.billme.app.data.repository.SettingsRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupService: EnhancedBackupService,
    private val backupScheduler: BackupScheduler,
    private val settingsRepository: SettingsRepository,
    private val googleDriveService: GoogleDriveService,
    private val googleDriveBackupService: GoogleDriveBackupService
) : ViewModel() {
    
    private val _availableBackups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val availableBackups: StateFlow<List<BackupInfo>> = _availableBackups.asStateFlow()
    
    private val _driveSignedIn = MutableStateFlow(false)
    val driveSignedIn: StateFlow<Boolean> = _driveSignedIn.asStateFlow()
    
    private val _driveUploadStatus = MutableStateFlow<DriveUploadStatus>(DriveUploadStatus.Idle)
    val driveUploadStatus: StateFlow<DriveUploadStatus> = _driveUploadStatus.asStateFlow()
    
    val backupProgress: StateFlow<EnhancedBackupService.BackupProgress> = 
        backupService.backupProgress.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EnhancedBackupService.BackupProgress.Idle
        )
    
    init {
        loadAvailableBackups()
        checkDriveSignInStatus()
    }
    
    /**
     * Check if user is signed in to Google Drive
     */
    private fun checkDriveSignInStatus() {
        viewModelScope.launch {
            val isSignedIn = googleDriveService.isSignedIn()
            _driveSignedIn.value = isSignedIn
            
            // If signed in, verify Drive service can be initialized
            if (isSignedIn) {
                val account = googleDriveService.getSignedInAccount()
                if (account != null) {
                    val initSuccess = googleDriveService.initializeDriveService(account)
                    if (!initSuccess) {
                        // Initialization failed, update state to not signed in
                        _driveSignedIn.value = false
                        android.util.Log.e("BackupViewModel", "Drive service initialization failed on startup")
                    }
                } else {
                    _driveSignedIn.value = false
                }
            }
        }
    }
    
    /**
     * Refresh Drive sign-in status (called when screen becomes visible)
     */
    fun refreshDriveSignInStatus() {
        checkDriveSignInStatus()
    }
    
    /**
     * Get Google Sign-In intent for OAuth flow
     */
    fun getSignInIntent(): Intent {
        android.util.Log.d("BackupViewModel", "Creating sign-in intent...")
        try {
            val intent = googleDriveService.getSignInIntent()
            android.util.Log.d("BackupViewModel", "Sign-in intent created successfully")
            return intent
        } catch (e: Exception) {
            android.util.Log.e("BackupViewModel", "Error creating sign-in intent: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Handle Google Sign-In result
     */
    fun handleSignInResult(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            if (account != null) {
                android.util.Log.d("BackupViewModel", "Processing sign-in for: ${account.email}")
                android.util.Log.d("BackupViewModel", "Account granted scopes: ${account.grantedScopes}")
                
                // Check if the account has the required Drive permissions
                if (!googleDriveService.hasRequiredPermissions(account)) {
                    android.util.Log.e("BackupViewModel", "Account does not have required Drive permissions")
                    _driveSignedIn.value = false
                    _driveUploadStatus.value = DriveUploadStatus.Error("Drive permissions not granted. Please try signing in again and grant Drive access.")
                    kotlinx.coroutines.delay(5000)
                    resetUploadStatus()
                    return@launch
                }
                
                _driveUploadStatus.value = DriveUploadStatus.Uploading(0f, "Connecting to Google Drive...")
                
                try {
                    val success = googleDriveService.initializeDriveService(account)
                    _driveSignedIn.value = success
                    
                    if (success) {
                        android.util.Log.d("BackupViewModel", "Successfully signed in to Google Drive")
                        _driveUploadStatus.value = DriveUploadStatus.Success("Connected to Google Drive")
                    } else {
                        android.util.Log.e("BackupViewModel", "Failed to initialize Drive service")
                        _driveSignedIn.value = false
                        
                        // Get the actual error from the service
                        val errorMessage = googleDriveService.getLastError() 
                            ?: "Failed to connect to Google Drive"
                        
                        android.util.Log.e("BackupViewModel", "Drive service error: $errorMessage")
                        _driveUploadStatus.value = DriveUploadStatus.Error(errorMessage)
                    }
                    
                    // Reset status after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    resetUploadStatus()
                } catch (e: Exception) {
                    android.util.Log.e("BackupViewModel", "Exception during sign-in: ${e.message}", e)
                    _driveSignedIn.value = false
                    _driveUploadStatus.value = DriveUploadStatus.Error("Connection error: ${e.message}")
                    kotlinx.coroutines.delay(3000)
                    resetUploadStatus()
                }
            } else {
                _driveSignedIn.value = false
                android.util.Log.e("BackupViewModel", "Sign-in account is null")
                _driveUploadStatus.value = DriveUploadStatus.Error("Sign-in failed. No account received.")
                kotlinx.coroutines.delay(3000)
                resetUploadStatus()
            }
        }
    }
    
    /**
     * Show sign-in error
     */
    fun showSignInError(message: String) {
        viewModelScope.launch {
            _driveUploadStatus.value = DriveUploadStatus.Error("Sign-in failed: $message")
            kotlinx.coroutines.delay(3000)
            resetUploadStatus()
        }
    }
    
    /**
     * Sign out from Google Drive
     */
    fun signOutFromDrive() {
        viewModelScope.launch {
            googleDriveService.signOut()
            _driveSignedIn.value = false
            android.util.Log.d("BackupViewModel", "Signed out from Google Drive")
        }
    }
    
    /**
     * Load all available backups
     */
    private fun loadAvailableBackups() {
        viewModelScope.launch {
            _availableBackups.value = backupService.getAvailableBackups()
        }
    }
    
    /**
     * Create a new manual backup
     */
    fun createBackup() {
        viewModelScope.launch {
            // Prevent multiple simultaneous backups
            if (backupProgress.value is EnhancedBackupService.BackupProgress.InProgress) {
                android.util.Log.w("BackupViewModel", "Backup already in progress, ignoring request")
                return@launch
            }
            
            backupService.createBackup(isAutoBackup = false)
            loadAvailableBackups()
        }
    }
    
    /**
     * Create backup and share for manual cloud upload
     */
    fun createAndShareBackup() {
        viewModelScope.launch {
            // Prevent multiple simultaneous backups
            if (backupProgress.value is EnhancedBackupService.BackupProgress.InProgress) {
                android.util.Log.w("BackupViewModel", "Backup already in progress, ignoring request")
                return@launch
            }
            
            val result = backupService.createBackup(isAutoBackup = false)
            
            if (result is com.billme.app.core.backup.BackupResult.Success) {
                val backupFile = java.io.File(result.filePath)
                
                if (!backupFile.exists()) {
                    android.util.Log.e("BackupViewModel", "Backup file not found: ${result.filePath}")
                    return@launch
                }
                
                val shareIntent = backupService.createShareIntent(backupFile)
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
                
                loadAvailableBackups()
            }
        }
    }
    
    /**
     * Create backup and upload directly to Google Drive
     */
    fun syncToGoogleDrive() {
        viewModelScope.launch {
            if (!_driveSignedIn.value) {
                _driveUploadStatus.value = DriveUploadStatus.Error("Please sign in to Google Drive first")
                return@launch
            }
            
            // Prevent multiple simultaneous backups
            if (backupProgress.value is EnhancedBackupService.BackupProgress.InProgress) {
                android.util.Log.w("BackupViewModel", "Backup already in progress, ignoring request")
                return@launch
            }
            
            // Verify Drive service is initialized
            val account = googleDriveService.getSignedInAccount()
            if (account == null) {
                android.util.Log.e("BackupViewModel", "No signed-in account found")
                _driveUploadStatus.value = DriveUploadStatus.Error("Google account not found. Please sign in again.")
                _driveSignedIn.value = false
                return@launch
            }
            
            // Re-initialize Drive service to ensure it's ready
            val initSuccess = googleDriveService.initializeDriveService(account)
            if (!initSuccess) {
                android.util.Log.e("BackupViewModel", "Failed to initialize Drive service")
                _driveUploadStatus.value = DriveUploadStatus.Error("Failed to initialize Google Drive. Please sign in again.")
                _driveSignedIn.value = false
                return@launch
            }
            
            _driveUploadStatus.value = DriveUploadStatus.Uploading(0f, "Creating backup...")
            
            // Create backup
            val result = backupService.createBackup(isAutoBackup = false)
            
            if (result is com.billme.app.core.backup.BackupResult.Success) {
                val backupFile = java.io.File(result.filePath)
                
                if (!backupFile.exists()) {
                    android.util.Log.e("BackupViewModel", "Backup file not found: ${result.filePath}")
                    _driveUploadStatus.value = DriveUploadStatus.Error("Backup file not found")
                    return@launch
                }
                
                android.util.Log.d("BackupViewModel", "Backup created successfully: ${backupFile.absolutePath} (${backupFile.length()} bytes)")
                
                _driveUploadStatus.value = DriveUploadStatus.Uploading(50f, "Uploading to Google Drive...")
                
                // Upload to Google Drive with automatic cleanup using enhanced method
                val uploadResult = googleDriveService.performAutomaticBackup(backupFile)
                
                when (uploadResult) {
                    is DriveUploadResult.Success -> {
                        _driveUploadStatus.value = DriveUploadStatus.Success(uploadResult.fileName)
                        android.util.Log.d("BackupViewModel", "Successfully uploaded to Drive with cleanup: ${uploadResult.fileName} (ID: ${uploadResult.fileId})")
                    }
                    is DriveUploadResult.Error -> {
                        _driveUploadStatus.value = DriveUploadStatus.Error(uploadResult.message)
                        android.util.Log.e("BackupViewModel", "Drive upload failed: ${uploadResult.message}")
                    }
                }
                
                loadAvailableBackups()
            } else if (result is com.billme.app.core.backup.BackupResult.Error) {
                _driveUploadStatus.value = DriveUploadStatus.Error("Failed to create backup: ${result.message}")
                android.util.Log.e("BackupViewModel", "Backup creation failed: ${result.message}")
            } else {
                _driveUploadStatus.value = DriveUploadStatus.Error("Failed to create backup")
                android.util.Log.e("BackupViewModel", "Backup creation failed with unknown result type")
            }
        }
    }
    
    /**
     * Reset upload status
     */
    fun resetUploadStatus() {
        _driveUploadStatus.value = DriveUploadStatus.Idle
    }
    
    /**
     * Restore backup from Google Drive
     */
    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            // Open file picker to select backup from Google Drive
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/octet-stream"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, "Select BillMe Backup")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            try {
                context.startActivity(Intent.createChooser(intent, "Restore from Google Drive"))
            } catch (e: Exception) {
                android.util.Log.e("BackupViewModel", "Error opening file picker: ${e.message}")
            }
        }
    }
    
    /**
     * Restore latest backup directly from Google Drive
     */
    fun restoreLatestFromGoogleDrive() {
        viewModelScope.launch {
            try {
                if (!_driveSignedIn.value) {
                    _driveUploadStatus.value = DriveUploadStatus.Error("Please sign in to Google Drive first")
                    return@launch
                }
                
                // Verify Drive service is initialized
                val account = googleDriveService.getSignedInAccount()
                if (account == null) {
                    _driveUploadStatus.value = DriveUploadStatus.Error("Google account not found. Please sign in again.")
                    _driveSignedIn.value = false
                    return@launch
                }
                
                // Re-initialize Drive service
                val initSuccess = googleDriveService.initializeDriveService(account)
                if (!initSuccess) {
                    _driveUploadStatus.value = DriveUploadStatus.Error("Failed to initialize Google Drive. Please sign in again.")
                    _driveSignedIn.value = false
                    return@launch
                }
                
                _driveUploadStatus.value = DriveUploadStatus.Uploading(25f, "Fetching latest backup from Drive...")
                
                // Create temporary file for download
                val tempFile = java.io.File(context.cacheDir, "restore_latest_backup.zip")
                
                _driveUploadStatus.value = DriveUploadStatus.Uploading(50f, "Downloading latest backup...")
                
                // Download latest backup using enhanced method
                val downloadResult = googleDriveService.downloadLatestBackup(tempFile)
                
                when (downloadResult) {
                    is DriveDownloadResult.Success -> {
                        _driveUploadStatus.value = DriveUploadStatus.Uploading(75f, "Restoring backup...")
                        
                        // Restore the backup
                        val result = backupService.restoreBackup(tempFile)
                        
                        // Clean up temp file
                        tempFile.delete()
                        
                        // If restore succeeded and needs app restart, restart automatically
                        if (result is RestoreResult.Success && result.needsAppRestart) {
                            _driveUploadStatus.value = DriveUploadStatus.Success("Backup restored! Restarting app...")
                            kotlinx.coroutines.delay(1500)
                            backupService.restartApp()
                        } else {
                            _driveUploadStatus.value = DriveUploadStatus.Success("Backup restored successfully!")
                            loadAvailableBackups()
                        }
                    }
                    is DriveDownloadResult.Error -> {
                        _driveUploadStatus.value = DriveUploadStatus.Error(downloadResult.message)
                    }
                }
            } catch (e: Exception) {
                _driveUploadStatus.value = DriveUploadStatus.Error("Error: ${e.message}")
                android.util.Log.e("BackupViewModel", "Error restoring from Google Drive: ${e.message}", e)
            }
        }
    }
    
    /**
     * Restore backup from selected URI (from Google Drive or other source)
     */
    fun restoreFromUri(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                // Copy the selected file to temporary location
                val tempFile = java.io.File(context.cacheDir, "restore_backup_${System.currentTimeMillis()}.db")
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Restore from the temporary file
                val result = backupService.restoreBackup(tempFile)
                
                // Clean up temporary file
                tempFile.delete()
                
                // If restore succeeded and needs app restart, restart automatically
                if (result is RestoreResult.Success && result.needsAppRestart) {
                    kotlinx.coroutines.delay(1500)
                    backupService.restartApp()
                } else {
                    // Handle result if needed
                    loadAvailableBackups()
                }
            } catch (e: Exception) {
                android.util.Log.e("BackupViewModel", "Error restoring from URI: ${e.message}")
            }
        }
    }
    
    /**
     * Restore from a backup
     */
    fun restoreBackup(backup: BackupInfo) {
        viewModelScope.launch {
            val result = backupService.restoreBackup(backup.file)
            // If restore succeeded and needs app restart, restart automatically
            if (result is RestoreResult.Success && result.needsAppRestart) {
                // Small delay to show success message
                kotlinx.coroutines.delay(1500)
                backupService.restartApp()
            }
        }
    }
    
    /**
     * Share a backup file
     */
    fun shareBackup(backup: BackupInfo) {
        val shareIntent = backupService.createShareIntent(backup.file)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
    
    /**
     * Delete a backup
     */
    fun deleteBackup(backup: BackupInfo) {
        viewModelScope.launch {
            backupService.deleteBackup(backup.file)
            loadAvailableBackups()
        }
    }
    
    /**
     * Enable or disable daily auto-backup
     */
    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // Update settings
            settingsRepository.updateAutoBackupEnabled(enabled)
            
            // Schedule or cancel backup based on setting
            if (enabled) {
                backupScheduler.scheduleDailyBackup()
            } else {
                backupScheduler.cancelDailyBackup()
            }
        }
    }
    
    /**
     * Check if daily auto-backup is enabled
     */
    suspend fun isAutoBackupEnabled(): Boolean {
        return settingsRepository.isAutoBackupEnabled()
    }
    
    /**
     * Check if daily backup is currently scheduled
     */
    fun isDailyBackupScheduled(): Boolean {
        return backupScheduler.isDailyBackupScheduled()
    }
    
    /**
     * Upload the latest auto-backup to Google Drive
     */
    fun uploadLatestAutoBackupToDrive() {
        viewModelScope.launch {
            try {
                // Get the latest auto-backup file
                val latestBackup = backupService.getAvailableBackups()
                    .filter { it.isAutoBackup }
                    .maxByOrNull { it.timestamp }
                
                if (latestBackup == null) {
                    android.util.Log.e("BackupViewModel", "No auto-backup found to upload")
                    return@launch
                }
                
                val backupFile = latestBackup.file
                if (!backupFile.exists()) {
                    android.util.Log.e("BackupViewModel", "Auto-backup file not found")
                    return@launch
                }
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    backupFile
                )
                
                val driveIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, backupFile.name)
                    putExtra(Intent.EXTRA_SUBJECT, "BillMe Auto-Backup")
                    putExtra(Intent.EXTRA_TEXT, "Auto-backup created on ${java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(latestBackup.timestamp))}")
                    setPackage("com.google.android.apps.docs")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                try {
                    context.startActivity(driveIntent)
                } catch (e: android.content.ActivityNotFoundException) {
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TITLE, backupFile.name)
                        putExtra(Intent.EXTRA_SUBJECT, "BillMe Auto-Backup")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(fallbackIntent, "Upload Auto-Backup"))
                }
            } catch (e: Exception) {
                android.util.Log.e("BackupViewModel", "Error uploading auto-backup: ${e.message}", e)
            }
        }
    }
    
    /**
     * Upload latest auto-backup to Google Drive using OAuth
     */
    fun uploadLatestAutoBackupToDriveOAuth() {
        viewModelScope.launch {
            if (!_driveSignedIn.value) {
                _driveUploadStatus.value = DriveUploadStatus.Error("Please sign in to Google Drive first")
                return@launch
            }
            
            try {
                val latestBackup = backupService.getAvailableBackups()
                    .filter { it.isAutoBackup }
                    .maxByOrNull { it.timestamp }
                
                if (latestBackup == null) {
                    _driveUploadStatus.value = DriveUploadStatus.Error("No auto-backup found")
                    return@launch
                }
                
                if (!latestBackup.file.exists()) {
                    _driveUploadStatus.value = DriveUploadStatus.Error("Auto-backup file not found")
                    return@launch
                }
                
                _driveUploadStatus.value = DriveUploadStatus.Uploading(25f, "Uploading to Google Drive...")
                
                val uploadResult = googleDriveService.uploadBackup(latestBackup.file)
                
                when (uploadResult) {
                    is DriveUploadResult.Success -> {
                        _driveUploadStatus.value = DriveUploadStatus.Success(uploadResult.fileName)
                        android.util.Log.d("BackupViewModel", "Successfully uploaded auto-backup to Drive")
                    }
                    is DriveUploadResult.Error -> {
                        _driveUploadStatus.value = DriveUploadStatus.Error(uploadResult.message)
                        android.util.Log.e("BackupViewModel", "Auto-backup upload failed: ${uploadResult.message}")
                    }
                }
            } catch (e: Exception) {
                _driveUploadStatus.value = DriveUploadStatus.Error(e.message ?: "Unknown error")
                android.util.Log.e("BackupViewModel", "Error uploading auto-backup: ${e.message}", e)
            }
        }
    }
}

/**
 * Upload status for Google Drive operations
 */
sealed class DriveUploadStatus {
    object Idle : DriveUploadStatus()
    data class Uploading(val progress: Float = 0f, val message: String = "") : DriveUploadStatus()
    data class Success(val fileName: String) : DriveUploadStatus()
    data class Error(val message: String) : DriveUploadStatus()
}
