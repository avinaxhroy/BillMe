package com.billme.app.data.model

import java.time.LocalDateTime
import java.util.*

/**
 * Comprehensive data models for Backup & Security system
 */

/**
 * Main backup metadata container
 */
data class BackupMetadata(
    val backupId: String = UUID.randomUUID().toString(),
    val version: String = "1.0",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val deviceId: String,
    val deviceName: String,
    val appVersion: String,
    val databaseVersion: Int,
    val backupType: BackupType,
    val encryptionMethod: EncryptionMethod,
    val compressionMethod: CompressionMethod,
    val size: Long, // Size in bytes
    val checksumSHA256: String,
    val tables: List<TableBackupInfo>,
    val isIncremental: Boolean = false,
    val baseBackupId: String? = null, // For incremental backups
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
)

/**
 * Information about each table in the backup
 */
data class TableBackupInfo(
    val tableName: String,
    val recordCount: Long,
    val size: Long,
    val lastModified: LocalDateTime,
    val checksum: String,
    val schema: TableSchema? = null
)

/**
 * Table schema information
 */
data class TableSchema(
    val tableName: String,
    val columns: List<ColumnInfo>,
    val indexes: List<IndexInfo>,
    val constraints: List<ConstraintInfo>
)

/**
 * Column information
 */
data class ColumnInfo(
    val name: String,
    val type: String,
    val isNullable: Boolean,
    val defaultValue: String? = null,
    val isPrimaryKey: Boolean = false,
    val isForeignKey: Boolean = false,
    val references: String? = null
)

/**
 * Index information
 */
data class IndexInfo(
    val name: String,
    val columns: List<String>,
    val isUnique: Boolean = false,
    val type: String = "BTREE"
)

/**
 * Constraint information
 */
data class ConstraintInfo(
    val name: String,
    val type: ConstraintType,
    val columns: List<String>,
    val referencedTable: String? = null,
    val referencedColumns: List<String> = emptyList()
)

/**
 * Backup file information
 */
data class BackupFile(
    val metadata: BackupMetadata,
    val filePath: String,
    val fileName: String,
    val isEncrypted: Boolean,
    val encryptionKey: String? = null, // Encrypted with master key
    val uploadStatus: UploadStatus = UploadStatus.NOT_UPLOADED,
    val cloudPath: String? = null,
    val lastSyncAt: LocalDateTime? = null
)

/**
 * Restore operation configuration
 */
data class RestoreConfig(
    val backupId: String,
    val restoreType: RestoreType = RestoreType.FULL,
    val selectedTables: List<String> = emptyList(), // Empty means all tables
    val conflictResolution: ConflictResolution = ConflictResolution.ASK_USER,
    val validateData: Boolean = true,
    val createBackupBeforeRestore: Boolean = true,
    val overwriteExisting: Boolean = false,
    val restoreToDate: LocalDateTime? = null // Point-in-time restore
)

/**
 * Restore operation result
 */
data class RestoreResult(
    val success: Boolean,
    val backupId: String,
    val restoredTables: List<String>,
    val failedTables: List<String>,
    val conflicts: List<DataConflict>,
    val totalRecords: Long,
    val restoredRecords: Long,
    val skippedRecords: Long,
    val duration: Long, // milliseconds
    val errors: List<RestoreError>,
    val warnings: List<String>
)

/**
 * Data conflict during restore
 */
data class DataConflict(
    val tableName: String,
    val recordId: String,
    val conflictType: ConflictType,
    val existingData: Map<String, Any?>,
    val incomingData: Map<String, Any?>,
    val resolution: ConflictResolution,
    val resolvedData: Map<String, Any?>? = null
)

/**
 * Restore error information
 */
data class RestoreError(
    val tableName: String,
    val recordId: String? = null,
    val errorType: RestoreErrorType,
    val message: String,
    val recoverable: Boolean = false
)

/**
 * Backup schedule configuration
 */
data class BackupSchedule(
    val id: String = UUID.randomUUID().toString(),
    val isEnabled: Boolean = true,
    val frequency: BackupFrequency = BackupFrequency.DAILY,
    val time: String = "02:00", // HH:mm format
    val backupType: BackupType = BackupType.INCREMENTAL,
    val maxLocalBackups: Int = 7,
    val maxCloudBackups: Int = 30,
    val wifiOnly: Boolean = true,
    val batteryLevel: Int = 20, // Minimum battery level
    val autoUpload: Boolean = true,
    val notifyOnCompletion: Boolean = true,
    val notifyOnFailure: Boolean = true
)

