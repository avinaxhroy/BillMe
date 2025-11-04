package com.billme.app.core.ocr.impl

import com.billme.app.core.ocr.TextProcessingPipeline
import com.billme.app.data.model.FieldCategory
import com.billme.app.data.model.FieldType
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Text Processing Pipeline for better OCR text cleanup
 */
@Singleton
class TextProcessingPipelineImpl @Inject constructor() : TextProcessingPipeline {
    
    companion object {
        // Common OCR mistakes and corrections
        private val OCR_CORRECTIONS = mapOf(
            "O" to "0", // Letter O to zero (in numeric contexts)
            "l" to "1", // Lowercase L to one (in numeric contexts)
            "I" to "1", // Uppercase I to one (in numeric contexts)
            "Z" to "2", // Z to 2 (in some contexts)
            "S" to "5", // S to 5 (in numeric contexts)
            "B" to "8", // B to 8 (in numeric contexts)
            "g" to "9", // g to 9 (in numeric contexts)
            "|" to "1", // Pipe to one
            "i" to "1"  // lowercase i to one (in numeric contexts)
        )
        
        // Brand name corrections
        private val BRAND_CORRECTIONS = mapOf(
            "redmi" to "Redmi",
            "realme" to "Realme",
            "samsung" to "Samsung",
            "vivo" to "Vivo",
            "oppo" to "Oppo",
            "oneplus" to "OnePlus",
            "iphone" to "iPhone",
            "poco" to "Poco",
            "motorola" to "Motorola",
            "nokia" to "Nokia",
            "iqoo" to "iQOO"
        )
        
        // Month abbreviations
        private val MONTH_NAMES = mapOf(
            "january" to "01", "jan" to "01",
            "february" to "02", "feb" to "02",
            "march" to "03", "mar" to "03",
            "april" to "04", "apr" to "04",
            "may" to "05",
            "june" to "06", "jun" to "06",
            "july" to "07", "jul" to "07",
            "august" to "08", "aug" to "08",
            "september" to "09", "sep" to "09", "sept" to "09",
            "october" to "10", "oct" to "10",
            "november" to "11", "nov" to "11",
            "december" to "12", "dec" to "12"
        )
    }
    
    override fun processField(value: String, fieldType: FieldType): String {
        var processed = value.trim()
        
        // Apply category-specific processing
        processed = when (fieldType.category) {
            FieldCategory.FINANCIAL -> processFinancialField(processed)
            FieldCategory.IDENTIFICATION -> processIdentificationField(processed, fieldType)
            FieldCategory.DATE -> processDateField(processed)
            FieldCategory.CONTACT -> processContactField(processed, fieldType)
            FieldCategory.ENTITY -> processEntityField(processed)
            FieldCategory.ITEM -> processItemField(processed, fieldType)
            else -> processed
        }
        
        return processed
    }
    
    private fun processFinancialField(value: String): String {
        var processed = value
        
        // Remove currency symbols
        processed = processed.replace(Regex("[₹\$€£¥]"), "")
        
        // Remove whitespace
        processed = processed.replace(Regex("\\s+"), "")
        
        // Fix OCR errors in numbers
        processed = fixNumericOCRErrors(processed)
        
        // Remove non-numeric characters except decimal point and comma
        processed = processed.replace(Regex("[^0-9.,]"), "")
        
        // Handle Indian number format (1,23,456.00)
        if (processed.contains(",")) {
            processed = processed.replace(",", "")
        }
        
        // Ensure valid decimal format
        if (processed.contains(".")) {
            val parts = processed.split(".")
            if (parts.size == 2) {
                processed = "${parts[0]}.${parts[1].take(2)}"
            }
        }
        
        return processed
    }
    
    private fun processIdentificationField(value: String, fieldType: FieldType): String {
        var processed = value.trim()
        
        when (fieldType) {
            FieldType.GST_NUMBER -> {
                // GST format: 22AAAAA0000A1Z5
                processed = processed.replace(Regex("\\s+"), "")
                processed = processed.uppercase()
                
                // Fix common OCR errors in GST
                if (processed.length >= 15) {
                    // First 2 chars should be digits (state code)
                    processed = processed.substring(0, 2).map { 
                        if (it.isLetter()) OCR_CORRECTIONS[it.toString()] ?: it else it
                    }.joinToString("") + processed.substring(2)
                }
            }
            
            FieldType.INVOICE_NUMBER, FieldType.RECEIPT_NUMBER -> {
                processed = processed.uppercase()
                processed = processed.replace(Regex("\\s+"), "")
            }
            
            else -> {
                processed = processed.uppercase()
            }
        }
        
        return processed
    }
    
    private fun processDateField(value: String): String {
        var processed = value.trim()
        
        // Try to standardize date format
        // Handle formats: DD-MM-YYYY, DD/MM/YYYY, DD-MMM-YY, etc.
        
        // Replace month names with numbers
        for ((monthName, monthNum) in MONTH_NAMES) {
            if (processed.contains(monthName, ignoreCase = true)) {
                processed = processed.replace(monthName, monthNum, ignoreCase = true)
                break
            }
        }
        
        // Standardize separators to /
        processed = processed.replace("-", "/")
        
        // Handle 2-digit years
        val yearPattern = Pattern.compile("(\\d{1,2}/\\d{1,2}/)(\\d{2})$")
        val matcher = yearPattern.matcher(processed)
        if (matcher.find()) {
            val year = matcher.group(2).toIntOrNull() ?: 0
            val fullYear = if (year < 50) "20$year" else "19$year"
            processed = matcher.replaceFirst("${matcher.group(1)}$fullYear")
        }
        
        // Ensure DD/MM/YYYY format with zero padding
        val parts = processed.split("/")
        if (parts.size == 3) {
            val day = parts[0].padStart(2, '0')
            val month = parts[1].padStart(2, '0')
            val year = parts[2]
            processed = "$day/$month/$year"
        }
        
        return processed
    }
    
