package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Entity(
    tableName = "transaction_line_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["transaction_id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["product_id"]),
        Index(value = ["imei_sold"])
    ]
)
data class TransactionLineItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "line_item_id")
    val lineItemId: Long = 0,
    
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,
    
    @ColumnInfo(name = "product_id")
    val productId: Long,
    
    @ColumnInfo(name = "product_name")
    val productName: String,
    
    @ColumnInfo(name = "imei_sold")
    val imeiSold: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: Int = 1,
    
    @ColumnInfo(name = "unit_cost")
    val unitCost: BigDecimal,
    
    @ColumnInfo(name = "unit_selling_price")
    val unitSellingPrice: BigDecimal,
    
    @ColumnInfo(name = "line_total")
    val lineTotal: BigDecimal,
    
    @ColumnInfo(name = "line_profit")
    val lineProfit: BigDecimal,
    
    @ColumnInfo(name = "serial_number")
    val serialNumber: String? = null
)