/**
 * Backup operation progress
 */
data class BackupProgress(
    val backupId: String,
    val phase: BackupPhase,
    val currentTable: String? = null,
    val totalTables: Int,
    val completedTables: Int,
    val totalRecords: Long,
    val processedRecords: Long,
    val currentOperation: String,
    val percentage: Double,
    val estimatedTimeRemaining: Long, // seconds
    val speed: Double, // records per second
    val errors: List<String> = emptyList()
)

/**
 * Cloud sync configuration
 */
data class CloudSyncConfig(
    val isEnabled: Boolean = false,
    val provider: CloudProvider = CloudProvider.GOOGLE_DRIVE,
    val accountId: String? = null,
    val accountName: String? = null,
    val folderId: String? = null,
    val folderPath: String = "/MobileShopBackups",
    val encryptionEnabled: Boolean = true,
    val compressionEnabled: Boolean = true,
    val autoSync: Boolean = true,
    val syncOnlyOnWifi: Boolean = true,
    val maxUploadSize: Long = 100 * 1024 * 1024, // 100MB
    val retryCount: Int = 3,
    val skipDuplicates: Boolean = true,
    val lastSyncAt: LocalDateTime? = null
)

/**
 * Cloud backup file information
 */
data class CloudBackupFile(
    val fileId: String,
    val fileName: String,
    val size: Long,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime,
    val checksum: String,
    val metadata: BackupMetadata,
    val downloadUrl: String? = null,
    val isAvailable: Boolean = true
)

/**
 * Security audit log entry
 */
data class SecurityAuditLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val eventType: SecurityEventType,
    val userId: String? = null,
    val deviceId: String,
    val ipAddress: String? = null,
    val action: String,
    val resource: String? = null,
    val success: Boolean,
    val errorMessage: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Encryption key information
 */
data class EncryptionKeyInfo(
    val keyId: String,
    val algorithm: String = "AES",
    val keySize: Int = 256,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime? = null,
    val isActive: Boolean = true,
    val purpose: KeyPurpose,
    val derivationMethod: String = "PBKDF2",
    val salt: String,
    val iterations: Int = 10000
)

/**
 * Data integrity verification result
 */
data class IntegrityCheckResult(
    val tableName: String,
    val totalRecords: Long,
    val validRecords: Long,
    val corruptedRecords: Long,
    val missingRecords: Long,
    val duplicateRecords: Long,
    val integrityScore: Double, // 0.0 to 1.0
    val issues: List<IntegrityIssue>,
    val recommendation: String
)

/**
 * Data integrity issue
 */
data class IntegrityIssue(
    val recordId: String,
    val issueType: IntegrityIssueType,
    val description: String,
    val severity: IssueSeverity,
    val autoFixable: Boolean = false,
    val suggestedFix: String? = null
)

// Enums

/**
 * Types of backup
 */
enum class BackupType {
    FULL,           // Complete database backup
    INCREMENTAL,    // Only changes since last backup
    DIFFERENTIAL,   // Changes since last full backup
    SCHEMA_ONLY,    // Only database structure
    DATA_ONLY       // Only data, no schema
}

/**
 * Encryption methods
 */
enum class EncryptionMethod {
    NONE,
    AES_256_GCM,
    AES_256_CBC,
    CHACHA20_POLY1305
}

/**
 * Compression methods
 */
enum class CompressionMethod {
    NONE,
    GZIP,
    DEFLATE,
    LZ4,
    ZSTD
}

/**
 * Backup sync status
 */
enum class SyncStatus {
    LOCAL_ONLY,
    UPLOADING,
    SYNCED,
    SYNC_FAILED,
    DOWNLOAD_AVAILABLE,
    DOWNLOADING,
    CONFLICT
}

/**
 * Cloud upload status
 */
enum class UploadStatus {
    NOT_UPLOADED,
    UPLOADING,
    UPLOADED,
    UPLOAD_FAILED,
    DOWNLOAD_AVAILABLE
}

/**
 * Types of restore
 */
enum class RestoreType {
    FULL,           // Restore entire backup
    PARTIAL,        // Restore selected tables
    POINT_IN_TIME,  // Restore to specific date/time
    SCHEMA_ONLY,    // Restore only structure
    DATA_ONLY       // Restore only data
}

/**
 * Conflict resolution strategies
 */
enum class ConflictResolution {
    ASK_USER,           // Prompt user for each conflict
    KEEP_EXISTING,      // Keep current data
    OVERWRITE,          // Use backup data
    MERGE,              // Attempt to merge data
    SKIP,               // Skip conflicted records
    CREATE_NEW          // Create new records with different IDs
}

