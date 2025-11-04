package com.billme.app.core.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.billme.app.core.util.ErrorMessageHandler
import com.billme.app.data.local.BillMeDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive Backup and Restore Service
 * 
 * Features:
 * 1. Complete database backup (all tables including settings, products, transactions, invoices)
 * 2. Backup validation and integrity checks
 * 3. Automatic backup with scheduling
 * 4. Restore with verification
 * 5. Export/import for cloud storage (Google Drive, etc.)
 * 6. First-time setup integration
 */
@Singleton
class EnhancedBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BillMeDatabase,
    private val databaseManager: com.billme.app.data.local.DatabaseManager,
    private val errorMessageHandler: ErrorMessageHandler
) {
    
    companion object {
        private const val TAG = "BackupService"
        private const val BACKUP_DIR = "Backups"
        private const val AUTO_BACKUP_DIR = "AutoBackups"
        private const val MAX_AUTO_BACKUPS = 7
        // Database file names - must match DatabaseManager.DATABASE_NAME
        private const val DB_FILE_NAME = "billme_database.db"
        private const val DB_WAL_FILE = "billme_database.db-wal"
        private const val DB_SHM_FILE = "billme_database.db-shm"
        private const val BACKUP_VERSION = "2.0"
    }
    
    private val backupDir: File by lazy {
        File(context.filesDir, BACKUP_DIR).also { it.mkdirs() }
    }
    
    private val autoBackupDir: File by lazy {
        File(context.filesDir, AUTO_BACKUP_DIR).also { it.mkdirs() }
    }
    
    private val _backupProgress = MutableStateFlow<BackupProgress>(BackupProgress.Idle)
    val backupProgress: StateFlow<BackupProgress> = _backupProgress.asStateFlow()
    
    @Serializable
    data class BackupMetadata(
        val version: String,
        val timestamp: Long,
        val appVersion: String = "1.0",
        val deviceName: String,
        val deviceModel: String,
        val androidVersion: String,
        
        // Data counts for validation
        val productCount: Int,
        val productIMEICount: Int,
        val transactionCount: Int,
        val customerCount: Int,
        val supplierCount: Int,
        val settingCount: Int,
        val invoiceCount: Int,
        val signatureCount: Int,
        
        // Database info
        val databaseSize: Long,
        val checksum: String? = null
    )
    
    sealed class BackupProgress {
        object Idle : BackupProgress()
        data class InProgress(val progress: Int, val message: String) : BackupProgress()
        data class Success(val filePath: String) : BackupProgress()
        data class Error(val message: String) : BackupProgress()
    }
    
    /**
     * Create comprehensive backup with all data and validation
     */
    suspend fun createBackup(isAutoBackup: Boolean = false): BackupResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting ${if (isAutoBackup) "auto" else "manual"} backup")
            _backupProgress.value = BackupProgress.InProgress(5, "Preparing backup...")
            
            // Checkpoint WAL to ensure all data is in main database file
            try {
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                    cursor.moveToFirst()
                    Log.d(TAG, "WAL checkpoint completed")
                }
            } catch (e: Exception) {
                Log.w(TAG, "WAL checkpoint failed (non-critical): ${e.message}")
            }
            
            _backupProgress.value = BackupProgress.InProgress(10, "Collecting statistics...")
            
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateStr = dateFormat.format(Date(timestamp))
            
            val backupFileName = if (isAutoBackup) {
                "auto_backup_$dateStr.zip"
            } else {
                "backup_$dateStr.zip"
            }
            
            val targetDir = if (isAutoBackup) autoBackupDir else backupDir
            val backupFile = File(targetDir, backupFileName)
            
            // Collect all data counts
            val productCount = database.productDao().getTotalProductCount()
            val productIMEICount = database.productIMEIDao().getTotalIMEICount()
            val transactionCount = database.transactionDao().getTransactionCountForPeriod(
                Instant.fromEpochMilliseconds(0),
                Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )
            val customerCount = database.customerDao().getCustomerCount()
            val supplierCount = database.supplierDao().getTotalSupplierCount()
            val settingCount = database.appSettingDao().getAllSettingsCount()
            val invoiceCount = database.invoiceDao().getTotalInvoiceCount()
            val signatureCount = database.signatureDao().getSignatureCount()
            
            Log.d(TAG, "Backup stats - Products: $productCount, IMEIs: $productIMEICount, " +
                    "Transactions: $transactionCount, Customers: $customerCount, " +
                    "Invoices: $invoiceCount")
            
            _backupProgress.value = BackupProgress.InProgress(25, "Backing up database...")
            
            val dbFile = context.getDatabasePath(DB_FILE_NAME)
            Log.d(TAG, "Database path: ${dbFile.absolutePath}")
            Log.d(TAG, "Database exists: ${dbFile.exists()}")
            Log.d(TAG, "Database size: ${dbFile.length()} bytes")
            Log.d(TAG, "Database readable: ${dbFile.canRead()}")
            
            val databaseSize = dbFile.length()
            
            // Create metadata
            val metadata = BackupMetadata(
                version = BACKUP_VERSION,
                timestamp = timestamp,
                deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL,
                deviceModel = android.os.Build.MODEL,
                androidVersion = android.os.Build.VERSION.RELEASE,
                productCount = productCount,
                productIMEICount = productIMEICount,
                transactionCount = transactionCount,
                customerCount = customerCount,
                supplierCount = supplierCount,
                settingCount = settingCount,
                invoiceCount = invoiceCount,
                signatureCount = signatureCount,
                databaseSize = databaseSize,
                checksum = calculateChecksum(dbFile)
            )
            
            _backupProgress.value = BackupProgress.InProgress(50, "Creating backup archive...")
            
            // Create ZIP backup with all files
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                // Add main database file
                Log.d(TAG, "=== Adding database to ZIP ===")
                Log.d(TAG, "Database file exists: ${dbFile.exists()}")
                Log.d(TAG, "Database file size: ${dbFile.length()}")
                
                if (dbFile.exists()) {
                    Log.d(TAG, "Adding database.db to backup...")
                    addFileToZip(zipOut, dbFile, "database.db")
                    Log.d(TAG, "✓ Database added successfully")
                    _backupProgress.value = BackupProgress.InProgress(70, "Packing data...")
                } else {
                    Log.e(TAG, "ERROR: Database file does NOT exist at ${dbFile.absolutePath}")
                }
                
                // Add WAL file if exists (for safety)
                val walFile = File(dbFile.parent, DB_WAL_FILE)
                if (walFile.exists() && walFile.length() > 0) {
                    addFileToZip(zipOut, walFile, "database.db-wal")
                }
                
                // Add SHM file if exists
                val shmFile = File(dbFile.parent, DB_SHM_FILE)
                if (shmFile.exists() && shmFile.length() > 0) {
                    addFileToZip(zipOut, shmFile, "database.db-shm")
                }
                
                _backupProgress.value = BackupProgress.InProgress(70, "Backing up invoices...")
                
                // Add invoice PDFs
                val invoicesDir = File(context.getExternalFilesDir(null), "Invoices")
                if (invoicesDir.exists() && invoicesDir.isDirectory) {
                    val invoiceFiles = invoicesDir.listFiles()?.filter { it.extension == "pdf" } ?: emptyList()
                    Log.d(TAG, "Found ${invoiceFiles.size} invoice PDFs to backup")
                    invoiceFiles.forEachIndexed { index, file ->
                        addFileToZip(zipOut, file, "invoices/${file.name}")
                        if (index % 10 == 0) {
                            val progress = 70 + (index * 10 / invoiceFiles.size.coerceAtLeast(1))
                            _backupProgress.value = BackupProgress.InProgress(progress, "Backing up invoices...")
                        }
                    }
                    Log.d(TAG, "✓ Backed up ${invoiceFiles.size} invoice PDFs")
                }
                
                _backupProgress.value = BackupProgress.InProgress(80, "Backing up signatures...")
                
                // Add signature images
                val signaturesDir = File(context.filesDir, "signatures")
                if (signaturesDir.exists() && signaturesDir.isDirectory) {
                    val signatureFiles = signaturesDir.listFiles()?.filter { 
                        it.extension in listOf("png", "jpg", "jpeg")
                    } ?: emptyList()
                    Log.d(TAG, "Found ${signatureFiles.size} signature images to backup")
                    signatureFiles.forEach { file ->
                        addFileToZip(zipOut, file, "signatures/${file.name}")
                    }
                    Log.d(TAG, "✓ Backed up ${signatureFiles.size} signature images")
                }
                
                // Add metadata as JSON
                val metadataJson = Json { prettyPrint = true }.encodeToString(metadata)
                zipOut.putNextEntry(ZipEntry("metadata.json"))
                zipOut.write(metadataJson.toByteArray())
                zipOut.closeEntry()
                
                _backupProgress.value = BackupProgress.InProgress(90, "Finalizing backup...")
            }
            
            // Verify backup was created successfully
            if (!backupFile.exists() || backupFile.length() == 0L) {
                throw Exception("Backup file creation failed")
            }
            
            Log.d(TAG, "Backup created successfully: ${backupFile.absolutePath} (${backupFile.length() / 1024}KB)")
            
            // Clean old auto-backups if this is auto-backup
            if (isAutoBackup) {
                cleanOldAutoBackups()
            }
            
            _backupProgress.value = BackupProgress.Success(backupFile.absolutePath)
            
            BackupResult.Success(
                filePath = backupFile.absolutePath,
                fileSize = backupFile.length(),
                timestamp = timestamp
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            val errorMsg = errorMessageHandler.getUserFriendlyMessage(e, "create backup")
            _backupProgress.value = BackupProgress.Error(errorMsg)
            BackupResult.Error(errorMsg)
        }
    }
    
    /**
     * Restore backup with comprehensive validation
     */
    suspend fun restoreBackup(backupFile: File): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting restore from: ${backupFile.absolutePath}")
            _backupProgress.value = BackupProgress.InProgress(5, "Validating backup...")
            
            if (!backupFile.exists()) {
                return@withContext RestoreResult.Error("Backup file not found")
            }
            
            if (backupFile.length() == 0L) {
                return@withContext RestoreResult.Error("Backup file is empty")
            }
            
            _backupProgress.value = BackupProgress.InProgress(15, "Extracting backup...")
            
            // Create temp directory
            val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            try {
                // Extract ZIP
                var metadata: BackupMetadata? = null
                var databaseRestored = false
                
                ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    
                    while (entry != null) {
                        val extractedFile = File(tempDir, entry.name)
                        
                        if (!entry.isDirectory) {
                            extractedFile.parentFile?.mkdirs()
                            FileOutputStream(extractedFile).use { output ->
                                zipIn.copyTo(output)
                            }
                            
                            // Parse metadata
                            if (entry.name == "metadata.json") {
                                val json = extractedFile.readText()
                                metadata = Json.decodeFromString<BackupMetadata>(json)
                                Log.d(TAG, "Backup metadata: version=${metadata?.version}, " +
                                        "products=${metadata?.productCount}, " +
                                        "transactions=${metadata?.transactionCount}")
                            }
                            
                            if (entry.name == "database.db") {
                                databaseRestored = true
                            }
                        }
                        
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
                
                if (!databaseRestored) {
                    throw Exception("Backup does not contain database file")
                }
                
                _backupProgress.value = BackupProgress.InProgress(40, "Validating data integrity...")
                
                // Validate restored database
                val restoredDb = File(tempDir, "database.db")
                Log.d(TAG, "=== Validating Restored Database ===")
                Log.d(TAG, "Restored DB exists: ${restoredDb.exists()}")
                Log.d(TAG, "Restored DB size: ${restoredDb.length()} bytes")
                
                if (!restoredDb.exists() || restoredDb.length() == 0L) {
                    throw Exception("Restored database is invalid or empty")
                }
                
                // Test if the database file is valid SQLite format
                try {
                    val header = ByteArray(16)
                    FileInputStream(restoredDb).use { it.read(header) }
                    val sqliteHeader = String(header).substring(0, 15)
                    Log.d(TAG, "Database header: $sqliteHeader")
                    if (!sqliteHeader.startsWith("SQLite format")) {
                        throw Exception("File is not a valid SQLite database")
                    }
                    Log.d(TAG, "✓ Valid SQLite database format confirmed")
                } catch (e: Exception) {
                    Log.e(TAG, "Database validation failed", e)
                    throw Exception("Restored file is not a valid database: ${e.message}")
                }
                
                // Verify checksum if available
                metadata?.checksum?.let { expectedChecksum ->
                    val actualChecksum = calculateChecksum(restoredDb)
                    if (actualChecksum != expectedChecksum) {
                        Log.w(TAG, "Checksum mismatch - backup may be corrupted")
                    }
                }
                
                _backupProgress.value = BackupProgress.InProgress(60, "Closing current database...")
                
                // Close all database connections
                database.close()
                
                // Small delay to ensure connections are closed
                kotlinx.coroutines.delay(500)
                
                _backupProgress.value = BackupProgress.InProgress(75, "Restoring database...")
                
                // Backup current database (in case restore fails)
                val currentDb = context.getDatabasePath(DB_FILE_NAME)
                Log.d(TAG, "=== Restoring Database ===")
                Log.d(TAG, "Target database path: ${currentDb.absolutePath}")
                Log.d(TAG, "Current database exists: ${currentDb.exists()}")
                Log.d(TAG, "Extracted database size: ${restoredDb.length()} bytes")
                
                val backupCurrent = File(context.cacheDir, "current_db_backup.db")
                if (currentDb.exists()) {
                    currentDb.copyTo(backupCurrent, overwrite = true)
                }
                
                try {
                    // Delete old database files
                    Log.d(TAG, "Deleting old database files...")
                    currentDb.delete()
                    File(currentDb.parent, DB_WAL_FILE).delete()
                    File(currentDb.parent, DB_SHM_FILE).delete()
                    
                    // Copy restored database
                    Log.d(TAG, "Copying database.db → ${currentDb.name}")
                    restoredDb.copyTo(currentDb, overwrite = true)
                    Log.d(TAG, "✓ Database file restored: ${currentDb.length()} bytes")
                    
                    // Copy WAL if exists
                    val restoredWal = File(tempDir, "database.db-wal")
                    if (restoredWal.exists()) {
                        Log.d(TAG, "Copying WAL file...")
                        restoredWal.copyTo(File(currentDb.parent, DB_WAL_FILE), overwrite = true)
                        Log.d(TAG, "✓ WAL file restored")
                    }
                    
                    // Copy SHM if exists
                    val restoredShm = File(tempDir, "database.db-shm")
                    if (restoredShm.exists()) {
                        Log.d(TAG, "Copying SHM file...")
                        restoredShm.copyTo(File(currentDb.parent, DB_SHM_FILE), overwrite = true)
                        Log.d(TAG, "✓ SHM file restored")
                    }
                    
                    _backupProgress.value = BackupProgress.InProgress(90, "Verifying restoration...")
                    
                    // Test database by opening it and querying
                    try {
                        Log.d(TAG, "Testing restored database...")
                        
                        // Open database and verify it works (READ-WRITE to check version)
                        val dbPath = currentDb.absolutePath
                        val testDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                            dbPath,
                            null,
                            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                        )
                        
                        // Check database version
                        val versionCursor = testDb.rawQuery("PRAGMA user_version", null)
                        var dbVersion = 0
                        if (versionCursor.moveToFirst()) {
                            dbVersion = versionCursor.getInt(0)
                            versionCursor.close()
                        }
                        Log.d(TAG, "Restored database version: $dbVersion")
                        
                        // Ensure database version is set to current version (11)
                        if (dbVersion != 11) {
                            Log.d(TAG, "Updating database version from $dbVersion to 11")
                            testDb.execSQL("PRAGMA user_version = 11")
                        }
                        
                        // Test basic query
                        val tablesCursor = testDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
                        val tableCount = tablesCursor.count
                        val tables = mutableListOf<String>()
                        while (tablesCursor.moveToNext()) {
                            tables.add(tablesCursor.getString(0))
                        }
                        tablesCursor.close()
                        
                        Log.d(TAG, "Database has $tableCount tables: ${tables.joinToString(", ")}")
                        if (tableCount == 0) {
                            testDb.close()
                            throw Exception("Database has no tables")
                        }
                        
                        // Count rows in key tables to verify data
                        try {
                            val productCursor = testDb.rawQuery("SELECT COUNT(*) FROM Product", null)
                            var productCount = 0
                            if (productCursor.moveToFirst()) {
                                productCount = productCursor.getInt(0)
                            }
                            productCursor.close()
                            
                            val transactionCursor = testDb.rawQuery("SELECT COUNT(*) FROM `Transaction`", null)
                            var transactionCount = 0
                            if (transactionCursor.moveToFirst()) {
                                transactionCount = transactionCursor.getInt(0)
                            }
                            transactionCursor.close()
                            
                            val invoiceCursor = testDb.rawQuery("SELECT COUNT(*) FROM Invoice", null)
                            var invoiceCount = 0
                            if (invoiceCursor.moveToFirst()) {
                                invoiceCount = invoiceCursor.getInt(0)
                            }
                            invoiceCursor.close()
                            
                            Log.d(TAG, "Data counts - Products: $productCount, Transactions: $transactionCount, Invoices: $invoiceCount")
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not count rows: ${e.message}")
                        }
                        
                        // Run SQLite integrity check
                        Log.d(TAG, "Running PRAGMA integrity_check...")
                        val integrityCursor = testDb.rawQuery("PRAGMA integrity_check", null)
                        if (integrityCursor.moveToFirst()) {
                            val result = integrityCursor.getString(0)
                            integrityCursor.close()
                            if (result != "ok") {
                                testDb.close()
                                throw Exception("Database integrity check failed: $result")
                            }
                            Log.d(TAG, "✓ Database integrity check passed")
                        } else {
                            integrityCursor.close()
                            testDb.close()
                            throw Exception("Could not run integrity check")
                        }
                        
                        testDb.close()
                        
                        Log.d(TAG, "✓ Database restored and validated successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Restored database validation failed", e)
                        // Restore original database
                        if (backupCurrent.exists()) {
                            Log.d(TAG, "Rolling back to original database...")
                            currentDb.delete()
                            backupCurrent.copyTo(currentDb, overwrite = true)
                            Log.d(TAG, "Original database restored")
                        }
                        throw Exception("Restored database is corrupted: ${e.message}")
                    }
                    
                    // Clean up backup of current db
                    backupCurrent.delete()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Database restoration failed", e)
                    // Restore original database
                    if (backupCurrent.exists()) {
                        backupCurrent.copyTo(currentDb, overwrite = true)
                        backupCurrent.delete()
                    }
                    throw e
                }
                
                _backupProgress.value = BackupProgress.InProgress(92, "Restoring invoice PDFs...")
                
                // Restore invoice PDFs
                val invoicesSourceDir = File(tempDir, "invoices")
                if (invoicesSourceDir.exists() && invoicesSourceDir.isDirectory) {
                    val invoicesTargetDir = File(context.getExternalFilesDir(null), "Invoices")
                    invoicesTargetDir.mkdirs()
                    
                    val invoiceFiles = invoicesSourceDir.listFiles()?.filter { it.extension == "pdf" } ?: emptyList()
                    Log.d(TAG, "Restoring ${invoiceFiles.size} invoice PDFs...")
                    invoiceFiles.forEach { file ->
                        val targetFile = File(invoicesTargetDir, file.name)
                        file.copyTo(targetFile, overwrite = true)
                    }
                    Log.d(TAG, "✓ Restored ${invoiceFiles.size} invoice PDFs")
                }
                
                _backupProgress.value = BackupProgress.InProgress(95, "Restoring signatures...")
                
                // Restore signature images
                val signaturesSourceDir = File(tempDir, "signatures")
                if (signaturesSourceDir.exists() && signaturesSourceDir.isDirectory) {
                    val signaturesTargetDir = File(context.filesDir, "signatures")
                    signaturesTargetDir.mkdirs()
                    
                    val signatureFiles = signaturesSourceDir.listFiles()?.filter { 
                        it.extension in listOf("png", "jpg", "jpeg")
                    } ?: emptyList()
                    Log.d(TAG, "Restoring ${signatureFiles.size} signature images...")
                    signatureFiles.forEach { file ->
                        val targetFile = File(signaturesTargetDir, file.name)
                        file.copyTo(targetFile, overwrite = true)
                    }
                    Log.d(TAG, "✓ Restored ${signatureFiles.size} signature images")
                }
                
                // CRITICAL: Clear database instance to force reconnection with restored data
                Log.d(TAG, "Clearing database instance to reload restored data...")
                databaseManager.clearInstance()
                
                _backupProgress.value = BackupProgress.Success("Restore completed")
                
                Log.d(TAG, "Restore completed successfully - App restart recommended")
                RestoreResult.Success(
                    message = "Successfully restored:\n" +
                            "• ${metadata?.productCount ?: 0} products\n" +
                            "• ${metadata?.transactionCount ?: 0} transactions\n" +
                            "• ${metadata?.invoiceCount ?: 0} invoices\n" +
                            "• ${metadata?.customerCount ?: 0} customers\n" +
                            "• All invoice PDFs and signatures\n" +
                            "• All settings and data\n\n" +
                            "⚠️ Please restart the app to see all restored data.",
                    needsAppRestart = true
                )
                
            } finally {
                // Clean up temp directory
                tempDir.deleteRecursively()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            val errorMsg = errorMessageHandler.getUserFriendlyMessage(e, "restore backup")
            _backupProgress.value = BackupProgress.Error(errorMsg)
            RestoreResult.Error(errorMsg)
        }
    }
    
    /**
     * Validate backup file without restoring
     */
    suspend fun validateBackup(backupFile: File): BackupValidation = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                return@withContext BackupValidation(false, "File not found")
            }
            
            if (backupFile.length() == 0L) {
                return@withContext BackupValidation(false, "File is empty")
            }
            
            // Try to extract and read metadata
            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                var foundDatabase = false
                var foundMetadata = false
                var metadata: BackupMetadata? = null
                
                while (entry != null) {
                    when (entry.name) {
                        "database.db" -> foundDatabase = true
                        "metadata.json" -> {
                            foundMetadata = true
                            val json = zipIn.readBytes().toString(Charsets.UTF_8)
                            metadata = try {
                                Json.decodeFromString<BackupMetadata>(json)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                
                if (!foundDatabase) {
                    return@withContext BackupValidation(false, "Missing database file")
                }
                
                val details = metadata?.let {
                    """
                    Version: ${it.version}
                    Created: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it.timestamp))}
                    Device: ${it.deviceName}
                    Products: ${it.productCount}
                    Transactions: ${it.transactionCount}
                    Invoices: ${it.invoiceCount}
                    Size: ${backupFile.length() / 1024}KB
                    """.trimIndent()
                } ?: "Valid backup (legacy format)"
                
                BackupValidation(true, details, metadata)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Backup validation failed", e)
            BackupValidation(false, "Invalid or corrupted backup: ${e.message}")
        }
    }
    
    /**
     * Calculate file checksum for integrity verification
     */
    private fun calculateChecksum(file: File): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Checksum calculation failed", e)
            ""
        }
    }
    
    /**
     * Get all available backups
     */
    suspend fun getAvailableBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val backups = mutableListOf<BackupInfo>()
        
        // Manual backups
        backupDir.listFiles()?.forEach { file ->
            if (file.extension == "zip") {
                backups.add(BackupInfo(
                    file = file,
                    timestamp = file.lastModified(),
                    size = file.length(),
                    isAutoBackup = false
                ))
            }
        }
        
        // Auto backups
        autoBackupDir.listFiles()?.forEach { file ->
            if (file.extension == "zip") {
                backups.add(BackupInfo(
                    file = file,
                    timestamp = file.lastModified(),
                    size = file.length(),
                    isAutoBackup = true
                ))
            }
        }
        
        backups.sortedByDescending { it.timestamp }
    }
    
    /**
     * Export backup to user-chosen location (for manual cloud upload)
     */
    fun createShareIntent(backupFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "BillMe Backup - ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}")
            putExtra(Intent.EXTRA_TEXT, "Backup file from BillMe app. You can upload this to Google Drive or any cloud storage.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { Intent.createChooser(it, "Share Backup (Upload to Drive)") }
    }
    
    /**
     * Delete old auto-backups, keeping only recent ones
     */
    private suspend fun cleanOldAutoBackups() = withContext(Dispatchers.IO) {
        val backups = autoBackupDir.listFiles()
            ?.filter { it.extension == "zip" }
            ?.sortedByDescending { it.lastModified() }
            ?: return@withContext
        
        if (backups.size > MAX_AUTO_BACKUPS) {
            backups.drop(MAX_AUTO_BACKUPS).forEach { it.delete() }
        }
    }
    
    /**
     * Delete a backup file
     */
    suspend fun deleteBackup(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        backupFile.delete()
    }
    
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { input ->
            zipOut.putNextEntry(ZipEntry(entryName))
            input.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
    /**
     * Restart the app to refresh all data after restore
     */
    fun restartApp() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(this)
            }
            // Force exit
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart app", e)
        }
    }
}

data class BackupInfo(
    val file: File,
    val timestamp: Long,
    val size: Long,
    val isAutoBackup: Boolean
) {
    val formattedDate: String
        get() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date(timestamp))
    
    val formattedSize: String
        get() = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
}

sealed class BackupResult {
    data class Success(val filePath: String, val fileSize: Long, val timestamp: Long) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val message: String, val needsAppRestart: Boolean = false) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

data class BackupValidation(
    val isValid: Boolean,
    val message: String,
    val metadata: EnhancedBackupService.BackupMetadata? = null
)
