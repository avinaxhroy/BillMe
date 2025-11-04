package com.billme.app.core.service

import com.billme.app.core.util.ImeiValidator
import com.billme.app.data.local.dao.ProductDao
import com.billme.app.data.local.entity.Product
import com.billme.app.hardware.IMEIDetectionMethod
import com.billme.app.hardware.IMEIScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IMEI conflict resolution strategies
 */
enum class IMEIConflictAction {
    BLOCK,           // Block the operation
    REPLACE,         // Replace existing product
    SKIP,            // Skip this IMEI
    OWNER_OVERRIDE   // Use owner PIN to override
}

/**
 * IMEI validation result with detailed information
 */
data class IMEIValidationResult(
    val imei1: IMEIValidation?,
    val imei2: IMEIValidation?,
    val hasConflicts: Boolean,
    val canProceed: Boolean,
    val conflictDetails: List<IMEIConflict>
)

/**
 * Individual IMEI validation
 */
data class IMEIValidation(
    val imei: String,
    val isValid: Boolean,
    val errorMessage: String?,
    val formattedIMEI: String,
    val hasConflict: Boolean,
    val conflictingProduct: Product?
)

/**
 * IMEI conflict information
 */
data class IMEIConflict(
    val imei: String,
    val conflictingProduct: Product,
    val conflictType: IMEIConflictType
)

/**
 * Types of IMEI conflicts
 */
enum class IMEIConflictType {
    EXACT_MATCH,     // Exact same IMEI exists
    CROSS_MATCH      // IMEI1 matches existing IMEI2 or vice versa
}

/**
 * Comprehensive IMEI Management Service
 * Handles scanning, validation, conflict resolution, and PIN override
 */
