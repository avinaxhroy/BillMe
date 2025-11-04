package com.billme.app.core.ocr.di

import com.billme.app.core.ocr.SmartFieldDetector
import com.billme.app.core.ocr.TemplateMatchingService
import com.billme.app.core.ocr.TextProcessingPipeline
import com.billme.app.core.ocr.impl.SmartFieldDetectorImpl
import com.billme.app.core.ocr.impl.TemplateMatchingServiceImpl
import com.billme.app.core.ocr.impl.TextProcessingPipelineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for OCR dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OCRModule {
    
    @Binds
    @Singleton
    abstract fun bindTextProcessingPipeline(
        impl: TextProcessingPipelineImpl
    ): TextProcessingPipeline
    
    @Binds
    @Singleton
    abstract fun bindSmartFieldDetector(
        impl: SmartFieldDetectorImpl
    ): SmartFieldDetector
    
    @Binds
    @Singleton
    abstract fun bindTemplateMatchingService(
        impl: TemplateMatchingServiceImpl
    ): TemplateMatchingService
}
