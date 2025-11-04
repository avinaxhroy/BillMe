package com.billme.app.core.ocr

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.billme.app.data.model.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.time.LocalDateTime
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR Engine Service with ML Kit Text Recognition
 */
@Singleton
class OCREngineService @Inject constructor(
    private val context: Context,
    private val textProcessor: TextProcessingPipeline,
    private val smartFieldDetector: SmartFieldDetector,
    private val templateMatcher: TemplateMatchingService,
    private val tesseractFilter: TesseractTextFilter
) {
    
    companion object {
        private const val MIN_CONFIDENCE_THRESHOLD = 0.5f
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
        private const val IMAGE_QUALITY_THRESHOLD = 0.4f
        private const val MAX_PROCESSING_TIME_MS = 30000L
        
        // Image preprocessing constants
        private const val TARGET_WIDTH = 4096
        private const val TARGET_HEIGHT = 4096
        private const val GAUSSIAN_BLUR_RADIUS = 1.0f
        private const val CONTRAST_FACTOR = 1.1f
        private const val BRIGHTNESS_FACTOR = 0
    }
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    private val _ocrProgress = MutableSharedFlow<OCRProgress>()
    val ocrProgress: SharedFlow<OCRProgress> = _ocrProgress.asSharedFlow()
    
    /**
     * Process image with comprehensive OCR pipeline
     */
    suspend fun processImage(
        imageUri: Uri,
        config: OCRConfig
    ): OCRResult = withContext(Dispatchers.IO) {
        
        val scanId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()
        
        try {
            emitProgress(scanId, OCRPhase.INITIALIZING, "Starting OCR processing", 0.0, startTime)
            
            // Load and validate image
            val originalBitmap = loadImageFromUri(imageUri)
                ?: return@withContext OCRResult.Error(scanId, "Failed to load image")
            
            // Assess image quality
            val imageQuality = assessImageQuality(originalBitmap)
            
            emitProgress(scanId, OCRPhase.IMAGE_PREPROCESSING, "Preparing image", 10.0, startTime)
            
            // CRITICAL: For high-quality images, use ORIGINAL without any processing
            // Preprocessing can DEGRADE quality for modern smartphone cameras
            val processedBitmap = when {
                // Excellent quality (>70%): Use ORIGINAL - no preprocessing at all
                imageQuality.overallScore > 0.7f -> {
                    originalBitmap
                }
                // Good quality (50-70%): Only resize if too large
                imageQuality.overallScore > 0.5f -> {
                    if (originalBitmap.width > 3000 || originalBitmap.height > 3000) {
                        // Gentle resize only
                        val scale = 3000f / maxOf(originalBitmap.width, originalBitmap.height)
                        Bitmap.createScaledBitmap(
                            originalBitmap,
                            (originalBitmap.width * scale).toInt(),
                            (originalBitmap.height * scale).toInt(),
                            true
                        )
                    } else {
                        originalBitmap
                    }
                }
                // Medium quality (40-50%): Minimal enhancement
                imageQuality.overallScore > 0.4f -> {
                    convertToGrayscale(originalBitmap)
                }
                // Low quality (<40%): Apply preprocessing
                else -> {
                    val enhancedOptions = ImagePreprocessingOptions(
                        enableDeskewing = false,
                        enableNoiseReduction = false,
                        enableContrastEnhancement = true,
                        enableBinarization = false,
                        enableSharpening = true
                    )
                    preprocessImage(originalBitmap, enhancedOptions)
                }
            }
            
            emitProgress(scanId, OCRPhase.TEXT_RECOGNITION, "Recognizing text with ML Kit", 30.0, startTime)
            
            // Perform text recognition with ML Kit
            android.util.Log.d("OCR", "Using ML Kit - Image size: ${processedBitmap.width}x${processedBitmap.height}, Quality score: ${imageQuality.overallScore}")
            var mlKitText = performTextRecognition(processedBitmap)
            
            // Log recognized text length
            android.util.Log.d("OCR", "Initial OCR result: ${mlKitText.text.length} characters")
            
            // If recognition returned poor results, try with original
            if (mlKitText.text.trim().length < 50 && imageQuality.overallScore > 0.6f) {
                android.util.Log.d("OCR", "Poor results on processed image, retrying with original")
                emitProgress(scanId, OCRPhase.IMAGE_PREPROCESSING, "Retrying with original image", 40.0, startTime)
                mlKitText = performTextRecognition(originalBitmap)
                android.util.Log.d("OCR", "Retry OCR result: ${mlKitText.text.length} characters")
            }
            
            // If recognition failed or returned minimal text, retry with grayscale
            if (mlKitText.text.trim().length < 10) {
                android.util.Log.d("OCR", "Still poor results, trying grayscale")
                emitProgress(scanId, OCRPhase.IMAGE_PREPROCESSING, "Retrying with grayscale", 45.0, startTime)
                val grayscaleBitmap = convertToGrayscale(originalBitmap)
                mlKitText = performTextRecognition(grayscaleBitmap)
                android.util.Log.d("OCR", "Grayscale OCR result: ${mlKitText.text.length} characters")
            }
            
            // If still failed, return error
            if (mlKitText.textBlocks.isEmpty()) {
                return@withContext OCRResult.Error(
                    scanId, 
                    "Text recognition failed. Please try with better lighting or manually enter the data."
                )
            }
            
            // Log extracted text for debugging
            android.util.Log.d("OCR", "Final text blocks: ${mlKitText.textBlocks.size}, Text preview: ${mlKitText.text.take(200)}")
            
            // CRITICAL: Filter ML Kit output to keep only relevant invoice data
            android.util.Log.d("OCR", "Applying intelligent text filtering...")
            val filteredResult = tesseractFilter.filterInvoiceText(mlKitText.text)
            
            android.util.Log.d("OCR", "Filtering results:")
            android.util.Log.d("OCR", "  - Removed ${filteredResult.removedLines.size} irrelevant lines")
            android.util.Log.d("OCR", "  - Kept ${filteredResult.filteredLineCount} relevant lines")
            android.util.Log.d("OCR", "  - Text reduction: ${String.format("%.1f", (1 - filteredResult.filteredText.length.toFloat() / filteredResult.originalText.length) * 100)}%")
            android.util.Log.d("OCR", "  - Product table detected: ${filteredResult.productTableDetected}")
            
            // Extract product lines specifically
            val productLines = tesseractFilter.extractProductLines(filteredResult.filteredText)
            android.util.Log.d("OCR", "Extracted ${productLines.size} product lines:")
            productLines.forEachIndexed { index, product ->
                android.util.Log.d("OCR", "  Product ${index + 1}: ${product.rawText.take(80)}")
                android.util.Log.d("OCR", "    Qty: ${product.quantity}, Rate: ${product.rate}, Amount: ${product.amount}")
            }
            
            // Convert ML Kit result to our format
            val textBlocks = mlKitText.textBlocks.map { block ->
                TextBlock(
                    blockId = block.boundingBox?.toString() ?: UUID.randomUUID().toString(),
                    text = block.text,
                    confidence = 0.8f, // ML Kit doesn't provide block-level confidence
                    boundingBox = block.boundingBox ?: Rect(),
                    lines = block.lines.map { line ->
                        TextLine(
                            text = line.text,
                            confidence = 0.8f,
                            boundingBox = line.boundingBox ?: Rect(),
                            angle = line.angle,
                            words = line.elements.map { element ->
                                TextWord(
                                    text = element.text,
                                    confidence = 0.8f,
                                    boundingBox = element.boundingBox ?: Rect()
                                )
                            }
                        )
                    },
                    recognizedLanguages = emptyList()
                )
            }
            val rawText = filteredResult.filteredText
            
            emitProgress(scanId, OCRPhase.FIELD_EXTRACTION, "Extracting fields", 60.0, startTime)
            
            // Extract structured fields
            val extractedFields = if (config.enableFieldExtraction) {
                extractFields(textBlocks, config)
            } else {
                emptyMap()
            }
            
            emitProgress(scanId, OCRPhase.VALIDATION, "Validating results", 80.0, startTime)
            
            // Validate extracted fields
            val validatedFields = if (config.enableValidation) {
                validateFields(extractedFields, config)
            } else {
                extractedFields
            }
            
            emitProgress(scanId, OCRPhase.POST_PROCESSING, "Post-processing", 90.0, startTime)
            
            // Apply post-processing
            val finalFields = if (config.postProcessingOptions.enableDataNormalization) {
                normalizeFieldData(validatedFields)
            } else {
                validatedFields
            }
            
            // Calculate confidence scores
            val confidence = calculateConfidenceScore(textBlocks, finalFields, imageQuality)
            
            // Detect template if enabled
            val templateId = if (config.fieldExtractionOptions.enableSmartFieldDetection) {
                templateMatcher.detectTemplate(config.documentType, finalFields)
            } else null
            
            val processingTime = System.currentTimeMillis() - startTime
            
            emitProgress(scanId, OCRPhase.COMPLETED, "Processing completed", 100.0, startTime)
            
            val scanResult = OCRScanResult(
                scanId = scanId,
                documentType = config.documentType,
                sourceImagePath = imageUri.toString(),
                processedImagePath = null, // Would save processed image if needed
                rawText = rawText,
                textBlocks = textBlocks,
                extractedFields = finalFields,
                confidence = confidence,
                processingTime = processingTime,
                templateId = templateId
            )
            
            OCRResult.Success(scanResult)
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            emitProgress(scanId, OCRPhase.FAILED, "Processing failed: ${e.message}", 0.0, startTime)
            OCRResult.Error(scanId, e.message ?: "OCR processing failed", processingTime)
        }
    }
    
    /**
     * Process image from camera
     */
    suspend fun processImageProxy(
        imageProxy: ImageProxy,
        config: OCRConfig
    ): OCRResult = withContext(Dispatchers.IO) {
        
        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(imageProxy)
            
            // Create temporary URI for processing
            val tempFile = File(context.cacheDir, "temp_ocr_${System.currentTimeMillis()}.jpg")
            saveBitmapToFile(bitmap, tempFile)
            val tempUri = Uri.fromFile(tempFile)
            
            // Process using main method
            val result = processImage(tempUri, config)
            
            // Cleanup temp file
            tempFile.delete()
            
            result
            
        } catch (e: Exception) {
            val scanId = UUID.randomUUID().toString()
            OCRResult.Error(scanId, "Failed to process camera image: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }
    
    /**
     * Batch process multiple images
     */
    suspend fun batchProcessImages(
        job: BatchOCRJob
    ): BatchOCRResult = withContext(Dispatchers.IO) {
        
        val results = mutableListOf<OCRScanResult>()
        val errors = mutableListOf<String>()
        var processedCount = 0
        
        try {
            for (imagePath in job.imageFiles) {
                try {
                    val imageUri = Uri.parse(imagePath)
                    when (val result = processImage(imageUri, job.config)) {
                        is OCRResult.Success -> {
                            results.add(result.scanResult)
                        }
                        is OCRResult.Error -> {
                            errors.add("${imagePath}: ${result.message}")
                        }
                    }
                } catch (e: Exception) {
                    errors.add("${imagePath}: ${e.message}")
                }
                
                processedCount++
                // Update batch progress would go here
            }
            
            BatchOCRResult.Success(
                jobId = job.jobId,
                successfulResults = results,
                errors = errors,
                totalProcessed = processedCount
            )
            
        } catch (e: Exception) {
            BatchOCRResult.Error(
                jobId = job.jobId,
                message = "Batch processing failed: ${e.message}",
                partialResults = results,
                totalProcessed = processedCount
            )
        }
    }
    
    // Private processing methods
    
    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun assessImageQuality(bitmap: Bitmap): ImageQualityMetrics {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Calculate sharpness (using Laplacian variance)
        val sharpness = calculateSharpness(pixels, width, height)
        
        // Calculate brightness
        val brightness = calculateBrightness(pixels)
        
        // Calculate contrast
        val contrast = calculateContrast(pixels)
        
        // Overall score
        val overallScore = (sharpness * 0.4f + brightness * 0.3f + contrast * 0.3f).coerceIn(0f, 1f)
        
        // Generate recommendations - more lenient thresholds
        val recommendations = mutableListOf<String>()
        if (sharpness < 0.15f) recommendations.add("Image is blurry, try steady camera")
        if (brightness < 0.2f) recommendations.add("Image too dark, improve lighting")
        if (brightness > 0.95f) recommendations.add("Image too bright, reduce lighting")
        if (contrast < 0.15f) recommendations.add("Poor contrast, adjust lighting angle")
        
        return ImageQualityMetrics(
            sharpness = sharpness,
            brightness = brightness,
            contrast = contrast,
            resolution = width to height,
            fileSize = bitmap.byteCount.toLong(),
            overallScore = overallScore,
            recommendations = recommendations
        )
    }
    
    private fun calculateSharpness(pixels: IntArray, width: Int, height: Int): Float {
        // Improved Laplacian variance calculation for better high-quality image detection
        var variance = 0.0
        val kernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 4, -1),
            intArrayOf(0, -1, 0)
        )
        
        // Sample every few pixels for faster processing on large images
        val step = maxOf(1, minOf(width, height) / 500)
        var sampleCount = 0
        
        for (y in 1 until height - 1 step step) {
            for (x in 1 until width - 1 step step) {
                var sum = 0.0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val gray = Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114
                        sum += gray * kernel[ky + 1][kx + 1]
                    }
                }
                variance += sum * sum
                sampleCount++
            }
        }
        
        variance /= sampleCount.toDouble()
        // Improved normalization for modern high-resolution cameras (like Samsung S23)
        // Higher quality images can have variance up to 50000+
        return (sqrt(variance) / 200.0).coerceIn(0.0, 1.0).toFloat()
    }
    
    private fun calculateBrightness(pixels: IntArray): Float {
        var totalBrightness = 0.0
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            totalBrightness += (r + g + b) / 3.0
        }
        return (totalBrightness / pixels.size / 255.0).toFloat()
    }
    
    private fun calculateContrast(pixels: IntArray): Float {
        val brightness = calculateBrightness(pixels) * 255
        var variance = 0.0
        
        for (pixel in pixels) {
            val gray = Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114
            variance += (gray - brightness).pow(2.0)
        }
        
        val standardDeviation = sqrt(variance / pixels.size)
        return (standardDeviation / 128.0).coerceIn(0.0, 1.0).toFloat()
    }
    
    private fun preprocessImage(bitmap: Bitmap, options: ImagePreprocessingOptions): Bitmap {
        var processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // IMPORTANT: Only resize if image is HUGE (>4096px) to preserve quality
        // Modern phones like Samsung S23 produce high-res images that NEED the resolution for OCR
        if (processedBitmap.width > TARGET_WIDTH || processedBitmap.height > TARGET_HEIGHT) {
            val scale = minOf(
                TARGET_WIDTH.toFloat() / processedBitmap.width,
                TARGET_HEIGHT.toFloat() / processedBitmap.height
            )
            // Only resize if scale < 0.75 (more than 25% reduction needed)
            if (scale < 0.75f) {
                val newWidth = (processedBitmap.width * scale).toInt()
                val newHeight = (processedBitmap.height * scale).toInt()
                processedBitmap = Bitmap.createScaledBitmap(processedBitmap, newWidth, newHeight, true)
            }
        }
        
        // Apply noise reduction ONLY if enabled
        if (options.enableNoiseReduction) {
            processedBitmap = applyGaussianBlur(processedBitmap, GAUSSIAN_BLUR_RADIUS)
        }
        
        // Enhance contrast ONLY if enabled - use minimal enhancement
        if (options.enableContrastEnhancement) {
            processedBitmap = adjustContrastAndBrightness(processedBitmap, CONTRAST_FACTOR, BRIGHTNESS_FACTOR)
        }
        
        // Apply sharpening ONLY if enabled
        if (options.enableSharpening) {
            processedBitmap = applySharpen(processedBitmap)
        }
        
        // Apply binarization ONLY if enabled
        if (options.enableBinarization) {
            processedBitmap = applyBinarization(processedBitmap)
        }
        
        // Deskewing would require more complex geometric transformations
        // This is a placeholder for deskewing implementation
        if (options.enableDeskewing) {
            // processedBitmap = deskewImage(processedBitmap)
        }
        
        return processedBitmap
    }
    
    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return grayscaleBitmap
    }
    
    private fun applyGaussianBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(blurredBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return blurredBitmap
    }
    
    private fun adjustContrastAndBrightness(bitmap: Bitmap, contrast: Float, brightness: Int): Bitmap {
        val adjustedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(adjustedBitmap)
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness.toFloat(),
                0f, contrast, 0f, 0f, brightness.toFloat(),
                0f, 0f, contrast, 0f, brightness.toFloat(),
                0f, 0f, 0f, 1f, 0f
            ))
        }
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return adjustedBitmap
    }
    
    private fun applySharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val result = IntArray(width * height)
        
        // Sharpening kernel
        val kernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 5, -1),
            intArrayOf(0, -1, 0)
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[ky + 1][kx + 1]
                        sumR += Color.red(pixel) * weight
                        sumG += Color.green(pixel) * weight
                        sumB += Color.blue(pixel) * weight
                    }
                }
                
                result[y * width + x] = Color.rgb(
                    sumR.coerceIn(0, 255),
                    sumG.coerceIn(0, 255),
                    sumB.coerceIn(0, 255)
                )
            }
        }
        
        val sharpenedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sharpenedBitmap.setPixels(result, 0, width, 0, 0, width, height)
        return sharpenedBitmap
    }
    
    private fun applyBinarization(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Calculate threshold using Otsu's method (simplified)
        val threshold = calculateOtsuThreshold(pixels)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val gray = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
            pixels[i] = if (gray > threshold) Color.WHITE else Color.BLACK
        }
        
        val binarizedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        binarizedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return binarizedBitmap
    }
    
    private fun calculateOtsuThreshold(pixels: IntArray): Int {
        // Simplified Otsu's thresholding
        val histogram = IntArray(256)
        
        // Build histogram
        for (pixel in pixels) {
            val gray = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
            histogram[gray]++
        }
        
        val totalPixels = pixels.size
        var sumB = 0
        var wB = 0
        var maximum = 0.0
        var threshold = 0
        
        val sum = histogram.indices.sumOf { it * histogram[it] }
        
        for (t in 0..255) {
            wB += histogram[t]
            if (wB == 0) continue
            
            val wF = totalPixels - wB
            if (wF == 0) break
            
            sumB += t * histogram[t]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            
            val varBetween = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            
            if (varBetween > maximum) {
                maximum = varBetween
                threshold = t
            }
        }
        
        return threshold
    }
    
    /**
     * Perform text recognition using ML Kit
     */
    private suspend fun performTextRecognition(bitmap: Bitmap): com.google.mlkit.vision.text.Text = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                continuation.resume(visionText)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }
    
    private suspend fun extractFields(
        textBlocks: List<TextBlock>,
        config: OCRConfig
    ): Map<String, ExtractedField> {
        
        val extractedFields = mutableMapOf<String, ExtractedField>()
        val allText = textBlocks.joinToString("\n") { it.text }
        
        // Get expected fields for document type
        val expectedFields = config.documentType.expectedFields
        
        // Use smart field detection if enabled
        if (config.fieldExtractionOptions.enableSmartFieldDetection) {
            val smartFields = smartFieldDetector.detectFields(textBlocks, config.documentType)
            extractedFields.putAll(smartFields)
        }
        
        // Pattern-based field extraction
        for (fieldName in expectedFields) {
            if (!extractedFields.containsKey(fieldName)) {
                val fieldType = mapFieldNameToType(fieldName)
                val extractedField = extractFieldByPattern(allText, textBlocks, fieldType)
                if (extractedField != null) {
                    extractedFields[fieldName] = extractedField
                }
            }
        }
        
        // Table extraction if enabled
        if (config.fieldExtractionOptions.enableTableExtraction) {
            val tableFields = extractTableData(textBlocks)
            extractedFields.putAll(tableFields)
        }
        
        // Key-value pair extraction if enabled
        if (config.fieldExtractionOptions.enableKeyValuePairing) {
            val keyValueFields = extractKeyValuePairs(textBlocks)
            extractedFields.putAll(keyValueFields)
        }
        
        return extractedFields
    }
    
    private fun extractFieldByPattern(
        text: String,
        textBlocks: List<TextBlock>,
        fieldType: FieldType
    ): ExtractedField? {
        
        val regex = fieldType.regex ?: return null
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        
        if (matcher.find()) {
            val rawValue = matcher.group()
            val processedValue = textProcessor.processField(rawValue, fieldType)
            
            // Find source text blocks
            val sourceBlocks = findSourceTextBlocks(rawValue, textBlocks)
            
            // Calculate bounding box
            val boundingBox = calculateFieldBoundingBox(sourceBlocks)
            
            return ExtractedField(
                fieldType = fieldType,
                rawValue = rawValue,
                processedValue = processedValue,
                confidence = calculateFieldConfidence(rawValue, fieldType),
                boundingBox = boundingBox,
                sourceTextBlocks = sourceBlocks.map { it.blockId },
                validationResult = FieldValidationResult(
                    isValid = true,
                    validationType = ValidationType.PATTERN_MATCHING,
                    confidence = 0.8f
                )
            )
        }
        
        return null
    }
    
    private fun extractTableData(textBlocks: List<TextBlock>): Map<String, ExtractedField> {
        // Simplified table extraction
        // In a real implementation, this would use more sophisticated table detection
        val tableFields = mutableMapOf<String, ExtractedField>()
        
        // Group text blocks by vertical position to identify rows
        val rows = textBlocks
            .sortedBy { it.boundingBox.top }
            .groupBy { it.boundingBox.top / 50 } // Group by approximate row
        
        // Look for patterns that suggest table structure
        for (rowBlocks in rows.values) {
            if (rowBlocks.size >= 2) {
                // Sort by horizontal position to identify columns
                val sortedBlocks = rowBlocks.sortedBy { it.boundingBox.left }
                
                // Try to identify quantity, rate, amount patterns
                if (sortedBlocks.size >= 3) {
                    val potentialQty = sortedBlocks[0].text
                    val potentialRate = sortedBlocks[1].text
                    val potentialAmount = sortedBlocks[2].text
                    
                    if (isNumeric(potentialQty) && isNumeric(potentialRate) && isNumeric(potentialAmount)) {
                        tableFields["item_quantity_${UUID.randomUUID()}"] = ExtractedField(
                            fieldType = FieldType.ITEM_QUANTITY,
                            rawValue = potentialQty,
                            processedValue = potentialQty,
                            confidence = 0.7f,
                            boundingBox = sortedBlocks[0].boundingBox,
                            sourceTextBlocks = listOf(sortedBlocks[0].blockId),
                            validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.7f)
                        )
                    }
                }
            }
        }
        
        return tableFields
    }
    
    private fun extractKeyValuePairs(textBlocks: List<TextBlock>): Map<String, ExtractedField> {
        val keyValueFields = mutableMapOf<String, ExtractedField>()
        
        // Common key patterns for invoices
        val keyPatterns = mapOf(
            "invoice.*?(?:number|no|#)" to FieldType.INVOICE_NUMBER,
            "date" to FieldType.INVOICE_DATE,
            "total" to FieldType.TOTAL_AMOUNT,
            "amount" to FieldType.TOTAL_AMOUNT,
            "gst.*?(?:number|no)" to FieldType.GST_NUMBER,
            "phone" to FieldType.PHONE_NUMBER,
            "email" to FieldType.EMAIL
        )
        
        for ((keyPattern, fieldType) in keyPatterns) {
            val regex = Pattern.compile("($keyPattern)\\s*:?\\s*([\\w\\s\\-\\.@/]+)", Pattern.CASE_INSENSITIVE)
            val fullText = textBlocks.joinToString(" ") { it.text }
            val matcher = regex.matcher(fullText)
            
            if (matcher.find()) {
                val value = matcher.group(2)?.trim()
                if (!value.isNullOrEmpty()) {
                    keyValueFields[fieldType.name.lowercase()] = ExtractedField(
                        fieldType = fieldType,
                        rawValue = value,
                        processedValue = textProcessor.processField(value, fieldType),
                        confidence = 0.75f,
                        boundingBox = null,
                        sourceTextBlocks = emptyList(),
                        validationResult = FieldValidationResult(true, ValidationType.PATTERN_MATCHING, confidence = 0.75f)
                    )
                }
            }
        }
        
        return keyValueFields
    }
    
    private fun validateFields(
        fields: Map<String, ExtractedField>,
        config: OCRConfig
    ): Map<String, ExtractedField> {
        
        return fields.mapValues { (_, field) ->
            val validationResult = when (field.fieldType.validationRule) {
                "Must be a valid number" -> validateNumericField(field.processedValue)
                "Must be valid date" -> validateDateField(field.processedValue)
                else -> field.validationResult
            }
            
            val suggestions = if (!validationResult.isValid && config.postProcessingOptions.enableSuggestions) {
                generateFieldSuggestions(field)
            } else {
                field.suggestions
            }
            
            field.copy(
                validationResult = validationResult,
                suggestions = suggestions
            )
        }
    }
    
    private fun normalizeFieldData(fields: Map<String, ExtractedField>): Map<String, ExtractedField> {
        return fields.mapValues { (_, field) ->
            val normalizedValue = when (field.fieldType.category) {
                FieldCategory.FINANCIAL -> normalizeFinancialValue(field.processedValue)
                FieldCategory.DATE -> normalizeDateValue(field.processedValue)
                FieldCategory.CONTACT -> normalizeContactValue(field.processedValue, field.fieldType)
                else -> field.processedValue
            }
            
            field.copy(processedValue = normalizedValue)
        }
    }
    
    private fun calculateConfidenceScore(
        textBlocks: List<TextBlock>,
        fields: Map<String, ExtractedField>,
        imageQuality: ImageQualityMetrics
    ): ConfidenceScore {
        
        val textRecognitionConfidence = if (textBlocks.isNotEmpty()) {
            textBlocks.map { it.confidence }.average().toFloat()
        } else 0f
        
        val fieldExtractionConfidence = if (fields.isNotEmpty()) {
            fields.values.map { it.confidence }.average().toFloat()
        } else 0f
        
        val validationConfidence = if (fields.isNotEmpty()) {
            fields.values.map { it.validationResult.confidence }.average().toFloat()
        } else 0f
        
        val completenessScore = calculateCompletenessScore(fields)
        
        val overallConfidence = (textRecognitionConfidence * 0.3f +
                fieldExtractionConfidence * 0.3f +
                validationConfidence * 0.2f +
                imageQuality.overallScore * 0.1f +
                completenessScore * 0.1f)
        
        return ConfidenceScore(
            overallConfidence = overallConfidence,
            textRecognitionConfidence = textRecognitionConfidence,
            fieldExtractionConfidence = fieldExtractionConfidence,
            validationConfidence = validationConfidence,
            qualityScore = imageQuality.overallScore,
            completenessScore = completenessScore
        )
    }
    
    // Utility methods
    
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }
    
    private fun mapFieldNameToType(fieldName: String): FieldType {
        return when (fieldName.lowercase()) {
            "invoice_number" -> FieldType.INVOICE_NUMBER
            "receipt_number" -> FieldType.RECEIPT_NUMBER
            "total_amount" -> FieldType.TOTAL_AMOUNT
            "subtotal" -> FieldType.SUBTOTAL
            "tax_amount" -> FieldType.TAX_AMOUNT
            "date" -> FieldType.INVOICE_DATE
            "vendor_name" -> FieldType.VENDOR_NAME
            "customer_name" -> FieldType.CUSTOMER_NAME
            "phone" -> FieldType.PHONE_NUMBER
            "email" -> FieldType.EMAIL
            "gst_number" -> FieldType.GST_NUMBER
            else -> FieldType.UNKNOWN
        }
    }
    
    private fun findSourceTextBlocks(value: String, textBlocks: List<TextBlock>): List<TextBlock> {
        return textBlocks.filter { block ->
            block.text.contains(value, ignoreCase = true)
        }
    }
    
    private fun calculateFieldBoundingBox(sourceBlocks: List<TextBlock>): Rect? {
        if (sourceBlocks.isEmpty()) return null
        
        val left = sourceBlocks.minOf { it.boundingBox.left }
        val top = sourceBlocks.minOf { it.boundingBox.top }
        val right = sourceBlocks.maxOf { it.boundingBox.right }
        val bottom = sourceBlocks.maxOf { it.boundingBox.bottom }
        
        return Rect(left, top, right, bottom)
    }
    
    private fun calculateFieldConfidence(value: String, fieldType: FieldType): Float {
        return when {
            fieldType.regex != null -> {
                val pattern = Pattern.compile(fieldType.regex)
                if (pattern.matcher(value).matches()) 0.9f else 0.6f
            }
            value.isNotBlank() -> 0.7f
            else -> 0.3f
        }
    }
    
    private fun isNumeric(text: String): Boolean {
        return text.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() != null
    }
    
    private fun validateNumericField(value: String): FieldValidationResult {
        val isValid = value.toDoubleOrNull() != null
        return FieldValidationResult(
            isValid = isValid,
            validationType = ValidationType.FORMAT_VALIDATION,
            errorMessage = if (!isValid) "Invalid number format" else null,
            confidence = if (isValid) 0.9f else 0.1f
        )
    }
    
    private fun validateDateField(value: String): FieldValidationResult {
        // Simplified date validation
        val datePattern = Pattern.compile("""\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}""")
        val isValid = datePattern.matcher(value).matches()
        return FieldValidationResult(
            isValid = isValid,
            validationType = ValidationType.FORMAT_VALIDATION,
            errorMessage = if (!isValid) "Invalid date format" else null,
            confidence = if (isValid) 0.9f else 0.1f
        )
    }
    
    private fun generateFieldSuggestions(field: ExtractedField): List<String> {
        return when (field.fieldType.category) {
            FieldCategory.FINANCIAL -> {
                // Try to extract numbers from text
                val numbers = Regex("""\\d+\\.?\\d*""").findAll(field.rawValue)
                numbers.map { it.value }.toList()
            }
            FieldCategory.DATE -> {
                // Try different date formats
                listOf(
                    field.rawValue.replace("-", "/"),
                    field.rawValue.replace("/", "-"),
                    field.rawValue.replace(".", "/")
                )
            }
            else -> emptyList()
        }
    }
    
    private fun normalizeFinancialValue(value: String): String {
        return value.replace("[^0-9.]".toRegex(), "")
    }
    
    private fun normalizeDateValue(value: String): String {
        return value.replace("[^0-9/-]".toRegex(), "")
    }
    
    private fun normalizeContactValue(value: String, fieldType: FieldType): String {
        return when (fieldType) {
            FieldType.PHONE_NUMBER -> value.replace("[^0-9+()-]".toRegex(), "")
            FieldType.EMAIL -> value.lowercase().trim()
            else -> value.trim()
        }
    }
    
    private fun calculateCompletenessScore(fields: Map<String, ExtractedField>): Float {
        if (fields.isEmpty()) return 0f
        
        val totalExpectedFields = 10 // Expected number of fields for a complete document
        val extractedFields = fields.size
        return (extractedFields.toFloat() / totalExpectedFields).coerceAtMost(1f)
    }
    
    private suspend fun emitProgress(
        scanId: String,
        phase: OCRPhase,
        operation: String,
        percentage: Double,
        startTime: Long
    ) {
        val progress = OCRProgress(
            scanId = scanId,
            phase = phase,
            currentOperation = operation,
            percentage = percentage,
            processingTime = System.currentTimeMillis() - startTime,
            estimatedTimeRemaining = if (percentage > 0) {
                ((System.currentTimeMillis() - startTime) * (100 - percentage) / percentage).toLong()
            } else 0L
        )
        _ocrProgress.emit(progress)
    }
}

/**
 * OCR processing result
 */
sealed class OCRResult {
    data class Success(val scanResult: OCRScanResult) : OCRResult()
    data class Error(
        val scanId: String, 
        val message: String, 
        val processingTime: Long = 0L
    ) : OCRResult()
}

/**
 * Batch OCR processing result
 */
sealed class BatchOCRResult {
    data class Success(
        val jobId: String,
        val successfulResults: List<OCRScanResult>,
        val errors: List<String>,
        val totalProcessed: Int
    ) : BatchOCRResult()
    
    data class Error(
        val jobId: String,
        val message: String,
        val partialResults: List<OCRScanResult>,
        val totalProcessed: Int
    ) : BatchOCRResult()
}

/**
 * Placeholder interfaces for injection
 */
interface TextProcessingPipeline {
    fun processField(value: String, fieldType: FieldType): String
}

interface SmartFieldDetector {
    suspend fun detectFields(textBlocks: List<TextBlock>, documentType: DocumentType): Map<String, ExtractedField>
}

interface TemplateMatchingService {
    suspend fun detectTemplate(documentType: DocumentType, fields: Map<String, ExtractedField>): String?
}