package com.billme.app.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility to convert numbers to words for invoice amounts (Indian format)
 */
@Singleton
class NumberToWordsConverter @Inject constructor() {
    
    private val ones = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )
    
    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )
    
    private val thousands = arrayOf(
        "", "Thousand", "Lakh", "Crore"
    )
    
    /**
     * Convert BigDecimal amount to words in Indian format
     */
    fun convert(amount: BigDecimal): String {
        if (amount == BigDecimal.ZERO) {
            return "Zero Rupees Only"
        }
        
        // Split into rupees and paise
        val rupees = amount.setScale(0, RoundingMode.DOWN).toLong()
        val paise = ((amount - BigDecimal.valueOf(rupees)) * BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP).toInt()
        
        val result = StringBuilder()
        
        // Convert rupees part
        if (rupees > 0) {
            result.append(convertNumberToWords(rupees))
            result.append(" Rupee")
            if (rupees != 1L) result.append("s")
        }
        
        // Convert paise part
        if (paise > 0) {
            if (rupees > 0) result.append(" and ")
            result.append(convertNumberToWords(paise.toLong()))
            result.append(" Paise")
        }
        
        result.append(" Only")
        return result.toString()
    }
    
    /**
     * Convert number to words using Indian numbering system
     */
    private fun convertNumberToWords(num: Long): String {
        if (num == 0L) return ""
        
        val result = StringBuilder()
        var number = num
        var level = 0
        
        // Handle crores (10,000,000)
        if (number >= 10000000) {
            val crores = number / 10000000
            result.append(convertHundreds(crores.toInt())).append(" Crore")
            if (crores > 1) result.append("s")
            number %= 10000000
            if (number > 0) result.append(" ")
        }
        
        // Handle lakhs (100,000)
        if (number >= 100000) {
            val lakhs = number / 100000
            result.append(convertHundreds(lakhs.toInt())).append(" Lakh")
            if (lakhs > 1) result.append("s")
            number %= 100000
            if (number > 0) result.append(" ")
        }
        
        // Handle thousands (1,000)
        if (number >= 1000) {
            val thousands = number / 1000
            result.append(convertHundreds(thousands.toInt())).append(" Thousand")
            number %= 1000
            if (number > 0) result.append(" ")
        }
        
        // Handle hundreds and below
        if (number > 0) {
            result.append(convertHundreds(number.toInt()))
        }
        
        return result.toString().trim()
    }
    
    /**
     * Convert numbers up to 999 to words
     */
    private fun convertHundreds(num: Int): String {
        if (num == 0) return ""
        
        val result = StringBuilder()
        
        // Handle hundreds
        if (num >= 100) {
            result.append(ones[num / 100]).append(" Hundred")
            val remainder = num % 100
            if (remainder > 0) result.append(" ")
        }
        
        // Handle tens and ones
        val remainder = num % 100
        if (remainder >= 20) {
            result.append(tens[remainder / 10])
            if (remainder % 10 > 0) {
                result.append(" ").append(ones[remainder % 10])
            }
        } else if (remainder > 0) {
            result.append(ones[remainder])
        }
        
        return result.toString()
    }
    
    /**
     * Convert amount to words with currency symbol
     */
    fun convertWithSymbol(amount: BigDecimal, currencySymbol: String = "₹"): String {
        return "$currencySymbol ${amount.setScale(2)} (${convert(amount)})"
    }
}