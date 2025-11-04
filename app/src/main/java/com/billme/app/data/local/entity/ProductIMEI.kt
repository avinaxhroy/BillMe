package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Status of individual IMEI units
 */
enum class IMEIStatus {
    AVAILABLE,    // Available for sale
    SOLD,         // Sold to customer
    RESERVED,     // Reserved for customer
    DAMAGED,      // Damaged unit
    RETURNED      // Returned by customer
}

/**
 * Individual IMEI tracking for products
 * Each smartphone/digital product unit has a unique IMEI
 */
@Entity(
    tableName = "product_imeis",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["transaction_id"],
            childColumns = ["sold_in_transaction_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["imei_number"], unique = true),
        Index(value = ["product_id"]),
        Index(value = ["status"]),
        Index(value = ["sold_in_transaction_id"]),
        Index(value = ["purchase_date"])
    ]
)
data class ProductIMEI(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "imei_id")
    val imeiId: Long = 0,
    
    @ColumnInfo(name = "product_id")
    val productId: Long,
    
    @ColumnInfo(name = "imei_number")
    val imeiNumber: String,
    
    @ColumnInfo(name = "imei2_number")
    val imei2Number: String? = null, // For dual SIM phones
    
    @ColumnInfo(name = "serial_number")
    val serialNumber: String? = null,
    
    @ColumnInfo(name = "status")
    val status: IMEIStatus = IMEIStatus.AVAILABLE,
    
    @ColumnInfo(name = "purchase_date")
    val purchaseDate: Instant,
    
    @ColumnInfo(name = "purchase_price")
    val purchasePrice: java.math.BigDecimal,
    
    @ColumnInfo(name = "sold_date")
    val soldDate: Instant? = null,
    
    @ColumnInfo(name = "sold_price")
    val soldPrice: java.math.BigDecimal? = null,
    
    @ColumnInfo(name = "sold_in_transaction_id")
    val soldInTransactionId: Long? = null,
    
    @ColumnInfo(name = "box_number")
    val boxNumber: String? = null,
    
    @ColumnInfo(name = "warranty_card_number")
    val warrantyCardNumber: String? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)

/**
 * Product with IMEI details for display
 */
data class ProductWithIMEIs(
    val product: Product,
    val imeis: List<ProductIMEI>
) {
    val availableCount: Int
        get() = imeis.count { it.status == IMEIStatus.AVAILABLE }
    
    val soldCount: Int
        get() = imeis.count { it.status == IMEIStatus.SOLD }
    
    val totalCount: Int
        get() = imeis.size
    
    val availableIMEIs: List<ProductIMEI>
        get() = imeis.filter { it.status == IMEIStatus.AVAILABLE }
}
