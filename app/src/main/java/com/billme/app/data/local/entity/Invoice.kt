package com.billme.app.data.local.entity

import androidx.room.*
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Enhanced Invoice entity with comprehensive GST support
 */
@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["transaction_id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["customer_id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = GSTConfiguration::class,
            parentColumns = ["config_id"],
            childColumns = ["gst_config_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["invoice_number"], unique = true),
        Index(value = ["transaction_id"]),
        Index(value = ["customer_id"]),
        Index(value = ["gst_config_id"]),
        Index(value = ["invoice_date"]),
        Index(value = ["gst_mode"]),
        Index(value = ["payment_status"]),
        Index(value = ["is_interstate"])
    ]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "invoice_id")
    val invoiceId: Long = 0,
    
    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,
    
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,
    
    @ColumnInfo(name = "customer_id")
    val customerId: Long? = null,
    
    @ColumnInfo(name = "customer_name")
    val customerName: String? = null,
    
    @ColumnInfo(name = "customer_phone")
    val customerPhone: String? = null,
    
    @ColumnInfo(name = "customer_gstin")
    val customerGSTIN: String? = null,
    
    @ColumnInfo(name = "customer_address")
    val customerAddress: String? = null,
    
    @ColumnInfo(name = "customer_state_code")
    val customerStateCode: String? = null,
    
    @ColumnInfo(name = "invoice_date")
    val invoiceDate: Instant,
    
    @ColumnInfo(name = "due_date")
    val dueDate: Instant? = null,
    
    // Basic amounts
    @ColumnInfo(name = "subtotal_amount")
    val subtotalAmount: BigDecimal,
    
    @ColumnInfo(name = "discount_amount")
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "discount_percentage")
    val discountPercentage: Double = 0.0,
    
    @ColumnInfo(name = "taxable_amount")
    val taxableAmount: BigDecimal,
    
    // GST Configuration
    @ColumnInfo(name = "gst_config_id")
    val gstConfigId: Long? = null,
    
    @ColumnInfo(name = "gst_mode")
    val gstMode: GSTMode,
    
    @ColumnInfo(name = "is_interstate")
    val isInterstate: Boolean = false,
    
    @ColumnInfo(name = "shop_gstin")
    val shopGSTIN: String? = null,
    
    @ColumnInfo(name = "shop_state_code")
    val shopStateCode: String? = null,
    
    // GST Breakdown
    @ColumnInfo(name = "cgst_amount")
    val cgstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "sgst_amount")
    val sgstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "igst_amount")
    val igstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "cess_amount")
    val cessAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "total_gst_amount")
    val totalGSTAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "gst_rate_applied")
    val gstRateApplied: Double = 0.0,
    
    // Final amounts
    @ColumnInfo(name = "round_off_amount")
    val roundOffAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "grand_total")
    val grandTotal: BigDecimal,
    
    @ColumnInfo(name = "amount_in_words")
    val amountInWords: String? = null,
    
    // Payment details
    @ColumnInfo(name = "payment_method")
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    
    @ColumnInfo(name = "payment_status")
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    
    @ColumnInfo(name = "amount_paid")
    val amountPaid: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "amount_due")
    val amountDue: BigDecimal = BigDecimal.ZERO,
    
    // Display settings
    @ColumnInfo(name = "show_gstin")
    val showGSTIN: Boolean = true,
    
    @ColumnInfo(name = "show_gst_summary")
    val showGSTSummary: Boolean = true,
    
    @ColumnInfo(name = "include_gst_in_price")
    val includeGSTInPrice: Boolean = false,
    
    // Additional metadata
    @ColumnInfo(name = "invoice_type")
    val invoiceType: InvoiceType = InvoiceType.SALE,
    
    @ColumnInfo(name = "place_of_supply")
    val placeOfSupply: String? = null,
    
    @ColumnInfo(name = "terms_and_conditions")
    val termsAndConditions: String? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "is_cancelled")
    val isCancelled: Boolean = false,
    
    @ColumnInfo(name = "cancelled_at")
    val cancelledAt: Instant? = null,
    
    @ColumnInfo(name = "cancellation_reason")
    val cancellationReason: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    
    @ColumnInfo(name = "created_by")
    val createdBy: String = "Admin"
)

// Extension properties for Invoice - compatibility aliases
val Invoice.subtotal: BigDecimal
    get() = this.subtotalAmount

val Invoice.gstAmount: BigDecimal
    get() = this.totalGSTAmount

val Invoice.finalTotal: BigDecimal
    get() = this.grandTotal

/**
 * Invoice line item with GST breakdown
 */
