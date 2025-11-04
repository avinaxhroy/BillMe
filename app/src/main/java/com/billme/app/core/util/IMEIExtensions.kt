package com.billme.app.core.util

import com.billme.app.data.local.entity.Product
import com.billme.app.hardware.IMEIScanResult
import java.util.regex.Pattern

/**
 * IMEI Extensions and Utilities
 * Comprehensive helper functions for IMEI processing
 */

/**
 * Extension functions for String IMEI processing
 */
fun String.isValidIMEI(): Boolean = ImeiValidator.isValidImei(this)

fun String.cleanIMEI(): String = ImeiValidator.cleanImei(this)

fun String.formatIMEI(): String? = ImeiValidator.formatImei(this)

fun String.validateIMEIWithDetails() = ImeiValidator.validateImei(this)

/**
 * Extract all possible IMEIs from a text string
 */
fun String.extractAllIMEIs(): List<String> {
    val pattern = Pattern.compile("\\b\\d{15}\\b")
    val matcher = pattern.matcher(this)
    val imeis = mutableListOf<String>()
    
    while (matcher.find()) {
        val imei = matcher.group()
        if (imei.isValidIMEI()) {
            imeis.add(imei)
        }
    }
    
    return imeis.distinct()
}

/**
 * Check if string contains any valid IMEI
 */
fun String.containsIMEI(): Boolean = this.extractAllIMEIs().isNotEmpty()

/**
 * Get IMEI manufacturer info (TAC - Type Allocation Code)
 */
fun String.getIMEIManufacturerInfo(): IMEIManufacturerInfo? {
    if (!this.isValidIMEI()) return null
    
    val cleanImei = this.cleanIMEI()
    val tac = cleanImei.substring(0, 8)
    
    // This would typically connect to a TAC database
    // For demo purposes, we'll return basic info
    return IMEIManufacturerInfo(
        tac = tac,
        manufacturer = getManufacturerFromTAC(tac),
        model = getModelFromTAC(tac),
        countryCode = tac.substring(0, 2)
    )
}

/**
 * IMEI comparison utilities
 */
fun String.isSimilarIMEI(other: String, tolerance: Int = 2): Boolean {
    val clean1 = this.cleanIMEI()
    val clean2 = other.cleanIMEI()
    
    if (clean1.length != 15 || clean2.length != 15) return false
    
    var differences = 0
    for (i in clean1.indices) {
        if (clean1[i] != clean2[i]) {
            differences++
            if (differences > tolerance) return false
        }
    }
    
    return differences <= tolerance
}

/**
 * Check if IMEI belongs to same device series
 */
fun String.isSameDeviceSeries(other: String): Boolean {
    val clean1 = this.cleanIMEI()
    val clean2 = other.cleanIMEI()
    
    if (clean1.length != 15 || clean2.length != 15) return false
    
    // Same TAC (first 8 digits) indicates same device model
    return clean1.substring(0, 8) == clean2.substring(0, 8)
}

/**
 * Product extension functions for IMEI handling
 */
fun Product.getAllIMEIs(): List<String> {
    return listOfNotNull(imei1, imei2).filter { it.isNotBlank() }
}

fun Product.hasIMEI(imei: String): Boolean {
    val cleanTarget = imei.cleanIMEI()
    return getAllIMEIs().any { it.cleanIMEI() == cleanTarget }
}

fun Product.getFormattedIMEIs(): List<String> {
    return getAllIMEIs().mapNotNull { it.formatIMEI() }
}

fun Product.hasValidIMEIs(): Boolean {
    return getAllIMEIs().all { it.isValidIMEI() }
}

fun Product.getIMEIManufacturerInfo(): List<IMEIManufacturerInfo> {
    return getAllIMEIs().mapNotNull { it.getIMEIManufacturerInfo() }
}

/**
 * Collection extensions for IMEI processing
 */