/**
 * Types of data conflicts
 */
enum class ConflictType {
    DUPLICATE_PRIMARY_KEY,
    FOREIGN_KEY_VIOLATION,
    UNIQUE_CONSTRAINT_VIOLATION,
    DATA_TYPE_MISMATCH,
    SCHEMA_MISMATCH,
    TIMESTAMP_CONFLICT,
    VERSION_CONFLICT
}

/**
 * Restore error types
 */
enum class RestoreErrorType {
    SCHEMA_MISMATCH,
    DATA_CORRUPTION,
    CONSTRAINT_VIOLATION,
    PERMISSION_DENIED,
    DISK_SPACE_FULL,
    NETWORK_ERROR,
    ENCRYPTION_ERROR,
    UNKNOWN_ERROR
}

/**
 * Database constraint types
 */
enum class ConstraintType {
    PRIMARY_KEY,
    FOREIGN_KEY,
    UNIQUE,
    CHECK,
    NOT_NULL
}

/**
 * Backup frequency options
 */
enum class BackupFrequency {
    MANUAL,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * Backup operation phases
 */
enum class BackupPhase {
    INITIALIZING,
    VALIDATING,
    READING_SCHEMA,
    READING_DATA,
    COMPRESSING,
    ENCRYPTING,
    WRITING_FILE,
    UPLOADING,
    FINALIZING,
    COMPLETED,
    FAILED
}

/**
 * Cloud storage providers
 */
enum class CloudProvider {
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE,
    AWS_S3,
    CUSTOM
}

/**
 * Security event types
 */
enum class SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    BACKUP_CREATED,
    BACKUP_RESTORED,
    BACKUP_DELETED,
    ENCRYPTION_KEY_CREATED,
    ENCRYPTION_KEY_ROTATED,
    UNAUTHORIZED_ACCESS_ATTEMPT,
    DATA_EXPORT,
    SETTINGS_CHANGED,
    CLOUD_SYNC_ENABLED,
    CLOUD_SYNC_DISABLED
}

/**
 * Encryption key purposes
 */
enum class KeyPurpose {
    DATABASE_ENCRYPTION,
    BACKUP_ENCRYPTION,
    CLOUD_SYNC_ENCRYPTION,
    EXPORT_ENCRYPTION
}

/**
 * Data integrity issue types
 */
enum class IntegrityIssueType {
    MISSING_RECORD,
    CORRUPTED_DATA,
    INVALID_REFERENCE,
    DUPLICATE_ENTRY,
    SCHEMA_VIOLATION,
    CHECKSUM_MISMATCH,
    ORPHANED_RECORD
}

/**
 * Issue severity levels
 */
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Backup statistics for monitoring
 */
data class BackupStatistics(
    val totalBackups: Int,
    val successfulBackups: Int,
    val failedBackups: Int,
    val totalSize: Long,
    val averageSize: Long,
    val averageDuration: Long,
    val oldestBackup: LocalDateTime?,
    val newestBackup: LocalDateTime?,
    val lastSuccessfulBackup: LocalDateTime?,
    val cloudBackupCount: Int,
    val localBackupCount: Int,
    val successRate: Double,
    val trendsData: List<BackupTrend>
)

/**
 * Backup trend data for analytics
 */
data class BackupTrend(
    val date: LocalDateTime,
    val backupCount: Int,
    val totalSize: Long,
    val averageDuration: Long,
    val successRate: Double
)

/**
 * Cloud sync progress tracking
 */
data class CloudSyncProgress(
    val syncId: String,
    val phase: SyncPhase,
    val message: String,
    val percentage: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Cloud backup operation results
 */
sealed class CloudBackupResult {
    data class Success(val message: String) : CloudBackupResult()
    data class Error(val message: String, val exception: Exception? = null) : CloudBackupResult()
    data class Progress(val progress: CloudSyncProgress) : CloudBackupResult()
}

/**
 * Authentication state for cloud services
 */
sealed class AuthenticationState {
    object NotAuthenticated : AuthenticationState()
    object Authenticating : AuthenticationState()
    data class Authenticated(
        val accountEmail: String,
        val accountName: String
    ) : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}

/**
 * Sync phase enumeration
 */
enum class SyncPhase {
    INITIALIZING,
    UPLOADING,
    DOWNLOADING,
    PROCESSING,
    VERIFYING,
    COMPLETED,
    FAILED
}