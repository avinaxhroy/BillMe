package com.billme.app.core.util

import android.database.sqlite.SQLiteException
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error message handler for user-friendly error messages
 */
@Singleton
class ErrorMessageHandler @Inject constructor() {
    
    /**
     * Convert exception to user-friendly message
     */
    fun getUserFriendlyMessage(exception: Throwable, context: String = ""): String {
        return when (exception) {
            is IOException -> handleIOException(exception, context)
            is SQLiteException -> handleDatabaseException(exception, context)
            is IllegalArgumentException -> handleValidationException(exception, context)
            is UnknownHostException -> "No internet connection. Please check your network settings."
            is SecurityException -> "Permission denied. Please grant necessary permissions."
            else -> handleGenericException(exception, context)
        }
    }
    
    private fun handleIOException(exception: IOException, context: String): String {
        return when {
            exception.message?.contains("ENOSPC", ignoreCase = true) == true ->
                "Not enough storage space. Please free up some space and try again."
            exception.message?.contains("Permission denied", ignoreCase = true) == true ->
                "Cannot access file. Please check app permissions."
            context.isNotEmpty() -> "Failed to $context. Please try again."
            else -> "An error occurred while accessing files. Please try again."
        }
    }
    
    private fun handleDatabaseException(exception: SQLiteException, context: String): String {
        return when {
            exception.message?.contains("UNIQUE", ignoreCase = true) == true ->
                "This item already exists. Please use a different identifier."
            exception.message?.contains("FOREIGN KEY", ignoreCase = true) == true ->
                "Cannot delete this item as it's being used elsewhere."
            exception.message?.contains("constraint", ignoreCase = true) == true ->
                "Invalid data. Please check your input and try again."
            context.isNotEmpty() -> "Failed to save $context. Please try again."
            else -> "Database error. Please restart the app and try again."
        }
    }
    
    private fun handleValidationException(exception: IllegalArgumentException, context: String): String {
        val message = exception.message
        return when {
            message != null && message.isNotBlank() -> message
            context.isNotEmpty() -> "Invalid $context. Please check your input."
            else -> "Invalid input. Please check your data and try again."
        }
    }
    
    private fun handleGenericException(exception: Throwable, context: String): String {
        val message = exception.message
        return when {
            message != null && message.length < 100 -> message
            context.isNotEmpty() -> "Failed to $context. Please try again."
            else -> "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Get specific error messages for common operations
     */
    object Messages {
        const val PRODUCT_NOT_FOUND = "Product not found. It may have been deleted."
        const val INVALID_QUANTITY = "Invalid quantity. Please enter a number greater than 0."
        const val INSUFFICIENT_STOCK = "Insufficient stock. Available quantity: "
        const val INVALID_PRICE = "Invalid price. Price must be greater than 0."
        const val INVALID_GST_NUMBER = "Invalid GST number. Must be 15 characters in format: 00AAAAA0000A0Z0"
        const val INVALID_EMAIL = "Invalid email address. Please check and try again."
        const val INVALID_PHONE = "Invalid phone number. Must be 10 digits."
        const val NETWORK_ERROR = "Network error. Please check your internet connection."
        const val FILE_NOT_FOUND = "File not found. It may have been moved or deleted."
        const val PERMISSION_REQUIRED = "Permission required. Please grant necessary permissions in settings."
        const val BACKUP_FAILED = "Backup failed. Please check storage space and try again."
        const val RESTORE_FAILED = "Restore failed. The backup file may be corrupted."
        const val PRINT_FAILED = "Printing failed. Please check printer connection and try again."
        const val SHARE_FAILED = "Sharing failed. Please ensure the file exists and try again."
        const val BARCODE_SCAN_FAILED = "Barcode scanning failed. Please try again or enter manually."
        const val CAMERA_PERMISSION_REQUIRED = "Camera permission is required to scan barcodes."
        const val STORAGE_PERMISSION_REQUIRED = "Storage permission is required to save files."
        const val LOW_STORAGE = "Low storage space. Please free up space to continue."
        const val DUPLICATE_ENTRY = "This entry already exists. Please use a different identifier."
        const val DELETE_IN_USE = "Cannot delete. This item is being used in transactions."
        const val INVALID_DATE_RANGE = "Invalid date range. End date must be after start date."
        const val NO_DATA_FOUND = "No data found for the selected criteria."
        const val EXPORT_SUCCESS = "Data exported successfully!"
        const val SAVE_SUCCESS = "Saved successfully!"
        const val DELETE_SUCCESS = "Deleted successfully!"
        const val UPDATE_SUCCESS = "Updated successfully!"
        const val SYNC_SUCCESS = "Synced successfully!"
        
        fun insufficientStock(available: Int) = "$INSUFFICIENT_STOCK$available"
        fun itemNotFound(itemType: String) = "$itemType not found. It may have been deleted."
        fun deletedSuccessfully(itemType: String) = "$itemType deleted successfully!"
        fun savedSuccessfully(itemType: String) = "$itemType saved successfully!"
        fun updatedSuccessfully(itemType: String) = "$itemType updated successfully!"
    }
}
