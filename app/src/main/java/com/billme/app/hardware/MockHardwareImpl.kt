package com.billme.app.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock barcode scanner implementation for development and testing
 */
class MockBarcodeScanner(private val config: ScannerConfig = ScannerConfig()) : BarcodeScanner {
    
    private val _state = MutableStateFlow(ScannerState.IDLE)
    override val state: StateFlow<ScannerState> = _state.asStateFlow()
    
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    override val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()
    
    private var hasPermissionGranted = false
    
    override suspend fun startScanning(): Boolean {
        if (!hasPermissionGranted) {
            _state.value = ScannerState.PERMISSION_REQUIRED
            return false
        }
        
        _state.value = ScannerState.SCANNING
        
        // Simulate scanning delay
        delay(2000)
        
        // Simulate successful scan with mock data
        val mockResults = listOf(
            ScanResult("123456789012", BarcodeFormat.EAN_13),
            ScanResult("IMEI123456789012345", BarcodeFormat.CODE_128),
            ScanResult("QR_TEST_DATA", BarcodeFormat.QR_CODE),
            ScanResult("987654321098", BarcodeFormat.EAN_13)
        )
        
        _scanResult.value = mockResults.random()
        _state.value = ScannerState.IDLE
        
        return true
    }
    
    override suspend fun stopScanning() {
        _state.value = ScannerState.IDLE
    }
    
    override fun hasPermission(): Boolean = hasPermissionGranted
    
    override suspend fun requestPermission(): Boolean {
        delay(500) // Simulate permission dialog
        hasPermissionGranted = true
        return true
    }
    
    override fun isAvailable(): Boolean = true
    
    override fun release() {
        _state.value = ScannerState.IDLE
        _scanResult.value = null
    }
}

/**
 * Mock thermal printer implementation for development and testing
 */
class MockThermalPrinter(private val config: PrinterConfig = PrinterConfig()) : ThermalPrinter {
    
    private val _state = MutableStateFlow(PrinterState.DISCONNECTED)
    override val state: StateFlow<PrinterState> = _state.asStateFlow()
    
    private var isConnected = false
    
    override suspend fun connect(deviceAddress: String): Boolean {
        _state.value = PrinterState.CONNECTING
        delay(1000) // Simulate connection delay
        
        isConnected = true
        _state.value = PrinterState.CONNECTED
        return true
    }
    
    override suspend fun disconnect() {
        isConnected = false
        _state.value = PrinterState.DISCONNECTED
    }
    
    override suspend fun print(content: ReceiptContent): PrintResult {
        if (!isConnected) {
            return PrintResult.Error("Printer not connected")
        }
        
        _state.value = PrinterState.PRINTING
        
        // Simulate printing process
        delay(2000)
        
        // Mock print content processing
        val commands = content.build()
        println("Mock Print Job:")
        commands.forEach { command ->
            when (command) {
                is PrintCommand.Text -> println("TEXT: ${command.content} (${command.alignment}, ${command.size})")
                is PrintCommand.BoldText -> println("BOLD: ${command.content} (${command.alignment})")
                is PrintCommand.Line -> println("LINE: ${command.char.toString().repeat(command.length)}")
                is PrintCommand.NewLine -> println("NEWLINE: ${command.count}")
                is PrintCommand.QRCode -> println("QR_CODE: ${command.content} (${command.size}x${command.size})")
            }
        }
        println("--- End of Receipt ---")
        
        _state.value = PrinterState.CONNECTED
        return PrintResult.Success
    }
    
    override suspend fun printText(text: String): PrintResult {
        if (!isConnected) {
            return PrintResult.Error("Printer not connected")
        }
        
        _state.value = PrinterState.PRINTING
        delay(1000)
        
        println("Mock Print: $text")
        
        _state.value = PrinterState.CONNECTED
        return PrintResult.Success
    }
    
    override fun isReady(): Boolean = isConnected && _state.value == PrinterState.CONNECTED
    
    override suspend fun getAvailablePrinters(): List<String> {
        delay(500) // Simulate device scan
        return listOf(
            "Mock Bluetooth Printer 1",
            "Mock Bluetooth Printer 2",
            "Mock USB Printer"
        )
    }
    
    override suspend fun testConnection(): Boolean {
        if (!isConnected) return false
        
        delay(200)
        return true
    }
    
    override fun release() {
        isConnected = false
        _state.value = PrinterState.DISCONNECTED
    }
}

/**
 * Hardware service provider for dependency injection
 */
object MockHardwareProvider {
    
    fun provideBarcodeScanner(config: ScannerConfig = ScannerConfig()): BarcodeScanner {
        return MockBarcodeScanner(config)
    }
    
    fun provideThermalPrinter(config: PrinterConfig = PrinterConfig()): ThermalPrinter {
        return MockThermalPrinter(config)
    }
}