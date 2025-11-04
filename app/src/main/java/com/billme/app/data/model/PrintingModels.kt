package com.billme.app.data.model

import java.time.LocalDateTime
import java.util.*

/**
 * Receipt template configuration
 */
data class ReceiptTemplate(
    val templateId: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ReceiptType,
    val paperSize: PaperSize,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val headerConfig: HeaderConfig,
    val bodyConfig: BodyConfig,
    val footerConfig: FooterConfig,
    val styling: ReceiptStyling,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Receipt types for different use cases
 */
enum class ReceiptType {
    CUSTOMER_COPY,
    OWNER_COPY,
    DUPLICATE,
    INVOICE,
    ESTIMATE,
    RETURN_RECEIPT,
    PAYMENT_RECEIPT,
    DELIVERY_CHALLAN
}

/**
 * Paper sizes supported by printers
 */
enum class PaperSize(val width: Int, val height: Int, val displayName: String) {
    THERMAL_58MM(384, 0, "58mm Thermal"),
    THERMAL_80MM(576, 0, "80mm Thermal"),
    A4(595, 842, "A4"),
    A5(420, 595, "A5"),
    LETTER(612, 792, "Letter"),
    CUSTOM(0, 0, "Custom")
}

/**
 * Page orientation
 */
enum class PageOrientation {
    PORTRAIT,
    LANDSCAPE
}

/**
 * Header configuration for receipts
 */
data class HeaderConfig(
    val showLogo: Boolean = true,
    val logoPath: String? = null,
    val logoSize: LogoSize = LogoSize.MEDIUM,
    val businessName: String,
    val businessAddress: String,
    val businessPhone: String? = null,
    val businessEmail: String? = null,
    val businessGSTIN: String? = null,
    val showQRCode: Boolean = false,
    val qrCodeContent: QRCodeContent = QRCodeContent.BUSINESS_INFO,
    val headerSeparator: SeparatorStyle = SeparatorStyle.LINE,
    val alignment: TextAlignment = TextAlignment.CENTER
)

/**
 * Body configuration for receipts
 */
data class BodyConfig(
    val showItemNumbers: Boolean = true,
    val showItemCodes: Boolean = false,
    val showItemDescription: Boolean = true,
    val showQuantity: Boolean = true,
    val showUnitPrice: Boolean = true,
    val showDiscount: Boolean = true,
    val showTaxDetails: Boolean = true,
    val showTotalAmount: Boolean = true,
    val itemSeparator: SeparatorStyle = SeparatorStyle.NONE,
    val groupByCategory: Boolean = false,
    val showRunningTotal: Boolean = false,
    val maxItemNameLength: Int = 20
)

/**
 * Footer configuration for receipts
 */
data class FooterConfig(
    val showPaymentMethod: Boolean = true,
    val showChangeAmount: Boolean = true,
    val showTotalSavings: Boolean = true,
    val showThanksMessage: Boolean = true,
    val thanksMessage: String = "Thank you for your business!",
    val showReturnPolicy: Boolean = false,
    val returnPolicyText: String = "",
    val showCustomMessage: Boolean = false,
    val customMessage: String = "",
    val showTimestamp: Boolean = true,
    val showCashier: Boolean = true,
    val footerSeparator: SeparatorStyle = SeparatorStyle.DOUBLE_LINE
)

/**
 * Receipt styling configuration
 */
data class ReceiptStyling(
    val fontFamily: FontFamily = FontFamily.MONOSPACE,
    val fontSize: FontSize = FontSize.NORMAL,
    val headerFontSize: FontSize = FontSize.LARGE,
    val titleFontSize: FontSize = FontSize.MEDIUM,
    val lineSpacing: LineSpacing = LineSpacing.NORMAL,
    val margins: Margins = Margins(),
    val colors: ColorScheme = ColorScheme(),
    val borders: BorderStyle = BorderStyle.NONE,
    val emphasis: EmphasisStyle = EmphasisStyle()
)

/**
 * Font family options
 */
enum class FontFamily(val displayName: String, val escPosCode: String) {
    MONOSPACE("Monospace", "\u001b@"),
    SANS_SERIF("Sans Serif", "\u001b!\u0001"),
    SERIF("Serif", "\u001b!\u0002")
}

/**
 * Font size options
 */
enum class FontSize(val displayName: String, val multiplier: Float, val escPosCode: String) {
    SMALL("Small", 0.8f, "\u001b!\u0000"),
    NORMAL("Normal", 1.0f, "\u001b!\u0000"),
    MEDIUM("Medium", 1.2f, "\u001b!\u0010"),
    LARGE("Large", 1.5f, "\u001b!\u0020"),
    EXTRA_LARGE("Extra Large", 2.0f, "\u001b!\u0030")
}

/**
 * Line spacing options
 */
enum class LineSpacing(val displayName: String, val multiplier: Float) {
    TIGHT("Tight", 0.8f),
    NORMAL("Normal", 1.0f),
    LOOSE("Loose", 1.2f),
    EXTRA_LOOSE("Extra Loose", 1.5f)
}

/**
 * Text alignment options
 */
enum class TextAlignment(val displayName: String, val escPosCode: String) {
    LEFT("Left", "\u001ba\u0000"),
    CENTER("Center", "\u001ba\u0001"),
    RIGHT("Right", "\u001ba\u0002")
}

/**
 * Logo size options
 */
enum class LogoSize(val displayName: String, val width: Int, val height: Int) {
    SMALL("Small", 64, 64),
    MEDIUM("Medium", 96, 96),
    LARGE("Large", 128, 128),
    EXTRA_LARGE("Extra Large", 192, 192)
}

/**
 * QR code content options
 */
enum class QRCodeContent {
    BUSINESS_INFO,
    RECEIPT_ID,
    PAYMENT_LINK,
    CUSTOM
}

/**
 * Separator style options
 */
enum class SeparatorStyle(val displayName: String, val character: String) {
    NONE("None", ""),
    LINE("Line", "-"),
    DOUBLE_LINE("Double Line", "="),
    DASH("Dash", "- "),
    DOT("Dot", ". "),
    STAR("Star", "*")
}

/**
 * Border style options
 */
enum class BorderStyle {
    NONE,
    LIGHT,
    HEAVY,
    DOUBLE
}

/**
 * Margins configuration
 */
data class Margins(
    val top: Int = 5,
    val bottom: Int = 5,
    val left: Int = 2,
    val right: Int = 2
)

/**
 * Color scheme for receipts
 */
data class ColorScheme(
    val textColor: String = "#000000",
    val backgroundColor: String = "#FFFFFF",
    val headerColor: String = "#000000",
    val highlightColor: String = "#666666",
    val accentColor: String = "#0066CC"
)

/**
 * Emphasis styling options
 */
data class EmphasisStyle(
    val boldHeaders: Boolean = true,
    val boldTotals: Boolean = true,
    val italicThanks: Boolean = false,
    val underlineImportant: Boolean = false
)

/**
 * Printer configuration
 */
data class PrinterConfig(
    val printerId: String = UUID.randomUUID().toString(),
    val name: String,
    val type: PrinterType,
    val connectionType: ConnectionType,
    val connectionDetails: ConnectionDetails,
    val paperSize: PaperSize,
    val characterSet: CharacterSet = CharacterSet.UTF_8,
    val dpi: Int = 203,
    val maxWidth: Int = 576,
    val capabilities: PrinterCapabilities,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val lastUsed: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Printer types
 */
enum class PrinterType(val displayName: String) {
    THERMAL("Thermal Printer"),
    INKJET("Inkjet Printer"),
    LASER("Laser Printer"),
    DOT_MATRIX("Dot Matrix Printer"),
    VIRTUAL("Virtual Printer")
}

/**
 * Connection types for printers
 */
enum class ConnectionType(val displayName: String) {
    BLUETOOTH("Bluetooth"),
    WIFI("Wi-Fi"),
    USB("USB"),
    ETHERNET("Ethernet"),
    SERIAL("Serial"),
    VIRTUAL("Virtual")
}

/**
 * Connection details for different connection types
 */
sealed class ConnectionDetails {
    data class Bluetooth(
        val deviceAddress: String,
        val deviceName: String,
        val isPaired: Boolean = false
    ) : ConnectionDetails()
    
    data class WiFi(
        val ipAddress: String,
        val port: Int = 9100,
        val networkName: String? = null
    ) : ConnectionDetails()
    
    data class USB(
        val vendorId: String,
        val productId: String,
        val devicePath: String
    ) : ConnectionDetails()
    
    data class Ethernet(
        val ipAddress: String,
        val port: Int = 9100,
        val macAddress: String? = null
    ) : ConnectionDetails()
    
    data class Serial(
        val portName: String,
        val baudRate: Int = 9600,
        val dataBits: Int = 8,
        val stopBits: Int = 1,
        val parity: SerialParity = SerialParity.NONE
    ) : ConnectionDetails()
    
    object Virtual : ConnectionDetails()
}

/**
 * Serial port parity options
 */
enum class SerialParity {
    NONE, ODD, EVEN, MARK, SPACE
}

/**
 * Character set encoding options
 */
enum class CharacterSet(val displayName: String, val encoding: String) {
    UTF_8("UTF-8", "UTF-8"),
    ASCII("ASCII", "ASCII"),
    ISO_8859_1("ISO-8859-1", "ISO-8859-1"),
    CP437("CP437", "CP437"),
    CP850("CP850", "CP850"),
    CP852("CP852", "CP852"),
    CP866("CP866", "CP866")
}

/**
 * Printer capabilities
 */
data class PrinterCapabilities(
    val supportsCutting: Boolean = false,
    val supportsPartialCut: Boolean = false,
    val supportsCashDrawer: Boolean = false,
    val supportsQRCode: Boolean = false,
    val supportsBarcode: Boolean = false,
    val supportsBitmap: Boolean = false,
    val supportsColor: Boolean = false,
    val maxPaperWidth: Int = 80, // in mm
    val supportedBarcodeTypes: List<BarcodeType> = emptyList(),
    val supportedCommands: List<ESCPOSCommand> = emptyList()
)

/**
 * Supported barcode types
 */
enum class BarcodeType {
    UPC_A, UPC_E, EAN13, EAN8, CODE39, ITF, CODABAR, CODE93, CODE128, QR_CODE, PDF417
}

/**
 * ESC/POS command support
 */
enum class ESCPOSCommand {
    INITIALIZE, FEED_LINE, CUT_PAPER, OPEN_DRAWER, BEEP, SET_FONT, SET_ALIGN, SET_EMPHASIS,
    PRINT_BARCODE, PRINT_QR, PRINT_BITMAP, SET_LINE_SPACING, SET_CHARACTER_SIZE
}

/**
 * Print job configuration
 */
data class PrintJob(
    val jobId: String = UUID.randomUUID().toString(),
    val receiptId: String,
    val templateId: String,
    val printerId: String,
    val copies: Int = 1,
    val priority: PrintPriority = PrintPriority.NORMAL,
    val options: PrintOptions,
    val status: PrintJobStatus = PrintJobStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val startedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
    val errorMessage: String? = null
)

/**
 * Print job priority
 */
enum class PrintPriority {
    LOW, NORMAL, HIGH, URGENT
}

/**
 * Print job status
 */
enum class PrintJobStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
}

/**
 * Print options
 */
data class PrintOptions(
    val openCashDrawer: Boolean = false,
    val cutPaper: Boolean = true,
    val partialCut: Boolean = false,
    val feedLines: Int = 3,
    val beepCount: Int = 0,
    val density: PrintDensity = PrintDensity.NORMAL,
    val speed: PrintSpeed = PrintSpeed.NORMAL,
    val doubleHeight: Boolean = false,
    val doubleWidth: Boolean = false,
    val customSettings: Map<String, String> = emptyMap()
)

/**
 * Print density options
 */
enum class PrintDensity(val displayName: String, val level: Int) {
    LIGHT("Light", 1),
    NORMAL("Normal", 2),
    DARK("Dark", 3),
    EXTRA_DARK("Extra Dark", 4)
}

/**
 * Print speed options
 */
enum class PrintSpeed(val displayName: String, val level: Int) {
    SLOW("Slow", 1),
    NORMAL("Normal", 2),
    FAST("Fast", 3)
}

/**
 * Receipt data for printing
 */
data class ReceiptData(
    val receiptId: String,
    val transactionId: String,
    val receiptType: ReceiptType,
    val businessInfo: BusinessInfo,
    val customerInfo: CustomerInfo? = null,
    val items: List<ReceiptItem>,
    val payments: List<PaymentInfo>,
    val totals: ReceiptTotals,
    val metadata: ReceiptMetadata,
    val customFields: Map<String, String> = emptyMap()
)

/**
 * Business information for receipts
 */
data class BusinessInfo(
    val name: String,
    val address: String,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val gstNumber: String? = null,
    val taxId: String? = null,
    val licenseNumber: String? = null,
    val logoPath: String? = null
)

/**
 * Customer information for receipts
 */
data class CustomerInfo(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val gstNumber: String? = null,
    val loyaltyNumber: String? = null
)

/**
 * Receipt item information
 */
data class ReceiptItem(
    val itemId: String,
    val name: String,
    val description: String? = null,
    val code: String? = null,
    val category: String? = null,
    val quantity: Double,
    val unit: String = "pcs",
    val unitPrice: Double,
    val discount: Double = 0.0,
    val discountType: DiscountType = DiscountType.AMOUNT,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val attributes: Map<String, String> = emptyMap()
)

/**
 * Discount type
 */
enum class DiscountType {
    AMOUNT, PERCENTAGE
}

/**
 * Payment information
 */
data class PaymentInfo(
    val method: PaymentMethod,
    val amount: Double,
    val reference: String? = null,
    val cardLast4: String? = null,
    val approvalCode: String? = null
)

/**
 * Payment methods
 */
enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    CARD("Card"),
    UPI("UPI"),
    BANK_TRANSFER("Bank Transfer"),
    CHEQUE("Cheque"),
    CREDIT("Store Credit"),
    GIFT_CARD("Gift Card"),
    OTHER("Other")
}

