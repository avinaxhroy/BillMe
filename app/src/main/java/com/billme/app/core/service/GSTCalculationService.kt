package com.billme.app.core.service

import com.billme.app.core.util.GSTValidator
import com.billme.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GST Calculation Line Item
 */
data class GSTCalculationItem(
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalAmount: BigDecimal,
    val gstRate: Double,
    val gstCategory: GSTRateCategory,
    val hsnCode: String? = null,
    val cgstAmount: BigDecimal = BigDecimal.ZERO,
    val sgstAmount: BigDecimal = BigDecimal.ZERO,
    val igstAmount: BigDecimal = BigDecimal.ZERO,
    val cessAmount: BigDecimal = BigDecimal.ZERO
)

/**
 * GST Breakdown by Rate
 */
data class GSTRateBreakdown(
    val gstRate: Double,
    val taxableAmount: BigDecimal,
    val cgstAmount: BigDecimal = BigDecimal.ZERO,
    val sgstAmount: BigDecimal = BigDecimal.ZERO,
    val igstAmount: BigDecimal = BigDecimal.ZERO,
    val cessAmount: BigDecimal = BigDecimal.ZERO,
    val totalGstAmount: BigDecimal
)

/**
 * HSN Summary for GST compliance
 */
data class HSNSummary(
    val hsnCode: String,
    val description: String? = null,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalAmount: BigDecimal,
    val gstRate: Double,
    val taxableValue: BigDecimal,
    val cgstAmount: BigDecimal = BigDecimal.ZERO,
    val sgstAmount: BigDecimal = BigDecimal.ZERO,
    val igstAmount: BigDecimal = BigDecimal.ZERO,
    val cessAmount: BigDecimal = BigDecimal.ZERO
)

/**
 * Complete GST Calculation Result
 */
data class GSTCalculationResult(
    val gstMode: GSTMode,
    val isInterstate: Boolean,
    val shopGSTIN: String?,
    val customerGSTIN: String?,
    val items: List<GSTCalculationItem>,
    val rateBreakdown: List<GSTRateBreakdown>,
    val hsnSummary: List<HSNSummary>,
    val subtotal: BigDecimal,
    val totalCGST: BigDecimal,
    val totalSGST: BigDecimal,
    val totalIGST: BigDecimal,
    val totalCess: BigDecimal,
    val totalGST: BigDecimal,
    val roundOffAmount: BigDecimal,
    val grandTotal: BigDecimal,
    val showGSTToCustomer: Boolean,
    val gstDisplayMode: GSTDisplayMode
)

/**
 * How GST should be displayed to customer
 */
enum class GSTDisplayMode {
    FULL_BREAKDOWN,     // Show complete CGST/SGST/IGST breakdown
    TOTAL_GST_ONLY,     // Show only total GST amount
    GSTIN_ONLY,         // Show only GSTIN reference
    HIDDEN              // Hide GST completely from customer
}

/**
 * GST Calculation Service
 * Handles all GST calculations based on different modes
 */
@Singleton
class GSTCalculationService @Inject constructor() {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    private val _currentCalculation = MutableStateFlow<GSTCalculationResult?>(null)
    val currentCalculation: StateFlow<GSTCalculationResult?> = _currentCalculation.asStateFlow()
    