@Entity(
    tableName = "invoice_line_items",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["invoice_id"],
            childColumns = ["invoice_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = GSTRate::class,
            parentColumns = ["rate_id"],
            childColumns = ["gst_rate_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["invoice_id"]),
        Index(value = ["product_id"]),
        Index(value = ["hsn_code"]),
        Index(value = ["gst_rate_id"])
    ]
)
data class InvoiceLineItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "line_item_id")
    val lineItemId: Long = 0,
    
    @ColumnInfo(name = "invoice_id")
    val invoiceId: Long,
    
    @ColumnInfo(name = "product_id")
    val productId: Long,
    
    @ColumnInfo(name = "product_name")
    val productName: String,
    
    @ColumnInfo(name = "product_description")
    val productDescription: String? = null,
    
    @ColumnInfo(name = "hsn_code")
    val hsnCode: String? = null,
    
    @ColumnInfo(name = "unit_of_measure")
    val unitOfMeasure: String = "PCS",
    
    @ColumnInfo(name = "quantity")
    val quantity: BigDecimal = BigDecimal.ONE,
    
    @ColumnInfo(name = "unit_price")
    val unitPrice: BigDecimal,
    
    @ColumnInfo(name = "discount_amount")
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "discount_percentage")
    val discountPercentage: Double = 0.0,
    
    @ColumnInfo(name = "taxable_amount")
    val taxableAmount: BigDecimal,
    
    // GST details for this line item
    @ColumnInfo(name = "gst_rate_id")
    val gstRateId: Long? = null,
    
    @ColumnInfo(name = "cgst_rate")
    val cgstRate: Double = 0.0,
    
    @ColumnInfo(name = "sgst_rate")
    val sgstRate: Double = 0.0,
    
    @ColumnInfo(name = "igst_rate")
    val igstRate: Double = 0.0,
    
    @ColumnInfo(name = "cess_rate")
    val cessRate: Double = 0.0,
    
    @ColumnInfo(name = "cgst_amount")
    val cgstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "sgst_amount")
    val sgstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "igst_amount")
    val igstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "cess_amount")
    val cessAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "total_gst_amount")
    val totalGSTAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "line_total")
    val lineTotal: BigDecimal,
    
    @ColumnInfo(name = "imei_serial")
    val imeiSerial: String? = null,
    
    @ColumnInfo(name = "batch_number")
    val batchNumber: String? = null,
    
    @ColumnInfo(name = "warranty_period")
    val warrantyPeriod: String? = null
)

/**
 * Invoice types supported
 */
enum class InvoiceType {
    SALE,
    RETURN,
    CREDIT_NOTE,
    DEBIT_NOTE,
    PROFORMA,
    QUOTATION
}

/**
 * Data class for invoice with complete details including line items and GST breakdown
 */
data class InvoiceWithDetails(
    val invoice: Invoice,
    val lineItems: List<InvoiceLineItem>,
    val customer: Customer? = null,
    val gstConfiguration: GSTConfiguration? = null,
    val invoiceGSTDetails: InvoiceGSTDetails? = null
)

/**
 * GST summary for invoice display
 */
data class InvoiceGSTSummary(
    val subtotal: BigDecimal,
    val discountAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    val cgstAmount: BigDecimal,
    val sgstAmount: BigDecimal,
    val igstAmount: BigDecimal,
    val cessAmount: BigDecimal,
    val totalGSTAmount: BigDecimal,
    val roundOffAmount: BigDecimal,
    val grandTotal: BigDecimal,
    val isInterstate: Boolean,
    val gstBreakdown: List<GSTRateBreakdownItem>
)

/**
 * GST rate breakdown item for detailed display
 */
data class GSTRateBreakdownItem(
    val gstRate: Double,
    val taxableAmount: BigDecimal,
    val cgstAmount: BigDecimal = BigDecimal.ZERO,
    val sgstAmount: BigDecimal = BigDecimal.ZERO,
    val igstAmount: BigDecimal = BigDecimal.ZERO,
    val cessAmount: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal,
    val hsnCodes: List<String> = emptyList()
)

// Extension method for creating return invoice from original
fun InvoiceWithDetails.toReturnInvoiceBuilder(billingService: Any? = null): InvoiceWithDetails {
    val returnInvoice = this.invoice.copy(
        invoiceType = InvoiceType.CREDIT_NOTE,
        invoiceNumber = "${this.invoice.invoiceNumber}-RET",
        grandTotal = -this.invoice.grandTotal,
        subtotalAmount = -this.invoice.subtotalAmount,
        totalGSTAmount = -this.invoice.totalGSTAmount,
        cgstAmount = -this.invoice.cgstAmount,
        sgstAmount = -this.invoice.sgstAmount,
        igstAmount = -this.invoice.igstAmount,
        cessAmount = -this.invoice.cessAmount,
        invoiceDate = kotlinx.datetime.Clock.System.now()
    )
    
    val returnLineItems = this.lineItems.map { item ->
        item.copy(
            quantity = -item.quantity,
            lineTotal = -item.lineTotal,
            cgstAmount = -item.cgstAmount,
            sgstAmount = -item.sgstAmount,
            igstAmount = -item.igstAmount,
            cessAmount = -item.cessAmount,
            totalGSTAmount = -item.totalGSTAmount
        )
    }
    
    return InvoiceWithDetails(
        invoice = returnInvoice,
        lineItems = returnLineItems,
        customer = this.customer,
        gstConfiguration = this.gstConfiguration,
        invoiceGSTDetails = this.invoiceGSTDetails
    )
}

// Legacy builder pattern support (returns an object with a build method for compatibility)
@Suppress("UNCHECKED_CAST")
fun InvoiceWithDetails.toReturnInvoiceBuilderLegacy(billingService: Any? = null): Any {
    return object {
        fun build(): InvoiceWithDetails = this@toReturnInvoiceBuilderLegacy
    }
}