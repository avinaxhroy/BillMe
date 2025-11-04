package com.billme.app.core.ocr.impl

import com.billme.app.data.model.ExtractedField
import com.billme.app.data.model.FieldType
import com.billme.app.data.model.FieldValidationResult
import com.billme.app.data.model.ValidationType
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart IMEI Detector - Only detects valid IMEI numbers, not random numbers
 * 
 * IMEI (International Mobile Equipment Identity) characteristics:
 * - Exactly 15 digits
 * - No letters or special characters
 * - Must pass Luhn algorithm check
 * - Should not be a sequential number (111111111111111)
 * - Should not be all zeros or all nines
 * - Usually found in specific contexts (near "IMEI" label, on product boxes, etc.)
 */
@Singleton
class IMEIDetector @Inject constructor() {
    
    companion object {
        // Pattern for potential IMEI numbers (15 consecutive digits)
        private val IMEI_PATTERN = Pattern.compile("\\b(\\d{15})\\b")
        
        // Context keywords that suggest nearby text is an IMEI
        private val IMEI_CONTEXT_KEYWORDS = setOf(
            "imei", "imei1", "imei2", "imei no", "imei number",
            "serial", "serial no", "serial number",
            "device id", "equipment id"
        )
        
        // Keywords that suggest the number is NOT an IMEI
        private val ANTI_CONTEXT_KEYWORDS = setOf(
            "irn", "invoice", "bill", "receipt", "gstin", "gst",
            "amount", "total", "price", "rate", "ack no", "acknowledgement",
            "reference", "po number", "order"
        )
    }
    
    /**
     * Detect IMEI numbers from text with high confidence
     * Returns only numbers that are likely to be actual IMEIs
     */
    fun detectIMEIs(
        text: String,
        contextWindow: Int = 50
    ): List<IMEICandidate> {
        val candidates = mutableListOf<IMEICandidate>()
        val matcher = IMEI_PATTERN.matcher(text)
        
        while (matcher.find()) {
            val potentialIMEI = matcher.group(1)
            val position = matcher.start()
            
            // Get surrounding context
            val contextStart = maxOf(0, position - contextWindow)
            val contextEnd = minOf(text.length, position + potentialIMEI.length + contextWindow)
            val context = text.substring(contextStart, contextEnd).lowercase()
            
            // Validate this is likely an IMEI
            val validation = validateIMEI(potentialIMEI, context)
            
            if (validation.isValid && validation.confidence >= 0.5f) {
                candidates.add(
                    IMEICandidate(
                        imei = potentialIMEI,
                        confidence = validation.confidence,
                        context = context,
                        position = position,
                        validationResult = validation
                    )
                )
            }
        }
        
        // Sort by confidence and position
        return candidates.sortedWith(
            compareByDescending<IMEICandidate> { it.confidence }
                .thenBy { it.position }
        )
    }
    
    /**
     * Detect IMEI with extracted field format
     */
    fun detectIMEIFields(text: String): Map<String, ExtractedField> {
        val fields = mutableMapOf<String, ExtractedField>()
        val imeiCandidates = detectIMEIs(text)
        
        // Only include high-confidence IMEIs
        val validIMEIs = imeiCandidates.filter { it.confidence >= 0.7f }
        
        if (validIMEIs.isNotEmpty()) {
            // First IMEI
            val imei1 = validIMEIs[0]
            fields["imei1"] = ExtractedField(
                fieldType = FieldType.UNKNOWN, // You can create IMEI field type
                rawValue = imei1.imei,
                processedValue = imei1.imei,
                confidence = imei1.confidence,
                boundingBox = null,
                sourceTextBlocks = emptyList(),
                validationResult = imei1.validationResult
            )
            
            // Second IMEI (if exists and different from first)
            if (validIMEIs.size > 1) {
                val imei2 = validIMEIs[1]
                if (imei2.imei != imei1.imei) {
                    fields["imei2"] = ExtractedField(
                        fieldType = FieldType.UNKNOWN,
                        rawValue = imei2.imei,
                        processedValue = imei2.imei,
                        confidence = imei2.confidence,
                        boundingBox = null,
                        sourceTextBlocks = emptyList(),
                        validationResult = imei2.validationResult
                    )
                }
            }
        }
        
        return fields
    }
    