    /**
     * Calculate GST for a transaction
     */
    fun calculateGST(
        items: List<TransactionLineItem>,
        products: List<Product>,
        gstConfig: GSTConfiguration,
        customerGSTIN: String? = null,
        overrideGSTMode: GSTMode? = null
    ): GSTCalculationResult {
        
        val effectiveGSTMode = overrideGSTMode ?: gstConfig.defaultGSTMode
        val isInterstate = determineIsInterstate(gstConfig.shopGSTIN, customerGSTIN)
        
        // Convert transaction items to calculation items
        val calculationItems = items.map { lineItem ->
            val product = products.find { it.productId == lineItem.productId }
            createCalculationItem(lineItem, product, gstConfig, isInterstate)
        }
        
        // Calculate totals
        val subtotal = calculationItems.sumOf { it.totalAmount }
        val totalCGST = if (!isInterstate) calculationItems.sumOf { it.cgstAmount } else BigDecimal.ZERO
        val totalSGST = if (!isInterstate) calculationItems.sumOf { it.sgstAmount } else BigDecimal.ZERO
        val totalIGST = if (isInterstate) calculationItems.sumOf { it.igstAmount } else BigDecimal.ZERO
        val totalCess = calculationItems.sumOf { it.cessAmount }
        val totalGST = totalCGST + totalSGST + totalIGST
        
        // Calculate round-off
        val grossTotal = subtotal + totalGST + totalCess
        val roundOffAmount = if (gstConfig.roundOffGST) {
            val rounded = grossTotal.setScale(0, RoundingMode.HALF_UP)
            rounded - grossTotal
        } else BigDecimal.ZERO
        
        val grandTotal = grossTotal + roundOffAmount
        
        // Create breakdowns
        val rateBreakdown = createRateBreakdown(calculationItems, isInterstate)
        val hsnSummary = createHSNSummary(calculationItems)
        
        // Determine display mode
        val (showToCustomer, displayMode) = determineDisplayMode(effectiveGSTMode, gstConfig)
        
        val result = GSTCalculationResult(
            gstMode = effectiveGSTMode,
            isInterstate = isInterstate,
            shopGSTIN = gstConfig.shopGSTIN,
            customerGSTIN = customerGSTIN,
            items = calculationItems,
            rateBreakdown = rateBreakdown,
            hsnSummary = hsnSummary,
            subtotal = subtotal,
            totalCGST = totalCGST,
            totalSGST = totalSGST,
            totalIGST = totalIGST,
            totalCess = totalCess,
            totalGST = totalGST,
            roundOffAmount = roundOffAmount,
            grandTotal = grandTotal,
            showGSTToCustomer = showToCustomer,
            gstDisplayMode = displayMode
        )
        
        _currentCalculation.value = result
        return result
    }
    
    /**
     * Create calculation item from transaction line item
     */
    private fun createCalculationItem(
        lineItem: TransactionLineItem,
        product: Product?,
        gstConfig: GSTConfiguration,
        isInterstate: Boolean
    ): GSTCalculationItem {
        
        val gstRate = product?.let { getProductGSTRate(it, gstConfig) } ?: gstConfig.defaultGSTRate
        val gstCategory = getGSTRateCategory(gstRate)
        val totalAmount = lineItem.unitSellingPrice * BigDecimal(lineItem.quantity)
        
        return if (gstConfig.allowsGSTCalculation && gstConfig.defaultGSTMode != GSTMode.NO_GST) {
            val taxableAmount = if (gstConfig.includeGSTInPrice) {
                // GST is included in price - calculate taxable amount
                val gstMultiplier = BigDecimal.ONE + (BigDecimal(gstRate) / BigDecimal(100))
                totalAmount / gstMultiplier
            } else {
                // GST is additional to price
                totalAmount
            }
            
            val gstAmount = taxableAmount * BigDecimal(gstRate) / BigDecimal(100)
            
            val (cgst, sgst, igst) = if (isInterstate) {
                Triple(BigDecimal.ZERO, BigDecimal.ZERO, gstAmount)
            } else {
                val halfGST = gstAmount / BigDecimal(2)
                Triple(halfGST, halfGST, BigDecimal.ZERO)
            }
            
            GSTCalculationItem(
                productName = lineItem.productName,
                quantity = lineItem.quantity,
                unitPrice = lineItem.unitSellingPrice,
                totalAmount = taxableAmount,
                gstRate = gstRate,
                gstCategory = gstCategory,
                hsnCode = product?.let { getProductHSNCode(it) },
                cgstAmount = cgst,
                sgstAmount = sgst,
                igstAmount = igst,
                cessAmount = BigDecimal.ZERO // CESS calculation can be added here
            )
        } else {
            // No GST calculation
            GSTCalculationItem(
                productName = lineItem.productName,
                quantity = lineItem.quantity,
                unitPrice = lineItem.unitSellingPrice,
                totalAmount = totalAmount,
                gstRate = 0.0,
                gstCategory = GSTRateCategory.EXEMPT,
                hsnCode = null
            )
        }
    }
    
