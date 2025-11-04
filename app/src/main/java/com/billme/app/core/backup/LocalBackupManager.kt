package com.billme.app.core.backup

import android.content.Context
import android.database.Cursor
import androidx.core.content.edit
import androidx.room.Database
import com.billme.app.core.database.AppDatabase
import com.billme.app.core.security.EncryptionService
import com.billme.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local backup manager for creating and managing encrypted database backups
 */
@Singleton
class LocalBackupManager @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val encryptionService: EncryptionService
) {
    
    companion object {
        private const val BACKUP_DIRECTORY = "backups"
        private const val BACKUP_FILE_EXTENSION = ".msb"
        private const val BACKUP_METADATA_EXTENSION = ".json"
        private const val CHUNK_SIZE = 8192
        private const val SQL_SEPARATOR = ";\n"
        private const val MAX_BACKUP_RETRIES = 3
    }
    
    private val backupDirectory: File by lazy {
        File(context.filesDir, BACKUP_DIRECTORY).also { it.mkdirs() }
    }
    
    private val _backupProgress = MutableSharedFlow<BackupProgress>()
    val backupProgress: SharedFlow<BackupProgress> = _backupProgress.asSharedFlow()
    
    /**
     * Create a new backup with specified configuration
     */
    suspend fun createBackup(
        backupType: BackupType = BackupType.FULL,
        encryptionMethod: EncryptionMethod = EncryptionMethod.AES_256_GCM,
        compressionMethod: CompressionMethod = CompressionMethod.GZIP,
        description: String? = null,
        password: String? = null
    ): LocalBackupResult = withContext(Dispatchers.IO) {
        val backupId = UUID.randomUUID().toString()
        var encryptionKeyId: String? = null
        
        try {
            // Initialize progress tracking
            emitProgress(backupId, BackupPhase.INITIALIZING, "Starting backup process", 0.0)
            
            // Generate encryption key if needed
            if (encryptionMethod != EncryptionMethod.NONE) {
                encryptionKeyId = encryptionService.generateEncryptionKey(
                    KeyPurpose.BACKUP_ENCRYPTION,
                    password
                ).keyId
            }
            
            // Validate database state
            emitProgress(backupId, BackupPhase.VALIDATING, "Validating database", 5.0)
            validateDatabaseState()
            
            // Create backup metadata
            val deviceId = getDeviceId()
            val deviceName = getDeviceName()
            val appVersion = getAppVersion()
            val databaseVersion = database.openHelper.readableDatabase.version
            
            // Get table information
            emitProgress(backupId, BackupPhase.READING_SCHEMA, "Reading database schema", 10.0)
            val tableInfo = getDatabaseTableInfo()
            
            // Create backup file
            val backupFileName = generateBackupFileName(backupId, backupType)
            val backupFile = File(backupDirectory, backupFileName)
            val tempFile = File(backupDirectory, "${backupFileName}.tmp")
            
            // Perform backup
            val totalRecords = tableInfo.sumOf { it.recordCount }
            val backupSize = performBackup(
                backupId = backupId,
                backupType = backupType,
                compressionMethod = compressionMethod,
                encryptionMethod = encryptionMethod,
                encryptionKeyId = encryptionKeyId,
                tempFile = tempFile,
                tableInfo = tableInfo,
                totalRecords = totalRecords
            )
            
            // Calculate checksum
            emitProgress(backupId, BackupPhase.FINALIZING, "Calculating checksum", 95.0)
            val checksum = calculateFileChecksum(tempFile)
            
            // Create metadata
            val metadata = BackupMetadata(
                backupId = backupId,
                createdAt = LocalDateTime.now(),
                deviceId = deviceId,
                deviceName = deviceName,
                appVersion = appVersion,
                databaseVersion = databaseVersion,
                backupType = backupType,
                encryptionMethod = encryptionMethod,
                compressionMethod = compressionMethod,
                size = backupSize,
                checksumSHA256 = checksum,
                tables = tableInfo,
                description = description
            )
            
            // Move temp file to final location
            Files.move(tempFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            
            // Save metadata
            saveBackupMetadata(metadata)
            
            emitProgress(backupId, BackupPhase.COMPLETED, "Backup completed successfully", 100.0)
            
            LocalBackupResult.Success(
                backupFile = BackupFile(
                    metadata = metadata,
                    filePath = backupFile.absolutePath,
                    fileName = backupFileName,
                    isEncrypted = encryptionMethod != EncryptionMethod.NONE,
                    encryptionKey = encryptionKeyId
                )
            )
            
        } catch (e: Exception) {
            emitProgress(backupId, BackupPhase.FAILED, "Backup failed: ${e.message}", 0.0)
            
            // Cleanup on failure
            encryptionKeyId?.let { cleanupEncryptionKey(it) }
            
            LocalBackupResult.Error(
                message = e.message ?: "Backup creation failed",
                backupId = backupId
            )
        }
    }
    
    /**
     * Get list of all local backups
     */
    suspend fun getLocalBackups(): List<BackupFile> = withContext(Dispatchers.IO) {
        val backupFiles = mutableListOf<BackupFile>()
        
        backupDirectory.listFiles { _, name -> 
            name.endsWith(BACKUP_FILE_EXTENSION) 
        }?.forEach { file ->
            try {
                val metadata = loadBackupMetadata(file.nameWithoutExtension)
                if (metadata != null) {
                    backupFiles.add(
                        BackupFile(
                            metadata = metadata,
                            filePath = file.absolutePath,
                            fileName = file.name,
                            isEncrypted = metadata.encryptionMethod != EncryptionMethod.NONE
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip corrupted backup files
            }
        }
        
        backupFiles.sortedByDescending { it.metadata.createdAt }
    }
    
    /**
     * Delete a backup file and its metadata
     */
    suspend fun deleteBackup(backupId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backups = getLocalBackups()
            val backup = backups.find { it.metadata.backupId == backupId }
                ?: return@withContext false
            
            // Delete backup file
            val backupFile = File(backup.filePath)
            val deleted = backupFile.delete()
            
            // Delete metadata file
            val metadataFile = File(backupDirectory, "${backup.fileName.removeSuffix(BACKUP_FILE_EXTENSION)}$BACKUP_METADATA_EXTENSION")
            metadataFile.delete()
            
            // Cleanup encryption key if it exists
            backup.encryptionKey?.let { cleanupEncryptionKey(it) }
            
            deleted
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verify backup integrity
     */
    suspend fun verifyBackupIntegrity(backupId: String): BackupVerificationResult = withContext(Dispatchers.IO) {
        try {
            val backups = getLocalBackups()
            val backup = backups.find { it.metadata.backupId == backupId }
                ?: return@withContext BackupVerificationResult.NotFound
            
            val backupFile = File(backup.filePath)
            if (!backupFile.exists()) {
                return@withContext BackupVerificationResult.FileNotFound
            }
            
            // Verify file size
            if (backupFile.length() != backup.metadata.size) {
                return@withContext BackupVerificationResult.SizeValidationFailed(
                    expected = backup.metadata.size,
                    actual = backupFile.length()
                )
            }
            
            // Verify checksum
            val actualChecksum = calculateFileChecksum(backupFile)
            if (actualChecksum != backup.metadata.checksumSHA256) {
                return@withContext BackupVerificationResult.ChecksumValidationFailed(
                    expected = backup.metadata.checksumSHA256,
                    actual = actualChecksum
                )
            }
            
            // Try to read backup structure
            val canRead = try {
                testBackupReadability(backupFile, backup.metadata)
                true
            } catch (e: Exception) {
                false
            }
            
            if (!canRead) {
                return@withContext BackupVerificationResult.CorruptedData
            }
            
            BackupVerificationResult.Valid
            
        } catch (e: Exception) {
            BackupVerificationResult.VerificationError(e.message ?: "Verification failed")
        }
    }
    
    /**
     * Get backup statistics
     */
    suspend fun getBackupStatistics(): BackupStatistics = withContext(Dispatchers.IO) {
        val backups = getLocalBackups()
        val successfulBackups = backups.size
        val totalSize = backups.sumOf { it.metadata.size }
        val averageSize = if (backups.isNotEmpty()) totalSize / backups.size else 0L
        
        BackupStatistics(
            totalBackups = successfulBackups,
            successfulBackups = successfulBackups,
            failedBackups = 0, // Would be tracked separately
            totalSize = totalSize,
            averageSize = averageSize,
            averageDuration = 0L, // Would be tracked during backup
            oldestBackup = backups.minByOrNull { it.metadata.createdAt }?.metadata?.createdAt,
            newestBackup = backups.maxByOrNull { it.metadata.createdAt }?.metadata?.createdAt,
            lastSuccessfulBackup = backups.firstOrNull()?.metadata?.createdAt,
            cloudBackupCount = 0, // Would be provided by cloud sync service
            localBackupCount = successfulBackups,
            successRate = 1.0,
            trendsData = emptyList()
        )
    }
    
    /**
     * Clean up old backups based on retention policy
     */
    suspend fun cleanupOldBackups(maxBackups: Int): Int = withContext(Dispatchers.IO) {
        val backups = getLocalBackups()
        val backupsToDelete = backups.drop(maxBackups)
        
        var deletedCount = 0
        backupsToDelete.forEach { backup ->
            if (deleteBackup(backup.metadata.backupId)) {
                deletedCount++
            }
        }
        
        deletedCount
    }
    
    // Private helper methods
    
    private suspend fun performBackup(
        backupId: String,
        backupType: BackupType,
        compressionMethod: CompressionMethod,
        encryptionMethod: EncryptionMethod,
        encryptionKeyId: String?,
        tempFile: File,
        tableInfo: List<TableBackupInfo>,
        totalRecords: Long
    ): Long {
        var processedRecords = 0L
        
        tempFile.outputStream().use { fileOut ->
            if (encryptionMethod != EncryptionMethod.NONE && encryptionKeyId != null) {
                // Encryption will be applied by EncryptionService
                val encrypted = ByteArrayOutputStream()
                val stream = when (compressionMethod) {
                    CompressionMethod.GZIP -> GZIPOutputStream(encrypted)
                    else -> encrypted
                }
                
                // Write data to compressed/raw stream
                writeBackupData(backupId, backupType, tableInfo, stream) { processed ->
                    processedRecords = processed
                    val percentage = if (totalRecords > 0) (processed.toDouble() / totalRecords) * 90.0 else 0.0
                    // Note: Progress callback is non-suspend, so emit separately
                }
                
                stream.close()
                
                // Emit progress after write completes
                emitProgress(backupId, BackupPhase.READING_DATA, 
                    "Backing up data: $processedRecords/$totalRecords records", 90.0)
                
                // Encrypt and write to file
                emitProgress(backupId, BackupPhase.ENCRYPTING, "Encrypting backup", 90.0)
                val encryptionResult = encryptionService.encryptFile(
                    ByteArrayInputStream(encrypted.toByteArray()),
                    fileOut,
                    encryptionKeyId,
                    encryptionMethod
                )
                
                if (!encryptionResult.success) {
                    throw RuntimeException("Encryption failed: ${encryptionResult.error}")
                }
                
                return encrypted.size().toLong()
            } else {
                val stream = when (compressionMethod) {
                    CompressionMethod.GZIP -> GZIPOutputStream(fileOut)
                    else -> fileOut
                }
                
                writeBackupData(backupId, backupType, tableInfo, stream) { processed ->
                    processedRecords = processed
                    val percentage = if (totalRecords > 0) (processed.toDouble() / totalRecords) * 90.0 else 0.0
                    // Note: Progress callback is non-suspend, so emit separately
                }
                
                stream.close()
                
                // Emit progress after write completes
                emitProgress(backupId, BackupPhase.WRITING_FILE, 
                    "Writing backup: $processedRecords/$totalRecords records", 90.0)
                
                return tempFile.length()
            }
        }
    }
    
    private fun writeBackupData(
        backupId: String,
        backupType: BackupType,
        tableInfo: List<TableBackupInfo>,
        outputStream: OutputStream,
        onProgress: (Long) -> Unit
    ) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
        var processedRecords = 0L
        
        writer.use { w ->
            // Write backup header
            w.write("-- Mobile Shop Backup\n")
            w.write("-- Backup ID: $backupId\n")
            w.write("-- Backup Type: ${backupType.name}\n")
            w.write("-- Created: ${LocalDateTime.now()}\n")
            w.write("\n")
            
            // Export schema if needed
            if (backupType == BackupType.FULL || backupType == BackupType.SCHEMA_ONLY) {
                writeSchemaData(w, tableInfo)
            }
            
            // Export data if needed
            if (backupType == BackupType.FULL || backupType == BackupType.DATA_ONLY || backupType == BackupType.INCREMENTAL) {
                tableInfo.forEach { table ->
                    writeTableData(w, table, backupType) { records ->
                        processedRecords += records
                        onProgress(processedRecords)
                    }
                }
            }
        }
    }
    
    private fun writeSchemaData(writer: BufferedWriter, tableInfo: List<TableBackupInfo>) {
        writer.write("-- Database Schema\n")
        tableInfo.forEach { table ->
            table.schema?.let { schema ->
                writer.write("-- Table: ${schema.tableName}\n")
                writer.write("CREATE TABLE IF NOT EXISTS ${schema.tableName} (\n")
                
                schema.columns.forEachIndexed { index, column ->
                    val nullable = if (column.isNullable) "" else " NOT NULL"
                    val default = column.defaultValue?.let { " DEFAULT $it" } ?: ""
                    val primary = if (column.isPrimaryKey) " PRIMARY KEY" else ""
                    
                    writer.write("  ${column.name} ${column.type}$nullable$default$primary")
                    if (index < schema.columns.size - 1) writer.write(",")
                    writer.write("\n")
                }
                
                writer.write(")$SQL_SEPARATOR")
                
                // Write indexes
                schema.indexes.forEach { index ->
                    val unique = if (index.isUnique) "UNIQUE " else ""
                    writer.write("CREATE ${unique}INDEX IF NOT EXISTS ${index.name} ON ${schema.tableName} (${index.columns.joinToString()})$SQL_SEPARATOR")
                }
                
                writer.write("\n")
            }
        }
    }
    
    private fun writeTableData(
        writer: BufferedWriter,
        table: TableBackupInfo,
        backupType: BackupType,
        onProgress: (Long) -> Unit
    ) {
        writer.write("-- Data for table: ${table.tableName}\n")
        
        // This is a simplified implementation - in reality, you would query the actual tables
        // For demonstration, we'll write placeholder data
        val batchSize = 1000L
        var processedRecords = 0L
        
        for (i in 0L until table.recordCount step batchSize) {
            val currentBatch = minOf(batchSize, table.recordCount - i)
            
            // Write batch of INSERT statements
            writer.write("INSERT INTO ${table.tableName} VALUES ")
            for (j in 0L until currentBatch) {
                writer.write("(/* record data would go here */)")
                if (j < currentBatch - 1) writer.write(", ")
            }
            writer.write("$SQL_SEPARATOR")
            
            processedRecords += currentBatch
            onProgress(currentBatch)
        }
        
        writer.write("\n")
    }
    
    private suspend fun getDatabaseTableInfo(): List<TableBackupInfo> {
        return withContext(Dispatchers.IO) {
            // Get all table names - using a simplified approach since Room doesn't expose rawQuery directly
            val tables = mutableListOf<TableBackupInfo>()
            
            // In a real implementation, you would use database.query() or implement a DAO
            // For now, return empty list as placeholder
            tables
        }
    }
    
    private fun getTableSchema(tableName: String): TableSchema {
        // Simplified schema retrieval - in reality, would need to access database schema
        val columns = mutableListOf<ColumnInfo>()
        val indexes = mutableListOf<IndexInfo>()
        
        return TableSchema(
            tableName = tableName,
            columns = columns,
            indexes = indexes,
            constraints = emptyList()
        )
    }
    
    private fun calculateTableChecksum(tableName: String): String {
        // Simplified checksum calculation
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(tableName.toByteArray())
        return Base64.getEncoder().encodeToString(digest.digest())
    }
    
    private suspend fun validateDatabaseState() {
        // Simplified validation - just check if database is accessible
        // In reality, would verify database integrity
    }
    
    private fun generateBackupFileName(backupId: String, backupType: BackupType): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "backup_${backupType.name.lowercase()}_${timestamp}_${backupId.take(8)}$BACKUP_FILE_EXTENSION"
    }
    
    private fun calculateFileChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(CHUNK_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return Base64.getEncoder().encodeToString(digest.digest())
    }
    
    private fun saveBackupMetadata(metadata: BackupMetadata) {
        val metadataFile = File(backupDirectory, "${metadata.backupId}$BACKUP_METADATA_EXTENSION")
        metadataFile.writeText(
            // In a real implementation, you would use a JSON serialization library
            "{\n" +
            "  \"backupId\": \"${metadata.backupId}\",\n" +
            "  \"createdAt\": \"${metadata.createdAt}\",\n" +
            "  \"backupType\": \"${metadata.backupType}\",\n" +
            "  \"size\": ${metadata.size},\n" +
            "  \"checksum\": \"${metadata.checksumSHA256}\"\n" +
            "}"
        )
    }
    
    private fun loadBackupMetadata(backupId: String): BackupMetadata? {
        val metadataFile = File(backupDirectory, "${backupId}$BACKUP_METADATA_EXTENSION")
        if (!metadataFile.exists()) return null
        
        return try {
            // Simplified JSON parsing - would use proper JSON library
            val content = metadataFile.readText()
            // Parse and return metadata
            // This is a placeholder - real implementation would use Gson/Jackson
            null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun testBackupReadability(backupFile: File, metadata: BackupMetadata): Boolean {
        return try {
            backupFile.inputStream().use { input ->
                if (metadata.encryptionMethod != EncryptionMethod.NONE) {
                    // Test decryption
                    val tempOut = ByteArrayOutputStream()
                    val decryptResult = runBlocking {
                        encryptionService.decryptFile(input, tempOut)
                    }
                    decryptResult.success
                } else {
                    // Test compression/raw read
                    val testStream = if (metadata.compressionMethod == CompressionMethod.GZIP) {
                        GZIPInputStream(input)
                    } else {
                        input
                    }
                    
                    testStream.use {
                        val buffer = ByteArray(1024)
                        it.read(buffer) > 0
                    }
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun emitProgress(
        backupId: String,
        phase: BackupPhase,
        operation: String,
        percentage: Double,
        currentTable: String? = null
    ) {
        val progress = BackupProgress(
            backupId = backupId,
            phase = phase,
            currentTable = currentTable,
            totalTables = 0, // Would be set appropriately
            completedTables = 0,
            totalRecords = 0L,
            processedRecords = 0L,
            currentOperation = operation,
            percentage = percentage,
            estimatedTimeRemaining = 0L, // Would be calculated based on current progress
            speed = 0.0
        )
        
        _backupProgress.emit(progress)
    }
    
    private fun cleanupEncryptionKey(keyId: String) {
        // Remove encryption key if it was created for this backup
        try {
            val sharedPrefs = context.getSharedPreferences("encrypted_keys", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove(keyId).apply()
        } catch (e: Exception) {
            // Key cleanup failed, but backup operation can continue
        }
    }
    
    private fun getDeviceId(): String {
        val sharedPrefs = context.getSharedPreferences("device_info", Context.MODE_PRIVATE)
        return sharedPrefs.getString("device_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            sharedPrefs.edit {
                putString("device_id", newId)
            }
            newId
        }
    }
    
    private fun getDeviceName(): String {
        return android.os.Build.MODEL
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}

/**
 * Backup operation result
 */
sealed class LocalBackupResult {
    data class Success(val backupFile: BackupFile) : LocalBackupResult()
    data class Error(val message: String, val backupId: String? = null) : LocalBackupResult()
}

/**
 * Backup verification result
 */
sealed class BackupVerificationResult {
    object Valid : BackupVerificationResult()
    object NotFound : BackupVerificationResult()
    object FileNotFound : BackupVerificationResult()
    data class SizeValidationFailed(val expected: Long, val actual: Long) : BackupVerificationResult()
    data class ChecksumValidationFailed(val expected: String, val actual: String) : BackupVerificationResult()
    object CorruptedData : BackupVerificationResult()
    data class VerificationError(val message: String) : BackupVerificationResult()
}