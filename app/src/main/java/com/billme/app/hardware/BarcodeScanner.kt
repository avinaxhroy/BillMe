package com.billme.app.hardware

import kotlinx.coroutines.flow.StateFlow

/**
 * Barcode scanning result
 */
data class ScanResult(
    val content: String,
    val format: BarcodeFormat,
    val timestamp: Long = System.currentTimeMillis(),
    val parsedIMEIs: IMEIScanResult? = null
)

/**
 * IMEI scanning result with auto-detection
 */
data class IMEIScanResult(
    val imei1: String?,
    val imei2: String?,
    val rawText: String,
    val detectionMethod: IMEIDetectionMethod,
    val isValid: Boolean
)

/**
 * IMEI detection methods
 */
enum class IMEIDetectionMethod {
    SINGLE_IMEI,           // Found single IMEI
    DUAL_IMEI_SEPARATED,   // Found two IMEIs separated by delimiter
    DUAL_IMEI_SEQUENTIAL,  // Found two IMEIs in sequence
    TEXT_EXTRACTION,       // Extracted from mixed text
    MANUAL_PARSE          // Manual parsing required
}

/**
 * Supported barcode formats
 */
enum class BarcodeFormat {
    CODE_128,
    CODE_39,
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    QR_CODE,
    DATA_MATRIX,
    UNKNOWN
}

/**
 * Scanner states
 */
enum class ScannerState {
    IDLE,
    SCANNING,
    ERROR,
    PERMISSION_REQUIRED
}

/**
 * Barcode scanner interface for hardware abstraction
 */
interface BarcodeScanner {
    
    /**
     * Current scanner state
     */
    val state: StateFlow<ScannerState>
    
    /**
     * Latest scan result
     */
    val scanResult: StateFlow<ScanResult?>
    
    /**
     * Start scanning
     * @return true if scanning started successfully
     */
    suspend fun startScanning(): Boolean
    
    /**
     * Stop scanning
     */
    suspend fun stopScanning()
    
    /**
     * Check if camera permission is granted
     */
    fun hasPermission(): Boolean
    
    /**
     * Request camera permission
     */
    suspend fun requestPermission(): Boolean
    
    /**
     * Check if scanner is available (camera + hardware)
     */
    fun isAvailable(): Boolean
    
    /**
     * Release scanner resources
     */
    fun release()
}

/**
 * Scanner configuration
 */
data class ScannerConfig(
    val enabledFormats: Set<BarcodeFormat> = setOf(
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
        BarcodeFormat.EAN_13,
        BarcodeFormat.QR_CODE
    ),
    val enableBeep: Boolean = true,
    val enableVibration: Boolean = true,
    val autoFocus: Boolean = true,
    val enableFlash: Boolean = false
)