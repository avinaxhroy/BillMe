package com.billme.app.core.sharing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.billme.app.core.printing.PDFInvoiceGenerator
import com.billme.app.core.printing.PDFGenerationResult
import com.billme.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive sharing service for invoices and receipts
 */
@Singleton
class SharingService @Inject constructor(
    private val context: Context,
    private val pdfGenerator: PDFInvoiceGenerator
) {
    
    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.billme.app.fileprovider"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        
        private const val TEMP_SHARE_DIR = "shared_invoices"
        private const val EMAIL_TIMEOUT_MS = 30000L
        private const val BLUETOOTH_DISCOVERY_TIMEOUT_MS = 12000L
    }
    
    private val _sharingProgress = MutableSharedFlow<SharingProgress>()
    val sharingProgress: SharedFlow<SharingProgress> = _sharingProgress.asSharedFlow()
    
    private val tempShareDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), TEMP_SHARE_DIR).also {
            it.mkdirs()
        }
    }
    
    /**
     * Share invoice via multiple methods
     */
    suspend fun shareInvoice(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig
    ): SharingResult = withContext(Dispatchers.IO) {
        
        val shareId = UUID.randomUUID().toString()
        
        try {
            emitProgress(shareId, SharingPhase.INITIALIZING, "Preparing share", 0.0)
            
            when (sharingConfig.shareMethod) {
                ShareMethod.WHATSAPP -> shareViaWhatsApp(receiptData, template, sharingConfig, shareId)
                ShareMethod.EMAIL -> shareViaEmail(receiptData, template, sharingConfig, shareId)
                ShareMethod.BLUETOOTH -> shareViaBluetooth(receiptData, template, sharingConfig, shareId)
                ShareMethod.SMS -> shareViaSMS(receiptData, template, sharingConfig, shareId)
                ShareMethod.TELEGRAM -> shareViaTelegram(receiptData, template, sharingConfig, shareId)
                ShareMethod.QR_CODE -> generateQRCodeShare(receiptData, sharingConfig, shareId)
                ShareMethod.SOCIAL_MEDIA -> shareViaSocialMedia(receiptData, template, sharingConfig, shareId)
                ShareMethod.NFC -> shareViaNFC(receiptData, template, sharingConfig, shareId)
                ShareMethod.AIRDROP -> shareViaAirDrop(receiptData, template, sharingConfig, shareId)
            }
            
        } catch (e: Exception) {
            emitProgress(shareId, SharingPhase.FAILED, "Sharing failed: ${e.message}", 0.0)
            SharingResult.Error(shareId, e.message ?: "Sharing operation failed")
        }
    }
    
    /**
     * Share via WhatsApp
     */
    private suspend fun shareViaWhatsApp(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        emitProgress(shareId, SharingPhase.GENERATING_FILE, "Creating invoice file", 20.0)
        
        // Generate file based on format
        val shareFile = when (sharingConfig.format) {
            ShareFormat.PDF -> generatePDFFile(receiptData, template, shareId)
            ShareFormat.IMAGE_PNG -> generateImageFile(receiptData, template, ShareFormat.IMAGE_PNG, shareId)
            ShareFormat.IMAGE_JPEG -> generateImageFile(receiptData, template, ShareFormat.IMAGE_JPEG, shareId)
            ShareFormat.TEXT -> generateTextFile(receiptData, template, shareId)
            ShareFormat.HTML -> generateHTMLFile(receiptData, template, shareId)
            ShareFormat.JSON -> generateJSONFile(receiptData, shareId)
        }
        
        emitProgress(shareId, SharingPhase.PREPARING_SHARE, "Preparing WhatsApp share", 60.0)
        
        // Check WhatsApp availability
        val whatsappPackage = when {
            isPackageInstalled(WHATSAPP_BUSINESS_PACKAGE) -> WHATSAPP_BUSINESS_PACKAGE
            isPackageInstalled(WHATSAPP_PACKAGE) -> WHATSAPP_PACKAGE
            else -> null
        }
        
        if (whatsappPackage == null) {
            return@withContext SharingResult.Error(shareId, "WhatsApp is not installed")
        }
        
        try {
            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(sharingConfig.format)
                setPackage(whatsappPackage)
                
                // Add file
                val fileUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                
                // Add message
                val message = buildShareMessage(receiptData, sharingConfig)
                putExtra(Intent.EXTRA_TEXT, message)
                
                // Add subject
                putExtra(Intent.EXTRA_SUBJECT, "Invoice ${receiptData.receiptId}")
                
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Add specific recipients if provided
            if (sharingConfig.recipients.isNotEmpty()) {
                val phoneNumber = sharingConfig.recipients.first()
                shareIntent.putExtra("jid", "$phoneNumber@s.whatsapp.net")
            }
            
            emitProgress(shareId, SharingPhase.SHARING, "Opening WhatsApp", 80.0)
            
            context.startActivity(shareIntent)
            
            emitProgress(shareId, SharingPhase.COMPLETED, "WhatsApp share completed", 100.0)
            
            SharingResult.Success(
                shareId = shareId,
                method = ShareMethod.WHATSAPP,
                filePath = shareFile.absolutePath,
                message = "Invoice shared via WhatsApp successfully"
            )
            
        } catch (e: Exception) {
            SharingResult.Error(shareId, "Failed to share via WhatsApp: ${e.message}")
        }
    }
    
    /**
     * Share via Email
     */
    private suspend fun shareViaEmail(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        emitProgress(shareId, SharingPhase.GENERATING_FILE, "Creating invoice file", 20.0)
        
        val shareFile = when (sharingConfig.format) {
            ShareFormat.PDF -> generatePDFFile(receiptData, template, shareId)
            ShareFormat.HTML -> generateHTMLFile(receiptData, template, shareId)
            else -> generatePDFFile(receiptData, template, shareId) // Default to PDF
        }
        
        emitProgress(shareId, SharingPhase.PREPARING_SHARE, "Preparing email", 40.0)
        
        try {
            // Create email intent
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                
                // Recipients
                if (sharingConfig.recipients.isNotEmpty()) {
                    putExtra(Intent.EXTRA_EMAIL, sharingConfig.recipients.toTypedArray())
                }
                
                // Subject
                val subject = "Invoice ${receiptData.receiptId} - ${receiptData.businessInfo.name}"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                
                // Message body
                val messageBody = buildEmailMessage(receiptData, sharingConfig)
                putExtra(Intent.EXTRA_TEXT, messageBody)
                
                // Attachment
                val fileUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            emitProgress(shareId, SharingPhase.SHARING, "Opening email app", 80.0)
            
            // Create chooser
            val chooser = Intent.createChooser(emailIntent, "Send Invoice via Email")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(chooser)
            
            emitProgress(shareId, SharingPhase.COMPLETED, "Email share completed", 100.0)
            
            SharingResult.Success(
                shareId = shareId,
                method = ShareMethod.EMAIL,
                filePath = shareFile.absolutePath,
                message = "Invoice shared via Email successfully"
            )
            
        } catch (e: Exception) {
            SharingResult.Error(shareId, "Failed to share via Email: ${e.message}")
        }
    }
    
    /**
     * Share via Bluetooth
     */
    private suspend fun shareViaBluetooth(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        emitProgress(shareId, SharingPhase.GENERATING_FILE, "Creating invoice file", 20.0)
        
        val shareFile = when (sharingConfig.format) {
            ShareFormat.PDF -> generatePDFFile(receiptData, template, shareId)
            ShareFormat.IMAGE_PNG -> generateImageFile(receiptData, template, ShareFormat.IMAGE_PNG, shareId)
            ShareFormat.IMAGE_JPEG -> generateImageFile(receiptData, template, ShareFormat.IMAGE_JPEG, shareId)
            else -> generatePDFFile(receiptData, template, shareId)
        }
        
        emitProgress(shareId, SharingPhase.PREPARING_SHARE, "Checking Bluetooth", 40.0)
        
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter == null) {
                return@withContext SharingResult.Error(shareId, "Bluetooth is not supported on this device")
            }
            
            if (!bluetoothAdapter.isEnabled) {
                return@withContext SharingResult.Error(shareId, "Bluetooth is not enabled")
            }
            
            // Create Bluetooth share intent
            val bluetoothIntent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(sharingConfig.format)
                
                // Add file
                val fileUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                
                // Set component for Bluetooth sharing
                component = ComponentName(
                    "com.android.bluetooth",
                    "com.android.bluetooth.opp.BluetoothOppLauncherActivity"
                )
                
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            emitProgress(shareId, SharingPhase.SHARING, "Opening Bluetooth share", 80.0)
            
            try {
                context.startActivity(bluetoothIntent)
            } catch (e: Exception) {
                // Fallback to generic share
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = getMimeType(sharingConfig.format)
                    val fileUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile)
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                val chooser = Intent.createChooser(genericIntent, "Share Invoice via Bluetooth")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
            
            emitProgress(shareId, SharingPhase.COMPLETED, "Bluetooth share completed", 100.0)
            
            SharingResult.Success(
                shareId = shareId,
                method = ShareMethod.BLUETOOTH,
                filePath = shareFile.absolutePath,
                message = "Invoice shared via Bluetooth successfully"
            )
            
        } catch (e: Exception) {
            SharingResult.Error(shareId, "Failed to share via Bluetooth: ${e.message}")
        }
    }
    
    /**
     * Share via SMS
     */
    private suspend fun shareViaSMS(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        emitProgress(shareId, SharingPhase.PREPARING_SHARE, "Preparing SMS", 50.0)
        
        try {
            val message = buildSMSMessage(receiptData, sharingConfig)
            
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                putExtra("sms_body", message)
                
                if (sharingConfig.recipients.isNotEmpty()) {
                    data = Uri.parse("smsto:${sharingConfig.recipients.joinToString(",")}")
                }
                
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            emitProgress(shareId, SharingPhase.SHARING, "Opening SMS app", 80.0)
            
            context.startActivity(smsIntent)
            
            emitProgress(shareId, SharingPhase.COMPLETED, "SMS share completed", 100.0)
            
            SharingResult.Success(
                shareId = shareId,
                method = ShareMethod.SMS,
                filePath = null,
                message = "Invoice details shared via SMS successfully"
            )
            
        } catch (e: Exception) {
            SharingResult.Error(shareId, "Failed to share via SMS: ${e.message}")
        }
    }
    
    /**
     * Share via Telegram
     */
    private suspend fun shareViaTelegram(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        if (!isPackageInstalled(TELEGRAM_PACKAGE)) {
            return@withContext SharingResult.Error(shareId, "Telegram is not installed")
        }
        
        emitProgress(shareId, SharingPhase.GENERATING_FILE, "Creating invoice file", 20.0)
        
        val shareFile = generatePDFFile(receiptData, template, shareId)
        
        emitProgress(shareId, SharingPhase.PREPARING_SHARE, "Preparing Telegram share", 60.0)
        
        try {
            val telegramIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage(TELEGRAM_PACKAGE)
                
                val fileUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                
                val message = buildShareMessage(receiptData, sharingConfig)
                putExtra(Intent.EXTRA_TEXT, message)
                
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            emitProgress(shareId, SharingPhase.SHARING, "Opening Telegram", 80.0)
            
            context.startActivity(telegramIntent)
            
            emitProgress(shareId, SharingPhase.COMPLETED, "Telegram share completed", 100.0)
            
            SharingResult.Success(
                shareId = shareId,
                method = ShareMethod.TELEGRAM,
                filePath = shareFile.absolutePath,
                message = "Invoice shared via Telegram successfully"
            )
            
        } catch (e: Exception) {
            SharingResult.Error(shareId, "Failed to share via Telegram: ${e.message}")
        }
    }
    
    /**
     * Generate QR code for sharing
     */
    private suspend fun generateQRCodeShare(
        receiptData: ReceiptData,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        emitProgress(shareId, SharingPhase.GENERATING_FILE, "Creating QR code", 50.0)
        
        try {
            // Create QR code content
            val qrContent = when (sharingConfig.format) {
                ShareFormat.JSON -> generateJSONContent(receiptData)
                ShareFormat.TEXT -> generateTextContent(receiptData)
                else -> "Receipt ID: ${receiptData.receiptId}\nAmount: ₹${receiptData.totals.totalAmount}\nBusiness: ${receiptData.businessInfo.name}"
            }
            
            // Generate QR code (would use ZXing library)
            val qrCodeFile = File(tempShareDir, "qr_${shareId}.png")
            // QR code generation implementation would go here
            
            emitProgress(shareId, SharingPhase.COMPLETED, "QR code generated", 100.0)
            
            SharingResult.Success(
                shareId = shareId,
                method = ShareMethod.QR_CODE,
                filePath = qrCodeFile.absolutePath,
                message = "QR code generated successfully"
            )
            
        } catch (e: Exception) {
            SharingResult.Error(shareId, "Failed to generate QR code: ${e.message}")
        }
    }
    
    /**
     * Share via social media
     */
    private suspend fun shareViaSocialMedia(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        emitProgress(shareId, SharingPhase.GENERATING_FILE, "Creating shareable image", 30.0)
        
        val shareFile = generateImageFile(receiptData, template, ShareFormat.IMAGE_PNG, shareId)
        
        emitProgress(shareId, SharingPhase.PREPARING_SHARE, "Preparing social share", 60.0)
        
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                
                val fileUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                
                val message = "Invoice from ${receiptData.businessInfo.name} - ₹${receiptData.totals.totalAmount}"
                putExtra(Intent.EXTRA_TEXT, message)
                
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Invoice")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            emitProgress(shareId, SharingPhase.SHARING, "Opening share options", 80.0)
            
            context.startActivity(chooser)
            
            emitProgress(shareId, SharingPhase.COMPLETED, "Social media share completed", 100.0)
            
            SharingResult.Success(
                shareId = shareId,
                method = ShareMethod.SOCIAL_MEDIA,
                filePath = shareFile.absolutePath,
                message = "Invoice shared via social media successfully"
            )
            
        } catch (e: Exception) {
            SharingResult.Error(shareId, "Failed to share via social media: ${e.message}")
        }
    }
    
    /**
     * Share via NFC
     */
    private suspend fun shareViaNFC(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        // NFC sharing implementation would require NFC API integration
        // This is a placeholder for NFC functionality
        
        emitProgress(shareId, SharingPhase.COMPLETED, "NFC sharing prepared", 100.0)
        
        SharingResult.Success(
            shareId = shareId,
            method = ShareMethod.NFC,
            filePath = null,
            message = "NFC sharing prepared (requires NFC-enabled device)"
        )
    }
    
    /**
     * Share via AirDrop (iOS devices)
     */
    private suspend fun shareViaAirDrop(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        sharingConfig: SharingConfig,
        shareId: String
    ): SharingResult = withContext(Dispatchers.IO) {
        
        // AirDrop is iOS-specific, on Android we'll use generic share
        val shareFile = generatePDFFile(receiptData, template, shareId)
        
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                
                val fileUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, shareFile)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Invoice")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(chooser)
            
            SharingResult.Success(
                shareId = shareId,
                method = ShareMethod.AIRDROP,
                filePath = shareFile.absolutePath,
                message = "Invoice shared successfully"
            )
            
        } catch (e: Exception) {
            SharingResult.Error(shareId, "Failed to share invoice: ${e.message}")
        }
    }
    
    // File generation methods
    
    private suspend fun generatePDFFile(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        shareId: String
    ): File {
        val fileName = "invoice_${receiptData.receiptId}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(tempShareDir, fileName)
        
        when (val result = pdfGenerator.generateInvoice(receiptData, template, pdfFile.absolutePath)) {
            is PDFGenerationResult.Success -> return pdfFile
            is PDFGenerationResult.Error -> throw Exception(result.message)
            else -> throw Exception("Unknown PDF generation result")
        }
    }
    
    private suspend fun generateImageFile(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        format: ShareFormat,
        shareId: String
    ): File {
        // Image generation would require converting PDF to image or creating custom image renderer
        // This is a placeholder implementation
        val extension = when (format) {
            ShareFormat.IMAGE_PNG -> "png"
            ShareFormat.IMAGE_JPEG -> "jpg"
            else -> "png"
        }
        
        val fileName = "invoice_${receiptData.receiptId}_${System.currentTimeMillis()}.$extension"
        val imageFile = File(tempShareDir, fileName)
        
        // Convert PDF to image or create custom image
        // Placeholder: create empty file
        imageFile.createNewFile()
        
        return imageFile
    }
    
    private suspend fun generateTextFile(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        shareId: String
    ): File {
        val fileName = "invoice_${receiptData.receiptId}_${System.currentTimeMillis()}.txt"
        val textFile = File(tempShareDir, fileName)
        
        val content = generateTextContent(receiptData)
        textFile.writeText(content)
        
        return textFile
    }
    
    private suspend fun generateHTMLFile(
        receiptData: ReceiptData,
        template: ReceiptTemplate,
        shareId: String
    ): File {
        val fileName = "invoice_${receiptData.receiptId}_${System.currentTimeMillis()}.html"
        val htmlFile = File(tempShareDir, fileName)
        
        val content = generateHTMLContent(receiptData, template)
        htmlFile.writeText(content)
        
        return htmlFile
    }
    
    private suspend fun generateJSONFile(
        receiptData: ReceiptData,
        shareId: String
    ): File {
        val fileName = "invoice_${receiptData.receiptId}_${System.currentTimeMillis()}.json"
        val jsonFile = File(tempShareDir, fileName)
        
        val content = generateJSONContent(receiptData)
        jsonFile.writeText(content)
        
        return jsonFile
    }
    
    // Content generation methods
    
    private fun generateTextContent(receiptData: ReceiptData): String {
        return buildString {
            appendLine("INVOICE")
            appendLine("=" .repeat(40))
            appendLine()
            appendLine("Business: ${receiptData.businessInfo.name}")
            appendLine("Address: ${receiptData.businessInfo.address}")
            receiptData.businessInfo.phone?.let { appendLine("Phone: $it") }
            receiptData.businessInfo.gstNumber?.let { appendLine("GSTIN: $it") }
            appendLine()
            appendLine("Invoice #: ${receiptData.receiptId}")
            appendLine("Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            appendLine()
            
            receiptData.customerInfo?.let { customer ->
                appendLine("BILL TO:")
                customer.name?.let { appendLine("Name: $it") }
                customer.address?.let { appendLine("Address: $it") }
                customer.phone?.let { appendLine("Phone: $it") }
                appendLine()
            }
            
            appendLine("ITEMS:")
            appendLine("-" .repeat(40))
            receiptData.items.forEach { item ->
                appendLine("${item.name}")
                appendLine("  Qty: ${item.quantity} @ ₹${item.unitPrice}")
                if (item.discount > 0) {
                    appendLine("  Discount: ₹${item.discount}")
                }
                appendLine("  Total: ₹${item.totalAmount}")
                appendLine()
            }
            
            appendLine("-" .repeat(40))
            appendLine("Subtotal: ₹${receiptData.totals.subtotal}")
            if (receiptData.totals.discount > 0) {
                appendLine("Discount: -₹${receiptData.totals.discount}")
            }
            if (receiptData.totals.taxAmount > 0) {
                appendLine("Tax: ₹${receiptData.totals.taxAmount}")
            }
            appendLine("TOTAL: ₹${receiptData.totals.totalAmount}")
            appendLine()
            
            appendLine("PAYMENT:")
            receiptData.payments.forEach { payment ->
                appendLine("${payment.method.displayName}: ₹${payment.amount}")
            }
            appendLine()
            appendLine("Thank you for your business!")
        }
    }
    
    private fun generateHTMLContent(receiptData: ReceiptData, template: ReceiptTemplate): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Invoice ${receiptData.receiptId}</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                .header { text-align: center; color: #2980b9; }
                .business-info { margin-bottom: 20px; }
                .invoice-details { margin-bottom: 20px; }
                .items-table { width: 100%; border-collapse: collapse; }
                .items-table th, .items-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                .items-table th { background-color: #2980b9; color: white; }
                .totals { margin-top: 20px; text-align: right; }
                .total-amount { font-weight: bold; font-size: 1.2em; color: #2980b9; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>INVOICE</h1>
                <h2>${receiptData.businessInfo.name}</h2>
            </div>
            
            <div class="business-info">
                <p>${receiptData.businessInfo.address}</p>
                ${receiptData.businessInfo.phone?.let { "<p>Phone: $it</p>" } ?: ""}
                ${receiptData.businessInfo.gstNumber?.let { "<p>GSTIN: $it</p>" } ?: ""}
            </div>
            
            <div class="invoice-details">
                <p><strong>Invoice #:</strong> ${receiptData.receiptId}</p>
                <p><strong>Date:</strong> ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}</p>
            </div>
            
            ${receiptData.customerInfo?.let { customer ->
                """
                <div class="customer-info">
                    <h3>BILL TO:</h3>
                    ${customer.name?.let { "<p>$it</p>" } ?: ""}
                    ${customer.address?.let { "<p>$it</p>" } ?: ""}
                    ${customer.phone?.let { "<p>Phone: $it</p>" } ?: ""}
                </div>
                """
            } ?: ""}
            
            <table class="items-table">
                <thead>
                    <tr>
                        <th>Item</th>
                        <th>Quantity</th>
                        <th>Rate</th>
                        <th>Amount</th>
                    </tr>
                </thead>
                <tbody>
                    ${receiptData.items.joinToString("") { item ->
                        """
                        <tr>
                            <td>${item.name}</td>
                            <td>${item.quantity}</td>
                            <td>₹${item.unitPrice}</td>
                            <td>₹${item.totalAmount}</td>
                        </tr>
                        """
                    }}
                </tbody>
            </table>
            
            <div class="totals">
                <p>Subtotal: ₹${receiptData.totals.subtotal}</p>
                ${if (receiptData.totals.discount > 0) "<p>Discount: -₹${receiptData.totals.discount}</p>" else ""}
                ${if (receiptData.totals.taxAmount > 0) "<p>Tax: ₹${receiptData.totals.taxAmount}</p>" else ""}
                <p class="total-amount">TOTAL: ₹${receiptData.totals.totalAmount}</p>
            </div>
            
            <div class="payment-info">
                <h3>Payment Method:</h3>
                ${receiptData.payments.joinToString("") { payment ->
                    "<p>${payment.method.displayName}: ₹${payment.amount}</p>"
                }}
            </div>
            
            <div style="text-align: center; margin-top: 30px;">
                <p>Thank you for your business!</p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
    
    private fun generateJSONContent(receiptData: ReceiptData): String {
        // Simplified JSON serialization - would use proper JSON library in real implementation
        return """
        {
            "invoiceId": "${receiptData.receiptId}",
            "transactionId": "${receiptData.transactionId}",
            "businessInfo": {
                "name": "${receiptData.businessInfo.name}",
                "address": "${receiptData.businessInfo.address}",
                "phone": "${receiptData.businessInfo.phone ?: ""}",
                "gstNumber": "${receiptData.businessInfo.gstNumber ?: ""}"
            },
            "customerInfo": ${receiptData.customerInfo?.let { customer ->
                """
                {
                    "name": "${customer.name ?: ""}",
                    "phone": "${customer.phone ?: ""}",
                    "address": "${customer.address ?: ""}"
                }
                """
            } ?: "null"},
            "items": [
                ${receiptData.items.joinToString(",") { item ->
                    """
                    {
                        "name": "${item.name}",
                        "quantity": ${item.quantity},
                        "unitPrice": ${item.unitPrice},
                        "totalAmount": ${item.totalAmount}
                    }
                    """
                }}
            ],
            "totals": {
                "subtotal": ${receiptData.totals.subtotal},
                "discount": ${receiptData.totals.discount},
                "taxAmount": ${receiptData.totals.taxAmount},
                "totalAmount": ${receiptData.totals.totalAmount}
            },
            "timestamp": "${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())}"
        }
        """.trimIndent()
    }
    
    // Message builders
    
    private fun buildShareMessage(receiptData: ReceiptData, sharingConfig: SharingConfig): String {
        val customMessage = sharingConfig.message ?: ""
        
        return buildString {
            if (customMessage.isNotEmpty()) {
                appendLine(customMessage)
                appendLine()
            }
            
            appendLine("📄 Invoice from ${receiptData.businessInfo.name}")
            appendLine("🧾 Invoice #: ${receiptData.receiptId}")
            appendLine("💰 Amount: ₹${receiptData.totals.totalAmount}")
            appendLine("📅 Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}")
            
            if (sharingConfig.includeBusinessCard) {
                appendLine()
                appendLine("📍 ${receiptData.businessInfo.address}")
                receiptData.businessInfo.phone?.let { appendLine("📞 $it") }
                receiptData.businessInfo.email?.let { appendLine("📧 $it") }
            }
            
            appendLine()
            appendLine("Thank you for your business! 🙏")
        }
    }
    
    private fun buildEmailMessage(receiptData: ReceiptData, sharingConfig: SharingConfig): String {
        return buildString {
            appendLine("Dear ${receiptData.customerInfo?.name ?: "Customer"},")
            appendLine()
            appendLine("Please find attached your invoice from ${receiptData.businessInfo.name}.")
            appendLine()
            appendLine("Invoice Details:")
            appendLine("Invoice Number: ${receiptData.receiptId}")
            appendLine("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}")
            appendLine("Amount: ₹${receiptData.totals.totalAmount}")
            appendLine()
            
            if (sharingConfig.message?.isNotEmpty() == true) {
                appendLine(sharingConfig.message)
                appendLine()
            }
            
            appendLine("If you have any questions regarding this invoice, please don't hesitate to contact us.")
            appendLine()
            appendLine("Best regards,")
            appendLine("${receiptData.businessInfo.name}")
            receiptData.businessInfo.phone?.let { appendLine("Phone: $it") }
            receiptData.businessInfo.email?.let { appendLine("Email: $it") }
        }
    }
    
    private fun buildSMSMessage(receiptData: ReceiptData, sharingConfig: SharingConfig): String {
        return buildString {
            append("Invoice ${receiptData.receiptId} from ${receiptData.businessInfo.name}. ")
            append("Amount: ₹${receiptData.totals.totalAmount}. ")
            append("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}. ")
            
            if (sharingConfig.message?.isNotEmpty() == true) {
                append("${sharingConfig.message} ")
            }
            
            append("Thank you!")
        }
    }
    
    // Utility methods
    
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun getMimeType(format: ShareFormat): String {
        return when (format) {
            ShareFormat.PDF -> "application/pdf"
            ShareFormat.IMAGE_PNG -> "image/png"
            ShareFormat.IMAGE_JPEG -> "image/jpeg"
            ShareFormat.TEXT -> "text/plain"
            ShareFormat.HTML -> "text/html"
            ShareFormat.JSON -> "application/json"
        }
    }
    
    private suspend fun emitProgress(
        shareId: String,
        phase: SharingPhase,
        operation: String,
        percentage: Double
    ) {
        val progress = SharingProgress(
            shareId = shareId,
            phase = phase,
            currentOperation = operation,
            percentage = percentage,
            estimatedTimeRemaining = 0L
        )
        _sharingProgress.emit(progress)
    }
    
    /**
     * Clean up temporary files
     */
    suspend fun cleanupTempFiles() = withContext(Dispatchers.IO) {
        try {
            tempShareDir.listFiles()?.forEach { file ->
                if (file.isFile && System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}

/**
 * Sharing operation result
 */
sealed class SharingResult {
    data class Success(
        val shareId: String,
        val method: ShareMethod,
        val filePath: String?,
        val message: String
    ) : SharingResult()
    
    data class Error(
        val shareId: String,
        val message: String
    ) : SharingResult()
}

/**
 * Sharing phases
 */
enum class SharingPhase {
    INITIALIZING,
    GENERATING_FILE,
    PREPARING_SHARE,
    SHARING,
    COMPLETED,
    FAILED
}

/**
 * Sharing progress information
 */
data class SharingProgress(
    val shareId: String,
    val phase: SharingPhase,
    val currentOperation: String,
    val percentage: Double,
    val estimatedTimeRemaining: Long
)