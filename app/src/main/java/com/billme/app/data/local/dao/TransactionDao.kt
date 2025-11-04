package com.billme.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import androidx.room.ColumnInfo
import com.billme.app.data.local.entity.Transaction
import com.billme.app.data.local.entity.TransactionLineItem
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.math.BigDecimal

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE is_draft = 0 ORDER BY transaction_date DESC")
    fun getAllCompletedTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE is_draft = 1 ORDER BY transaction_date DESC")
    fun getAllDraftTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE transaction_id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): Transaction?
    
    @Query("""
        SELECT * FROM transactions 
        WHERE transaction_date >= :startDate 
        AND transaction_date <= :endDate 
        AND is_draft = 0 
        ORDER BY transaction_date DESC
    """)
    fun getTransactionsByDateRange(startDate: Instant, endDate: Instant): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE customer_phone = :phoneNumber AND is_draft = 0 ORDER BY transaction_date DESC")
    fun getTransactionsByCustomer(phoneNumber: String): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE transaction_number LIKE '%' || :query || '%' OR customer_name LIKE '%' || :query || '%'")
    fun searchTransactions(query: String): Flow<List<Transaction>>
    
    // Daily sales summary
    @Query("""
        SELECT COALESCE(SUM(grand_total), 0) 
        FROM transactions 
        WHERE DATE(transaction_date/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch') 
        AND is_draft = 0
    """)
    suspend fun getDailySales(date: Instant): BigDecimal
    
    @Query("""
        SELECT COALESCE(SUM(profit_amount), 0) 
        FROM transactions 
        WHERE DATE(transaction_date/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch') 
        AND is_draft = 0
    """)
    suspend fun getDailyProfit(date: Instant): BigDecimal
    
    @Query("""
        SELECT COUNT(*) 
        FROM transactions 
        WHERE DATE(transaction_date/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch') 
        AND is_draft = 0
    """)
    suspend fun getDailyTransactionCount(date: Instant): Int
    
    // Recent transactions
    @Query("SELECT * FROM transactions WHERE is_draft = 0 ORDER BY transaction_date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Query("UPDATE transactions SET is_draft = 0 WHERE transaction_id = :transactionId")
    suspend fun completeDraftTransaction(transactionId: Long)
    
    @Query("UPDATE transactions SET receipt_printed = 1 WHERE transaction_id = :transactionId")
    suspend fun markReceiptPrinted(transactionId: Long)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions WHERE is_draft = 1 AND transaction_date < :cutoffDate")
    suspend fun deleteOldDrafts(cutoffDate: Instant)
    
    // Get next transaction number
    @Query("SELECT MAX(CAST(SUBSTR(transaction_number, 9) AS INTEGER)) FROM transactions WHERE transaction_number LIKE 'TXN-' || strftime('%Y', 'now') || '-%'")
    suspend fun getLastTransactionNumber(): Int?
    
    // Analytics methods for BusinessMetricsRepository
    @Query("""
        SELECT COALESCE(SUM(grand_total), 0) 
        FROM transactions 
        WHERE created_at BETWEEN :startDate AND :endDate 
        AND is_draft = 0
    """)
    suspend fun getTotalSalesForPeriod(startDate: Instant, endDate: Instant): BigDecimal
    
    @Query("""
        SELECT COALESCE(SUM(profit_amount), 0) 
        FROM transactions 
        WHERE created_at BETWEEN :startDate AND :endDate 
        AND is_draft = 0
    """)
    suspend fun getTotalProfitForPeriod(startDate: Instant, endDate: Instant): BigDecimal
    
    @Query("""
        SELECT COUNT(*) 
        FROM transactions 
        WHERE created_at BETWEEN :startDate AND :endDate 
        AND is_draft = 0
    """)
    suspend fun getTransactionCountForPeriod(startDate: Instant, endDate: Instant): Int
    
    @Query("""
        SELECT COALESCE(SUM(grand_total), 0) 
        FROM transactions 
        WHERE payment_status = 'PENDING' OR payment_status = 'PARTIALLY_PAID'
        AND is_draft = 0
    """)
    suspend fun getPendingPayments(): BigDecimal
    
    /**
     * Get monthly sales data for a date range
     */
    @Query("""
        SELECT STRFTIME('%Y-%m', created_at/1000, 'unixepoch') as month,
               COALESCE(SUM(grand_total), 0) as totalAmount,
               COUNT(*) as transactionCount
        FROM transactions 
        WHERE created_at BETWEEN :startDate AND :endDate 
        AND is_draft = 0
        GROUP BY STRFTIME('%Y-%m', created_at/1000, 'unixepoch')
        ORDER BY month DESC
    """)
    suspend fun getMonthlySalesData(startDate: Instant, endDate: Instant): List<MonthlySalesDataQuery>
    
    @Query("""
        SELECT DATE(created_at/1000, 'unixepoch') as date, 
               COALESCE(SUM(grand_total), 0) as totalAmount,
               COUNT(*) as transactionCount
        FROM transactions 
        WHERE created_at BETWEEN :startDate AND :endDate 
        AND is_draft = 0
        GROUP BY DATE(created_at/1000, 'unixepoch')
        ORDER BY date DESC
    """)
    fun getDailySalesForPeriod(startDate: Instant, endDate: Instant): Flow<List<DailySalesDataQuery>>
}

// Local query result classes
data class MonthlySalesDataQuery(
    val month: String,
    val totalAmount: BigDecimal,
    val transactionCount: Int
)

data class DailySalesDataQuery(
    val date: String,
    val totalAmount: BigDecimal,
    val transactionCount: Int
)
