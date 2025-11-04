package com.billme.app.core.ocr.processor

import android.graphics.Rect
import android.net.Uri
import com.billme.app.core.ocr.OCREngineService
import com.billme.app.core.ocr.OCRResult
import com.billme.app.core.ocr.impl.IMEIDetector
import com.billme.app.core.ocr.impl.IMEIFieldSuggestion
import com.billme.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized processor for mobile shop invoices/bills
 * Optimized for the format in the sample invoice (A.P.Communication style)
 */
@Singleton
class MobileShopInvoiceProcessor @Inject constructor(
    private val ocrEngine: OCREngineService,
    private val imeiDetector: IMEIDetector
) {
    
    /**
     * Process mobile shop invoice with enhanced accuracy
     */
    suspend fun processInvoice(imageUri: Uri): InvoiceProcessingResult = withContext(Dispatchers.IO) {
        try {
            // Configure OCR for invoice processing
            val config = OCRConfig(
                documentType = DocumentType.INVOICE,
                enableFieldExtraction = true,
                enableValidation = true,
                preprocessingOptions = ImagePreprocessingOptions(
                    enableNoiseReduction = true,
                    enableContrastEnhancement = true,
                    enableBinarization = true,
                    enableDeskewing = false
                ),
                fieldExtractionOptions = FieldExtractionOptions(
                    enableSmartFieldDetection = true,
                    enableTableExtraction = true,
                    enableKeyValuePairing = true
                ),
                postProcessingOptions = PostProcessingOptions(
                    enableDataNormalization = true,
                    enableSuggestions = true
                )
            )
            
            // Process with OCR engine
            val result = ocrEngine.processImage(imageUri, config)
            
            when (result) {
                is OCRResult.Success -> {
                    val scanResult = result.scanResult
                    
                    // Extract invoice data
                    val invoiceData = extractInvoiceData(scanResult)
                    
                    // Extract product items
                    val products = extractProducts(scanResult)
                    
                    // Detect IMEIs (if any)
                    val imeis = detectIMEIsFromInvoice(scanResult.rawText)
                    
                    InvoiceProcessingResult.Success(
                        invoiceData = invoiceData,
                        products = products,
                        imeis = imeis,
                        scanResult = scanResult
                    )
                }
                is OCRResult.Error -> {
                    InvoiceProcessingResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            InvoiceProcessingResult.Error(e.message ?: "Invoice processing failed")
        }
    }
    
    private fun extractInvoiceData(scanResult: OCRScanResult): InvoiceData {
        val fields = scanResult.extractedFields
        
        return InvoiceData(
            invoiceNumber = fields["invoice_number"]?.processedValue ?: "",
            date = fields["invoice_date"]?.processedValue ?: "",
            vendorName = fields["vendor_name"]?.processedValue ?: "",
            vendorGSTIN = fields["gst_number"]?.processedValue ?: "",
            vendorPhone = fields["vendor_phone"]?.processedValue ?: "",
            vendorAddress = fields["vendor_address"]?.processedValue ?: "",
            customerName = fields["customer_name"]?.processedValue ?: "",
            customerPhone = fields["customer_phone"]?.processedValue ?: "",
            customerGSTIN = fields["customer_gst"]?.processedValue ?: "",
            customerAddress = fields["buyer_address"]?.processedValue ?: "",
            totalAmount = fields["total_amount"]?.processedValue?.toDoubleOrNull() ?: 0.0,
            taxAmount = fields["tax_amount"]?.processedValue?.toDoubleOrNull() ?: 0.0,
            confidence = scanResult.confidence.overallConfidence
        )
    }
    
    private fun extractProducts(scanResult: OCRScanResult): List<ProductItem> {
        val products = mutableListOf<ProductItem>()
        val fields = scanResult.extractedFields
        
        // Group fields by product index
        val productGroups = fields.entries
            .filter { it.key.startsWith("product_") }
            .groupBy { entry ->
                // Extract index from key like "product_1_description"
                val parts = entry.key.split("_")
                if (parts.size >= 2) parts[1] else null
            }
            .filterKeys { it != null }
        
        for ((index, productFields) in productGroups) {
            val fieldMap = productFields.associate { it.key to it.value }
            
            val description = fieldMap["product_${index}_description"]?.processedValue ?: continue
            val quantity = fieldMap["product_${index}_quantity"]?.processedValue?.toDoubleOrNull() ?: 1.0
            val rate = fieldMap["product_${index}_rate"]?.processedValue?.toDoubleOrNull() ?: 0.0
            val amount = fieldMap["product_${index}_amount"]?.processedValue?.toDoubleOrNull() ?: 0.0
            
            // Parse product description to extract brand and model
            val (brand, model, variant) = parseProductDescription(description)
            
            products.add(
                ProductItem(
                    serialNumber = index?.toIntOrNull() ?: products.size + 1,
                    description = description,
                    brand = brand,
                    model = model,
                    variant = variant,
                    quantity = quantity,
                    rate = rate,
                    amount = amount
                )
            )
        }
        
        return products
    }
    
    private fun parseProductDescription(description: String): Triple<String, String, String> {
        // Extract brand (Redmi, Realme, Samsung, etc.)
        val brands = listOf("Redmi", "Realme", "Samsung", "Vivo", "Oppo", "OnePlus", "iPhone", "Poco", "Motorola")
        var brand = ""
        var remainingText = description
        
        for (b in brands) {
            if (description.contains(b, ignoreCase = true)) {
                brand = b
                remainingText = description.replace(b, "", ignoreCase = true).trim()
                break
            }
        }
        
        // Extract variant (5G, 4G, storage like 256GB)
        val variantPattern = Regex("(\\d+GB|5G|4G|Pro|Plus|Max|Ultra|Lite)", RegexOption.IGNORE_CASE)
        val variants = variantPattern.findAll(remainingText).map { it.value }.joinToString(" ")
        
        // Remaining is model
        val model = remainingText.replace(variantPattern, "").trim()
        
        return Triple(brand, model, variants)
    }
    
    private fun detectIMEIsFromInvoice(text: String): List<String> {
        val imeiFields = imeiDetector.detectIMEIFields(text)
        val imeis = mutableListOf<String>()
        
        imeiFields["imei1"]?.processedValue?.let { imeis.add(it) }
        imeiFields["imei2"]?.processedValue?.let { imeis.add(it) }
        
        return imeis
    }
    
    /**
     * Suggest IMEI field configuration based on detection
     */
    fun suggestIMEIFieldCount(text: String): IMEIFieldSuggestion {
        return imeiDetector.suggestIMEIFieldCount(text)
    }
}

/**
 * Result of invoice processing
 */
sealed class InvoiceProcessingResult {
    data class Success(
        val invoiceData: InvoiceData,
        val products: List<ProductItem>,
        val imeis: List<String>,
        val scanResult: OCRScanResult
    ) : InvoiceProcessingResult()
    
    data class Error(val message: String) : InvoiceProcessingResult()
}

/**
 * Extracted invoice data
 */
data class InvoiceData(
    val invoiceNumber: String,
    val date: String,
    val vendorName: String,
    val vendorGSTIN: String,
    val vendorPhone: String,
    val vendorAddress: String,
    val customerName: String,
    val customerPhone: String,
    val customerGSTIN: String,
    val customerAddress: String,
    val totalAmount: Double,
    val taxAmount: Double,
    val confidence: Float
)

/**
 * Product line item from invoice
 */
data class ProductItem(
    val serialNumber: Int,
    val description: String,
    val brand: String,
    val model: String,
    val variant: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double,
    val imei: String? = null  // IMEI specific to this product
)
