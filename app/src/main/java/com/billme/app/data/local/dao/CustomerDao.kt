package com.billme.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.ColumnInfo
import com.billme.app.data.local.entity.Customer
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface CustomerDao {
    
    @Query("SELECT * FROM customers WHERE is_active = 1 ORDER BY customer_name ASC")
    fun getAllActiveCustomers(): Flow<List<Customer>>
    
    @Query("SELECT * FROM customers WHERE customer_id = :customerId")
    suspend fun getCustomerById(customerId: Long): Customer?
    
    @Query("SELECT * FROM customers WHERE phone_number = :phoneNumber")
    suspend fun getCustomerByPhone(phoneNumber: String): Customer?
    
    @Query("""
        SELECT * FROM customers 
        WHERE is_active = 1 
        AND (customer_name LIKE '%' || :query || '%' OR phone_number LIKE '%' || :query || '%')
        ORDER BY customer_name ASC
    """)
    fun searchCustomers(query: String): Flow<List<Customer>>
    
    @Query("SELECT * FROM customers WHERE customer_segment = :segment AND is_active = 1 ORDER BY total_purchases DESC")
    fun getCustomersBySegment(segment: String): Flow<List<Customer>>
    
    @Query("SELECT * FROM customers ORDER BY total_purchases DESC LIMIT :limit")
    fun getTopCustomers(limit: Int = 10): Flow<List<Customer>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long
    
    @Update
    suspend fun updateCustomer(customer: Customer)
    
    @Delete
    suspend fun deleteCustomer(customer: Customer)
    
    @Query("UPDATE customers SET is_active = 0 WHERE customer_id = :customerId")
    suspend fun deactivateCustomer(customerId: Long)
    
    // Update customer stats after purchase
    @Query("""
        UPDATE customers 
        SET total_purchases = total_purchases + :amount,
            total_transactions = total_transactions + 1,
            last_purchase_date = :purchaseDate,
            loyalty_points = loyalty_points + :points
        WHERE phone_number = :phoneNumber
    """)
    suspend fun updateCustomerStats(
        phoneNumber: String, 
        amount: Double, 
        purchaseDate: Long, 
        points: Int
    )
    
    // Analytics methods for BusinessMetricsRepository
    @Query("SELECT COUNT(*) FROM customers WHERE is_active = 1")
    suspend fun getTotalCustomerCount(): Int
    
    @Query("SELECT COUNT(*) FROM customers WHERE is_active = 1 AND last_purchase_date >= :sinceDate")
    suspend fun getActiveCustomerCount(sinceDate: kotlinx.datetime.Instant): Int
    
    @Query("SELECT COUNT(*) FROM customers WHERE created_at BETWEEN :startDate AND :endDate")
    suspend fun getNewCustomersInPeriod(startDate: kotlinx.datetime.Instant, endDate: kotlinx.datetime.Instant): Int
    
    @Query("SELECT COUNT(*) FROM customers WHERE last_purchase_date BETWEEN :startDate AND :endDate AND total_transactions > 1")
    suspend fun getReturningCustomersInPeriod(startDate: kotlinx.datetime.Instant, endDate: kotlinx.datetime.Instant): Int
    
    @Query("SELECT customer_id, customer_name, phone_number, total_purchases, total_transactions, last_purchase_date FROM customers ORDER BY total_purchases DESC LIMIT :limit")
    suspend fun getTopCustomersData(limit: Int): List<TopCustomer>
    
    @Query("SELECT customer_segment, COUNT(*) as count FROM customers WHERE is_active = 1 GROUP BY customer_segment")
    suspend fun getCustomerSegments(): List<CustomerSegment>
    
    @Query("SELECT AVG(total_purchases) FROM customers WHERE is_active = 1")
    suspend fun getAverageCustomerValue(): java.math.BigDecimal
    
    @Query("""
        SELECT 
        (SELECT COUNT(*) FROM customers WHERE last_purchase_date >= :oneYearAgo) * 1.0 / 
        (SELECT COUNT(*) FROM customers WHERE created_at <= :oneYearAgo) * 100
        FROM customers LIMIT 1
    """)
    suspend fun getCustomerRetentionRate(oneYearAgo: kotlinx.datetime.Instant): Double
    
    /**
     * Get total customer count (for backup)
     */
    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int
}

/**
 * Data classes for customer analytics
 */
data class TopCustomer(
    @ColumnInfo(name = "customer_id") val customerId: Long,
    @ColumnInfo(name = "customer_name") val customerName: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String?,
    @ColumnInfo(name = "total_purchases") val totalPurchases: BigDecimal,
    @ColumnInfo(name = "total_transactions") val totalTransactions: Int,
    @ColumnInfo(name = "last_purchase_date") val lastPurchaseDate: kotlinx.datetime.Instant?
)

data class CustomerSegment(
    @ColumnInfo(name = "customer_segment") val segment: String,
    @ColumnInfo(name = "count") val count: Int
)
