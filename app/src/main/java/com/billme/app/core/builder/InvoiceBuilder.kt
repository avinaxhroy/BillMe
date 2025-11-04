package com.billme.app.core.builder

import com.billme.app.core.service.EnhancedBillingService
import com.billme.app.core.service.InvoiceLineItemRequest
import com.billme.app.core.service.InvoiceRequest
import com.billme.app.data.local.entity.*
import kotlinx.datetime.Instant
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builder pattern implementation for creating invoices with proper GST calculations
 */
@Singleton
class InvoiceBuilder @Inject constructor(
    private val enhancedBillingService: EnhancedBillingService
) {
    private var transactionId: Long = 0
    private var customerId: Long? = null
    private var customer: Customer? = null
    private var customerName: String? = null
    private var customerPhone: String? = null
    private var customerGSTIN: String? = null
    private var customerAddress: String? = null
    private var lineItems: MutableList<InvoiceLineItemRequest> = mutableListOf()
    private var globalDiscount: BigDecimal = BigDecimal.ZERO
    private var globalDiscountType: DiscountType = DiscountType.AMOUNT
    private var globalDiscountPercentage: Double = 0.0
    private var paymentMethod: PaymentMethod = PaymentMethod.CASH
    private var paymentStatus: PaymentStatus = PaymentStatus.PAID
    private var amountPaid: BigDecimal = BigDecimal.ZERO
    private var invoiceDate: Instant? = null
    private var dueDate: Instant? = null
    private var invoiceType: InvoiceType = InvoiceType.SALE
    private var placeOfSupply: String? = null
    private var termsAndConditions: String? = null
    private var notes: String? = null
    private var createdBy: String = "Admin"
    private var overrideGSTMode: GSTMode? = null
    
    /**
     * Set transaction ID (mandatory)
     */
    fun withTransactionId(transactionId: Long): InvoiceBuilder {
        this.transactionId = transactionId
        return this
    }
    
    /**
     * Set customer information
     */
    fun withCustomer(
        customerId: Long? = null,
        customer: Customer? = null,
        customerName: String? = null,
        customerPhone: String? = null,
        customerGSTIN: String? = null,
        customerAddress: String? = null
    ): InvoiceBuilder {
        this.customerId = customerId
        this.customer = customer
        this.customerName = customerName
        this.customerPhone = customerPhone
        this.customerGSTIN = customerGSTIN
        this.customerAddress = customerAddress
        return this
    }
    
    /**
     * Add a line item to the invoice
     */
    fun addLineItem(
        productId: Long,
        productName: String,
        unitPrice: BigDecimal,
        quantity: BigDecimal = BigDecimal.ONE,
        productDescription: String? = null,
        hsnCode: String? = null,
        unitOfMeasure: String = "PCS",
        discountAmount: BigDecimal = BigDecimal.ZERO,
        discountPercentage: Double = 0.0,
        imeiSerial: String? = null,
        batchNumber: String? = null,
        warrantyPeriod: String? = null
    ): InvoiceBuilder {
        lineItems.add(
            InvoiceLineItemRequest(
                productId = productId,
                productName = productName,
                productDescription = productDescription,
                hsnCode = hsnCode,
                unitOfMeasure = unitOfMeasure,
                quantity = quantity,
                unitPrice = unitPrice,
                discountAmount = discountAmount,
                discountPercentage = discountPercentage,
                imeiSerial = imeiSerial,
                batchNumber = batchNumber,
                warrantyPeriod = warrantyPeriod
            )
        )
        return this
    }
    
    /**
     * Add multiple line items at once
     */
    fun addLineItems(items: List<InvoiceLineItemRequest>): InvoiceBuilder {
        lineItems.addAll(items)
        return this
    }
    
    /**
     * Set global discount
     */
    fun withGlobalDiscount(
        discount: BigDecimal,
        discountType: DiscountType = DiscountType.AMOUNT,
        discountPercentage: Double = 0.0
    ): InvoiceBuilder {
        this.globalDiscount = discount
        this.globalDiscountType = discountType
        this.globalDiscountPercentage = discountPercentage
        return this
    }
    
    /**
     * Set payment information
     */
    fun withPayment(
        method: PaymentMethod = PaymentMethod.CASH,
        status: PaymentStatus = PaymentStatus.PAID,
        amountPaid: BigDecimal = BigDecimal.ZERO
    ): InvoiceBuilder {
        this.paymentMethod = method
        this.paymentStatus = status
        this.amountPaid = amountPaid
        return this
    }
    
    /**
     * Set invoice dates
     */
    fun withDates(
        invoiceDate: Instant? = null,
        dueDate: Instant? = null
    ): InvoiceBuilder {
        this.invoiceDate = invoiceDate
        this.dueDate = dueDate
        return this
    }
    
    /**
     * Set invoice type
     */
    fun withType(type: InvoiceType): InvoiceBuilder {
        this.invoiceType = type
        return this
    }
    
    /**
     * Set additional information
     */
    fun withAdditionalInfo(
        placeOfSupply: String? = null,
        termsAndConditions: String? = null,
        notes: String? = null,
        createdBy: String = "Admin"
    ): InvoiceBuilder {
        this.placeOfSupply = placeOfSupply
        this.termsAndConditions = termsAndConditions
        this.notes = notes
        this.createdBy = createdBy
        return this
    }
    
    /**
     * Override GST mode for this specific invoice
     */
    fun withGSTModeOverride(gstMode: GSTMode): InvoiceBuilder {
        this.overrideGSTMode = gstMode
        return this
    }
    
    /**
     * Build the invoice with all GST calculations
     */
    suspend fun build(): InvoiceWithDetails {
        require(transactionId > 0) { "Transaction ID is required" }
        require(lineItems.isNotEmpty()) { "At least one line item is required" }
        
        val request = InvoiceRequest(
            transactionId = transactionId,
            customerId = customerId,
            customer = customer,
            customerName = customerName,
            customerPhone = customerPhone,
            customerGSTIN = customerGSTIN,
            customerAddress = customerAddress,
            lineItems = lineItems,
            globalDiscount = globalDiscount,
            globalDiscountType = globalDiscountType,
            globalDiscountPercentage = globalDiscountPercentage,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            amountPaid = amountPaid,
            invoiceDate = invoiceDate,
            dueDate = dueDate,
            invoiceType = invoiceType,
            placeOfSupply = placeOfSupply,
            termsAndConditions = termsAndConditions,
            notes = notes,
            createdBy = createdBy,
            overrideGSTMode = overrideGSTMode
        )
        
        return enhancedBillingService.buildInvoice(request)
    }
    
    /**
     * Reset the builder for reuse
     */
    fun reset(): InvoiceBuilder {
        transactionId = 0
        customerId = null
        customer = null
        customerName = null
        customerPhone = null
        customerGSTIN = null
        customerAddress = null
        lineItems.clear()
        globalDiscount = BigDecimal.ZERO
        globalDiscountType = DiscountType.AMOUNT
        globalDiscountPercentage = 0.0
        paymentMethod = PaymentMethod.CASH
        paymentStatus = PaymentStatus.PAID
        amountPaid = BigDecimal.ZERO
        invoiceDate = null
        dueDate = null
        invoiceType = InvoiceType.SALE
        placeOfSupply = null
        termsAndConditions = null
        notes = null
        createdBy = "Admin"
        overrideGSTMode = null
        return this
    }
    
    /**
     * Create a copy of current builder state
     */
    fun copy(): InvoiceBuilder {
        return InvoiceBuilder(enhancedBillingService).apply {
            transactionId = this@InvoiceBuilder.transactionId
            customerId = this@InvoiceBuilder.customerId
            customer = this@InvoiceBuilder.customer
            customerName = this@InvoiceBuilder.customerName
            customerPhone = this@InvoiceBuilder.customerPhone
            customerGSTIN = this@InvoiceBuilder.customerGSTIN
            customerAddress = this@InvoiceBuilder.customerAddress
            lineItems = this@InvoiceBuilder.lineItems.toMutableList()
            globalDiscount = this@InvoiceBuilder.globalDiscount
            globalDiscountType = this@InvoiceBuilder.globalDiscountType
            globalDiscountPercentage = this@InvoiceBuilder.globalDiscountPercentage
            paymentMethod = this@InvoiceBuilder.paymentMethod
            paymentStatus = this@InvoiceBuilder.paymentStatus
            amountPaid = this@InvoiceBuilder.amountPaid
            invoiceDate = this@InvoiceBuilder.invoiceDate
            dueDate = this@InvoiceBuilder.dueDate
            invoiceType = this@InvoiceBuilder.invoiceType
            placeOfSupply = this@InvoiceBuilder.placeOfSupply
            termsAndConditions = this@InvoiceBuilder.termsAndConditions
            notes = this@InvoiceBuilder.notes
            createdBy = this@InvoiceBuilder.createdBy
            overrideGSTMode = this@InvoiceBuilder.overrideGSTMode
        }
    }
}

