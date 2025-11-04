package com.billme.app.ui.screen.inventory

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.service.BulkImportService
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.ProductIMEI
import com.billme.app.data.local.entity.ProductWithIMEIs
import com.billme.app.data.local.entity.StockAdjustment
import com.billme.app.data.local.entity.StockAdjustmentReason
import com.billme.app.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val selectedCategory: String = "All",
    val categories: List<String> = emptyList(),
    val searchQuery: String = "",
    val sortBy: SortOption = SortOption.NAME,
    val showLowStockOnly: Boolean = false,
    val isLoading: Boolean = false,
    val totalProducts: Int = 0,
    val lowStockCount: Int = 0,
    val totalValue: Double = 0.0,
    val importProgress: BulkImportProgress? = null,
    val importResult: BulkImportResult? = null,
    val expandedProductId: Long? = null,
    val productIMEIs: Map<Long, List<ProductIMEI>> = emptyMap(),
    val isSelectionMode: Boolean = false,
    val selectedProductIds: Set<Long> = emptySet(),
    val errorMessage: String? = null
)

enum class SortOption {
    NAME, STOCK, PRICE, BRAND, RECENTLY_ADDED
}

@OptIn(FlowPreview::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BillMeDatabase,
    private val databaseRepository: com.billme.app.data.repository.DatabaseRepository
    // TODO: Add bulkImportService when Room compilation issue is resolved
    // private val bulkImportService: BulkImportService
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow("All")
    private val _sortBy = MutableStateFlow(SortOption.NAME)
    private val _showLowStockOnly = MutableStateFlow(false)
    private val _importResult = MutableStateFlow<BulkImportResult?>(null)
    private val _expandedProductId = MutableStateFlow<Long?>(null)
    private val _productIMEIs = MutableStateFlow<Map<Long, List<ProductIMEI>>>(emptyMap())
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedProductIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _errorState = MutableStateFlow<String?>(null)
    
    private val allProducts: StateFlow<List<Product>> = try {
        database.productDao()
            .getAllActiveProducts()
            .catch { e: Throwable -> 
                _errorState.value = "Database error: ${e.message}"
                emit(emptyList<Product>())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } catch (e: Exception) {
        _errorState.value = "Failed to initialize: ${e.message}"
        MutableStateFlow<List<Product>>(emptyList()).asStateFlow()
    }
    
    private val categories: StateFlow<List<String>> = try {
        database.productDao()
            .getAllCategories()
            .catch { e: Throwable ->
                _errorState.value = "Database error: ${e.message}"
                emit(emptyList<String>())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } catch (e: Exception) {
        _errorState.value = "Failed to load categories: ${e.message}"
        MutableStateFlow<List<String>>(emptyList()).asStateFlow()
    }
    
    val importProgress: StateFlow<BulkImportProgress> = MutableStateFlow(
        BulkImportProgress(ImportPhase.IDLE, 0, 0)
    ).asStateFlow()
    
    init {
        // Sync all product stocks with available IMEIs on initialization
        viewModelScope.launch {
            try {
                database.productDao().syncAllProductStocksWithIMEIs()
            } catch (e: Exception) {
                // Silent fail - stock will sync on next IMEI operation
            }
        }
    }
    /*  bulkImportService.importProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BulkImportProgress(ImportPhase.IDLE, 0, 0)
        ) */
    
    val uiState: StateFlow<InventoryUiState> = combine(
        allProducts,
        categories,
        _searchQuery.debounce(300),
        _selectedCategory,
        _sortBy,
        _showLowStockOnly,
        importProgress,
        _importResult,
        _expandedProductId,
        _productIMEIs,
        _isSelectionMode,
        _selectedProductIds,
        _errorState
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val products = flows[0] as? List<Product> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val cats = flows[1] as? List<String> ?: emptyList()
        val query = flows[2] as? String ?: ""
        val category = flows[3] as? String ?: ""
        val sortBy = flows[4] as? SortOption ?: SortOption.NAME
        val lowStockOnly = flows[5] as? Boolean ?: false
        val importProg = flows[6] as? BulkImportProgress ?: BulkImportProgress(
            phase = ImportPhase.IDLE,
            processedRows = 0,
            totalRows = 0
        )
        val importRes = flows[7] as? BulkImportResult?
        val expandedId = flows[8] as? Long
        @Suppress("UNCHECKED_CAST")
        val imeis = flows[9] as? Map<Long, List<ProductIMEI>> ?: emptyMap()
        val selectionMode = flows[10] as? Boolean ?: false
        @Suppress("UNCHECKED_CAST")
        val selectedIds = flows[11] as? Set<Long> ?: emptySet()
        val errorMsg = flows[12] as? String
        
        var filtered = products
        
        // Apply search filter
        if (query.isNotBlank()) {
            filtered = filtered.filter { product ->
                product.productName.contains(query, ignoreCase = true) ||
                product.brand.contains(query, ignoreCase = true) ||
                product.model.contains(query, ignoreCase = true) ||
                product.imei1.contains(query, ignoreCase = true) ||
                product.barcode?.contains(query) == true
            }
        }
        
        // Apply category filter
        if (category != "All") {
            filtered = filtered.filter { it.category == category }
        }
        
        // Apply low stock filter
        if (lowStockOnly) {
            filtered = filtered.filter { it.isLowStock }
        }
        
        // Apply sorting
        filtered = when (sortBy) {
            SortOption.NAME -> filtered.sortedBy { it.productName }
            SortOption.STOCK -> filtered.sortedBy { it.currentStock }
            SortOption.PRICE -> filtered.sortedByDescending { it.sellingPrice }
            SortOption.BRAND -> filtered.sortedBy { it.brand }
            SortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.createdAt }
        }
        
        val totalValue = products.sumOf { 
            (it.sellingPrice * it.currentStock.toBigDecimal()).toDouble()
        }
        
        val lowStockCount = products.count { it.isLowStock }
        
        InventoryUiState(
            products = products,
            filteredProducts = filtered,
            selectedCategory = category,
            categories = listOf("All") + cats,
            searchQuery = query,
            sortBy = sortBy,
            showLowStockOnly = lowStockOnly,
            totalProducts = products.size,
            lowStockCount = lowStockCount,
            totalValue = totalValue,
            importProgress = if (importProg.phase != ImportPhase.IDLE) importProg else null,
            importResult = importRes,
            expandedProductId = expandedId,
            productIMEIs = imeis,
            isSelectionMode = selectionMode,
            selectedProductIds = selectedIds,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState()
    )
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }
    
    fun onSortByChanged(sortOption: SortOption) {
        _sortBy.value = sortOption
    }
    
    fun toggleLowStockFilter() {
        _showLowStockOnly.value = !_showLowStockOnly.value
    }
    
    /**
     * Adjust product stock with reason tracking
     */
    fun updateProductStock(
        product: Product,
        newStock: Int,
        reason: StockAdjustmentReason,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                // Update product stock directly
                database.productDao().updateStock(product.productId, newStock)
                
                // Update the updatedAt timestamp
                val updatedProduct = product.copy(
                    currentStock = newStock,
                    updatedAt = Clock.System.now()
                )
                database.productDao().updateProduct(updatedProduct)
                
                // Record stock adjustment
                val adjustment = StockAdjustment(
                    productId = product.productId,
                    previousStock = product.currentStock,
                    newStock = newStock,
                    adjustmentQuantity = newStock - product.currentStock,
                    reason = reason,
                    notes = notes.ifBlank { null },
                    adjustedBy = "User", // TODO: Get from user session
                    adjustmentDate = Clock.System.now()
                )
                database.stockAdjustmentDao().insertAdjustment(adjustment)
                
                // Force reload of products to ensure UI updates
                android.util.Log.d("InventoryViewModel", "Stock updated for product ${product.productId} from ${product.currentStock} to $newStock")
                
            } catch (e: Exception) {
                android.util.Log.e("InventoryViewModel", "Failed to update stock", e)
                _errorState.value = "Failed to update stock: ${e.message}"
            }
        }
    }
    
    /**
     * Bulk import from CSV - TODO: Enable when BulkImportService is available
     */
    fun importFromCSV(uri: Uri, config: BulkImportConfig = BulkImportConfig()) {
        /* viewModelScope.launch {
            try {
                val parseResult = bulkImportService.parseCSVFile(uri, config)
                
                if (parseResult.validCount > 0) {
                    val importResult = bulkImportService.importProducts(
                        parseResult.validProducts,
                        config
                    )
                    _importResult.value = importResult
                } else {
                    _importResult.value = BulkImportResult(
                        success = false,
                        importedCount = 0,
                        skippedCount = 0,
                        errorCount = parseResult.errorCount,
                        duration = 0,
                        errors = parseResult.invalidProducts,
                        warnings = emptyList()
                    )
                }
            } catch (e: Exception) {
                _importResult.value = BulkImportResult(
                    success = false,
                    importedCount = 0,
                    skippedCount = 0,
                    errorCount = 1,
                    duration = 0,
                    errors = listOf(
                        ProductImportError(0, emptyMap(), listOf(e.message ?: "Import failed"))
                    ),
                    warnings = emptyList()
                )
            }
        } */
    }
    
    /**
     * Generate CSV template - TODO: Enable when BulkImportService is available
     */
    fun generateCSVTemplate(): String {
        return "" // bulkImportService.generateCSVTemplate()
    }
    
    /**
     * Export inventory to CSV
     */
    fun exportToCSV(products: List<Product> = uiState.value.filteredProducts): File? {
        return try {
            val fileName = "inventory_export_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.append("Product Name,Brand,Model,Variant,Category,IMEI1,IMEI2,")
                writer.append("MRP,Cost Price,Selling Price,Stock,Min Stock,Barcode,Status\n")
                
                // Write data
                products.forEach { product ->
                    writer.append("\"${product.productName}\",")
                    writer.append("\"${product.brand}\",")
                    writer.append("\"${product.model}\",")
                    writer.append("\"${product.variant ?: ""}\",")
                    writer.append("\"${product.category}\",")
                    writer.append("${product.imei1},")
                    writer.append("${product.imei2 ?: ""},")
                    writer.append("${product.mrp ?: ""},")
                    writer.append("${product.costPrice},")
                    writer.append("${product.sellingPrice},")
                    writer.append("${product.currentStock},")
                    writer.append("${product.minStockLevel},")
                    writer.append("${product.barcode ?: ""},")
                    writer.append("${product.productStatus}\n")
                }
            }
            
            file
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Clear import result
     */
    fun clearImportResult() {
        _importResult.value = null
    }
    
    fun deactivateProduct(productId: Long) {
        viewModelScope.launch {
            database.productDao().deactivateProduct(productId)
        }
    }
    
    /**
     * Update selling price for a product
     */
    fun updateSellingPrice(productId: Long, newPrice: java.math.BigDecimal) {
        viewModelScope.launch {
            try {
                val product = database.productDao().getProductById(productId)
                if (product != null) {
                    val updatedProduct = product.copy(
                        sellingPrice = newPrice,
                        updatedAt = Clock.System.now()
                    )
                    database.productDao().updateProduct(updatedProduct)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Toggle product expansion to show/hide IMEIs
     * Fixed: Always load IMEIs when expanding to ensure fresh data
     */
    fun toggleProductExpansion(productId: Long) {
        viewModelScope.launch {
            val currentExpanded = _expandedProductId.value
            if (currentExpanded == productId) {
                // Collapse
                _expandedProductId.value = null
            } else {
                // Always load fresh IMEI data when expanding
                try {
                    val imeis = database.productIMEIDao().getIMEIsByProductIdSync(productId)
                    _productIMEIs.value = _productIMEIs.value + (productId to imeis)
                    _expandedProductId.value = productId
                } catch (e: Exception) {
                    android.util.Log.e("InventoryViewModel", "Failed to load IMEIs for product $productId", e)
                    // Still expand but with empty IMEI list
                    _productIMEIs.value = _productIMEIs.value + (productId to emptyList())
                    _expandedProductId.value = productId
                }
            }
        }
    }
    
    /**
     * Load IMEIs for a product
     */
    fun loadIMEIsForProduct(productId: Long) {
        viewModelScope.launch {
            val imeis = database.productIMEIDao().getIMEIsByProductIdSync(productId)
            _productIMEIs.value = _productIMEIs.value + (productId to imeis)
        }
    }
    
    /**
     * Add new IMEI to product
     */
    fun addIMEIToProduct(
        productId: Long,
        imeiNumber: String,
        imei2Number: String? = null,
        serialNumber: String? = null,
        purchasePrice: java.math.BigDecimal,
        boxNumber: String? = null,
        warrantyCardNumber: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            try {
                val now = Clock.System.now()
                val newIMEI = ProductIMEI(
                    productId = productId,
                    imeiNumber = imeiNumber,
                    imei2Number = imei2Number,
                    serialNumber = serialNumber,
                    purchaseDate = now,
                    purchasePrice = purchasePrice,
                    boxNumber = boxNumber,
                    warrantyCardNumber = warrantyCardNumber,
                    notes = notes,
                    createdAt = now,
                    updatedAt = now
                )
                database.productIMEIDao().insertIMEI(newIMEI)
                
                // Sync product stock with available IMEI count
                database.productDao().syncStockWithAvailableIMEIs(productId)
                
                // Reload IMEIs for this product
                loadIMEIsForProduct(productId)
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error - maybe IMEI already exists
            }
        }
    }
    
    /**
     * Add multiple IMEIs to product at once (bulk scan)
     */
    fun addBulkIMEIsToProduct(
        productId: Long,
        imeiPairs: List<Pair<String, String?>>,
        defaultPurchasePrice: java.math.BigDecimal,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val now = Clock.System.now()
                var successCount = 0
                
                imeiPairs.forEach { (imei1, imei2) ->
                    try {
                        val newIMEI = ProductIMEI(
                            productId = productId,
                            imeiNumber = imei1,
                            imei2Number = imei2,
                            serialNumber = null,
                            purchaseDate = now,
                            purchasePrice = defaultPurchasePrice,
                            boxNumber = null,
                            warrantyCardNumber = null,
                            notes = null,
                            createdAt = now,
                            updatedAt = now
                        )
                        database.productIMEIDao().insertIMEI(newIMEI)
                        successCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue with next IMEI even if one fails
                    }
                }
                
                // Sync product stock with available IMEI count
                database.productDao().syncStockWithAvailableIMEIs(productId)
                
                // Reload IMEIs for this product
                loadIMEIsForProduct(productId)
                
                onComplete(successCount)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(0)
            }
        }
    }
    
    /**
     * Update IMEI details
     */
    fun updateIMEI(imei: ProductIMEI) {
        viewModelScope.launch {
            try {
                val updated = imei.copy(updatedAt = Clock.System.now())
                database.productIMEIDao().updateIMEI(updated)
                
                // Sync product stock with available IMEI count
                database.productDao().syncStockWithAvailableIMEIs(imei.productId)
                
                loadIMEIsForProduct(imei.productId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Delete IMEI
     */
    fun deleteIMEI(imei: ProductIMEI) {
        viewModelScope.launch {
            try {
                database.productIMEIDao().deleteIMEI(imei)
                loadIMEIsForProduct(imei.productId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Delete product and all its IMEIs with proper error handling
     * Fixed: Added proper error feedback and state update
     */
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                val success = databaseRepository.deleteProduct(product.productId)
                if (!success) {
                    _errorState.value = "Failed to delete product"
                    android.util.Log.e("InventoryViewModel", "Failed to delete product: ${product.productId}")
                }
                // Clear expanded state if deleting current expanded product
                if (_expandedProductId.value == product.productId) {
                    _expandedProductId.value = null
                }
                // Remove from IMEI map
                _productIMEIs.value = _productIMEIs.value - product.productId
            } catch (e: Exception) {
                _errorState.value = "Error deleting product: ${e.message}"
                android.util.Log.e("InventoryViewModel", "Exception deleting product: ${product.productId}", e)
            }
        }
    }
    
    /**
     * Delete multiple products in bulk
     * Uses DatabaseRepository for centralized database operations
     */
    fun deleteProductsBulk(productIds: List<Long>) {
        viewModelScope.launch {
            val success = databaseRepository.deleteProductsBulk(productIds)
            if (!success) {
                android.util.Log.e("InventoryViewModel", "Failed to bulk delete products")
            } else {
                // Exit selection mode after successful deletion
                exitSelectionMode()
            }
        }
    }
    
    // Selection mode functions
    fun enterSelectionMode() {
        _isSelectionMode.value = true
        _selectedProductIds.value = emptySet()
    }
    
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedProductIds.value = emptySet()
    }
    
    fun toggleProductSelection(productId: Long) {
        val currentSelection = _selectedProductIds.value
        _selectedProductIds.value = if (currentSelection.contains(productId)) {
            currentSelection - productId
        } else {
            currentSelection + productId
        }
    }
    
    fun selectAllProducts() {
        val allIds = uiState.value.filteredProducts.map { it.productId }.toSet()
        _selectedProductIds.value = allIds
    }
    
    fun clearSelection() {
        _selectedProductIds.value = emptySet()
    }
    
    fun deleteSelectedProducts() {
        val selected = _selectedProductIds.value.toList()
        if (selected.isNotEmpty()) {
            deleteProductsBulk(selected)
        }
    }

    
    /**
     * Update product details (name, brand, model, pricing, stock, etc.)
     */
    fun updateProduct(
        productId: Long,
        productName: String,
        brand: String,
        model: String,
        color: String?,
        variant: String?,
        category: String,
        costPrice: java.math.BigDecimal,
        sellingPrice: java.math.BigDecimal,
        currentStock: Int,
        minStockLevel: Int,
        description: String?
    ) {
        viewModelScope.launch {
            try {
                val product = database.productDao().getProductById(productId)
                if (product != null) {
                    val updated = product.copy(
                        productName = productName,
                        brand = brand,
                        model = model,
                        color = color,
                        variant = variant,
                        category = category,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        currentStock = currentStock,
                        minStockLevel = minStockLevel,
                        description = description,
                        updatedAt = Clock.System.now()
                    )
                    database.productDao().updateProduct(updated)
                    android.util.Log.d("InventoryViewModel", "Product updated: $productId, stock: $currentStock")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
