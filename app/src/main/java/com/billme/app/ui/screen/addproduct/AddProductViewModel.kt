package com.billme.app.ui.screen.addproduct

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.util.ErrorMessageHandler
import com.billme.app.core.util.ImeiHelper
import com.billme.app.core.ocr.processor.MobileShopInvoiceProcessor
import com.billme.app.core.ocr.processor.InvoiceProcessingResult
import com.billme.app.core.ocr.impl.IMEIDetector
import com.billme.app.core.ocr.impl.IMEIFieldSuggestion
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.math.BigDecimal
import javax.inject.Inject

data class IMEIPair(
    val imei1: String,
    val imei2: String? = null
)

data class AddProductUiState(
    val productName: String = "",
    val brand: String = "",
    val model: String = "",
    val color: String = "",
    val variant: String = "",
    val category: String = "",
    val imei1: String = "",
    val imei2: String = "",
    val additionalIMEIs: List<String> = emptyList(),
    val additionalIMEIPairs: List<IMEIPair> = emptyList(), // New field for IMEI pairs
    val hsnCode: String = "",
    val barcode: String = "",
    val mrp: String = "",
    val costPrice: String = "",
    val sellingPrice: String = "",
    val currentStock: String = "1",
    val minStockLevel: String = "1",
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showBarcodeScanner: Boolean = false,
    val showIMEIScanner: Boolean = false,
    val showOCRScanner: Boolean = false,
    val showBulkIMEIDialog: Boolean = false,
    val isCategoryDropdownExpanded: Boolean = false,
    val isHSNDropdownExpanded: Boolean = false,
    val availableCategories: List<String> = emptyList(),
    val availableHSNCodes: List<String> = emptyList(),
    val suggestedPrice: BigDecimal? = null,
    val productSaved: Boolean = false,
    val showExistingProductDialog: Boolean = false,
    val existingProduct: Product? = null,
    val isProcessingOCR: Boolean = false,
    val ocrConfidence: Float = 0f,
    val imei2Required: Boolean = false,
    val extractedProducts: List<ExtractedProductData> = emptyList(),
    val showProductSelectionDialog: Boolean = false
)

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val database: BillMeDatabase,
    private val errorMessageHandler: ErrorMessageHandler,
    private val invoiceProcessor: MobileShopInvoiceProcessor,
    private val imeiDetector: IMEIDetector
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
        loadCommonHSNCodes()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                // Default categories for mobile shop
                val defaultCategories = listOf(
                    "Smartphone",
                    "Feature Phone",
                    "Tablet",
                    "Smartwatch",
                    "Phone Case",
                    "Screen Protector",
                    "Charger",
                    "Power Bank",
                    "Earphones",
                    "Bluetooth Speaker",
                    "Mobile Accessories",
                    "Memory Card",
                    "SIM Card",
                    "Other"
                )
                
                database.productDao().getAllCategories().collect { dbCategories ->
                    // Combine default categories with database categories (remove duplicates)
                    val allCategories = (defaultCategories + dbCategories).distinct().sorted()
                    _uiState.update { it.copy(availableCategories = allCategories) }
                }
            } catch (e: Exception) {
                // If database fails, at least show default categories
                val defaultCategories = listOf(
                    "Smartphone",
                    "Feature Phone",
                    "Tablet",
                    "Smartwatch",
                    "Phone Case",
                    "Screen Protector",
                    "Charger",
                    "Power Bank",
                    "Earphones",
                    "Bluetooth Speaker",
                    "Mobile Accessories",
                    "Memory Card",
                    "SIM Card",
                    "Other"
                )
                _uiState.update { 
                    it.copy(
                        availableCategories = defaultCategories,
                        errorMessage = "Using default categories. Database error: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun loadCommonHSNCodes() {
        // Common HSN codes for mobile shop
        val commonHSN = listOf(
            "8517" to "Mobile Phones",
            "8518" to "Headphones/Earphones",
            "8529" to "Mobile Accessories",
            "8504" to "Chargers/Power Banks",
            "8523" to "Memory Cards",
            "8544" to "USB Cables",
            "9006" to "Camera Accessories",
            "3926" to "Mobile Covers/Cases",
            "8527" to "Bluetooth Speakers"
        )
        _uiState.update { 
            it.copy(availableHSNCodes = commonHSN.map { (code, desc) -> "$code - $desc" })
        }
    }
    
    fun updateProductName(name: String) {
        _uiState.update { it.copy(productName = name) }
    }
    
    fun updateBrand(brand: String) {
        _uiState.update { it.copy(brand = brand) }
    }
    
    fun updateModel(model: String) {
        _uiState.update { it.copy(model = model) }
    }
    
    fun updateColor(color: String) {
        _uiState.update { it.copy(color = color) }
    }
    
    fun updateVariant(variant: String) {
        _uiState.update { it.copy(variant = variant) }
    }
    
    fun updateCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }
    
    fun updateIMEI1(imei: String) {
        val cleaned = ImeiHelper.cleanInput(imei)
        _uiState.update { it.copy(imei1 = cleaned) }
        
        // Auto-validate and provide feedback
        if (cleaned.length == 15) {
            validateIMEI(cleaned, isFirstIMEI = true)
        }
    }
    
    fun updateIMEI2(imei: String) {
        val cleaned = ImeiHelper.cleanInput(imei)
        _uiState.update { it.copy(imei2 = cleaned) }
        
        // Auto-validate and provide feedback
        if (cleaned.length == 15) {
            validateIMEI(cleaned, isFirstIMEI = false)
        }
    }
    
    /**
     * Validate IMEI with enhanced checks
     */
    private fun validateIMEI(imei: String, isFirstIMEI: Boolean) {
        viewModelScope.launch {
            try {
                // Validate IMEI
                val result = ImeiHelper.validate(imei)
                
                if (!result.isValid) {
                    _uiState.update { 
                        it.copy(
                            errorMessage = "${if (isFirstIMEI) "IMEI 1" else "IMEI 2"}: ${result.errorMessage}"
                        )
                    }
                    clearMessages()
                    return@launch
                }
                
                // Check if dual IMEIs are compatible (if both present)
                val state = _uiState.value
                if (state.imei1.length == 15 && state.imei2.length == 15) {
                    val dualResult = ImeiHelper.validateDualPair(state.imei1, state.imei2)
                    
                    if (!dualResult.isValid) {
                        _uiState.update { 
                            it.copy(errorMessage = dualResult.errorMessage)
                        }
                        clearMessages()
                        return@launch
                    }
                    
                    if (dualResult.warning != null) {
                        _uiState.update { 
                            it.copy(errorMessage = "Warning: ${dualResult.warning}")
                        }
                        clearMessages()
                    }
                }
                
                // Check for duplicates in database
                val existingProduct = database.productDao().getProductByImei(imei)
                if (existingProduct != null) {
                    _uiState.update { 
                        it.copy(
                            errorMessage = "${if (isFirstIMEI) "IMEI 1" else "IMEI 2"} already exists in inventory (${existingProduct.productName})"
                        )
                    }
                    clearMessages()
                }
            } catch (e: Exception) {
                // Silent fail - don't block user input
            }
        }
    }
    
    fun addAdditionalIMEI(imei: String) {
        if (imei.length == 15 && imei.all { it.isDigit() }) {
            _uiState.update { 
                it.copy(additionalIMEIs = it.additionalIMEIs + imei)
            }
        }
    }
    
    fun addIMEIPair(imei1: String, imei2: String?) {
        _uiState.update {
            it.copy(additionalIMEIPairs = it.additionalIMEIPairs + IMEIPair(imei1, imei2))
        }
    }
    
    fun removeIMEIPair(index: Int) {
        _uiState.update {
            it.copy(additionalIMEIPairs = it.additionalIMEIPairs.filterIndexed { i, _ -> i != index })
        }
    }
    
    fun clearAdditionalIMEIPairs() {
        _uiState.update {
            it.copy(additionalIMEIPairs = emptyList())
        }
    }
    
    fun removeAdditionalIMEI(index: Int) {
        _uiState.update { 
            it.copy(additionalIMEIs = it.additionalIMEIs.filterIndexed { i, _ -> i != index })
        }
    }
    
    fun addMultipleIMEIs(imeis: List<String>) {
        _uiState.update { 
            it.copy(additionalIMEIs = it.additionalIMEIs + imeis)
        }
    }
    
    fun updateHSNCode(code: String) {
        _uiState.update { it.copy(hsnCode = code) }
    }
    
    fun updateBarcode(barcode: String) {
        _uiState.update { it.copy(barcode = barcode) }
    }
    
    fun updateMRP(mrp: String) {
        _uiState.update { it.copy(mrp = mrp) }
        calculateSuggestedPrice()
    }
    
    fun updateCostPrice(price: String) {
        _uiState.update { it.copy(costPrice = price) }
        calculateSuggestedPrice()
    }
    
    fun updateSellingPrice(price: String) {
        _uiState.update { it.copy(sellingPrice = price) }
    }
    
    fun updateStock(stock: String) {
        _uiState.update { it.copy(currentStock = stock) }
    }
    
    fun updateMinStock(stock: String) {
        _uiState.update { it.copy(minStockLevel = stock) }
    }
    
    fun updateImage(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri) }
    }
    
    fun showBarcodeScanner() {
        _uiState.update { it.copy(showBarcodeScanner = true) }
    }
    
    fun hideBarcodeScanner() {
        _uiState.update { it.copy(showBarcodeScanner = false) }
    }
    
    fun showIMEIScanner() {
        _uiState.update { it.copy(showIMEIScanner = true) }
    }
    
    fun hideIMEIScanner() {
        _uiState.update { it.copy(showIMEIScanner = false) }
    }
    
    fun showOCRScanner() {
        _uiState.update { it.copy(showOCRScanner = true) }
    }
    
    fun hideOCRScanner() {
        _uiState.update { it.copy(showOCRScanner = false) }
    }
    
    fun onBarcodeScanned(barcode: String) {
        updateBarcode(barcode)
        hideBarcodeScanner()
    }
    
    fun onIMEIScanned(imei: String) {
        // Clean and validate IMEI using ImeiHelper
        val result = ImeiHelper.validate(imei)
        
        if (!result.isValid) {
            _uiState.update { 
                it.copy(errorMessage = "Scanned IMEI: ${result.errorMessage}")
            }
            clearMessages()
            return
        }
        
        val cleaned = result.imei
        
        // Assign to appropriate field with smart logic
        when {
            _uiState.value.imei1.isBlank() -> {
                updateIMEI1(cleaned)
                _uiState.update { 
                    it.copy(successMessage = "IMEI 1 scanned successfully")
                }
                clearMessages()
            }
            _uiState.value.imei2.isBlank() -> {
                // Check if it's compatible with IMEI1 for dual IMEI
                val dualResult = ImeiHelper.validateDualPair(_uiState.value.imei1, cleaned)
                
                if (dualResult.isValid) {
                    updateIMEI2(cleaned)
                    if (dualResult.warning != null) {
                        _uiState.update { 
                            it.copy(errorMessage = "IMEI 2 scanned. ${dualResult.warning}")
                        }
                    } else {
                        _uiState.update { 
                            it.copy(successMessage = "Dual IMEI detected and validated ✓")
                        }
                    }
                    clearMessages()
                } else {
                    updateIMEI2(cleaned)
                    _uiState.update { 
                        it.copy(errorMessage = dualResult.errorMessage)
                    }
                    clearMessages()
                }
            }
            else -> {
                addAdditionalIMEI(cleaned)
                _uiState.update { 
                    it.copy(successMessage = "Added to additional IMEIs")
                }
                clearMessages()
            }
        }
    }
    
    fun onOCRDataExtracted(data: Map<String, String>) {
        // Extract data from OCR result
        data["brand"]?.let { updateBrand(it) }
        data["model"]?.let { updateModel(it) }
        data["variant"]?.let { updateVariant(it) }
        data["mrp"]?.let { updateMRP(it) }
        
        // Handle IMEI extraction - add to appropriate field
        data["imei"]?.let { imei ->
            when {
                _uiState.value.imei1.isBlank() -> updateIMEI1(imei)
                _uiState.value.imei2.isBlank() -> updateIMEI2(imei)
                else -> addAdditionalIMEI(imei)
            }
        }
        
        // Handle multiple IMEIs if OCR extracted a list
        data["imeis"]?.let { imeiString ->
            // Parse comma or newline separated IMEIs
            val imeis = imeiString.split(Regex("[,\\n]")).map { it.trim() }
                .filter { it.length == 15 && it.all { c -> c.isDigit() } }
            
            imeis.forEach { imei ->
                when {
                    _uiState.value.imei1.isBlank() -> updateIMEI1(imei)
                    _uiState.value.imei2.isBlank() -> updateIMEI2(imei)
                    else -> addAdditionalIMEI(imei)
                }
            }
        }
        
        hideOCRScanner()
    }
    
    private fun calculateSuggestedPrice() {
        val cost = _uiState.value.costPrice.toBigDecimalOrNull()
        val mrp = _uiState.value.mrp.toBigDecimalOrNull()
        
        if (cost != null && cost > BigDecimal.ZERO) {
            // Suggest 25% margin
            val suggested = cost.multiply(BigDecimal("1.25"))
                .setScale(2, java.math.RoundingMode.HALF_UP)
            
            val finalPrice = if (mrp != null && suggested > mrp) {
                mrp // Don't exceed MRP
            } else {
                suggested
            }
            
            _uiState.update { it.copy(suggestedPrice = finalPrice) }
        }
    }
    
    fun applySuggestedPrice() {
        _uiState.value.suggestedPrice?.let { price ->
            updateSellingPrice(price.toString())
        }
    }
    
    fun validateAndSave(onSuccess: () -> Unit) {
        val state = _uiState.value
        
        // Validation
        val errors = mutableListOf<String>()
        
        if (state.productName.isBlank()) errors.add("Product name is required")
        if (state.brand.isBlank()) errors.add("Brand is required")
        if (state.model.isBlank()) errors.add("Model is required")
        if (state.costPrice.isBlank() || state.costPrice.toBigDecimalOrNull() == null) {
            errors.add("Valid cost price is required")
        }
        if (state.sellingPrice.isBlank() || state.sellingPrice.toBigDecimalOrNull() == null) {
            errors.add("Valid selling price is required")
        }
        if (state.currentStock.isBlank() || state.currentStock.toIntOrNull() == null) {
            errors.add("Valid stock quantity is required")
        }
        
        // IMEI validation if provided
        if (state.imei1.isNotBlank()) {
            val result1 = ImeiHelper.validate(state.imei1)
            if (!result1.isValid) {
                errors.add("IMEI 1: ${result1.errorMessage}")
            }
        }
        
        if (state.imei2.isNotBlank()) {
            val result2 = ImeiHelper.validate(state.imei2)
            if (!result2.isValid) {
                errors.add("IMEI 2: ${result2.errorMessage}")
            } else if (state.imei1.isNotBlank()) {
                // Check dual IMEI compatibility
                val dualResult = ImeiHelper.validateDualPair(state.imei1, state.imei2)
                if (!dualResult.isValid) {
                    errors.add(dualResult.errorMessage ?: "Invalid dual IMEI pair")
                } else if (dualResult.warning != null) {
                    // Add as warning, not error
                    android.util.Log.w("AddProduct", "Dual IMEI warning: ${dualResult.warning}")
                }
            }
        }
        
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = errors.joinToString("\n")) }
            clearMessages()
            return
        }
        
        // Check if product already exists by brand + model + color + variant
        // ONLY if IMEI is provided (for mobile products)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Only check for duplicates if IMEI is provided
                // Many products like accessories don't have IMEI
                if (state.imei1.isNotBlank() && state.imei1 != "N/A") {
                    val existingProduct = database.productDao().getProductByBrandAndModel(
                        state.brand,
                        state.model
                    )
                    
                    // Check if exact match (brand + model + color + variant)
                    val isExactMatch = existingProduct != null &&
                        existingProduct.color.equals(state.color, ignoreCase = true) &&
                        existingProduct.variant.equals(state.variant, ignoreCase = true)
                    
                    if (isExactMatch && existingProduct != null) {
                        // Exact product exists - show dialog to add IMEI to existing product
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                showExistingProductDialog = true,
                                existingProduct = existingProduct
                            )
                        }
                        return@launch
                    }
                }
                
                // Product doesn't exist or is a different variant or no IMEI - create new product
                createNewProduct(state, onSuccess)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMessageHandler.getUserFriendlyMessage(e, "check product")
                    )
                }
                clearMessages()
            }
        }
    }
    
    private suspend fun createNewProduct(state: AddProductUiState, onSuccess: () -> Unit) {
        try {
            val product = Product(
                productId = 0,
                productName = state.productName,
                brand = state.brand,
                model = state.model,
                color = state.color.ifBlank { null },
                variant = state.variant.ifBlank { null },
                category = state.category.ifBlank { "Uncategorized" },
                imei1 = state.imei1.ifBlank { "N/A" },
                imei2 = state.imei2.ifBlank { null },
                barcode = state.barcode.ifBlank { null },
                mrp = state.mrp.toBigDecimalOrNull(),
                costPrice = state.costPrice.toBigDecimal(),
                sellingPrice = state.sellingPrice.toBigDecimal(),
                currentStock = state.currentStock.toInt(),
                minStockLevel = state.minStockLevel.toInt(),
                isActive = true,
                purchaseDate = Clock.System.now(),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            
            val productId = database.productDao().insertProduct(product)
            
            // Create list of all IMEIs to insert
            val imeisToInsert = mutableListOf<com.billme.app.data.local.entity.ProductIMEI>()
            
            // Add primary IMEI if provided
            if (state.imei1.isNotBlank() && state.imei1 != "N/A") {
                val productIMEI = com.billme.app.data.local.entity.ProductIMEI(
                    productId = productId,
                    imeiNumber = state.imei1,
                    imei2Number = if (state.imei2.isNotBlank() && state.imei2 != "N/A") state.imei2 else null,
                    status = com.billme.app.data.local.entity.IMEIStatus.AVAILABLE,
                    purchasePrice = state.costPrice.toBigDecimal(),
                    purchaseDate = Clock.System.now(),
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                imeisToInsert.add(productIMEI)
            }
            
            // Add all additional IMEIs as separate ProductIMEI entries
            state.additionalIMEIs.forEach { imeiNumber ->
                if (imeiNumber.isNotBlank()) {
                    val productIMEI = com.billme.app.data.local.entity.ProductIMEI(
                        productId = productId,
                        imeiNumber = imeiNumber,
                        imei2Number = null, // Additional IMEIs don't have IMEI2
                        status = com.billme.app.data.local.entity.IMEIStatus.AVAILABLE,
                        purchasePrice = state.costPrice.toBigDecimal(),
                        purchaseDate = Clock.System.now(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                    imeisToInsert.add(productIMEI)
                }
            }
            
            // Add all additional IMEI pairs as separate ProductIMEI entries
            state.additionalIMEIPairs.forEach { pair ->
                if (pair.imei1.isNotBlank() && pair.imei1 != "N/A") {
                    val productIMEI = com.billme.app.data.local.entity.ProductIMEI(
                        productId = productId,
                        imeiNumber = pair.imei1,
                        imei2Number = if (pair.imei2?.isNotBlank() == true && pair.imei2 != "N/A") pair.imei2 else null,
                        status = com.billme.app.data.local.entity.IMEIStatus.AVAILABLE,
                        purchasePrice = state.costPrice.toBigDecimal(),
                        purchaseDate = Clock.System.now(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                    imeisToInsert.add(productIMEI)
                }
            }
            
            // Bulk insert all IMEIs
            if (imeisToInsert.isNotEmpty()) {
                database.productIMEIDao().insertIMEIs(imeisToInsert)
            }
            
            val totalIMEIsAdded = imeisToInsert.size
            val successMsg = if (totalIMEIsAdded > 1) {
                "Product saved with $totalIMEIsAdded IMEI units"
            } else {
                ErrorMessageHandler.Messages.savedSuccessfully("Product")
            }
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    successMessage = successMsg,
                    productSaved = true
                )
            }
            
            clearMessages()
            onSuccess()
            
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = errorMessageHandler.getUserFriendlyMessage(e, "save product")
                )
            }
            clearMessages()
        }
    }
    
    fun dismissExistingProductDialog() {
        _uiState.update { 
            it.copy(
                showExistingProductDialog = false,
                existingProduct = null,
                isLoading = false
            )
        }
    }
    
    fun addIMEIToExistingProduct(onNavigateToAddIMEI: (Product) -> Unit) {
        val existingProduct = _uiState.value.existingProduct
        if (existingProduct != null) {
            dismissExistingProductDialog()
            onNavigateToAddIMEI(existingProduct)
        }
    }
    
    private fun clearMessages() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
        }
    }
    
    // Alias methods for compatibility
    fun setProductImage(uri: Uri) {
        updateImage(uri)
    }
    
    fun setProductName(name: String) {
        updateProductName(name)
    }
    
    fun setBrand(brand: String) {
        updateBrand(brand)
    }
    
    fun setModel(model: String) {
        updateModel(model)
    }
    
    fun setVariant(variant: String) {
        updateVariant(variant)
    }
    
    fun setCategory(category: String) {
        updateCategory(category)
    }
    
    fun setIMEI1(imei: String) {
        updateIMEI1(imei)
    }
    
    fun setIMEI2(imei: String) {
        updateIMEI2(imei)
    }
    
    fun setBarcode(barcode: String) {
        updateBarcode(barcode)
    }
    
    fun setHSNCode(code: String) {
        updateHSNCode(code)
    }
    
    fun setMRP(mrp: String) {
        updateMRP(mrp)
    }
    
    fun setCostPrice(price: String) {
        updateCostPrice(price)
    }
    
    fun setSellingPrice(price: String) {
        updateSellingPrice(price)
    }
    
    fun setStock(stock: String) {
        updateStock(stock)
    }
    
    fun setStockQuantity(stock: String) {
        updateStock(stock)
    }
    
    fun setMinStock(stock: String) {
        updateMinStock(stock)
    }
    
    fun toggleHSNDropdown() {
        _uiState.update { it.copy(isHSNDropdownExpanded = !it.isHSNDropdownExpanded) }
    }
    
    fun extractDataFromImage(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessingOCR = true, isLoading = true) }
                
                // Process invoice/bill with enhanced OCR
                when (val result = invoiceProcessor.processInvoice(uri)) {
                    is InvoiceProcessingResult.Success -> {
                        val invoiceData = result.invoiceData
                        val products = result.products
                        
                        // Check if multiple products detected
                        if (products.size > 1) {
                            // Multiple products - show selection dialog
                            val extractedProducts = products.map { product ->
                                // Parse variant to extract color and specs
                                val parsed = parseVariantString(product.variant)
                                
                                ExtractedProductData(
                                    serialNumber = product.serialNumber,
                                    productName = "${product.brand} ${product.model}".trim() + 
                                        (parsed.color?.let { " $it" } ?: "") +
                                        (parsed.variant?.let { " $it" } ?: ""),
                                    brand = product.brand,
                                    model = product.model,
                                    color = parsed.color,
                                    variant = parsed.variant,
                                    imei = product.imei,  // Each product has its own IMEI
                                    costPrice = product.rate,
                                    mrp = product.amount,
                                    quantity = product.quantity
                                )
                            }
                            
                            _uiState.update { 
                                it.copy(
                                    isProcessingOCR = false,
                                    isLoading = false,
                                    extractedProducts = extractedProducts,
                                    showProductSelectionDialog = true,
                                    ocrConfidence = invoiceData.confidence,
                                    successMessage = "Found ${products.size} products! Select one to add."
                                )
                            }
                        } else if (products.isNotEmpty()) {
                            // Single product - auto-fill directly
                            val product = products.first()
                            val parsed = parseVariantString(product.variant)
                            val extractedProduct = ExtractedProductData(
                                serialNumber = product.serialNumber,
                                productName = "${product.brand} ${product.model}".trim() + 
                                    (parsed.color?.let { " $it" } ?: "") +
                                    (parsed.variant?.let { " $it" } ?: ""),
                                brand = product.brand,
                                model = product.model,
                                color = parsed.color,
                                variant = parsed.variant,
                                imei = product.imei,
                                costPrice = product.rate,
                                mrp = product.amount,
                                quantity = product.quantity
                            )
                            fillProductData(extractedProduct, invoiceData.confidence)
                        } else {
                            // No products found
                            _uiState.update { 
                                it.copy(
                                    isProcessingOCR = false,
                                    isLoading = false,
                                    errorMessage = "No products detected in invoice. Please enter manually."
                                )
                            }
                        }
                        
                        clearMessages()
                    }
                    is InvoiceProcessingResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isProcessingOCR = false,
                                isLoading = false,
                                errorMessage = "OCR failed: ${result.message}. Please enter data manually."
                            )
                        }
                    }
                }
                
                clearMessages()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessingOCR = false,
                        isLoading = false,
                        errorMessage = "OCR processing failed: ${e.message}"
                    )
                }
                clearMessages()
            }
        }
    }
    
    /**
     * Fill product data from extracted product
     */
    private fun fillProductData(product: ExtractedProductData, confidence: Float) {
        // Use color and variant directly from ExtractedProductData
        
        // Update product details
        if (product.brand.isNotBlank()) {
            updateBrand(product.brand)
        }
        if (product.model.isNotBlank()) {
            updateModel(product.model)
        }
        if (product.color?.isNotBlank() == true) {
            updateColor(product.color)
        }
        if (product.variant?.isNotBlank() == true) {
            updateVariant(product.variant)
        }
        
        // Generate product name from brand + model (color and variant will be shown separately)
        val productName = "${product.brand} ${product.model}".trim()
        if (productName.isNotBlank()) {
            updateProductName(productName)
        }
        
        // Set prices
        if (product.costPrice > 0) {
            updateCostPrice(product.costPrice.toString())
        }
        if (product.mrp > 0) {
            updateMRP(product.mrp.toString())
        }
        
        // Set quantity
        if (product.quantity > 0) {
            updateStock(product.quantity.toInt().toString())
        }
        
        // Set IMEI for this specific product (IMEI1 only, as each product has one IMEI)
        if (!product.imei.isNullOrBlank()) {
            updateIMEI1(product.imei)
            _uiState.update { 
                it.copy(
                    imei2Required = false,  // Single product = single IMEI
                    isProcessingOCR = false,
                    isLoading = false,
                    ocrConfidence = confidence,
                    successMessage = "Product details and IMEI extracted! Add more details if needed."
                )
            }
        } else {
            _uiState.update { 
                it.copy(
                    imei2Required = false,
                    isProcessingOCR = false,
                    isLoading = false,
                    ocrConfidence = confidence,
                    successMessage = "Product details extracted! Please add IMEI manually."
                )
            }
        }
    }
    
    /**
     * Select a product from multi-product invoice
     */
    fun selectProductFromList(productData: ExtractedProductData) {
        // Fill form with selected product data
        fillProductData(productData, _uiState.value.ocrConfidence)
        dismissProductSelectionDialog()
    }
    
    /**
     * Add all products from multi-product invoice at once
     * This is MUCH faster - adds all products in one go!
     */
    fun addAllProducts(onSuccess: (Int) -> Unit) {
        val productsToAdd = _uiState.value.extractedProducts
        if (productsToAdd.isEmpty()) return
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val now = Clock.System.now()
                var successCount = 0
                val errors = mutableListOf<String>()
                
                for (productData in productsToAdd) {
                    try {
                        // Create product entity
                        val product = Product(
                            productName = productData.productName,
                            brand = productData.brand,
                            model = productData.model,
                            color = productData.color,
                            variant = productData.variant,
                            category = _uiState.value.category.ifBlank { "Mobile Phone" },
                            imei1 = productData.imei ?: "",
                            imei2 = null,
                            costPrice = productData.costPrice.toBigDecimal(),
                            sellingPrice = productData.mrp.toBigDecimal(),
                            currentStock = productData.quantity.toInt(),
                            minStockLevel = 1,
                            barcode = null,
                            description = "From invoice OCR",
                            purchaseDate = now,
                            createdAt = now,
                            updatedAt = now,
                            isActive = true
                        )
                        
                        // Check if IMEI already exists
                        if (productData.imei != null) {
                            val imeiExists = database.productDao().isImeiExists(productData.imei)
                            if (imeiExists) {
                                errors.add("${productData.brand} ${productData.variant}: IMEI already exists")
                                continue
                            }
                        }
                        
                        // Insert product
                        database.productDao().insertProduct(product)
                        successCount++
                        
                    } catch (e: Exception) {
                        errors.add("${productData.brand} ${productData.variant}: ${e.message}")
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = when {
                            successCount == productsToAdd.size -> 
                                "✓ All $successCount products added successfully!"
                            successCount > 0 -> 
                                "✓ Added $successCount/${productsToAdd.size} products. ${errors.size} failed."
                            else -> 
                                "Failed to add products: ${errors.joinToString(", ")}"
                        }
                    ) 
                }
                
                dismissProductSelectionDialog()
                
                if (successCount > 0) {
                    onSuccess(successCount)
                }
                
                clearMessages()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Bulk add failed: ${e.message}"
                    )
                }
                clearMessages()
            }
        }
    }
    
    fun dismissProductSelectionDialog() {
        _uiState.update { it.copy(showProductSelectionDialog = false) }
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    fun setSuccessMessage(message: String) {
        _uiState.update { it.copy(successMessage = message) }
    }
    
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun toggleBarcodeScanner() {
        val current = _uiState.value.showBarcodeScanner
        if (current) {
            hideBarcodeScanner()
        } else {
            showBarcodeScanner()
        }
    }
    
    fun toggleIMEIScanner() {
        val current = _uiState.value.showIMEIScanner
        if (current) {
            hideIMEIScanner()
        } else {
            showIMEIScanner()
        }
    }
    
    fun toggleMultiIMEIScanner() {
        _uiState.update { it.copy(showBulkIMEIDialog = !it.showBulkIMEIDialog) }
    }
    
    fun hideBulkIMEIDialog() {
        _uiState.update { it.copy(showBulkIMEIDialog = false) }
    }
    
    fun onBulkIMEIsAdded(imeis: List<String>) {
        if (imeis.isEmpty()) {
            hideBulkIMEIDialog()
            return
        }
        
        // First two IMEIs go into IMEI1 and IMEI2 fields
        val imei1Value = if (imeis.isNotEmpty()) imeis[0] else ""
        val imei2Value = if (imeis.size > 1) imeis[1] else ""
        
        // Remaining IMEIs are paired up (every 2 IMEIs = 1 pair)
        val additionalPairs = mutableListOf<IMEIPair>()
        var i = 2 // Start from index 2 (after first two IMEIs)
        
        while (i < imeis.size) {
            val primaryImei = imeis[i]
            val secondaryImei = if (i + 1 < imeis.size) imeis[i + 1] else null
            
            additionalPairs.add(IMEIPair(imei1 = primaryImei, imei2 = secondaryImei))
            
            // Move by 2 if we have a pair, or by 1 if only single IMEI left
            i += if (secondaryImei != null) 2 else 1
        }
        
        _uiState.update { 
            it.copy(
                imei1 = imei1Value,
                imei2 = imei2Value,
                additionalIMEIPairs = additionalPairs,
                additionalIMEIs = emptyList(), // Clear old single IMEI list
                successMessage = "Added ${imeis.size} IMEIs (${additionalPairs.size + 1} pairs) successfully"
            )
        }
        
        hideBulkIMEIDialog()
        clearMessages()
    }
    
    fun clearAdditionalIMEIs() {
        _uiState.update { 
            it.copy(
                additionalIMEIs = emptyList(),
                additionalIMEIPairs = emptyList()
            )
        }
    }
    
    fun removeAdditionalIMEIPair(index: Int) {
        _uiState.update { 
            it.copy(additionalIMEIPairs = it.additionalIMEIPairs.filterIndexed { i, _ -> i != index })
        }
    }
    
    fun toggleCategoryDropdown() {
        _uiState.update { it.copy(isCategoryDropdownExpanded = !it.isCategoryDropdownExpanded) }
    }
    
    fun clearForm() {
        _uiState.value = AddProductUiState(
            availableCategories = _uiState.value.availableCategories,
            availableHSNCodes = _uiState.value.availableHSNCodes
        )
    }
}