    /**
     * Get GST rate for a product
     */
    private fun getProductGSTRate(product: Product, gstConfig: GSTConfiguration): Double {
        // This would typically look up the GST rate from gst_rates table
        // For now, return default rate based on category
        return when (product.category.lowercase()) {
            "mobile", "smartphone", "phone" -> 18.0
            "accessory", "charger", "cable" -> 18.0
            "case", "cover" -> 12.0
            else -> gstConfig.defaultGSTRate
        }
    }
    
    /**
     * Get GST rate category
     */
    private fun getGSTRateCategory(rate: Double): GSTRateCategory {
        return when (rate) {
            0.0 -> GSTRateCategory.EXEMPT
            5.0 -> GSTRateCategory.GST_5
            12.0 -> GSTRateCategory.GST_12
            18.0 -> GSTRateCategory.GST_18
            28.0 -> GSTRateCategory.GST_28
            else -> GSTRateCategory.CUSTOM
        }
    }
    
    /**
     * Get HSN code for a product
     */
    private fun getProductHSNCode(product: Product): String? {
        // This would typically be stored in product or looked up from HSN database
        return when (product.category.lowercase()) {
            "mobile", "smartphone", "phone" -> "8517"
            "accessory", "charger" -> "8544"
            "case", "cover" -> "3926"
            else -> null
        }
    }
    
    /**
     * Determine if transaction is interstate
     */
    private fun determineIsInterstate(shopGSTIN: String?, customerGSTIN: String?): Boolean {
        return if (shopGSTIN != null && customerGSTIN != null) {
            !GSTValidator.isSameState(shopGSTIN, customerGSTIN)
        } else {
            false // Default to intrastate if GSTIN info is incomplete
        }
    }
    
    /**
     * Create rate-wise breakdown
     */
    private fun createRateBreakdown(
        items: List<GSTCalculationItem>,
        isInterstate: Boolean
    ): List<GSTRateBreakdown> {
        return items.groupBy { it.gstRate }.map { (rate, rateItems) ->
            val taxableAmount = rateItems.sumOf { it.totalAmount }
            val cgstAmount = if (!isInterstate) rateItems.sumOf { it.cgstAmount } else BigDecimal.ZERO
            val sgstAmount = if (!isInterstate) rateItems.sumOf { it.sgstAmount } else BigDecimal.ZERO
            val igstAmount = if (isInterstate) rateItems.sumOf { it.igstAmount } else BigDecimal.ZERO
            val cessAmount = rateItems.sumOf { it.cessAmount }
            val totalGstAmount = cgstAmount + sgstAmount + igstAmount
            
            GSTRateBreakdown(
                gstRate = rate,
                taxableAmount = taxableAmount,
                cgstAmount = cgstAmount,
                sgstAmount = sgstAmount,
                igstAmount = igstAmount,
                cessAmount = cessAmount,
                totalGstAmount = totalGstAmount
            )
        }.sortedBy { it.gstRate }
    }
    
    /**
     * Create HSN-wise summary
     */
    private fun createHSNSummary(items: List<GSTCalculationItem>): List<HSNSummary> {
        return items.filter { it.hsnCode != null }
            .groupBy { it.hsnCode!! }
            .map { (hsnCode, hsnItems) ->
                val quantity = hsnItems.sumOf { it.quantity }
                val totalAmount = hsnItems.sumOf { it.totalAmount }
                val unitPrice = if (quantity > 0) totalAmount / BigDecimal(quantity) else BigDecimal.ZERO
                val gstRate = hsnItems.first().gstRate // Assuming same rate for same HSN
                val cgstAmount = hsnItems.sumOf { it.cgstAmount }
                val sgstAmount = hsnItems.sumOf { it.sgstAmount }
                val igstAmount = hsnItems.sumOf { it.igstAmount }
                val cessAmount = hsnItems.sumOf { it.cessAmount }
                
                HSNSummary(
                    hsnCode = hsnCode,
                    description = getHSNDescription(hsnCode),
                    quantity = quantity,
                    unitPrice = unitPrice,
                    totalAmount = totalAmount,
                    gstRate = gstRate,
                    taxableValue = totalAmount,
                    cgstAmount = cgstAmount,
                    sgstAmount = sgstAmount,
                    igstAmount = igstAmount,
                    cessAmount = cessAmount
                )
            }
    }
    
