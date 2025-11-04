package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone_number"], unique = true),
        Index(value = ["customer_segment"]),
        Index(value = ["last_purchase_date"])
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "customer_id")
    val customerId: Long = 0,
    
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    
    @ColumnInfo(name = "customer_name")
    val customerName: String? = null,
    
    @ColumnInfo(name = "email")
    val email: String? = null,
    
    @ColumnInfo(name = "address")
    val address: String? = null,
    
    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: LocalDate? = null,
    
    @ColumnInfo(name = "total_purchases")
    val totalPurchases: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "total_transactions")
    val totalTransactions: Int = 0,
    
    @ColumnInfo(name = "last_purchase_date")
    val lastPurchaseDate: Instant? = null,
    
    @ColumnInfo(name = "customer_segment")
    val customerSegment: CustomerSegment = CustomerSegment.NEW,
    
    @ColumnInfo(name = "loyalty_points")
    val loyaltyPoints: Int = 0,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null
)

enum class CustomerSegment {
    NEW, REGULAR, VIP
}