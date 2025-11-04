package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.Supplier
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface SupplierDao {
    
    @Query("SELECT * FROM suppliers WHERE is_active = 1 ORDER BY supplier_name ASC")
    fun getAllActiveSuppliers(): Flow<List<Supplier>>
    
    @Query("SELECT * FROM suppliers ORDER BY supplier_name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>
    
    @Query("SELECT * FROM suppliers WHERE supplier_id = :supplierId")
    suspend fun getSupplierById(supplierId: Long): Supplier?
    
    @Query("SELECT * FROM suppliers WHERE supplier_name = :supplierName AND is_active = 1")
    suspend fun getSupplierByName(supplierName: String): Supplier?
    
    @Query("SELECT * FROM suppliers WHERE supplier_phone = :phone AND is_active = 1")
    suspend fun getSupplierByPhone(phone: String): Supplier?
    
    @Query("SELECT * FROM suppliers WHERE gst_number = :gstNumber AND is_active = 1")
    suspend fun getSupplierByGstNumber(gstNumber: String): Supplier?
    
    @Query("""
        SELECT * FROM suppliers 
        WHERE is_active = 1 AND (
            supplier_name LIKE '%' || :query || '%' OR
            supplier_phone LIKE '%' || :query || '%' OR
            contact_person LIKE '%' || :query || '%'
        )
        ORDER BY supplier_name ASC
    """)
    fun searchSuppliers(query: String): Flow<List<Supplier>>
    
    @Query("SELECT DISTINCT supplier_name FROM suppliers WHERE is_active = 1 ORDER BY supplier_name ASC")
    fun getAllSupplierNames(): Flow<List<String>>
    
    @Query("SELECT COUNT(*) FROM suppliers WHERE is_active = 1")
    suspend fun getActiveSuppliersCount(): Int
    
    @Query("SELECT COUNT(*) FROM suppliers WHERE outstanding_balance > credit_limit AND credit_limit > 0 AND is_active = 1")
    suspend fun getSuppliersExceedingCreditLimit(): Int
    
    @Query("SELECT SUM(outstanding_balance) FROM suppliers WHERE is_active = 1")
    suspend fun getTotalOutstandingBalance(): BigDecimal?
    
    @Query("""
        SELECT * FROM suppliers 
        WHERE outstanding_balance > credit_limit 
        AND credit_limit > 0 
        AND is_active = 1
        ORDER BY outstanding_balance DESC
    """)
    fun getSuppliersWithExceededCredit(): Flow<List<Supplier>>
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSupplier(supplier: Supplier): Long
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSuppliers(suppliers: List<Supplier>): List<Long>
    
    @Update
    suspend fun updateSupplier(supplier: Supplier)
    
    @Update
    suspend fun updateSuppliers(suppliers: List<Supplier>)
    
    @Query("UPDATE suppliers SET outstanding_balance = outstanding_balance + :amount WHERE supplier_id = :supplierId")
    suspend fun updateOutstandingBalance(supplierId: Long, amount: BigDecimal)
    
    @Query("UPDATE suppliers SET is_active = 0 WHERE supplier_id = :supplierId")
    suspend fun deactivateSupplier(supplierId: Long)
    
    @Query("UPDATE suppliers SET is_active = 1 WHERE supplier_id = :supplierId")
    suspend fun activateSupplier(supplierId: Long)
    
    @Delete
    suspend fun deleteSupplier(supplier: Supplier)
    
    @Query("DELETE FROM suppliers WHERE supplier_id = :supplierId")
    suspend fun deleteSupplierById(supplierId: Long)
    
    // Analytics queries
    @Query("""
        SELECT s.*, COUNT(p.product_id) as product_count 
        FROM suppliers s 
        LEFT JOIN products p ON s.supplier_id = p.supplier_id 
        WHERE s.is_active = 1 
        GROUP BY s.supplier_id 
        ORDER BY product_count DESC
    """)
    suspend fun getSuppliersWithProductCount(): List<SupplierWithProductCount>
    
    @Query("""
        SELECT * FROM suppliers 
        WHERE payment_terms_days > 0 
        AND outstanding_balance > 0 
        AND is_active = 1
        ORDER BY outstanding_balance DESC
    """)
    fun getSuppliersWithOutstandingPayments(): Flow<List<Supplier>>
    
    /**
     * Get total supplier count (for backup)
     */
    @Query("SELECT COUNT(*) FROM suppliers")
    suspend fun getTotalSupplierCount(): Int
}

/**
 * Data class for supplier analytics with product count
 */
data class SupplierWithProductCount(
    @Embedded val supplier: Supplier,
    @ColumnInfo(name = "product_count") val productCount: Int
)