/**
 * Extension functions for convenient invoice building
 */

/**
 * Create invoice builder from transaction
 */
fun Transaction.toInvoiceBuilder(billingService: EnhancedBillingService): InvoiceBuilder {
    return InvoiceBuilder(billingService)
        .withTransactionId(transactionId)
        .withCustomer(
            customerName = customerName,
            customerPhone = customerPhone
        )
        .withPayment(
            method = paymentMethod,
            status = paymentStatus
        )
        .withAdditionalInfo(
            notes = notes
        )
}

/**
 * Create invoice builder for return/credit note
 */
fun InvoiceWithDetails.toReturnInvoiceBuilder(billingService: EnhancedBillingService): InvoiceBuilder {
    return InvoiceBuilder(billingService)
        .withTransactionId(invoice.transactionId)
        .withCustomer(
            customerId = invoice.customerId,
            customer = customer,
            customerName = invoice.customerName,
            customerPhone = invoice.customerPhone,
            customerGSTIN = invoice.customerGSTIN,
            customerAddress = invoice.customerAddress
        )
        .withType(InvoiceType.RETURN)
        .withGSTModeOverride(invoice.gstMode)
        .apply {
            // Add line items with negative quantities for return
            lineItems.forEach { lineItem ->
                addLineItem(
                    productId = lineItem.productId,
                    productName = lineItem.productName,
                    unitPrice = lineItem.unitPrice,
                    quantity = -lineItem.quantity, // Negative for return
                    productDescription = lineItem.productDescription,
                    hsnCode = lineItem.hsnCode,
                    unitOfMeasure = lineItem.unitOfMeasure,
                    imeiSerial = lineItem.imeiSerial,
                    batchNumber = lineItem.batchNumber,
                    warrantyPeriod = lineItem.warrantyPeriod
                )
            }
        }
}