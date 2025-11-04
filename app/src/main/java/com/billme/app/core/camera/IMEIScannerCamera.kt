package com.billme.app.core.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.billme.app.core.util.ImeiValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IMEI Scanner using CameraX and ML Kit Text Recognition
 */
@OptIn(ExperimentalGetImage::class)
@Singleton
class IMEIScannerCamera @Inject constructor() {
    
    private var cameraExecutor: ExecutorService? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    private val _scanResult = MutableStateFlow<IMEIScanState>(IMEIScanState.Idle)
    val scanResult: StateFlow<IMEIScanState> = _scanResult.asStateFlow()
    
    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()
    
    private var camera: Camera? = null
    private var isScanning = false
    private var scanMode = ScanMode.SINGLE
    private val scannedIMEIs = mutableSetOf<String>()
    private val recentDetections = mutableMapOf<String, Int>() // IMEI -> confidence count
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Start camera and IMEI scanning
     */
    fun startScanning(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        mode: ScanMode = ScanMode.SINGLE
    ) {
        scanMode = mode
        isScanning = true
        scannedIMEIs.clear()
        _scanResult.value = IMEIScanState.Scanning(emptyList())
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Check if camera is available
                if (!context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)) {
                    _scanResult.value = IMEIScanState.Error("No camera available on this device")
                    return@addListener
                }
                
