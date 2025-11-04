package com.billme.app.core.ocr.examples

import android.net.Uri
import com.billme.app.core.ocr.processor.InvoiceProcessingResult
import com.billme.app.core.ocr.processor.MobileShopInvoiceProcessor
import com.billme.app.core.ocr.impl.IMEIDetector
import com.billme.app.core.ocr.impl.IMEIFieldSuggestion
import javax.inject.Inject

/**
 * Usage examples for the enhanced OCR system
 * 
 * This class demonstrates how to use the improved OCR features:
 * 1. Better invoice/bill scanning with accurate field extraction
 * 2. Smart IMEI detection that won't pick up random numbers
 * 3. Intelligent product name and details extraction
 * 4. Optional IMEI2 field based on detection
 */
class OCRUsageExamples @Inject constructor(
    private val invoiceProcessor: MobileShopInvoiceProcessor,
    private val imeiDetector: IMEIDetector
) {
    
    /**
     * Example 1: Process a mobile shop invoice/bill
     * 
     * This will:
     * - Extract invoice details (number, date, vendor, customer)
     * - Extract product items with brand, model, quantity, rate
     * - Detect IMEIs intelligently (only valid ones)
     * - Provide high confidence results
     */
    suspend fun processInvoiceExample(imageUri: Uri) {
        when (val result = invoiceProcessor.processInvoice(imageUri)) {
            is InvoiceProcessingResult.Success -> {
                // Invoice metadata extracted
                val invoice = result.invoiceData
                println("Invoice: ${invoice.invoiceNumber}")
                println("Date: ${invoice.date}")
                println("Vendor: ${invoice.vendorName}")
                println("Customer: ${invoice.customerName}")
                println("Total: ${invoice.totalAmount}")
                
                // Products extracted
                result.products.forEach { product ->
                    println("Product: ${product.brand} ${product.model} ${product.variant}")
                    println("  Quantity: ${product.quantity}")
                    println("  Rate: ${product.rate}")
                    println("  Amount: ${product.amount}")
                }
                
                // IMEIs detected (only valid ones)
                result.imeis.forEach { imei ->
                    println("IMEI detected: $imei")
                }
                
                // Confidence score
                println("Confidence: ${invoice.confidence * 100}%")
            }
            is InvoiceProcessingResult.Error -> {
                println("Error: ${result.message}")
            }
        }
    }
    
    /**
     * Example 2: Smart IMEI detection
     * 
     * This will ONLY detect valid IMEIs, not random 15-digit numbers like:
     * - Invoice numbers (8b82e27ca657d389a2f5068d53a9c805a3bc216dc2045c...)
     * - GST numbers (10ANKPK3971F1Z7)
     * - Phone numbers (8051460488)
     * - Dates (30-Oct-25)
     */
    fun detectIMEIExample(text: String) {
        val imeiCandidates = imeiDetector.detectIMEIs(text)
        
        // Only high-confidence IMEIs
        val validIMEIs = imeiCandidates.filter { it.confidence >= 0.7f }
        
        validIMEIs.forEach { candidate ->
            println("IMEI: ${candidate.imei}")
            println("  Confidence: ${candidate.confidence * 100}%")
            println("  Context: ${candidate.context}")
            println("  Valid: ${candidate.validationResult.isValid}")
        }
    }
    
    /**
     * Example 3: Determine IMEI field count
     * 
     * Based on detection, suggest:
     * - SINGLE: Show only IMEI1 field (don't force IMEI2)
     * - DUAL: Show both IMEI1 and IMEI2 fields
     * - MULTIPLE: Show multi-IMEI interface
     * - NONE: No IMEI detected, require manual entry
     */
    fun suggestIMEIFieldsExample(text: String) {
        val suggestion = invoiceProcessor.suggestIMEIFieldCount(text)
        
        when (suggestion) {
            IMEIFieldSuggestion.NONE -> {
                println("No IMEI detected - show manual entry only")
            }
            IMEIFieldSuggestion.SINGLE -> {
                println("One IMEI detected - show only IMEI1 field")
                println("IMEI2 should be optional, not required")
            }
            IMEIFieldSuggestion.DUAL -> {
                println("Two IMEIs detected - show IMEI1 and IMEI2 fields")
            }
            IMEIFieldSuggestion.MULTIPLE -> {
                println("Multiple IMEIs detected - show multi-IMEI interface")
            }
        }
    }
    
    /**
     * Example 4: Understanding product extraction
     * 
     * From: "Redmi Note 14 5g Crimson Art 8gb 256gb"
     * Extracts:
     * - Brand: Redmi
     * - Model: Note 14
     * - Variant: 5G Crimson Art 8GB 256GB
     * - Category: Mobile Phone (inferred)
     */
    fun productExtractionExample() {
        // This is handled automatically by the invoiceProcessor
        // The example shows what it extracts from the sample invoice:
        
        // Product 1: Redmi Note 14 5g Crimson Art 8gb 256gb
        //   Brand: Redmi
        //   Model: Note 14
        //   Variant: 5G 8GB 256GB
        //   Quantity: 1.00 PCS
        //   Rate: 17,759.00
        //   Amount: 15,050.00
        
        println("Product extraction is automatic - see processInvoiceExample()")
    }
    
    /**
     * Example 5: Understanding what's NOT detected as IMEI
     * 
     * These will NOT be detected as IMEIs:
     */
    fun whatIsNotIMEIExample() {
        val notIMEIs = listOf(
            "8b82e27ca657d389a2f5068d53a9c805a3bc216dc2045c" to "Invoice IRN - too long, contains letters",
            "182520602255884" to "Ack Number - might be flagged low confidence",
            "8051460488" to "Phone number - only 10 digits",
            "10ANKPK3971F1Z7" to "GST number - contains letters, wrong format",
            "APCM/I/3716" to "Invoice number - contains letters and special chars",
            "111111111111111" to "All same digits - rejected",
            "123456789012345" to "Sequential - rejected or low confidence"
        )
        
        notIMEIs.forEach { (number, reason) ->
            println("$number - $reason")
            
            val candidates = imeiDetector.detectIMEIs(number)
            if (candidates.isEmpty()) {
                println("  ✓ Correctly NOT detected as IMEI")
            } else {
                val validOnes = candidates.filter { it.confidence >= 0.7f }
                if (validOnes.isEmpty()) {
                    println("  ✓ Detected but confidence too low (rejected)")
                } else {
                    println("  ✗ Incorrectly detected (confidence: ${validOnes[0].confidence})")
                }
            }
        }
    }
}

