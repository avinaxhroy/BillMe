package com.billme.app.core.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.billme.app.data.model.*
import com.billme.app.core.util.formatLocale
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.*
import com.itextpdf.layout.properties.TextAlignment as ITextAlignment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF invoice generator for professional A4 invoices
 */
@Singleton
class PDFInvoiceGenerator @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val MARGIN_LEFT = 36f // 0.5 inch
        private const val MARGIN_RIGHT = 36f
        private const val MARGIN_TOP = 72f // 1 inch
        private const val MARGIN_BOTTOM = 72f
        
        private val HEADER_COLOR = DeviceRgb(41, 128, 185) // Professional blue
        private val ACCENT_COLOR = DeviceRgb(52, 152, 219)
        private val TEXT_COLOR = DeviceRgb(44, 62, 80) // Dark gray
        private val LIGHT_GRAY = DeviceRgb(236, 240, 241)
        
        private const val LOGO_MAX_HEIGHT = 80f
        private const val LOGO_MAX_WIDTH = 200f
        
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            currency = Currency.getInstance("INR")
        }
    }
    
    private val _pdfProgress = MutableSharedFlow<PDFGenerationProgress>()
    val pdfProgress: SharedFlow<PDFGenerationProgress> = _pdfProgress.asSharedFlow()
    
    /**
     * Generate PDF invoice
     */
    suspend fun generateInvoice(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        outputPath: String,
        watermark: String? = null
    ): PDFGenerationResult = withContext(Dispatchers.IO) {
        
        val jobId = UUID.randomUUID().toString()
        
        try {
            emitProgress(jobId, PDFPhase.INITIALIZING, "Starting PDF generation", 0.0)
            
            // Create PDF document
            val pdfWriter = PdfWriter(outputPath)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4)
            
            // Set margins
            document.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT)
            
            // Generate document content
            generateDocumentContent(document, receiptData, template, jobId, watermark)
            
            // Close document
            document.close()
            pdfDocument.close()
            pdfWriter.close()
            
            emitProgress(jobId, PDFPhase.COMPLETED, "PDF generated successfully", 100.0)
            
            PDFGenerationResult.Success(
                jobId = jobId,
                filePath = outputPath,
                message = "Invoice PDF generated successfully"
            )
            
        } catch (e: Exception) {
            emitProgress(jobId, PDFPhase.FAILED, "PDF generation failed: ${e.message}", 0.0)
            PDFGenerationResult.Error(jobId, e.message ?: "PDF generation failed")
        }
    }
    
    /**
     * Generate document content
     */
    private suspend fun generateDocumentContent(
        document: Document,
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        jobId: String,
        watermark: String?
    ) {
        // Fonts
        val headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
        val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)
        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
        
        emitProgress(jobId, PDFPhase.GENERATING_HEADER, "Creating header", 10.0)
        
        // Header section
        generateHeader(document, receiptData, template, headerFont, regularFont)
        
        emitProgress(jobId, PDFPhase.GENERATING_CONTENT, "Adding invoice details", 30.0)
        
        // Invoice details section
        generateInvoiceDetails(document, receiptData, boldFont, regularFont)
        
        emitProgress(jobId, PDFPhase.GENERATING_CONTENT, "Adding customer information", 40.0)
        
        // Customer information
        if (receiptData.customerInfo != null) {
            generateCustomerInfo(document, receiptData, boldFont, regularFont)
        }
        
        emitProgress(jobId, PDFPhase.GENERATING_CONTENT, "Creating items table", 50.0)
        
        // Items table
        generateItemsTable(document, receiptData.items, template, boldFont, regularFont)
        
        emitProgress(jobId, PDFPhase.GENERATING_CONTENT, "Adding totals", 70.0)
        
        // Totals section
        generateTotals(document, receiptData.totals, boldFont, regularFont)
        
        emitProgress(jobId, PDFPhase.GENERATING_CONTENT, "Adding payment information", 80.0)
        
        // Payment information
        generatePaymentInfo(document, receiptData.payments, boldFont, regularFont)
        
        emitProgress(jobId, PDFPhase.GENERATING_FOOTER, "Creating footer", 90.0)
        
        // Footer
        generateFooter(document, receiptData, template, regularFont)
        
        // Watermark if specified
        if (watermark != null) {
            addWatermark(document, watermark)
        }
        
        // QR Code if enabled
        if (template.headerConfig.showQRCode) {
            generateQRCode(document, receiptData, template.headerConfig.qrCodeContent)
        }
    }
    
    /**
     * Generate header section
     */
    private fun generateHeader(
        document: Document,
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)
        
        // Left side - Business info
        val businessInfoCell = Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingRight(20f)
        
        // Logo if available
        template.headerConfig.logoPath?.let { logoPath ->
            try {
                val logoFile = File(logoPath)
                if (logoFile.exists()) {
                    val imageData = ImageDataFactory.create(logoPath)
                    val logo = Image(imageData)
                        .scaleToFit(LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT)
                        .setMarginBottom(10f)
                    businessInfoCell.add(logo)
                }
            } catch (e: Exception) {
                // Logo failed to load, skip it
            }
        }
        
        // Business name
        val businessName = Paragraph(receiptData.businessInfo.name)
            .setFont(headerFont)
            .setFontSize(20f)
            .setFontColor(HEADER_COLOR)
            .setMarginBottom(5f)
        businessInfoCell.add(businessName)
        
        // Business address
        val address = Paragraph(receiptData.businessInfo.address)
            .setFont(regularFont)
            .setFontSize(10f)
            .setMarginBottom(3f)
        businessInfoCell.add(address)
        
        // Contact information
        receiptData.businessInfo.phone?.let {
            businessInfoCell.add(createContactLine("Phone: $it", regularFont))
        }
        receiptData.businessInfo.email?.let {
            businessInfoCell.add(createContactLine("Email: $it", regularFont))
        }
        receiptData.businessInfo.gstNumber?.let {
            businessInfoCell.add(createContactLine("GSTIN: $it", regularFont))
        }
        
        headerTable.addCell(businessInfoCell)
        
        // Right side - Invoice info
        val invoiceInfoCell = Cell()
            .setBorder(Border.NO_BORDER)
            .setPaddingLeft(20f)
            .setTextAlignment(ITextAlignment.RIGHT)
        
        val invoiceTitle = Paragraph(getReceiptTypeTitle(receiptData.receiptType))
            .setFont(headerFont)
            .setFontSize(24f)
            .setFontColor(HEADER_COLOR)
            .setMarginBottom(10f)
        invoiceInfoCell.add(invoiceTitle)
        
        // Invoice number
        val invoiceNumber = Paragraph("Invoice #: ${receiptData.receiptId}")
            .setFont(regularFont)
            .setFontSize(10f)
            .setMarginBottom(3f)
        invoiceInfoCell.add(invoiceNumber)
        
        // Date
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val invoiceDate = Paragraph("Date: ${receiptData.metadata.timestamp.format(formatter)}")
            .setFont(regularFont)
            .setFontSize(10f)
            .setMarginBottom(3f)
        invoiceInfoCell.add(invoiceDate)
        
        // Time
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val invoiceTime = Paragraph("Time: ${receiptData.metadata.timestamp.format(timeFormatter)}")
            .setFont(regularFont)
            .setFontSize(10f)
            .setMarginBottom(3f)
        invoiceInfoCell.add(invoiceTime)
        
        headerTable.addCell(invoiceInfoCell)
        
        document.add(headerTable)
        document.add(Paragraph().setMarginBottom(20f))
    }
    
    /**
     * Generate invoice details section
     */
    private fun generateInvoiceDetails(
        document: Document,
        receiptData: ReceiptData,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)
            .setMarginBottom(15f)
        
        // Transaction ID
        if (receiptData.transactionId.isNotEmpty()) {
            detailsTable.addCell(createDetailCell("Transaction ID:", receiptData.transactionId, boldFont, regularFont))
        }
        
        // Cashier
        receiptData.metadata.cashier?.let {
            detailsTable.addCell(createDetailCell("Cashier:", it, boldFont, regularFont))
        }
        
        // Terminal
        receiptData.metadata.terminal?.let {
            detailsTable.addCell(createDetailCell("Terminal:", it, boldFont, regularFont))
        }
        
        document.add(detailsTable)
    }
    
    /**
     * Generate customer information section
     */
    private fun generateCustomerInfo(
        document: Document,
        receiptData: ReceiptData,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        val customerInfo = receiptData.customerInfo!!
        
        val billToTitle = Paragraph("BILL TO:")
            .setFont(boldFont)
            .setFontSize(12f)
            .setFontColor(HEADER_COLOR)
            .setMarginBottom(10f)
        document.add(billToTitle)
        
        val customerTable = Table(UnitValue.createPercentArray(floatArrayOf(1f)))
            .setWidth(UnitValue.createPercentValue(50f))
            .setBorder(Border.NO_BORDER)
            .setMarginBottom(20f)
        
        val customerCell = Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(10f)
            .setBackgroundColor(LIGHT_GRAY)
        
        customerInfo.name?.let {
            customerCell.add(createContactLine(it, boldFont, 11f))
        }
        customerInfo.address?.let {
            customerCell.add(createContactLine(it, regularFont))
        }
        customerInfo.phone?.let {
            customerCell.add(createContactLine("Phone: $it", regularFont))
        }
        customerInfo.email?.let {
            customerCell.add(createContactLine("Email: $it", regularFont))
        }
        customerInfo.gstNumber?.let {
            customerCell.add(createContactLine("GST No: $it", regularFont))
        }
        
        customerTable.addCell(customerCell)
        document.add(customerTable)
    }
    
    /**
     * Generate items table
     */
    private fun generateItemsTable(
        document: Document,
        items: List<ReceiptItem>,
        template: ReceiptTemplate,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        val bodyConfig = template.bodyConfig
        
        // Determine column widths based on configuration
        val columnWidths = mutableListOf<Float>()
        val headers = mutableListOf<String>()
        
        if (bodyConfig.showItemNumbers) {
            columnWidths.add(0.5f)
            headers.add("#")
        }
        
        columnWidths.add(3f) // Item name
        headers.add("Description")
        
        if (bodyConfig.showItemCodes) {
            columnWidths.add(1f)
            headers.add("Code")
        }
        
        if (bodyConfig.showQuantity) {
            columnWidths.add(0.8f)
            headers.add("Qty")
        }
        
        if (bodyConfig.showUnitPrice) {
            columnWidths.add(1f)
            headers.add("Rate")
        }
        
        if (bodyConfig.showDiscount) {
            columnWidths.add(1f)
            headers.add("Disc.")
        }
        
        if (bodyConfig.showTaxDetails) {
            columnWidths.add(1f)
            headers.add("Tax")
        }
        
        columnWidths.add(1.2f) // Total
        headers.add("Amount")
        
        val itemsTable = Table(UnitValue.createPercentArray(columnWidths.toFloatArray()))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        // Headers
        headers.forEach { header ->
            val headerCell = Cell()
                .add(Paragraph(header).setFont(boldFont).setFontSize(10f).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(HEADER_COLOR)
                .setTextAlignment(ITextAlignment.CENTER)
                .setPadding(8f)
            itemsTable.addHeaderCell(headerCell)
        }
        
        // Group items by category if enabled
        val groupedItems = if (bodyConfig.groupByCategory) {
            items.groupBy { it.category ?: "Other" }
        } else {
            mapOf("" to items)
        }
        
        var itemNumber = 1
        for ((category, categoryItems) in groupedItems) {
            // Category header
            if (bodyConfig.groupByCategory && category.isNotEmpty()) {
                val categoryCell = Cell(1, columnWidths.size)
                    .add(Paragraph(category.uppercase()).setFont(boldFont).setFontSize(10f))
                    .setBackgroundColor(LIGHT_GRAY)
                    .setPadding(5f)
                itemsTable.addCell(categoryCell)
            }
            
            // Items
            for (item in categoryItems) {
                val cells = mutableListOf<Cell>()
                
                if (bodyConfig.showItemNumbers) {
                    cells.add(createItemCell(itemNumber.toString(), regularFont, ITextAlignment.CENTER))
                }
                
                // Item description with code if needed
                val itemDescription = StringBuilder(item.name)
                if (item.description != null && !bodyConfig.showItemCodes) {
                    itemDescription.append("\n${item.description}")
                }
                cells.add(createItemCell(itemDescription.toString(), regularFont))
                
                if (bodyConfig.showItemCodes) {
                    cells.add(createItemCell(item.code ?: "-", regularFont, ITextAlignment.CENTER))
                }
                
                if (bodyConfig.showQuantity) {
                    cells.add(createItemCell(formatQuantity(item.quantity), regularFont, ITextAlignment.CENTER))
                }
                
                if (bodyConfig.showUnitPrice) {
                    cells.add(createItemCell(formatCurrency(item.unitPrice), regularFont, ITextAlignment.RIGHT))
                }
                
                if (bodyConfig.showDiscount) {
                    val discountText = if (item.discount > 0) {
                        when (item.discountType) {
                            DiscountType.PERCENTAGE -> "${item.discount}%"
                            DiscountType.AMOUNT -> formatCurrency(item.discount)
                        }
                    } else "-"
                    cells.add(createItemCell(discountText, regularFont, ITextAlignment.CENTER))
                }
                
                if (bodyConfig.showTaxDetails) {
                    val taxText = if (item.taxRate > 0) "${item.taxRate}%" else "-"
                    cells.add(createItemCell(taxText, regularFont, ITextAlignment.CENTER))
                }
                
                if (bodyConfig.showTotalAmount) {
                    cells.add(createItemCell(formatCurrency(item.totalAmount), boldFont, ITextAlignment.RIGHT))
                }
                
                cells.forEach { itemsTable.addCell(it) }
                itemNumber++
            }
        }
        
        document.add(itemsTable)
    }
    
    /**
     * Generate totals section
     */
    private fun generateTotals(
        document: Document,
        totals: ReceiptTotals,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(3f, 1f)))
            .setWidth(UnitValue.createPercentValue(50f))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setBorder(Border.NO_BORDER)
            .setMarginBottom(20f)
        
        // Subtotal
        totalsTable.addCell(createTotalLabelCell("Subtotal:", regularFont))
        totalsTable.addCell(createTotalAmountCell(formatCurrency(totals.subtotal), regularFont))
        
        // Discount
        if (totals.discount > 0) {
            totalsTable.addCell(createTotalLabelCell("Discount:", regularFont))
            totalsTable.addCell(createTotalAmountCell("-${formatCurrency(totals.discount)}", regularFont))
        }
        
        // Tax
        if (totals.taxAmount > 0) {
            totalsTable.addCell(createTotalLabelCell("Tax:", regularFont))
            totalsTable.addCell(createTotalAmountCell(formatCurrency(totals.taxAmount), regularFont))
        }
        
        // Rounding adjustment
        if (totals.roundingAdjustment != 0.0) {
            totalsTable.addCell(createTotalLabelCell("Rounding:", regularFont))
            totalsTable.addCell(createTotalAmountCell(formatCurrency(totals.roundingAdjustment), regularFont))
        }
        
        // Grand total
        val totalLabelCell = Cell()
            .add(Paragraph("TOTAL:").setFont(boldFont).setFontSize(12f))
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(HEADER_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(8f)
            .setTextAlignment(ITextAlignment.LEFT)
        
        val totalAmountCell = Cell()
            .add(Paragraph(formatCurrency(totals.totalAmount)).setFont(boldFont).setFontSize(12f))
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(HEADER_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(8f)
            .setTextAlignment(ITextAlignment.RIGHT)
        
        totalsTable.addCell(totalLabelCell)
        totalsTable.addCell(totalAmountCell)
        
        // Total savings
        if (totals.totalSavings > 0) {
            totalsTable.addCell(createTotalLabelCell("You Saved:", boldFont))
            totalsTable.addCell(createTotalAmountCell(formatCurrency(totals.totalSavings), boldFont))
        }
        
        document.add(totalsTable)
    }
    
    /**
     * Generate payment information
     */
    private fun generatePaymentInfo(
        document: Document,
        payments: List<PaymentInfo>,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        if (payments.isEmpty()) return
        
        val paymentTitle = Paragraph("PAYMENT INFORMATION:")
            .setFont(boldFont)
            .setFontSize(12f)
            .setFontColor(HEADER_COLOR)
            .setMarginBottom(10f)
        document.add(paymentTitle)
        
        val paymentTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 2f, 1f)))
            .setWidth(UnitValue.createPercentValue(60f))
            .setMarginBottom(20f)
        
        // Headers
        paymentTable.addHeaderCell(createTableHeaderCell("Payment Method", boldFont))
        paymentTable.addHeaderCell(createTableHeaderCell("Reference", boldFont))
        paymentTable.addHeaderCell(createTableHeaderCell("Amount", boldFont))
        
        // Payment rows
        for (payment in payments) {
            val methodText = buildString {
                append(payment.method.displayName)
                payment.cardLast4?.let { append(" ****$it") }
            }
            
            paymentTable.addCell(createItemCell(methodText, regularFont))
            paymentTable.addCell(createItemCell(payment.reference ?: "-", regularFont))
            paymentTable.addCell(createItemCell(formatCurrency(payment.amount), regularFont, ITextAlignment.RIGHT))
        }
        
        document.add(paymentTable)
    }
    
    /**
     * Generate footer section
     */
    private fun generateFooter(
        document: Document,
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        val footerConfig = template.footerConfig
        
        // Thank you message
        if (footerConfig.showThanksMessage) {
            val thanksMessage = Paragraph(footerConfig.thanksMessage)
                .setFont(regularFont)
                .setFontSize(12f)
                .setFontColor(HEADER_COLOR)
                .setTextAlignment(ITextAlignment.CENTER)
                .setMarginTop(30f)
                .setMarginBottom(10f)
            document.add(thanksMessage)
        }
        
        // Return policy
        if (footerConfig.showReturnPolicy && footerConfig.returnPolicyText.isNotEmpty()) {
            val returnPolicy = Paragraph(footerConfig.returnPolicyText)
                .setFont(regularFont)
                .setFontSize(8f)
                .setTextAlignment(ITextAlignment.CENTER)
                .setMarginBottom(10f)
            document.add(returnPolicy)
        }
        
        // Custom message
        if (footerConfig.showCustomMessage && footerConfig.customMessage.isNotEmpty()) {
            val customMessage = Paragraph(footerConfig.customMessage)
                .setFont(regularFont)
                .setFontSize(9f)
                .setTextAlignment(ITextAlignment.CENTER)
                .setMarginBottom(20f)
            document.add(customMessage)
        }
        
        // Footer separator line
        val separator = Paragraph()
            .setBorderTop(com.itextpdf.layout.borders.SolidBorder(LIGHT_GRAY, 1f))
            .setMarginTop(20f)
            .setMarginBottom(10f)
        document.add(separator)
        
        // Footer information
        val footerInfo = Paragraph("This invoice was generated electronically and is valid without signature.")
            .setFont(regularFont)
            .setFontSize(8f)
            .setFontColor(ColorConstants.GRAY)
            .setTextAlignment(ITextAlignment.CENTER)
        document.add(footerInfo)
    }
    
    /**
     * Add watermark to document
     */
    private fun addWatermark(document: Document, watermarkText: String) {
        // Watermark implementation would require additional iText setup
        // This is a placeholder for watermark functionality
    }
    
    /**
     * Generate QR code
     */
    private fun generateQRCode(
        document: Document,
        receiptData: ReceiptData,
        qrContent: QRCodeContent
    ) {
        // QR code generation would require additional library (like ZXing)
        // This is a placeholder for QR code functionality
        val content = when (qrContent) {
            QRCodeContent.BUSINESS_INFO -> "${receiptData.businessInfo.name}\n${receiptData.businessInfo.phone}"
            QRCodeContent.RECEIPT_ID -> receiptData.receiptId
            QRCodeContent.PAYMENT_LINK -> "https://pay.example.com/${receiptData.receiptId}"
            QRCodeContent.CUSTOM -> receiptData.receiptId
        }
        
        // Add QR code placeholder text
        val qrPlaceholder = Paragraph("QR Code: $content")
            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
            .setFontSize(8f)
            .setTextAlignment(ITextAlignment.RIGHT)
            .setMarginTop(20f)
        document.add(qrPlaceholder)
    }
    
    // Helper methods
    
    private fun createContactLine(
        text: String, 
        font: com.itextpdf.kernel.font.PdfFont, 
        size: Float = 10f
    ): Paragraph {
        return Paragraph(text)
            .setFont(font)
            .setFontSize(size)
            .setMarginBottom(2f)
    }
    
    private fun createDetailCell(
        label: String,
        value: String,
        boldFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ): Cell {
        val paragraph = Paragraph()
            .add(Text(label).setFont(boldFont).setFontSize(10f))
            .add(Text(" $value").setFont(regularFont).setFontSize(10f))
        
        return Cell()
            .add(paragraph)
            .setBorder(Border.NO_BORDER)
            .setPaddingBottom(5f)
    }
    
    private fun createItemCell(
        text: String,
        font: com.itextpdf.kernel.font.PdfFont,
        alignment: ITextAlignment = ITextAlignment.LEFT
    ): Cell {
        return Cell()
            .add(Paragraph(text).setFont(font).setFontSize(9f))
            .setBorder(Border.NO_BORDER)
            .setBorderBottom(com.itextpdf.layout.borders.SolidBorder(LIGHT_GRAY, 0.5f))
            .setPadding(5f)
            .setTextAlignment(alignment)
    }
    
    private fun createTotalLabelCell(
        label: String,
        font: com.itextpdf.kernel.font.PdfFont
    ): Cell {
        return Cell()
            .add(Paragraph(label).setFont(font).setFontSize(10f))
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(3f)
            .setPaddingBottom(3f)
            .setTextAlignment(ITextAlignment.LEFT)
    }
    
    private fun createTotalAmountCell(
        amount: String,
        font: com.itextpdf.kernel.font.PdfFont
    ): Cell {
        return Cell()
            .add(Paragraph(amount).setFont(font).setFontSize(10f))
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(3f)
            .setPaddingBottom(3f)
            .setTextAlignment(ITextAlignment.RIGHT)
    }
    
    private fun createTableHeaderCell(
        text: String,
        font: com.itextpdf.kernel.font.PdfFont
    ): Cell {
        return Cell()
            .add(Paragraph(text).setFont(font).setFontSize(9f).setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(ACCENT_COLOR)
            .setPadding(5f)
            .setTextAlignment(ITextAlignment.CENTER)
    }
    
    private fun getReceiptTypeTitle(receiptType: ReceiptType): String {
        return when (receiptType) {
            ReceiptType.CUSTOMER_COPY -> "INVOICE"
            ReceiptType.OWNER_COPY -> "INVOICE (COPY)"
            ReceiptType.DUPLICATE -> "DUPLICATE INVOICE"
            ReceiptType.INVOICE -> "INVOICE"
            ReceiptType.ESTIMATE -> "ESTIMATE"
            ReceiptType.RETURN_RECEIPT -> "RETURN RECEIPT"
            ReceiptType.PAYMENT_RECEIPT -> "PAYMENT RECEIPT"
            ReceiptType.DELIVERY_CHALLAN -> "DELIVERY CHALLAN"
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }
    
    private fun formatQuantity(quantity: Double): String {
        return if (quantity == quantity.toInt().toDouble()) {
            quantity.toInt().toString()
        } else {
            quantity.formatLocale("%.2f")
        }
    }
    
    private suspend fun emitProgress(
        jobId: String,
        phase: PDFPhase,
        operation: String,
        percentage: Double
    ) {
        val progress = PDFGenerationProgress(
            jobId = jobId,
            phase = phase,
            currentOperation = operation,
            percentage = percentage,
            estimatedTimeRemaining = 0L
        )
        _pdfProgress.emit(progress)
    }
}

/**
 * PDF generation result
 */
sealed class PDFGenerationResult {
    data class Success(
        val jobId: String,
        val filePath: String,
        val message: String
    ) : PDFGenerationResult()
    
    data class Error(
        val jobId: String,
        val message: String
    ) : PDFGenerationResult()
}

/**
 * PDF generation phases
 */
enum class PDFPhase {
    INITIALIZING,
    GENERATING_HEADER,
    GENERATING_CONTENT,
    GENERATING_FOOTER,
    COMPLETED,
    FAILED
}

/**
 * PDF generation progress information
 */
data class PDFGenerationProgress(
    val jobId: String,
    val phase: PDFPhase,
    val currentOperation: String,
    val percentage: Double,
    val estimatedTimeRemaining: Long
)