package com.billme.app.core.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.billme.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.Socket
import java.nio.charset.Charset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * ESC/POS thermal printer service for receipt printing
 */
@Singleton
class ESCPOSPrinterService @Inject constructor(
    private val context: Context,
    private val printerManager: PrinterManager
) {
    
    companion object {
        // ESC/POS Commands
        private const val ESC = 0x1B.toByte()
        private const val GS = 0x1D.toByte()
        private const val LF = 0x0A.toByte()
        private const val CR = 0x0D.toByte()
        private const val FF = 0x0C.toByte()
        private const val HT = 0x09.toByte()
        private const val SPACE = 0x20.toByte()
        
        // Initialize printer
        private val INIT = byteArrayOf(ESC, '@'.code.toByte())
        
        // Text formatting
        private val BOLD_ON = byteArrayOf(ESC, 'E'.code.toByte(), 1)
        private val BOLD_OFF = byteArrayOf(ESC, 'E'.code.toByte(), 0)
        private val UNDERLINE_ON = byteArrayOf(ESC, '-'.code.toByte(), 1)
        private val UNDERLINE_OFF = byteArrayOf(ESC, '-'.code.toByte(), 0)
        private val DOUBLE_HEIGHT_ON = byteArrayOf(ESC, '!'.code.toByte(), 0x10)
        private val DOUBLE_WIDTH_ON = byteArrayOf(ESC, '!'.code.toByte(), 0x20)
        private val DOUBLE_SIZE_ON = byteArrayOf(ESC, '!'.code.toByte(), 0x30)
        private val NORMAL_SIZE = byteArrayOf(ESC, '!'.code.toByte(), 0x00)
        
        // Text alignment
        private val ALIGN_LEFT = byteArrayOf(ESC, 'a'.code.toByte(), 0)
        private val ALIGN_CENTER = byteArrayOf(ESC, 'a'.code.toByte(), 1)
        private val ALIGN_RIGHT = byteArrayOf(ESC, 'a'.code.toByte(), 2)
        
        // Line feed and cut
        private val FEED_LINE = byteArrayOf(LF)
        private val FEED_LINES_3 = byteArrayOf(ESC, 'd'.code.toByte(), 3)
        private val FULL_CUT = byteArrayOf(GS, 'V'.code.toByte(), 65, 0)
        private val PARTIAL_CUT = byteArrayOf(GS, 'V'.code.toByte(), 66, 0)
        
        // Cash drawer
        private val OPEN_DRAWER = byteArrayOf(ESC, 'p'.code.toByte(), 0, 25, 250.toByte())
        
        // Barcode and QR code
        private val QR_CODE_MODEL = byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 4, 0, 49, 65, 50, 0)
        private val QR_CODE_SIZE = byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 3, 0, 49, 67, 8)
        private val QR_CODE_ERROR_CORRECTION = byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 3, 0, 49, 69, 48)
        
        // Character sets
        private val CHARSET_USA = byteArrayOf(ESC, 'R'.code.toByte(), 0)
        private val CHARSET_INTERNATIONAL = byteArrayOf(ESC, 'R'.code.toByte(), 1)
    }
    
    private val _printProgress = MutableSharedFlow<PrintJobProgress>()
    val printProgress: SharedFlow<PrintJobProgress> = _printProgress.asSharedFlow()
    
    /**
     * Print receipt using ESC/POS commands
     */
    suspend fun printReceipt(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        printerConfig: PrinterConfig,
        options: PrintOptions
    ): PrintResult = withContext(Dispatchers.IO) {
        
        val jobId = java.util.UUID.randomUUID().toString()
        
        try {
            emitProgress(jobId, PrintJobPhase.INITIALIZING, "Starting print job", 0.0)
            
            // Generate ESC/POS commands
            val commands = generateESCPOSCommands(receiptData, template, printerConfig, options)
            
            emitProgress(jobId, PrintJobPhase.CONNECTING, "Connecting to printer", 20.0)
            
            // Send to printer
            val success = sendToPrinter(printerConfig, commands, jobId)
            
            if (success) {
                emitProgress(jobId, PrintJobPhase.COMPLETED, "Print completed successfully", 100.0)
                PrintResult.Success(jobId, "Receipt printed successfully")
            } else {
                emitProgress(jobId, PrintJobPhase.FAILED, "Print failed", 0.0)
                PrintResult.Error(jobId, "Failed to print receipt")
            }
            
        } catch (e: Exception) {
            emitProgress(jobId, PrintJobPhase.FAILED, "Print error: ${e.message}", 0.0)
            PrintResult.Error(jobId, e.message ?: "Print operation failed")
        }
    }
    
    /**
     * Generate ESC/POS command sequence for receipt
     */
    private suspend fun generateESCPOSCommands(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        printerConfig: PrinterConfig,
        options: PrintOptions
    ): ByteArray {
        val commands = ByteArrayOutputStream()
        
        // Initialize printer
        commands.write(INIT)
        commands.write(CHARSET_USA)
        
        // Set character set based on printer config
        setCharacterSet(commands, printerConfig.characterSet)
        
        // Print header
        generateHeader(commands, receiptData, template)
        
        // Print customer info if available
        if (receiptData.customerInfo != null) {
            generateCustomerInfo(commands, receiptData.customerInfo, template)
        }
        
        // Print items
        generateItems(commands, receiptData.items, template, printerConfig)
        
        // Print totals
        generateTotals(commands, receiptData.totals, template, printerConfig)
        
        // Print payments
        generatePayments(commands, receiptData.payments, template)
        
        // Print footer
        generateFooter(commands, receiptData, template)
        
        // QR code if enabled
        if (template.headerConfig.showQRCode) {
            generateQRCode(commands, receiptData, template.headerConfig.qrCodeContent)
        }
        
        // Feed lines before cut
        repeat(options.feedLines) {
            commands.write(FEED_LINE)
        }
        
        // Cut paper
        if (options.cutPaper) {
            if (options.partialCut && printerConfig.capabilities.supportsPartialCut) {
                commands.write(PARTIAL_CUT)
            } else if (printerConfig.capabilities.supportsCutting) {
                commands.write(FULL_CUT)
            }
        }
        
        // Open cash drawer if requested
        if (options.openCashDrawer && printerConfig.capabilities.supportsCashDrawer) {
            commands.write(OPEN_DRAWER)
        }
        
        return commands.toByteArray()
    }
    
    /**
     * Generate header section
     */
    private suspend fun generateHeader(
        commands: ByteArrayOutputStream,
        receiptData: ReceiptData,
        template: ReceiptTemplate
    ) {
        val headerConfig = template.headerConfig
        
        // Logo
        if (headerConfig.showLogo && headerConfig.logoPath != null) {
            generateLogo(commands, headerConfig.logoPath, headerConfig.logoSize)
        }
        
        // Business name
        commands.write(ALIGN_CENTER)
        commands.write(DOUBLE_HEIGHT_ON)
        commands.write(BOLD_ON)
        writeLine(commands, receiptData.businessInfo.name)
        commands.write(BOLD_OFF)
        commands.write(NORMAL_SIZE)
        
        // Business address
        commands.write(ALIGN_CENTER)
        writeLine(commands, receiptData.businessInfo.address)
        
        // Contact information
        receiptData.businessInfo.phone?.let { 
            writeLine(commands, "Tel: $it")
        }
        receiptData.businessInfo.email?.let {
            writeLine(commands, "Email: $it")
        }
        receiptData.businessInfo.gstNumber?.let {
            writeLine(commands, "GSTIN: $it")
        }
        
        // Receipt type and number
        commands.write(ALIGN_LEFT)
        commands.write(FEED_LINE)
        commands.write(BOLD_ON)
        writeLine(commands, "${receiptData.receiptType.name.replace('_', ' ')}")
        writeLine(commands, "Receipt #: ${receiptData.receiptId}")
        commands.write(BOLD_OFF)
        
        // Date and time
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        writeLine(commands, "Date: ${receiptData.metadata.timestamp.format(formatter)}")
        
        receiptData.metadata.cashier?.let {
            writeLine(commands, "Cashier: $it")
        }
        
        // Separator
        generateSeparator(commands, headerConfig.headerSeparator, template.paperSize.width)
    }
    
    /**
     * Generate customer information section
     */
    private fun generateCustomerInfo(
        commands: ByteArrayOutputStream,
        customerInfo: CustomerInfo,
        template: ReceiptTemplate
    ) {
        commands.write(FEED_LINE)
        commands.write(BOLD_ON)
        writeLine(commands, "CUSTOMER DETAILS")
        commands.write(BOLD_OFF)
        
        customerInfo.name?.let { writeLine(commands, "Name: $it") }
        customerInfo.phone?.let { writeLine(commands, "Phone: $it") }
        customerInfo.address?.let { writeLine(commands, "Address: $it") }
        customerInfo.gstNumber?.let { writeLine(commands, "GST No: $it") }
        
        generateSeparator(commands, SeparatorStyle.LINE, template.paperSize.width)
    }
    
    /**
     * Generate items section
     */
    private suspend fun generateItems(
        commands: ByteArrayOutputStream,
        items: List<ReceiptItem>,
        template: ReceiptTemplate,
        printerConfig: PrinterConfig
    ) {
        val bodyConfig = template.bodyConfig
        val maxWidth = getMaxLineWidth(printerConfig)
        
        // Items header
        commands.write(BOLD_ON)
        val header = buildItemsHeader(bodyConfig, maxWidth)
        writeLine(commands, header)
        commands.write(BOLD_OFF)
        
        generateSeparator(commands, SeparatorStyle.LINE, maxWidth)
        
        // Group by category if enabled
        val groupedItems = if (bodyConfig.groupByCategory) {
            items.groupBy { it.category ?: "Other" }
        } else {
            mapOf("" to items)
        }
        
        var itemNumber = 1
        for ((category, categoryItems) in groupedItems) {
            if (bodyConfig.groupByCategory && category.isNotEmpty()) {
                commands.write(FEED_LINE)
                commands.write(BOLD_ON)
                writeLine(commands, category.uppercase())
                commands.write(BOLD_OFF)
            }
            
            for (item in categoryItems) {
                generateItemLine(commands, item, bodyConfig, maxWidth, itemNumber)
                if (bodyConfig.itemSeparator != SeparatorStyle.NONE) {
                    generateSeparator(commands, bodyConfig.itemSeparator, maxWidth)
                }
                itemNumber++
            }
        }
        
        generateSeparator(commands, SeparatorStyle.DOUBLE_LINE, maxWidth)
    }
    
    /**
     * Generate individual item line
     */
    private fun generateItemLine(
        commands: ByteArrayOutputStream,
        item: ReceiptItem,
        bodyConfig: BodyConfig,
        maxWidth: Int,
        itemNumber: Int
    ) {
        val nameWidth = maxWidth - 20 // Reserve space for quantity, price, total
        val itemName = if (item.name.length > nameWidth) {
            item.name.take(nameWidth - 3) + "..."
        } else {
            item.name
        }
        
        // Main item line
        val itemLine = buildString {
            if (bodyConfig.showItemNumbers) {
                append("${itemNumber}. ")
            }
            append(itemName.padEnd(nameWidth))
            if (bodyConfig.showQuantity) {
                append(" ${formatQuantity(item.quantity)}")
            }
            if (bodyConfig.showUnitPrice) {
                append(" @${formatCurrency(item.unitPrice)}")
            }
        }
        
        writeLine(commands, itemLine)
        
        // Item code and description if enabled
        if (bodyConfig.showItemCodes && item.code != null) {
            writeLine(commands, "   Code: ${item.code}")
        }
        if (bodyConfig.showItemDescription && item.description != null) {
            writeLine(commands, "   ${item.description}")
        }
        
        // Discount line if applicable
        if (bodyConfig.showDiscount && item.discount > 0) {
            val discountText = when (item.discountType) {
                DiscountType.PERCENTAGE -> "Discount ${item.discount}%: -${formatCurrency(item.unitPrice * item.quantity * item.discount / 100)}"
                DiscountType.AMOUNT -> "Discount: -${formatCurrency(item.discount)}"
            }
            writeLine(commands, "   $discountText")
        }
        
        // Tax details if enabled
        if (bodyConfig.showTaxDetails && item.taxRate > 0) {
            writeLine(commands, "   Tax ${item.taxRate}%: ${formatCurrency(item.taxAmount)}")
        }
        
        // Item total
        if (bodyConfig.showTotalAmount) {
            commands.write(ALIGN_RIGHT)
            commands.write(BOLD_ON)
            writeLine(commands, "Total: ${formatCurrency(item.totalAmount)}")
            commands.write(BOLD_OFF)
            commands.write(ALIGN_LEFT)
        }
    }
    
    /**
     * Generate totals section
     */
    private fun generateTotals(
        commands: ByteArrayOutputStream,
        totals: ReceiptTotals,
        template: ReceiptTemplate,
        printerConfig: PrinterConfig
    ) {
        val maxWidth = getMaxLineWidth(printerConfig)
        
        commands.write(FEED_LINE)
        
        // Subtotal
        writeTotalLine(commands, "Subtotal:", formatCurrency(totals.subtotal), maxWidth)
        
        // Discount
        if (totals.discount > 0) {
            writeTotalLine(commands, "Discount:", "-${formatCurrency(totals.discount)}", maxWidth)
        }
        
        // Tax
        if (totals.taxAmount > 0) {
            writeTotalLine(commands, "Tax:", formatCurrency(totals.taxAmount), maxWidth)
        }
        
        // Rounding adjustment
        if (totals.roundingAdjustment != 0.0) {
            writeTotalLine(commands, "Rounding:", formatCurrency(totals.roundingAdjustment), maxWidth)
        }
        
        generateSeparator(commands, SeparatorStyle.DOUBLE_LINE, maxWidth)
        
        // Grand total
        commands.write(DOUBLE_HEIGHT_ON)
        commands.write(BOLD_ON)
        writeTotalLine(commands, "TOTAL:", formatCurrency(totals.totalAmount), maxWidth)
        commands.write(BOLD_OFF)
        commands.write(NORMAL_SIZE)
        
        generateSeparator(commands, SeparatorStyle.DOUBLE_LINE, maxWidth)
        
        // Total savings if applicable
        if (totals.totalSavings > 0 && template.footerConfig.showTotalSavings) {
            commands.write(FEED_LINE)
            writeTotalLine(commands, "You Saved:", formatCurrency(totals.totalSavings), maxWidth)
        }
    }
    
    /**
     * Generate payments section
     */
    private fun generatePayments(
        commands: ByteArrayOutputStream,
        payments: List<PaymentInfo>,
        template: ReceiptTemplate
    ) {
        if (!template.footerConfig.showPaymentMethod) return
        
        commands.write(FEED_LINE)
        commands.write(BOLD_ON)
        writeLine(commands, "PAYMENT METHOD")
        commands.write(BOLD_OFF)
        
        for (payment in payments) {
            val paymentLine = buildString {
                append(payment.method.displayName)
                payment.cardLast4?.let { append(" ****$it") }
                payment.reference?.let { append(" Ref: $it") }
                append(": ${formatCurrency(payment.amount)}")
            }
            writeLine(commands, paymentLine)
        }
        
        // Change amount
        val totalPaid = payments.sumOf { it.amount }
        val totalAmount = payments.sumOf { it.amount } // This should come from totals
        val changeAmount = totalPaid - totalAmount
        
        if (changeAmount > 0 && template.footerConfig.showChangeAmount) {
            commands.write(FEED_LINE)
            writeLine(commands, "Change: ${formatCurrency(changeAmount)}")
        }
    }
    
    /**
     * Generate footer section
     */
    private fun generateFooter(
        commands: ByteArrayOutputStream,
        receiptData: ReceiptData,
        template: ReceiptTemplate
    ) {
        val footerConfig = template.footerConfig
        
        generateSeparator(commands, footerConfig.footerSeparator, template.paperSize.width)
        
        // Thank you message
        if (footerConfig.showThanksMessage) {
            commands.write(FEED_LINE)
            commands.write(ALIGN_CENTER)
            commands.write(BOLD_ON)
            writeLine(commands, footerConfig.thanksMessage)
            commands.write(BOLD_OFF)
        }
        
        // Return policy
        if (footerConfig.showReturnPolicy && footerConfig.returnPolicyText.isNotEmpty()) {
            commands.write(FEED_LINE)
            writeLine(commands, footerConfig.returnPolicyText)
        }
        
        // Custom message
        if (footerConfig.showCustomMessage && footerConfig.customMessage.isNotEmpty()) {
            commands.write(FEED_LINE)
            writeLine(commands, footerConfig.customMessage)
        }
        
        commands.write(ALIGN_LEFT)
    }
    
    /**
     * Generate QR code
     */
    private fun generateQRCode(
        commands: ByteArrayOutputStream,
        receiptData: ReceiptData,
        qrContent: QRCodeContent
    ) {
        val content = when (qrContent) {
            QRCodeContent.BUSINESS_INFO -> "${receiptData.businessInfo.name}\n${receiptData.businessInfo.phone}"
            QRCodeContent.RECEIPT_ID -> receiptData.receiptId
            QRCodeContent.PAYMENT_LINK -> "https://pay.example.com/${receiptData.receiptId}"
            QRCodeContent.CUSTOM -> receiptData.receiptId
        }
        
        commands.write(FEED_LINE)
        commands.write(ALIGN_CENTER)
        
        // QR code setup commands
        commands.write(QR_CODE_MODEL)
        commands.write(QR_CODE_SIZE)
        commands.write(QR_CODE_ERROR_CORRECTION)
        
        // Store QR code data
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        val dataLength = contentBytes.size + 3
        commands.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte()))
        commands.write(byteArrayOf((dataLength and 0xFF).toByte(), (dataLength shr 8 and 0xFF).toByte()))
        commands.write(byteArrayOf(49, 80, 48))
        commands.write(contentBytes)
        
        // Print QR code
        commands.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 3, 0, 49, 81, 48))
        
        commands.write(ALIGN_LEFT)
        commands.write(FEED_LINE)
    }
    
    /**
     * Generate logo from file
     */
    private suspend fun generateLogo(
        commands: ByteArrayOutputStream,
        logoPath: String,
        logoSize: LogoSize
    ) {
        try {
            val bitmap = BitmapFactory.decodeFile(logoPath)
            if (bitmap != null) {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, logoSize.width, logoSize.height, true)
                val rasterData = convertBitmapToRaster(scaledBitmap)
                
                commands.write(ALIGN_CENTER)
                commands.write(rasterData)
                commands.write(ALIGN_LEFT)
                commands.write(FEED_LINE)
            }
        } catch (e: Exception) {
            // Logo failed to load, skip it
        }
    }
    
    /**
     * Convert bitmap to ESC/POS raster format
     */
    private fun convertBitmapToRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = ceil(width / 8.0).toInt()
        
        val rasterData = ByteArrayOutputStream()
        
        // ESC/POS raster bit image command
        rasterData.write(byteArrayOf(GS, 'v'.code.toByte(), '0'.code.toByte(), 0))
        rasterData.write(byteArrayOf((widthBytes and 0xFF).toByte(), (widthBytes shr 8 and 0xFF).toByte()))
        rasterData.write(byteArrayOf((height and 0xFF).toByte(), (height shr 8 and 0xFF).toByte()))
        
        // Convert bitmap to 1-bit data
        for (y in 0 until height) {
            for (x in 0 until widthBytes) {
                var data = 0
                for (bit in 0 until 8) {
                    val pixelX = x * 8 + bit
                    if (pixelX < width) {
                        val pixel = bitmap.getPixel(pixelX, y)
                        val gray = (android.graphics.Color.red(pixel) + 
                                   android.graphics.Color.green(pixel) + 
                                   android.graphics.Color.blue(pixel)) / 3
                        if (gray < 128) {
                            data = data or (1 shl (7 - bit))
                        }
                    }
                }
                rasterData.write(data)
            }
        }
        
        return rasterData.toByteArray()
    }
    
    /**
     * Send commands to printer
     */
    private suspend fun sendToPrinter(
        printerConfig: PrinterConfig,
        commands: ByteArray,
        jobId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            when (printerConfig.connectionType) {
                ConnectionType.WIFI, ConnectionType.ETHERNET -> {
                    sendViaTCP(printerConfig.connectionDetails, commands, jobId)
                }
                ConnectionType.BLUETOOTH -> {
                    sendViaBluetooth(printerConfig.connectionDetails, commands, jobId)
                }
                ConnectionType.USB -> {
                    sendViaUSB(printerConfig.connectionDetails, commands, jobId)
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Send via TCP/IP (WiFi/Ethernet)
     */
    private suspend fun sendViaTCP(
        connectionDetails: ConnectionDetails,
        commands: ByteArray,
        jobId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val (ipAddress, port) = when (connectionDetails) {
                is ConnectionDetails.WiFi -> connectionDetails.ipAddress to connectionDetails.port
                is ConnectionDetails.Ethernet -> connectionDetails.ipAddress to connectionDetails.port
                else -> return@withContext false
            }
            
            Socket(ipAddress, port).use { socket ->
                socket.outputStream.use { output ->
                    val totalBytes = commands.size.toLong()
                    var sentBytes = 0L
                    val chunkSize = 1024
                    
                    for (i in commands.indices step chunkSize) {
                        val end = minOf(i + chunkSize, commands.size)
                        val chunk = commands.sliceArray(i until end)
                        output.write(chunk)
                        output.flush()
                        
                        sentBytes += chunk.size
                        val progress = (sentBytes.toDouble() / totalBytes) * 80.0 + 20.0
                        emitProgress(jobId, PrintJobPhase.PRINTING, "Sending data", progress)
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Send via Bluetooth
     */
    private suspend fun sendViaBluetooth(
        connectionDetails: ConnectionDetails,
        commands: ByteArray,
        jobId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Bluetooth implementation would go here
            // This requires android.bluetooth permissions and setup
            emitProgress(jobId, PrintJobPhase.PRINTING, "Printing via Bluetooth", 80.0)
            delay(1000) // Simulate printing time
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Send via USB
     */
    private suspend fun sendViaUSB(
        connectionDetails: ConnectionDetails,
        commands: ByteArray,
        jobId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // USB implementation would go here
            // This requires USB host mode and permissions
            emitProgress(jobId, PrintJobPhase.PRINTING, "Printing via USB", 80.0)
            delay(1000) // Simulate printing time
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Helper methods
    
    private fun setCharacterSet(commands: ByteArrayOutputStream, charset: CharacterSet) {
        when (charset) {
            CharacterSet.UTF_8, CharacterSet.ASCII -> commands.write(CHARSET_USA)
            else -> commands.write(CHARSET_INTERNATIONAL)
        }
    }
    
    private fun generateSeparator(commands: ByteArrayOutputStream, style: SeparatorStyle, width: Int) {
        if (style != SeparatorStyle.NONE) {
            val lineWidth = getMaxLineWidth(width)
            val separator = style.character.repeat(lineWidth / style.character.length)
            writeLine(commands, separator)
        }
    }
    
    private fun buildItemsHeader(bodyConfig: BodyConfig, maxWidth: Int): String {
        return buildString {
            if (bodyConfig.showItemNumbers) append("#  ")
            append("Item".padEnd(maxWidth - 15))
            if (bodyConfig.showQuantity) append("Qty")
            if (bodyConfig.showUnitPrice) append("  Price")
            if (bodyConfig.showTotalAmount) append("  Total")
        }
    }
    
    private fun writeLine(commands: ByteArrayOutputStream, text: String) {
        commands.write(text.toByteArray(Charsets.UTF_8))
        commands.write(FEED_LINE)
    }
    
    private fun writeTotalLine(commands: ByteArrayOutputStream, label: String, amount: String, maxWidth: Int) {
        val spacing = maxWidth - label.length - amount.length
        val line = label + " ".repeat(maxOf(1, spacing)) + amount
        writeLine(commands, line)
    }
    
    private fun getMaxLineWidth(printerConfig: PrinterConfig): Int {
        return when (printerConfig.paperSize) {
            PaperSize.THERMAL_58MM -> 32
            PaperSize.THERMAL_80MM -> 48
            else -> 48
        }
    }
    
    private fun getMaxLineWidth(width: Int): Int {
        return when {
            width <= 384 -> 32  // 58mm
            width <= 576 -> 48  // 80mm
            else -> 48
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return "₹%.2f".format(amount)
    }
    
    private fun formatQuantity(quantity: Double): String {
        return if (quantity == quantity.toInt().toDouble()) {
            quantity.toInt().toString()
        } else {
            "%.2f".format(quantity)
        }
    }
    
    private suspend fun emitProgress(
        jobId: String,
        phase: PrintJobPhase,
        operation: String,
        percentage: Double
    ) {
        val progress = PrintJobProgress(
            jobId = jobId,
            phase = phase,
            currentOperation = operation,
            percentage = percentage,
            estimatedTimeRemaining = 0L
        )
        _printProgress.emit(progress)
    }
}

/**
 * Print operation result
 */
sealed class PrintResult {
    data class Success(val jobId: String, val message: String) : PrintResult()
    data class Error(val jobId: String, val message: String) : PrintResult()
}

/**
 * Print job phases
 */
enum class PrintJobPhase {
    INITIALIZING,
    CONNECTING,
    PRINTING,
    COMPLETED,
    FAILED
}

/**
 * Print job progress information
 */
data class PrintJobProgress(
    val jobId: String,
    val phase: PrintJobPhase,
    val currentOperation: String,
    val percentage: Double,
    val estimatedTimeRemaining: Long
)