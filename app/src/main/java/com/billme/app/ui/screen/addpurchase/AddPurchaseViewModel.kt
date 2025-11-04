package com.billme.app.ui.screen.addpurchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.math.BigDecimal
import javax.inject.Inject

data class AddPurchaseUiState(
    val productName: String = "",
    val brand: String = "",
    val model: String = "",
    val category: String = "",
    val imei1: String = "",
    val imei2: String = "",
    val barcode: String = "",
    val costPrice: String = "",
    val sellingPrice: String = "",
    val currentStock: String = "1",
    val minStockLevel: String = "1",
    val description: String = "",
    
    val existingCategories: List<String> = emptyList(),
    val existingBrands: List<String> = emptyList(),
    
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    
    val profitAmount: BigDecimal = BigDecimal.ZERO,
    val profitPercentage: Double = 0.0,
    
    val imei1Error: String? = null,
    val imei2Error: String? = null,
    val costPriceError: String? = null,
    val sellingPriceError: String? = null
) {
    val isValid: Boolean
        get() = productName.isNotBlank() &&
                brand.isNotBlank() &&
                model.isNotBlank() &&
                category.isNotBlank() &&
                imei1.isNotBlank() &&
                costPrice.isNotBlank() &&
                sellingPrice.isNotBlank() &&
                currentStock.isNotBlank() &&
                imei1Error == null &&
                imei2Error == null &&
                costPriceError == null &&
                sellingPriceError == null
}

@HiltViewModel
class AddPurchaseViewModel @Inject constructor(
    private val database: BillMeDatabase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddPurchaseUiState())
    val uiState: StateFlow<AddPurchaseUiState> = _uiState.asStateFlow()
    
    init {
        loadExistingData()
    }
    
    private fun loadExistingData() {
        viewModelScope.launch {
            database.productDao().getAllCategories().collect { categories ->
                _uiState.update { it.copy(existingCategories = categories) }
            }
        }
        
        viewModelScope.launch {
            database.productDao().getAllBrands().collect { brands ->
                _uiState.update { it.copy(existingBrands = brands) }
            }
        }
    }
    
    fun onProductNameChange(value: String) {
        _uiState.update { it.copy(productName = value, errorMessage = null) }
    }
    
    fun onBrandChange(value: String) {
        _uiState.update { it.copy(brand = value, errorMessage = null) }
    }
    
    fun onModelChange(value: String) {
        _uiState.update { it.copy(model = value, errorMessage = null) }
    }
    
    fun onCategoryChange(value: String) {
        _uiState.update { it.copy(category = value, errorMessage = null) }
    }
    
    fun onImei1Change(value: String) {
        val trimmed = value.trim()
        _uiState.update { 
            it.copy(
                imei1 = trimmed,
                imei1Error = validateImei(trimmed),
                errorMessage = null
            ) 
        }
        
        // Check if IMEI already exists
        if (trimmed.isNotBlank() && validateImei(trimmed) == null) {
            viewModelScope.launch {
                val exists = database.productDao().isImeiExists(trimmed)
                if (exists) {
                    _uiState.update { 
                        it.copy(imei1Error = "IMEI already exists in inventory") 
                    }
                }
            }
        }
    }
    
    fun onImei2Change(value: String) {
        val trimmed = value.trim()
        _uiState.update { 
            it.copy(
                imei2 = trimmed,
                imei2Error = if (trimmed.isNotBlank()) validateImei(trimmed) else null,
                errorMessage = null
            ) 
        }
        
        // Check if IMEI already exists
        if (trimmed.isNotBlank() && validateImei(trimmed) == null) {
            viewModelScope.launch {
                val exists = database.productDao().isImeiExists(trimmed)
                if (exists) {
                    _uiState.update { 
                        it.copy(imei2Error = "IMEI already exists in inventory") 
                    }
                }
            }
        }
    }
    
    fun onBarcodeChange(value: String) {
        _uiState.update { it.copy(barcode = value.trim(), errorMessage = null) }
    }
    
    fun onCostPriceChange(value: String) {
        _uiState.update { 
            it.copy(
                costPrice = value,
                costPriceError = validatePrice(value),
                errorMessage = null
            ) 
        }
        calculateProfit()
    }
    
    fun onSellingPriceChange(value: String) {
        _uiState.update { 
            it.copy(
                sellingPrice = value,
                sellingPriceError = validatePrice(value),
                errorMessage = null
            ) 
        }
        calculateProfit()
    }
    
    fun onCurrentStockChange(value: String) {
        if (value.isEmpty() || value.toIntOrNull() != null) {
            _uiState.update { it.copy(currentStock = value, errorMessage = null) }
        }
    }
    
    fun onMinStockLevelChange(value: String) {
        if (value.isEmpty() || value.toIntOrNull() != null) {
            _uiState.update { it.copy(minStockLevel = value, errorMessage = null) }
        }
    }
    
    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }
    
    private fun validateImei(imei: String): String? {
        return when {
            imei.length != 15 -> "IMEI must be exactly 15 digits"
            !imei.all { it.isDigit() } -> "IMEI must contain only digits"
            else -> null
        }
    }
    
    private fun validatePrice(price: String): String? {
        return when {
            price.isBlank() -> "Price is required"
            price.toBigDecimalOrNull() == null -> "Invalid price format"
            price.toBigDecimal() <= BigDecimal.ZERO -> "Price must be greater than 0"
            else -> null
        }
    }
    
    private fun calculateProfit() {
        val state = _uiState.value
        val cost = state.costPrice.toBigDecimalOrNull()
        val selling = state.sellingPrice.toBigDecimalOrNull()
        
        if (cost != null && selling != null && cost > BigDecimal.ZERO && selling > BigDecimal.ZERO) {
            val profit = selling - cost
            // Calculate profit percentage based on cost price: (profit / cost) * 100
            val profitPercent = (profit.divide(cost, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)).toDouble()
            
            _uiState.update { 
                it.copy(
                    profitAmount = profit,
                    profitPercentage = profitPercent
                ) 
            }
        } else {
            _uiState.update { 
                it.copy(
                    profitAmount = BigDecimal.ZERO,
                    profitPercentage = 0.0
                ) 
            }
        }
    }
    
    fun saveProduct(onSuccess: () -> Unit) {
        val state = _uiState.value
        
        if (!state.isValid) {
            _uiState.update { it.copy(errorMessage = "Please fill all required fields correctly") }
            return
        }
        
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            try {
                val now = Clock.System.now()
                val product = Product(
                    productName = state.productName.trim(),
                    brand = state.brand.trim(),
                    model = state.model.trim(),
                    category = state.category.trim(),
                    imei1 = state.imei1,
                    imei2 = state.imei2.ifBlank { null },
                    costPrice = state.costPrice.toBigDecimal(),
                    sellingPrice = state.sellingPrice.toBigDecimal(),
                    currentStock = state.currentStock.toInt(),
                    minStockLevel = state.minStockLevel.toInt(),
                    barcode = state.barcode.ifBlank { null },
                    description = state.description.ifBlank { null },
                    purchaseDate = now,
                    createdAt = now,
                    updatedAt = now,
                    isActive = true
                )
                
                database.productDao().insertProduct(product)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        successMessage = "Product added successfully!"
                    ) 
                }
                
                onSuccess()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to add product: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun clearForm() {
        _uiState.update { 
            AddPurchaseUiState(
                existingCategories = it.existingCategories,
                existingBrands = it.existingBrands
            ) 
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