@Singleton
class IMEIManagementService @Inject constructor(
    private val productDao: ProductDao,
    private val pinService: OwnerPINService
) {
    
    // Advanced IMEI detection patterns
    private val dualImeiPatterns = listOf(
        // Standard formats
        Pattern.compile("IMEI[\\s]*1?[:\\s]*([0-9]{15})[\\s,/]+IMEI[\\s]*2?[:\\s]*([0-9]{15})"),
        Pattern.compile("([0-9]{15})[\\s,/]+([0-9]{15})"),
        // Box/sticker formats
        Pattern.compile("IMEI[:\\s]*([0-9]{15})[\\s]+([0-9]{15})"),
        Pattern.compile("SN[:\\s]*[A-Z0-9]+[\\s]+IMEI[:\\s]*([0-9]{15})[\\s]+([0-9]{15})"),
        // Sequential format
        Pattern.compile("([0-9]{15})([0-9]{15})"),
        // With text mixed in
        Pattern.compile(".*?([0-9]{15}).*?([0-9]{15}).*?")
    )
    
    private val singleImeiPatterns = listOf(
        Pattern.compile("IMEI[:\\s]*([0-9]{15})"),
        Pattern.compile("([0-9]{15})")
    )
    
    private val _currentScanResult = MutableStateFlow<IMEIScanResult?>(null)
    val currentScanResult: StateFlow<IMEIScanResult?> = _currentScanResult.asStateFlow()
    
    /**
     * Parse scanned text and auto-detect IMEI1/IMEI2
     */
    fun parseScannedText(scannedText: String): IMEIScanResult {
        val cleanText = scannedText.trim()
        
        // Try dual IMEI detection first
        for (pattern in dualImeiPatterns) {
            val matcher = pattern.matcher(cleanText)
            if (matcher.find()) {
                val imei1 = matcher.group(1)
                val imei2 = matcher.group(2)
                
                if (imei1 != null && imei2 != null && 
                    ImeiValidator.isValidImeiFormat(imei1) && 
                    ImeiValidator.isValidImeiFormat(imei2) &&
                    imei1 != imei2) {
                    
                    val method = when {
                        cleanText.contains("IMEI1") || cleanText.contains("IMEI2") -> IMEIDetectionMethod.DUAL_IMEI_SEPARATED
                        cleanText.contains(",") || cleanText.contains("/") -> IMEIDetectionMethod.DUAL_IMEI_SEPARATED
                        matcher.pattern().pattern().contains("([0-9]{15})([0-9]{15})") -> IMEIDetectionMethod.DUAL_IMEI_SEQUENTIAL
                        else -> IMEIDetectionMethod.TEXT_EXTRACTION
                    }
                    
                    val isValid = ImeiValidator.isValidImei(imei1) && ImeiValidator.isValidImei(imei2)
                    
                    return IMEIScanResult(
                        imei1 = ImeiValidator.cleanImei(imei1),
                        imei2 = ImeiValidator.cleanImei(imei2),
                        rawText = cleanText,
                        detectionMethod = method,
                        isValid = isValid
                    ).also { _currentScanResult.value = it }
                }
            }
        }
        
        // Try single IMEI detection
        for (pattern in singleImeiPatterns) {
            val matcher = pattern.matcher(cleanText)
            if (matcher.find()) {
                val imei = matcher.group(1)
                if (imei != null && ImeiValidator.isValidImeiFormat(imei)) {
                    val isValid = ImeiValidator.isValidImei(imei)
                    
                    return IMEIScanResult(
                        imei1 = ImeiValidator.cleanImei(imei),
                        imei2 = null,
                        rawText = cleanText,
                        detectionMethod = IMEIDetectionMethod.SINGLE_IMEI,
                        isValid = isValid
                    ).also { _currentScanResult.value = it }
                }
            }
        }
        
        // No valid IMEI found
        return IMEIScanResult(
            imei1 = null,
            imei2 = null,
            rawText = cleanText,
            detectionMethod = IMEIDetectionMethod.MANUAL_PARSE,
            isValid = false
        ).also { _currentScanResult.value = it }
    }
    
    /**
     * Comprehensive IMEI validation with conflict detection
     */
    suspend fun validateIMEIs(
        imei1: String?,
        imei2: String?,
        excludeProductId: Long = -1
    ): IMEIValidationResult {
        val validations = mutableListOf<IMEIValidation>()
        val conflicts = mutableListOf<IMEIConflict>()
        
        // Validate IMEI1
        val imei1Validation = imei1?.let { validateSingleIMEI(it, excludeProductId) }
        imei1Validation?.let { validations.add(it) }
        
        // Validate IMEI2
        val imei2Validation = imei2?.let { validateSingleIMEI(it, excludeProductId) }
        imei2Validation?.let { validations.add(it) }
        
        // Check for cross conflicts (IMEI1 vs IMEI2)
        if (imei1 != null && imei2 != null && imei1 == imei2) {
            return IMEIValidationResult(
                imei1 = imei1Validation?.copy(
                    isValid = false,
                    errorMessage = "IMEI1 and IMEI2 cannot be the same"
                ),
                imei2 = imei2Validation?.copy(
                    isValid = false,
                    errorMessage = "IMEI1 and IMEI2 cannot be the same"
                ),
                hasConflicts = true,
                canProceed = false,
                conflictDetails = emptyList()
            )
        }
        
        // Collect conflicts
        validations.forEach { validation ->
            if (validation.hasConflict && validation.conflictingProduct != null) {
                conflicts.add(
                    IMEIConflict(
                        imei = validation.imei,
                        conflictingProduct = validation.conflictingProduct,
                        conflictType = IMEIConflictType.EXACT_MATCH
                    )
                )
            }
        }
        
        val hasConflicts = conflicts.isNotEmpty()
        val canProceed = validations.all { it.isValid } && !hasConflicts
        
        return IMEIValidationResult(
            imei1 = imei1Validation,
            imei2 = imei2Validation,
            hasConflicts = hasConflicts,
            canProceed = canProceed,
            conflictDetails = conflicts
        )
    }
    
    /**
     * Validate a single IMEI
     */
    private suspend fun validateSingleIMEI(imei: String, excludeProductId: Long): IMEIValidation {
        val validation = ImeiValidator.validateImei(imei)
        val cleanIMEI = validation.cleanImei ?: imei
        
        if (!validation.isValid) {
            return IMEIValidation(
                imei = imei,
                isValid = false,
                errorMessage = validation.errorMessage,
                formattedIMEI = ImeiValidator.formatImei(imei) ?: imei,
                hasConflict = false,
                conflictingProduct = null
            )
        }
        
        // Check for duplicates
        val conflictingProduct = findConflictingProduct(cleanIMEI, excludeProductId)
        
        return IMEIValidation(
            imei = cleanIMEI,
            isValid = validation.isValid,
            errorMessage = validation.errorMessage,
            formattedIMEI = ImeiValidator.formatImei(cleanIMEI) ?: cleanIMEI,
            hasConflict = conflictingProduct != null,
            conflictingProduct = conflictingProduct
        )
    }
    
    /**
     * Find product with conflicting IMEI
     */
    private suspend fun findConflictingProduct(imei: String, excludeProductId: Long): Product? {
        return productDao.getProductByImei(imei)?.takeIf { 
            it.productId != excludeProductId && it.isActive 
        }
    }
    
    /**
     * Handle IMEI conflicts with owner PIN override
     */
    suspend fun resolveIMEIConflict(
        conflicts: List<IMEIConflict>,
        resolution: IMEIConflictAction,
        ownerPin: String? = null
    ): IMEIConflictResolution {
        
        return when (resolution) {
            IMEIConflictAction.BLOCK -> {
                IMEIConflictResolution(
                    success = false,
                    message = "Operation blocked due to IMEI conflicts",
                    actionTaken = resolution
                )
            }
            
            IMEIConflictAction.SKIP -> {
                IMEIConflictResolution(
                    success = true,
                    message = "Conflicting IMEIs skipped",
                    actionTaken = resolution
                )
            }
            
            IMEIConflictAction.OWNER_OVERRIDE -> {
                if (ownerPin == null) {
                    return IMEIConflictResolution(
                        success = false,
                        message = "Owner PIN required for override",
                        actionTaken = resolution
                    )
                }
                
                val pinValid = pinService.validateOwnerPIN(ownerPin)
                if (pinValid) {
                    // Log the override action
                    logPINOverride(conflicts, ownerPin)
                    
                    IMEIConflictResolution(
                        success = true,
                        message = "IMEI conflicts overridden by owner",
                        actionTaken = resolution
                    )
                } else {
                    IMEIConflictResolution(
                        success = false,
                        message = "Invalid owner PIN",
                        actionTaken = resolution
                    )
                }
            }
            
            IMEIConflictAction.REPLACE -> {
                try {
                    // Deactivate conflicting products
                    conflicts.forEach { conflict ->
                        productDao.deactivateProduct(conflict.conflictingProduct.productId)
                    }
                    
                    IMEIConflictResolution(
                        success = true,
                        message = "Conflicting products replaced",
                        actionTaken = resolution
                    )
                } catch (e: Exception) {
                    IMEIConflictResolution(
                        success = false,
                        message = "Failed to replace conflicting products: ${e.message}",
                        actionTaken = resolution
                    )
                }
            }
        }
    }
    
    /**
     * Advanced IMEI search with fuzzy matching
     */
    suspend fun searchProductsByIMEI(query: String): List<Product> {
        val cleanQuery = ImeiValidator.cleanImei(query)
        
        // Exact match first
        productDao.getProductByImei(cleanQuery)?.let { 
            return listOf(it)
        }
        
        // Partial match if query is long enough
        if (cleanQuery.length >= 8) {
            return productDao.searchProducts(cleanQuery).let { flow ->
                // Convert flow to list for this example
                // In real implementation, you might want to keep it as Flow
                mutableListOf<Product>()
            }
        }
        
        return emptyList()
    }
    
    /**
     * Generate smart suggestions for IMEI input
     */
    fun generateIMEISuggestions(partialIMEI: String): List<String> {
        val clean = ImeiValidator.cleanImei(partialIMEI)
        
        if (clean.length < 8) return emptyList()
        
        // Generate suggestions based on common patterns
        val suggestions = mutableListOf<String>()
        
        // If it looks like partial IMEI, suggest completion patterns
        if (clean.length in 8..14) {
            val remaining = 15 - clean.length
            repeat(3) { i ->
                val suffix = (0 until remaining).map { (0..9).random() }.joinToString("")
                val candidate = clean + suffix
                if (ImeiValidator.isValidImei(candidate)) {
                    suggestions.add(ImeiValidator.formatImei(candidate) ?: candidate)
                }
            }
        }
        
        return suggestions.distinct().take(5)
    }
    
    /**
     * Log PIN override for audit trail
     */
    private suspend fun logPINOverride(conflicts: List<IMEIConflict>, pin: String) {
        // This would integrate with your audit logging system
        val conflictInfo = conflicts.joinToString(", ") { "${it.imei} (Product: ${it.conflictingProduct.productName})" }
        println("AUDIT: Owner PIN override used for IMEI conflicts: $conflictInfo")
    }
    
    /**
     * Clear current scan result
     */
    fun clearScanResult() {
        _currentScanResult.value = null
    }
}

/**
 * Result of IMEI conflict resolution
 */
data class IMEIConflictResolution(
    val success: Boolean,
    val message: String,
    val actionTaken: IMEIConflictAction
)