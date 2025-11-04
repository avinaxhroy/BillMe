package com.billme.app.core.util

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Utility class for handling Android 13+ (API 33) scoped storage permissions
 * Provides helper methods to request appropriate storage permissions based on Android version
 */
object StoragePermissionHelper {
    
    /**
     * Get the required storage read permissions based on Android API level
     * 
     * For Android 13+ (API 33): Returns new granular media permissions
     * For Android 12 and below: Returns legacy READ_EXTERNAL_STORAGE
     */
    fun getReadStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires granular media permissions
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 and below use legacy permission
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Get the required storage write permissions based on Android API level
     * 
     * For Android 13+ (API 33): No specific write permission needed for MediaStore operations
     * For Android 12 and below: Returns WRITE_EXTERNAL_STORAGE
     */
    fun getWriteStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ handles writes through MediaStore without explicit write permission
            emptyArray()
        } else {
            // Android 12 and below need explicit write permission
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Get all required storage permissions (read + write)
     */
    fun getAllStoragePermissions(): Array<String> {
        val readPerms = getReadStoragePermissions()
        val writePerms = getWriteStoragePermissions()
        return (readPerms.toList() + writePerms.toList()).toTypedArray()
    }
    
    /**
     * Check if we need to handle storage permissions
     * Returns true if API level requires permission handling
     */
    fun needsStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M // API 23+
    }
    
    /**
     * Check if read media permissions are needed (Android 13+)
     */
    fun needsReadMediaPermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU // API 33+
    }
}
