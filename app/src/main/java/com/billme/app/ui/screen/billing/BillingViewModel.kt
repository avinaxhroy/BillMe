package com.billme.app.ui.screen.billing

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.pdf.InvoiceFileManager
import com.billme.app.core.pdf.InvoicePdfGenerator
import com.billme.app.core.util.ErrorMessageHandler
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.DiscountType
import com.billme.app.data.local.entity.GSTConfiguration
import com.billme.app.data.local.entity.GSTMode
import com.billme.app.data.local.entity.GSTType
import com.billme.app.data.local.entity.PaymentMethod
import com.billme.app.data.local.entity.PaymentStatus
import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.Transaction
import com.billme.app.data.local.entity.TransactionItem
import com.billme.app.data.local.entity.TransactionLineItem
import com.billme.app.service.ReceiptService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.Clock
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CartItem(
    val product: Product,
    val quantity: Int,
    val customPrice: BigDecimal? = null,
    val imeiId: Long? = null // For IMEI-tracked products
) {
    val effectivePrice: BigDecimal
        get() = customPrice ?: product.sellingPrice
    
    val totalPrice: BigDecimal
        get() = effectivePrice.multiply(BigDecimal(quantity))
    
    val totalCost: BigDecimal
        get() = product.costPrice.multiply(BigDecimal(quantity))
}

