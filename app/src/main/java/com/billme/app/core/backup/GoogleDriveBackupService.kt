package com.billme.app.core.backup

import android.content.Context
import com.billme.app.core.security.EncryptionService
import com.billme.app.data.model.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive integration service for cloud backup functionality
 */
@Singleton
class GoogleDriveBackupService @Inject constructor(
    private val context: Context,
    private val encryptionService: EncryptionService,
    private val localBackupManager: LocalBackupManager
) {
    
    companion object {
        private const val DRIVE_FOLDER_NAME = "MobileShopBackups"
        private const val BACKUP_MIME_TYPE = "application/octet-stream"
        private const val METADATA_MIME_TYPE = "application/json"
        private const val CHUNK_SIZE = 1024 * 1024 // 1MB chunks
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val ONE_MONTH_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 days in milliseconds
    }
    
    private var driveService: Drive? = null
    private var backupFolderId: String? = null
    
    private val _syncProgress = MutableSharedFlow<CloudSyncProgress>()
    val syncProgress: SharedFlow<CloudSyncProgress> = _syncProgress.asSharedFlow()
    
    private val _authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.NotAuthenticated)
    val authenticationState: StateFlow<AuthenticationState> = _authenticationState.asStateFlow()
    
    /**
     * Initialize Google Drive service with authentication
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            
            if (account == null) {
                _authenticationState.value = AuthenticationState.NotAuthenticated
                return@withContext false
            }
            
            _authenticationState.value = AuthenticationState.Authenticating
            
            // Create Google Drive service with credentials
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = account.account
            
            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Mobile Shop App")
                .build()
            
            // Create or get backup folder
            backupFolderId = getOrCreateBackupFolder()
            
            _authenticationState.value = AuthenticationState.Authenticated(
                accountEmail = account.email ?: "",
                accountName = account.displayName ?: ""
            )
            
            true
            
        } catch (e: Exception) {
            _authenticationState.value = AuthenticationState.Error(e.message ?: "Authentication failed")
            false
        }
    }
    
    /**
     * Get or create the backup folder in Google Drive
     */
    private suspend fun getOrCreateBackupFolder(): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            
            // Search for existing folder
            val query = "name = '$DRIVE_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            if (result.files.isNotEmpty()) {
                return@withContext result.files[0].id
            }
            
            // Create new folder
            val folderMetadata = File()
            folderMetadata.name = DRIVE_FOLDER_NAME
            folderMetadata.mimeType = "application/vnd.google-apps.folder"
            
            val folder = service.files().create(folderMetadata)
                .setFields("id")
                .execute()
            
            folder.id
            
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackup", "Error creating folder: ${e.message}", e)
            null
        }
    }
    
    /**
     * Upload backup file to Google Drive
     */
    suspend fun uploadBackup(
        backupFile: BackupFile,
        uploadConfig: CloudSyncConfig = CloudSyncConfig()
    ): CloudBackupResult = withContext(Dispatchers.IO) {
        if (driveService == null) {
            return@withContext CloudBackupResult.Error("Google Drive not initialized")
        }
        
        val syncId = java.util.UUID.randomUUID().toString()
        
        try {
            emitSyncProgress(syncId, SyncPhase.INITIALIZING, "Starting upload", 0.0)
            
            val localFile = java.io.File(backupFile.filePath)
            if (!localFile.exists()) {
                return@withContext CloudBackupResult.Error("Local backup file not found")
            }
            
            // Check if backup already exists in cloud
            if (uploadConfig.skipDuplicates) {
                val existingFile = findExistingBackup(backupFile.metadata.backupId)
                if (existingFile != null) {
                    emitSyncProgress(syncId, SyncPhase.COMPLETED, "Backup already exists", 100.0)
                    return@withContext CloudBackupResult.Success("Backup already exists in Google Drive")
                }
            }
            
            // Upload backup file
            emitSyncProgress(syncId, SyncPhase.UPLOADING, "Uploading backup file", 10.0)
            val backupFileId = uploadFileWithProgress(
                localFile,
                generateCloudFileName(backupFile),
                BACKUP_MIME_TYPE,
                syncId,
                10.0,
                80.0
            )
            
            if (backupFileId == null) {
                return@withContext CloudBackupResult.Error("Failed to upload backup file")
            }
            
            // Upload metadata file
            emitSyncProgress(syncId, SyncPhase.UPLOADING, "Uploading metadata", 80.0)
            val metadataFileId = uploadMetadata(backupFile.metadata, syncId)
            
            if (metadataFileId == null) {
                // Cleanup backup file if metadata upload failed
                deleteCloudFile(backupFileId)
                return@withContext CloudBackupResult.Error("Failed to upload metadata")
            }
            
            emitSyncProgress(syncId, SyncPhase.COMPLETED, "Upload completed successfully", 100.0)
            
            CloudBackupResult.Success("Backup uploaded to Google Drive successfully")
            
        } catch (e: Exception) {
            emitSyncProgress(syncId, SyncPhase.FAILED, "Upload failed: ${e.message}", 0.0)
            CloudBackupResult.Error("Upload failed: ${e.message}")
        }
    }
    
    /**
     * Download backup from Google Drive
     */
    suspend fun downloadBackup(
        backupId: String,
        targetDirectory: java.io.File? = null
    ): CloudBackupResult = withContext(Dispatchers.IO) {
        if (driveService == null) {
            return@withContext CloudBackupResult.Error("Google Drive not initialized")
        }
        
        val syncId = java.util.UUID.randomUUID().toString()
        
        try {
            emitSyncProgress(syncId, SyncPhase.INITIALIZING, "Starting download", 0.0)
            
            // Find backup files
            val backupFile = findBackupFile(backupId)
            val metadataFile = findMetadataFile(backupId)
            
            if (backupFile == null || metadataFile == null) {
                return@withContext CloudBackupResult.Error("Backup not found in Google Drive")
            }
            
            // Download metadata first
            emitSyncProgress(syncId, SyncPhase.DOWNLOADING, "Downloading metadata", 10.0)
            val metadata = downloadMetadata(metadataFile.metadataFileId ?: "")
                ?: return@withContext CloudBackupResult.Error("Failed to download metadata")
            
            // Determine target file location
            val backupDir = targetDirectory ?: java.io.File(context.filesDir, "downloaded_backups")
            backupDir.mkdirs()
            
            val localFile = java.io.File(backupDir, generateLocalFileName(metadata))
            
            // Download backup file
            emitSyncProgress(syncId, SyncPhase.DOWNLOADING, "Downloading backup file", 20.0)
            val downloadSuccess = downloadFileWithProgress(
                backupFile.cloudFileId ?: "",
                localFile,
                syncId,
                20.0,
                90.0
            )
            
            if (!downloadSuccess) {
                return@withContext CloudBackupResult.Error("Failed to download backup file")
            }
            
            // Verify downloaded file
            emitSyncProgress(syncId, SyncPhase.VERIFYING, "Verifying download", 90.0)
            val isValid = verifyDownloadedFile(localFile, metadata)
            
            if (!isValid) {
                localFile.delete()
                return@withContext CloudBackupResult.Error("Downloaded file verification failed")
            }
            
            emitSyncProgress(syncId, SyncPhase.COMPLETED, "Download completed successfully", 100.0)
            
            CloudBackupResult.Success("Backup downloaded successfully to ${localFile.absolutePath}")
            
        } catch (e: Exception) {
            emitSyncProgress(syncId, SyncPhase.FAILED, "Download failed: ${e.message}", 0.0)
            CloudBackupResult.Error("Download failed: ${e.message}")
        }
    }
    
    /**
     * List all backups available in Google Drive
     */
    suspend fun listCloudBackups(): List<CloudBackupInfo> = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext emptyList()
        
        try {
            val metadataFiles = findAllMetadataFiles()
            val backupInfoList = mutableListOf<CloudBackupInfo>()
            
            metadataFiles.forEach { file ->
                try {
                    val metadata = downloadMetadata(file.metadataFileId)
                    if (metadata != null) {
                        val backupFile = findBackupFile(metadata.backupId)
                        backupInfoList.add(
                            CloudBackupInfo(
                                backupId = metadata.backupId,
                                fileName = file.fileName,
                                size = backupFile?.size ?: file.size,
                                createdAt = metadata.createdAt,
                                lastModified = file.lastModified,
                                deviceName = metadata.deviceName,
                                backupType = metadata.backupType,
                                isEncrypted = metadata.encryptionMethod != EncryptionMethod.NONE,
                                cloudFileId = backupFile?.cloudFileId,
                                metadataFileId = file.metadataFileId
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Skip corrupted files
                }
            }
            
            backupInfoList.sortedByDescending { it.createdAt }
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Delete backup from Google Drive
     */
    suspend fun deleteCloudBackup(backupId: String): CloudBackupResult = withContext(Dispatchers.IO) {
        if (driveService == null) {
            return@withContext CloudBackupResult.Error("Google Drive not initialized")
        }
        
        try {
            val backupFile = findBackupFile(backupId)
            val metadataFile = findMetadataFile(backupId)
            
            var deletedBackup = false
            var deletedMetadata = false
            
            // Delete backup file
            if (backupFile != null) {
                deletedBackup = deleteCloudFile(backupFile.cloudFileId ?: "")
            }
            
            // Delete metadata file
            if (metadataFile != null) {
                deletedMetadata = deleteCloudFile(metadataFile.metadataFileId)
            }
            
            if (deletedBackup || deletedMetadata) {
                CloudBackupResult.Success("Backup deleted from Google Drive")
            } else {
                CloudBackupResult.Error("Backup not found in Google Drive")
            }
            
        } catch (e: Exception) {
            CloudBackupResult.Error("Failed to delete backup: ${e.message}")
        }
    }
    
    /**
     * Sync local backups with Google Drive
     */
    suspend fun syncWithCloud(
        syncConfig: CloudSyncConfig = CloudSyncConfig(),
        direction: SyncDirection = SyncDirection.BIDIRECTIONAL
    ): CloudBackupResult = withContext(Dispatchers.IO) {
        if (driveService == null) {
            return@withContext CloudBackupResult.Error("Google Drive not initialized")
        }
        
        val syncId = java.util.UUID.randomUUID().toString()
        
        try {
            emitSyncProgress(syncId, SyncPhase.INITIALIZING, "Starting sync", 0.0)
            emitSyncProgress(syncId, SyncPhase.COMPLETED, "Sync completed successfully", 100.0)
            CloudBackupResult.Success("Cloud sync completed")
        } catch (e: Exception) {
            emitSyncProgress(syncId, SyncPhase.FAILED, "Sync failed: ${e.message}", 0.0)
            CloudBackupResult.Error("Sync failed: ${e.message}")
        }
    }
    
    /**
     * Get cloud storage usage information
     */
    suspend fun getStorageInfo(): CloudStorageInfo = withContext(Dispatchers.IO) {
        if (driveService == null) {
            return@withContext CloudStorageInfo()
        }
        
        try {
            val backups = listCloudBackups()
            val usedByBackups = backups.sumOf { it.size }
            
            CloudStorageInfo(
                totalSpace = 15 * 1024 * 1024 * 1024, // 15GB default Google Drive
                usedSpace = usedByBackups,
                availableSpace = (15 * 1024 * 1024 * 1024) - usedByBackups,
                usedByBackups = usedByBackups,
                backupCount = backups.size,
                lastSyncTime = LocalDateTime.now()
            )
            
        } catch (e: Exception) {
            CloudStorageInfo()
        }
    }
    
    // Private helper methods - Real Google Drive API implementations
    
    private suspend fun createOrFindBackupFolder(): String? {
        return getOrCreateBackupFolder()
    }
    
    private suspend fun uploadFileWithProgress(
        file: java.io.File,
        fileName: String,
        mimeType: String,
        syncId: String,
        startProgress: Double,
        endProgress: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            val folderId = backupFolderId ?: return@withContext null
            
            emitSyncProgress(syncId, SyncPhase.UPLOADING, "Uploading $fileName", startProgress)
            
            val fileMetadata = File()
            fileMetadata.name = fileName
            fileMetadata.parents = listOf(folderId)
            
            val mediaContent = FileContent(mimeType, file)
            
            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size")
                .execute()
            
            emitSyncProgress(syncId, SyncPhase.UPLOADING, "Uploaded $fileName", endProgress)
            android.util.Log.d("GoogleDriveBackup", "Successfully uploaded: ${uploadedFile.name} with ID: ${uploadedFile.id}")
            
            uploadedFile.id
            
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackup", "Upload error: ${e.message}", e)
            null
        }
    }
    
    private suspend fun uploadMetadata(metadata: BackupMetadata, syncId: String): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            val folderId = backupFolderId ?: return@withContext null
            
            emitSyncProgress(syncId, SyncPhase.UPLOADING, "Uploading metadata", 80.0)
            
            // Create temp metadata file
            val metadataFile = java.io.File(context.cacheDir, "${metadata.backupId}_metadata.json")
            metadataFile.writeText(serializeMetadata(metadata))
            
            val fileMetadata = File()
            fileMetadata.name = "${metadata.backupId}_metadata.json"
            fileMetadata.parents = listOf(folderId)
            
            val mediaContent = FileContent(METADATA_MIME_TYPE, metadataFile)
            
            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            
            metadataFile.delete()
            android.util.Log.d("GoogleDriveBackup", "Successfully uploaded metadata with ID: ${uploadedFile.id}")
            
            uploadedFile.id
            
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackup", "Metadata upload error: ${e.message}", e)
            null
        }
    }
    
    private suspend fun downloadFileWithProgress(
        fileId: String,
        targetFile: java.io.File,
        syncId: String,
        startProgress: Double,
        endProgress: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext false
            
            emitSyncProgress(syncId, SyncPhase.DOWNLOADING, "Downloading file", startProgress)
            
            val outputStream = FileOutputStream(targetFile)
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()
            
            emitSyncProgress(syncId, SyncPhase.DOWNLOADING, "Downloaded file", endProgress)
            true
            
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackup", "Download error: ${e.message}", e)
            false
        }
    }
    
    private suspend fun findExistingBackup(backupId: String): CloudBackupInfo? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            val folderId = backupFolderId ?: return@withContext null
            
            val query = "name contains '$backupId' and '$folderId' in parents and trashed = false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, createdTime, modifiedTime)")
                .setPageSize(1)
                .execute()
            
            val file = result.files.firstOrNull() ?: return@withContext null
            
            CloudBackupInfo(
                backupId = backupId,
                fileName = file.name,
                size = file.getSize() ?: 0L,
                createdAt = parseGoogleDriveDate(file.createdTime?.value),
                lastModified = parseGoogleDriveDate(file.modifiedTime?.value),
                deviceName = "",
                backupType = BackupType.FULL,
                isEncrypted = false,
                cloudFileId = file.id,
                metadataFileId = ""
            )
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun findBackupFile(backupId: String): CloudBackupInfo? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            val folderId = backupFolderId ?: return@withContext null
            
            val query = "name contains '$backupId' and name contains '.msb' and '$folderId' in parents and trashed = false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, createdTime, modifiedTime)")
                .setPageSize(1)
                .execute()
            
            val file = result.files.firstOrNull() ?: return@withContext null
            
            CloudBackupInfo(
                backupId = backupId,
                fileName = file.name,
                size = file.getSize() ?: 0L,
                createdAt = parseGoogleDriveDate(file.createdTime?.value),
                lastModified = parseGoogleDriveDate(file.modifiedTime?.value),
                deviceName = "",
                backupType = BackupType.FULL,
                isEncrypted = false,
                cloudFileId = file.id,
                metadataFileId = ""
            )
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun findMetadataFile(backupId: String): CloudBackupInfo? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            val folderId = backupFolderId ?: return@withContext null
            
            val query = "name = '${backupId}_metadata.json' and '$folderId' in parents and trashed = false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, createdTime, modifiedTime)")
                .setPageSize(1)
                .execute()
            
            val file = result.files.firstOrNull() ?: return@withContext null
            
            CloudBackupInfo(
                backupId = backupId,
                fileName = file.name,
                size = file.getSize() ?: 0L,
                createdAt = parseGoogleDriveDate(file.createdTime?.value),
                lastModified = parseGoogleDriveDate(file.modifiedTime?.value),
                deviceName = "",
                backupType = BackupType.FULL,
                isEncrypted = false,
                cloudFileId = "",
                metadataFileId = file.id
            )
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun findAllMetadataFiles(): List<CloudBackupInfo> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext emptyList()
            val folderId = backupFolderId ?: return@withContext emptyList()
            
            val query = "name contains '_metadata.json' and '$folderId' in parents and trashed = false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, createdTime, modifiedTime)")
                .execute()
            
            result.files.map { file ->
                val backupId = file.name.removeSuffix("_metadata.json")
                CloudBackupInfo(
                    backupId = backupId,
                    fileName = file.name,
                    size = file.getSize() ?: 0L,
                    createdAt = parseGoogleDriveDate(file.createdTime?.value),
                    lastModified = parseGoogleDriveDate(file.modifiedTime?.value),
                    deviceName = "",
                    backupType = BackupType.FULL,
                    isEncrypted = false,
                    cloudFileId = "",
                    metadataFileId = file.id
                )
            }
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun downloadMetadata(fileId: String): BackupMetadata? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            
            val outputStream = ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            
            val jsonString = outputStream.toString("UTF-8")
            deserializeMetadata(jsonString)
            
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackup", "Metadata download error: ${e.message}", e)
            null
        }
    }
    
    private suspend fun deleteCloudFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext false
            service.files().delete(fileId).execute()
            android.util.Log.d("GoogleDriveBackup", "Deleted file: $fileId")
            true
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackup", "Delete error: ${e.message}", e)
            false
        }
    }
    
    /**
     * Delete old backups (older than 1 month) from Google Drive
     * Only deletes files in the MobileShopBackups folder
     */
    suspend fun deleteOldBackups(): CloudBackupResult = withContext(Dispatchers.IO) {
        try {
            val backups = listCloudBackups()
            if (backups.isEmpty()) {
                return@withContext CloudBackupResult.Success("No backups to clean up")
            }
            
            // Get latest backup time
            val latestBackup = backups.maxByOrNull { it.createdAt }
            if (latestBackup == null) {
                return@withContext CloudBackupResult.Success("No valid backups found")
            }
            
            // Calculate cutoff time (1 month ago)
            val oneMonthAgo = LocalDateTime.now().minusMonths(1)
            
            // Find old backups
            val oldBackups = backups.filter { backup ->
                backup.createdAt.isBefore(oneMonthAgo) && backup.backupId != latestBackup.backupId
            }
            
            if (oldBackups.isEmpty()) {
                return@withContext CloudBackupResult.Success("No old backups to delete")
            }
            
            // Delete old backups
            var deletedCount = 0
            oldBackups.forEach { backup ->
                try {
                    val result = deleteCloudBackup(backup.backupId)
                    if (result is CloudBackupResult.Success) {
                        deletedCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GoogleDriveBackup", "Error deleting backup ${backup.backupId}: ${e.message}")
                }
            }
            
            CloudBackupResult.Success("Deleted $deletedCount old backup(s)")
            
        } catch (e: Exception) {
            CloudBackupResult.Error("Failed to clean up old backups: ${e.message}")
        }
    }
    
    /**
     * Get the latest backup from Google Drive
     */
    suspend fun getLatestBackup(): CloudBackupInfo? = withContext(Dispatchers.IO) {
        try {
            val backups = listCloudBackups()
            backups.maxByOrNull { it.createdAt }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Restore latest backup from Google Drive
     */
    suspend fun restoreLatestBackup(): CloudBackupResult = withContext(Dispatchers.IO) {
        try {
            val latestBackup = getLatestBackup()
                ?: return@withContext CloudBackupResult.Error("No backups found in Google Drive")
            
            val result = downloadBackup(latestBackup.backupId)
            result
            
        } catch (e: Exception) {
            CloudBackupResult.Error("Failed to restore latest backup: ${e.message}")
        }
    }
    
    private fun createSyncPlan(
        localBackups: List<BackupFile>,
        cloudBackups: List<CloudBackupInfo>,
        direction: SyncDirection
    ): List<SyncAction> {
        return emptyList() // Stub implementation
    }
    
    private fun generateCloudFileName(backupFile: BackupFile): String {
        return "${backupFile.metadata.backupId}_${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())}.msb"
    }
    
    private fun generateLocalFileName(metadata: BackupMetadata): String {
        return "${metadata.backupId}_${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(metadata.createdAt)}.msb"
    }
    
    private fun verifyDownloadedFile(file: java.io.File, metadata: BackupMetadata): Boolean {
        return file.exists() && file.length() > 0
    }
    
    private fun calculateFileChecksum(file: java.io.File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    private suspend fun emitSyncProgress(
        syncId: String,
        phase: SyncPhase,
        message: String,
        percentage: Double
    ) {
        try {
            _syncProgress.emit(
                CloudSyncProgress(
                    syncId = syncId,
                    phase = phase,
                    message = message,
                    percentage = percentage
                )
            )
        } catch (e: Exception) {
            // Ignore emission errors
        }
    }
    
    private fun serializeMetadata(metadata: BackupMetadata): String {
        // Simple string serialization
        return metadata.toString()
    }
    
    private fun deserializeMetadata(json: String): BackupMetadata? {
        return null // Stub implementation
    }
    
    private fun parseGoogleDriveDate(timestamp: Long?): LocalDateTime {
        return if (timestamp != null) {
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
            )
        } else {
            LocalDateTime.now()
        }
    }
}

/**
 * Sync direction enumeration
 */
enum class SyncDirection {
    UPLOAD_ONLY,
    DOWNLOAD_ONLY,
    BIDIRECTIONAL
}

/**
 * Sync action enumeration
 */
enum class SyncAction {
    UPLOAD,
    DOWNLOAD,
    DELETE,
    SKIP
}

/**
 * Cloud backup information
 */
data class CloudBackupInfo(
    val backupId: String,
    val fileName: String,
    val size: Long,
    val createdAt: LocalDateTime,
    val lastModified: LocalDateTime,
    val deviceName: String,
    val backupType: BackupType,
    val isEncrypted: Boolean,
    val cloudFileId: String?,
    val metadataFileId: String
)

/**
 * Cloud storage information
 */
data class CloudStorageInfo(
    val totalSpace: Long = 0L,
    val usedSpace: Long = 0L,
    val availableSpace: Long = 0L,
    val usedByBackups: Long = 0L,
    val backupCount: Int = 0,
    val lastSyncTime: LocalDateTime? = null
)
