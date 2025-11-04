package com.billme.app.core.automation

import com.billme.app.core.util.ImeiValidator
import com.billme.app.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple IMEI scanner and validator
 * Handles basic IMEI validation and duplicate detection
 */
@Singleton
class SimpleImeiScanner @Inject constructor(
    private val productRepository: ProductRepository
) {
    
    /**
     * Validate and check IMEI
     */
    suspend fun validateImei(imei: String): ImeiValidationResult = withContext(Dispatchers.IO) {
        try {
            // Basic format validation
            if (imei.isBlank()) {
                return@withContext ImeiValidationResult.Error("IMEI cannot be empty")
            }
            
            if (imei.length != 15) {
                return@withContext ImeiValidationResult.Error("IMEI must be 15 digits")
            }
            
            if (!imei.all { it.isDigit() }) {
                return@withContext ImeiValidationResult.Error("IMEI must contain only digits")
            }
            
            // Luhn algorithm validation
            if (!ImeiValidator.isValidImei(imei)) {
                return@withContext ImeiValidationResult.Error("IMEI failed Luhn check")
            }
            
            // Check for duplicates in inventory
            val existingProduct = productRepository.getProductByImei(imei)
            
            if (existingProduct != null) {
                return@withContext ImeiValidationResult.Duplicate(
                    imei = imei,
                    existingProductId = existingProduct.productId,
                    existingProductName = existingProduct.productName
                )
            }
            
            // IMEI is valid and unique
            ImeiValidationResult.Valid(
                imei = imei,
                tac = imei.substring(0, 8),
                serialNumber = imei.substring(8, 14),
                checkDigit = imei.substring(14, 15)
            )
            
        } catch (e: Exception) {
            ImeiValidationResult.Error(e.message ?: "Validation failed")
        }
    }
    
    /**
     * Validate dual IMEI (for dual SIM phones)
     */
    suspend fun validateDualImei(
        imei1: String,
        imei2: String?
    ): DualImeiValidationResult = withContext(Dispatchers.IO) {
        try {
            // Validate first IMEI
            val result1 = validateImei(imei1)
            if (result1 is ImeiValidationResult.Error) {
                return@withContext DualImeiValidationResult.Error("IMEI 1: ${result1.message}")
            }
            if (result1 is ImeiValidationResult.Duplicate) {
                return@withContext DualImeiValidationResult.Error(
                    "IMEI 1 already exists in product: ${result1.existingProductName}"
                )
            }
            
            // Validate second IMEI if provided
            if (!imei2.isNullOrBlank()) {
                val result2 = validateImei(imei2)
                if (result2 is ImeiValidationResult.Error) {
                    return@withContext DualImeiValidationResult.Error("IMEI 2: ${result2.message}")
                }
                if (result2 is ImeiValidationResult.Duplicate) {
                    return@withContext DualImeiValidationResult.Error(
                        "IMEI 2 already exists in product: ${result2.existingProductName}"
                    )
                }
                
                // Check if both IMEIs are the same
                if (imei1 == imei2) {
                    return@withContext DualImeiValidationResult.Error("IMEI 1 and IMEI 2 cannot be the same")
                }
                
                return@withContext DualImeiValidationResult.BothValid(imei1, imei2)
            }
            
            // Only IMEI 1 provided
            DualImeiValidationResult.SingleValid(imei1)
            
        } catch (e: Exception) {
            DualImeiValidationResult.Error(e.message ?: "Validation failed")
        }
    }
    
    /**
     * Extract device info from IMEI (TAC lookup would require external database)
     */
    fun extractBasicInfo(imei: String): ImeiBasicInfo {
        return ImeiBasicInfo(
            tac = imei.substring(0, 8),
            serialNumber = imei.substring(8, 14),
            checkDigit = imei.substring(14, 15),
            manufacturer = "Unknown", // Would need TAC database
            model = "Unknown" // Would need TAC database
        )
    }
}

/**
 * IMEI validation result
 */
sealed class ImeiValidationResult {
    data class Valid(
        val imei: String,
        val tac: String,
        val serialNumber: String,
        val checkDigit: String
    ) : ImeiValidationResult()
    
    data class Duplicate(
        val imei: String,
        val existingProductId: Long,
        val existingProductName: String
    ) : ImeiValidationResult()
    
    data class Error(
        val message: String
    ) : ImeiValidationResult()
}

/**
 * Dual IMEI validation result
 */
sealed class DualImeiValidationResult {
    data class BothValid(val imei1: String, val imei2: String) : DualImeiValidationResult()
    data class SingleValid(val imei1: String) : DualImeiValidationResult()
    data class Error(val message: String) : DualImeiValidationResult()
}

/**
 * Basic IMEI info
 */
data class ImeiBasicInfo(
    val tac: String,
    val serialNumber: String,
    val checkDigit: String,
    val manufacturer: String,
    val model: String
)
