package com.billme.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.*
import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.ProductStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Dao
interface ProductDao {
    
    @Query("SELECT * FROM products WHERE is_active = 1 ORDER BY updated_at DESC")
    fun getAllActiveProducts(): Flow<List<Product>>
    
    @Query("SELECT * FROM products WHERE product_id = :productId")
    suspend fun getProductById(productId: Long): Product?
    
    @Query("SELECT * FROM products WHERE brand = :brand AND model = :model AND is_active = 1 LIMIT 1")
    suspend fun getProductByBrandAndModel(brand: String, model: String): Product?
    
    @Query("SELECT * FROM products WHERE imei1 = :imei OR imei2 = :imei")
    suspend fun getProductByImei(imei: String): Product?
    
    @Query("SELECT * FROM products WHERE barcode = :barcode AND is_active = 1")
    suspend fun getProductByBarcode(barcode: String): Product?
    
    @Query("""
        SELECT * FROM products 
        WHERE is_active = 1 
        AND (
            product_name LIKE '%' || :query || '%' OR 
            brand LIKE '%' || :query || '%' OR 
            model LIKE '%' || :query || '%' OR
            variant LIKE '%' || :query || '%' OR
            imei1 LIKE '%' || :query || '%' OR
            imei2 LIKE '%' || :query || '%'
        )
        ORDER BY product_name ASC
    """)
    fun searchProducts(query: String): Flow<List<Product>>
    
    @Query("SELECT * FROM products WHERE category = :category AND is_active = 1 ORDER BY product_name ASC")
    fun getProductsByCategory(category: String): Flow<List<Product>>
    
    @Query("SELECT * FROM products WHERE current_stock <= min_stock_level AND is_active = 1")
    fun getLowStockProducts(): Flow<List<Product>>
    
    @Query("SELECT DISTINCT category FROM products WHERE is_active = 1 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
    
    @Query("SELECT DISTINCT brand FROM products WHERE is_active = 1 ORDER BY brand ASC")
    fun getAllBrands(): Flow<List<String>>
    
    @Query("SELECT DISTINCT variant FROM products WHERE is_active = 1 AND variant IS NOT NULL ORDER BY variant ASC")
    fun getAllVariants(): Flow<List<String>>
    
    @Query("SELECT * FROM products WHERE supplier_id = :supplierId AND is_active = 1 ORDER BY product_name ASC")
    fun getProductsBySupplier(supplierId: Long): Flow<List<Product>>
    
    @Query("SELECT * FROM products WHERE product_status = :status AND is_active = 1 ORDER BY updated_at DESC")
    fun getProductsByStatus(status: ProductStatus): Flow<List<Product>>
    
    @Query("SELECT * FROM products WHERE warranty_expiry_date BETWEEN :startDate AND :endDate AND is_active = 1")
    fun getProductsWithWarrantyExpiring(startDate: LocalDate, endDate: LocalDate): Flow<List<Product>>
    
    @Query("SELECT * FROM products WHERE warranty_expiry_date < :currentDate AND is_active = 1")
    fun getProductsWithExpiredWarranty(currentDate: LocalDate): Flow<List<Product>>
    
    @Query("SELECT COUNT(*) FROM products WHERE is_active = 1")
    suspend fun getTotalProductCount(): Int
    
    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE current_stock <= min_stock_level AND is_active = 1")
    suspend fun getLowStockCount(): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE product_status = :status AND is_active = 1")
    suspend fun getProductCountByStatus(status: ProductStatus): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE supplier_id = :supplierId AND is_active = 1")
    suspend fun getProductCountBySupplier(supplierId: Long): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE warranty_expiry_date < :currentDate AND is_active = 1")
    suspend fun getExpiredWarrantyCount(currentDate: LocalDate): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE warranty_expiry_date BETWEEN :startDate AND :endDate AND is_active = 1")
    suspend fun getWarrantyExpiringCount(startDate: LocalDate, endDate: LocalDate): Int
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: Product): Long
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProducts(products: List<Product>): List<Long>
    
    @Update
    suspend fun updateProduct(product: Product)
    
