package com.billme.app.core.ocr

import android.util.Log
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent text filter for Tesseract OCR output
 * Filters out irrelevant text and keeps only invoice/product data
 * 
 * This is critical because Tesseract extracts ALL text from image,
 * but we only need specific structured data like:
 * - Invoice header (number, date, GST)
 * - Product descriptions with specs
 * - Quantities, rates, amounts
 * - Totals and taxes
 */
@Singleton
class TesseractTextFilter @Inject constructor() {
    
    companion object {
        private const val TAG = "TesseractFilter"
        
        // Section headers that mark important data
        private val SECTION_HEADERS = listOf(
            "Tax Invoice",
            "Invoice",
            "Description of Goods",
            "SI",
            "HSN/SAC",
            "Quantity",
            "Rate",
            "Amount",
            "Total",
            "CGST",
            "SGST",
            "IGST"
        )
        
        // Patterns to KEEP (invoice data we need)
        private val KEEP_PATTERNS = listOf(
            // Invoice metadata
            Pattern.compile("(?:Invoice|Bill)\\s*(?:No|Number|#)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Date|Dated)\\s*:?\\s*\\d", Pattern.CASE_INSENSITIVE),
            Pattern.compile("GSTIN\\s*/\\s*UIN", Pattern.CASE_INSENSITIVE),
            
            // Product brands (mobile phones)
            Pattern.compile("(Redmi|Realme|Samsung|Vivo|Oppo|OnePlus|iPhone|Poco|MI|Motorola|Nokia|Infinix|Itel|Lava)", Pattern.CASE_INSENSITIVE),
            
            // Storage specs (indicates product line)
            Pattern.compile("\\d+\\s*(?:gb|GB|pg|PG)", Pattern.CASE_INSENSITIVE),
            
            // Color names (for product variants)
            Pattern.compile("(?:Black|White|Blue|Purple|Green|Red|Gold|Silver|Gray|Pink|Phantom|Midnight|Aurora|Nebula|Graphite)", Pattern.CASE_INSENSITIVE),
            
            // Quantities and amounts
            Pattern.compile("\\d+\\.?\\d*\\s*(?:PCS|Nos|Unit)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:₹|Rs\\.?|INR)\\s*\\d", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d+[,\\d]*\\.\\d{2}", Pattern.CASE_INSENSITIVE), // Money amounts
            
            // Tax lines
            Pattern.compile("(?:CGST|SGST|IGST|GST)", Pattern.CASE_INSENSITIVE),
            
            // HSN codes (product classification)
            Pattern.compile("HSN.*?\\d{4,8}", Pattern.CASE_INSENSITIVE),
            
            // Table headers
            Pattern.compile("(?:SI|Sl\\.?\\s*No|Description|Qty|Rate|Amount)", Pattern.CASE_INSENSITIVE)
        )
        
        // Patterns to REMOVE (irrelevant text)
        private val REMOVE_PATTERNS = listOf(
            // Company letterhead boilerplate
            Pattern.compile(".*(?:Registered Office|Corporate Office|Head Office).*", Pattern.CASE_INSENSITIVE),
            
            // Legal text and terms
            Pattern.compile(".*(?:Terms and Conditions|Terms of Sale|Warranty|Disclaimer|E\\.?&\\.?O\\.?E\\.?).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*(?:Subject to|Jurisdiction|Goods once sold|All disputes).*", Pattern.CASE_INSENSITIVE),
            
            // Marketing text
            Pattern.compile(".*(?:Thank you for your business|Visit us|Follow us|Like us on Facebook).*", Pattern.CASE_INSENSITIVE),
            
            // Bank details (not needed for product extraction)
            Pattern.compile(".*(?:Bank Name|Account Number|IFSC Code|Branch).*", Pattern.CASE_INSENSITIVE),
            
            // Addresses (keep buyer/seller names but not full addresses)
            Pattern.compile(".*(?:Pin Code|Postal Code|Zip Code).*", Pattern.CASE_INSENSITIVE),
            
            // Email domains and websites (keep contact but not promotional links)
            Pattern.compile(".*(?:www\\.|http|@gmail\\.com|@yahoo\\.com).*", Pattern.CASE_INSENSITIVE),
            
            // Certificate/authorization text
            Pattern.compile(".*(?:Authorized Signatory|Certified|ISO Certified).*", Pattern.CASE_INSENSITIVE),
            
            // Empty or very short lines (OCR noise)
            Pattern.compile("^[\\s\\W]{0,3}$"), // Only punctuation/whitespace
            
            // Repeated characters (OCR artifacts)
            Pattern.compile("(.)\\1{5,}") // Same character repeated 6+ times
        )
        
        // Keywords that indicate VENDOR section (not products)
        private val VENDOR_KEYWORDS = setOf(
            "seller", "vendor", "from", "supplier", "sold by",
            "consignor", "shipper", "gstin of supplier"
        )
        
        // Keywords that indicate BUYER section (not products)
        private val BUYER_KEYWORDS = setOf(
            "buyer", "bill to", "ship to", "sold to", "consignee",
            "shipping address", "billing address", "customer"
        )
    }
    
    /**
     * Filter raw Tesseract output to keep only relevant invoice data
     * @param fullText Complete OCR output from Tesseract
     * @return Filtered text with only invoice/product data
     */
    fun filterInvoiceText(fullText: String): FilteredOCRText {
        Log.d(TAG, "Filtering text: ${fullText.length} characters")
        
        val lines = fullText.split("\n")
        val filteredLines = mutableListOf<String>()
        val removedLines = mutableListOf<String>()
        
        var inProductTable = false
        var productTableStartLine = -1
        var vendorSectionEnded = false
        var buyerSectionEnded = false
        
        // Pass 1: Identify sections
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            
            val lowerLine = line.lowercase()
            
            // Detect product table start
            if (lowerLine.contains("description of goods") || 
                lowerLine.matches(Regex(".*si.*description.*quantity.*", RegexOption.IGNORE_CASE))) {
                inProductTable = true
                productTableStartLine = i
                filteredLines.add(line)
                Log.d(TAG, "Product table starts at line $i")
                continue
            }
            
            // Detect product table end (totals section)
            if (inProductTable && lowerLine.matches(Regex(".*(?:output|total|sub.?total|grand total|taxable value).*", RegexOption.IGNORE_CASE))) {
                inProductTable = false
                filteredLines.add(line)
                Log.d(TAG, "Product table ends at line $i")
                continue
            }
            
            // Mark when vendor section ends
            if (BUYER_KEYWORDS.any { lowerLine.contains(it) }) {
                vendorSectionEnded = true
            }
            
            // Mark when buyer section ends
            if (vendorSectionEnded && (inProductTable || lowerLine.contains("description"))) {
                buyerSectionEnded = true
            }
        }
        
        // Pass 2: Filter lines
        var currentLineIndex = 0
        var skipVendorAddress = false
        var skipBuyerAddress = false
        
        for (line in lines) {
            currentLineIndex++
            val trimmedLine = line.trim()
            
            // Skip empty lines
            if (trimmedLine.isBlank()) continue
            
            // Skip if matches remove patterns
            var shouldRemove = false
            for (pattern in REMOVE_PATTERNS) {
                if (pattern.matcher(trimmedLine).matches()) {
                    shouldRemove = true
                    removedLines.add("PATTERN_MATCH: $trimmedLine")
                    break
                }
            }
            if (shouldRemove) continue
            
            val lowerLine = trimmedLine.lowercase()
            
            // CRITICAL FILTERING LOGIC
            
            // 1. Always keep invoice header lines
            if (lowerLine.contains("tax invoice") || 
                lowerLine.contains("invoice no") ||
                lowerLine.contains("invoice #") ||
                lowerLine.matches(Regex(".*invoice.*\\d+.*"))) {
                filteredLines.add(trimmedLine)
                continue
            }
            
            // 2. Always keep date lines
            if (lowerLine.contains("date") && trimmedLine.matches(Regex(".*\\d{1,2}[-/]\\w{3}[-/]\\d{2,4}.*"))) {
                filteredLines.add(trimmedLine)
                continue
            }
            
            // 3. Keep GST numbers
            if (lowerLine.contains("gstin") && trimmedLine.matches(Regex(".*\\d{2}[A-Z]{5}\\d{4}[A-Z].*", RegexOption.IGNORE_CASE))) {
                filteredLines.add(trimmedLine)
                continue
            }
            
            // 4. Keep vendor name (first match only, not full address)
            if (!vendorSectionEnded && VENDOR_KEYWORDS.any { lowerLine.contains(it) }) {
                filteredLines.add(trimmedLine)
                skipVendorAddress = true
                continue
            }
            
            // 5. Skip vendor address details
            if (skipVendorAddress && !buyerSectionEnded) {
                if (lowerLine.matches(Regex(".*(?:bihar|state|pin|mob|email|[a-z]{6,}\\s*-\\s*\\d{6}).*"))) {
                    removedLines.add("VENDOR_ADDRESS: $trimmedLine")
                    continue
                }
            }
            
            // 6. Keep buyer name (first match only)
            if (!buyerSectionEnded && BUYER_KEYWORDS.any { lowerLine.contains(it) }) {
                filteredLines.add(trimmedLine)
                skipBuyerAddress = true
                continue
            }
            
            // 7. Skip buyer address details
            if (skipBuyerAddress && !inProductTable) {
                if (lowerLine.matches(Regex(".*(?:bihar|state|pin|mob|email|code).*"))) {
                    removedLines.add("BUYER_ADDRESS: $trimmedLine")
                    continue
                }
            }
            
            // 8. KEEP ALL PRODUCT TABLE CONTENT
            if (inProductTable || (productTableStartLine > 0 && currentLineIndex > productTableStartLine)) {
                // Keep if line contains product indicators
                val hasProductIndicator = KEEP_PATTERNS.any { pattern ->
                    pattern.matcher(trimmedLine).find()
                }
                
                if (hasProductIndicator) {
                    filteredLines.add(trimmedLine)
                    continue
                }
                
                // Keep numeric data in product section (quantities, prices)
                if (trimmedLine.matches(Regex(".*\\d+.*"))) {
                    filteredLines.add(trimmedLine)
                    continue
                }
            }
            
            // 9. Keep tax summary lines
            if (lowerLine.matches(Regex(".*(?:cgst|sgst|igst|tax|total).*\\d+.*"))) {
                filteredLines.add(trimmedLine)
                continue
            }
            
            // 10. Keep if contains any strong keep pattern
            val shouldKeep = KEEP_PATTERNS.any { pattern ->
                pattern.matcher(trimmedLine).find()
            }
            
            if (shouldKeep) {
                filteredLines.add(trimmedLine)
            } else {
                removedLines.add("NO_MATCH: $trimmedLine")
            }
        }
        
        val filteredText = filteredLines.joinToString("\n")
        
        Log.d(TAG, "Filtering complete:")
        Log.d(TAG, "  Original: ${lines.size} lines, ${fullText.length} chars")
        Log.d(TAG, "  Filtered: ${filteredLines.size} lines, ${filteredText.length} chars")
        Log.d(TAG, "  Removed: ${removedLines.size} lines")
        Log.d(TAG, "  Reduction: ${String.format("%.1f", (1 - filteredText.length.toFloat() / fullText.length) * 100)}%")
        
        return FilteredOCRText(
            originalText = fullText,
            filteredText = filteredText,
            originalLineCount = lines.size,
            filteredLineCount = filteredLines.size,
            removedLines = removedLines,
            productTableDetected = inProductTable || productTableStartLine > 0
        )
    }
    
