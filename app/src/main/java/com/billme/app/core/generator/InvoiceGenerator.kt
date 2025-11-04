package com.billme.app.core.generator

import com.billme.app.core.builder.InvoiceBuilder
import com.billme.app.core.service.EnhancedBillingService
import com.billme.app.core.service.InvoiceLineItemRequest
import com.billme.app.core.util.NumberToWordsConverter
import com.billme.app.data.local.entity.*
import com.billme.app.data.repository.InvoiceRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.*
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Invoice generation logic with comprehensive GST support and visibility controls
 */
@Singleton
class InvoiceGenerator @Inject constructor(
    private val invoiceBuilder: InvoiceBuilder,
    private val enhancedBillingService: EnhancedBillingService,
    private val invoiceRepository: InvoiceRepository,
    private val numberToWordsConverter: NumberToWordsConverter
) {
    
    /**
     * Generate invoice from transaction data
     */
    suspend fun generateInvoiceFromTransaction(
        transaction: Transaction,
        lineItems: List<TransactionLineItem>,
        customer: Customer? = null,
        overrideGSTMode: GSTMode? = null,
        additionalSettings: InvoiceGenerationSettings = InvoiceGenerationSettings()
    ): Result<InvoiceWithDetails> {
        return try {
            // Convert transaction line items to invoice line items
            val invoiceLineItems = lineItems.map { transactionItem ->
                InvoiceLineItemRequest(
                    productId = transactionItem.productId,
                    productName = transactionItem.productName,
                    unitPrice = transactionItem.unitSellingPrice,
                    quantity = BigDecimal.valueOf(transactionItem.quantity.toDouble()),
                    imeiSerial = transactionItem.imeiSold,
                    // Additional fields would need to be mapped from product data
                    hsnCode = additionalSettings.defaultHSNCode,
                    unitOfMeasure = "PCS"
                )
            }
            
            // Build invoice using the builder pattern
            val invoiceWithDetails = invoiceBuilder
                .withTransactionId(transaction.transactionId)
                .withCustomer(
                    customer = customer,
                    customerName = transaction.customerName,
                    customerPhone = transaction.customerPhone
                )
                .addLineItems(invoiceLineItems)
                .withGlobalDiscount(
                    discount = transaction.discountAmount,
                    discountType = transaction.discountType
                )
                .withPayment(
                    method = transaction.paymentMethod,
                    status = transaction.paymentStatus
                )
                .withAdditionalInfo(
                    notes = transaction.notes,
                    createdBy = transaction.salesPerson,
                    termsAndConditions = additionalSettings.defaultTermsAndConditions
                )
                .apply {
                    overrideGSTMode?.let { withGSTModeOverride(it) }
                }
                .build()
            
            Result.success(invoiceWithDetails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate custom invoice with full control
     */
    suspend fun generateCustomInvoice(
        request: CustomInvoiceRequest
    ): Result<InvoiceWithDetails> {
        return try {
            val invoiceWithDetails = invoiceBuilder
                .withTransactionId(request.transactionId)
                .withCustomer(
                    customerId = request.customerId,
                    customer = request.customer,
                    customerName = request.customerName,
                    customerPhone = request.customerPhone,
                    customerGSTIN = request.customerGSTIN,
                    customerAddress = request.customerAddress
                )
                .addLineItems(request.lineItems)
                .withGlobalDiscount(
                    discount = request.globalDiscount,
                    discountType = request.globalDiscountType,
                    discountPercentage = request.globalDiscountPercentage
                )
                .withPayment(
                    method = request.paymentMethod,
                    status = request.paymentStatus,
                    amountPaid = request.amountPaid
                )
                .withDates(
                    invoiceDate = request.invoiceDate,
                    dueDate = request.dueDate
                )
                .withType(request.invoiceType)
                .withAdditionalInfo(
                    placeOfSupply = request.placeOfSupply,
                    termsAndConditions = request.termsAndConditions,
                    notes = request.notes,
                    createdBy = request.createdBy
                )
                .apply {
                    request.overrideGSTMode?.let { withGSTModeOverride(it) }
                }
                .build()
            
            Result.success(invoiceWithDetails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate proforma invoice (quotation)
     */
    suspend fun generateProformaInvoice(
        customerId: Long? = null,
        customerName: String? = null,
        customerPhone: String? = null,
        customerGSTIN: String? = null,
        lineItems: List<InvoiceLineItemRequest>,
        validityDays: Int = 30,
        notes: String? = null
    ): Result<InvoiceWithDetails> {
        return try {
            val now = Clock.System.now()
            // Add validity days - use a simpler calculation to avoid duration parsing issues
            val validityMillis = (validityDays.toLong() * 24 * 60 * 60 * 1000)
            val validUntil = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + validityMillis)
            
            val invoiceWithDetails = invoiceBuilder
                .withTransactionId(0L) // No transaction for proforma
                .withCustomer(
                    customerId = customerId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerGSTIN = customerGSTIN
                )
                .addLineItems(lineItems)
                .withType(InvoiceType.PROFORMA)
                .withDates(
                    invoiceDate = now,
                    dueDate = validUntil
                )
                .withPayment(
                    status = PaymentStatus.PENDING,
                    amountPaid = BigDecimal.ZERO
                )
                .withAdditionalInfo(
                    notes = notes,
                    termsAndConditions = "This is a proforma invoice valid for $validityDays days."
                )
                .build()
            
            Result.success(invoiceWithDetails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate credit note from original invoice
     */
    suspend fun generateCreditNote(
        originalInvoiceId: Long,
        reason: String,
        lineItemsToCredit: List<CreditLineItem>? = null, // null means full credit
        adjustmentAmount: BigDecimal = BigDecimal.ZERO
    ): Result<InvoiceWithDetails> {
        return try {
            val originalInvoice = invoiceRepository.getInvoiceWithDetails(originalInvoiceId)
                ?: throw IllegalArgumentException("Original invoice not found")
            
            // Determine line items to credit
            val creditLineItems = lineItemsToCredit?.map { creditItem ->
                val originalItem = originalInvoice.lineItems.find { it.lineItemId == creditItem.originalLineItemId }
                    ?: throw IllegalArgumentException("Original line item not found")
                
                InvoiceLineItemRequest(
                    productId = originalItem.productId,
                    productName = originalItem.productName,
                    productDescription = originalItem.productDescription,
                    hsnCode = originalItem.hsnCode,
                    unitOfMeasure = originalItem.unitOfMeasure,
                    quantity = -creditItem.quantity, // Negative for credit
                    unitPrice = originalItem.unitPrice,
                    discountAmount = BigDecimal.ZERO,
                    imeiSerial = originalItem.imeiSerial,
                    batchNumber = originalItem.batchNumber,
                    warrantyPeriod = originalItem.warrantyPeriod
                )
            } ?: originalInvoice.lineItems.map { originalItem ->
                // Full credit - negate all line items
                InvoiceLineItemRequest(
                    productId = originalItem.productId,
                    productName = originalItem.productName,
                    productDescription = originalItem.productDescription,
                    hsnCode = originalItem.hsnCode,
                    unitOfMeasure = originalItem.unitOfMeasure,
                    quantity = -originalItem.quantity, // Negative for credit
                    unitPrice = originalItem.unitPrice,
                    discountAmount = BigDecimal.ZERO,
                    imeiSerial = originalItem.imeiSerial,
                    batchNumber = originalItem.batchNumber,
                    warrantyPeriod = originalItem.warrantyPeriod
                )
            }
            
            val creditNote = invoiceBuilder
                .withTransactionId(originalInvoice.invoice.transactionId)
                .withCustomer(
                    customerId = originalInvoice.invoice.customerId,
                    customer = originalInvoice.customer,
                    customerName = originalInvoice.invoice.customerName,
                    customerPhone = originalInvoice.invoice.customerPhone,
                    customerGSTIN = originalInvoice.invoice.customerGSTIN,
                    customerAddress = originalInvoice.invoice.customerAddress
                )
                .addLineItems(creditLineItems)
                .withType(InvoiceType.CREDIT_NOTE)
                .withGSTModeOverride(originalInvoice.invoice.gstMode)
                .withAdditionalInfo(
                    notes = "Credit Note for Invoice #${originalInvoice.invoice.invoiceNumber}. Reason: $reason",
                    termsAndConditions = originalInvoice.invoice.termsAndConditions
                )
                .withGlobalDiscount(
                    discount = adjustmentAmount,
                    discountType = DiscountType.AMOUNT
                )
                .build()
            
            Result.success(creditNote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate consolidated invoice from multiple transactions
     */
    suspend fun generateConsolidatedInvoice(
        customerId: Long,
        transactionIds: List<Long>,
        consolidationPeriod: String? = null
    ): Result<InvoiceWithDetails> {
        return try {
            // This would require fetching multiple transactions and combining them
            // Implementation depends on specific business requirements
            TODO("Implement consolidated invoice generation")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Apply invoice formatting and visibility rules
     */
    fun applyInvoiceVisibilityRules(
        invoiceWithDetails: InvoiceWithDetails,
        displaySettings: InvoiceDisplaySettings
    ): InvoiceWithDetails {
        val invoice = invoiceWithDetails.invoice
        
        // Apply display settings based on GST mode and configuration
        val updatedInvoice = invoice.copy(
            showGSTIN = when {
                displaySettings.forceHideGSTIN -> false
                displaySettings.forceShowGSTIN -> true
                else -> invoice.showGSTIN
            },
            showGSTSummary = when (invoice.gstMode) {
                GSTMode.FULL_GST -> if (displaySettings.hideGSTDetails) false else invoice.showGSTSummary
                GSTMode.PARTIAL_GST -> false // Never show GST summary for partial mode
                GSTMode.GST_REFERENCE -> false // Never show GST summary for reference mode
                GSTMode.NO_GST -> false
            },
            // Apply amount formatting
            amountInWords = if (displaySettings.showAmountInWords) {
                invoice.amountInWords
            } else null,
            // Apply custom terms if provided
            termsAndConditions = displaySettings.customTermsAndConditions 
                ?: invoice.termsAndConditions
        )
        
        // Filter line items if needed
        val filteredLineItems = if (displaySettings.hideZeroAmountItems) {
            invoiceWithDetails.lineItems.filter { it.lineTotal > BigDecimal.ZERO }
        } else {
            invoiceWithDetails.lineItems
        }
        
        return invoiceWithDetails.copy(
            invoice = updatedInvoice,
            lineItems = filteredLineItems
        )
    }
    
    /**
     * Validate invoice before generation
     */
    suspend fun validateInvoiceRequest(request: CustomInvoiceRequest): InvoiceValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Basic validation
        if (request.lineItems.isEmpty()) {
            errors.add("At least one line item is required")
        }
        
        if (request.transactionId <= 0) {
            warnings.add("No transaction ID provided")
        }
        
        // Validate line items
        request.lineItems.forEachIndexed { index, item ->
            if (item.quantity <= BigDecimal.ZERO) {
                errors.add("Line item ${index + 1}: Quantity must be greater than zero")
            }
            if (item.unitPrice < BigDecimal.ZERO) {
                errors.add("Line item ${index + 1}: Unit price cannot be negative")
            }
            if (item.productName.isBlank()) {
                errors.add("Line item ${index + 1}: Product name is required")
            }
        }
        
        // Validate customer GSTIN if provided
        if (!request.customerGSTIN.isNullOrBlank()) {
            // Add GSTIN validation logic
            val gstinValidation = com.billme.app.core.util.GSTValidator.validateGSTINWithDetails(request.customerGSTIN!!)
            if (!gstinValidation.isValid) {
                warnings.add("Customer GSTIN format is invalid: ${gstinValidation.errorMessage}")
            }
        }
        
        // Payment validation
        if (request.paymentStatus == PaymentStatus.PAID && request.amountPaid <= BigDecimal.ZERO) {
            warnings.add("Payment status is PAID but amount paid is zero")
        }
        
        return InvoiceValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}

/**
 * Configuration classes for invoice generation
 */

data class InvoiceGenerationSettings(
    val defaultHSNCode: String? = null,
    val defaultTermsAndConditions: String? = null,
    val autoGenerateInvoiceNumber: Boolean = true,
    val includeGSTBreakdown: Boolean = true,
    val roundAmounts: Boolean = true
)

data class CustomInvoiceRequest(
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
    val overrideGSTMode: GSTMode? = null
)

data class CreditLineItem(
    val originalLineItemId: Long,
    val quantity: BigDecimal,
    val reason: String? = null
)

data class InvoiceDisplaySettings(
    val forceShowGSTIN: Boolean = false,
    val forceHideGSTIN: Boolean = false,
    val hideGSTDetails: Boolean = false,
    val showAmountInWords: Boolean = true,
    val hideZeroAmountItems: Boolean = false,
    val customTermsAndConditions: String? = null,
    val compactMode: Boolean = false,
    val showLineItemDetails: Boolean = true
)

data class InvoiceValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)
