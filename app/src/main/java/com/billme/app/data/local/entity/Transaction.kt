package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import java.math.BigDecimal

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transaction_date"]),
        Index(value = ["customer_phone"]),
        Index(value = ["is_draft"]),
        Index(value = ["payment_status"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long = 0,
    
    @ColumnInfo(name = "transaction_number")
    val transactionNumber: String,
    
    @ColumnInfo(name = "customer_phone")
    val customerPhone: String? = null,
    
    @ColumnInfo(name = "customer_name")
    val customerName: String? = null,
    
    @ColumnInfo(name = "transaction_date")
    val transactionDate: Instant,
    
    @ColumnInfo(name = "subtotal")
    val subtotal: BigDecimal,
    
    @ColumnInfo(name = "discount_amount")
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "discount_type")
    val discountType: DiscountType = DiscountType.AMOUNT,
    
    @ColumnInfo(name = "tax_amount")
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "tax_rate")
    val taxRate: Double = 0.0,
    
    @ColumnInfo(name = "grand_total")
    val grandTotal: BigDecimal,
    
    @ColumnInfo(name = "payment_method")
    val paymentMethod: PaymentMethod,
    
    @ColumnInfo(name = "payment_status")
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    
    @ColumnInfo(name = "profit_amount")
    val profitAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "receipt_printed")
    val receiptPrinted: Boolean = false,
    
    @ColumnInfo(name = "is_draft")
    val isDraft: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "sales_person")
    val salesPerson: String = "Admin"
)

enum class DiscountType {
    AMOUNT, PERCENT
}

enum class PaymentMethod {
    CASH, CARD, UPI, OTHER
}

enum class PaymentStatus {
    PAID, PENDING, PARTIALLY_PAID
}