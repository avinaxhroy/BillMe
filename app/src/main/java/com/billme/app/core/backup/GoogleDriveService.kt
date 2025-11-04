package com.billme.app.core.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.FileContent
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Robust Google Drive integration service for automatic cloud backups
 * Handles OAuth authentication and file upload/download with proper error handling
 */
@Singleton
class GoogleDriveService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GoogleDriveService"
        private const val BACKUP_FOLDER_NAME = "BillMe Backups"
        const val RC_SIGN_IN = 9001
        
        // Using only DRIVE_FILE scope as per Android best practices
        private val DRIVE_SCOPES = listOf(DriveScopes.DRIVE_FILE)
    }

    private var driveService: Drive? = null
    private var backupFolderId: String? = null
    private var lastError: String? = null

    /**
     * Get the last error message from initialization
     */
    fun getLastError(): String? = lastError

    /**
     * Get GoogleSignInClient for authentication
     * Using proper Android OAuth flow without ID token request
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        Log.d(TAG, "Creating GoogleSignInClient with Drive scopes")
        
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_FILE)
            )
            .build()

        return GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Get sign-in intent for OAuth flow
     */
    fun getSignInIntent(): Intent {
        Log.d(TAG, "Getting sign-in intent")
        return try {
            val client = getGoogleSignInClient()
            val intent = client.signInIntent
            Log.d(TAG, "Sign-in intent created successfully")
            intent
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sign-in intent: ${e.message}", e)
            throw IllegalStateException("Failed to create sign-in intent", e)
        }
    }

    /**
     * Check if user is already signed in to Google Drive
     */
    fun isSignedIn(): Boolean {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val hasPermissions = account != null && GoogleSignIn.hasPermissions(
                account,
                Scope(DriveScopes.DRIVE_FILE)
            )
            Log.d(TAG, "Sign-in check: account=${account?.email}, hasPermissions=$hasPermissions")
            hasPermissions
        } catch (e: Exception) {
            Log.e(TAG, "Error checking sign-in status", e)
            false
        }
    }

    /**
     * Get the signed-in account
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return try {
            GoogleSignIn.getLastSignedInAccount(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signed-in account", e)
            null
        }
    }

    /**
     * Initialize Drive service with signed-in account
     * Improved with better error handling and validation
     */
    suspend fun initializeDriveService(account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            lastError = null // Clear previous error
            
            Log.d(TAG, "=== Initializing Drive service ===")
            Log.d(TAG, "Account email: ${account.email}")
            Log.d(TAG, "Account ID: ${account.id}")
            Log.d(TAG, "Account granted scopes: ${account.grantedScopes}")
            
            // Verify the account has the necessary permissions
            val hasPermission = GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
            Log.d(TAG, "Has DRIVE_FILE permission: $hasPermission")
            
            if (!hasPermission) {
                lastError = "Drive permissions not granted"
                Log.e(TAG, "Account does not have DRIVE_FILE permission")
                return@withContext false
            }
            
            // Create credential with Drive scopes using the account
            Log.d(TAG, "Creating GoogleAccountCredential...")
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                DRIVE_SCOPES
            )
            
            // Set the account
            if (account.account != null) {
                credential.selectedAccount = account.account
                Log.d(TAG, "Selected account: ${account.account?.name}")
            } else {
                Log.w(TAG, "Account.account is null, using email")
                credential.selectedAccountName = account.email
            }
            
            // Force token fetch to ensure credentials work
            try {
                Log.d(TAG, "Fetching OAuth token...")
                val token = credential.token
                if (token == null) {
                    lastError = "Failed to obtain OAuth token"
                    Log.e(TAG, "Token is null after fetch")
                    return@withContext false
                }
                Log.d(TAG, "✓ OAuth token obtained successfully")
            } catch (e: Exception) {
                lastError = "Token fetch failed: ${e.message}"
                Log.e(TAG, "Failed to fetch token", e)
                return@withContext false
            }
            
            Log.d(TAG, "Building Drive service...")

            // Build Drive service with error handling
            driveService = try {
                Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName("BillMe")
                    .build()
            } catch (e: Exception) {
                lastError = "Failed to create Drive service: ${e.message}"
                Log.e(TAG, "Failed to build Drive service", e)
                throw e
            }

            Log.d(TAG, "✓ Drive service created successfully")
            Log.d(TAG, "✓ Drive service initialized - ready for operations")
            
            // Note: We'll get/create the backup folder lazily when first needed
            // This avoids network calls during initialization
            
            true
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "✗ Failed to initialize Drive service", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Error cause: ${e.cause?.message}")
            e.printStackTrace()
            driveService = null
            backupFolderId = null
            false
        }
    }

    /**
     * Sign out from Google Drive
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            getGoogleSignInClient().signOut()
            driveService = null
            backupFolderId = null
            Log.d(TAG, "Signed out from Google Drive")
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
        }
    }

    /**
     * Get or create the backup folder in Google Drive
     */
    private suspend fun getOrCreateBackupFolder(): String = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw IllegalStateException("Drive service not initialized")

        try {
            Log.d(TAG, "=== Getting/Creating backup folder ===")
            Log.d(TAG, "Searching for existing backup folder: $BACKUP_FOLDER_NAME")
            
            // Search for existing backup folder
            val result = drive.files().list()
                .setQ("name='$BACKUP_FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) {
                Log.d(TAG, "✓ Found existing backup folder: ${result.files[0].id}")
                return@withContext result.files[0].id
            }

            Log.d(TAG, "Backup folder not found, creating new folder...")
            
            // Create new backup folder
            val folderMetadata = DriveFile().apply {
                name = BACKUP_FOLDER_NAME
                mimeType = "application/vnd.google-apps.folder"
            }

            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()

            Log.d(TAG, "✓ Created backup folder successfully: ${folder.id}")
            folder.id
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error getting/creating backup folder", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Upload backup file to Google Drive
     */
    suspend fun uploadBackup(backupFile: File): DriveUploadResult = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext DriveUploadResult.Error("Drive service not initialized. Please sign in first.")
        
        try {
            // Get or create backup folder if not already done
            if (backupFolderId == null) {
                Log.d(TAG, "Backup folder not initialized, getting/creating it now...")
                backupFolderId = getOrCreateBackupFolder()
            }
            
            val folderId = backupFolderId ?: return@withContext DriveUploadResult.Error("Failed to access backup folder")
            
            if (!backupFile.exists()) {
                return@withContext DriveUploadResult.Error("Backup file not found")
            }

            Log.d(TAG, "Uploading backup: ${backupFile.name} (${backupFile.length()} bytes)")

            // Check if file with same name already exists
            val existingFiles = drive.files().list()
                .setQ("name='${backupFile.name}' and '${folderId}' in parents and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val fileMetadata = DriveFile().apply {
                name = backupFile.name
                parents = listOf(folderId)
                mimeType = "application/zip"
                description = "BillMe backup created on ${java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
            }

            val mediaContent = FileContent("application/zip", backupFile)

            val uploadedFile = if (existingFiles.files.isNotEmpty()) {
                // Update existing file
                Log.d(TAG, "Updating existing file: ${existingFiles.files[0].id}")
                drive.files().update(existingFiles.files[0].id, fileMetadata, mediaContent)
                    .setFields("id, name, size, modifiedTime")
                    .execute()
            } else {
                // Create new file
                Log.d(TAG, "Creating new file")
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, size, modifiedTime")
                    .execute()
            }

            Log.d(TAG, "Upload successful: ${uploadedFile.id}")
            DriveUploadResult.Success(uploadedFile.id, uploadedFile.name)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed - Exception type: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Upload failed - Message: ${e.message}")
            Log.e(TAG, "Upload failed - Cause: ${e.cause?.message}")
            e.printStackTrace()
            
            // Get the actual error message
            val actualError = e.message ?: "Unknown error"
            Log.e(TAG, "Actual error string: $actualError")
            
            // Return the real error without trying to guess
            DriveUploadResult.Error("Upload failed: $actualError")
        }
    }

    /**
     * List all backup files in Google Drive
     */
    suspend fun listBackups(): List<DriveBackupInfo> = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext emptyList()
        
        try {
            // Get or create backup folder if not already done
            if (backupFolderId == null) {
                Log.d(TAG, "Backup folder not initialized, getting/creating it now...")
                backupFolderId = getOrCreateBackupFolder()
            }
            
            val folderId = backupFolderId ?: return@withContext emptyList()

            val result = drive.files().list()
                .setQ("'${folderId}' in parents and trashed=false and mimeType='application/zip'")
                .setSpaces("drive")
                .setFields("files(id, name, size, modifiedTime, createdTime)")
                .setOrderBy("modifiedTime desc")
                .execute()

            result.files.map { file ->
                DriveBackupInfo(
                    id = file.id,
                    name = file.name,
                    size = file.getSize() ?: 0,
                    modifiedTime = file.modifiedTime?.value ?: 0,
                    createdTime = file.createdTime?.value ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing backups", e)
            emptyList()
        }
    }

    /**
     * Download backup file from Google Drive
     */
    suspend fun downloadBackup(fileId: String, destinationFile: File): DriveDownloadResult = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext DriveDownloadResult.Error("Drive service not initialized")

        try {
            Log.d(TAG, "Downloading backup: $fileId")

            destinationFile.parentFile?.mkdirs()
            destinationFile.outputStream().use { outputStream ->
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }

            Log.d(TAG, "Download successful: ${destinationFile.absolutePath}")
            DriveDownloadResult.Success(destinationFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            DriveDownloadResult.Error("Download failed: ${e.message}")
        }
    }

    /**
     * Delete backup file from Google Drive
     */
    suspend fun deleteBackup(fileId: String): Boolean = withContext(Dispatchers.IO) {
        val drive = driveService ?: return@withContext false

        try {
            drive.files().delete(fileId).execute()
            Log.d(TAG, "Deleted backup: $fileId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup", e)
            false
        }
    }

    /**
     * Get the latest backup file from Google Drive
     * Returns null if no backups exist
     */
    suspend fun getLatestBackup(): DriveBackupInfo? = withContext(Dispatchers.IO) {
        try {
            val backups = listBackups()
            if (backups.isEmpty()) {
                Log.d(TAG, "No backups found in Google Drive")
                return@withContext null
            }
            
            val latest = backups.first() // Already sorted by modifiedTime desc
            Log.d(TAG, "Latest backup: ${latest.name} (${latest.modifiedTime})")
            latest
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest backup", e)
            null
        }
    }

    /**
     * Download and restore the latest backup from Google Drive
     * Returns the path to the downloaded file or null if failed
     */
    suspend fun downloadLatestBackup(destinationFile: File): DriveDownloadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Downloading latest backup ===")
            
            val latestBackup = getLatestBackup()
            if (latestBackup == null) {
                Log.e(TAG, "No backups available to download")
                return@withContext DriveDownloadResult.Error("No backups found in Google Drive")
            }
            
            Log.d(TAG, "Downloading: ${latestBackup.name} (${latestBackup.size} bytes)")
            val result = downloadBackup(latestBackup.id, destinationFile)
            
            when (result) {
                is DriveDownloadResult.Success -> {
                    Log.d(TAG, "✓ Latest backup downloaded successfully")
                }
                is DriveDownloadResult.Error -> {
                    Log.e(TAG, "✗ Failed to download latest backup: ${result.message}")
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error downloading latest backup", e)
            DriveDownloadResult.Error("Failed to download latest backup: ${e.message}")
        }
    }

    /**
     * Cleanup old backups keeping only the most recent ones
     * @param keepCount Number of recent backups to keep (default: 5)
     * @return Number of backups deleted
     */
    suspend fun cleanupOldBackups(keepCount: Int = 5): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Cleaning up old backups (keeping $keepCount most recent) ===")
            
            val allBackups = listBackups()
            if (allBackups.isEmpty()) {
                Log.d(TAG, "No backups to clean up")
                return@withContext 0
            }
            
            Log.d(TAG, "Found ${allBackups.size} total backups")
            
            // Check if latest backup exists
            if (allBackups.isEmpty()) {
                Log.d(TAG, "No backups exist, skipping cleanup")
                return@withContext 0
            }
            
            // Keep only the most recent backups, delete the rest
            if (allBackups.size <= keepCount) {
                Log.d(TAG, "Only ${allBackups.size} backups exist, no cleanup needed")
                return@withContext 0
            }
            
            val backupsToDelete = allBackups.drop(keepCount)
            Log.d(TAG, "Deleting ${backupsToDelete.size} old backups...")
            
            var deletedCount = 0
            backupsToDelete.forEach { backup ->
                Log.d(TAG, "Deleting: ${backup.name} (modified: ${java.util.Date(backup.modifiedTime)})")
                if (deleteBackup(backup.id)) {
                    deletedCount++
                }
            }
            
            Log.d(TAG, "✓ Cleanup complete: deleted $deletedCount backups")
            Log.d(TAG, "✓ Remaining backups: ${allBackups.size - deletedCount}")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error cleaning up old backups", e)
            0
        }
    }

    /**
     * Perform automatic backup with cleanup
     * 1. Uploads the backup file to Google Drive
     * 2. Cleans up old backups (keeps only 5 most recent)
     * @return DriveUploadResult indicating success or failure
     */
    suspend fun performAutomaticBackup(backupFile: File): DriveUploadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Starting automatic backup ===")
            
            // Upload the backup
            val uploadResult = uploadBackup(backupFile)
            
            when (uploadResult) {
                is DriveUploadResult.Success -> {
                    Log.d(TAG, "✓ Backup uploaded successfully: ${uploadResult.fileName}")
                    
                    // Cleanup old backups after successful upload
                    Log.d(TAG, "Cleaning up old backups...")
                    val deletedCount = cleanupOldBackups(keepCount = 5)
                    Log.d(TAG, "✓ Automatic backup complete (deleted $deletedCount old backups)")
                }
                is DriveUploadResult.Error -> {
                    Log.e(TAG, "✗ Backup upload failed: ${uploadResult.message}")
                }
            }
            
            uploadResult
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error performing automatic backup", e)
            DriveUploadResult.Error("Automatic backup failed: ${e.message}")
        }
    }

    /**
     * Check if Drive service is properly initialized and ready for operations
     */
    fun isDriveServiceInitialized(): Boolean {
        val initialized = driveService != null
        Log.d(TAG, "Drive service initialized: $initialized")
        return initialized
    }

    /**
     * Request additional Drive permissions if not already granted
     * Returns true if permissions are already granted, false if need to request
     */
    fun hasRequiredPermissions(account: GoogleSignInAccount): Boolean {
        val hasPermission = GoogleSignIn.hasPermissions(
            account,
            Scope(DriveScopes.DRIVE_FILE)
        )
        Log.d(TAG, "Has required permissions: $hasPermission")
        return hasPermission
    }
}

/**
 * Result types for Drive operations
 */
sealed class DriveUploadResult {
    data class Success(val fileId: String, val fileName: String) : DriveUploadResult()
    data class Error(val message: String) : DriveUploadResult()
}

sealed class DriveDownloadResult {
    data class Success(val filePath: String) : DriveDownloadResult()
    data class Error(val message: String) : DriveDownloadResult()
}

/**
 * Drive backup file information
 */
data class DriveBackupInfo(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedTime: Long,
    val createdTime: Long
)
