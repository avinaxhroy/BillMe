package com.billme.app.core.util

import java.util.regex.Pattern

object ImeiValidator {
    
    private val IMEI_PATTERN = Pattern.compile("^[0-9]{15}$")
    private val IMEI_WITH_SEPARATORS_PATTERN = Pattern.compile("^[0-9]{15}|[0-9]{2}[- ][0-9]{6}[- ][0-9]{6}[- ][0-9]{1}$")
    private val IMEI_IN_TEXT_PATTERN = Pattern.compile("(?:IMEI[:\\s]*)?([0-9]{15})(?:\\s|$|[^0-9])")
    private val DUAL_IMEI_PATTERN = Pattern.compile("(?:IMEI[12]?[:\\s]*)?([0-9]{15})(?:\\s|[,/]|\\s+IMEI[12]?[:\\s]*([0-9]{15}))?")
    
    /**
     * Validates IMEI using Luhn checksum algorithm
     */
    fun isValidImei(imei: String?): Boolean {
        if (imei.isNullOrBlank()) return false
        
        val cleanImei = cleanImei(imei)
        if (!IMEI_PATTERN.matcher(cleanImei).matches()) return false
        
        return validateLuhnChecksum(cleanImei)
    }
    
    /**
     * Extracts IMEI from text that might contain other information
     */
    fun extractImei(text: String?): String? {
        if (text.isNullOrBlank()) return null
        
        val matcher = IMEI_IN_TEXT_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
    
    /**
     * Extracts dual IMEIs from text
     */
    fun extractDualImei(text: String?): Pair<String?, String?> {
        if (text.isNullOrBlank()) return Pair(null, null)
        
        // Find all 15-digit numbers (potential IMEIs)
        val imeiMatches = IMEI_IN_TEXT_PATTERN.matcher(text)
        val imeis = mutableListOf<String?>()
        
        while (imeiMatches.find()) {
            imeis.add(imeiMatches.group(1))
            if (imeis.size >= 2) break
        }
        
        return when {
            imeis.size >= 2 -> Pair(imeis[0], imeis[1])
            imeis.size == 1 -> Pair(imeis[0], null)
            else -> Pair(null, null)
        }
    }
    
    /**
     * Cleans IMEI by removing separators and spaces
     */
    fun cleanImei(imei: String): String {
        return imei.replace(Regex("[^0-9]"), "")
    }
    
    /**
     * Formats IMEI with separators for display (123456-789012-345)
     */
    fun formatImei(imei: String?): String? {
        if (imei == null) return null
        
        val cleanImei = cleanImei(imei)
        if (cleanImei.length != 15) return imei
        
        return "${cleanImei.substring(0, 6)}-${cleanImei.substring(6, 12)}-${cleanImei.substring(12)}"
    }
    
    /**
     * Validates IMEI checksum using Luhn algorithm
     * 
     * Algorithm:
     * 1. Take the first 14 digits of the IMEI
     * 2. Starting from the right, double every second digit
     * 3. If doubling results in two digits, add them together
     * 4. Sum all digits
     * 5. Calculate check digit: (10 - (sum % 10)) % 10
     * 6. Compare with the 15th digit of the original IMEI
     */
    private fun validateLuhnChecksum(imei: String): Boolean {
        if (imei.length != 15) return false
        
        val digits = imei.take(14).map { it.digitToInt() }
        var sum = 0
        
        for (i in digits.indices) {
            var digit = digits[i]
            
            // Double every second digit from the right (positions 13, 11, 9, 7, 5, 3, 1)
            if ((13 - i) % 2 == 0) {
                digit *= 2
                if (digit > 9) {
                    digit = digit / 10 + digit % 10
                }
            }
            sum += digit
        }
        
        val checkDigit = (10 - (sum % 10)) % 10
        val lastDigit = imei.last().digitToInt()
        
        return checkDigit == lastDigit
    }
    
    /**
     * Check if IMEI has sufficient digit variety
     * Real IMEIs should have at least 5 different digits
     */
    fun hasSufficientDigitVariety(imei: String): Boolean {
        if (imei.length != 15) return false
        return imei.toSet().size >= 5
    }
    
    /**
     * Check if IMEI is a known invalid pattern
     * Filters out common fake/test IMEIs
     */
    fun isKnownInvalidPattern(imei: String): Boolean {
        if (imei.length != 15) return true
        
        // All same digit
        if (imei.toSet().size == 1) return true
        
        // Sequential patterns
        if (imei == "123456789012345" || imei == "012345678901234") return true
        
        // All zeros or nines
        if (imei.all { it == '0' } || imei.all { it == '9' }) return true
        
        // Repeating 5-digit patterns (e.g., 555555555555555)
        val chunks = imei.chunked(5)
        if (chunks.distinct().size == 1) return true
        
        // TAC (first 8 digits) should not start with 00
        if (imei.startsWith("00")) return true
        
        return false
    }
    
    /**
     * Enhanced IMEI validation with pattern checking
     */
    fun isValidImeiWithPatternCheck(imei: String?): Boolean {
        if (imei.isNullOrBlank()) return false
        
        val cleanImei = cleanImei(imei)
        if (!IMEI_PATTERN.matcher(cleanImei).matches()) return false
        
        // Check for known invalid patterns
        if (isKnownInvalidPattern(cleanImei)) return false
        
        // Check digit variety
        if (!hasSufficientDigitVariety(cleanImei)) return false
        
        // Finally validate checksum
        return validateLuhnChecksum(cleanImei)
    }
    
    /**
     * Calculate similarity between two IMEIs (0.0 to 1.0)
     * Used to verify dual IMEI from same device
     */
    fun calculateIMEISimilarity(imei1: String, imei2: String): Float {
        if (imei1.length != 15 || imei2.length != 15) return 0f
        
        val matchingDigits = imei1.zip(imei2).count { (a, b) -> a == b }
        return matchingDigits / 15f
    }
    
    /**
     * Check if two IMEIs are likely from the same device (dual IMEI)
     * Dual IMEIs typically share the TAC (first 8 digits) and differ in serial number
     */
    fun areLikelyDualIMEIs(imei1: String, imei2: String): Boolean {
        if (imei1.length != 15 || imei2.length != 15) return false
        if (imei1 == imei2) return false // Same IMEI is not dual
        
        val similarity = calculateIMEISimilarity(imei1, imei2)
        
        // Dual IMEIs typically have 80-93% similarity (12-14 matching digits out of 15)
        return similarity >= 0.80f && similarity <= 0.93f
    }
    
    /**
     * Validates IMEI format (exactly 15 digits)
     */
    fun isValidImeiFormat(imei: String?): Boolean {
        if (imei.isNullOrBlank()) return false
        return IMEI_PATTERN.matcher(cleanImei(imei)).matches()
    }
    
    /**
     * Gets validation error message for invalid IMEI
     */
    fun getValidationError(imei: String?): String? {
        if (imei.isNullOrBlank()) {
            return "IMEI cannot be empty"
        }
        
        // Check if original input contains non-digit characters
        if (!imei.all { it.isDigit() || it in setOf('-', ' ', '/') }) {
            return "IMEI must contain only numbers"
        }
        
        val cleanImei = cleanImei(imei)
        
        return when {
            cleanImei.length != 15 -> "IMEI must be exactly 15 digits"
            !validateLuhnChecksum(cleanImei) -> "Invalid IMEI checksum"
            else -> null
        }
    }
    
    /**
     * Validation result with detailed information
     */
    data class ValidationResult(
        val isValid: Boolean,
        val cleanImei: String?,
        val errorMessage: String?
    )
    
    /**
     * Comprehensive IMEI validation with detailed result
     */
    fun validateImei(imei: String?): ValidationResult {
        if (imei.isNullOrBlank()) {
            return ValidationResult(false, null, "IMEI cannot be empty")
        }
        
        val cleanImei = cleanImei(imei)
        val errorMessage = getValidationError(imei)
        
        return ValidationResult(
            isValid = errorMessage == null,
            cleanImei = if (errorMessage == null) cleanImei else null,
            errorMessage = errorMessage
        )
    }
}