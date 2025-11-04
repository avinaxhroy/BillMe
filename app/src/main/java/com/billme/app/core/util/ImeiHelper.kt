package com.billme.app.core.util

/**
 * IMEI Helper - Centralized IMEI utilities for the entire app
 * Provides high-level IMEI operations with consistent validation
 */
object ImeiHelper {
    
    /**
     * Clean IMEI input (remove non-digits, limit to 15)
     */
    fun cleanInput(input: String): String {
        return input.filter { it.isDigit() }.take(15)
    }
    
    /**
     * Validate IMEI with comprehensive checks
     * Returns validation result with details
     */
    fun validate(imei: String): ImeiValidationResult {
        val cleaned = cleanInput(imei)
        
        // Length check
        if (cleaned.length != 15) {
            return ImeiValidationResult(
                isValid = false,
                imei = cleaned,
                errorMessage = "IMEI must be exactly 15 digits (got ${cleaned.length})"
            )
        }
        
        // Known invalid patterns
        if (ImeiValidator.isKnownInvalidPattern(cleaned)) {
            return ImeiValidationResult(
                isValid = false,
                imei = cleaned,
                errorMessage = "IMEI contains invalid pattern"
            )
        }
        
        // Digit variety check
        if (!ImeiValidator.hasSufficientDigitVariety(cleaned)) {
            return ImeiValidationResult(
                isValid = false,
                imei = cleaned,
                errorMessage = "IMEI has insufficient digit variety"
            )
        }
        
        // Luhn checksum validation
        if (!ImeiValidator.isValidImeiWithPatternCheck(cleaned)) {
            return ImeiValidationResult(
                isValid = false,
                imei = cleaned,
                errorMessage = "IMEI failed checksum validation"
            )
        }
        
        return ImeiValidationResult(
            isValid = true,
            imei = cleaned,
            errorMessage = null
        )
    }
    
    /**
     * Quick validation check (just returns boolean)
     */
    fun isValid(imei: String): Boolean {
        return validate(imei).isValid
    }
    
    /**
     * Validate dual IMEI pair
     */
    fun validateDualPair(imei1: String, imei2: String): DualImeiValidationResult {
        val result1 = validate(imei1)
        val result2 = validate(imei2)
        
        if (!result1.isValid) {
            return DualImeiValidationResult(
                isValid = false,
                imei1 = result1.imei,
                imei2 = result2.imei,
                errorMessage = "IMEI 1: ${result1.errorMessage}"
            )
        }
        
        if (!result2.isValid) {
            return DualImeiValidationResult(
                isValid = false,
                imei1 = result1.imei,
                imei2 = result2.imei,
                errorMessage = "IMEI 2: ${result2.errorMessage}"
            )
        }
        
        // Check if same
        if (result1.imei == result2.imei) {
            return DualImeiValidationResult(
                isValid = false,
                imei1 = result1.imei,
                imei2 = result2.imei,
                errorMessage = "IMEI 1 and IMEI 2 cannot be the same"
            )
        }
        
        // Check if compatible (likely from same device)
        if (!ImeiValidator.areLikelyDualIMEIs(result1.imei, result2.imei)) {
            return DualImeiValidationResult(
                isValid = true, // Still valid, just warning
                imei1 = result1.imei,
                imei2 = result2.imei,
                errorMessage = null,
                warning = "IMEIs may not be from the same device"
            )
        }
        
        return DualImeiValidationResult(
            isValid = true,
            imei1 = result1.imei,
            imei2 = result2.imei,
            errorMessage = null
        )
    }
    
    /**
     * Format IMEI for display (with separators)
     */
    fun format(imei: String): String {
        val cleaned = cleanInput(imei)
        return ImeiValidator.formatImei(cleaned) ?: cleaned
    }
    
    /**
     * Extract all potential IMEIs from text
     */
    fun extractFromText(text: String): List<String> {
        val pattern = Regex("\\b(\\d{15})\\b")
        return pattern.findAll(text)
            .map { it.value }
            .filter { isValid(it) }
            .distinct()
            .toList()
    }
    
    /**
     * Extract dual IMEI from text (returns pair or null)
     */
    fun extractDualFromText(text: String): Pair<String, String>? {
        val imeis = extractFromText(text)
        
        if (imeis.size >= 2) {
            // Try to find the best dual pair
            for (i in 0 until imeis.size - 1) {
                for (j in i + 1 until imeis.size) {
                    if (ImeiValidator.areLikelyDualIMEIs(imeis[i], imeis[j])) {
                        return Pair(imeis[i], imeis[j])
                    }
                }
            }
            // If no good pair found, return first two
            return Pair(imeis[0], imeis[1])
        }
        
        return null
    }
    
    /**
     * Calculate quality score for IMEI (0-100)
     */
    fun calculateQualityScore(imei: String): Int {
        val cleaned = cleanInput(imei)
        
        if (cleaned.length != 15) return 0
        
        var score = 0
        
        // Valid checksum
        if (ImeiValidator.isValidImei(cleaned)) score += 40
        
        // Digit variety
        val variety = cleaned.toSet().size
        score += (variety * 3).coerceAtMost(15)
        
        // No known invalid patterns
        if (!ImeiValidator.isKnownInvalidPattern(cleaned)) score += 20
        
        // TAC validation (not starting with 00)
        if (!cleaned.startsWith("00")) score += 15
        
        // No repeating patterns
        val chunks = cleaned.chunked(5)
        if (chunks.distinct().size > 1) score += 10
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * Check if IMEI looks like a test/fake number
     */
    fun isTestIMEI(imei: String): Boolean {
        val cleaned = cleanInput(imei)
        return ImeiValidator.isKnownInvalidPattern(cleaned)
    }
    
    /**
     * Get human-readable validation message
     */
    fun getValidationMessage(imei: String): String {
        val result = validate(imei)
        
        if (result.isValid) {
            val score = calculateQualityScore(imei)
            return when {
                score >= 80 -> "Valid IMEI ✓ (High quality)"
                score >= 60 -> "Valid IMEI ✓ (Good quality)"
                else -> "Valid IMEI ✓"
            }
        }
        
        return result.errorMessage ?: "Invalid IMEI"
    }
}

/**
 * IMEI validation result
 */
data class ImeiValidationResult(
    val isValid: Boolean,
    val imei: String,
    val errorMessage: String?
)

/**
 * Dual IMEI validation result
 */
data class DualImeiValidationResult(
    val isValid: Boolean,
    val imei1: String,
    val imei2: String,
    val errorMessage: String?,
    val warning: String? = null
)
