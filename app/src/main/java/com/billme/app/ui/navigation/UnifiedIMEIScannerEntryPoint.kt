package com.billme.app.ui.navigation

import com.billme.app.core.scanner.UnifiedIMEIScanner
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UnifiedIMEIScannerEntryPoint {
    fun unifiedIMEIScanner(): UnifiedIMEIScanner
}
