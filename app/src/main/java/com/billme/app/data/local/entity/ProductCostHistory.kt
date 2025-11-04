package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Tracks historical cost prices for a product
 * Allows multiple purchase prices to be recorded for better profit analysis
 * Useful when product costs vary due to market fluctuations
 */
@Entity(
    tableName = "product_cost_history",
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
        Index(value = ["purchase_date"]),
        Index(value = ["is_current"])
    ]
)
data class ProductCostHistory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cost_history_id")
    val costHistoryId: Long = 0,
    
    @ColumnInfo(name = "product_id")
    val productId: Long,
    
    @ColumnInfo(name = "cost_price")
    val costPrice: BigDecimal,
    
    @ColumnInfo(name = "purchase_date")
    val purchaseDate: Instant,
    
    @ColumnInfo(name = "quantity_purchased")
    val quantityPurchased: Int = 1,
    
    @ColumnInfo(name = "supplier_name")
    val supplierName: String? = null,
    
    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String? = null,
    
    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean = true,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
