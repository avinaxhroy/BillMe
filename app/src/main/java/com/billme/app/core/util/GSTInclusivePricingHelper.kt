package com.billme.app.core.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Helper object for GST-inclusive and GST-exclusive pricing calculations
 * Supports conversion between inclusive and exclusive pricing models
 */
object GSTInclusivePricingHelper {
    
    /**
     * Convert GST-exclusive price to GST-inclusive price
     * Formula: Inclusive Price = Exclusive Price * (1 + GST Rate / 100)
     * 
     * @param exclusivePrice Price without GST
     * @param gstRate GST rate percentage (e.g., 18.0 for 18%)
     * @return Price with GST included
     */
    fun convertToInclusive(
        exclusivePrice: BigDecimal,
        gstRate: Double
    ): BigDecimal {
        val multiplier = BigDecimal.ONE + BigDecimal(gstRate).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        return exclusivePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Convert GST-inclusive price to GST-exclusive price
     * Formula: Exclusive Price = Inclusive Price / (1 + GST Rate / 100)
     * 
     * @param inclusivePrice Price with GST included
     * @param gstRate GST rate percentage (e.g., 18.0 for 18%)
     * @return Price without GST
     */
    fun convertToExclusive(
        inclusivePrice: BigDecimal,
        gstRate: Double
    ): BigDecimal {
        val divisor = BigDecimal.ONE + BigDecimal(gstRate).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        return inclusivePrice.divide(divisor, 2, RoundingMode.HALF_UP)
    }
    
    /**
     * Extract GST amount from GST-inclusive price
     * Formula: GST Amount = Inclusive Price - (Inclusive Price / (1 + GST Rate / 100))
     * 
     * @param inclusivePrice Price with GST included
     * @param gstRate GST rate percentage
     * @return GST amount extracted from the price
     */
    fun extractGSTFromInclusive(
        inclusivePrice: BigDecimal,
        gstRate: Double
    ): BigDecimal {
        val exclusivePrice = convertToExclusive(inclusivePrice, gstRate)
        return inclusivePrice.subtract(exclusivePrice).setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate taxable value from inclusive price
     * This is the base price without GST
     */
    fun getTaxableValue(
        inclusivePrice: BigDecimal,
        gstRate: Double
    ): BigDecimal {
        return convertToExclusive(inclusivePrice, gstRate)
    }
    
    /**
     * Calculate CGST and SGST amounts for intrastate transactions (inclusive pricing)
     */
    fun calculateCGSTSGSTFromInclusive(
        inclusivePrice: BigDecimal,
        gstRate: Double
    ): Pair<BigDecimal, BigDecimal> {
        val totalGST = extractGSTFromInclusive(inclusivePrice, gstRate)
        val halfGST = totalGST.divide(BigDecimal(2), 2, RoundingMode.HALF_UP)
        return Pair(halfGST, halfGST)
    }
    
    /**
     * Calculate IGST amount for interstate transactions (inclusive pricing)
     */
    fun calculateIGSTFromInclusive(
        inclusivePrice: BigDecimal,
        gstRate: Double
    ): BigDecimal {
        return extractGSTFromInclusive(inclusivePrice, gstRate)
    }
    
    /**
     * Calculate profit margin with GST-inclusive pricing
     * Returns profit after GST is extracted
     */
    fun calculateProfitWithInclusive(
        costPrice: BigDecimal,
        inclusiveSellingPrice: BigDecimal,
        gstRate: Double
    ): BigDecimal {
        val exclusivePrice = convertToExclusive(inclusiveSellingPrice, gstRate)
        return exclusivePrice.subtract(costPrice).setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Calculate profit percentage with GST-inclusive pricing
     */
    fun calculateProfitPercentageWithInclusive(
        costPrice: BigDecimal,
        inclusiveSellingPrice: BigDecimal,
        gstRate: Double
    ): Double {
        if (costPrice <= BigDecimal.ZERO) return 0.0
        
        val profit = calculateProfitWithInclusive(costPrice, inclusiveSellingPrice, gstRate)
        return profit.divide(costPrice, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .toDouble()
    }
    
    /**
     * Suggest inclusive selling price based on cost and desired profit margin
     * 
     * @param costPrice Base cost price
     * @param desiredProfitPercentage Desired profit percentage (e.g., 20.0 for 20%)
     * @param gstRate GST rate percentage
     * @return Suggested GST-inclusive selling price
     */
    fun suggestInclusivePrice(
        costPrice: BigDecimal,
        desiredProfitPercentage: Double,
        gstRate: Double
    ): BigDecimal {
        // Calculate desired exclusive price with profit
        val profitMultiplier = BigDecimal.ONE + BigDecimal(desiredProfitPercentage).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        val exclusivePriceWithProfit = costPrice.multiply(profitMultiplier)
        
        // Convert to inclusive
        return convertToInclusive(exclusivePriceWithProfit, gstRate)
    }
    
    /**
     * Round price to nearest rupee or specified decimal places
     */
    fun roundPrice(
        price: BigDecimal,
        decimalPlaces: Int = 0,
        roundingMode: RoundingMode = RoundingMode.HALF_UP
    ): BigDecimal {
        return price.setScale(decimalPlaces, roundingMode)
    }
    
    /**
     * Calculate invoice breakdown for GST-inclusive pricing
     */
    fun calculateInvoiceBreakdown(
        inclusivePrice: BigDecimal,
        quantity: Int,
        gstRate: Double,
        isInterstate: Boolean = false
    ): InvoiceBreakdown {
        val totalInclusive = inclusivePrice.multiply(BigDecimal(quantity))
        val taxableValue = getTaxableValue(totalInclusive, gstRate)
        val totalGST = extractGSTFromInclusive(totalInclusive, gstRate)
        
        return if (isInterstate) {
            InvoiceBreakdown(
                taxableValue = taxableValue,
                cgst = BigDecimal.ZERO,
                sgst = BigDecimal.ZERO,
                igst = totalGST,
                totalGST = totalGST,
                totalAmount = totalInclusive
            )
        } else {
            val (cgst, sgst) = calculateCGSTSGSTFromInclusive(totalInclusive, gstRate)
            InvoiceBreakdown(
                taxableValue = taxableValue,
                cgst = cgst,
                sgst = sgst,
                igst = BigDecimal.ZERO,
                totalGST = totalGST,
                totalAmount = totalInclusive
            )
        }
    }
}

/**
 * Data class representing invoice breakdown with GST details
 */
data class InvoiceBreakdown(
    val taxableValue: BigDecimal,
    val cgst: BigDecimal,
    val sgst: BigDecimal,
    val igst: BigDecimal,
    val totalGST: BigDecimal,
    val totalAmount: BigDecimal
)