fun List<Product>.findByIMEI(imei: String): Product? {
    val cleanImei = imei.cleanIMEI()
    return this.find { product ->
        product.getAllIMEIs().any { it.cleanIMEI() == cleanImei }
    }
}

fun List<Product>.findSimilarIMEIs(imei: String, tolerance: Int = 2): List<Product> {
    return this.filter { product ->
        product.getAllIMEIs().any { productImei ->
            productImei.isSimilarIMEI(imei, tolerance)
        }
    }
}

fun List<Product>.findSameDeviceSeries(imei: String): List<Product> {
    return this.filter { product ->
        product.getAllIMEIs().any { productImei ->
            productImei.isSameDeviceSeries(imei)
        }
    }
}

fun List<Product>.groupBySameSeries(): Map<String, List<Product>> {
    return this.filter { it.getAllIMEIs().isNotEmpty() }
        .groupBy { product ->
            product.getAllIMEIs().first().cleanIMEI().substring(0, 8)
        }
}

fun List<String>.removeDuplicateIMEIs(): List<String> {
    return this.map { it.cleanIMEI() }.distinct()
}

fun List<String>.validateAllIMEIs(): Map<String, Boolean> {
    return this.associateWith { it.isValidIMEI() }
}

/**
 * IMEI generation utilities (for testing)
 */
object IMEIGenerator {
    
    /**
     * Generate a valid IMEI for testing purposes
     * WARNING: Only use for testing/demo purposes
     */
    fun generateValidIMEI(tac: String = "01234567"): String {
        require(tac.length == 8 && tac.all { it.isDigit() }) { 
            "TAC must be 8 digits" 
        }
        
        val serial = (100000..999999).random().toString()
        val partial = tac + serial
        val checkDigit = ImeiValidator.calculateLuhnCheckDigit(partial)
        
        return partial + checkDigit
    }
    
    /**
     * Generate dual IMEIs for same device
     */
    fun generateDualIMEIs(tac: String = "01234567"): Pair<String, String> {
        val serial1 = (100000..999999).random()
        val serial2 = serial1 + 1
        
        val imei1 = tac + serial1.toString()
        val imei2 = tac + serial2.toString()
        
        val checkDigit1 = ImeiValidator.calculateLuhnCheckDigit(imei1)
        val checkDigit2 = ImeiValidator.calculateLuhnCheckDigit(imei2)
        
        return Pair(imei1 + checkDigit1, imei2 + checkDigit2)
    }
}

/**
 * IMEI analysis utilities
 */
object IMEIAnalyzer {
    
    /**
     * Analyze IMEI patterns in a list of products
     */
    fun analyzeIMEIPatterns(products: List<Product>): IMEIPatternAnalysis {
        val allImeis = products.flatMap { it.getAllIMEIs() }
        val validImeis = allImeis.filter { it.isValidIMEI() }
        
        val tacGroups = validImeis.groupBy { it.cleanIMEI().substring(0, 8) }
        val manufacturers = validImeis.mapNotNull { it.getIMEIManufacturerInfo()?.manufacturer }.distinct()
        
        val duplicateImeis = allImeis.groupBy { it.cleanIMEI() }
            .filter { it.value.size > 1 }
            .keys.toList()
        
        val sequentialPairs = findSequentialIMEIPairs(validImeis)
        
        return IMEIPatternAnalysis(
            totalIMEIs = allImeis.size,
            validIMEIs = validImeis.size,
            uniqueTACs = tacGroups.size,
            manufacturers = manufacturers,
            duplicateIMEIs = duplicateImeis,
            sequentialPairs = sequentialPairs,
            tacDistribution = tacGroups.mapValues { it.value.size }
        )
    }
    
    private fun findSequentialIMEIPairs(imeis: List<String>): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        val cleanImeis = imeis.map { it.cleanIMEI() }.sorted()
        