    private fun processContactField(value: String, fieldType: FieldType): String {
        var processed = value.trim()
        
        when (fieldType) {
            FieldType.PHONE_NUMBER -> {
                // Remove all non-numeric characters except +
                processed = processed.replace(Regex("[^0-9+]"), "")
                
                // Fix OCR errors in phone numbers
                processed = fixNumericOCRErrors(processed)
                
                // Remove leading +91 if present
                if (processed.startsWith("+91")) {
                    processed = processed.substring(3)
                } else if (processed.startsWith("91") && processed.length == 12) {
                    processed = processed.substring(2)
                }
                
                // Ensure 10 digits
                if (processed.length > 10) {
                    processed = processed.takeLast(10)
                }
            }
            
            FieldType.EMAIL -> {
                processed = processed.lowercase()
                processed = processed.replace(Regex("\\s+"), "")
                
                // Fix common OCR errors in email
                processed = processed.replace(Regex("[,;]"), ".")
            }
            
            else -> {}
        }
        
        return processed
    }
    
    private fun processEntityField(value: String): String {
        var processed = value.trim()
        
        // Capitalize each word
        processed = processed.split(Regex("\\s+"))
            .joinToString(" ") { word ->
                if (word.length > 1) {
                    word[0].uppercaseChar() + word.substring(1).lowercase()
                } else {
                    word.uppercase()
                }
            }
        
        // Fix brand names
        for ((incorrect, correct) in BRAND_CORRECTIONS) {
            if (processed.contains(incorrect, ignoreCase = true)) {
                processed = processed.replace(incorrect, correct, ignoreCase = true)
            }
        }
        
        return processed
    }
    
    private fun processItemField(value: String, fieldType: FieldType): String {
        var processed = value.trim()
        
        when (fieldType) {
            FieldType.ITEM_DESCRIPTION -> {
                // Clean up product descriptions
                processed = processed.replace(Regex("\\s+"), " ")
                
                // Fix brand names
                for ((incorrect, correct) in BRAND_CORRECTIONS) {
                    if (processed.contains(incorrect, ignoreCase = true)) {
                        processed = processed.replace(incorrect, correct, ignoreCase = true)
                        break
                    }
                }
                
                // Standardize storage/RAM notation
                processed = processed.replace(Regex("(\\d+)\\s*gb", RegexOption.IGNORE_CASE), "$1GB")
                processed = processed.replace(Regex("(\\d+)\\s*pg", RegexOption.IGNORE_CASE), "$1GB") // OCR error pg->gb
                processed = processed.replace(Regex("(\\d+)\\s*mg", RegexOption.IGNORE_CASE), "$1GB") // OCR error mg->gb
                
                // Standardize 5G/4G notation
                processed = processed.replace(Regex("\\s*5g\\s*", RegexOption.IGNORE_CASE), " 5G ")
                processed = processed.replace(Regex("\\s*4g\\s*", RegexOption.IGNORE_CASE), " 4G ")
                
                // Remove extra spaces
                processed = processed.replace(Regex("\\s+"), " ").trim()
            }
            
            FieldType.ITEM_QUANTITY, FieldType.ITEM_RATE, FieldType.ITEM_AMOUNT -> {
                processed = processFinancialField(processed)
            }
            
            else -> {}
        }
        
        return processed
    }
    
    /**
     * Fix common OCR errors in numeric strings
     */
    private fun fixNumericOCRErrors(value: String): String {
        var fixed = value
        
        // Only apply numeric corrections if it looks like a number
        if (value.matches(Regex("[0-9OlISZBg|i.,]+", RegexOption.IGNORE_CASE))) {
            for ((error, correction) in OCR_CORRECTIONS) {
                fixed = fixed.replace(error, correction)
            }
        }
        
        return fixed
    }
    
    /**
     * Clean and normalize text by removing extra whitespace and special characters
     */
    fun cleanText(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^\\x20-\\x7E]"), "") // Remove non-printable ASCII
            .trim()
    }
    
    /**
     * Extract numbers from text
     */
    fun extractNumbers(text: String): List<String> {
        val pattern = Pattern.compile("\\d+(?:[.,]\\d+)?")
        val matcher = pattern.matcher(text)
        val numbers = mutableListOf<String>()
        
        while (matcher.find()) {
            numbers.add(matcher.group())
        }
        
        return numbers
    }
    
    /**
     * Check if a string looks like a valid Indian mobile number
     */
    fun isValidIndianMobile(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[^0-9]"), "")
        
        // Indian mobile numbers are 10 digits starting with 6-9
        return cleaned.length == 10 && cleaned[0] in '6'..'9'
    }
    
    /**
     * Check if a string looks like a valid GST number
     */
    fun isValidGST(gst: String): Boolean {
        val pattern = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        return pattern.matcher(gst).matches()
    }
    
    /**
     * Check if a string looks like a valid amount
     */
    fun isValidAmount(amount: String): Boolean {
        val cleaned = amount.replace(",", "")
        return cleaned.toDoubleOrNull() != null
    }
}
