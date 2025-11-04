package com.billme.app.data.repository

import com.billme.app.core.util.ImeiValidator
import com.billme.app.data.local.dao.ProductDao
import com.billme.app.data.local.entity.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao
) {
    
    fun getAllActiveProducts(): Flow<List<Product>> = productDao.getAllActiveProducts()
    
    suspend fun getProductById(productId: Long): Product? = productDao.getProductById(productId)
    
    suspend fun getProductByImei(imei: String): Product? {
        val cleanImei = ImeiValidator.cleanImei(imei)
        return productDao.getProductByImei(cleanImei)
    }
    
    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getProductByBarcode(barcode)
    
    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)
    
    fun getProductsByCategory(category: String): Flow<List<Product>> = 
        productDao.getProductsByCategory(category)
    
    fun getLowStockProducts(): Flow<List<Product>> = productDao.getLowStockProducts()
    
    fun getAllCategories(): Flow<List<String>> = productDao.getAllCategories()
    
    fun getAllBrands(): Flow<List<String>> = productDao.getAllBrands()
    
    suspend fun getTotalProductCount(): Int = productDao.getTotalProductCount()
    
    suspend fun getLowStockCount(): Int = productDao.getLowStockCount()
    
    /**
     * Adds a new product with IMEI validation
     */
    suspend fun addProduct(product: Product): Result<Long> {
        return try {
            // Validate IMEI1
            val imei1Validation = ImeiValidator.validateImei(product.imei1)
            if (!imei1Validation.isValid) {
                return Result.failure(Exception("Invalid IMEI1: ${imei1Validation.errorMessage}"))
            }
            
            // Validate IMEI2 if provided
            product.imei2?.let { imei2 ->
                val imei2Validation = ImeiValidator.validateImei(imei2)
                if (!imei2Validation.isValid) {
                    return Result.failure(Exception("Invalid IMEI2: ${imei2Validation.errorMessage}"))
                }
            }
            
            // Check for duplicate IMEI1
            if (isImeiExists(imei1Validation.cleanImei!!)) {
                return Result.failure(Exception("IMEI1 already exists: ${product.imei1}"))
            }
            
            // Check for duplicate IMEI2
            product.imei2?.let { imei2 ->
                val cleanImei2 = ImeiValidator.cleanImei(imei2)
                if (isImeiExists(cleanImei2)) {
                    return Result.failure(Exception("IMEI2 already exists: $imei2"))
                }
            }
            
            val now = Clock.System.now()
            val productToInsert = product.copy(
                imei1 = imei1Validation.cleanImei,
                imei2 = product.imei2?.let { ImeiValidator.cleanImei(it) },
                createdAt = now,
                updatedAt = now
            )
            
            val productId = productDao.insertProduct(productToInsert)
            Result.success(productId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates an existing product with validation
     */
    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            // Validate IMEI1
            val imei1Validation = ImeiValidator.validateImei(product.imei1)
            if (!imei1Validation.isValid) {
                return Result.failure(Exception("Invalid IMEI1: ${imei1Validation.errorMessage}"))
            }
            
            // Validate IMEI2 if provided
            product.imei2?.let { imei2 ->
                val imei2Validation = ImeiValidator.validateImei(imei2)
                if (!imei2Validation.isValid) {
                    return Result.failure(Exception("Invalid IMEI2: ${imei2Validation.errorMessage}"))
                }
            }
            
            // Check for duplicate IMEI1 (excluding current product)
            if (productDao.isImeiExists(imei1Validation.cleanImei!!, product.productId)) {
                return Result.failure(Exception("IMEI1 already exists: ${product.imei1}"))
            }
            
            // Check for duplicate IMEI2 (excluding current product)
            product.imei2?.let { imei2 ->
                val cleanImei2 = ImeiValidator.cleanImei(imei2)
                if (productDao.isImeiExists(cleanImei2, product.productId)) {
                    return Result.failure(Exception("IMEI2 already exists: $imei2"))
                }
            }
            
            val productToUpdate = product.copy(
                imei1 = imei1Validation.cleanImei,
                imei2 = product.imei2?.let { ImeiValidator.cleanImei(it) },
                updatedAt = Clock.System.now()
            )
            
            productDao.updateProduct(productToUpdate)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reduces product stock (used during sales)
     */
    suspend fun reduceStock(productId: Long, quantity: Int = 1): Result<Unit> {
        return try {
            val product = productDao.getProductById(productId)
                ?: return Result.failure(Exception("Product not found"))
            
            if (product.currentStock < quantity) {
                return Result.failure(Exception("Insufficient stock. Available: ${product.currentStock}, Required: $quantity"))
            }
            
            productDao.reduceStock(productId, quantity)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Increases product stock (used for stock adjustments)
     */
    suspend fun increaseStock(productId: Long, quantity: Int): Result<Unit> {
        return try {
            productDao.increaseStock(productId, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Soft delete product (mark as inactive)
     */
    suspend fun deactivateProduct(productId: Long): Result<Unit> {
        return try {
            productDao.deactivateProduct(productId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if IMEI exists in the database
     */
    suspend fun isImeiExists(imei: String, excludeProductId: Long = -1): Boolean {
        val cleanImei = ImeiValidator.cleanImei(imei)
        return productDao.isImeiExists(cleanImei, excludeProductId)
    }
    
    /**
     * Get products with pagination
     */
    suspend fun getProductsPaginated(limit: Int, offset: Int): List<Product> =
        productDao.getProductsPaginated(limit, offset)
    
    /**
     * Bulk import products with validation
     */
    suspend fun importProducts(products: List<Product>): Result<ImportResult> {
        return try {
            val results = mutableListOf<ProductImportResult>()
            var successCount = 0
            var errorCount = 0
            
            products.forEachIndexed { index, product ->
                val result = addProduct(product)
                if (result.isSuccess) {
                    successCount++
                    results.add(ProductImportResult(index, product.productName, true, null))
                } else {
                    errorCount++
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    results.add(ProductImportResult(index, product.productName, false, errorMessage))
                }
            }
            
            Result.success(ImportResult(successCount, errorCount, results))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate next product ID for barcode scanning
     */
    suspend fun generateProductBarcode(productId: Long): String {
        return "PRD${productId.toString().padStart(8, '0')}"
    }
    
    /**
     * Get product by ID synchronously - convenience method for pricing engine
     */
    suspend fun getProductByIdSync(productId: Long): Product? = productDao.getProductByIdSync(productId)
}

data class ImportResult(
    val successCount: Int,
    val errorCount: Int,
    val details: List<ProductImportResult>
)

data class ProductImportResult(
    val rowIndex: Int,
    val productName: String,
    val success: Boolean,
    val errorMessage: String?
)