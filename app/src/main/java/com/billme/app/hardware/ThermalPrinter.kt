package com.billme.app.hardware

import kotlinx.coroutines.flow.StateFlow

/**
 * Printer connection states
 */
enum class PrinterState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PRINTING,
    ERROR,
    OUT_OF_PAPER
}

/**
 * Print job result
 */
sealed class PrintResult {
    object Success : PrintResult()
    data class Error(val message: String, val cause: Throwable? = null) : PrintResult()
}

/**
 * Text alignment options
 */
enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT
}

/**
 * Text size options
 */
enum class TextSize {
    SMALL,
    NORMAL,
    LARGE,
    EXTRA_LARGE
}

/**
 * Receipt content builder
 */
class ReceiptContent {
    private val commands = mutableListOf<PrintCommand>()
    
    fun text(content: String, alignment: TextAlignment = TextAlignment.LEFT, size: TextSize = TextSize.NORMAL): ReceiptContent {
        commands.add(PrintCommand.Text(content, alignment, size))
        return this
    }
    
    fun bold(content: String, alignment: TextAlignment = TextAlignment.LEFT): ReceiptContent {
        commands.add(PrintCommand.BoldText(content, alignment))
        return this
    }
    
    fun line(char: Char = '-', length: Int = 32): ReceiptContent {
        commands.add(PrintCommand.Line(char, length))
        return this
    }
    
    fun newLine(count: Int = 1): ReceiptContent {
        commands.add(PrintCommand.NewLine(count))
        return this
    }
    
    fun qrCode(content: String, size: Int = 200): ReceiptContent {
        commands.add(PrintCommand.QRCode(content, size))
        return this
    }
    
    internal fun build(): List<PrintCommand> = commands.toList()
}

/**
 * Print command types
 */
sealed class PrintCommand {
    data class Text(val content: String, val alignment: TextAlignment, val size: TextSize) : PrintCommand()
    data class BoldText(val content: String, val alignment: TextAlignment) : PrintCommand()
    data class Line(val char: Char, val length: Int) : PrintCommand()
    data class NewLine(val count: Int) : PrintCommand()
    data class QRCode(val content: String, val size: Int) : PrintCommand()
}

/**
 * Printer configuration
 */
data class PrinterConfig(
    val paperWidth: Int = 58, // mm
    val charactersPerLine: Int = 32,
    val enableBeep: Boolean = true,
    val autoReconnect: Boolean = true,
    val connectionTimeout: Long = 5000L // ms
)

/**
 * Thermal printer interface for hardware abstraction
 */
interface ThermalPrinter {
    
    /**
     * Current printer state
     */
    val state: StateFlow<PrinterState>
    
    /**
     * Connect to printer
     * @param deviceAddress Bluetooth address or USB device path
     * @return true if connection initiated successfully
     */
    suspend fun connect(deviceAddress: String): Boolean
    
    /**
     * Disconnect from printer
     */
    suspend fun disconnect()
    
    /**
     * Print receipt content
     * @param content Receipt content to print
     * @return Print result
     */
    suspend fun print(content: ReceiptContent): PrintResult
    
    /**
     * Print raw text (for testing)
     * @param text Text to print
     * @return Print result
     */
    suspend fun printText(text: String): PrintResult
    
    /**
     * Check if printer is connected and ready
     */
    fun isReady(): Boolean
    
    /**
     * Get list of available printers
     * @return List of device addresses/names
     */
    suspend fun getAvailablePrinters(): List<String>
    
    /**
     * Test printer connection
     * @return true if printer responds
     */
    suspend fun testConnection(): Boolean
    
    /**
     * Release printer resources
     */
    fun release()
}

/**
 * Convenience function to build receipt content
 */
fun buildReceipt(builder: ReceiptContent.() -> Unit): ReceiptContent {
    return ReceiptContent().apply(builder)
}