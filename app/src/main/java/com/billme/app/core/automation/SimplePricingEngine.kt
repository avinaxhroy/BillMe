package com.billme.app.core.automation

import com.billme.app.data.local.entity.Product
import com.billme.app.data.repository.ProductRepository
import com.billme.app.data.repository.SalesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple pricing engine for suggesting product prices
 * Based on cost, margin, and basic market factors
 */
@Singleton
class SimplePricingEngine @Inject constructor(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository
) {
    
    companion object {
        private const val DEFAULT_PROFIT_MARGIN = 0.15 // 15%
        private const val MIN_PROFIT_MARGIN = 0.05 // 5%
        private const val MAX_PROFIT_MARGIN = 0.50 // 50%
        private const val COMPETITIVE_ADJUSTMENT = 0.03 // 3% adjustment for competition
    }
    
    /**
     * Generate simple price suggestion for a product
     */
    suspend fun generatePriceSuggestion(productId: Long): PricingSuggestionResult = withContext(Dispatchers.IO) {
        try {
            val product = productRepository.getProductByIdSync(productId)
                ?: return@withContext PricingSuggestionResult.Error("Product not found")
            
            val suggestion = calculatePriceSuggestion(product)
            PricingSuggestionResult.Success(suggestion)
            
        } catch (e: Exception) {
            PricingSuggestionResult.Error(e.message ?: "Failed to generate price suggestion")
        }
    }
    
    /**
     * Calculate price suggestion based on cost and market factors
     */
    private suspend fun calculatePriceSuggestion(product: Product): PriceSuggestion {
        val costPrice = product.costPrice
        
        // Calculate basic prices with different margin strategies
        val conservativeMargin = DEFAULT_PROFIT_MARGIN - 0.05
        val standardMargin = DEFAULT_PROFIT_MARGIN
        val aggressiveMargin = DEFAULT_PROFIT_MARGIN + 0.10
        
        val conservativePrice = calculatePrice(costPrice, conservativeMargin)
        val standardPrice = calculatePrice(costPrice, standardMargin)
        val aggressivePrice = calculatePrice(costPrice, aggressiveMargin)
        
        // Determine recommended price based on product category and sales history
        val recommendedPrice = determineRecommendedPrice(
            product,
            conservativePrice,
            standardPrice,
            aggressivePrice
        )
        
        return PriceSuggestion(
            productId = product.productId,
            productName = product.productName,
            currentPrice = product.sellingPrice,
            costPrice = costPrice,
            suggestedPrice = recommendedPrice,
            minPrice = conservativePrice,
            maxPrice = aggressivePrice,
            confidence = calculateConfidence(product),
            reasoning = generateReasoning(product, recommendedPrice)
        )
    }
    
    /**
     * Calculate price with given margin
     */
    private fun calculatePrice(cost: BigDecimal, margin: Double): BigDecimal {
        val multiplier = BigDecimal.valueOf(1.0 + margin)
        return (cost * multiplier).setScale(2, RoundingMode.HALF_UP)
    }
    
    /**
     * Determine best recommended price
     */
    private suspend fun determineRecommendedPrice(
        product: Product,
        conservative: BigDecimal,
        standard: BigDecimal,
        aggressive: BigDecimal
    ): BigDecimal {
        // Check sales history for this product
        val salesHistory = try {
            salesRepository.getProductSalesHistory(product.productId, 30)
        } catch (e: Exception) {
            emptyList()
        }
        
        return when {
            // If product sells well, suggest aggressive pricing
            salesHistory.size > 10 -> aggressive
            
            // If moderate sales, use standard pricing
            salesHistory.size in 3..10 -> standard
            
            // If low/no sales, use conservative pricing
            else -> conservative
        }
    }
    
    /**
     * Calculate confidence score
     */
    private fun calculateConfidence(product: Product): Double {
        var confidence = 0.5 // Base confidence
        
        // Increase confidence if we have sales history
        if (product.currentStock > 0) {
            confidence += 0.2
        }
        
        // Increase confidence if product has consistent pricing
        if (product.sellingPrice > BigDecimal.ZERO) {
            confidence += 0.2
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    /**
     * Generate reasoning text
     */
    private fun generateReasoning(product: Product, suggestedPrice: BigDecimal): String {
        val margin = if (product.costPrice > BigDecimal.ZERO) {
            ((suggestedPrice - product.costPrice) / product.costPrice * BigDecimal.valueOf(100.0))
                .setScale(1, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        return buildString {
            append("Based on cost of ${formatCurrency(product.costPrice)}, ")
            append("suggested price of ${formatCurrency(suggestedPrice)} ")
            append("provides ${margin}% margin. ")
            
            if (product.currentStock > 10) {
                append("Good stock availability. ")
            } else if (product.currentStock < 5) {
                append("Low stock - consider premium pricing. ")
            }
        }
    }
    
    /**
     * Format currency for display
     */
    private fun formatCurrency(amount: BigDecimal): String {
        return "₹${amount.setScale(2, RoundingMode.HALF_UP)}"
    }
    
    /**
     * Apply suggested price to product
     */
    suspend fun applyPriceSuggestion(
        productId: Long,
        newPrice: BigDecimal
    ): ApplyPriceResult = withContext(Dispatchers.IO) {
        try {
            val product = productRepository.getProductByIdSync(productId)
                ?: return@withContext ApplyPriceResult.Error("Product not found")
            
            // Validate price is above cost
            if (newPrice <= product.costPrice) {
                return@withContext ApplyPriceResult.Error("Price must be higher than cost price")
            }
            
            // Update product with new price
            val updatedProduct = product.copy(sellingPrice = newPrice)
            productRepository.updateProduct(updatedProduct)
            
            ApplyPriceResult.Success(updatedProduct)
            
        } catch (e: Exception) {
            ApplyPriceResult.Error(e.message ?: "Failed to apply price")
        }
    }
}

/**
 * Price suggestion result
 */
sealed class PricingSuggestionResult {
    data class Success(val suggestion: PriceSuggestion) : PricingSuggestionResult()
    data class Error(val message: String) : PricingSuggestionResult()
}

/**
 * Price suggestion data
 */
data class PriceSuggestion(
    val productId: Long,
    val productName: String,
    val currentPrice: BigDecimal,
    val costPrice: BigDecimal,
    val suggestedPrice: BigDecimal,
    val minPrice: BigDecimal,
    val maxPrice: BigDecimal,
    val confidence: Double,
    val reasoning: String
)

/**
 * Apply price result
 */
sealed class ApplyPriceResult {
    data class Success(val product: Product) : ApplyPriceResult()
    data class Error(val message: String) : ApplyPriceResult()
}
