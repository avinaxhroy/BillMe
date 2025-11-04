package com.billme.app.data.local.entity

import java.math.BigDecimal

/**
 * Data class representing an item in a transaction for invoice generation
 * This is a simplified version used for PDF/receipt generation
 */
data class TransactionItem(
    val transactionItemId: Long,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null,
    val variant: String? = null,
    val quantity: Int,
    val costPrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val imeiNumber: String? = null, // Primary IMEI number for the product
    val imei2Number: String? = null // Secondary IMEI number (for dual SIM)
)
