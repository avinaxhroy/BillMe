package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Reasons for stock adjustment
 */
enum class StockAdjustmentReason {
    MANUAL_RECOUNT,      // Physical inventory recount
    DAMAGED,             // Product damaged
    THEFT,               // Product stolen
    FOUND,               // Found during audit
    RETURNED,            // Customer return
    SUPPLIER_ERROR,      // Wrong quantity from supplier
    CORRECTION,          // Data entry correction
    INITIAL_STOCK,       // Initial stock entry
    TRANSFER_IN,         // Transfer from another location
    TRANSFER_OUT,        // Transfer to another location
    EXPIRED,             // Product expired
    LOST,                // Product lost
    OTHER;               // Other reason

    fun getDisplayName(): String = when (this) {
        MANUAL_RECOUNT -> "Manual Recount"
        DAMAGED -> "Damaged"
        THEFT -> "Theft/Stolen"
        FOUND -> "Found in Audit"
        RETURNED -> "Customer Return"
        SUPPLIER_ERROR -> "Supplier Error"
        CORRECTION -> "Data Correction"
        INITIAL_STOCK -> "Initial Stock"
        TRANSFER_IN -> "Transfer In"
        TRANSFER_OUT -> "Transfer Out"
        EXPIRED -> "Expired"
        LOST -> "Lost"
        OTHER -> "Other"
    }
}

/**
 * Stock adjustment history for audit trail
 */
@Entity(
    tableName = "stock_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["adjustment_date"]),
        Index(value = ["reason"])
    ]
)
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "adjustment_id")
    val adjustmentId: Long = 0,
    
    @ColumnInfo(name = "product_id")
    val productId: Long,
    
    @ColumnInfo(name = "previous_stock")
    val previousStock: Int,
    
    @ColumnInfo(name = "new_stock")
    val newStock: Int,
    
    @ColumnInfo(name = "adjustment_quantity")
    val adjustmentQuantity: Int, // Can be positive or negative
    
    @ColumnInfo(name = "reason")
    val reason: StockAdjustmentReason,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "adjusted_by")
    val adjustedBy: String, // User who made the adjustment
    
    @ColumnInfo(name = "adjustment_date")
    val adjustmentDate: Instant,
    
    @ColumnInfo(name = "reference_number")
    val referenceNumber: String? = null // For linking to transactions, returns, etc.
)

/**
 * Data class for stock adjustment with product details
 */
data class StockAdjustmentWithProduct(
    @ColumnInfo(name = "adjustment_id") val adjustmentId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "previous_stock") val previousStock: Int,
    @ColumnInfo(name = "new_stock") val newStock: Int,
    @ColumnInfo(name = "adjustment_quantity") val adjustmentQuantity: Int,
    @ColumnInfo(name = "reason") val reason: StockAdjustmentReason,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "adjusted_by") val adjustedBy: String,
    @ColumnInfo(name = "adjustment_date") val adjustmentDate: Instant,
    @ColumnInfo(name = "reference_number") val referenceNumber: String?,
    val productName: String,
    val brand: String,
    val model: String,
    val imei1: String
)
