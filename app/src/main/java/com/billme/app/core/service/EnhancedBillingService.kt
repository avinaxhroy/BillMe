package com.billme.app.core.service

import com.billme.app.core.util.GSTValidator
import com.billme.app.core.util.NumberToWordsConverter
import com.billme.app.core.util.formatLocale
import com.billme.app.data.local.entity.*
import com.billme.app.data.repository.GSTRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced billing service with comprehensive GST support
 */
@Singleton
class EnhancedBillingService @Inject constructor(
    private val gstRepository: GSTRepository,
    private val gstCalculationService: GSTCalculationService,
    private val numberToWordsConverter: NumberToWordsConverter
) {
    
    /**
     * Build complete invoice with GST calculations
     */
    suspend fun buildInvoice(request: InvoiceRequest): InvoiceWithDetails {
        val gstConfig = gstRepository.getActiveConfiguration().value
            ?: throw IllegalStateException("GST configuration not found")
        
        // Determine effective GST mode
        val effectiveGSTMode = request.overrideGSTMode ?: gstConfig.defaultGSTMode
        
        // Validate customer GSTIN if provided
        val customerGSTINValidation = request.customerGSTIN?.let { gstin ->
            GSTValidator.validateGSTINWithDetails(gstin)
        }
        
        // Determine interstate status
        val isInterstate = determineInterstateStatus(
            shopGSTIN = gstConfig.shopGSTIN,
            customerGSTIN = request.customerGSTIN,
            autoDetect = gstConfig.autoDetectInterstate
        )
        
        // Calculate line items with GST
        val calculatedLineItems = calculateLineItemsWithGST(
            lineItems = request.lineItems,
            gstMode = effectiveGSTMode,
            gstConfig = gstConfig,
            isInterstate = isInterstate
        )
        
        // Calculate invoice totals
        val invoiceTotals = calculateInvoiceTotals(
            lineItems = calculatedLineItems,
            globalDiscount = request.globalDiscount,
            globalDiscountType = request.globalDiscountType,
            roundOffGST = gstConfig.roundOffGST
        )
        
        // Generate invoice number
        val invoiceNumber = generateInvoiceNumber(request.invoiceType)
        
        val now = Clock.System.now()
        
        // Build main invoice
        val invoice = Invoice(
            invoiceNumber = invoiceNumber,
            transactionId = request.transactionId,
            customerId = request.customerId,
            customerName = request.customerName,
            customerPhone = request.customerPhone,
            customerGSTIN = request.customerGSTIN,
            customerAddress = request.customerAddress,
            customerStateCode = customerGSTINValidation?.stateCode,
            invoiceDate = request.invoiceDate ?: now,
            dueDate = request.dueDate,
            
            // Basic amounts
            subtotalAmount = invoiceTotals.subtotal,
            discountAmount = invoiceTotals.discountAmount,
            discountPercentage = request.globalDiscountPercentage,
            taxableAmount = invoiceTotals.taxableAmount,
            
            // GST configuration
            gstConfigId = gstConfig.configId,
            gstMode = effectiveGSTMode,
            isInterstate = isInterstate,
            shopGSTIN = gstConfig.shopGSTIN,
            shopStateCode = gstConfig.shopStateCode,
            
            // GST breakdown
            cgstAmount = invoiceTotals.cgstAmount,
            sgstAmount = invoiceTotals.sgstAmount,
            igstAmount = invoiceTotals.igstAmount,
            cessAmount = invoiceTotals.cessAmount,
            totalGSTAmount = invoiceTotals.totalGSTAmount,
            gstRateApplied = calculateWeightedAverageGSTRate(calculatedLineItems),
            
            // Final amounts
            roundOffAmount = invoiceTotals.roundOffAmount,
            grandTotal = invoiceTotals.grandTotal,
            amountInWords = numberToWordsConverter.convert(invoiceTotals.grandTotal),
            
            // Payment details
            paymentMethod = request.paymentMethod,
            paymentStatus = request.paymentStatus,
            amountPaid = request.amountPaid,
            amountDue = invoiceTotals.grandTotal - request.amountPaid,
            
            // Display settings from configuration
            showGSTIN = gstConfig.showGSTINOnInvoice,
            showGSTSummary = when (effectiveGSTMode) {
                GSTMode.FULL_GST -> gstConfig.showGSTSummary
                else -> false
            },
            includeGSTInPrice = gstConfig.includeGSTInPrice,
            
            // Additional metadata
            invoiceType = request.invoiceType,
            placeOfSupply = request.placeOfSupply ?: gstConfig.shopStateName,
            termsAndConditions = request.termsAndConditions,
            notes = request.notes,
            createdAt = now,
            updatedAt = now,
            createdBy = request.createdBy
        )
        
        // Create invoice GST details for compliance
        val invoiceGSTDetails = if (effectiveGSTMode != GSTMode.NO_GST) {
            createInvoiceGSTDetails(
                invoice = invoice,
                lineItems = calculatedLineItems,
                gstConfig = gstConfig
            )
        } else null
        
        return InvoiceWithDetails(
            invoice = invoice,
            lineItems = calculatedLineItems,
            customer = request.customer,
            gstConfiguration = gstConfig,
            invoiceGSTDetails = invoiceGSTDetails
        )
    }
    
    /**
     * Calculate line items with appropriate GST based on mode
     */
    private suspend fun calculateLineItemsWithGST(
        lineItems: List<InvoiceLineItemRequest>,
        gstMode: GSTMode,
        gstConfig: GSTConfiguration,
        isInterstate: Boolean
    ): List<InvoiceLineItem> {
        return lineItems.map { request ->
            // Get GST rate for this product
            val gstRate = if (gstMode != GSTMode.NO_GST) {
                gstRepository.getGSTRateForProduct(
                    productId = request.productId,
                    hsnCode = request.hsnCode
                ) ?: gstRepository.getDefaultGSTRate(gstConfig.defaultGSTCategory)
            } else null
            
            // Calculate line item totals
            val lineSubtotal = request.unitPrice * request.quantity
            val lineDiscountAmount = if (request.discountPercentage > 0) {
                lineSubtotal * BigDecimal.valueOf(request.discountPercentage / 100.0)
            } else request.discountAmount
            
            val taxableAmount = lineSubtotal - lineDiscountAmount
            
            // Calculate GST amounts based on mode
            val gstAmounts = when (gstMode) {
                GSTMode.FULL_GST, GSTMode.PARTIAL_GST -> {
                    calculateGSTForLineItem(taxableAmount, gstRate, isInterstate)
                }
                GSTMode.GST_REFERENCE, GSTMode.NO_GST -> {
                    GSTAmounts() // Zero amounts
                }
            }
            
            val lineTotal = taxableAmount + gstAmounts.totalGST
            
            InvoiceLineItem(
                invoiceId = 0, // Will be set when invoice is saved
                productId = request.productId,
                productName = request.productName,
                productDescription = request.productDescription,
                hsnCode = request.hsnCode,
                unitOfMeasure = request.unitOfMeasure,
                quantity = request.quantity,
                unitPrice = request.unitPrice,
                discountAmount = lineDiscountAmount,
                discountPercentage = request.discountPercentage,
                taxableAmount = taxableAmount,
                
                // GST details
                gstRateId = gstRate?.rateId,
                cgstRate = gstRate?.cgstRate ?: 0.0,
                sgstRate = gstRate?.sgstRate ?: 0.0,
                igstRate = gstRate?.igstRate ?: 0.0,
                cessRate = gstRate?.cessRate ?: 0.0,
                cgstAmount = gstAmounts.cgst,
                sgstAmount = gstAmounts.sgst,
                igstAmount = gstAmounts.igst,
                cessAmount = gstAmounts.cess,
                totalGSTAmount = gstAmounts.totalGST,
                
                lineTotal = lineTotal,
                imeiSerial = request.imeiSerial,
                batchNumber = request.batchNumber,
                warrantyPeriod = request.warrantyPeriod
            )
        }
    }
    
    /**
     * Calculate GST amounts for a line item
     */
    private fun calculateGSTForLineItem(
        taxableAmount: BigDecimal,
        gstRate: GSTRate?,
        isInterstate: Boolean
    ): GSTAmounts {
        if (gstRate == null) {
            return GSTAmounts()
        }
        
        return if (isInterstate) {
            val igstAmount = (taxableAmount * BigDecimal.valueOf((gstRate.igstRate ?: 0.0) / 100.0))
                .setScale(2, RoundingMode.HALF_UP)
            val cessAmount = (taxableAmount * BigDecimal.valueOf((gstRate.cessRate ?: 0.0) / 100.0))
                .setScale(2, RoundingMode.HALF_UP)
            
            GSTAmounts(
                igst = igstAmount,
                cess = cessAmount,
                totalGST = igstAmount + cessAmount
            )
        } else {
            val cgstAmount = (taxableAmount * BigDecimal.valueOf((gstRate.cgstRate ?: 0.0) / 100.0))
                .setScale(2, RoundingMode.HALF_UP)
            val sgstAmount = (taxableAmount * BigDecimal.valueOf((gstRate.sgstRate ?: 0.0) / 100.0))
                .setScale(2, RoundingMode.HALF_UP)
            val cessAmount = (taxableAmount * BigDecimal.valueOf((gstRate.cessRate ?: 0.0) / 100.0))
                .setScale(2, RoundingMode.HALF_UP)
            
            GSTAmounts(
                cgst = cgstAmount,
                sgst = sgstAmount,
                cess = cessAmount,
                totalGST = cgstAmount + sgstAmount + cessAmount
            )
        }
    }
    
    /**
     * Calculate invoice totals from line items
     */
    private fun calculateInvoiceTotals(
        lineItems: List<InvoiceLineItem>,
        globalDiscount: BigDecimal,
        globalDiscountType: DiscountType,
        roundOffGST: Boolean
    ): InvoiceTotals {
        val subtotal = lineItems.sumOf { it.quantity * it.unitPrice }
        val lineDiscountAmount = lineItems.sumOf { it.discountAmount }
        
        // Apply global discount
        val globalDiscountAmount = when (globalDiscountType) {
            DiscountType.PERCENT -> {
                val afterLineDiscount = subtotal - lineDiscountAmount
                (afterLineDiscount * globalDiscount / BigDecimal.valueOf(100.0))
                    .setScale(2, RoundingMode.HALF_UP)
            }
            DiscountType.AMOUNT -> globalDiscount
        }
        
        val totalDiscountAmount = lineDiscountAmount + globalDiscountAmount
        val taxableAmount = subtotal - totalDiscountAmount
        
        // Sum up GST amounts
        val cgstAmount = lineItems.sumOf { it.cgstAmount }
        val sgstAmount = lineItems.sumOf { it.sgstAmount }
        val igstAmount = lineItems.sumOf { it.igstAmount }
        val cessAmount = lineItems.sumOf { it.cessAmount }
        val totalGSTAmount = cgstAmount + sgstAmount + igstAmount + cessAmount
        
        // Calculate total before rounding
        val totalBeforeRounding = taxableAmount + totalGSTAmount
        
        // Apply rounding if enabled
        val (finalTotal, roundOffAmount) = if (roundOffGST) {
            val rounded = totalBeforeRounding.setScale(0, RoundingMode.HALF_UP)
            val roundOff = rounded - totalBeforeRounding
            Pair(rounded, roundOff)
        } else {
            Pair(totalBeforeRounding, BigDecimal.ZERO)
        }
        
        return InvoiceTotals(
            subtotal = subtotal,
            discountAmount = totalDiscountAmount,
            taxableAmount = taxableAmount,
            cgstAmount = cgstAmount,
            sgstAmount = sgstAmount,
            igstAmount = igstAmount,
            cessAmount = cessAmount,
            totalGSTAmount = totalGSTAmount,
            roundOffAmount = roundOffAmount,
            grandTotal = finalTotal
        )
    }
    
    /**
     * Determine if transaction is interstate
     */
    private fun determineInterstateStatus(
        shopGSTIN: String?,
        customerGSTIN: String?,
        autoDetect: Boolean
    ): Boolean {
        if (!autoDetect || shopGSTIN.isNullOrBlank()) {
            return false
        }
        
        if (customerGSTIN.isNullOrBlank()) {
            return false // Default to intrastate for B2C
        }
        
        val shopStateCode = GSTValidator.getStateCode(shopGSTIN)
        val customerStateCode = GSTValidator.getStateCode(customerGSTIN)
        
        return shopStateCode != customerStateCode
    }
    
    /**
     * Calculate weighted average GST rate for the invoice
     */
    private fun calculateWeightedAverageGSTRate(lineItems: List<InvoiceLineItem>): Double {
        val totalTaxableAmount = lineItems.sumOf { it.taxableAmount }
        if (totalTaxableAmount == BigDecimal.ZERO) return 0.0
        
        val weightedSum = lineItems.sumOf { lineItem ->
            val rate = maxOf(
                lineItem.cgstRate + lineItem.sgstRate,
                lineItem.igstRate
            ) + lineItem.cessRate
            lineItem.taxableAmount * BigDecimal.valueOf(rate)
        }
        
        return (weightedSum / totalTaxableAmount).toDouble()
    }
    
    /**
     * Create invoice GST details for compliance and reporting
     */
    private suspend fun createInvoiceGSTDetails(
        invoice: Invoice,
        lineItems: List<InvoiceLineItem>,
        gstConfig: GSTConfiguration
    ): InvoiceGSTDetails? {
        if (invoice.gstMode == GSTMode.NO_GST) return null
        
        val rateBreakdowns = createGSTRateBreakdowns(lineItems)
        
        return InvoiceGSTDetails(
            transactionId = invoice.transactionId,
            gstMode = invoice.gstMode,
            isInterstate = invoice.isInterstate,
            shopGSTIN = invoice.shopGSTIN,
            customerGSTIN = invoice.customerGSTIN,
            taxableAmount = invoice.taxableAmount,
            cgstAmount = invoice.cgstAmount,
            sgstAmount = invoice.sgstAmount,
            igstAmount = invoice.igstAmount,
            cessAmount = invoice.cessAmount,
            totalGSTAmount = invoice.totalGSTAmount,
            roundOffAmount = invoice.roundOffAmount,
            gstRateBreakdown = rateBreakdowns.joinToString(",") { "${it.gstRate}:${it.taxableAmount}:${it.totalGstAmount}" },
            createdAt = invoice.createdAt
        )
    }
    
    /**
     * Create GST rate breakdowns for detailed reporting
     */
    private fun createGSTRateBreakdowns(lineItems: List<InvoiceLineItem>): List<GSTRateBreakdown> {
        return lineItems.groupBy { 
            Triple(it.cgstRate + it.sgstRate + it.igstRate, it.cgstRate > 0, it.hsnCode) 
        }.map { (key, items) ->
            val totalRate = key.first
            val isIntrastate = key.second
            val hsnCode = key.third
            
            GSTRateBreakdown(
                gstRate = totalRate,
                taxableAmount = items.sumOf { it.taxableAmount },
                cgstAmount = if (isIntrastate) items.sumOf { it.cgstAmount } else BigDecimal.ZERO,
                sgstAmount = if (isIntrastate) items.sumOf { it.sgstAmount } else BigDecimal.ZERO,
                igstAmount = if (!isIntrastate) items.sumOf { it.igstAmount } else BigDecimal.ZERO,
                cessAmount = items.sumOf { it.cessAmount },
                totalGstAmount = items.sumOf { it.totalGSTAmount }
            )
        }
    }
    
    /**
     * Generate unique invoice number
     */
    private fun generateInvoiceNumber(invoiceType: InvoiceType): String {
        val prefix = when (invoiceType) {
            InvoiceType.SALE -> "INV"
            InvoiceType.RETURN -> "RTN"
            InvoiceType.CREDIT_NOTE -> "CN"
            InvoiceType.DEBIT_NOTE -> "DN"
            InvoiceType.PROFORMA -> "PRO"
            InvoiceType.QUOTATION -> "QUO"
        }
        
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val uniqueId = timestamp % 1000000 // Last 6 digits
        
        return "$prefix${uniqueId.toInt().formatLocale("%06d")}"
    }
    
    /**
     * Generate GST summary for invoice display
     */
    fun generateGSTSummary(invoiceWithDetails: InvoiceWithDetails): InvoiceGSTSummary {
        val invoice = invoiceWithDetails.invoice
        val lineItems = invoiceWithDetails.lineItems
        
        val gstBreakdown = lineItems.groupBy { 
            it.cgstRate + it.sgstRate + it.igstRate + it.cessRate 
        }.map { (rate, items) ->
            GSTRateBreakdownItem(
                gstRate = rate,
                taxableAmount = items.sumOf { it.taxableAmount },
                cgstAmount = items.sumOf { it.cgstAmount },
                sgstAmount = items.sumOf { it.sgstAmount },
                igstAmount = items.sumOf { it.igstAmount },
                cessAmount = items.sumOf { it.cessAmount },
                totalAmount = items.sumOf { it.lineTotal },
                hsnCodes = items.mapNotNull { it.hsnCode }.distinct()
            )
        }.sortedBy { it.gstRate }
        
        return InvoiceGSTSummary(
            subtotal = invoice.subtotalAmount,
            discountAmount = invoice.discountAmount,
            taxableAmount = invoice.taxableAmount,
            cgstAmount = invoice.cgstAmount,
            sgstAmount = invoice.sgstAmount,
            igstAmount = invoice.igstAmount,
            cessAmount = invoice.cessAmount,
            totalGSTAmount = invoice.totalGSTAmount,
            roundOffAmount = invoice.roundOffAmount,
            grandTotal = invoice.grandTotal,
            isInterstate = invoice.isInterstate,
            gstBreakdown = gstBreakdown
        )
    }
}

