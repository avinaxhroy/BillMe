package com.billme.app.core.scanner

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Camera Helper - Device capability checks and diagnostics
 */
object CameraHelper {
    
    /**
     * Check if device has any camera
     */
    fun hasCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    
    /**
     * Check if device has back camera
     */
    fun hasBackCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }
    
    /**
     * Check if device has front camera
     */
    fun hasFrontCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }
    
    /**
     * Check if camera has autofocus
     */
    fun hasAutofocus(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)
    }
    
    /**
     * Check if camera has flash
     */
    fun hasFlash(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }
    
    /**
     * Get available camera IDs
     */
    fun getAvailableCameraIds(context: Context): List<String> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.cameraIdList?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Check if specific camera supports autofocus
     */
    fun cameraSupportsAutofocus(context: Context, cameraId: String): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val characteristics = cameraManager?.getCameraCharacteristics(cameraId)
            val afModes = characteristics?.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            afModes != null && afModes.isNotEmpty() && 
                (afModes.contains(CameraCharacteristics.CONTROL_AF_MODE_AUTO) ||
                 afModes.contains(CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get camera diagnostic information
     */
    suspend fun getCameraDiagnostics(context: Context): CameraDiagnostics = withContext(Dispatchers.IO) {
        CameraDiagnostics(
            hasCameraHardware = hasCameraHardware(context),
            hasBackCamera = hasBackCamera(context),
            hasFrontCamera = hasFrontCamera(context),
            hasAutofocus = hasAutofocus(context),
            hasFlash = hasFlash(context),
            availableCameras = getAvailableCameraIds(context),
            androidVersion = Build.VERSION.SDK_INT,
            cameraXAvailable = isCameraXAvailable(context)
        )
    }
    
    /**
     * Check if CameraX is available and working
     */
    private suspend fun isCameraXAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.hasCamera(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get recommended image quality based on device capabilities
     */
    fun getRecommendedQuality(context: Context): IMEIScannerConfig.ImageQuality {
        // Check device performance characteristics
        val cameraCount = getAvailableCameraIds(context).size
        val hasGoodHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && cameraCount > 1
        
        return when {
            // High-end devices
            hasGoodHardware && hasAutofocus(context) -> 
                IMEIScannerConfig.ImageQuality.HIGH
            // Mid-range devices
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> 
                IMEIScannerConfig.ImageQuality.MEDIUM
            // Older devices
            else -> 
                IMEIScannerConfig.ImageQuality.LOW
        }
    }
    
    /**
     * Get human-readable error message from exception
     */
    fun getErrorMessage(exception: Exception): String {
        val message = exception.message ?: return IMEIScannerConfig.ErrorMessages.INIT_FAILED
        
        return when {
            message.contains("CAMERA_DISABLED", ignoreCase = true) -> 
                IMEIScannerConfig.ErrorMessages.CAMERA_DISABLED
            message.contains("CAMERA_IN_USE", ignoreCase = true) -> 
                IMEIScannerConfig.ErrorMessages.CAMERA_IN_USE
            message.contains("MAX_CAMERAS_IN_USE", ignoreCase = true) -> 
                IMEIScannerConfig.ErrorMessages.MAX_CAMERAS_IN_USE
            message.contains("CAMERA_UNAVAILABLE", ignoreCase = true) -> 
                IMEIScannerConfig.ErrorMessages.CAMERA_UNAVAILABLE
            message.contains("Permission", ignoreCase = true) -> 
                IMEIScannerConfig.ErrorMessages.PERMISSION_DENIED
            else -> 
                "Camera error: $message"
        }
    }
}

/**
 * Camera diagnostics data
 */
data class CameraDiagnostics(
    val hasCameraHardware: Boolean,
    val hasBackCamera: Boolean,
    val hasFrontCamera: Boolean,
    val hasAutofocus: Boolean,
    val hasFlash: Boolean,
    val availableCameras: List<String>,
    val androidVersion: Int,
    val cameraXAvailable: Boolean
) {
    /**
     * Check if device is capable of IMEI scanning
     */
    fun isCapableOfScanning(): Boolean {
        return hasCameraHardware && (hasBackCamera || hasFrontCamera) && cameraXAvailable
    }
    
    /**
     * Get diagnostic summary
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Camera Hardware: ${if (hasCameraHardware) "✓" else "✗"}")
            appendLine("Back Camera: ${if (hasBackCamera) "✓" else "✗"}")
            appendLine("Front Camera: ${if (hasFrontCamera) "✓" else "✗"}")
            appendLine("Autofocus: ${if (hasAutofocus) "✓" else "✗"}")
            appendLine("Flash: ${if (hasFlash) "✓" else "✗"}")
            appendLine("Available Cameras: ${availableCameras.size}")
            appendLine("Android Version: $androidVersion")
            appendLine("CameraX Available: ${if (cameraXAvailable) "✓" else "✗"}")
            appendLine()
            appendLine("Scanning Capable: ${if (isCapableOfScanning()) "✓ YES" else "✗ NO"}")
        }
    }
}
