package com.billme.app.core.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage

/**
 * Image Preprocessor for better IMEI text recognition
 * Enhances image quality before ML Kit processing
 */
object ImagePreprocessor {
    
    /**
     * Enhance image contrast for better text recognition
     * Useful for labels with poor lighting or faded text
     */
    fun enhanceContrast(bitmap: Bitmap, contrast: Float = 1.5f): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            // Adjust contrast
            val scale = contrast
            val translate = (-(0.5f * scale) + 0.5f) * 255f
            
            set(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Increase brightness for images taken in low light
     */
    fun increaseBrightness(bitmap: Bitmap, brightnessValue: Float = 30f): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, brightnessValue,
                0f, 1f, 0f, 0f, brightnessValue,
                0f, 0f, 1f, 0f, brightnessValue,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Sharpen image for clearer text
     */
    fun sharpenImage(bitmap: Bitmap): Bitmap {
        // Simple sharpening using color matrix
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                0f, -1f, 0f, 0f, 0f,
                -1f, 5f, -1f, 0f, 0f,
                0f, -1f, 0f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Convert to grayscale for better text contrast
     * Often improves OCR accuracy
     */
    fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Automatic image enhancement - tries to detect and fix common issues
     */
    fun autoEnhance(bitmap: Bitmap, enhancementLevel: EnhancementLevel = EnhancementLevel.MEDIUM): Bitmap {
        var enhanced = bitmap
        
        when (enhancementLevel) {
            EnhancementLevel.LIGHT -> {
                // Minimal enhancement
                enhanced = enhanceContrast(enhanced, 1.2f)
            }
            EnhancementLevel.MEDIUM -> {
                // Balanced enhancement
                enhanced = convertToGrayscale(enhanced)
                enhanced = enhanceContrast(enhanced, 1.4f)
            }
            EnhancementLevel.HEAVY -> {
                // Maximum enhancement for difficult cases
                enhanced = convertToGrayscale(enhanced)
                enhanced = increaseBrightness(enhanced, 20f)
                enhanced = enhanceContrast(enhanced, 1.6f)
                enhanced = sharpenImage(enhanced)
            }
        }
        
        return enhanced
    }
    
    /**
     * Check if image might benefit from enhancement
     */
    fun needsEnhancement(bitmap: Bitmap): Boolean {
        // Simple heuristic: check average brightness
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var totalBrightness = 0L
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalBrightness += (r + g + b) / 3
        }
        
        val avgBrightness = totalBrightness / pixels.size
        
        // If average brightness is too low or too high, enhancement might help
        return avgBrightness < 100 || avgBrightness > 200
    }
    
    /**
     * Create InputImage from ImageProxy with optional enhancement
     */
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun createInputImage(
        imageProxy: ImageProxy, 
        enableEnhancement: Boolean = false,
        enhancementLevel: EnhancementLevel = EnhancementLevel.MEDIUM
    ): InputImage {
        val mediaImage = imageProxy.image
        
        return if (enableEnhancement && mediaImage != null) {
            // Convert to bitmap, enhance, then create InputImage
            val bitmap = imageProxy.toBitmap()
            val enhanced = if (needsEnhancement(bitmap)) {
                autoEnhance(bitmap, enhancementLevel)
            } else {
                bitmap
            }
            InputImage.fromBitmap(enhanced, 0)
        } else {
            // Use original image
            InputImage.fromMediaImage(mediaImage!!, imageProxy.imageInfo.rotationDegrees)
        }
    }
    
    enum class EnhancementLevel {
        LIGHT,      // Minimal processing, faster
        MEDIUM,     // Balanced (recommended)
        HEAVY       // Maximum enhancement, slower
    }
}