/**
 * Request object for building an invoice
 */
data class InvoiceRequest(
    val transactionId: Long,
    val customerId: Long? = null,
    val customer: Customer? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerGSTIN: String? = null,
    val customerAddress: String? = null,
    val lineItems: List<InvoiceLineItemRequest>,
    val globalDiscount: BigDecimal = BigDecimal.ZERO,
    val globalDiscountType: DiscountType = DiscountType.AMOUNT,
    val globalDiscountPercentage: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val amountPaid: BigDecimal = BigDecimal.ZERO,
    val invoiceDate: Instant? = null,
    val dueDate: Instant? = null,
    val invoiceType: InvoiceType = InvoiceType.SALE,
    val placeOfSupply: String? = null,
    val termsAndConditions: String? = null,
    val notes: String? = null,
    val createdBy: String = "Admin",
    val overrideGSTMode: GSTMode? = null // Override default GST mode for this invoice
)

/**
 * Request object for invoice line items
 */
data class InvoiceLineItemRequest(
    val productId: Long,
    val productName: String,
    val productDescription: String? = null,
    val hsnCode: String? = null,
    val unitOfMeasure: String = "PCS",
    val quantity: BigDecimal = BigDecimal.ONE,
    val unitPrice: BigDecimal,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val discountPercentage: Double = 0.0,
    val imeiSerial: String? = null,
    val batchNumber: String? = null,
    val warrantyPeriod: String? = null
)

/**
 * Internal data classes for calculations
 */
private data class GSTAmounts(
    val cgst: BigDecimal = BigDecimal.ZERO,
    val sgst: BigDecimal = BigDecimal.ZERO,
    val igst: BigDecimal = BigDecimal.ZERO,
    val cess: BigDecimal = BigDecimal.ZERO,
    val totalGST: BigDecimal = cgst + sgst + igst + cess
)

private data class InvoiceTotals(
    val subtotal: BigDecimal,
    val discountAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    val cgstAmount: BigDecimal,
    val sgstAmount: BigDecimal,
    val igstAmount: BigDecimal,
    val cessAmount: BigDecimal,
    val totalGSTAmount: BigDecimal,
    val roundOffAmount: BigDecimal,
    val grandTotal: BigDecimal
)