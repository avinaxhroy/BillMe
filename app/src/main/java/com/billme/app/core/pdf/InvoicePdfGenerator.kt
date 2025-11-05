package com.billme.app.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.billme.app.data.model.GSTBreakdown
import com.billme.app.data.local.entity.GSTConfiguration
import com.billme.app.data.local.entity.GSTMode
import com.billme.app.data.local.entity.PaymentStatus
import com.billme.app.data.local.entity.Transaction
import com.billme.app.data.local.entity.TransactionItem
import com.billme.app.core.util.HindiTransliterator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates professional PDF invoices with proper formatting
 * Supports bilingual (English/Hindi) invoices
 */
@Singleton
class InvoicePdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PAGE_WIDTH = 595 // A4 width in points
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 40f
        private const val LINE_HEIGHT = 20f
    }

    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val headerPaint = Paint().apply {
        color = Color.BLACK
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val normalPaint = Paint().apply {
        color = Color.BLACK
        textSize = 12f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    private val hindiPaint = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL) // Better Hindi support
        isAntiAlias = true
    }

    private val boldPaint = Paint().apply {
        color = Color.BLACK
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1f
        isAntiAlias = true
    }

    private val smallPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 9f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    private val totalPaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

        /**
     * Generate invoice PDF with configuration options
     * @param transaction The transaction data
     * @param items List of transaction items
     * @param shopName Shop name from settings
     * @param shopAddress Shop address from settings
     * @param shopPhone Shop phone from settings
     * @param shopGst Shop GST number from settings
     * @param customerCopy If true, generates customer copy, otherwise owner copy
     * @param gstConfig GST configuration for flexible GST system
     * @param signatureFilePath Optional path to signature image file
     * @param bilingualMode If true, generates bilingual (English+Hindi) invoice, otherwise English only
     * @return File object of generated PDF
     */
    fun generateInvoicePdf(
        transaction: Transaction,
        items: List<TransactionItem>,
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        shopGst: String,
        customerCopy: Boolean = true,
        gstConfig: GSTConfiguration = GSTConfiguration(
            shopGSTIN = shopGst,
            defaultGSTMode = GSTMode.FULL_GST,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        signatureFilePath: String? = null,
        bilingualMode: Boolean = false
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var yPos = MARGIN + 20f

        // Draw invoice header
        yPos = drawHeader(canvas, yPos, shopName, shopAddress, shopPhone, shopGst, customerCopy, gstConfig)

        // Draw transaction details
        yPos = drawTransactionDetails(canvas, yPos, transaction)

        // Draw customer details if available
        if (!transaction.customerName.isNullOrBlank()) {
            yPos = drawCustomerDetails(canvas, yPos, transaction, bilingualMode)
        }

        // Draw line separator
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint)
        yPos += LINE_HEIGHT

        // Draw items table
        yPos = drawItemsTable(canvas, yPos, items)

        // Draw totals with GST breakdown
        yPos = drawTotals(canvas, yPos, transaction, gstConfig, customerCopy)

        // Draw payment details
        yPos = drawPaymentDetails(canvas, yPos, transaction)

        // Draw footer with signature
        drawFooter(canvas, yPos, customerCopy, gstConfig, signatureFilePath)

        pdfDocument.finishPage(page)

        // Save to file
        val fileName = if (customerCopy) {
            "Invoice_${transaction.transactionNumber}_Customer.pdf"
        } else {
            "Invoice_${transaction.transactionNumber}_Owner.pdf"
        }

        val invoicesDir = File(context.getExternalFilesDir(null), "Invoices")
        if (!invoicesDir.exists()) {
            invoicesDir.mkdirs()
        }

        val file = File(invoicesDir, fileName)
        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()

        return file
    }

    private fun drawHeader(
        canvas: Canvas,
        startY: Float,
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        shopGst: String,
        customerCopy: Boolean,
        gstConfig: GSTConfiguration
    ): Float {
        var yPos = startY

        // Shop name (centered)
        val shopNameWidth = titlePaint.measureText(shopName)
        canvas.drawText(shopName, (PAGE_WIDTH - shopNameWidth) / 2, yPos, titlePaint)
        yPos += LINE_HEIGHT + 10

        // Shop details (centered)
        if (shopAddress.isNotBlank()) {
            val addressWidth = normalPaint.measureText(shopAddress)
            canvas.drawText(shopAddress, (PAGE_WIDTH - addressWidth) / 2, yPos, normalPaint)
            yPos += LINE_HEIGHT
        }

        if (shopPhone.isNotBlank()) {
            val phoneText = "Phone: $shopPhone"
            val phoneWidth = normalPaint.measureText(phoneText)
            canvas.drawText(phoneText, (PAGE_WIDTH - phoneWidth) / 2, yPos, normalPaint)
            yPos += LINE_HEIGHT
        }

        // Show GSTIN based on GST mode
        if (gstConfig.defaultGSTMode.shouldShowGSTIN() && shopGst.isNotBlank()) {
            val gstText = "GSTIN: $shopGst"
            val gstWidth = normalPaint.measureText(gstText)
            canvas.drawText(gstText, (PAGE_WIDTH - gstWidth) / 2, yPos, normalPaint)
            yPos += LINE_HEIGHT
        }

        yPos += 10

        // Invoice title - adjust based on GST mode
        val invoiceTitle = when {
            !customerCopy -> "TAX INVOICE (OWNER COPY)"
            gstConfig.defaultGSTMode == GSTMode.FULL_GST || gstConfig.defaultGSTMode == GSTMode.PARTIAL_GST -> "TAX INVOICE"
            gstConfig.defaultGSTMode == GSTMode.GST_REFERENCE -> "INVOICE"
            else -> "INVOICE"
        }
        val titleWidth = headerPaint.measureText(invoiceTitle)
        canvas.drawText(invoiceTitle, (PAGE_WIDTH - titleWidth) / 2, yPos, headerPaint)
        yPos += LINE_HEIGHT + 10

        // Line separator
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint)
        yPos += LINE_HEIGHT

        return yPos
    }

    private fun drawTransactionDetails(
        canvas: Canvas,
        startY: Float,
        transaction: Transaction
    ): Float {
        var yPos = startY

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val transactionDate = dateFormat.format(Date(transaction.transactionDate.toEpochMilliseconds()))

        // Left side - Invoice Number
        canvas.drawText("Invoice No:", MARGIN, yPos, boldPaint)
        canvas.drawText(transaction.transactionNumber, MARGIN + 100, yPos, normalPaint)

        // Right side - Date
        val dateText = "Date: $transactionDate"
        val dateWidth = normalPaint.measureText(dateText)
        canvas.drawText(dateText, PAGE_WIDTH - MARGIN - dateWidth, yPos, normalPaint)
        yPos += LINE_HEIGHT + 10

        return yPos
    }

    private fun drawCustomerDetails(
        canvas: Canvas,
        startY: Float,
        transaction: Transaction,
        bilingualMode: Boolean = false
    ): Float {
        var yPos = startY

        canvas.drawText("Bill To:", MARGIN, yPos, boldPaint)
        yPos += LINE_HEIGHT

        // Customer name in English
        val customerName = transaction.customerName ?: ""
        canvas.drawText(customerName, MARGIN + 10, yPos, normalPaint)
        yPos += LINE_HEIGHT

        // Draw Hindi name only if bilingual mode is enabled
        if (bilingualMode) {
            // Extract Hindi name from notes if available, otherwise transliterate using specialized method
            val hindiName = extractHindiNameFromNotes(transaction.notes)
                ?: if (customerName.isNotBlank()) HindiTransliterator.transliterateFullName(customerName) else ""
            
            if (hindiName.isNotBlank()) {
                canvas.drawText(hindiName, MARGIN + 10, yPos, hindiPaint)
                yPos += LINE_HEIGHT
            }
        }

        if (!transaction.customerPhone.isNullOrBlank()) {
            canvas.drawText("Phone: ${transaction.customerPhone}", MARGIN + 10, yPos, normalPaint)
            yPos += LINE_HEIGHT
        }

        // Extract customer address from notes field
        val customerAddress = extractAddressFromNotes(transaction.notes)
        if (customerAddress.isNotBlank()) {
            // Address in English
            val addressLines = wrapText(customerAddress, PAGE_WIDTH - MARGIN - 60, normalPaint)
            addressLines.forEach { line ->
                canvas.drawText(line, MARGIN + 10, yPos, normalPaint)
                yPos += LINE_HEIGHT * 0.9f  // Slightly tighter spacing for multi-line address
            }
            
            // Draw Hindi address only if bilingual mode is enabled
            if (bilingualMode) {
                // Extract Hindi address from notes if available, otherwise transliterate using specialized method
                val hindiAddress = extractHindiAddressFromNotes(transaction.notes)
                    ?: HindiTransliterator.transliterateAddress(customerAddress)
                
                if (hindiAddress.isNotBlank()) {
                    val hindiAddressLines = wrapText(hindiAddress, PAGE_WIDTH - MARGIN - 60, hindiPaint)
                    hindiAddressLines.forEach { line ->
                        canvas.drawText(line, MARGIN + 10, yPos, hindiPaint)
                        yPos += LINE_HEIGHT * 0.9f
                    }
                }
            }
        }

        yPos += 10
        return yPos
    }

    /**
     * Extract Hindi customer name from notes field
     * Format in notes: "HindiName:<hindi text>"
     */
    private fun extractHindiNameFromNotes(notes: String?): String? {
        if (notes.isNullOrBlank()) return null
        val hindiNamePattern = "HindiName:([^\n]+)".toRegex()
        val match = hindiNamePattern.find(notes)
        return match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /**
     * Extract Hindi customer address from notes field
     * Format in notes: "HindiAddress:<hindi text>"
     */
    private fun extractHindiAddressFromNotes(notes: String?): String? {
        if (notes.isNullOrBlank()) return null
        val hindiAddressPattern = "HindiAddress:([^\n]+)".toRegex()
        val match = hindiAddressPattern.find(notes)
        return match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /**
     * Extract customer address from notes field
     * Format: "Address: <actual address>"
     */
    private fun extractAddressFromNotes(notes: String?): String {
        if (notes.isNullOrBlank()) return ""
        
        // Extract address line that starts with "Address: "
        val lines = notes.split("\n")
        val addressLine = lines.find { it.startsWith("Address: ") }
        return addressLine?.removePrefix("Address: ")?.trim() ?: ""
    }

    private fun drawItemsTable(
        canvas: Canvas,
        startY: Float,
        items: List<TransactionItem>
    ): Float {
        var yPos = startY

        // Table headers
        canvas.drawText("#", MARGIN, yPos, boldPaint)
        canvas.drawText("Item", MARGIN + 30, yPos, boldPaint)
        canvas.drawText("Qty", PAGE_WIDTH - MARGIN - 250, yPos, boldPaint)
        canvas.drawText("Price", PAGE_WIDTH - MARGIN - 180, yPos, boldPaint)
        canvas.drawText("Amount", PAGE_WIDTH - MARGIN - 80, yPos, boldPaint)
        yPos += LINE_HEIGHT

        // Line under headers
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint)
        yPos += LINE_HEIGHT

        // Items
        items.forEachIndexed { index, item ->
            val itemNumber = "${index + 1}"
            val itemName = item.productName
            val quantity = item.quantity.toString()
            val price = "₹${item.sellingPrice}"
            val amount = "₹${item.totalPrice}"

            canvas.drawText(itemNumber, MARGIN, yPos, normalPaint)
            
            // Handle long product names
            val nameLines = wrapText(itemName, PAGE_WIDTH - MARGIN - 300, normalPaint)
            var nameYPos = yPos
            nameLines.forEach { line ->
                canvas.drawText(line, MARGIN + 30, nameYPos, normalPaint)
                nameYPos += LINE_HEIGHT
            }

            canvas.drawText(quantity, PAGE_WIDTH - MARGIN - 250, yPos, normalPaint)
            canvas.drawText(price, PAGE_WIDTH - MARGIN - 180, yPos, normalPaint)
            
            val amountWidth = normalPaint.measureText(amount)
            canvas.drawText(amount, PAGE_WIDTH - MARGIN - amountWidth, yPos, normalPaint)

            yPos = nameYPos
            
            // Display color and variant if available
            if (!item.color.isNullOrBlank() || !item.variant.isNullOrBlank()) {
                val specs = buildString {
                    if (!item.color.isNullOrBlank()) append("Color: ${item.color}")
                    if (!item.color.isNullOrBlank() && !item.variant.isNullOrBlank()) append(" | ")
                    if (!item.variant.isNullOrBlank()) append("Variant: ${item.variant}")
                }
                canvas.drawText(specs, MARGIN + 40, yPos, smallPaint)
                yPos += 12
            }
            
            // Display IMEI numbers if available
            if (!item.imeiNumber.isNullOrBlank()) {
                val imeiText = "IMEI: ${item.imeiNumber}"
                canvas.drawText(imeiText, MARGIN + 40, yPos, smallPaint)
                yPos += 12 // Smaller line height for IMEI
                
                // Display second IMEI if available (for dual SIM)
                if (!item.imei2Number.isNullOrBlank()) {
                    val imei2Text = "IMEI 2: ${item.imei2Number}"
                    canvas.drawText(imei2Text, MARGIN + 40, yPos, smallPaint)
                    yPos += 12
                }
            }

            yPos += 5
        }

        // Line after items
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint)
        yPos += LINE_HEIGHT

        return yPos
    }

    private fun drawTotals(
        canvas: Canvas,
        startY: Float,
        transaction: Transaction,
        gstConfig: GSTConfiguration,
        customerCopy: Boolean
    ): Float {
        var yPos = startY

        val labelX = PAGE_WIDTH - MARGIN - 200
        val valueX = PAGE_WIDTH - MARGIN
        var valueWidth: Float

        // For GST-inclusive pricing:
        // transaction.subtotal = taxable value (selling price - GST)
        // transaction.taxAmount = extracted GST
        // transaction.grandTotal = final price customer pays
        
        val gstRate = gstConfig.defaultGSTRate
        val cgst = if (gstConfig.defaultGSTMode.shouldApplyGST()) {
            transaction.taxAmount.divide(BigDecimal(2), 2, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        val sgst = cgst
        val igst = transaction.taxAmount
        
        val gstBreakdown = GSTBreakdown(
            cgst = cgst,
            sgst = sgst,
            igst = BigDecimal.ZERO,
            cess = BigDecimal.ZERO,
            total = transaction.taxAmount
        )

        // Show Subtotal only for FULL_GST and PARTIAL_GST modes
        // Don't show for GST_REFERENCE or NO_GST modes
        if (gstConfig.defaultGSTMode == GSTMode.FULL_GST || gstConfig.defaultGSTMode == GSTMode.PARTIAL_GST) {
            val subtotalText = "₹${transaction.subtotal}"
            valueWidth = normalPaint.measureText(subtotalText)
            canvas.drawText("Subtotal:", labelX, yPos, normalPaint)
            canvas.drawText(subtotalText, valueX - valueWidth, yPos, normalPaint)
            yPos += LINE_HEIGHT

            // Discount
            if (transaction.discountAmount > BigDecimal.ZERO) {
                val discountText = "- ₹${transaction.discountAmount}"
                valueWidth = normalPaint.measureText(discountText)
                canvas.drawText("Discount:", labelX, yPos, normalPaint)
                canvas.drawText(discountText, valueX - valueWidth, yPos, normalPaint)
                yPos += LINE_HEIGHT
            }
        }

        // GST Breakdown - based on mode and copy type
        // FULL_GST: Show on all invoices
        // PARTIAL_GST: Show only on owner invoice
        // GST_REFERENCE: Don't show breakdown
        // NO_GST: Don't show breakdown
        val shouldShowGST = when (gstConfig.defaultGSTMode) {
            GSTMode.FULL_GST -> true // Always show
            GSTMode.PARTIAL_GST -> !customerCopy // Show only on owner copy
            GSTMode.GST_REFERENCE -> false // Don't show breakdown for reference mode
            GSTMode.NO_GST -> false // Don't show for no GST mode
        }

        if (shouldShowGST && gstBreakdown.totalGST > BigDecimal.ZERO) {
            // Show detailed GST breakdown
            if (gstBreakdown.hasIGST()) {
                // IGST (Inter-state)
                val igstText = "₹${gstBreakdown.igst}"
                valueWidth = normalPaint.measureText(igstText)
                canvas.drawText("IGST (${gstConfig.defaultGSTRate}%):", labelX, yPos, normalPaint)
                canvas.drawText(igstText, valueX - valueWidth, yPos, normalPaint)
                yPos += LINE_HEIGHT
            } else if (gstBreakdown.hasCGSTSGST()) {
                // CGST (Intra-state)
                val cgstText = "₹${gstBreakdown.cgst}"
                valueWidth = normalPaint.measureText(cgstText)
                val cgstRate = gstConfig.defaultGSTRate / 2
                canvas.drawText("CGST ($cgstRate%):", labelX, yPos, normalPaint)
                canvas.drawText(cgstText, valueX - valueWidth, yPos, normalPaint)
                yPos += LINE_HEIGHT

                // SGST (Intra-state)
                val sgstText = "₹${gstBreakdown.sgst}"
                valueWidth = normalPaint.measureText(sgstText)
                canvas.drawText("SGST ($cgstRate%):", labelX, yPos, normalPaint)
                canvas.drawText(sgstText, valueX - valueWidth, yPos, normalPaint)
                yPos += LINE_HEIGHT
            }

            // Total GST
            val totalGstText = "₹${gstBreakdown.totalGST}"
            valueWidth = normalPaint.measureText(totalGstText)
            canvas.drawText("Total GST:", labelX, yPos, boldPaint)
            canvas.drawText(totalGstText, valueX - valueWidth, yPos, boldPaint)
            yPos += LINE_HEIGHT
        }

        // Line before total
        canvas.drawLine(labelX, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint)
        yPos += LINE_HEIGHT

        // Total Amount (final price customer pays)
        val totalText = "₹${transaction.grandTotal.setScale(2, java.math.RoundingMode.HALF_UP)}"
        valueWidth = totalPaint.measureText(totalText)
        canvas.drawText("Total Amount:", labelX, yPos, totalPaint)
        canvas.drawText(totalText, valueX - valueWidth, yPos, totalPaint)
        yPos += LINE_HEIGHT + 10

        return yPos
    }

    private fun drawPaymentDetails(
        canvas: Canvas,
        startY: Float,
        transaction: Transaction
    ): Float {
        var yPos = startY

        canvas.drawText("Payment Method:", MARGIN, yPos, boldPaint)
        val paymentMethodText = when (transaction.paymentMethod.name) {
            "EMI" -> "Credit on Sale/EMI"
            else -> transaction.paymentMethod.name
        }
        canvas.drawText(paymentMethodText, MARGIN + 120, yPos, normalPaint)
        yPos += LINE_HEIGHT

        if (transaction.paymentStatus == PaymentStatus.PAID) {
            canvas.drawText("Payment Status:", MARGIN, yPos, boldPaint)
            
            val statusPaint = Paint(normalPaint).apply {
                color = Color.parseColor("#4CAF50") // Green color
            }
            canvas.drawText("PAID", MARGIN + 120, yPos, statusPaint)
            yPos += LINE_HEIGHT
        }

        yPos += 20
        return yPos
    }

    private fun drawFooter(canvas: Canvas, startY: Float, customerCopy: Boolean, gstConfig: GSTConfiguration, signatureFilePath: String? = null) {
        var yPos = PAGE_HEIGHT - MARGIN - 110

        // Draw signature if available or placeholder
        if (!signatureFilePath.isNullOrBlank()) {
            // Check if it's a placeholder request
            if (signatureFilePath.equals("PLACEHOLDER", ignoreCase = true)) {
                // Draw simple signature placeholder - just underline and text
                val signatureWidth = 150f
                val signatureX = PAGE_WIDTH - MARGIN - signatureWidth - 10
                
                // Draw underline for signature
                val underlineY = yPos - 5
                canvas.drawLine(signatureX - 20, underlineY, signatureX + signatureWidth + 20, underlineY, linePaint)
                
                // Draw label
                val labelPaint = Paint(smallPaint).apply {
                    color = Color.BLACK
                    textSize = 10f
                }
                val labelText = "Authorized Signature"
                val labelWidth = labelPaint.measureText(labelText)
                val labelX = signatureX + (signatureWidth - labelWidth) / 2
                canvas.drawText(labelText, labelX, yPos + 8, labelPaint)
                
                yPos += 20
            } else {
                // Handle digital signature image
                try {
                    val signatureFile = File(signatureFilePath)
                    if (signatureFile.exists()) {
                        // Decode with high quality options
                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inDither = true
                        }
                        val bitmap = BitmapFactory.decodeFile(signatureFilePath, options)
                        if (bitmap != null) {
                            // Calculate signature dimensions for better quality
                            // Use larger intermediate size to preserve quality
                            val maxWidth = 150f  // Increased from 100
                            val maxHeight = 75f  // Increased from 50
                            val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
                            val scaledWidth = (bitmap.width * scale).toInt()
                            val scaledHeight = (bitmap.height * scale).toInt()
                            
                            // Resize bitmap with high-quality filtering
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                            
                            // Create a paint with high-quality rendering
                            val signaturePaint = Paint().apply {
                                isAntiAlias = true
                                isFilterBitmap = true
                                isDither = true
                            }
                            
                            // Draw signature on right side with proper positioning
                            val signatureX = PAGE_WIDTH - MARGIN - scaledWidth - 10
                            val signatureY = yPos - scaledHeight - 10
                            canvas.drawBitmap(scaledBitmap, signatureX, signatureY, signaturePaint)
                            
                            // Draw underline for signature
                            val underlineY = yPos - 5
                            canvas.drawLine(signatureX - 20, underlineY, signatureX + scaledWidth + 20, underlineY, linePaint)
                            
                            // Draw label
                            val labelPaint = Paint(smallPaint).apply {
                                color = Color.BLACK
                                textSize = 10f
                            }
                            val labelText = "Authorized Signature"
                            val labelWidth = labelPaint.measureText(labelText)
                            val labelX = signatureX + (scaledWidth - labelWidth) / 2
                            canvas.drawText(labelText, labelX, yPos + 8, labelPaint)
                            
                            yPos += scaledHeight + 20
                            scaledBitmap.recycle()
                            bitmap.recycle()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If signature can't be loaded, just skip it
                }
            }
        }

        // Warranty note - Important for IMEI tracked products (with text wrapping)
        val warrantyNotePaint = Paint(normalPaint).apply {
            color = Color.parseColor("#D32F2F") // Red color for emphasis
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val warrantyText = "For warranty please visit service center of respective product"
        val warrantyTextHindi = "वारंटी के लिए कृपया सर्विस सेंटर पर जाएं"
        
        // Wrap warranty text to ensure it fits
        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val wrappedWarrantyEN = wrapText(warrantyText, maxWidth - 20, warrantyNotePaint)
        for (line in wrappedWarrantyEN) {
            val warrantyWidth = warrantyNotePaint.measureText(line)
            canvas.drawText(line, (PAGE_WIDTH - warrantyWidth) / 2, yPos, warrantyNotePaint)
            yPos += LINE_HEIGHT - 5
        }
        
        // Hindi warranty note
        val warrantyHindiPaint = Paint(hindiPaint).apply {
            color = Color.parseColor("#D32F2F")
            textSize = 10f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val wrappedWarrantyHI = wrapText(warrantyTextHindi, maxWidth - 20, warrantyHindiPaint)
        for (line in wrappedWarrantyHI) {
            val warrantyWidth = warrantyHindiPaint.measureText(line)
            canvas.drawText(line, (PAGE_WIDTH - warrantyWidth) / 2, yPos, warrantyHindiPaint)
            yPos += LINE_HEIGHT - 5
        }

        yPos += 5

        // Thank you message
        val smallThankyouPaint = Paint(normalPaint).apply {
            textSize = 11f
        }
        val thankYouText = "Thank you for your business!"
        val thankYouWidth = smallThankyouPaint.measureText(thankYouText)
        canvas.drawText(thankYouText, (PAGE_WIDTH - thankYouWidth) / 2, yPos, smallThankyouPaint)
        yPos += LINE_HEIGHT - 5

        // Standard terms and conditions
        val termsPaint = Paint(smallPaint).apply {
            textSize = 9f
        }
        val termsText = if (!customerCopy) {
            "OWNER COPY - For internal records only"
        } else {
            "Computer-generated invoice"
        }
        val termsWidth = termsPaint.measureText(termsText)
        canvas.drawText(termsText, (PAGE_WIDTH - termsWidth) / 2, yPos, termsPaint)
    }

    /**
     * Wrap text to fit within a given width
     */
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    /**
     * Generate text-based receipt for thermal printers
     */
    fun generateTextReceipt(
        transaction: Transaction,
        items: List<TransactionItem>,
        shopName: String,
        shopAddress: String,
        shopPhone: String,
        shopGst: String
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val transactionDate = dateFormat.format(Date(transaction.transactionDate.toEpochMilliseconds()))

        return buildString {
            appendLine("================================")
            appendLine(shopName.center(32))
            appendLine(shopAddress.center(32))
            if (shopPhone.isNotBlank()) {
                appendLine("Ph: ${shopPhone}".center(32))
            }
            if (shopGst.isNotBlank()) {
                appendLine("GSTIN: ${shopGst}".center(32))
            }
            appendLine("================================")
            appendLine("Invoice: ${transaction.transactionNumber}")
            appendLine("Date: $transactionDate")
            
            if (!transaction.customerName.isNullOrBlank()) {
                appendLine("--------------------------------")
                appendLine("Customer: ${transaction.customerName}")
                if (!transaction.customerPhone.isNullOrBlank()) {
                    appendLine("Phone: ${transaction.customerPhone}")
                }
            }
            
            appendLine("================================")
            appendLine("Item                 Qty   Amount")
            appendLine("--------------------------------")
            
            items.forEach { item ->
                val name = item.productName.take(20).padEnd(20)
                val qty = item.quantity.toString().padStart(3)
                val amount = item.totalPrice.toString().padStart(7)
                appendLine("$name $qty $amount")
            }
            
            appendLine("================================")
            appendLine("Subtotal:".padEnd(25) + "₹${transaction.subtotal}".padStart(7))
            
            if (transaction.discountAmount > BigDecimal.ZERO) {
                appendLine("Discount:".padEnd(25) + "-₹${transaction.discountAmount}".padStart(7))
            }
            
            if (transaction.taxAmount > BigDecimal.ZERO) {
                val taxLabel = if (transaction.taxRate > 0) "GST (${transaction.taxRate}%)" else "Tax"
                appendLine("$taxLabel:".padEnd(25) + "₹${transaction.taxAmount}".padStart(7))
            }
            
            appendLine("--------------------------------")
            appendLine("TOTAL:".padEnd(25) + "₹${transaction.grandTotal}".padStart(7))
            appendLine("================================")
            val paymentMethodText = when (transaction.paymentMethod.name) {
                "EMI" -> "Credit on Sale/EMI"
                else -> transaction.paymentMethod.name
            }
            appendLine("Payment: $paymentMethodText")
            appendLine("Status: ${transaction.paymentStatus.name}")
            appendLine()
            appendLine("   Thank you for your business!  ")
            appendLine()
            appendLine()
        }
    }

    private fun String.center(width: Int): String {
        if (this.length >= width) return this
        val padding = (width - this.length) / 2
        return " ".repeat(padding) + this
    }
}
