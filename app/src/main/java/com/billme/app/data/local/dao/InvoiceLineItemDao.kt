package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.InvoiceLineItem
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * DAO for InvoiceLineItem operations
 */
@Dao
interface InvoiceLineItemDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItem(lineItem: InvoiceLineItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(lineItems: List<InvoiceLineItem>): List<Long>
    
    @Update
    suspend fun updateLineItem(lineItem: InvoiceLineItem)
    
    @Delete
    suspend fun deleteLineItem(lineItem: InvoiceLineItem)
    
    @Query("DELETE FROM invoice_line_items WHERE invoice_id = :invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: Long)
    
    @Query("SELECT * FROM invoice_line_items WHERE line_item_id = :lineItemId")
    suspend fun getLineItemById(lineItemId: Long): InvoiceLineItem?
    
    @Query("SELECT * FROM invoice_line_items WHERE invoice_id = :invoiceId ORDER BY line_item_id")
    suspend fun getLineItemsByInvoiceId(invoiceId: Long): List<InvoiceLineItem>
    
    @Query("SELECT * FROM invoice_line_items WHERE product_id = :productId ORDER BY line_item_id DESC")
    fun getLineItemsByProductId(productId: Long): Flow<List<InvoiceLineItem>>
    
    @Query("SELECT COUNT(*) FROM invoice_line_items WHERE invoice_id = :invoiceId")
    suspend fun getLineItemCount(invoiceId: Long): Int
    
    @Query("SELECT SUM(line_total) FROM invoice_line_items WHERE invoice_id = :invoiceId")
    suspend fun getTotalForInvoice(invoiceId: Long): BigDecimal?
    
    @Query("""
        SELECT product_name, SUM(quantity) as totalQuantity, SUM(line_total) as totalRevenue 
        FROM invoice_line_items 
        GROUP BY product_id 
        ORDER BY totalRevenue DESC 
        LIMIT :limit
    """)
    fun getTopProductsByRevenue(limit: Int): Flow<List<ProductRevenue>>
    
    @Query("SELECT * FROM invoice_line_items WHERE hsn_code = :hsnCode ORDER BY line_item_id DESC")
    fun getLineItemsByHsnCode(hsnCode: String): Flow<List<InvoiceLineItem>>
    
    @Query("""
        SELECT product_name, SUM(quantity) as totalQuantity 
        FROM invoice_line_items 
        WHERE invoice_id IN (SELECT invoice_id FROM invoices WHERE invoice_date >= :fromDate)
        GROUP BY product_id 
        ORDER BY totalQuantity DESC 
        LIMIT :limit
    """)
    suspend fun getTopSellingProductsByQuantity(limit: Int, fromDate: kotlinx.datetime.Instant): List<ProductQuantity>
    
    @Query("SELECT SUM(total_gst_amount) FROM invoice_line_items WHERE invoice_id = :invoiceId")
    suspend fun getTotalGSTForInvoice(invoiceId: Long): BigDecimal?
}

/**
 * Data classes for reporting
 */
data class ProductRevenue(
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "totalQuantity") val totalQuantity: BigDecimal,
    @ColumnInfo(name = "totalRevenue") val totalRevenue: BigDecimal
)

data class ProductQuantity(
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "totalQuantity") val totalQuantity: BigDecimal
)