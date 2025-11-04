package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

/**
 * Product status tracking for inventory management
 */
enum class ProductStatus {
    IN_STOCK,     // Available for sale
    SOLD,         // Sold to customer
    RETURNED,     // Returned by customer
    DAMAGED,      // Damaged goods
    RESERVED,     // Reserved for customer
    OUT_OF_STOCK  // Stock depleted
}

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["supplier_id"],
            childColumns = ["supplier_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["imei1"], unique = true),
        Index(value = ["imei2"], unique = true),
        Index(value = ["brand", "model"]),
        Index(value = ["brand", "model", "color", "variant"]),
        Index(value = ["category"]),
        Index(value = ["color"]),
        Index(value = ["variant"]),
        Index(value = ["current_stock"]),
        Index(value = ["product_status"]),
        Index(value = ["supplier_id"]),
        Index(value = ["warranty_expiry_date"]),
        Index(value = ["is_active"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "product_id")
    val productId: Long = 0,
    
    @ColumnInfo(name = "product_name")
    val productName: String,
    
    @ColumnInfo(name = "brand")
    val brand: String,
    
    @ColumnInfo(name = "model")
    val model: String,
    
    @ColumnInfo(name = "color")
    val color: String? = null,
    
    @ColumnInfo(name = "variant")
    val variant: String? = null,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "imei1")
    val imei1: String,
    
    @ColumnInfo(name = "imei2")
    val imei2: String? = null,
    
    @ColumnInfo(name = "mrp")
    val mrp: BigDecimal? = null,
    
    @ColumnInfo(name = "cost_price")
    val costPrice: BigDecimal,
    
    @ColumnInfo(name = "selling_price")
    val sellingPrice: BigDecimal,
    
    @ColumnInfo(name = "product_status")
    val productStatus: ProductStatus = ProductStatus.IN_STOCK,
    
    @ColumnInfo(name = "current_stock")
    val currentStock: Int = 1,
    
    @ColumnInfo(name = "min_stock_level")
    val minStockLevel: Int = 1,
    
    @ColumnInfo(name = "barcode")
    val barcode: String? = null,
    
    @ColumnInfo(name = "product_photos")
    val productPhotos: String? = null, // Comma-separated file paths
    
    @ColumnInfo(name = "supplier_id")
    val supplierId: Long? = null,
    
    @ColumnInfo(name = "warranty_period_months")
    val warrantyPeriodMonths: Int? = null,
    
    @ColumnInfo(name = "warranty_start_date")
    val warrantyStartDate: LocalDate? = null,
    
    @ColumnInfo(name = "warranty_expiry_date")
    val warrantyExpiryDate: LocalDate? = null,
    
    @ColumnInfo(name = "purchase_date")
    val purchaseDate: Instant,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
) {
    val profitAmount: BigDecimal
        get() = sellingPrice - costPrice
    
    val profitPercentage: Double
        get() = if (costPrice > BigDecimal.ZERO) {
            ((sellingPrice - costPrice) / costPrice * BigDecimal(100)).toDouble()
        } else 0.0
    
    val mrpDiscountAmount: BigDecimal?
        get() = mrp?.let { it - sellingPrice }
    
    val mrpDiscountPercentage: Double?
        get() = mrp?.let { mrpValue ->
            if (mrpValue > BigDecimal.ZERO) {
                ((mrpValue - sellingPrice) / mrpValue * BigDecimal(100)).toDouble()
            } else 0.0
        }
    
    val isLowStock: Boolean
        get() = currentStock <= minStockLevel
    
    val isAvailableForSale: Boolean
        get() = productStatus == ProductStatus.IN_STOCK && currentStock > 0 && isActive
    
    val isWarrantyExpiring: Boolean
        get() = warrantyExpiryDate?.let { expiryDate ->
            val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
            val daysUntilExpiry = expiryDate.toEpochDays() - today.toEpochDays()
            daysUntilExpiry in 1..30 // Warning if expiring within 30 days
        } ?: false
    
    val isWarrantyExpired: Boolean
        get() = warrantyExpiryDate?.let { expiryDate ->
            val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
            expiryDate < today
        } ?: false
    
    val warrantyStatus: WarrantyStatus
        get() = when {
            warrantyExpiryDate == null -> WarrantyStatus.NO_WARRANTY
            isWarrantyExpired -> WarrantyStatus.EXPIRED
            isWarrantyExpiring -> WarrantyStatus.EXPIRING_SOON
            else -> WarrantyStatus.ACTIVE
        }
    
    /**
     * Full product display name including color and variant
     */
    val fullProductName: String
        get() = buildString {
            append(productName)
            if (!color.isNullOrBlank() || !variant.isNullOrBlank()) {
                append(" (")
                val parts = mutableListOf<String>()
                if (!color.isNullOrBlank()) parts.add(color)
                if (!variant.isNullOrBlank()) parts.add(variant)
                append(parts.joinToString(", "))
                append(")")
            }
        }
    
    /**
     * Complete product identifier for search and display
     */
    val productIdentifier: String
        get() = buildString {
            append("$brand $model")
            if (!color.isNullOrBlank()) append(" $color")
            if (!variant.isNullOrBlank()) append(" $variant")
        }
}

/**
 * Warranty status enumeration
 */
enum class WarrantyStatus {
    NO_WARRANTY,
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED
}
