package com.billme.app.ui.screen.addproduct

/**
 * Extracted product data from OCR for multi-product invoices
 */
data class ExtractedProductData(
    val serialNumber: Int,
    val productName: String,
    val brand: String,
    val model: String,
    val color: String?,
    val variant: String?,
    val imei: String?,
    val costPrice: Double,
    val mrp: Double,
    val quantity: Double
)

/**
 * Parse variant text to extract color and technical specifications
 * Example: "Crimson Art 8GB 256GB" -> color="Crimson Art", variant="8GB/256GB"
 */
data class ParsedVariant(
    val color: String?,
    val variant: String?
)

/**
 * Parse variant string to separate color from technical specs
 */
fun parseVariantString(variantText: String): ParsedVariant {
    if (variantText.isBlank()) {
        return ParsedVariant(null, null)
    }
    
    // Common color patterns (can be 1-3 words)
    val colorPatterns = listOf(
        // Two-word colors
        "Crimson Art", "Phantom Purple", "Ivy Green", "Midnight Black", "Pearl White",
        "Glacier Blue", "Arctic Silver", "Cosmic Gray", "Ocean Blue", "Sunset Gold",
        "Forest Green", "Rose Gold", "Space Gray", "Starlight Silver", "Aurora Green",
        
        // Single-word colors
        "Black", "White", "Blue", "Red", "Green", "Gold", "Silver", "Gray", "Grey",
        "Purple", "Pink", "Orange", "Yellow", "Brown", "Crimson", "Phantom", "Ivy",
        "Cosmic", "Ocean", "Sunset", "Forest", "Aurora", "Glacier", "Arctic", "Midnight",
        "Pearl", "Starlight", "Space"
    )
    
    // Storage/RAM patterns (GB, TB, etc.)
    val specPattern = Regex("""(\d+\s*(?:GB|TB|MB))""", RegexOption.IGNORE_CASE)
    
    var detectedColor: String? = null
    var remainingText = variantText.trim()
    
    // Try to match known color names (prioritize multi-word colors first)
    for (colorName in colorPatterns.sortedByDescending { it.split(" ").size }) {
        if (remainingText.contains(colorName, ignoreCase = true)) {
            detectedColor = colorName
            remainingText = remainingText.replace(colorName, "", ignoreCase = true).trim()
            break
        }
    }
    
    // Extract technical specs (RAM/Storage)
    val specs = specPattern.findAll(remainingText)
        .map { it.value.replace(" ", "") }
        .toList()
    
    val variantSpec = if (specs.isNotEmpty()) {
        specs.joinToString("/")
    } else {
        // If no specs found, maybe the remaining text is the variant
        val cleanedRemaining = remainingText.trim()
        if (cleanedRemaining.isNotBlank() && detectedColor != null) {
            cleanedRemaining
        } else {
            null
        }
    }
    
    // If no color was detected, check if the entire string might be a color
    if (detectedColor == null && variantSpec == null) {
        // The whole string might be a color
        detectedColor = variantText.trim()
    }
    
    return ParsedVariant(
        color = detectedColor?.trim(),
        variant = variantSpec?.trim()
    )
}

