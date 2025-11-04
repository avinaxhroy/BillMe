package com.billme.app.data.model

import android.graphics.Rect
import java.time.LocalDateTime
import java.util.*

/**
 * OCR scan result containing extracted text and field mappings
 */
data class OCRScanResult(
    val scanId: String = UUID.randomUUID().toString(),
    val documentType: DocumentType,
    val sourceImagePath: String,
    val processedImagePath: String? = null,
    val rawText: String,
    val textBlocks: List<TextBlock>,
    val extractedFields: Map<String, ExtractedField>,
    val confidence: ConfidenceScore,
    val processingTime: Long,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val corrections: List<FieldCorrection> = emptyList(),
    val isManuallyVerified: Boolean = false,
    val templateId: String? = null
)

/**
 * Types of documents that can be processed
 */
enum class DocumentType(val displayName: String, val expectedFields: List<String>) {
    INVOICE(
        "Invoice", 
        listOf("invoice_number", "date", "vendor_name", "total_amount", "items", "tax_amount", "subtotal")
    ),
    RECEIPT(
        "Receipt", 
        listOf("receipt_number", "date", "merchant_name", "total_amount", "items", "payment_method")
    ),
    PURCHASE_ORDER(
        "Purchase Order", 
        listOf("po_number", "date", "vendor_name", "items", "total_amount", "delivery_date")
    ),
    DELIVERY_CHALLAN(
        "Delivery Challan", 
        listOf("challan_number", "date", "from_address", "to_address", "items", "vehicle_number")
    ),
    CREDIT_NOTE(
        "Credit Note", 
        listOf("credit_note_number", "date", "customer_name", "amount", "reason", "reference_invoice")
    ),
    QUOTATION(
        "Quotation", 
        listOf("quote_number", "date", "customer_name", "items", "total_amount", "validity_date")
    ),
    GENERIC(
        "Generic Document", 
        listOf("text_content", "date", "amount", "reference_number")
    )
}

/**
 * Text block with position and content information
 */
data class TextBlock(
    val blockId: String = UUID.randomUUID().toString(),
    val text: String,
    val boundingBox: Rect,
    val confidence: Float,
    val lines: List<TextLine>,
    val recognizedLanguages: List<String> = emptyList(),
    val fieldType: FieldType? = null,
    val isSelected: Boolean = false
)

/**
 * Individual text line within a block
 */
data class TextLine(
    val lineId: String = UUID.randomUUID().toString(),
    val text: String,
    val boundingBox: Rect,
    val confidence: Float,
    val words: List<TextWord>,
    val angle: Float = 0f
)

/**
 * Individual word within a line
 */
data class TextWord(
    val wordId: String = UUID.randomUUID().toString(),
    val text: String,
    val boundingBox: Rect,
    val confidence: Float,
    val symbols: List<TextSymbol> = emptyList()
)

/**
 * Individual character/symbol
 */
data class TextSymbol(
    val symbol: String,
    val boundingBox: Rect,
    val confidence: Float
)

/**
 * Extracted field with validation and confidence
 */
data class ExtractedField(
    val fieldId: String = UUID.randomUUID().toString(),
    val fieldType: FieldType,
    val rawValue: String,
    val processedValue: String,
    val confidence: Float,
    val boundingBox: Rect?,
    val sourceTextBlocks: List<String>, // References to TextBlock IDs
    val validationResult: FieldValidationResult,
    val suggestions: List<String> = emptyList(),
    val isUserCorrected: Boolean = false,
    val correctedValue: String? = null
)

/**
 * Types of fields that can be extracted
 */
