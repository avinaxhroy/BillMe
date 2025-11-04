package com.billme.app.data.repository

import com.billme.app.data.local.dao.SupplierDao
import com.billme.app.data.local.entity.Supplier
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepository @Inject constructor(
    private val supplierDao: SupplierDao
) {
    
    fun getAllActiveSuppliers(): Flow<List<Supplier>> = supplierDao.getAllActiveSuppliers()
    
    fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    
    suspend fun getSupplierById(supplierId: Long): Supplier? = supplierDao.getSupplierById(supplierId)
    
    suspend fun getSupplierByName(supplierName: String): Supplier? = 
        supplierDao.getSupplierByName(supplierName)
    
    suspend fun getSupplierByPhone(phone: String): Supplier? = 
        supplierDao.getSupplierByPhone(phone)
    
    suspend fun getSupplierByGstNumber(gstNumber: String): Supplier? = 
        supplierDao.getSupplierByGstNumber(gstNumber)
    
    fun searchSuppliers(query: String): Flow<List<Supplier>> = supplierDao.searchSuppliers(query)
    
    fun getAllSupplierNames(): Flow<List<String>> = supplierDao.getAllSupplierNames()
    
    suspend fun getActiveSuppliersCount(): Int = supplierDao.getActiveSuppliersCount()
    
    suspend fun getSuppliersExceedingCreditLimit(): Int = supplierDao.getSuppliersExceedingCreditLimit()
    
    suspend fun getTotalOutstandingBalance(): BigDecimal = 
        supplierDao.getTotalOutstandingBalance() ?: BigDecimal.ZERO
    
    fun getSuppliersWithExceededCredit(): Flow<List<Supplier>> = 
        supplierDao.getSuppliersWithExceededCredit()
    
    fun getSuppliersWithOutstandingPayments(): Flow<List<Supplier>> = 
        supplierDao.getSuppliersWithOutstandingPayments()
    
    /**
     * Adds a new supplier with validation
     */
    suspend fun addSupplier(supplier: Supplier): Result<Long> {
        return try {
            // Check for duplicate name
            getSupplierByName(supplier.supplierName)?.let {
                return Result.failure(Exception("Supplier with this name already exists"))
            }
            
            // Check for duplicate phone if provided
            supplier.supplierPhone?.let { phone ->
                getSupplierByPhone(phone)?.let {
                    return Result.failure(Exception("Supplier with this phone number already exists"))
                }
            }
            
            // Check for duplicate GST number if provided
            supplier.gstNumber?.let { gst ->
                getSupplierByGstNumber(gst)?.let {
                    return Result.failure(Exception("Supplier with this GST number already exists"))
                }
            }
            
            val now = Clock.System.now()
            val supplierToInsert = supplier.copy(
                createdAt = now,
                updatedAt = now
            )
            
            val supplierId = supplierDao.insertSupplier(supplierToInsert)
            Result.success(supplierId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates an existing supplier with validation
     */
    suspend fun updateSupplier(supplier: Supplier): Result<Unit> {
        return try {
            // Check for duplicate name (excluding current supplier)
            getSupplierByName(supplier.supplierName)?.let { existing ->
                if (existing.supplierId != supplier.supplierId) {
                    return Result.failure(Exception("Supplier with this name already exists"))
                }
            }
            
            // Check for duplicate phone (excluding current supplier)
            supplier.supplierPhone?.let { phone ->
                getSupplierByPhone(phone)?.let { existing ->
                    if (existing.supplierId != supplier.supplierId) {
                        return Result.failure(Exception("Supplier with this phone number already exists"))
                    }
                }
            }
            
            // Check for duplicate GST (excluding current supplier)
            supplier.gstNumber?.let { gst ->
                getSupplierByGstNumber(gst)?.let { existing ->
                    if (existing.supplierId != supplier.supplierId) {
                        return Result.failure(Exception("Supplier with this GST number already exists"))
                    }
                }
            }
            
            val updatedSupplier = supplier.copy(updatedAt = Clock.System.now())
            supplierDao.updateSupplier(updatedSupplier)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates supplier outstanding balance (for purchase transactions)
     */
    suspend fun updateOutstandingBalance(supplierId: Long, amount: BigDecimal): Result<Unit> {
        return try {
            supplierDao.updateOutstandingBalance(supplierId, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deactivate supplier (soft delete)
     */
    suspend fun deactivateSupplier(supplierId: Long): Result<Unit> {
        return try {
            supplierDao.deactivateSupplier(supplierId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Activate supplier
     */
    suspend fun activateSupplier(supplierId: Long): Result<Unit> {
        return try {
            supplierDao.activateSupplier(supplierId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if supplier can be deleted (has no associated products)
     */
    suspend fun canDeleteSupplier(supplierId: Long): Boolean {
        // This would require a join query to check if supplier has products
        // For now, we'll always allow deactivation instead of deletion
        return false
    }
    
    /**
     * Get supplier analytics with product count
     */
    suspend fun getSuppliersWithProductCount() = supplierDao.getSuppliersWithProductCount()
    
    /**
     * Bulk import suppliers with validation
     */
    suspend fun importSuppliers(suppliers: List<Supplier>): Result<SupplierImportResult> {
        return try {
            val results = mutableListOf<SupplierImportDetails>()
            var successCount = 0
            var errorCount = 0
            
            suppliers.forEachIndexed { index, supplier ->
                val result = addSupplier(supplier)
                if (result.isSuccess) {
                    successCount++
                    results.add(SupplierImportDetails(index, supplier.supplierName, true, null))
                } else {
                    errorCount++
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    results.add(SupplierImportDetails(index, supplier.supplierName, false, errorMessage))
                }
            }
            
            Result.success(SupplierImportResult(successCount, errorCount, results))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Result data classes for supplier import
 */
data class SupplierImportResult(
    val successCount: Int,
    val errorCount: Int,
    val details: List<SupplierImportDetails>
)

data class SupplierImportDetails(
    val rowIndex: Int,
    val supplierName: String,
    val success: Boolean,
    val errorMessage: String?
)