    /**
     * Extract only product lines from filtered text
     * Returns lines that likely contain product descriptions
     */
    fun extractProductLines(filteredText: String): List<ProductLine> {
        val lines = filteredText.split("\n")
        val productLines = mutableListOf<ProductLine>()
        
        var inProductSection = false
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            
            val lowerLine = trimmed.lowercase()
            
            // Start of product section
            if (lowerLine.contains("description of goods") || lowerLine.contains("description")) {
                inProductSection = true
                continue
            }
            
            // End of product section
            if (inProductSection && lowerLine.matches(Regex(".*(?:total|output|taxable).*"))) {
                inProductSection = false
                continue
            }
            
            // Extract product lines
            if (inProductSection) {
                // Must contain brand name AND storage spec
                val hasBrand = trimmed.matches(Regex(".*(?:Redmi|Realme|Samsung|Vivo|Oppo|OnePlus|iPhone|Poco|MI).*", RegexOption.IGNORE_CASE))
                val hasStorage = trimmed.matches(Regex(".*\\d+\\s*(?:gb|GB).*", RegexOption.IGNORE_CASE))
                
                if (hasBrand && hasStorage) {
                    // Extract quantity, rate, amount if present
                    val quantityMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:PCS|Nos)", RegexOption.IGNORE_CASE).find(trimmed)
                    val rateMatch = Regex("(\\d{1,6}[,\\d]*\\.\\d{2})").findAll(trimmed).toList()
                    
                    productLines.add(ProductLine(
                        rawText = trimmed,
                        quantity = quantityMatch?.groupValues?.get(1),
                        rate = rateMatch.getOrNull(0)?.value,
                        amount = rateMatch.getOrNull(1)?.value
                    ))
                }
            }
        }
        
        Log.d(TAG, "Extracted ${productLines.size} product lines")
        return productLines
    }
}

/**
 * Result of text filtering
 */
data class FilteredOCRText(
    val originalText: String,
    val filteredText: String,
    val originalLineCount: Int,
    val filteredLineCount: Int,
    val removedLines: List<String>,
    val productTableDetected: Boolean
)

/**
 * A product line extracted from invoice
 */
data class ProductLine(
    val rawText: String,
    val quantity: String?,
    val rate: String?,
    val amount: String?
)