enum class FieldType(
    val displayName: String, 
    val regex: String?, 
    val validationRule: String? = null,
    val category: FieldCategory
) {
    // Financial Fields
    TOTAL_AMOUNT("Total Amount", """\d+\.?\d*""", "Must be a valid number", FieldCategory.FINANCIAL),
    SUBTOTAL("Subtotal", """\d+\.?\d*""", "Must be a valid number", FieldCategory.FINANCIAL),
    TAX_AMOUNT("Tax Amount", """\d+\.?\d*""", "Must be a valid number", FieldCategory.FINANCIAL),
    DISCOUNT("Discount", """\d+\.?\d*""", "Must be a valid number", FieldCategory.FINANCIAL),
    
    // Identification Fields
    INVOICE_NUMBER("Invoice Number", """[A-Z0-9\-/]+""", null, FieldCategory.IDENTIFICATION),
    RECEIPT_NUMBER("Receipt Number", """[A-Z0-9\-/]+""", null, FieldCategory.IDENTIFICATION),
    PO_NUMBER("PO Number", """[A-Z0-9\-/]+""", null, FieldCategory.IDENTIFICATION),
    GST_NUMBER("GST Number", """[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}""", null, FieldCategory.IDENTIFICATION),
    
    // Date Fields
    INVOICE_DATE("Invoice Date", """\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""", "Must be valid date", FieldCategory.DATE),
    DUE_DATE("Due Date", """\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""", "Must be valid date", FieldCategory.DATE),
    DELIVERY_DATE("Delivery Date", """\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""", "Must be valid date", FieldCategory.DATE),
    
    // Entity Fields
    VENDOR_NAME("Vendor Name", null, null, FieldCategory.ENTITY),
    CUSTOMER_NAME("Customer Name", null, null, FieldCategory.ENTITY),
    MERCHANT_NAME("Merchant Name", null, null, FieldCategory.ENTITY),
    
    // Address Fields
    BILLING_ADDRESS("Billing Address", null, null, FieldCategory.ADDRESS),
    SHIPPING_ADDRESS("Shipping Address", null, null, FieldCategory.ADDRESS),
    VENDOR_ADDRESS("Vendor Address", null, null, FieldCategory.ADDRESS),
    
    // Contact Fields
    PHONE_NUMBER("Phone Number", """[\+]?[0-9\-\s\(\)]+""", null, FieldCategory.CONTACT),
    EMAIL("Email", """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""", null, FieldCategory.CONTACT),
    
    // Item Fields
    ITEM_DESCRIPTION("Item Description", null, null, FieldCategory.ITEM),
    ITEM_QUANTITY("Item Quantity", """\d+\.?\d*""", "Must be a valid number", FieldCategory.ITEM),
    ITEM_RATE("Item Rate", """\d+\.?\d*""", "Must be a valid number", FieldCategory.ITEM),
    ITEM_AMOUNT("Item Amount", """\d+\.?\d*""", "Must be a valid number", FieldCategory.ITEM),
    
    // Payment Fields
    PAYMENT_METHOD("Payment Method", null, null, FieldCategory.PAYMENT),
    PAYMENT_TERMS("Payment Terms", null, null, FieldCategory.PAYMENT),
    
    // Miscellaneous
    REFERENCE_NUMBER("Reference Number", """[A-Z0-9\-/]+""", null, FieldCategory.IDENTIFICATION),
    NOTES("Notes", null, null, FieldCategory.MISCELLANEOUS),
    CURRENCY("Currency", """[A-Z]{3}""", null, FieldCategory.FINANCIAL),
    
    // Generic
    UNKNOWN("Unknown", null, null, FieldCategory.MISCELLANEOUS)
}

/**
 * Field categories for organization
 */
enum class FieldCategory {
    FINANCIAL, IDENTIFICATION, DATE, ENTITY, ADDRESS, CONTACT, ITEM, PAYMENT, MISCELLANEOUS
}

/**
 * Field validation result
 */
data class FieldValidationResult(
    val isValid: Boolean,
    val validationType: ValidationType,
    val errorMessage: String? = null,
    val suggestion: String? = null,
    val confidence: Float
)

/**
 * Validation types
 */
enum class ValidationType {
    FORMAT_VALIDATION,
    BUSINESS_LOGIC_VALIDATION,
    CROSS_FIELD_VALIDATION,
    DICTIONARY_VALIDATION,
    PATTERN_MATCHING,
    CONTEXT_BASED,
    LUHN_ALGORITHM
}

/**
 * Overall confidence scoring
 */
data class ConfidenceScore(
    val overallConfidence: Float,
    val textRecognitionConfidence: Float,
    val fieldExtractionConfidence: Float,
    val validationConfidence: Float,
    val qualityScore: Float, // Image quality assessment
    val completenessScore: Float // How complete the extraction is
)

/**
 * Manual correction made by user
 */
data class FieldCorrection(
    val correctionId: String = UUID.randomUUID().toString(),
    val fieldType: FieldType,
    val originalValue: String,
    val correctedValue: String,
    val correctionReason: CorrectionReason,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val confidence: Float
)

/**
 * Reasons for manual corrections
 */