/**
 * Key Improvements in the Enhanced OCR System:
 * 
 * 1. BETTER ACCURACY
 *    - Context-aware field detection
 *    - Pattern matching for mobile brands (Redmi, Realme, Samsung, etc.)
 *    - Indian invoice format support (GST, state codes, etc.)
 *    - OCR error correction (O->0, l->1 in numbers)
 * 
 * 2. BETTER DATA FILLING
 *    - Automatic extraction of vendor/customer details
 *    - Product line item detection from table rows
 *    - Quantity, rate, amount extraction with validation
 *    - Address parsing with state detection
 * 
 * 3. BETTER PRODUCT UNDERSTANDING
 *    - Brand recognition (Redmi, Realme, Samsung, etc.)
 *    - Model extraction (Note 14, Pro 5G, etc.)
 *    - Variant detection (5G, 256GB, color names)
 *    - Storage/RAM normalization (256gb -> 256GB)
 * 
 * 4. SMART IMEI DETECTION
 *    - Luhn algorithm validation
 *    - Context analysis (looks for "IMEI" keyword nearby)
 *    - Anti-context filtering (rejects invoice numbers, GST, etc.)
 *    - Rejects sequential numbers (123456789012345)
 *    - Rejects all-same-digit numbers (111111111111111)
 *    - Only returns high-confidence results
 * 
 * 5. OPTIONAL IMEI2 FIELD
 *    - If only one IMEI detected, IMEI2 is not required
 *    - If two IMEIs detected, both fields are shown
 *    - If no IMEI detected, fields are optional (manual entry)
 *    - Intelligent field suggestion based on detection
 * 
 * 6. INVOICE FORMAT UNDERSTANDING
 *    - Header section (Invoice number, date, GST)
 *    - Vendor section (Name, address, contact)
 *    - Buyer section (Name, address, contact)
 *    - Item table (Description, Qty, Rate, Amount)
 *    - Footer section (Subtotal, tax, total)
 *    - Multi-page support (continued on page 2)
 */
