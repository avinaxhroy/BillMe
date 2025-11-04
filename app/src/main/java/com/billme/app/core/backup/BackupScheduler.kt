package com.billme.app.core.backup

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages scheduling of daily automated backups using WorkManager
 */
@Singleton
class BackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule daily automatic backups
     * Runs once per day at optimal time (when device is idle and charging)
     */
    fun scheduleDailyBackup() {
        // Create constraints for backup work
        // Run only when:
        // - Device is not in use (idle)
        // - Battery is not low
        // - Device is charging (optional, but preferred)
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(false) // Don't wait for idle to be more reliable
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false) // Don't require charging for flexibility
            .build()

        // Create periodic work request
        // Runs every 24 hours with 15-minute flex period
        val backupRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15, // Allow 15-minute flexibility
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("backup")
            .addTag("daily_backup")
            .build()

        // Enqueue the work request
        // REPLACE existing work with same name to avoid duplicates
        workManager.enqueueUniquePeriodicWork(
            DailyBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            backupRequest
        )
    }

    /**
     * Cancel daily automatic backups
     * Called when user disables auto-backup in settings
     */
    fun cancelDailyBackup() {
        workManager.cancelUniqueWork(DailyBackupWorker.WORK_NAME)
    }

    /**
     * Check if daily backup is currently scheduled
     */
    fun isDailyBackupScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(DailyBackupWorker.WORK_NAME).get()
        return workInfos.any { 
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING 
        }
    }

    /**
     * Trigger an immediate one-time backup (outside of schedule)
     */
    fun triggerImmediateBackup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val immediateBackupRequest = OneTimeWorkRequestBuilder<DailyBackupWorker>()
            .setConstraints(constraints)
            .addTag("backup")
            .addTag("immediate_backup")
            .build()

        workManager.enqueue(immediateBackupRequest)
    }
}
