package com.billme.app.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Utility functions for formatting currency, percentages, and numbers
 */

/**
 * Format BigDecimal as currency (Indian Rupees)
 */
fun BigDecimal.formatCurrency(): String {
    val formatter = DecimalFormat("₹#,##0.00")
    return formatter.format(this.setScale(2, RoundingMode.HALF_UP))
}

/**
 * Format Double as percentage
 */
fun Double.formatPercentage(): String {
    return "${this}%"
}

/**
 * Format BigDecimal as percentage
 */
fun BigDecimal.formatPercentage(): String {
    return "${this.setScale(2, RoundingMode.HALF_UP)}%"
}

/**
 * Format number in Indian numbering system (lakhs, crores)
 */
fun BigDecimal.formatIndianNumber(): String {
    val formatter = DecimalFormat("#,##,##0.00", DecimalFormatSymbols(Locale("en", "IN")))
    return formatter.format(this)
}

/**
 * Format currency with Indian numbering system
 */
fun BigDecimal.formatIndianCurrency(): String {
    val formatter = DecimalFormat("₹#,##,##0.00", DecimalFormatSymbols(Locale("en", "IN")))
    return formatter.format(this.setScale(2, RoundingMode.HALF_UP))
}

/**
 * Format number as compact string (1K, 1M, etc.)
 */
fun BigDecimal.formatCompact(): String {
    val value = this.toDouble()
    return when {
        value >= 10000000 -> "₹${(value / 10000000).format(1)}Cr"
        value >= 100000 -> "₹${(value / 100000).format(1)}L"
        value >= 1000 -> "₹${(value / 1000).format(1)}K"
        else -> this.formatCurrency()
    }
}

/**
 * Helper function to format double with specified decimal places
 */
private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}

/**
 * Format GSTIN with proper spacing
 */
fun String.formatGSTIN(): String {
    if (this.length != 15) return this
    return "${this.substring(0, 2)} ${this.substring(2, 7)} ${this.substring(7, 12)} ${this.substring(12)}"
}

/**
 * Format phone number (Indian format)
 */
fun String.formatPhoneNumber(): String {
    val cleaned = this.replace(Regex("[^0-9]"), "")
    return when {
        cleaned.length == 10 -> "+91 ${cleaned.substring(0, 5)} ${cleaned.substring(5)}"
        cleaned.length == 11 && cleaned.startsWith("0") -> "+91 ${cleaned.substring(1, 6)} ${cleaned.substring(6)}"
        cleaned.length == 12 && cleaned.startsWith("91") -> "+${cleaned.substring(0, 2)} ${cleaned.substring(2, 7)} ${cleaned.substring(7)}"
        cleaned.length == 13 && cleaned.startsWith("091") -> "+${cleaned.substring(1, 3)} ${cleaned.substring(3, 8)} ${cleaned.substring(8)}"
        else -> this
    }
}

/**
 * Mask sensitive information (like account numbers, partial GSTIN)
 */
fun String.maskSensitive(visibleChars: Int = 4): String {
    if (this.length <= visibleChars) return this
    val visible = this.takeLast(visibleChars)
    val masked = "*".repeat(this.length - visibleChars)
    return "$masked$visible"
}

/**
 * Format date for invoice display
 */
fun kotlinx.datetime.Instant.formatForInvoice(): String {
    val localDateTime = this.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val date = localDateTime.date
    return "${date.dayOfMonth.toString().padStart(2, '0')}-${date.monthNumber.toString().padStart(2, '0')}-${date.year}"
}

/**
 * Format datetime for invoice display
 */
fun kotlinx.datetime.Instant.formatDateTimeForInvoice(): String {
    val localDateTime = this.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val date = localDateTime.date
    val time = localDateTime.time
    return "${date.dayOfMonth.toString().padStart(2, '0')}-${date.monthNumber.toString().padStart(2, '0')}-${date.year} " +
            "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

/**
 * Format double with locale awareness
 */
fun Double.formatLocale(format: String): String {
    return String.format(Locale.US, format, this)
}

/**
 * Format float with locale awareness
 */
fun Float.formatLocale(format: String): String {
    return String.format(Locale.US, format, this)
}

/**
 * Format integer with locale awareness
 */
fun Int.formatLocale(format: String): String {
    return String.format(Locale.US, format, this)
}