data class BillingUiState(
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val gstAmount: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val profit: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val applyGST: Boolean = true,
    val phoneError: String? = null,
    val canEditPrice: Boolean = true,
    val showIMEISelection: Boolean = false,
    val productForIMEISelection: Product? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class BillingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BillMeDatabase,
    private val receiptService: ReceiptService,
    private val pdfGenerator: InvoicePdfGenerator,
    private val fileManager: InvoiceFileManager,
    private val errorMessageHandler: ErrorMessageHandler,
    private val signatureRepository: com.billme.app.data.repository.SignatureRepository
) : ViewModel() {
    
    private val _customerName = MutableStateFlow("")
    private val _customerPhone = MutableStateFlow("")
    private val _customerAddress = MutableStateFlow("")
    private val _cartItems = MutableStateFlow<Map<Long, CartItem>>(emptyMap())
    private val _discount = MutableStateFlow(BigDecimal.ZERO)
    private val _applyGST = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _showIMEISelection = MutableStateFlow(false)
    private val _productForIMEISelection = MutableStateFlow<Product?>(null)
    
    // Hindi transliteration fields
    private val _hindiCustomerName = MutableStateFlow("")
    private val _hindiCustomerAddress = MutableStateFlow("")
    
    val searchQuery = MutableStateFlow("")
    
    val searchResults: StateFlow<List<Product>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                database.productDao().searchProducts(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val uiState: StateFlow<BillingUiState> = combine(
        _customerName,
        _customerPhone,
        _customerAddress,
        _cartItems,
        _discount,
        _applyGST,
        _errorMessage,
        _successMessage,
        _showIMEISelection,
        _productForIMEISelection
    ) { values ->
        val customerName = values[0] as? String ?: ""
        val customerPhone = values[1] as? String ?: ""
        val customerAddress = values[2] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val cartMap = values[3] as? Map<Long, CartItem> ?: emptyMap()
        val discount = values[4] as? BigDecimal ?: BigDecimal.ZERO
        val applyGST = values[5] as? Boolean ?: false
        val errorMsg = values[6] as? String
        val successMsg = values[7] as? String
        val showIMEISelection = values[8] as? Boolean ?: false
        val productForIMEI = values[9] as? Product
        
        val items = cartMap.values.toList()
        
        // When GST is inclusive:
        // sellingPrice = taxableAmount + GST
        // subtotal (shown) = taxableAmount (selling price - GST)
        // total (final price) = sellingPrice (what customer pays)
        val finalTotal = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.totalPrice)
        }
        
        val discountedTotal = finalTotal.subtract(discount)
        
        // GST is INCLUSIVE - the price already contains GST
        val gstAmount = if (applyGST) {
            // Extract GST from inclusive price
            // Formula: GST = Price - (Price / (1 + Rate/100))
            // For 18% GST: GST = Price - (Price / 1.18)
            val taxableAmount = discountedTotal.divide(
                BigDecimal("1.18"),
                4,
                RoundingMode.HALF_UP
            )
            discountedTotal.subtract(taxableAmount)
                .setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        // Calculate subtotal as taxable value (selling price - GST)
        val subtotal = discountedTotal.subtract(gstAmount)
            .setScale(2, RoundingMode.HALF_UP)
        
        // Total is the final price customer pays (includes GST)
        val total = discountedTotal
        
        val profit = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.totalPrice.subtract(item.totalCost))
        }
        
        val phoneError = validatePhone(customerPhone)
        
        BillingUiState(
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            cartItems = items,
            subtotal = subtotal,
            discount = discount,
            gstAmount = gstAmount,
            total = total,
            profit = profit,
            applyGST = applyGST,
            errorMessage = errorMsg,
            successMessage = successMsg,
            phoneError = phoneError,
            showIMEISelection = showIMEISelection,
            productForIMEISelection = productForIMEI
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BillingUiState()
    )
    
    private fun validatePhone(phone: String): String? {
        if (phone.isBlank()) return null
        if (phone.length != 10) return "Phone number must be 10 digits"
        if (!phone.all { it.isDigit() }) return "Phone number must contain only digits"
        return null
    }
    
    fun updateCustomerName(name: String) {
        _customerName.value = name
    }
    
    fun updateCustomerPhone(phone: String) {
        _customerPhone.value = phone
    }
    
    fun updateCustomerAddress(address: String) {
        _customerAddress.value = address
    }
    
    fun updateHindiCustomerName(hindiName: String) {
        _hindiCustomerName.value = hindiName
    }
    
    fun updateHindiCustomerAddress(hindiAddress: String) {
        _hindiCustomerAddress.value = hindiAddress
    }
    
    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }
    
    fun addToCart(product: Product) {
        viewModelScope.launch {
            try {
                // Refresh product to get latest stock
                val latestProduct = database.productDao().getProductById(product.productId)
                if (latestProduct == null) {
                    _errorMessage.value = ErrorMessageHandler.Messages.PRODUCT_NOT_FOUND
                    return@launch
                }
                
                // Check if product has IMEIs (smartphones category)
                val availableIMEIs = database.productIMEIDao().getAvailableIMEIs(latestProduct.productId)
                if (availableIMEIs.isNotEmpty()) {
                    // Show IMEI selection dialog
                    _showIMEISelection.value = true
                    _productForIMEISelection.value = latestProduct
                    return@launch
                }
                
                // Check stock before adding (synced with IMEI availability)
                if (latestProduct.currentStock <= 0) {
                    _errorMessage.value = "Out of stock: ${latestProduct.productName}"
                    return@launch
                }
                
                val currentCart = _cartItems.value.toMutableMap()
                val existing = currentCart[product.productId]
                
                if (existing != null) {
                    // Check if increment is possible
                    if (existing.quantity >= latestProduct.currentStock) {
                        _errorMessage.value = ErrorMessageHandler.Messages.insufficientStock(latestProduct.currentStock)
                        return@launch
                    }
                    currentCart[product.productId] = existing.copy(
                        product = latestProduct, // Update with latest data
                        quantity = existing.quantity + 1
                    )
                } else {
                    // Add new item
                    currentCart[product.productId] = CartItem(
                        product = latestProduct,
                        quantity = 1
                    )
                }
                
                _cartItems.value = currentCart
                _successMessage.value = "Added to cart"
                clearMessages()
            } catch (e: Exception) {
                _errorMessage.value = errorMessageHandler.getUserFriendlyMessage(e, "add product to cart")
            }
        }
    }
    
    fun addToCartWithIMEI(product: Product, imeiId: Long) {
        viewModelScope.launch {
            try {
                val currentCart = _cartItems.value.toMutableMap()
                
                // For IMEI tracked items, each IMEI is a separate cart entry
                // Use a unique key combining product ID and IMEI ID
                val cartKey = product.productId
                
                currentCart[cartKey] = CartItem(
                    product = product,
                    quantity = 1,
                    imeiId = imeiId
                )
                
                _cartItems.value = currentCart
                _successMessage.value = "Added to cart"
                clearMessages()
            } catch (e: Exception) {
                _errorMessage.value = errorMessageHandler.getUserFriendlyMessage(e, "add product to cart")
            }
        }
    }
    
    fun dismissIMEISelection() {
        _showIMEISelection.value = false
        _productForIMEISelection.value = null
    }
    
    suspend fun getAvailableIMEIs(productId: Long): List<com.billme.app.data.local.entity.ProductIMEI> {
        return database.productIMEIDao().getAvailableIMEIs(productId)
    }
    
    /**
     * Add product to cart by scanning IMEI
     * Fixed: More robust error handling and better cart key generation
     */
    fun addProductByIMEI(imeiNumber: String) {
        viewModelScope.launch {
            try {
                // Clean the IMEI number
                val cleanIMEI = imeiNumber.trim().filter { it.isDigit() }
                
                if (cleanIMEI.length != 15) {
                    _errorMessage.value = "Invalid IMEI format: $imeiNumber"
                    clearMessages()
                    return@launch
                }
                
                // Find the ProductIMEI by IMEI number
                val productIMEI = database.productIMEIDao().getIMEIByNumber(cleanIMEI)
                
                if (productIMEI == null) {
                    _errorMessage.value = "IMEI not found in inventory: $cleanIMEI"
                    clearMessages()
                    return@launch
                }
                
                // Check if already sold
                if (productIMEI.status != com.billme.app.data.local.entity.IMEIStatus.AVAILABLE) {
                    _errorMessage.value = "IMEI not available (Status: ${productIMEI.status})"
                    clearMessages()
                    return@launch
                }
                
                // Get the product details
                val product = database.productDao().getProductById(productIMEI.productId)
                
                if (product == null) {
                    _errorMessage.value = "Product not found for IMEI: $cleanIMEI"
                    clearMessages()
                    return@launch
                }
                
                // Check if this IMEI is already in cart
                val existingInCart = _cartItems.value.values.any { it.imeiId == productIMEI.imeiId }
                if (existingInCart) {
                    _errorMessage.value = "IMEI already in cart"
                    clearMessages()
                    return@launch
                }
                
                // Add to cart with IMEI
                val currentCart = _cartItems.value.toMutableMap()
                
                // For IMEI-tracked items, use a unique key that combines product ID and IMEI ID
                // This allows multiple units of the same product with different IMEIs
                val cartKey = product.productId + (productIMEI.imeiId * 1000000)
                
                currentCart[cartKey] = CartItem(
                    product = product,
                    quantity = 1,
                    imeiId = productIMEI.imeiId
                )
                
                _cartItems.value = currentCart
                _successMessage.value = "✓ Added: ${product.productName}"
                clearMessages()
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add product: ${e.message}"
                android.util.Log.e("BillingViewModel", "Error adding product by IMEI", e)
                clearMessages()
            }
        }
    }
    
    private fun clearMessages() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _errorMessage.value = null
            _successMessage.value = null
        }
    }
    
    fun increaseQuantity(product: Product) {
        viewModelScope.launch {
            try {
                val latestProduct = database.productDao().getProductById(product.productId)
                if (latestProduct == null) {
                    _errorMessage.value = "Product not found"
                    clearMessages()
                    return@launch
                }
                
                val currentCart = _cartItems.value.toMutableMap()
                val existing = currentCart[product.productId]
                
                if (existing == null) {
                    _errorMessage.value = "Item not in cart"
                    clearMessages()
                    return@launch
                }
                
                // Skip IMEI-tracked items (quantity is always 1)
                if (existing.imeiId != null) {
                    _errorMessage.value = "Cannot increase IMEI-tracked items"
                    clearMessages()
                    return@launch
                }
                
                if (existing.quantity >= latestProduct.currentStock) {
                    _errorMessage.value = ErrorMessageHandler.Messages.insufficientStock(latestProduct.currentStock)
                    clearMessages()
                    return@launch
                }
                
                currentCart[product.productId] = existing.copy(
                    product = latestProduct,
                    quantity = existing.quantity + 1
                )
                _cartItems.value = currentCart
                
            } catch (e: Exception) {
                _errorMessage.value = errorMessageHandler.getUserFriendlyMessage(e, "update quantity")
                clearMessages()
            }
        }
    }
    
    fun decreaseQuantity(product: Product) {
        viewModelScope.launch {
            try {
                val currentCart = _cartItems.value.toMutableMap()
                val existing = currentCart[product.productId]
                
                if (existing == null) {
                    _errorMessage.value = "Item not in cart"
                    clearMessages()
                    return@launch
                }
                
                if (existing.quantity > 1) {
                    currentCart[product.productId] = existing.copy(
                        quantity = existing.quantity - 1
                    )
                    _cartItems.value = currentCart
                } else {
                    // Remove item if quantity becomes 0
                    currentCart.remove(product.productId)
                    _cartItems.value = currentCart
                }
                
            } catch (e: Exception) {
                _errorMessage.value = errorMessageHandler.getUserFriendlyMessage(e, "update quantity")
                clearMessages()
            }
        }
    }
    
    fun setQuantity(product: Product, quantity: Int) {
        if (quantity < 1) {
            removeFromCart(product)
            return
        }
        
        viewModelScope.launch {
            try {
                val latestProduct = database.productDao().getProductById(product.productId) ?: return@launch
                
                if (quantity > latestProduct.currentStock) {
                    _errorMessage.value = ErrorMessageHandler.Messages.insufficientStock(latestProduct.currentStock)
                    clearMessages()
                    return@launch
                }
                
                val currentCart = _cartItems.value.toMutableMap()
                currentCart[product.productId] = CartItem(
                    product = latestProduct,
                    quantity = quantity
                )
                _cartItems.value = currentCart
            } catch (e: Exception) {
                _errorMessage.value = errorMessageHandler.getUserFriendlyMessage(e, "set quantity")
            }
        }
    }
    
    fun toggleGST(apply: Boolean) {
        _applyGST.value = apply
    }
    
    fun updateItemPrice(productId: Long, newPrice: BigDecimal) {
        val currentCart = _cartItems.value.toMutableMap()
        val existing = currentCart[productId] ?: return
        
        if (newPrice <= BigDecimal.ZERO) {
            _errorMessage.value = ErrorMessageHandler.Messages.INVALID_PRICE
            clearMessages()
            return
        }
        
        if (newPrice < existing.product.costPrice) {
            _errorMessage.value = "Warning: Price is below cost price"
            clearMessages()
        }
        
        currentCart[productId] = existing.copy(customPrice = newPrice)
        _cartItems.value = currentCart
    }
    
    fun resetItemPrice(productId: Long) {
        val currentCart = _cartItems.value.toMutableMap()
        val existing = currentCart[productId] ?: return
        currentCart[productId] = existing.copy(customPrice = null)
        _cartItems.value = currentCart
    }
    
    fun updateDiscount(discount: BigDecimal) {
        _discount.value = discount
    }
    
    /**
     * Update the subtotal manually
     * This distributes the new subtotal proportionally across all cart items
     */
    fun updateSubtotal(newSubtotal: BigDecimal) {
        val currentCart = _cartItems.value
        if (currentCart.isEmpty()) return
        
        val currentSubtotal = currentCart.values.sumOf { it.totalPrice }
        if (currentSubtotal <= BigDecimal.ZERO) return
        
        // Calculate the adjustment ratio
        val adjustmentRatio = newSubtotal.divide(currentSubtotal, 4, RoundingMode.HALF_UP)
        
        // Update each item's price proportionally
        val updatedCart = currentCart.mapValues { (_, cartItem) ->
            val newItemPrice = cartItem.effectivePrice
                .multiply(adjustmentRatio)
                .setScale(2, RoundingMode.HALF_UP)
            
            cartItem.copy(customPrice = newItemPrice)
        }
        
        _cartItems.value = updatedCart
    }
    
    /**
     * Update the total amount manually
     * This automatically recalculates subtotal and GST based on the new total
     * The new total is treated as the final amount inclusive of GST
     */
    fun updateTotal(newTotal: BigDecimal) {
        val currentCart = _cartItems.value
        if (currentCart.isEmpty()) return
        
        // Clear any discount when manually setting total
        _discount.value = BigDecimal.ZERO
        
        // Calculate subtotal and GST using INCLUSIVE GST logic
        // Total stays exactly as user entered (e.g., 70)
        // GST = Total × 18/118
        // Subtotal = Total - GST
        val divisor = BigDecimal("118")
        val multiplier = BigDecimal("18")
        val newGST = newTotal.multiply(multiplier).divide(divisor, 2, RoundingMode.HALF_UP)
        val newSubtotal = newTotal.subtract(newGST)
            .setScale(2, RoundingMode.HALF_UP)
        
        // Get current subtotal (sum of all item prices)
        val currentSubtotal = currentCart.values.sumOf { it.totalPrice }
        if (currentSubtotal <= BigDecimal.ZERO) return
        
        // Calculate the adjustment ratio
        val adjustmentRatio = newSubtotal.divide(currentSubtotal, 4, RoundingMode.HALF_UP)
        
        // Update each item's price proportionally
        val updatedCart = currentCart.mapValues { (_, cartItem) ->
            val newItemPrice = cartItem.effectivePrice
                .multiply(adjustmentRatio)
                .setScale(2, RoundingMode.HALF_UP)
            
            cartItem.copy(customPrice = newItemPrice)
        }
        
        _cartItems.value = updatedCart
    }
    
    fun removeFromCart(product: Product) {
        viewModelScope.launch {
            try {
                val currentCart = _cartItems.value.toMutableMap()
                val removed = currentCart.remove(product.productId)
                
                if (removed != null) {
                    _cartItems.value = currentCart
                    _successMessage.value = "Item removed from cart"
                    clearMessages()
                } else {
                    _errorMessage.value = "Item not found in cart"
                    clearMessages()
                }
                
            } catch (e: Exception) {
                _errorMessage.value = errorMessageHandler.getUserFriendlyMessage(e, "remove item")
                clearMessages()
            }
        }
    }
    
    fun clearCart() {
        _customerName.value = ""
        _customerPhone.value = ""
        _customerAddress.value = ""
        _cartItems.value = emptyMap()
        _discount.value = BigDecimal.ZERO
        searchQuery.value = ""
    }
    
    fun completeTransaction(
        paymentMethod: String,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState.cartItems.isEmpty() || currentState.customerName.isBlank()) {
                    return@launch
                }
                
                // Create transaction
                val now = Clock.System.now()
                val transactionNumber = "TXN${System.currentTimeMillis()}"
                val payMethod = when (paymentMethod) {
                    "CASH" -> PaymentMethod.CASH
                    "CARD" -> PaymentMethod.CARD
                    "UPI" -> PaymentMethod.UPI
                    else -> PaymentMethod.OTHER
                }
                
                // Build notes with address and Hindi transliteration data
                // Format: Each piece on a separate line for clean extraction
                val notesBuilder = StringBuilder()
                if (currentState.customerAddress.isNotBlank()) {
                    notesBuilder.append("Address: ${currentState.customerAddress}\n")
                }
                // Store corrected Hindi transliteration data for bilingual PDF
                // Only store if user explicitly corrected it
                if (_hindiCustomerName.value.isNotBlank()) {
                    notesBuilder.append("HindiName:${_hindiCustomerName.value}\n")
                }
                if (_hindiCustomerAddress.value.isNotBlank()) {
                    notesBuilder.append("HindiAddress:${_hindiCustomerAddress.value}\n")
                }
                
                val transaction = Transaction(
                    transactionNumber = transactionNumber,
                    customerPhone = currentState.customerPhone.ifBlank { null },
                    customerName = currentState.customerName,
                    transactionDate = now,
                    subtotal = currentState.subtotal,
                    discountAmount = currentState.discount,
                    discountType = DiscountType.AMOUNT,
                    taxAmount = currentState.gstAmount,
                    taxRate = 18.0,
                    grandTotal = currentState.total,
                    paymentMethod = payMethod,
                    paymentStatus = PaymentStatus.PAID,
                    profitAmount = currentState.profit,
                    notes = notesBuilder.toString().ifBlank { null },
                    createdAt = now
                )
                
                // Insert transaction
                val transactionId = database.transactionDao().insertTransaction(transaction)
                
                // Insert line items and mark IMEIs as sold
                val lineItems = currentState.cartItems.map { cartItem ->
                    TransactionLineItem(
                        transactionId = transactionId,
                        productId = cartItem.product.productId,
                        productName = cartItem.product.productName,
                        imeiSold = cartItem.product.imei1,
                        quantity = cartItem.quantity,
                        unitCost = cartItem.product.costPrice,
                        unitSellingPrice = cartItem.product.sellingPrice,
                        lineTotal = cartItem.totalPrice,
                        lineProfit = cartItem.totalPrice.subtract(cartItem.totalCost)
                    )
                }
                
                lineItems.forEach { lineItem ->
                    database.transactionLineItemDao().insertLineItem(lineItem)
                }
                
                // Reduce stock and mark IMEIs as sold
                currentState.cartItems.forEach { cartItem ->
                    // If this cart item has an IMEI, mark it as sold
                    if (cartItem.imeiId != null) {
                        database.productIMEIDao().markAsSold(
                            imeiId = cartItem.imeiId,
                            soldDate = now,
                            soldPrice = cartItem.effectivePrice,
                            transactionId = transactionId,
                            updatedAt = now
                        )
                        // Sync stock count after marking IMEI as sold
                        database.productDao().syncStockWithAvailableIMEIs(cartItem.product.productId)
                    }
                    
                    database.productDao().reduceStock(
                        productId = cartItem.product.productId,
                        quantity = cartItem.quantity
                    )
                }
                
                onComplete(transactionId)
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error - could show a toast or error state
            }
        }
    }
    
    fun printReceipt(transactionId: Long) {
        viewModelScope.launch {
            try {
                // Fetch transaction and line items
                val transaction = database.transactionDao().getTransactionById(transactionId)
                val lineItems = database.transactionLineItemDao().getLineItemsByTransactionIdSync(transactionId)
                
                if (transaction != null) {
                    // Generate receipt content
                    val receipt = receiptService.generateReceipt(transaction, lineItems)
                    
                    // Display receipt content as formatted text
                    val receiptText = buildString {
                        appendLine("╔══════════════════════════════════════╗")
                        appendLine("║         TRANSACTION RECEIPT           ║")
                        appendLine("╠══════════════════════════════════════╣")
                        appendLine("║ Transaction: #$transactionId")
                        appendLine("║ Customer: ${transaction.customerName ?: "Walk-in"}")
                        transaction.customerPhone?.let {
                            appendLine("║ Phone: $it")
                        }
                        appendLine("║ Total: ₹${transaction.grandTotal}")
                        appendLine("║ Payment: ${transaction.paymentMethod.name}")
                        appendLine("╚══════════════════════════════════════╝")
                        appendLine()
                        appendLine("Receipt has been generated!")
                        appendLine("In production, this will be sent to your thermal printer.")
                        appendLine()
                        appendLine("Receipt Content Preview:")
                        appendLine(receipt.toString())
                    }
                    
                    println(receiptText)
                    
                    // TODO: When real printer is connected, use:
                    // printerService.printReceipt(receipt)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error printing receipt: ${e.message}")
            }
        }
    }
    
    // ============================================
    // Invoice Generation & Sharing Methods
    // ============================================
    
    private suspend fun getShopSettings(): ShopSettings {
        val settingsDao = database.appSettingDao()
        return ShopSettings(
            shopName = settingsDao.getSettingByKey("shop_name")?.settingValue ?: "BillMe Shop",
            shopAddress = settingsDao.getSettingByKey("shop_address")?.settingValue ?: "",
            shopPhone = settingsDao.getSettingByKey("shop_phone")?.settingValue ?: "",
            shopGst = settingsDao.getSettingByKey("gst_number")?.settingValue ?: ""
        )
    }
    
    private suspend fun getGSTConfiguration(): GSTConfiguration {
        val settingsDao = database.appSettingDao()
        
        // Get GST mode from settings
        val gstModeStr = settingsDao.getSettingByKey("gst_mode")?.settingValue ?: "FULL_GST"
        val gstMode = try {
            GSTMode.valueOf(gstModeStr)
        } catch (e: Exception) {
            GSTMode.FULL_GST
        }
        
        // Get GST rate
        val gstRate = settingsDao.getSettingByKey("default_gst_rate")?.settingValue?.toDoubleOrNull() ?: 18.0
        
        // Get GST type (IGST or CGST+SGST)
        val gstTypeStr = settingsDao.getSettingByKey("gst_type")?.settingValue ?: "CGST_SGST"
        val gstType = try {
            GSTType.valueOf(gstTypeStr)
        } catch (e: Exception) {
            GSTType.CGST_SGST
        }
        
        // Get GST number
        val gstNumber = settingsDao.getSettingByKey("gst_number")?.settingValue
        
        // Get state code
        val stateCode = settingsDao.getSettingByKey("state_code")?.settingValue
        
        val now = kotlinx.datetime.Clock.System.now()
        return GSTConfiguration(
            shopGSTIN = gstNumber,
            shopStateCode = stateCode,
            defaultGSTMode = gstMode,
            defaultGSTRate = gstRate,
            createdAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Generate invoice PDF with IMEI information
     * Fixed: Better Hindi name/address extraction and validation
     * @param transactionId Transaction ID
     * @param customerCopy If true, generates customer copy, otherwise owner copy
     * @return Generated PDF file
     */
    suspend fun generateInvoicePdf(transactionId: Long, customerCopy: Boolean = true): File? {
        return try {
            val transaction = database.transactionDao().getTransactionById(transactionId) ?: return null
            val lineItems = database.transactionLineItemDao().getLineItemsByTransactionIdSync(transactionId) ?: emptyList()
            val shopSettings = getShopSettings()
            val gstConfig = getGSTConfiguration()
            
            // Get active signature for this invoice
            val activeSignature = signatureRepository.getActiveSignature()
            val signatureFilePath = activeSignature?.signatureFilePath
            
            // Extract customer details with proper fallback
            var customerNameForPdf = transaction.customerName
            var customerAddressForPdf = ""
            
            // Parse notes to extract address and Hindi data
            if (transaction.notes != null) {
                val notes = transaction.notes
                
                // Extract Address (always try this first)
                val addressRegex = Regex("Address:\\s*(.+?)(?=\n|$)", RegexOption.MULTILINE)
                addressRegex.find(notes)?.let { match ->
                    customerAddressForPdf = match.groupValues[1].trim()
                }
                
                // Check if Hindi name was explicitly set (user corrected it)
                val hindiNameRegex = Regex("HindiName:\\s*(.+?)(?=\n|$)", RegexOption.MULTILINE)
                hindiNameRegex.find(notes)?.let { match ->
                    val hindiName = match.groupValues[1].trim()
                    // Only use Hindi name if it's not blank and contains actual Hindi characters
                    if (hindiName.isNotBlank() && hindiName != customerNameForPdf) {
                        customerNameForPdf = hindiName
                    }
                }
                
                // Check if Hindi address was explicitly set (user corrected it)
                val hindiAddressRegex = Regex("HindiAddress:\\s*(.+?)(?=\n|$)", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
                hindiAddressRegex.find(notes)?.let { match ->
                    val hindiAddress = match.groupValues.getOrNull(1)?.trim() ?: ""
                    // Only use Hindi address if it's not blank and different from English
                    if (hindiAddress.isNotBlank() && hindiAddress != customerAddressForPdf) {
                        customerAddressForPdf = hindiAddress
                    }
                }
            }
            
            // Fallback: If no customer name, use a default
            if (customerNameForPdf?.isBlank() != false) {
                customerNameForPdf = "Walk-in Customer"
            }
            
            // Get IMEI details for all items in this transaction
            val transactionIMEIs = database.productIMEIDao().getIMEIsByTransaction(transactionId)
            val imeiMap = transactionIMEIs.associateBy { it.imeiNumber }
            
            // Convert TransactionLineItem to TransactionItem for PDF generator
            val items = lineItems.map { lineItem ->
                // Get IMEI details if available
                val imeiDetails = imeiMap[lineItem.imeiSold]
                
                // Get product details for color and variant
                val product = database.productDao().getProductById(lineItem.productId)
                
                TransactionItem(
                    transactionItemId = lineItem.lineItemId,
                    transactionId = lineItem.transactionId,
                    productId = lineItem.productId,
                    productName = lineItem.productName,
                    brand = product?.brand,
                    model = product?.model,
                    color = product?.color,
                    variant = product?.variant,
                    quantity = lineItem.quantity,
                    costPrice = lineItem.unitCost,
                    sellingPrice = lineItem.unitSellingPrice,
                    totalPrice = lineItem.lineTotal,
                    discountAmount = BigDecimal.ZERO,
                    taxAmount = BigDecimal.ZERO,
                    notes = null,
                    imeiNumber = imeiDetails?.imeiNumber,
                    imei2Number = imeiDetails?.imei2Number
                )
            }
            
            // Create a modified transaction object with extracted data
            val modifiedTransaction = transaction.copy(
                customerName = customerNameForPdf,
                // Pass address through notes field in a format the PDF generator expects
                notes = if (customerAddressForPdf.isNotBlank()) {
                    "Address: $customerAddressForPdf"
                } else {
                    transaction.notes
                }
            )
            
            // Determine if we should use bilingual mode
            // Only enable if user explicitly set Hindi data
            val useBilingualMode = transaction.notes?.contains("HindiName:") == true || 
                                  transaction.notes?.contains("HindiAddress:") == true
            
            pdfGenerator.generateInvoicePdf(
                transaction = modifiedTransaction,
                items = items,
                shopName = shopSettings.shopName,
                shopAddress = shopSettings.shopAddress,
                shopPhone = shopSettings.shopPhone,
                shopGst = shopSettings.shopGst,
                customerCopy = customerCopy,
                gstConfig = gstConfig,
                signatureFilePath = signatureFilePath,
                bilingualMode = useBilingualMode
            )
        } catch (e: Exception) {
            android.util.Log.e("BillingViewModel", "Error generating invoice PDF", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Generate and save both owner and customer copies automatically
     */
    suspend fun generateAndSaveBothCopies(transactionId: Long): Pair<File?, File?> {
        val customerCopy = generateInvoicePdf(transactionId, customerCopy = true)
        val ownerCopy = generateInvoicePdf(transactionId, customerCopy = false)
        return Pair(customerCopy, ownerCopy)
    }
    
    /**
     * Share invoice via intent chooser
     */
    fun shareInvoice(file: File) {
        try {
            if (!file.exists()) {
                _errorMessage.value = ErrorMessageHandler.Messages.FILE_NOT_FOUND
                return
            }
            
            val intent = fileManager.createShareIntent(file)
            // Ensure NEW_TASK flag is set for context.startActivity
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: SecurityException) {
            _errorMessage.value = ErrorMessageHandler.Messages.STORAGE_PERMISSION_REQUIRED
        } catch (e: Exception) {
            _errorMessage.value = errorMessageHandler.getUserFriendlyMessage(e, "share invoice")
        }
    }
    
    /**
     * Share invoice via WhatsApp
     */
    fun shareInvoiceViaWhatsApp(file: File, phoneNumber: String? = null) {
        try {
            if (!file.exists()) {
                _errorMessage.value = ErrorMessageHandler.Messages.FILE_NOT_FOUND
                return
            }
            
            val intent = fileManager.createWhatsAppShareIntent(file, phoneNumber)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                _errorMessage.value = "WhatsApp not installed. Please install WhatsApp or use another sharing method."
            }
        } catch (e: Exception) {
            _errorMessage.value = errorMessageHandler.getUserFriendlyMessage(e, "share via WhatsApp")
        }
    }
    
    /**
     * Share invoice via Email
     */
    fun shareInvoiceViaEmail(file: File, recipientEmail: String? = null) {
        try {
            if (!file.exists()) {
                _errorMessage.value = ErrorMessageHandler.Messages.FILE_NOT_FOUND
                return
            }
            
            val intent = fileManager.createEmailShareIntent(file, recipientEmail)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            _errorMessage.value = "No email app found. Please install an email app or use another sharing method."
        }
    }
    
    /**
     * Print invoice using Android Print Framework
     */
    fun printInvoice(file: File) {
        fileManager.printPdf(file, "Invoice")
    }
    
    /**
     * Open invoice in external PDF viewer
     */
    fun viewInvoice(file: File) {
        val intent = fileManager.openPdfInViewer(file)
        context.startActivity(intent)
    }
    
    /**
     * Get invoice file if it exists
     */
    suspend fun getInvoiceFile(transactionNumber: String, customerCopy: Boolean = true): File? {
        return fileManager.getInvoiceByNumber(transactionNumber, customerCopy)
    }
    
    /**
     * Check if invoice exists
     */
    fun invoiceExists(transactionNumber: String, customerCopy: Boolean = true): Boolean {
        return fileManager.invoiceExists(transactionNumber, customerCopy)
    }
    
    private data class ShopSettings(
        val shopName: String,
        val shopAddress: String,
        val shopPhone: String,
        val shopGst: String
    )
}
