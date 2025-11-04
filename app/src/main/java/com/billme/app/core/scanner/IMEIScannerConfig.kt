package com.billme.app.core.scanner

import android.util.Size
import androidx.camera.core.ImageAnalysis

/**
 * Configuration for IMEI Scanner
 * Optimized settings for best performance and accuracy
 */
object IMEIScannerConfig {
    
    /**
     * Image analysis resolution - balance between quality and performance
     * Increased to 1920x1080 for better text recognition at angles
     */
    val TARGET_RESOLUTION = Size(1920, 1080)
    
    /**
     * High resolution for devices with powerful processors
     * Ultra-high for maximum accuracy when tilted
     */
    val HIGH_RESOLUTION = Size(1920, 1080)
    
    /**
     * Lower resolution for older/slower devices
     * Still decent for tilt handling
     */
    val LOW_RESOLUTION = Size(1280, 720)
    
    /**
     * Image output format - YUV_420_888 is optimal for ML Kit
     */
    const val OUTPUT_FORMAT = ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
    
    /**
     * Backpressure strategy - keep only latest frame to avoid lag
     */
    const val BACKPRESSURE_STRATEGY = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    
    /**
     * Text recognition confidence threshold
     * Increased for better accuracy with enhanced validation
     */
    const val TEXT_CONFIDENCE_THRESHOLD = 0.65f
    
    /**
     * Minimum detections before accepting IMEI (confidence filter)
     * Set to 1 for faster detection since we have multi-layer validation
     */
    const val MIN_DETECTION_COUNT = 1
    
    /**
     * Minimum different digits in IMEI (variety check)
     * Filters out patterns like 111111111111111
     */
    const val MIN_DIGIT_VARIETY = 5
    
    /**
     * Maximum digit differences for dual IMEI validation
     * Dual IMEIs should be similar (typically differ in 1-8 digits)
     */
    const val MAX_DUAL_IMEI_DIFFERENCES = 8
    
    /**
     * Minimum time between scans (milliseconds)
     * Prevents rapid duplicate scans while allowing quick re-detection
     */
    const val SCAN_DEBOUNCE_DELAY = 400L
    
    /**
     * Maximum scan attempts before showing error
     * Increased to give more time for successful detection
     */
    const val MAX_SCAN_ATTEMPTS = 1500
    
    /**
     * Timeout for camera initialization (seconds)
     */
    const val CAMERA_INIT_TIMEOUT = 10
    
    /**
     * Enable/disable torch by default
     */
    const val DEFAULT_TORCH_STATE = false
    
    /**
     * Auto-retry on camera errors
     */
    const val AUTO_RETRY_ON_ERROR = true
    
    /**
     * Max auto-retry attempts
     */
    const val MAX_AUTO_RETRY = 3
    
    /**
     * Camera selection preference
     */
    enum class CameraPreference {
        BACK_CAMERA,
        FRONT_CAMERA,
        ANY_AVAILABLE
    }
    
    /**
     * Image quality settings
     */
    enum class ImageQuality {
        LOW,      // 640x480 - faster processing, lower accuracy
        MEDIUM,   // 1280x720 - balanced (default)
        HIGH      // 1920x1080 - best accuracy, slower processing
    }
    
    /**
     * Get resolution based on quality setting
     */
    fun getResolutionForQuality(quality: ImageQuality): Size {
        return when (quality) {
            ImageQuality.LOW -> LOW_RESOLUTION
            ImageQuality.MEDIUM -> TARGET_RESOLUTION
            ImageQuality.HIGH -> HIGH_RESOLUTION
        }
    }
    
    /**
     * Camera error messages
     */
    object ErrorMessages {
        const val CAMERA_DISABLED = "Camera is disabled. Please enable it in device settings."
        const val CAMERA_IN_USE = "Camera is being used by another app. Please close other camera apps."
        const val MAX_CAMERAS_IN_USE = "Maximum cameras in use. Please close other camera apps."
        const val CAMERA_UNAVAILABLE = "Camera is temporarily unavailable. Please try again."
        const val NO_CAMERA = "No camera available on this device."
        const val PERMISSION_DENIED = "Camera permission is required to scan IMEI."
        const val INIT_FAILED = "Camera initialization failed. Please restart the app."
        const val BIND_FAILED = "Failed to bind camera to lifecycle."
    }
    
    /**
     * User guidance messages
     */
    object GuidanceMessages {
        const val POSITION_DEVICE = "Position camera over IMEI label"
        const val HOLD_STEADY = "Hold steady while scanning..."
        const val MOVE_CLOSER = "Move closer to the IMEI label"
        const val BETTER_LIGHT = "Try to improve lighting"
        const val CLEAN_LENS = "Clean camera lens for better results"
        const val SCANNING = "Scanning for IMEI..."
        const val FOUND_ONE = "Found 1 IMEI, scanning for second..."
        const val SUCCESS = "IMEI scanned successfully!"
        const val REDUCE_ANGLE = "Tilt is OK - adjusting..."
        const val AVOID_REFLECTION = "Avoid reflections and glare"
        const val TILT_HANDLING = "Angle detected - processing..."
    }
    
    /**
     * Real-world label detection tips
     */
    object ScanningTips {
        val TIPS = listOf(
            "Hold phone 10-15cm from label",
            "Ensure label is well-lit",
            "Keep phone steady",
            "Avoid shadows and glare",
            "Clean camera lens if blurry",
            "Label should fill 60-80% of screen",
            "Tap to focus if needed",
            "Tilt angle is OK - scanner adapts",
            "Works from multiple angles"
        )
        
        fun getRandomTip(): String = TIPS.random()
    }
    
    /**
     * Text preprocessing settings for better OCR
     */
    object TextProcessing {
        // Filter out common non-IMEI numbers found on phone boxes
        val EXCLUDE_PATTERNS = listOf(
            Regex("\\b\\d{10}\\b"),          // 10-digit numbers (phone numbers)
            Regex("\\b\\d{12}\\b"),          // 12-digit numbers (serial numbers)
            Regex("\\b\\d{13}\\b"),          // 13-digit numbers (barcodes)
            Regex("^[0-9]{2,4}[-/][0-9]{2}[-/][0-9]{2,4}$")  // Dates
        )
        
        // Keywords that indicate nearby text is likely IMEI
        val IMEI_KEYWORDS = listOf(
            "IMEI", "imei", "Imei",
            "IMEI1", "IMEI2", "IMEI 1", "IMEI 2",
            "Device ID", "Serial"
        )
        
        // Minimum confidence for text recognition
        const val MIN_CONFIDENCE = 0.6f
    }
}