    @Query("UPDATE products SET current_stock = current_stock - :quantity WHERE product_id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Int)
    
    @Query("UPDATE products SET current_stock = current_stock + :quantity WHERE product_id = :productId")
    suspend fun increaseStock(productId: Long, quantity: Int)
    
    @Query("UPDATE products SET current_stock = :stock WHERE product_id = :productId")
    suspend fun updateStock(productId: Long, stock: Int)
    
    /**
     * Sync product stock with available IMEI count
     * This should be called after IMEI insert/update/delete operations
     */
    @Query("""
        UPDATE products 
        SET current_stock = (
            SELECT COUNT(*) 
            FROM product_imeis 
            WHERE product_imeis.product_id = products.product_id 
            AND product_imeis.status = 'AVAILABLE'
        )
        WHERE product_id = :productId
    """)
    suspend fun syncStockWithAvailableIMEIs(productId: Long)
    
    /**
     * Sync all products' stock with their available IMEI counts
     */
    @Query("""
        UPDATE products 
        SET current_stock = (
            SELECT COUNT(*) 
            FROM product_imeis 
            WHERE product_imeis.product_id = products.product_id 
            AND product_imeis.status = 'AVAILABLE'
        )
        WHERE product_id IN (
            SELECT DISTINCT product_id FROM product_imeis
        )
    """)
    suspend fun syncAllProductStocksWithIMEIs()
    
    @Query("UPDATE products SET product_status = :status WHERE product_id = :productId")
    suspend fun updateProductStatus(productId: Long, status: ProductStatus)
    
    @Query("UPDATE products SET is_active = 0 WHERE product_id = :productId")
    suspend fun deactivateProduct(productId: Long)
    
    @Delete
    suspend fun deleteProduct(product: Product)
    
    @Query("DELETE FROM products WHERE is_active = 0")
    suspend fun deleteInactiveProducts()
    
    // Check if IMEI exists (for duplicate validation)
    @Query("SELECT COUNT(*) > 0 FROM products WHERE (imei1 = :imei OR imei2 = :imei) AND product_id != :excludeProductId")
    suspend fun isImeiExists(imei: String, excludeProductId: Long = -1): Boolean
    
    // Get products with pagination
    @Query("""
        SELECT * FROM products 
        WHERE is_active = 1 
        ORDER BY updated_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getProductsPaginated(limit: Int, offset: Int): List<Product>
    
    // Analytics methods for BusinessMetricsRepository
    @Query("SELECT COUNT(*) FROM products WHERE current_stock <= min_stock_level AND is_active = 1")
    suspend fun getLowStockItemCount(): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE current_stock = 0 AND is_active = 1")
    suspend fun getOutOfStockItemCount(): Int
    
    @Query("SELECT product_id, product_name, category, current_stock, cost_price, selling_price FROM products WHERE current_stock = 0 AND updated_at < :cutoffDate AND is_active = 1")
    suspend fun getDeadStockItems(cutoffDate: kotlinx.datetime.Instant): List<DeadStockItem>
    
    @Query("SELECT product_id, product_name, category, current_stock, selling_price FROM products WHERE current_stock > 0 AND is_active = 1 ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getFastMovingItems(limit: Int = 20): List<FastMovingItem>
    
    @Query("SELECT category, SUM(CAST(cost_price as DECIMAL) * current_stock) as costValue, SUM(CAST(selling_price as DECIMAL) * current_stock) as sellingValue FROM products WHERE is_active = 1 GROUP BY category")
    suspend fun getCategoryValuation(): List<CategoryValuation>
    
    @Query("SELECT SUM(CAST(cost_price as DECIMAL) * current_stock) FROM products WHERE is_active = 1")
    suspend fun getTotalStockValue(): BigDecimal
    
    @Query("SELECT SUM(current_stock) FROM products WHERE is_active = 1")
    suspend fun getTotalStockQuantity(): Int
    
    @Query("SELECT AVG(JULIANDAY('now') - JULIANDAY(created_at / 1000, 'unixepoch')) FROM products WHERE is_active = 1")
    suspend fun getAverageStockAge(): Int
    
    @Query("SELECT * FROM products WHERE current_stock > 0 AND is_active = 1")
    suspend fun getOutOfStockProducts(): List<Product>
    
    // Convenience methods for repository compatibility
    @Query("SELECT * FROM products WHERE product_id = :productId LIMIT 1")
    suspend fun getProductByIdSync(productId: Long): Product?
    
    @Query("""
        SELECT product_id, COUNT(*) as sales_count, SUM(quantity) as total_sold
        FROM transaction_line_items 
        WHERE product_id = :productId
        GROUP BY product_id
    """)
    suspend fun getProductSalesHistory(productId: Long): ProductSalesHistory?
}

/**
 * Data class for product sales history
 */
data class ProductSalesHistory(
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "sales_count") val salesCount: Int = 0,
    @ColumnInfo(name = "total_sold") val totalSold: BigDecimal = BigDecimal.ZERO
)/**
 * Data classes for product analytics
 */
data class DeadStockItem(
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "current_stock") val currentStock: Int,
    @ColumnInfo(name = "cost_price") val costPrice: BigDecimal,
    @ColumnInfo(name = "selling_price") val sellingPrice: BigDecimal
)

data class FastMovingItem(
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "current_stock") val currentStock: Int,
    @ColumnInfo(name = "selling_price") val sellingPrice: BigDecimal
)

data class CategoryValuation(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "costValue") val costValue: BigDecimal,
    @ColumnInfo(name = "sellingValue") val sellingValue: BigDecimal
)
