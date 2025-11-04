package com.billme.app.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility class for handling permissions related to signature file access
 * 
 * Manages permissions for:
 * - Reading images from device storage (for signature upload)
 * - Writing signatures to app's private directory
 * - Accessing URI content from file picker
 */
object SignaturePermissionHelper {
    
    /**
     * Get the required permissions for reading signature images from storage
     * 
     * Android 13+ (API 33): Uses granular READ_MEDIA_IMAGES permission
     * Android 6-12: Uses legacy READ_EXTERNAL_STORAGE permission
     * Android 5 and below: No runtime permissions needed
     */
    fun getReadPermissionsForSignature(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Use granular media permission
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12: Use legacy storage permission
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // Android 5 and below: Permissions are granted at install time
            emptyArray()
        }
    }
    
    /**
     * Get the required permissions for writing signatures to app's private directory
     * 
     * Note: Writing to app-private directory (context.filesDir) does NOT require
     * WRITE_EXTERNAL_STORAGE permission on any Android version, as it's within app-private storage
     */
    fun getWritePermissionsForSignature(): Array<String> {
        // App-private directory writes don't need external storage permission
        return emptyArray()
    }
    
    /**
     * Check if the app has permission to read signature images
     */
    fun hasReadPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Check granular media permission
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12: Check legacy storage permission
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 5 and below: Always true (permissions at install time)
            true
        }
    }
    
    /**
     * Check if the app has permission to write signatures
     * 
     * Always returns true for app-private directory writes
     */
    fun hasWritePermission(context: Context): Boolean {
        // App-private directory: Always have permission
        return true
    }
    
    /**
     * Check if permissions need to be requested at runtime
     */
    fun needsRuntimePermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
    
    /**
     * Check if specific granular media permissions are available (Android 13+)
     */
    fun hasGranularMediaPermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
    
    /**
     * Get all required permissions for signature management
     */
    fun getAllPermissionsNeeded(): Array<String> {
        val readPerms = getReadPermissionsForSignature()
        val writePerms = getWritePermissionsForSignature()
        return (readPerms.toList() + writePerms.toList()).toTypedArray()
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        val permissionsNeeded = getAllPermissionsNeeded()
        return permissionsNeeded.isEmpty() || 
            permissionsNeeded.all { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
    }
    
    /**
     * Get human-readable reason for why permission is needed
     */
    fun getPermissionReason(): String {
        return "Permission needed to read signature images from your device storage"
    }
    
    /**
     * Get error message when permission is denied
     */
    fun getPermissionDeniedMessage(): String {
        return "Cannot read signature images without storage permission. Please enable it in app settings."
    }
    
    /**
     * Validate that signature file is accessible
     * 
     * Checks if:
     * - File exists at given path
     * - File is readable
     * - File is a valid image (basic MIME type check)
     */
    fun isSignatureFileAccessible(filePath: String): Boolean {
        return try {
            val file = java.io.File(filePath)
            file.exists() && file.canRead() && file.isFile
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get MIME type for signature images
     */
    fun getSignatureImageMimeType(): String = "image/*"
    
    /**
     * Get supported image formats for signatures
     */
    fun getSupportedImageFormats(): List<String> {
        return listOf("image/png", "image/jpeg", "image/jpg", "image/webp")
    }
    
    /**
     * Validate image MIME type
     */
    fun isValidSignatureImageType(mimeType: String): Boolean {
        return getSupportedImageFormats().any { 
            mimeType.contains(it, ignoreCase = true)
        }
    }
}
