package com.billme.app.core.ocr.impl

import android.graphics.Rect
import com.billme.app.core.ocr.SmartFieldDetector
import com.billme.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Enhanced Smart Field Detector with better context awareness
 * Specifically optimized for mobile shop invoices and bills
 */
@Singleton
class SmartFieldDetectorImpl @Inject constructor() : SmartFieldDetector {
    
    companion object {
        // Invoice field patterns with better context
        private val INVOICE_PATTERNS = mapOf(
            "invoice_number" to listOf(
                Pattern.compile("(?:Invoice|Bill)\\s*(?:No\\.?|Number|#)\\s*:?\\s*([A-Z0-9/-]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:APCM|Invoice)\\s*/\\s*([0-9]+)", Pattern.CASE_INSENSITIVE)
            ),
            "date" to listOf(
                Pattern.compile("(?:Date|Dated)\\s*:?\\s*(\\d{1,2}[-/]\\w{3}[-/]\\d{2,4})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:Ack Date|Invoice Date)\\s*:?\\s*(\\d{1,2}[-/]\\w{3}[-/]\\d{2,4})", Pattern.CASE_INSENSITIVE)
            ),
            "gst_number" to listOf(
                Pattern.compile("GSTIN\\s*/\\s*UIN\\s*:?\\s*([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1})", Pattern.CASE_INSENSITIVE)
            ),
            "vendor_name" to listOf(
                Pattern.compile("([A-Z][A-Za-z\\s\\.]+(?:Communication|Electronic|Traders|Shop))\\s*\\(", Pattern.CASE_INSENSITIVE)
            ),
            "phone" to listOf(
                Pattern.compile("Mob\\s*No\\s*[=:]?\\s*([0-9]{10})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:Mobile|Phone|Contact)\\s*:?\\s*([0-9]{10})", Pattern.CASE_INSENSITIVE)
            )
        )
        
        // Product description patterns for mobile phones - improved to capture full variants
        private val PRODUCT_PATTERNS = listOf(
            // Matches: Brand Model Variant Color RAM Storage (e.g., "Redmi Note 14 Pro 5g Phantom Purple 8gb 256gb")
            Pattern.compile("(Redmi|Realme|Samsung|Vivo|Oppo|OnePlus|iPhone|Poco|MI|Motorola|Nokia|Infinix|Itel|Lava)\\s+([\\w\\s]+?)\\s+(\\d+gb|\\d+pg|\\d{2,3}gb)", Pattern.CASE_INSENSITIVE),
            // More specific pattern for Note series with Pro/Max/Ultra/Lite variants
            Pattern.compile("([A-Z][a-z]+)\\s+(Note|Pro|Max|Ultra|Lite|Plus|Prime)\\s+([0-9]+[A-Za-z]?)\\s+", Pattern.CASE_INSENSITIVE)
        )
        
        // Amount patterns with better context
        private val AMOUNT_PATTERNS = mapOf(
            "rate" to Pattern.compile("(?:Rate|Price)\\s*\\(?(?:Incl|Excl)?.*?Tax\\)?\\s*[:\\-]?\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE),
            "amount" to Pattern.compile("(?:Amount|Total|Sum)\\s*[:\\-]?\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE)
        )
        
        // State names for location detection
        private val STATE_NAMES = setOf(
            "Bihar", "Maharashtra", "Gujarat", "Karnataka", "Tamil Nadu", "Kerala", "Andhra Pradesh",
            "Telangana", "West Bengal", "Uttar Pradesh", "Madhya Pradesh", "Rajasthan", "Punjab",
            "Haryana", "Odisha", "Jharkhand", "Chhattisgarh", "Assam", "Delhi", "Goa"
        )
    }
    
    override suspend fun detectFields(
        textBlocks: List<TextBlock>,
        documentType: DocumentType
    ): Map<String, ExtractedField> = withContext(Dispatchers.Default) {
        
        val extractedFields = mutableMapOf<String, ExtractedField>()
        val allText = textBlocks.joinToString("\n") { it.text }
        
        when (documentType) {
            DocumentType.INVOICE, DocumentType.RECEIPT -> {
                // Extract invoice/receipt specific fields
                extractedFields.putAll(extractInvoiceFields(textBlocks, allText))
                
                // Extract vendor information
                extractedFields.putAll(extractVendorInfo(textBlocks, allText))
                
                // Extract buyer information
                extractedFields.putAll(extractBuyerInfo(textBlocks, allText))
                
                // Extract product line items
                extractedFields.putAll(extractProductItems(textBlocks))
                
                // Extract financial summary
                extractedFields.putAll(extractFinancialSummary(textBlocks, allText))
                
                // Extract addresses
                extractedFields.putAll(extractAddresses(textBlocks, allText))
            }
            else -> {
                // Generic field extraction
                extractedFields.putAll(extractGenericFields(textBlocks, allText))
            }
        }
        
        extractedFields
    }
    
    private fun extractInvoiceFields(textBlocks: List<TextBlock>, allText: String): Map<String, ExtractedField> {
        val fields = mutableMapOf<String, ExtractedField>()
        
        // Extract invoice number
        for (pattern in INVOICE_PATTERNS["invoice_number"] ?: emptyList()) {
            val matcher = pattern.matcher(allText)
            if (matcher.find()) {
                val value = matcher.group(1) ?: continue
                fields["invoice_number"] = ExtractedField(
                    fieldType = FieldType.INVOICE_NUMBER,
                    rawValue = value,
                    processedValue = value.trim(),
                    confidence = 0.95f,
                    boundingBox = findBoundingBox(textBlocks, value),
                    sourceTextBlocks = findSourceBlocks(textBlocks, value),
                    validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.95f)
                )
                break
            }
        }
        
        // Extract date
        for (pattern in INVOICE_PATTERNS["date"] ?: emptyList()) {
            val matcher = pattern.matcher(allText)
            if (matcher.find()) {
                val value = matcher.group(1) ?: continue
                fields["invoice_date"] = ExtractedField(
                    fieldType = FieldType.INVOICE_DATE,
                    rawValue = value,
                    processedValue = normalizeDate(value),
                    confidence = 0.90f,
                    boundingBox = findBoundingBox(textBlocks, value),
                    sourceTextBlocks = findSourceBlocks(textBlocks, value),
                    validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.90f)
                )
                break
            }
        }
        
        // Extract GST number
        for (pattern in INVOICE_PATTERNS["gst_number"] ?: emptyList()) {
            val matcher = pattern.matcher(allText)
            if (matcher.find()) {
                val value = matcher.group(1) ?: continue
                fields["gst_number"] = ExtractedField(
                    fieldType = FieldType.GST_NUMBER,
                    rawValue = value,
                    processedValue = value.trim(),
                    confidence = 0.95f,
                    boundingBox = findBoundingBox(textBlocks, value),
                    sourceTextBlocks = findSourceBlocks(textBlocks, value),
                    validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.95f)
                )
                break
            }
        }
        