enum class CorrectionReason {
    OCR_ERROR,
    FIELD_MAPPING_ERROR,
    VALIDATION_ERROR,
    MISSING_FIELD,
    WRONG_FIELD_TYPE,
    USER_PREFERENCE
}

/**
 * OCR processing configuration
 */
data class OCRConfig(
    val documentType: DocumentType,
    val language: String = "en",
    val enableTextRecognition: Boolean = true,
    val enableFieldExtraction: Boolean = true,
    val enableValidation: Boolean = true,
    val confidenceThreshold: Float = 0.7f,
    val preprocessingOptions: ImagePreprocessingOptions,
    val fieldExtractionOptions: FieldExtractionOptions,
    val postProcessingOptions: PostProcessingOptions
)

/**
 * Image preprocessing options
 */
data class ImagePreprocessingOptions(
    val enableDeskewing: Boolean = true,
    val enableNoiseReduction: Boolean = true,
    val enableContrastEnhancement: Boolean = true,
    val enableBinarization: Boolean = true,
    val enableSharpening: Boolean = false,
    val targetDPI: Int = 300,
    val cropPadding: Int = 20
)

/**
 * Field extraction configuration
 */
data class FieldExtractionOptions(
    val enableSmartFieldDetection: Boolean = true,
    val enableTableExtraction: Boolean = true,
    val enableKeyValuePairing: Boolean = true,
    val fieldPriorityList: List<FieldType> = emptyList(),
    val customPatterns: Map<String, String> = emptyMap()
)

/**
 * Post-processing options
 */
data class PostProcessingOptions(
    val enableSpellCheck: Boolean = true,
    val enableFieldValidation: Boolean = true,
    val enableDataNormalization: Boolean = true,
    val enableSuggestions: Boolean = true
)

/**
 * OCR processing progress
 */
data class OCRProgress(
    val scanId: String,
    val phase: OCRPhase,
    val currentOperation: String,
    val percentage: Double,
    val processingTime: Long,
    val estimatedTimeRemaining: Long
)

/**
 * OCR processing phases
 */
enum class OCRPhase {
    INITIALIZING,
    IMAGE_PREPROCESSING,
    TEXT_RECOGNITION,
    FIELD_EXTRACTION,
    VALIDATION,
    POST_PROCESSING,
    COMPLETED,
    FAILED
}

/**
 * OCR template for document recognition
 */
data class OCRTemplate(
    val templateId: String = UUID.randomUUID().toString(),
    val name: String,
    val documentType: DocumentType,
    val fieldMappings: List<FieldMapping>,
    val layoutRules: List<LayoutRule>,
    val validationRules: List<ValidationRule>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val usageCount: Int = 0,
    val successRate: Float = 0f
)

/**
 * Field mapping within a template
 */
data class FieldMapping(
    val mappingId: String = UUID.randomUUID().toString(),
    val fieldType: FieldType,
    val expectedRegion: Rect?, // Expected location on document
    val relativePosition: RelativePosition?, // Position relative to other fields
    val alternateLabels: List<String> = emptyList(), // Alternative names for the field
    val required: Boolean = true,
    val priority: Int = 0
)

/**
 * Relative position for field mapping
 */
data class RelativePosition(
    val referenceField: FieldType,
    val direction: Direction,
    val distance: Int, // in pixels
    val tolerance: Int = 50
)

/**
 * Layout directions
 */
enum class Direction {
    ABOVE, BELOW, LEFT, RIGHT, DIAGONAL_TOP_LEFT, DIAGONAL_TOP_RIGHT, DIAGONAL_BOTTOM_LEFT, DIAGONAL_BOTTOM_RIGHT
}

/**
 * Layout rules for template matching
 */
data class LayoutRule(
    val ruleId: String = UUID.randomUUID().toString(),
    val ruleName: String,
    val condition: String, // Rule condition in expression format
    val action: String, // Action to take when condition is met
    val priority: Int = 0
)

/**
 * Validation rules for extracted data
 */
data class ValidationRule(
    val ruleId: String = UUID.randomUUID().toString(),
    val fieldType: FieldType,
    val validationType: ValidationType,
    val rule: String, // Validation rule expression
    val errorMessage: String,
    val severity: ValidationSeverity = ValidationSeverity.ERROR
)

/**
 * Validation severity levels
 */
enum class ValidationSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

