package com.billme.app.data.repository

import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified database repository to centralize all database operations
 * Provides a single point of access for data operations across the app
 */
@Singleton
class DatabaseRepository @Inject constructor(
    private val database: BillMeDatabase
) {
    // Product operations
    suspend fun insertProduct(product: Product): Long {
        return database.productDao().insertProduct(product)
    }
    
    suspend fun updateProduct(product: Product) {
        database.productDao().updateProduct(product)
    }
    
    suspend fun deleteProduct(productId: Long): Boolean {
        return try {
            android.util.Log.d("DatabaseRepository", "Deleting product with ID: $productId")
            
            // Try to delete all IMEIs first (if any exist)
            try {
                val imeis = database.productIMEIDao().getIMEIsByProductIdSync(productId)
                android.util.Log.d("DatabaseRepository", "Found ${imeis.size} IMEIs to delete for product $productId")
                
                imeis.forEach { imei ->
                    try {
                        database.productIMEIDao().deleteIMEI(imei)
                        android.util.Log.d("DatabaseRepository", "Deleted IMEI: ${imei.imeiNumber}")
                    } catch (e: Exception) {
                        android.util.Log.w("DatabaseRepository", "Failed to delete IMEI ${imei.imeiNumber}: ${e.message}")
                        // Continue deleting other IMEIs even if one fails
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DatabaseRepository", "Error fetching IMEIs for product $productId: ${e.message}")
                // Continue with product deletion even if IMEI deletion fails
                // This handles products that don't have IMEIs (accessories, etc.)
            }
            
            // Delete the product
            val product = database.productDao().getProductById(productId)
            if (product != null) {
                database.productDao().deleteProduct(product)
                android.util.Log.d("DatabaseRepository", "Successfully deleted product: ${product.productName}")
                true
            } else {
                android.util.Log.e("DatabaseRepository", "Product $productId not found")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Error deleting product $productId: ${e.message}", e)
            false
        }
    }
    
    suspend fun deleteProductsBulk(productIds: List<Long>): Boolean {
        return try {
            productIds.forEach { productId ->
                deleteProduct(productId)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("DatabaseRepository", "Error bulk deleting products: ${e.message}")
            false
        }
    }
    
    fun getAllProducts(): Flow<List<Product>> {
        return database.productDao().getAllActiveProducts()
    }
    
    suspend fun getProductById(productId: Long): Product? {
        return database.productDao().getProductById(productId)
    }
    
    // IMEI operations
    suspend fun insertIMEI(imei: ProductIMEI): Long {
        return database.productIMEIDao().insertIMEI(imei)
    }
    
    suspend fun updateIMEI(imei: ProductIMEI) {
        database.productIMEIDao().updateIMEI(imei)
    }
    
    suspend fun deleteIMEI(imei: ProductIMEI) {
        database.productIMEIDao().deleteIMEI(imei)
    }
    
    fun getIMEIsByProductId(productId: Long): Flow<List<ProductIMEI>> {
        return database.productIMEIDao().getIMEIsByProductId(productId)
    }
    
    suspend fun getIMEIsByProductIdSync(productId: Long): List<ProductIMEI> {
        return database.productIMEIDao().getIMEIsByProductIdSync(productId)
    }
    
    // Customer operations
    suspend fun insertCustomer(customer: Customer): Long {
        return database.customerDao().insertCustomer(customer)
    }
    
    suspend fun updateCustomer(customer: Customer) {
        database.customerDao().updateCustomer(customer)
    }
    
    suspend fun deleteCustomer(customer: Customer) {
        database.customerDao().deleteCustomer(customer)
    }
    
    fun getAllCustomers(): Flow<List<Customer>> {
        return database.customerDao().getAllActiveCustomers()
    }
    
    suspend fun getCustomerById(customerId: Long): Customer? {
        return database.customerDao().getCustomerById(customerId)
    }
    
    suspend fun getCustomerByPhone(phone: String): Customer? {
        return database.customerDao().getCustomerByPhone(phone)
    }
    
    // Invoice operations
    suspend fun insertInvoice(invoice: Invoice): Long {
        return database.invoiceDao().insertInvoice(invoice)
    }
    
    suspend fun updateInvoice(invoice: Invoice) {
        database.invoiceDao().updateInvoice(invoice)
    }
    
    suspend fun deleteInvoice(invoice: Invoice) {
        database.invoiceDao().deleteInvoice(invoice)
    }
    
    fun getAllInvoices(): Flow<List<Invoice>> {
        return database.invoiceDao().getAllInvoices()
    }
    
    suspend fun getInvoiceById(invoiceId: Long): Invoice? {
        return database.invoiceDao().getInvoiceById(invoiceId)
    }
    
    // GST Configuration operations
    suspend fun insertGSTConfig(config: GSTConfiguration): Long {
        return database.gstConfigurationDao().insertConfiguration(config)
    }
    
    suspend fun updateGSTConfig(config: GSTConfiguration) {
        database.gstConfigurationDao().updateConfiguration(config)
    }
    
    fun getActiveGSTConfig(): Flow<List<GSTConfiguration>> {
        return database.gstConfigurationDao().getAllGSTConfigurations()
    }
    
    // Utility functions
    suspend fun getDatabaseSize(): Long {
        return try {
            val dbFile = database.openHelper.writableDatabase.path?.let { java.io.File(it) }
            dbFile?.length() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    fun getAllCategories(): Flow<List<String>> {
        return database.productDao().getAllCategories()
    }
}