/**
 * Receipt totals
 */
data class ReceiptTotals(
    val subtotal: Double,
    val discount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double,
    val changeAmount: Double = 0.0,
    val roundingAdjustment: Double = 0.0,
    val totalSavings: Double = 0.0
)

/**
 * Receipt metadata
 */
data class ReceiptMetadata(
    val timestamp: LocalDateTime,
    val cashier: String? = null,
    val terminal: String? = null,
    val shift: String? = null,
    val sequenceNumber: Long? = null,
    val fiscalYear: String? = null,
    val location: String? = null,
    val department: String? = null
)

/**
 * Sharing configuration
 */
data class SharingConfig(
    val shareId: String = UUID.randomUUID().toString(),
    val receiptId: String,
    val shareMethod: ShareMethod,
    val recipients: List<String>,
    val format: ShareFormat,
    val message: String? = null,
    val includeBusinessCard: Boolean = false,
    val passwordProtect: Boolean = false,
    val password: String? = null,
    val expiryDate: LocalDateTime? = null,
    val customBranding: Boolean = true
)

/**
 * Sharing methods
 */
enum class ShareMethod {
    EMAIL, SMS, WHATSAPP, TELEGRAM, BLUETOOTH, AIRDROP, SOCIAL_MEDIA, QR_CODE, NFC
}

