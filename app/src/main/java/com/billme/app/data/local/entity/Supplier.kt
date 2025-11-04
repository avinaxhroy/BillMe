package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import java.math.BigDecimal

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["supplier_name"], unique = true),
        Index(value = ["supplier_phone"], unique = true),
        Index(value = ["gst_number"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "supplier_id")
    val supplierId: Long = 0,
    
    @ColumnInfo(name = "supplier_name")
    val supplierName: String,
    
    @ColumnInfo(name = "supplier_phone")
    val supplierPhone: String? = null,
    
    @ColumnInfo(name = "supplier_email")
    val supplierEmail: String? = null,
    
    @ColumnInfo(name = "supplier_address")
    val supplierAddress: String? = null,
    
    @ColumnInfo(name = "contact_person")
    val contactPerson: String? = null,
    
    @ColumnInfo(name = "gst_number")
    val gstNumber: String? = null,
    
    @ColumnInfo(name = "credit_limit")
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "outstanding_balance")
    val outstandingBalance: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "payment_terms_days")
    val paymentTermsDays: Int = 30,
    
    @ColumnInfo(name = "discount_percentage")
    val discountPercentage: Double = 0.0,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
) {
    val remainingCreditLimit: BigDecimal
        get() = creditLimit - outstandingBalance
        
    val isCreditLimitExceeded: Boolean
        get() = outstandingBalance > creditLimit && creditLimit > BigDecimal.ZERO
}