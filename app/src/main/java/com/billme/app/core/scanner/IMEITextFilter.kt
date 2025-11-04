package com.billme.app.core.scanner

import com.billme.app.core.util.ImeiValidator

/**
 * Enhanced IMEI Text Filter - Advanced accuracy improvements
 * Filters false positives and validates IMEI quality
 */
object IMEITextFilter {
    
    /**
     * Check if IMEI has sufficient digit variety
     * Real IMEIs have at least 5 different digits
     */
    fun hasSufficientVariety(imei: String): Boolean {
        return imei.toSet().size >= IMEIScannerConfig.MIN_DIGIT_VARIETY
    }
    
    /**
     * Check if IMEI has repeating patterns
     * Patterns like 555555555555555 or 123451234512345 are invalid
     */
    fun hasRepeatingPattern(imei: String): Boolean {
        if (imei.length != 15) return false
        
        // Check for 3-digit repeating patterns
        val chunk3 = imei.chunked(3)
        if (chunk3.distinct().size == 1) return true
        
        // Check for 5-digit repeating patterns
        val chunk5 = imei.chunked(5)
        if (chunk5.distinct().size == 1) return true
        
        return false
    }
    
    /**
     * Check if all digits are the same
     */
    fun isAllSameDigit(imei: String): Boolean {
        return imei.toSet().size == 1
    }
    
    /**
     * Check if IMEI is a sequential pattern
     */
    fun isSequentialPattern(imei: String): Boolean {
        if (imei.length != 15) return false
        
        // Check ascending sequence (e.g., 012345678901234)
        var ascendingCount = 0
        var descendingCount = 0
        
        for (i in 0 until imei.length - 1) {
            val current = imei[i].digitToInt()
            val next = imei[i + 1].digitToInt()
            
            if (next == (current + 1) % 10) ascendingCount++
            if (next == (current + 9) % 10) descendingCount++
        }
        
        // If more than 70% is sequential, it's likely a pattern
        return ascendingCount >= 10 || descendingCount >= 10
    }
    
    /**
     * Calculate digit difference between two IMEIs
     * Used to verify dual IMEI similarity
     */
    fun calculateDigitDifference(imei1: String, imei2: String): Int {
        if (imei1.length != 15 || imei2.length != 15) return 15
        return imei1.zip(imei2).count { (a, b) -> a != b }
    }
    
    /**
     * Check if two IMEIs are likely from the same device
     * Dual IMEIs typically differ by 1-8 digits
     */
    fun areLikelyDualIMEIs(imei1: String, imei2: String): Boolean {
        val difference = calculateDigitDifference(imei1, imei2)
        return difference in 1..IMEIScannerConfig.MAX_DUAL_IMEI_DIFFERENCES
    }
    
    /**
     * Validate IMEI quality (format, variety, patterns)
     */
    fun isHighQualityIMEI(imei: String): Boolean {
        if (imei.length != 15) return false
        if (!ImeiValidator.isValidImeiFormat(imei)) return false
        if (ImeiValidator.isKnownInvalidPattern(imei)) return false
        if (!hasSufficientVariety(imei)) return false
        if (hasRepeatingPattern(imei)) return false
        if (isSequentialPattern(imei)) return false
        if (isAllSameDigit(imei)) return false
        
        return true
    }
    
    /**
     * Extract context around IMEI in text
     * Helps verify if it's actually an IMEI label
     */
    fun extractIMEIContext(text: String, imei: String): String {
        val index = text.indexOf(imei)
        if (index == -1) return ""
        
        val start = maxOf(0, index - 30)
        val end = minOf(text.length, index + imei.length + 30)
        return text.substring(start, end)
    }
    
    /**
     * Check if context suggests this is an IMEI
     */
    fun hasIMEIContext(context: String): Boolean {
        val keywords = listOf(
            "IMEI", "imei", "Imei", 
            "IMEI1", "IMEI2", "IMEI 1", "IMEI 2",
            "Device ID", "Serial", "SN"
        )
        return keywords.any { context.contains(it, ignoreCase = false) }
    }
    