/**
 * Sharing formats
 */
enum class ShareFormat {
    PDF, IMAGE_PNG, IMAGE_JPEG, TEXT, HTML, JSON
}

/**
 * Print queue management
 */
data class PrintQueue(
    val queueId: String = UUID.randomUUID().toString(),
    val printerId: String,
    val jobs: List<PrintJob>,
    val status: QueueStatus = QueueStatus.ACTIVE,
    val maxSize: Int = 100,
    val processingJob: String? = null,
    val lastProcessed: LocalDateTime? = null,
    val errorCount: Int = 0
)

/**
 * Queue status
 */
enum class QueueStatus {
    ACTIVE, PAUSED, STOPPED, ERROR
}

/**
 * Print statistics
 */
data class PrintStatistics(
    val totalJobs: Long = 0,
    val successfulJobs: Long = 0,
    val failedJobs: Long = 0,
    val cancelledJobs: Long = 0,
    val averageJobTime: Long = 0, // in milliseconds
    val totalPaperUsed: Double = 0.0, // in meters
    val totalInkUsed: Double = 0.0, // in milliliters
    val lastJobTime: LocalDateTime? = null,
    val printerUptime: Long = 0, // in seconds
    val errorRate: Double = 0.0
)

/**
 * Template preview data
 */
data class TemplatePreview(
    val templateId: String,
    val previewImage: String, // Base64 encoded image
    val previewText: String,
    val estimatedHeight: Int,
    val estimatedWidth: Int,
    val pageCount: Int = 1,
    val generatedAt: LocalDateTime = LocalDateTime.now()
)