    /**
     * Get HSN description
     */
    private fun getHSNDescription(hsnCode: String): String? {
        return when (hsnCode) {
            "8517" -> "Mobile Phones"
            "8544" -> "Mobile Accessories"
            "3926" -> "Mobile Cases & Covers"
            else -> null
        }
    }
    
    /**
     * Determine how GST should be displayed
     */
    private fun determineDisplayMode(
        gstMode: GSTMode,
        gstConfig: GSTConfiguration
    ): Pair<Boolean, GSTDisplayMode> {
        return when (gstMode) {
            GSTMode.FULL_GST -> Pair(true, GSTDisplayMode.FULL_BREAKDOWN)
            GSTMode.PARTIAL_GST -> Pair(false, GSTDisplayMode.HIDDEN)
            GSTMode.GST_REFERENCE -> Pair(true, GSTDisplayMode.GSTIN_ONLY)
            GSTMode.NO_GST -> Pair(false, GSTDisplayMode.HIDDEN)
        }
    }
    
    /**
     * Create invoice GST details entity
     */
    fun createInvoiceGSTDetails(
        transactionId: Long,
        calculation: GSTCalculationResult
    ): InvoiceGSTDetails {
        return InvoiceGSTDetails(
            transactionId = transactionId,
            gstMode = calculation.gstMode,
            shopGSTIN = calculation.shopGSTIN,
            customerGSTIN = calculation.customerGSTIN,
            isInterstate = calculation.isInterstate,
            taxableAmount = calculation.subtotal,
            cgstAmount = calculation.totalCGST,
            sgstAmount = calculation.totalSGST,
            igstAmount = calculation.totalIGST,
            cessAmount = calculation.totalCess,
            totalGSTAmount = calculation.totalGST,
            roundOffAmount = calculation.roundOffAmount,
            gstRateBreakdown = json.encodeToString(calculation.rateBreakdown),
            hsnSummary = json.encodeToString(calculation.hsnSummary),
            createdAt = kotlinx.datetime.Clock.System.now()
        )
    }
    
    /**
     * Validate GST calculation
     */
    fun validateCalculation(calculation: GSTCalculationResult): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate GSTIN if GST is applicable
        if (calculation.gstMode != GSTMode.NO_GST) {
            if (calculation.shopGSTIN.isNullOrBlank()) {
                errors.add("Shop GSTIN is required for GST transactions")
            } else if (!GSTValidator.isValidGSTIN(calculation.shopGSTIN)) {
                errors.add("Invalid Shop GSTIN format")
            }
            
            calculation.customerGSTIN?.let { customerGSTIN ->
                if (!GSTValidator.isValidGSTIN(customerGSTIN)) {
                    errors.add("Invalid Customer GSTIN format")
                }
            }
        }
        
        // Validate amounts
        if (calculation.subtotal < BigDecimal.ZERO) {
            errors.add("Subtotal cannot be negative")
        }
        
        if (calculation.totalGST < BigDecimal.ZERO) {
            errors.add("Total GST cannot be negative")
        }
        
        // Validate rate breakdown totals match
        val breakdownTotal = calculation.rateBreakdown.sumOf { it.totalGstAmount }
        if (breakdownTotal != calculation.totalGST) {
            errors.add("GST rate breakdown total does not match total GST")
        }
        
        return errors
    }
    
    /**
     * Clear current calculation
     */
    fun clearCalculation() {
        _currentCalculation.value = null
    }
}