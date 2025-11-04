package com.billme.app.core.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.billme.app.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * WorkManager worker for daily automated backups
 * Runs once per day when auto-backup is enabled in settings
 */
@HiltWorker
class DailyBackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupService: EnhancedBackupService,
    private val settingsRepository: SettingsRepository,
    private val googleDriveService: GoogleDriveService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "daily_backup_work"
        private const val CHANNEL_ID = "backup_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if auto-backup is enabled in settings
            val isEnabled = settingsRepository.isAutoBackupEnabled()
            
            if (!isEnabled) {
                // Auto-backup is disabled, stop the worker
                return@withContext Result.success()
            }

            // Perform the backup
            when (val result = backupService.createBackup(isAutoBackup = true)) {
                is BackupResult.Success -> {
                    // Check if user is signed in to Google Drive
                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(appContext)
                    if (account != null) {
                        // Initialize Drive service
                        val initialized = googleDriveService.initializeDriveService(account)
                        
                        if (initialized) {
                            try {
                                // Get the backup file
                                val backupFile = java.io.File(result.filePath)
                                
                                android.util.Log.d("DailyBackupWorker", "Auto-backup created: ${backupFile.name}")
                                
                                // Automatically upload to Google Drive with cleanup
                                val uploadResult = googleDriveService.performAutomaticBackup(backupFile)
                                
                                when (uploadResult) {
                                    is DriveUploadResult.Success -> {
                                        // Upload successful with automatic cleanup
                                        android.util.Log.d("DailyBackupWorker", "Auto-backup uploaded with cleanup: ${uploadResult.fileName}")
                                        showBackupAndUploadSuccessNotification(backupFile.name)
                                    }
                                    is DriveUploadResult.Error -> {
                                        // Upload failed, show notification with manual upload option
                                        android.util.Log.e("DailyBackupWorker", "Upload failed: ${uploadResult.message}")
                                        showBackupSuccessWithUploadErrorNotification(result.filePath, uploadResult.message)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("DailyBackupWorker", "Upload error: ${e.message}", e)
                                showBackupSuccessNotification(result.filePath)
                            }
                        } else {
                            // Drive service initialization failed
                            android.util.Log.w("DailyBackupWorker", "Drive service initialization failed")
                            showBackupSuccessNotification(result.filePath)
                        }
                    } else {
                        // Not signed in, show notification with option to upload
                        android.util.Log.d("DailyBackupWorker", "User not signed in to Google Drive")
                        showBackupSuccessNotification(result.filePath)
                    }
                    Result.success()
                }
                is BackupResult.Error -> {
                    // Backup failed, retry
                    showBackupFailureNotification(result.message)
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            // Error occurred, show notification and retry
            showBackupFailureNotification(e.message ?: "Unknown error")
            Result.retry()
        }
    }
    
    private fun showBackupAndUploadSuccessNotification(fileName: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Auto-Backup Completed")
            .setContentText("Backup uploaded to Google Drive")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your data has been backed up locally and uploaded to Google Drive as '$fileName'."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showBackupSuccessWithUploadErrorNotification(backupFilePath: String, errorMessage: String) {
        createNotificationChannel()
        
        val backupFile = File(backupFilePath)
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            backupFile
        )
        
        val uploadIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, backupFile.name)
            putExtra(Intent.EXTRA_SUBJECT, "BillMe Auto-Backup")
            setPackage("com.google.android.apps.docs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val uploadPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent.createChooser(uploadIntent, "Upload to Google Drive"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Auto-Backup Completed")
            .setContentText("Backup created, but upload failed")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your data has been backed up locally, but automatic upload to Google Drive failed: $errorMessage\n\nTap 'Upload to Drive' to try again."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_upload,
                "Upload to Drive",
                uploadPendingIntent
            )
            .build()
        
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showBackupSuccessNotification(backupFilePath: String) {
        createNotificationChannel()
        
        val backupFile = File(backupFilePath)
        
        // Create intent to upload to Google Drive
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            backupFile
        )
        
        val uploadIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, backupFile.name)
            putExtra(Intent.EXTRA_SUBJECT, "BillMe Auto-Backup")
            setPackage("com.google.android.apps.docs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val uploadPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent.createChooser(uploadIntent, "Upload to Google Drive"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Auto-Backup Completed")
            .setContentText("Daily backup created successfully")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your data has been backed up locally. Tap 'Upload to Drive' to save it to Google Drive."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_upload,
                "Upload to Drive",
                uploadPendingIntent
            )
            .build()
        
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showBackupFailureNotification(errorMessage: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Auto-Backup Failed")
            .setContentText("Failed to create backup")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Error: $errorMessage\n\nWe'll try again later."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backup Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for backup operations"
            }
            
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
