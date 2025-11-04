package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * DAO for Invoice operations
 */
@Dao
interface InvoiceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<Invoice>): List<Long>
    
    @Update
    suspend fun updateInvoice(invoice: Invoice)
    
    @Delete
    suspend fun deleteInvoice(invoice: Invoice)
    
    @Query("SELECT * FROM invoices WHERE invoice_id = :invoiceId")
    suspend fun getInvoiceById(invoiceId: Long): Invoice?
    
    @Query("SELECT * FROM invoices WHERE invoice_number = :invoiceNumber")
    suspend fun getInvoiceByNumber(invoiceNumber: String): Invoice?
    
    @Query("SELECT * FROM invoices ORDER BY invoice_date DESC")
    fun getAllInvoices(): Flow<List<Invoice>>
    
    @Query("SELECT * FROM invoices WHERE invoice_date BETWEEN :startDate AND :endDate ORDER BY invoice_date DESC")
    fun getInvoicesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Invoice>>
    
    @Query("SELECT * FROM invoices WHERE customer_id = :customerId ORDER BY invoice_date DESC")
    fun getInvoicesByCustomer(customerId: Long): Flow<List<Invoice>>
    
    @Query("SELECT * FROM invoices WHERE gst_mode = :gstMode ORDER BY invoice_date DESC")
    fun getInvoicesByGSTMode(gstMode: GSTMode): Flow<List<Invoice>>
    
    @Query("SELECT * FROM invoices WHERE payment_status IN ('PENDING', 'PARTIALLY_PAID') ORDER BY invoice_date DESC")
    fun getPendingInvoices(): Flow<List<Invoice>>
    
    @Query("SELECT * FROM invoices WHERE invoice_number LIKE :query OR customer_name LIKE :query ORDER BY invoice_date DESC")
    fun searchInvoices(query: String): Flow<List<Invoice>>
    
    @Query("UPDATE invoices SET is_cancelled = 1, cancelled_at = :cancelledAt, cancellation_reason = :reason WHERE invoice_id = :invoiceId")
    suspend fun cancelInvoice(invoiceId: Long, cancelledAt: Instant, reason: String)
    
    @Query("SELECT c.* FROM customers c WHERE c.customer_id = :customerId")
    suspend fun getCustomerById(customerId: Long): Customer?
    
    @Query("SELECT COUNT(*) as totalInvoices, SUM(grand_total) as totalAmount, AVG(grand_total) as averageAmount FROM invoices WHERE invoice_date BETWEEN :startDate AND :endDate AND is_cancelled = 0")
    suspend fun getInvoiceStatistics(startDate: Instant, endDate: Instant): InvoiceStatistics
    
    @Query("SELECT SUM(cgst_amount) as totalCGST, SUM(sgst_amount) as totalSGST, SUM(igst_amount) as totalIGST, SUM(cess_amount) as totalCess FROM invoices WHERE invoice_date BETWEEN :startDate AND :endDate AND is_cancelled = 0")
    suspend fun getGSTStatistics(startDate: Instant, endDate: Instant): GSTStatistics
    
    @Query("SELECT invoice_id, invoice_number, invoice_date, customer_gstin, subtotal_amount, cgst_amount, sgst_amount, igst_amount, cess_amount, grand_total, is_interstate FROM invoices WHERE invoice_date BETWEEN :startDate AND :endDate AND gst_mode != 'NO_GST' AND is_cancelled = 0 ORDER BY invoice_date")
    suspend fun getInvoicesForGSTReturn(startDate: Instant, endDate: Instant): List<GSTReturnInvoiceData>
    
    @Query("UPDATE invoices SET payment_status = :paymentStatus WHERE invoice_id IN (:invoiceIds)")
    suspend fun bulkUpdatePaymentStatus(invoiceIds: List<Long>, paymentStatus: PaymentStatus)
    
    @Query("SELECT invoice_id, invoice_number, invoice_date, grand_total, cgst_amount, sgst_amount, igst_amount, total_gst_amount FROM invoices WHERE invoice_date BETWEEN :startDate AND :endDate ORDER BY invoice_date DESC")
    fun getInvoicesWithGSTBreakdown(startDate: Instant, endDate: Instant): Flow<List<InvoiceGSTBreakdown>>
    
    // Convenience methods for repository compatibility
    @Query("SELECT * FROM invoices WHERE invoice_date BETWEEN :startDate AND :endDate ORDER BY invoice_date DESC")
    suspend fun getInvoicesForExport(startDate: Instant, endDate: Instant): List<Invoice>
    
    /**
     * Get total invoice count (for backup)
     */
    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun getTotalInvoiceCount(): Int
}

/**
 * Data classes for invoice statistics and reporting
 */
data class InvoiceStatistics(
    val totalInvoices: Int,
    val totalAmount: BigDecimal,
    val averageAmount: BigDecimal
)

data class GSTStatistics(
    val totalCGST: BigDecimal,
    val totalSGST: BigDecimal,
    val totalIGST: BigDecimal,
    val totalCess: BigDecimal
) {
    val totalGST: BigDecimal
        get() = totalCGST + totalSGST + totalIGST + totalCess
}

data class GSTReturnInvoiceData(
    @ColumnInfo(name = "invoice_id") val invoiceId: Long,
    @ColumnInfo(name = "invoice_number") val invoiceNumber: String,
    @ColumnInfo(name = "invoice_date") val invoiceDate: Instant,
    @ColumnInfo(name = "customer_gstin") val customerGSTIN: String?,
    @ColumnInfo(name = "subtotal_amount") val subtotalAmount: BigDecimal,
    @ColumnInfo(name = "cgst_amount") val cgstAmount: BigDecimal,
    @ColumnInfo(name = "sgst_amount") val sgstAmount: BigDecimal,
    @ColumnInfo(name = "igst_amount") val igstAmount: BigDecimal,
    @ColumnInfo(name = "cess_amount") val cessAmount: BigDecimal,
    @ColumnInfo(name = "grand_total") val grandTotal: BigDecimal,
    @ColumnInfo(name = "is_interstate") val isInterstate: Boolean
)

data class InvoiceGSTBreakdown(
    @ColumnInfo(name = "invoice_id") val invoiceId: Long,
    @ColumnInfo(name = "invoice_number") val invoiceNumber: String,
    @ColumnInfo(name = "invoice_date") val invoiceDate: Instant,
    @ColumnInfo(name = "grand_total") val grandTotal: BigDecimal,
    @ColumnInfo(name = "cgst_amount") val cgstAmount: BigDecimal,
    @ColumnInfo(name = "sgst_amount") val sgstAmount: BigDecimal,
    @ColumnInfo(name = "igst_amount") val igstAmount: BigDecimal,
    @ColumnInfo(name = "total_gst_amount") val totalGstAmount: BigDecimal
)