        return fields
    }
    
    private fun extractVendorInfo(textBlocks: List<TextBlock>, allText: String): Map<String, ExtractedField> {
        val fields = mutableMapOf<String, ExtractedField>()
        
        // Extract vendor name - usually in first few lines
        for (pattern in INVOICE_PATTERNS["vendor_name"] ?: emptyList()) {
            val matcher = pattern.matcher(allText)
            if (matcher.find()) {
                val value = matcher.group(1)?.trim() ?: continue
                fields["vendor_name"] = ExtractedField(
                    fieldType = FieldType.VENDOR_NAME,
                    rawValue = value,
                    processedValue = value,
                    confidence = 0.85f,
                    boundingBox = findBoundingBox(textBlocks, value),
                    sourceTextBlocks = findSourceBlocks(textBlocks, value),
                    validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.85f)
                )
                break
            }
        }
        
        // Extract phone number
        for (pattern in INVOICE_PATTERNS["phone"] ?: emptyList()) {
            val matcher = pattern.matcher(allText)
            if (matcher.find()) {
                val value = matcher.group(1) ?: continue
                fields["vendor_phone"] = ExtractedField(
                    fieldType = FieldType.PHONE_NUMBER,
                    rawValue = value,
                    processedValue = value,
                    confidence = 0.90f,
                    boundingBox = findBoundingBox(textBlocks, value),
                    sourceTextBlocks = findSourceBlocks(textBlocks, value),
                    validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.90f)
                )
                break
            }
        }
        
        return fields
    }
    
    private fun extractBuyerInfo(textBlocks: List<TextBlock>, allText: String): Map<String, ExtractedField> {
        val fields = mutableMapOf<String, ExtractedField>()
        
        // Look for "Buyer" or "Bill to" section
        val buyerPattern = Pattern.compile("(?:M/s|Buyer|Bill\\s+to)[:\\s]+([A-Z][A-Za-z\\s\\.]+?)(?:\\s*\\(|\\n|Mob)", Pattern.CASE_INSENSITIVE)
        val matcher = buyerPattern.matcher(allText)
        if (matcher.find()) {
            val value = matcher.group(1)?.trim() ?: ""
            if (value.isNotBlank()) {
                fields["customer_name"] = ExtractedField(
                    fieldType = FieldType.CUSTOMER_NAME,
                    rawValue = value,
                    processedValue = value,
                    confidence = 0.85f,
                    boundingBox = findBoundingBox(textBlocks, value),
                    sourceTextBlocks = findSourceBlocks(textBlocks, value),
                    validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.85f)
                )
            }
        }
        
        return fields
    }
    
    private fun extractProductItems(textBlocks: List<TextBlock>): Map<String, ExtractedField> {
        val fields = mutableMapOf<String, ExtractedField>()
        val items = mutableListOf<ProductLineItem>()
        
        // Group blocks by vertical position to identify rows
        val sortedBlocks = textBlocks.sortedBy { it.boundingBox.top }
        val rows = groupIntoRows(sortedBlocks)
        
        // Find the product table section (starts after "Description of Goods" header)
        var inProductSection = false
        var productTableStartIndex = -1
        var productTableEndIndex = rows.size
        
        for (i in rows.indices) {
            val rowText = rows[i].joinToString(" ") { it.text }
            
            // Find table header row
            if (rowText.matches(Regex(".*(?:SI|Sl).*(?:Description of Goods|Description).*(?:HSN|SAC).*(?:Quantity|Qty).*", RegexOption.IGNORE_CASE))) {
                productTableStartIndex = i + 1
                inProductSection = true
                continue
            }
            
            // Alternative: look for just "Description" and "Quantity" columns
            if (!inProductSection && rowText.matches(Regex(".*Description.*Quantity.*Rate.*Amount.*", RegexOption.IGNORE_CASE))) {
                productTableStartIndex = i + 1
                inProductSection = true
                continue
            }
            
            // Stop at totals/subtotals section
            if (inProductSection && rowText.matches(Regex(".*(?:Total|Sub.?total|Grand Total|Amount|C Gst|CGST|SGST|IGST).*", RegexOption.IGNORE_CASE))) {
                productTableEndIndex = i
                break
            }
        }
        
        // If we couldn't find the table header, try to detect products anyway but be more strict
        if (productTableStartIndex == -1) {
            productTableStartIndex = 0
        }
        
        var itemIndex = 1
        
        // Only process rows within the product table section
        for (rowIndex in productTableStartIndex until productTableEndIndex) {
            if (rowIndex >= rows.size) break
            
            val row = rows[rowIndex]
            val rowText = row.joinToString(" ") { it.text }
            
            // Skip empty or very short rows
            if (rowText.trim().length < 10) continue
            
            // Skip rows that look like addresses or contact info
            if (rowText.matches(Regex(".*(?:Bihar|State Name|GSTIN|UIN|Mob No|Email|Consignee|Buyer|Seller|Terms of Delivery|State Code).*", RegexOption.IGNORE_CASE))) {
                continue
            }
            
            // Try to match product pattern - look for brand name PLUS numeric storage specs
            var productMatched = false
            
            // Match brand + storage pattern (e.g., "Redmi Note 14" + "8gb" or "256gb")
            val productPattern = Pattern.compile("(Redmi|Realme|Samsung|Vivo|Oppo|OnePlus|iPhone|Poco|MI|Motorola|Nokia|Infinix|Itel|Lava)\\s+[\\w\\s]+?\\d+(?:gb|pg|GB)", Pattern.CASE_INSENSITIVE)
            val productMatcher = productPattern.matcher(rowText)
            
            if (productMatcher.find()) {
                // Found a valid product - extract full product description
                val startPos = productMatcher.start()
                var productDesc = rowText.substring(startPos)
                
                // Clean up the description - remove trailing HSN codes, quantities, rates
                // Keep: Brand Model Variant Color Storage (e.g., "Redmi Note 14 Pro 5g Phantom Purple 8gb 256gb")
                // Remove: HSN codes (4-8 digits), PCS, amounts
                productDesc = productDesc.replaceFirst(Regex("\\s+\\d{4,8}(?:\\s+|$).*"), "") // Remove HSN and everything after
                productDesc = productDesc.replaceFirst(Regex("\\s+\\d+(?:\\.\\d+)?\\s+PCS.*", RegexOption.IGNORE_CASE), "") // Remove quantity and after
                productDesc = productDesc.replaceFirst(Regex("\\s+\\d{2,3}[,\\d]*\\.\\d{2}.*"), "") // Remove prices and after
                productDesc = productDesc.trim()
                
                // Additional validation: must contain storage size
                if (!productDesc.matches(Regex(".*\\d+(?:gb|GB).*"))) {
                    continue
                }
                
                // Extract quantity, rate, and amount from the row
                val quantity = extractQuantityFromRow(row)
                val rate = extractRateFromRow(row)
                val amount = extractAmountFromRow(row)
                
                // Only add if we have reasonable confidence it's a product
                if (productDesc.isNotBlank() && productDesc.length > 5) {
                    fields["product_${itemIndex}_description"] = ExtractedField(
                        fieldType = FieldType.ITEM_DESCRIPTION,
                        rawValue = rowText,
                        processedValue = productDesc,
                        confidence = 0.92f,
                        boundingBox = calculateRowBoundingBox(row),
                        sourceTextBlocks = row.map { it.blockId },
                        validationResult = FieldValidationResult(true, ValidationType.CONTEXT_BASED, confidence = 0.92f)
                    )
                    
                    if (quantity != null) {
                        fields["product_${itemIndex}_quantity"] = ExtractedField(
                            fieldType = FieldType.ITEM_QUANTITY,
                            rawValue = quantity,
                            processedValue = quantity,
                            confidence = 0.90f,
                            boundingBox = null,
                            sourceTextBlocks = row.map { it.blockId },
                            validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.90f)
                        )
                    }
                    
                    if (rate != null) {
                        fields["product_${itemIndex}_rate"] = ExtractedField(
                            fieldType = FieldType.ITEM_RATE,
                            rawValue = rate,
                            processedValue = rate.replace(",", ""),
                            confidence = 0.90f,
                            boundingBox = null,
                            sourceTextBlocks = row.map { it.blockId },
                            validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.90f)
                        )
                    }
                    
                    if (amount != null) {
                        fields["product_${itemIndex}_amount"] = ExtractedField(
                            fieldType = FieldType.ITEM_AMOUNT,
                            rawValue = amount,
                            processedValue = amount.replace(",", ""),
                            confidence = 0.90f,
                            boundingBox = null,
                            sourceTextBlocks = row.map { it.blockId },
                            validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.90f)
                        )
                    }
                    
                    // Look for IMEI in the next 1-2 rows after product description
                    val imei = extractIMEIForProduct(rows, rowIndex)
                    if (imei != null) {
                        fields["product_${itemIndex}_imei"] = ExtractedField(
                            fieldType = FieldType.UNKNOWN,
                            rawValue = imei,
                            processedValue = imei,
                            confidence = 0.95f,
                            boundingBox = null,
                            sourceTextBlocks = emptyList(),
                            validationResult = FieldValidationResult(true, ValidationType.LUHN_ALGORITHM, confidence = 0.95f)
                        )
                    }
                    
                    itemIndex++
                    productMatched = true
                }
            }
        }
        
        return fields
    }
    
    /**
     * Extract IMEI from rows immediately following a product description
     * This handles cases where IMEI appears on the line below the product
     */
    private fun extractIMEIForProduct(rows: List<List<TextBlock>>, productRowIndex: Int): String? {
        // Check next 2 rows for IMEI (usually on the immediate next line)
        for (offset in 1..2) {
            val nextRowIndex = productRowIndex + offset
            if (nextRowIndex >= rows.size) break
            
            val nextRow = rows[nextRowIndex]
            val nextRowText = nextRow.joinToString(" ") { it.text }
            
            // Look for 15-digit IMEI pattern
            val imeiPattern = Pattern.compile("\\b(\\d{15})\\b")
            val matcher = imeiPattern.matcher(nextRowText)
            
            if (matcher.find()) {
                val potentialIMEI = matcher.group(1) ?: continue
                
                // Validate with Luhn algorithm
                if (validateIMEILuhn(potentialIMEI)) {
                    // Check if this looks like an IMEI (not an invoice/ack number)
                    // IMEI context: should not be near invoice-related keywords
                    val contextCheck = nextRowText.lowercase()
                    val hasAntiContext = contextCheck.contains("invoice") || 
                                        contextCheck.contains("irn") ||
                                        contextCheck.contains("ack") ||
                                        contextCheck.contains("reference")
                    
                    if (!hasAntiContext) {
                        return potentialIMEI
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Validate IMEI using Luhn algorithm
     */
    private fun validateIMEILuhn(imei: String): Boolean {
        if (imei.length != 15) return false
        if (!imei.all { it.isDigit() }) return false
        
        // Reject all same digits
        if (imei.all { it == imei[0] }) return false
        
        // Reject sequential
        var sequential = 0
        for (i in 0 until imei.length - 1) {
            val current = imei[i].toString().toInt()
            val next = imei[i + 1].toString().toInt()
            if (next == current + 1 || (current == 9 && next == 0)) sequential++
        }
        if (sequential > imei.length * 0.6) return false
        
        // Luhn algorithm
        var sum = 0
        var alternate = false
        
        for (i in imei.length - 1 downTo 0) {
            var digit = imei[i].toString().toInt()
            
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
    
    private fun extractFinancialSummary(textBlocks: List<TextBlock>, allText: String): Map<String, ExtractedField> {
        val fields = mutableMapOf<String, ExtractedField>()
        
        // Look for total amount (last significant amount on the page)
        val amountPattern = Pattern.compile("([0-9]{2,}\\.[0-9]{2})")
        val amounts = mutableListOf<Pair<String, Int>>()
        
        val matcher = amountPattern.matcher(allText)
        while (matcher.find()) {
            amounts.add(matcher.group(1) to matcher.start())
        }
        
        // The last amount is usually the grand total
        if (amounts.isNotEmpty()) {
            val totalAmount = amounts.last().first
            fields["total_amount"] = ExtractedField(
                fieldType = FieldType.TOTAL_AMOUNT,
                rawValue = totalAmount,
                processedValue = totalAmount,
                confidence = 0.85f,
                boundingBox = findBoundingBox(textBlocks, totalAmount),
                sourceTextBlocks = findSourceBlocks(textBlocks, totalAmount),
                validationResult = FieldValidationResult(true, ValidationType.CONTEXT_BASED, confidence = 0.85f)
            )
        }
        
        // Look for GST/Tax amount
        val gstPattern = Pattern.compile("(?:C\\s*Gst|CGST|Tax).*?([0-9,]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE)
        val gstMatcher = gstPattern.matcher(allText)
        if (gstMatcher.find()) {
            val taxAmount = gstMatcher.group(1)
            fields["tax_amount"] = ExtractedField(
                fieldType = FieldType.TAX_AMOUNT,
                rawValue = taxAmount,
                processedValue = taxAmount.replace(",", ""),
                confidence = 0.80f,
                boundingBox = findBoundingBox(textBlocks, taxAmount),
                sourceTextBlocks = findSourceBlocks(textBlocks, taxAmount),
                validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.80f)
            )
        }
        
        return fields
    }
    
    private fun extractAddresses(textBlocks: List<TextBlock>, allText: String): Map<String, ExtractedField> {
        val fields = mutableMapOf<String, ExtractedField>()
        
        // Look for state names to identify address blocks
        for (state in STATE_NAMES) {
            if (allText.contains(state, ignoreCase = true)) {
                // Find the context around the state name
                val statePattern = Pattern.compile("([\\s\\S]{0,200}${state}[\\s\\S]{0,50})", Pattern.CASE_INSENSITIVE)
                val matcher = statePattern.matcher(allText)
                if (matcher.find()) {
                    val address = matcher.group(1)?.trim() ?: continue
                    
                    // Determine if it's vendor or buyer address based on position
                    val fieldKey = if (allText.indexOf(address) < allText.length / 2) {
                        "vendor_address"
                    } else {
                        "buyer_address"
                    }
                    
                    fields[fieldKey] = ExtractedField(
                        fieldType = if (fieldKey == "vendor_address") FieldType.VENDOR_ADDRESS else FieldType.BILLING_ADDRESS,
                        rawValue = address,
                        processedValue = cleanAddress(address),
                        confidence = 0.75f,
                        boundingBox = null,
                        sourceTextBlocks = emptyList(),
                        validationResult = FieldValidationResult(true, ValidationType.CONTEXT_BASED, confidence = 0.75f)
                    )
                    break
                }
            }
        }
        
        return fields
    }
    
    private fun extractGenericFields(textBlocks: List<TextBlock>, allText: String): Map<String, ExtractedField> {
        val fields = mutableMapOf<String, ExtractedField>()
        
        // Extract any phone numbers
        val phonePattern = Pattern.compile("(?<!\\d)(\\d{10})(?!\\d)")
        val phoneMatcher = phonePattern.matcher(allText)
        var phoneIndex = 1
        while (phoneMatcher.find()) {
            val phone = phoneMatcher.group(1)
            fields["phone_$phoneIndex"] = ExtractedField(
                fieldType = FieldType.PHONE_NUMBER,
                rawValue = phone,
                processedValue = phone,
                confidence = 0.80f,
                boundingBox = findBoundingBox(textBlocks, phone),
                sourceTextBlocks = findSourceBlocks(textBlocks, phone),
                validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.80f)
            )
            phoneIndex++
        }
        
        return fields
    }
    
    // Helper functions
    
    private fun groupIntoRows(blocks: List<TextBlock>, tolerance: Int = 20): List<List<TextBlock>> {
        if (blocks.isEmpty()) return emptyList()
        
        val rows = mutableListOf<MutableList<TextBlock>>()
        var currentRow = mutableListOf(blocks[0])
        
        for (i in 1 until blocks.size) {
            val currentBlock = blocks[i]
            val previousBlock = blocks[i - 1]
            
            // Check if blocks are on the same line (similar vertical position)
            if (abs(currentBlock.boundingBox.top - previousBlock.boundingBox.top) <= tolerance) {
                currentRow.add(currentBlock)
            } else {
                // Start new row
                rows.add(currentRow.sortedBy { it.boundingBox.left }.toMutableList())
                currentRow = mutableListOf(currentBlock)
            }
        }
        
        // Add the last row
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow.sortedBy { it.boundingBox.left }.toMutableList())
        }
        
        return rows
    }
    
    private fun extractQuantityFromRow(row: List<TextBlock>): String? {
        // Look for patterns like "1.00 PCS" or "1" in the quantity column
        val rowText = row.joinToString(" ") { it.text }
        
        // Pattern 1: "X.XX PCS"
        val qtyWithPcsPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:PCS|pcs|Pcs)")
        val qtyWithPcsMatcher = qtyWithPcsPattern.matcher(rowText)
        if (qtyWithPcsMatcher.find()) {
            val qty = qtyWithPcsMatcher.group(1) ?: return null
            if (qty.toDoubleOrNull()?.let { it <= 100 } == true) {
                return qty
            }
        }
        
        // Pattern 2: standalone small numbers (likely quantity)
        for (block in row) {
            val text = block.text.trim()
            if (text.matches(Regex("^\\d{1,2}$"))) {
                val qty = text.toIntOrNull()
                if (qty != null && qty <= 100) {
                    return text
                }
            }
        }
        
        return null
    }
    
    private fun extractRateFromRow(row: List<TextBlock>): String? {
        val rowText = row.joinToString(" ") { it.text }
        
        // Look for amounts with or without decimals: "17,759.00" or "17759" or "17,759"
        val ratePattern = Pattern.compile("\\b([0-9]{2,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)\\b")
        val amounts = mutableListOf<Pair<String, Double>>()
        
        val matcher = ratePattern.matcher(rowText)
        while (matcher.find()) {
            val rateStr = matcher.group(1) ?: continue
            val rateValue = rateStr.replace(",", "").toDoubleOrNull()
            // Rate should be a reasonable amount (1000-100000 for mobiles)
            if (rateValue != null && rateValue >= 1000 && rateValue <= 100000) {
                amounts.add(rateStr to rateValue)
            }
        }
        
        // Return the first valid rate found (usually appears before amount)
        return amounts.firstOrNull()?.first
    }
    
    private fun extractAmountFromRow(row: List<TextBlock>): String? {
        // Amount is usually the rightmost number in the row
        val rowText = row.joinToString(" ") { it.text }
        
        // Look for amounts: "15,050.00" or "15050" etc.
        val amountPattern = Pattern.compile("\\b([0-9]{2,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)\\b")
        val amounts = mutableListOf<Pair<String, Double>>()
        
        val matcher = amountPattern.matcher(rowText)
        while (matcher.find()) {
            val amountStr = matcher.group(1) ?: continue
            val amountValue = amountStr.replace(",", "").toDoubleOrNull()
            // Amount should be reasonable (500-200000 for mobile invoices)
            if (amountValue != null && amountValue >= 500 && amountValue <= 200000) {
                amounts.add(amountStr to amountValue)
            }
        }
        
        // Return the last (rightmost) valid amount found
        return amounts.lastOrNull()?.first
    }
    
    private fun findBoundingBox(textBlocks: List<TextBlock>, value: String): Rect? {
        val matchingBlocks = textBlocks.filter { it.text.contains(value, ignoreCase = true) }
        if (matchingBlocks.isEmpty()) return null
        
        val left = matchingBlocks.minOf { it.boundingBox.left }
        val top = matchingBlocks.minOf { it.boundingBox.top }
        val right = matchingBlocks.maxOf { it.boundingBox.right }
        val bottom = matchingBlocks.maxOf { it.boundingBox.bottom }
        
        return Rect(left, top, right, bottom)
    }
    
    private fun findSourceBlocks(textBlocks: List<TextBlock>, value: String): List<String> {
        return textBlocks
            .filter { it.text.contains(value, ignoreCase = true) }
            .map { it.blockId }
    }
    
    private fun calculateRowBoundingBox(row: List<TextBlock>): Rect? {
        if (row.isEmpty()) return null
        
        val left = row.minOf { it.boundingBox.left }
        val top = row.minOf { it.boundingBox.top }
        val right = row.maxOf { it.boundingBox.right }
        val bottom = row.maxOf { it.boundingBox.bottom }
        
        return Rect(left, top, right, bottom)
    }
    
    private fun normalizeDate(date: String): String {
        // Convert formats like "30-Oct-25" to "30/10/2025"
        val months = mapOf(
            "Jan" to "01", "Feb" to "02", "Mar" to "03", "Apr" to "04",
            "May" to "05", "Jun" to "06", "Jul" to "07", "Aug" to "08",
            "Sep" to "09", "Oct" to "10", "Nov" to "11", "Dec" to "12"
        )
        
        var normalized = date
        for ((month, num) in months) {
            if (date.contains(month, ignoreCase = true)) {
                normalized = date.replace(month, num, ignoreCase = true)
                break
            }
        }
        
        // Convert 2-digit year to 4-digit
        val yearPattern = Pattern.compile("(\\d{1,2}[-/]\\d{2}[-/])(\\d{2})$")
        val matcher = yearPattern.matcher(normalized)
        if (matcher.find()) {
            val year = matcher.group(2).toInt()
            val fullYear = if (year < 50) "20$year" else "19$year"
            normalized = matcher.replaceFirst("${matcher.group(1)}$fullYear")
        }
        
        return normalized.replace("-", "/")
    }
    
    private fun cleanAddress(address: String): String {
        return address
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * Represents a product line item from invoice
 */
data class ProductLineItem(
    val description: String,
    val quantity: String?,
    val rate: String?,
    val amount: String?
)