    /**
     * Validate if a 15-digit number is likely an IMEI
     */
    private fun validateIMEI(number: String, context: String): FieldValidationResult {
        var confidence = 0.0f
        val issues = mutableListOf<String>()
        
        // Basic format check
        if (number.length != 15) {
            return FieldValidationResult(
                isValid = false,
                validationType = ValidationType.FORMAT_VALIDATION,
                errorMessage = "IMEI must be exactly 15 digits",
                confidence = 0.0f
            )
        }
        
        // Check for invalid patterns
        
        // 1. All same digits
        if (number.all { it == number[0] }) {
            issues.add("All digits are same")
            confidence -= 0.5f
        } else {
            confidence += 0.2f
        }
        
        // 2. Sequential digits (enhanced check)
        if (isSequential(number)) {
            issues.add("Sequential digits detected")
            confidence -= 0.4f
        } else {
            confidence += 0.2f
        }
        
        // 3. Repeating patterns (e.g., 123451234512345)
        if (hasRepeatingPattern(number)) {
            issues.add("Repeating pattern detected")
            confidence -= 0.3f
        } else {
            confidence += 0.15f
        }
        
        // 4. Check Luhn algorithm (checksum validation)
        if (luhnCheck(number)) {
            confidence += 0.3f
        } else {
            issues.add("Failed Luhn checksum")
            confidence -= 0.3f
        }
        
        // 5. Context analysis (critical for accuracy)
        val contextScore = analyzeContext(context)
        confidence += contextScore
        
        if (contextScore > 0.2f) {
            // Strong positive IMEI context found
            confidence += 0.15f
        } else if (contextScore < -0.1f) {
            // Negative context found (this is likely not an IMEI)
            issues.add("Context suggests this is not an IMEI")
            confidence -= 0.2f
        }
        
        // 6. Check first 8 digits (TAC - Type Allocation Code)
        // Valid TACs don't start with 00
        if (number.startsWith("00")) {
            issues.add("Invalid TAC (Type Allocation Code)")
            confidence -= 0.2f
        } else {
            confidence += 0.1f
        }
        
        // 7. Check digit variety (at least 5 different digits)
        val digitVariety = number.toSet().size
        if (digitVariety < 5) {
            issues.add("Insufficient digit variety")
            confidence -= 0.2f
        } else {
            confidence += 0.1f
        }
        
        // Final confidence clamping
        confidence = confidence.coerceIn(0.0f, 1.0f)
        
        // Higher threshold for validation (70% confidence required)
        val isValid = confidence >= 0.7f && issues.isEmpty()
        
        return FieldValidationResult(
            isValid = isValid,
            validationType = ValidationType.LUHN_ALGORITHM,
            errorMessage = if (issues.isNotEmpty()) issues.joinToString(", ") else null,
            confidence = confidence
        )
    }
    
    /**
     * Check for repeating patterns in IMEI
     */
    private fun hasRepeatingPattern(number: String): Boolean {
        if (number.length != 15) return false
        
        // Check for 3-digit repeating patterns
        val chunks3 = number.chunked(3)
        if (chunks3.distinct().size <= 2) return true
        
        // Check for 5-digit repeating patterns
        val chunks5 = number.chunked(5)
        if (chunks5.distinct().size == 1) return true
        
        return false
    }
    
    /**
     * Luhn algorithm check for IMEI validation
     */
    private fun luhnCheck(number: String): Boolean {
        var sum = 0
        var alternate = false
        
        // Process digits from right to left
        for (i in number.length - 1 downTo 0) {
            var digit = number[i].toString().toInt()
            
            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit % 10) + 1
                }
            }
            
            sum += digit
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
    
    /**
     * Check if number is sequential (e.g., 123456789012345 or 987654321098765)
     */
    private fun isSequential(number: String): Boolean {
        var ascending = 0
        var descending = 0
        
        for (i in 0 until number.length - 1) {
            val current = number[i].toString().toInt()
            val next = number[i + 1].toString().toInt()
            
            if (next == current + 1 || (current == 9 && next == 0)) ascending++
            if (next == current - 1 || (current == 0 && next == 9)) descending++
        }
        
        // If more than 60% of transitions are sequential
        return ascending > number.length * 0.6 || descending > number.length * 0.6
    }
    
    /**
     * Analyze context to determine if number is likely an IMEI
     * Returns: positive score if IMEI context found, negative if anti-context found
     */
    private fun analyzeContext(context: String): Float {
        val lowerContext = context.lowercase()
        
        // Check for positive IMEI context
        var positiveScore = 0f
        for (keyword in IMEI_CONTEXT_KEYWORDS) {
            if (lowerContext.contains(keyword)) {
                // Exact match gets higher score
                positiveScore += if (keyword.length > 4) 0.35f else 0.25f
            }
        }
        
        // Check for negative context (this is NOT an IMEI)
        var negativeScore = 0f
        for (keyword in ANTI_CONTEXT_KEYWORDS) {
            if (lowerContext.contains(keyword)) {
                negativeScore += 0.25f
            }
        }
        
        // Return net score (can be negative if anti-context dominates)
        return positiveScore - negativeScore
    }
    
    /**
     * Check if text has exactly one potential IMEI
     * Used to determine if IMEI2 field should be optional
     */
    fun hasSingleIMEI(text: String): Boolean {
        val candidates = detectIMEIs(text)
        return candidates.filter { it.confidence >= 0.7f }.size == 1
    }
    
    /**
     * Check if text has multiple IMEIs
     */
    fun hasMultipleIMEIs(text: String): Boolean {
        val candidates = detectIMEIs(text)
        return candidates.filter { it.confidence >= 0.7f }.size > 1
    }
    
    /**
     * Get IMEI count suggestion for UI
     */
    fun suggestIMEIFieldCount(text: String): IMEIFieldSuggestion {
        val validCandidates = detectIMEIs(text).filter { it.confidence >= 0.7f }
        
        return when (validCandidates.size) {
            0 -> IMEIFieldSuggestion.NONE
            1 -> IMEIFieldSuggestion.SINGLE
            2 -> IMEIFieldSuggestion.DUAL
            else -> IMEIFieldSuggestion.MULTIPLE
        }
    }
}

/**
 * IMEI candidate with validation info
 */
data class IMEICandidate(
    val imei: String,
    val confidence: Float,
    val context: String,
    val position: Int,
    val validationResult: FieldValidationResult
)

/**
 * Suggestion for how many IMEI fields to show
 */
enum class IMEIFieldSuggestion {
    NONE,       // No IMEI detected, require manual entry
    SINGLE,     // One IMEI detected, show only IMEI1 field
    DUAL,       // Two IMEIs detected, show IMEI1 and IMEI2
    MULTIPLE    // More than 2 IMEIs, show multi-IMEI interface
}