                // Preview with improved configuration
                val preview = Preview.Builder()
                    .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // Image analysis with optimized settings for tilt handling
                imageAnalyzer = ImageAnalysis.Builder()
                    // Higher resolution for better text recognition even at angles
                    .setTargetResolution(android.util.Size(1920, 1080))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    // YUV format is better for ML Kit
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor!!) { imageProxy ->
                            processImageProxy(imageProxy)
                        }
                    }
                
                // Select back camera with proper error handling
                val cameraSelector = try {
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                } catch (e: Exception) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                
                // Verify camera is bound and preview is working
                if (camera == null) {
                    _scanResult.value = IMEIScanState.Error("Failed to bind camera to lifecycle")
                    return@addListener
                }
                
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("CAMERA_DISABLED") == true -> 
                        "Camera is disabled. Please enable it in device settings."
                    e.message?.contains("CAMERA_IN_USE") == true -> 
                        "Camera is being used by another app. Please close other camera apps."
                    e.message?.contains("MAX_CAMERAS_IN_USE") == true -> 
                        "Maximum cameras in use. Please close other camera apps."
                    e.message?.contains("CAMERA_UNAVAILABLE") == true -> 
                        "Camera is temporarily unavailable. Please try again."
                    else -> 
                        "Camera initialization failed: ${e.message ?: "Unknown error"}"
                }
                _scanResult.value = IMEIScanState.Error(errorMessage)
            }
            
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * Process camera frame for text recognition
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanning) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    
                    // Quick check if text likely contains IMEI
                    if (text.isNotBlank() && 
                        (text.contains("IMEI", ignoreCase = true) || Regex("\\d{15}").containsMatchIn(text))) {
                        
                        // Try to extract IMEIs from recognized text
                        val extractedIMEIs = extractIMEIsFromText(text)
                        
                        if (extractedIMEIs.isNotEmpty()) {
                            // Apply confidence scoring
                            extractedIMEIs.forEach { imei ->
                                val count = recentDetections.getOrDefault(imei, 0) + 1
                                recentDetections[imei] = count
                            }
                            
                            // Only use IMEIs detected multiple times
                            val confirmedIMEIs = extractedIMEIs.filter { imei ->
                                recentDetections.getOrDefault(imei, 0) >= 2
                            }
                            
                            if (confirmedIMEIs.isNotEmpty()) {
                                handleDetectedIMEIs(confirmedIMEIs)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Continue scanning even on error
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    /**
     * Extract IMEIs from recognized text - Optimized for tilted labels and real phone labels
     * Enhanced to handle various angles and perspectives
     */
    private fun extractIMEIsFromText(text: String): List<String> {
        val imeis = mutableListOf<String>()
        
        // Normalize text - handle multiple forms of whitespace and line breaks
        val cleanText = text
            .replace("\\s+".toRegex(), " ")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("O", "0")  // Common OCR mistake
            .replace("o", "0")  // Common OCR mistake
            .replace("l", "1")  // Common OCR mistake (lowercase L)
            .replace("I", "1")  // Common OCR mistake (uppercase I)
            .trim()
        
        // Pattern 1: IMEI1: and IMEI2: (most common format)
        val imei1Pattern = Regex("IMEI\\s*1\\s*[:\\s]+(\\d{15})\\b", RegexOption.IGNORE_CASE)
        val imei2Pattern = Regex("IMEI\\s*2\\s*[:\\s]+(\\d{15})\\b", RegexOption.IGNORE_CASE)
        
        imei1Pattern.find(cleanText)?.let { match ->
            val imei = match.groupValues[1]
            if (ImeiValidator.isValidImei(imei)) {
                imeis.add(imei)
            }
        }
        
        imei2Pattern.find(cleanText)?.let { match ->
            val imei = match.groupValues[1]
            if (ImeiValidator.isValidImei(imei) && !imeis.contains(imei)) {
                imeis.add(imei)
            }
        }
        
        // Pattern 2: IMEI 1: and IMEI 2: (with space)
        if (imeis.isEmpty()) {
            val spaceImei1Pattern = Regex("IMEI\\s+1\\s*[:\\s]+(\\d{15})\\b", RegexOption.IGNORE_CASE)
            val spaceImei2Pattern = Regex("IMEI\\s+2\\s*[:\\s]+(\\d{15})\\b", RegexOption.IGNORE_CASE)
            
            spaceImei1Pattern.find(cleanText)?.let { match ->
                val imei = match.groupValues[1]
                if (ImeiValidator.isValidImei(imei)) {
                    imeis.add(imei)
                }
            }
            
            spaceImei2Pattern.find(cleanText)?.let { match ->
                val imei = match.groupValues[1]
                if (ImeiValidator.isValidImei(imei) && !imeis.contains(imei)) {
                    imeis.add(imei)
                }
            }
        }
        
        // Pattern 3: Simple IMEI: (without number)
        if (imeis.isEmpty()) {
            val simplePattern = Regex("IMEI\\s*[:\\s]+(\\d{15})\\b", RegexOption.IGNORE_CASE)
            simplePattern.findAll(cleanText).forEach { match ->
                val imei = match.groupValues[1]
                if (ImeiValidator.isValidImei(imei) && !imeis.contains(imei)) {
                    imeis.add(imei)
                }
            }
        }
        
        // Pattern 4: Any 15-digit sequence with enhanced filtering (fallback)
        if (imeis.isEmpty()) {
            // More flexible pattern to handle spaces and special chars from OCR errors
            val flexiblePattern = Regex("\\b\\d[\\d\\s]{13,}\\d\\b")
            flexiblePattern.findAll(cleanText).forEach { match ->
                // Extract only digits
                val potentialIMEI = match.value.filter { it.isDigit() }
                
                if (potentialIMEI.length == 15) {
                    // Enhanced filtering for better accuracy
                    val hasRepeatingPattern = potentialIMEI.chunked(5).distinct().size == 1
                    val isAllSame = potentialIMEI.toSet().size == 1
                    val hasEnoughVariety = potentialIMEI.toSet().size >= 5
                    
                    // Check for sequential patterns (123456789012345)
                    val isSequential = potentialIMEI.zipWithNext().all { (a, b) -> 
                        b.digitToInt() == (a.digitToInt() + 1) % 10 
                    }
                    
                    if (ImeiValidator.isValidImei(potentialIMEI) && 
                        !hasRepeatingPattern && 
                        !isAllSame &&
                        !isSequential &&
                        hasEnoughVariety &&
                        !imeis.contains(potentialIMEI)) {
                        imeis.add(potentialIMEI)
                    }
                }
            }
        }
        
        return imeis
    }
    
    /**
     * Handle detected IMEIs based on scan mode
     */
    private fun handleDetectedIMEIs(imeis: List<String>) {
        when (scanMode) {
            ScanMode.SINGLE -> {
                // Take first valid IMEI and stop
                val imei = imeis.firstOrNull()
                if (imei != null) {
                    isScanning = false
                    _scanResult.value = IMEIScanState.Success(
                        listOf(ScannedIMEI(imei, ImeiValidator.formatImei(imei) ?: imei))
                    )
                }
            }
            ScanMode.DUAL -> {
                // Look for two IMEIs
                if (imeis.size >= 2) {
                    isScanning = false
                    _scanResult.value = IMEIScanState.Success(
                        imeis.take(2).map { 
                            ScannedIMEI(it, ImeiValidator.formatImei(it) ?: it) 
                        }
                    )
                } else if (imeis.isNotEmpty()) {
                    _scanResult.value = IMEIScanState.Scanning(
                        imeis.map { ScannedIMEI(it, ImeiValidator.formatImei(it) ?: it) },
                        "Found 1 IMEI, scanning for second..."
                    )
                }
            }
            ScanMode.MULTIPLE -> {
                // Collect multiple unique IMEIs
                imeis.forEach { scannedIMEIs.add(it) }
                _scanResult.value = IMEIScanState.Scanning(
                    scannedIMEIs.map { 
                        ScannedIMEI(it, ImeiValidator.formatImei(it) ?: it) 
                    },
                    "Scanned ${scannedIMEIs.size} IMEI(s)"
                )
            }
        }
    }
    
    /**
     * Toggle camera torch/flash
     */
    fun toggleTorch() {
        camera?.let {
            val currentState = it.cameraInfo.torchState.value == TorchState.ON
            it.cameraControl.enableTorch(!currentState)
            _isTorchOn.value = !currentState
        }
    }
    
    /**
     * Stop scanning and release resources
     */
    fun stopScanning() {
        isScanning = false
        imageAnalyzer?.clearAnalyzer()
        cameraProvider?.unbindAll()
        cameraExecutor?.shutdown()
        textRecognizer.close()
    }
    
    /**
     * Retry scanning after error
     */
    fun retryScan() {
        scannedIMEIs.clear()
        recentDetections.clear()
        isScanning = true
        _scanResult.value = IMEIScanState.Scanning(emptyList())
    }
    
    /**
     * Complete multi-IMEI scan
     */
    fun completeMultiScan() {
        if (scanMode == ScanMode.MULTIPLE && scannedIMEIs.isNotEmpty()) {
            isScanning = false
            _scanResult.value = IMEIScanState.Success(
                scannedIMEIs.map { 
                    ScannedIMEI(it, ImeiValidator.formatImei(it) ?: it) 
                }
            )
        }
    }
}

/**
 * IMEI scan state
 */
sealed class IMEIScanState {
    object Idle : IMEIScanState()
    data class Scanning(
        val detectedIMEIs: List<ScannedIMEI>,
        val message: String? = null
    ) : IMEIScanState()
    data class Success(val imeis: List<ScannedIMEI>) : IMEIScanState()
    data class Error(val message: String) : IMEIScanState()
}

/**
 * Scanned IMEI data
 */
data class ScannedIMEI(
    val imei: String,
    val formattedIMEI: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Scan modes
 */
enum class ScanMode {
    SINGLE,    // Scan one IMEI and stop
    DUAL,      // Scan two IMEIs (for dual SIM phones)
    MULTIPLE   // Scan multiple IMEIs (bulk scanning)
}
