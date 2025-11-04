package com.billme.app.ui.screen.addproduct

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR Service for extracting product information from paper bills/boxes
 */
@Singleton
class ProductOCRService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Extract product data from image
     */
    suspend fun extractProductData(imageUri: Uri): Map<String, String> {
        return withContext(Dispatchers.Default) {
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                val visionText = Tasks.await(recognizer.process(inputImage))
                
                val extractedData = mutableMapOf<String, String>()
                val fullText = visionText.text
            
                // Extract all IMEIs (15 digits) - can be multiple
                val imeiPattern = Regex("""\b\d{15}\b""")
                val allIMEIs = imeiPattern.findAll(fullText)
                    .map { it.value }
                    .filter { validateIMEI(it) } // Validate using Luhn algorithm
                    .distinct()
                    .toList()
                
                if (allIMEIs.isNotEmpty()) {
                    // First IMEI as primary
                    extractedData["imei"] = allIMEIs.first()
                    
                    // If multiple IMEIs found, store them as comma-separated
                    if (allIMEIs.size > 1) {
                        extractedData["imeis"] = allIMEIs.joinToString(",")
                    }
                }
                
                // Extract MRP (₹ followed by numbers)
                val mrpPatterns = listOf(
                    Regex("""MRP[:\s]*₹?\s*(\d+(?:[,\.]\d+)*)""", RegexOption.IGNORE_CASE),
                    Regex("""₹\s*(\d+(?:[,\.]\d+)*)"""),
                    Regex("""RS[.\s]*(\d+(?:[,\.]\d+)*)""", RegexOption.IGNORE_CASE)
                )
                
                var found = false
                for (pattern in mrpPatterns) {
                    if (found) break
                    pattern.find(fullText)?.groupValues?.get(1)?.let { price ->
                        extractedData["mrp"] = price.replace(",", "")
                        found = true
                    }
                }
                
                // Extract Brand (common mobile brands)
                val commonBrands = listOf(
                    "Samsung", "Apple", "Xiaomi", "Redmi", "OnePlus", "Oppo", "Vivo",
                    "Realme", "Nokia", "Motorola", "Google", "Pixel", "Nothing",
                    "Asus", "Sony", "LG", "Huawei", "Honor", "Poco", "iQOO"
                )
                
                for (brand in commonBrands) {
                    if (fullText.contains(brand, ignoreCase = true)) {
                        extractedData["brand"] = brand
                        break
                    }
                }
                
                // Extract Model (usually after brand, alphanumeric)
                val modelPattern = Regex("""[A-Z0-9]{2,}[-\s]?[A-Z0-9]+""")
                val models = modelPattern.findAll(fullText).map { it.value }.toList()
                if (models.isNotEmpty()) {
                    // Take the most likely model (shortest, usually model numbers are compact)
                    val likelyModel = models.minByOrNull { it.length }
                    likelyModel?.let { extractedData["model"] = it }
                }
                
                // Extract Color/Variant
                val colors = listOf(
                    "Black", "White", "Blue", "Red", "Green", "Silver", "Gold",
                    "Rose Gold", "Midnight", "Starlight", "Purple", "Pink", "Grey", "Gray"
                )
                
                for (color in colors) {
                    if (fullText.contains(color, ignoreCase = true)) {
                        extractedData["variant"] = color
                        break
                    }
                }
                
                // Extract Storage (GB)
                val storagePattern = Regex("""(\d+)\s*GB""", RegexOption.IGNORE_CASE)
                storagePattern.find(fullText)?.groupValues?.get(1)?.let { storage ->
                    val currentVariant = extractedData["variant"] ?: ""
                    extractedData["variant"] = if (currentVariant.isNotEmpty()) {
                        "$currentVariant, ${storage}GB"
                    } else {
                        "${storage}GB"
                    }
                }
                
                extractedData
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
    
    /**
     * Extract data from bitmap
     */
    suspend fun extractProductData(bitmap: Bitmap): Map<String, String> {
        return withContext(Dispatchers.Default) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val visionText = Tasks.await(recognizer.process(inputImage))
                
                val extractedData = mutableMapOf<String, String>()
                val fullText = visionText.text
                
                // Extract all IMEIs (15 digits) - validate with Luhn algorithm
                val imeiPattern = Regex("""\b\d{15}\b""")
                val allIMEIs = imeiPattern.findAll(fullText)
                    .map { it.value }
                    .filter { validateIMEI(it) }
                    .distinct()
                    .toList()
                
                if (allIMEIs.isNotEmpty()) {
                    extractedData["imei"] = allIMEIs.first()
                    if (allIMEIs.size > 1) {
                        extractedData["imeis"] = allIMEIs.joinToString(",")
                    }
                }
                
                // MRP
                val mrpPattern = Regex("""MRP[:\s]*₹?\s*(\d+(?:[,\.]\d+)*)""", RegexOption.IGNORE_CASE)
                mrpPattern.find(fullText)?.groupValues?.get(1)?.let { price ->
                    extractedData["mrp"] = price.replace(",", "")
                }
                
                extractedData
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
    
    /**
     * Validate IMEI checksum (Luhn algorithm)
     */
    fun validateIMEI(imei: String): Boolean {
        if (imei.length != 15 || !imei.all { it.isDigit() }) return false
        
        val digits = imei.map { it.toString().toInt() }
        var sum = 0
        
        for (i in digits.indices) {
            var digit = digits[i]
            
            // Double every second digit
            if (i % 2 == 1) {
                digit *= 2
                if (digit > 9) {
                    digit = digit / 10 + digit % 10
                }
            }
            
            sum += digit
        }
        
        return sum % 10 == 0
    }
}