/**
 * OCR learning data for improving recognition
 */
data class OCRLearningData(
    val learningId: String = UUID.randomUUID().toString(),
    val documentType: DocumentType,
    val originalText: String,
    val correctedText: String,
    val fieldType: FieldType?,
    val correctionType: CorrectionType,
    val userFeedback: UserFeedback?,
    val contextData: Map<String, String> = emptyMap(),
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Types of corrections for learning
 */
enum class CorrectionType {
    TEXT_CORRECTION,
    FIELD_MAPPING_CORRECTION,
    TEMPLATE_LEARNING,
    PATTERN_RECOGNITION_IMPROVEMENT
}

/**
 * User feedback for OCR results
 */
data class UserFeedback(
    val rating: Int, // 1-5 rating
    val comments: String?,
    val suggestedImprovement: String?,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * OCR processing statistics
 */
data class OCRStatistics(
    val totalScansProcessed: Long = 0,
    val successfulScans: Long = 0,
    val failedScans: Long = 0,
    val averageProcessingTime: Long = 0,
    val averageConfidence: Float = 0f,
    val mostCommonErrors: List<String> = emptyList(),
    val templateUsageStats: Map<String, Int> = emptyMap(),
    val fieldAccuracyStats: Map<FieldType, Float> = emptyMap(),
    val documentTypeStats: Map<DocumentType, Long> = emptyMap()
)

/**
 * Camera capture session for document scanning
 */
data class CameraScanSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val documentType: DocumentType,
    val capturedImages: List<CapturedImage>,
    val scanSettings: CameraScanSettings,
    val status: ScanSessionStatus = ScanSessionStatus.ACTIVE,
    val startTime: LocalDateTime = LocalDateTime.now(),
    val endTime: LocalDateTime? = null
)

/**
 * Individual captured image in session
 */
data class CapturedImage(
    val imageId: String = UUID.randomUUID().toString(),
    val imagePath: String,
    val thumbnail: String?,
    val captureTime: LocalDateTime = LocalDateTime.now(),
    val imageQuality: ImageQualityMetrics,
    val isProcessed: Boolean = false,
    val ocrResultId: String? = null
)

/**
 * Image quality assessment
 */
data class ImageQualityMetrics(
    val sharpness: Float,
    val brightness: Float,
    val contrast: Float,
    val resolution: Pair<Int, Int>,
    val fileSize: Long,
    val overallScore: Float,
    val recommendations: List<String> = emptyList()
)

/**
 * Camera scanning settings
 */
data class CameraScanSettings(
    val autoCapture: Boolean = false,
    val multiPageMode: Boolean = false,
    val flashEnabled: Boolean = false,
    val imageFormat: ImageFormat = ImageFormat.JPEG,
    val imageQuality: Int = 90,
    val scanRegion: Rect? = null, // Crop region if specified
    val guidanceEnabled: Boolean = true
)

/**
 * Supported image formats
 */
enum class ImageFormat {
    JPEG, PNG, TIFF, PDF
}

/**
 * Scan session status
 */
enum class ScanSessionStatus {
    ACTIVE, PAUSED, COMPLETED, CANCELLED, ERROR
}

/**
 * Smart suggestion for field correction
 */
data class SmartSuggestion(
    val suggestionId: String = UUID.randomUUID().toString(),
    val fieldType: FieldType,
    val currentValue: String,
    val suggestedValue: String,
    val confidence: Float,
    val reasoning: String,
    val sourceData: String, // What data was used to generate suggestion
    val accepted: Boolean? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Batch OCR processing job
 */
data class BatchOCRJob(
    val jobId: String = UUID.randomUUID().toString(),
    val jobName: String,
    val documentType: DocumentType,
    val imageFiles: List<String>,
    val config: OCRConfig,
    val status: BatchJobStatus = BatchJobStatus.PENDING,
    val progress: BatchProgress = BatchProgress(),
    val results: List<OCRScanResult> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val startedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
    val errorMessage: String? = null
)

/**
 * Batch job status
 */
enum class BatchJobStatus {
    PENDING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
}

/**
 * Batch processing progress
 */
data class BatchProgress(
    val totalFiles: Int = 0,
    val processedFiles: Int = 0,
    val successfulFiles: Int = 0,
    val failedFiles: Int = 0,
    val currentFile: String? = null,
    val estimatedTimeRemaining: Long = 0
)