        for (i in 0 until cleanImeis.size - 1) {
            val current = cleanImeis[i].toLongOrNull() ?: continue
            val next = cleanImeis[i + 1].toLongOrNull() ?: continue
            
            if (next - current == 1L) {
                pairs.add(Pair(cleanImeis[i], cleanImeis[i + 1]))
            }
        }
        
        return pairs
    }
    
    /**
     * Detect suspicious IMEI patterns
     */
    fun detectSuspiciousPatterns(imeis: List<String>): List<IMEISuspiciousPattern> {
        val patterns = mutableListOf<IMEISuspiciousPattern>()
        val cleanImeis = imeis.map { it.cleanIMEI() }
        
        // Check for repeated digits
        cleanImeis.forEach { imei ->
            if (hasRepeatedPattern(imei)) {
                patterns.add(
                    IMEISuspiciousPattern(
                        imei = imei,
                        patternType = SuspiciousPatternType.REPEATED_DIGITS,
                        description = "Contains repeated digit patterns"
                    )
                )
            }
        }
        
        // Check for sequential numbers
        cleanImeis.forEach { imei ->
            if (hasSequentialPattern(imei)) {
                patterns.add(
                    IMEISuspiciousPattern(
                        imei = imei,
                        patternType = SuspiciousPatternType.SEQUENTIAL_DIGITS,
                        description = "Contains sequential digit patterns"
                    )
                )
            }
        }
        
        return patterns
    }
    
    private fun hasRepeatedPattern(imei: String): Boolean {
        // Check for 4+ consecutive same digits
        return imei.windowed(4).any { it.all { char -> char == it[0] } }
    }
    
    private fun hasSequentialPattern(imei: String): Boolean {
        // Check for 4+ consecutive sequential digits
        return imei.windowed(4).any { window ->
            window.zipWithNext().all { (a, b) -> b.digitToInt() - a.digitToInt() == 1 }
        }
    }
}

// Helper function to add to ImeiValidator
fun ImeiValidator.calculateLuhnCheckDigit(partial: String): String {
    require(partial.length == 14) { "Partial IMEI must be 14 digits" }
    
    val digits = partial.map { it.digitToInt() }
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
    return checkDigit.toString()
}

/**
 * Data classes for IMEI utilities
 */
data class IMEIManufacturerInfo(
    val tac: String,
    val manufacturer: String?,
    val model: String?,
    val countryCode: String
)

data class IMEIPatternAnalysis(
    val totalIMEIs: Int,
    val validIMEIs: Int,
    val uniqueTACs: Int,
    val manufacturers: List<String>,
    val duplicateIMEIs: List<String>,
    val sequentialPairs: List<Pair<String, String>>,
    val tacDistribution: Map<String, Int>
)

data class IMEISuspiciousPattern(
    val imei: String,
    val patternType: SuspiciousPatternType,
    val description: String
)

enum class SuspiciousPatternType {
    REPEATED_DIGITS,
    SEQUENTIAL_DIGITS,
    COMMON_TEST_IMEI,
    INVALID_TAC
}

/**
 * Mock TAC database (in real app, this would be a proper database)
 */
private fun getManufacturerFromTAC(tac: String): String? {
    val tacMap = mapOf(
        "35000000" to "Apple",
        "35100000" to "Apple", 
        "35200000" to "Apple",
        "86000000" to "Samsung",
        "35700000" to "Samsung",
        "35300000" to "Nokia",
        "35400000" to "Huawei",
        "35500000" to "Xiaomi",
        "35600000" to "OnePlus"
    )
    
    return tacMap[tac.substring(0, 8)] ?: "Unknown"
}

private fun getModelFromTAC(tac: String): String? {
    // This would be a comprehensive TAC to model mapping
    return when (tac.substring(0, 6)) {
        "350000" -> "iPhone Series"
        "860000" -> "Galaxy Series"
        "353000" -> "Nokia Series"
        else -> "Unknown Model"
    }
}