    /**
     * Check for negative context that suggests this is NOT an IMEI
     */
    fun hasNegativeContext(context: String): Boolean {
        val antiKeywords = listOf(
            "invoice", "bill", "receipt", "total", "amount",
            "price", "GST", "GSTIN", "IRN", "ACK"
        )
        return antiKeywords.any { context.contains(it, ignoreCase = true) }
    }
    
    /**
     * Score IMEI detection confidence (0-100)
     */
    fun calculateConfidenceScore(
        imei: String,
        context: String,
        detectionCount: Int
    ): Int {
        var score = 0
        
        // Base score for valid Luhn checksum
        if (ImeiValidator.isValidImei(imei)) score += 35
        
        // Digit variety bonus
        if (hasSufficientVariety(imei)) score += 15
        
        // No repeating pattern bonus
        if (!hasRepeatingPattern(imei)) score += 10
        
        // No sequential pattern bonus
        if (!isSequentialPattern(imei)) score += 10
        
        // Context keywords bonus
        if (hasIMEIContext(context)) score += 20
        
        // Negative context penalty
        if (hasNegativeContext(context)) score -= 30
        
        // Multiple detections bonus
        score += minOf(detectionCount * 5, 15)
        
        // Known invalid pattern penalty
        if (ImeiValidator.isKnownInvalidPattern(imei)) score -= 50
        
        return minOf(maxOf(score, 0), 100)
    }
    
    /**
     * Filter list of IMEIs keeping only high quality ones
     */
    fun filterHighQualityIMEIs(imeis: List<String>): List<String> {
        return imeis.filter { isHighQualityIMEI(it) }
            .distinctBy { it } // Remove exact duplicates
    }
    
    /**
     * Rank IMEIs by quality and return best ones
     */
    fun rankIMEIs(imeis: List<String>, text: String): List<Pair<String, Int>> {
        return imeis.map { imei ->
            val context = extractIMEIContext(text, imei)
            val score = calculateConfidenceScore(imei, context, 1)
            imei to score
        }.sortedByDescending { it.second }
            .filter { it.second >= 50 } // Only return IMEIs with at least 50% confidence
    }
    
    /**
     * Common false positive patterns to exclude
     */
    private val FALSE_POSITIVE_PATTERNS = listOf(
        Regex("^0{15}$"),           // All zeros
        Regex("^1{15}$"),           // All ones
        Regex("^(\\d)\\1{14}$"),    // Any digit repeated 15 times
        Regex("^123456789012345$"), // Sequential pattern
        Regex("^987654321098765$"), // Reverse sequential
        Regex("^999999999999999$"),  // All nines
        Regex("^00\\d{13}$")        // Invalid TAC starting with 00
    )
    
    /**
     * Check if IMEI matches known false positive patterns
     */
    fun isFalsePositive(imei: String): Boolean {
        return FALSE_POSITIVE_PATTERNS.any { it.matches(imei) }
    }
    
    /**
     * Clean and validate IMEI with all checks
     */
    fun validateAndClean(imei: String): String? {
        // Remove any whitespace
        val cleaned = imei.replace("\\s".toRegex(), "")
        
        // Must be exactly 15 digits
        if (cleaned.length != 15 || !cleaned.all { it.isDigit() }) {
            return null
        }
        
        // Check quality
        if (!isHighQualityIMEI(cleaned)) {
            return null
        }
        
        // Check false positives
        if (isFalsePositive(cleaned)) {
            return null
        }
        
        // Final Luhn validation with pattern check
        if (!ImeiValidator.isValidImeiWithPatternCheck(cleaned)) {
            return null
        }
        
        return cleaned
    }
    
    /**
     * Validate dual IMEI pair
     * Returns true if both are valid and likely from same device
     */
    fun validateDualIMEIPair(imei1: String, imei2: String): Boolean {
        if (imei1 == imei2) return false // Same IMEI is not valid dual
        
        val cleaned1 = validateAndClean(imei1) ?: return false
        val cleaned2 = validateAndClean(imei2) ?: return false
        
        return ImeiValidator.areLikelyDualIMEIs(cleaned1, cleaned2)
    }
}
