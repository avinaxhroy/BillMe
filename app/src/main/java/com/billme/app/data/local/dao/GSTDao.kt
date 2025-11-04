package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

// GST Configuration and Rate DAOs are now in separate files
// GSTConfigurationDao.kt and GSTRateDao.kt

@Dao
interface InvoiceGSTDetailsDao {
    
    @Query("SELECT * FROM invoice_gst_details WHERE transaction_id = :transactionId")
    suspend fun getGSTDetailsByTransactionId(transactionId: Long): InvoiceGSTDetails?
    
    @Query("SELECT * FROM invoice_gst_details WHERE transaction_id = :transactionId")
    fun getGSTDetailsByTransactionIdFlow(transactionId: Long): Flow<InvoiceGSTDetails?>
    
    @Query("""
        SELECT * FROM invoice_gst_details 
        WHERE gst_mode = :gstMode 
        ORDER BY created_at DESC
    """)
    fun getGSTDetailsByMode(gstMode: GSTMode): Flow<List<InvoiceGSTDetails>>
    
    @Query("""
        SELECT * FROM invoice_gst_details 
        WHERE shop_gstin = :shopGSTIN 
        AND created_at BETWEEN :startDate AND :endDate
        ORDER BY created_at DESC
    """)
    fun getGSTDetailsForPeriod(
        shopGSTIN: String,
        startDate: Instant,
        endDate: Instant
    ): Flow<List<InvoiceGSTDetails>>
    
    @Query("""
        SELECT SUM(total_gst_amount) FROM invoice_gst_details 
        WHERE shop_gstin = :shopGSTIN 
        AND created_at BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalGSTForPeriod(
        shopGSTIN: String,
        startDate: Instant,
        endDate: Instant
    ): Double?
    
    @Query("""
        SELECT SUM(cgst_amount) as total_cgst,
               SUM(sgst_amount) as total_sgst,
               SUM(igst_amount) as total_igst,
               SUM(cess_amount) as total_cess
        FROM invoice_gst_details 
        WHERE shop_gstin = :shopGSTIN 
        AND created_at BETWEEN :startDate AND :endDate
    """)
    suspend fun getGSTBreakdownForPeriod(
        shopGSTIN: String,
        startDate: Instant,
        endDate: Instant
    ): GSTSummary?
    
    @Query("""
        SELECT COUNT(*) FROM invoice_gst_details 
        WHERE gst_mode != 'NO_GST' 
        AND created_at BETWEEN :startDate AND :endDate
    """)
    suspend fun getGSTTransactionCountForPeriod(
        startDate: Instant,
        endDate: Instant
    ): Int
    
    @Query("""
        SELECT * FROM invoice_gst_details 
        WHERE is_interstate = :isInterstate 
        AND created_at BETWEEN :startDate AND :endDate
        ORDER BY created_at DESC
    """)
    fun getGSTDetailsByState(
        isInterstate: Boolean,
        startDate: Instant,
        endDate: Instant
    ): Flow<List<InvoiceGSTDetails>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGSTDetails(details: InvoiceGSTDetails): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleGSTDetails(details: List<InvoiceGSTDetails>): List<Long>
    
    @Update
    suspend fun updateGSTDetails(details: InvoiceGSTDetails)
    
    @Delete
    suspend fun deleteGSTDetails(details: InvoiceGSTDetails)
    
    @Query("DELETE FROM invoice_gst_details WHERE transaction_id = :transactionId")
    suspend fun deleteGSTDetailsByTransactionId(transactionId: Long)
    
    // Analytics queries
    @Query("""
        SELECT gst_mode, COUNT(*) as count, SUM(total_gst_amount) as total_amount
        FROM invoice_gst_details 
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY gst_mode
    """)
    suspend fun getGSTModeAnalytics(
        startDate: Instant,
        endDate: Instant
    ): List<GSTModeAnalytics>
    
    @Query("""
        SELECT 
            CASE WHEN is_interstate = 1 THEN 'Interstate' ELSE 'Intrastate' END as transaction_type,
            COUNT(*) as count,
            SUM(total_gst_amount) as total_amount,
            SUM(cgst_amount) as total_cgst,
            SUM(sgst_amount) as total_sgst,
            SUM(igst_amount) as total_igst
        FROM invoice_gst_details 
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY is_interstate
    """)
    suspend fun getStateWiseGSTAnalytics(
        startDate: Instant,
        endDate: Instant
    ): List<StateWiseGSTAnalytics>
}

/**
 * Data classes for GST analytics
 */
data class GSTSummary(
    @ColumnInfo(name = "total_cgst") val totalCgst: Double,
    @ColumnInfo(name = "total_sgst") val totalSgst: Double,
    @ColumnInfo(name = "total_igst") val totalIgst: Double,
    @ColumnInfo(name = "total_cess") val totalCess: Double
)

data class GSTModeAnalytics(
    @ColumnInfo(name = "gst_mode") val gstMode: GSTMode,
    val count: Int,
    @ColumnInfo(name = "total_amount") val totalAmount: Double
)

data class StateWiseGSTAnalytics(
    @ColumnInfo(name = "transaction_type") val transactionType: String,
    val count: Int,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    @ColumnInfo(name = "total_cgst") val totalCgst: Double,
    @ColumnInfo(name = "total_sgst") val totalSgst: Double,
    @ColumnInfo(name = "total_igst") val totalIgst: Double
)
