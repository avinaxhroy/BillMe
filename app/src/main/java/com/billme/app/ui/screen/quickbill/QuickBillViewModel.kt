package com.billme.app.ui.screen.quickbill

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.pdf.InvoiceFileManager
import com.billme.app.core.pdf.InvoicePdfGenerator
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.DiscountType
import com.billme.app.data.local.entity.GSTConfiguration
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
    val imeiId: Long? = null // For IMEI-tracked products
) {
    val totalPrice: BigDecimal
        get() = product.sellingPrice.multiply(BigDecimal(quantity))
    
    val totalCost: BigDecimal
        get() = product.costPrice.multiply(BigDecimal(quantity))
}

data class QuickBillUiState(
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val gstAmount: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val profit: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false,
    val showIMEISelection: Boolean = false,
    val productForIMEISelection: Product? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickBillViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BillMeDatabase,
    private val receiptService: ReceiptService,
    private val pdfGenerator: InvoicePdfGenerator,
    private val fileManager: InvoiceFileManager
) : ViewModel() {
    
    private val _cartItems = MutableStateFlow<Map<Long, CartItem>>(emptyMap())
    
    private val _discount = MutableStateFlow(BigDecimal.ZERO)
    
    private val _showIMEISelection = MutableStateFlow(false)
    private val _productForIMEISelection = MutableStateFlow<Product?>(null)
    
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
    
    val uiState: StateFlow<QuickBillUiState> = combine(
        _cartItems,
        _discount,
        _showIMEISelection,
        _productForIMEISelection
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val cartMap = values[0] as? Map<Long, CartItem> ?: emptyMap()
        val discount = values[1] as? BigDecimal ?: BigDecimal.ZERO
        val showIMEISelection = values[2] as? Boolean ?: false
        val productForIMEI = values[3] as? Product?
        
        val items = cartMap.values.toList()
        
        // Calculate subtotal
        val subtotal = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.totalPrice)
        }
        
        // Apply discount
        val discountedTotal = subtotal.subtract(discount)
        
        // GST is INCLUSIVE in Indian billing - price already contains GST
        // Extract GST from inclusive price: GST = Price - (Price / (1 + Rate/100))
        // For 18% GST: GST = Price - (Price / 1.18)
        val gstAmount = discountedTotal.subtract(
            discountedTotal.divide(
                BigDecimal("1.18"),
                4,
                RoundingMode.HALF_UP
            )
        ).setScale(2, RoundingMode.HALF_UP)
        
        // Total is the discounted amount (already includes GST)
        val total = discountedTotal
        
        val totalCost = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.totalCost)
        }
        val profit = subtotal.subtract(totalCost).subtract(discount)
            .setScale(2, RoundingMode.HALF_UP)
        
        QuickBillUiState(
            cartItems = items,
            subtotal = subtotal,
            discount = discount,
            gstAmount = gstAmount,
            total = total,
            profit = profit,
            showIMEISelection = showIMEISelection,
            productForIMEISelection = productForIMEI
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickBillUiState()
    )
    
    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }
    
    fun addToCart(product: Product) {
        viewModelScope.launch {
            // Check if product has IMEIs (smartphones category)
            val availableIMEIs = database.productIMEIDao().getAvailableIMEIs(product.productId)
            if (availableIMEIs.isNotEmpty()) {
                // Show IMEI selection dialog
                _showIMEISelection.value = true
                _productForIMEISelection.value = product
                return@launch
            }
            
            // No IMEIs, add normally
            val currentCart = _cartItems.value.toMutableMap()
            val existing = currentCart[product.productId]
            
            if (existing != null) {
                // Increment quantity if not exceeding stock
                if (existing.quantity < product.currentStock) {
                    currentCart[product.productId] = existing.copy(
                        quantity = existing.quantity + 1
                    )
                }
            } else {
                // Add new item with quantity 1
                if (product.currentStock > 0) {
                    currentCart[product.productId] = CartItem(
                        product = product,
                        quantity = 1
                    )
                }
            }
            
            _cartItems.value = currentCart
        }
    }
    
    fun addToCartWithIMEI(product: Product, imeiId: Long) {
        val currentCart = _cartItems.value.toMutableMap()
        
        // For IMEI tracked items, each IMEI is a separate cart entry
        currentCart[product.productId] = CartItem(
            product = product,
            quantity = 1,
            imeiId = imeiId
        )
        
        _cartItems.value = currentCart
    }
    
    fun dismissIMEISelection() {
        _showIMEISelection.value = false
        _productForIMEISelection.value = null
    }
    
    suspend fun getAvailableIMEIs(productId: Long): List<com.billme.app.data.local.entity.ProductIMEI> {
        return database.productIMEIDao().getAvailableIMEIs(productId)
    }
    
    fun increaseQuantity(product: Product) {
        val currentCart = _cartItems.value.toMutableMap()
        val existing = currentCart[product.productId] ?: return
        
        if (existing.quantity < product.currentStock) {
            currentCart[product.productId] = existing.copy(
                quantity = existing.quantity + 1
            )
            _cartItems.value = currentCart
        }
    }
    
    fun decreaseQuantity(product: Product) {
        val currentCart = _cartItems.value.toMutableMap()
        val existing = currentCart[product.productId] ?: return
        
        if (existing.quantity > 1) {
            currentCart[product.productId] = existing.copy(
                quantity = existing.quantity - 1
            )
        } else {
            currentCart.remove(product.productId)
        }
        
        _cartItems.value = currentCart
    }
    
    fun removeFromCart(product: Product) {
        val currentCart = _cartItems.value.toMutableMap()
        currentCart.remove(product.productId)
        _cartItems.value = currentCart
    }
    
    fun addUnlistedProduct(productName: String, price: BigDecimal, quantity: Int) {
        // Create a temporary product for unlisted items
        val now = Clock.System.now()
        val unlistedProduct = Product(
            productId = -System.currentTimeMillis(), // Negative ID for unlisted products
            productName = productName,
            brand = "Unlisted",
            model = "-",
            variant = null,
            category = "Unlisted",
            imei1 = "",
            imei2 = null,
            mrp = null,
            costPrice = price, // Use same as selling price since we don't know cost
            sellingPrice = price,
            productStatus = com.billme.app.data.local.entity.ProductStatus.IN_STOCK,
            currentStock = quantity, // Set stock as quantity requested
            minStockLevel = 0,
            barcode = null,
            productPhotos = null,
            supplierId = null,
            warrantyPeriodMonths = null,
            warrantyStartDate = null,
            warrantyExpiryDate = null,
            purchaseDate = now,
            description = "Unlisted product",
            createdAt = now,
            updatedAt = now,
            isActive = true
        )
        
        val currentCart = _cartItems.value.toMutableMap()
        currentCart[unlistedProduct.productId] = CartItem(
            product = unlistedProduct,
            quantity = quantity
        )
        _cartItems.value = currentCart
    }
    
    fun updateDiscount(discount: BigDecimal) {
        _discount.value = discount
    }
    
    fun clearCart() {
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
                if (currentState.cartItems.isEmpty()) {
                    android.util.Log.w("QuickBillViewModel", "Cannot complete transaction: cart is empty")
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
                
                val transaction = Transaction(
                    transactionNumber = transactionNumber,
                    customerPhone = null,
                    customerName = "Walk-in Customer",
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
                    createdAt = now
                )
                
                android.util.Log.d("QuickBillViewModel", "Creating transaction: ${transaction.transactionNumber}")
                
                // Insert transaction
                val transactionId = database.transactionDao().insertTransaction(transaction)
                
                android.util.Log.d("QuickBillViewModel", "Transaction created with ID: $transactionId")
                
                // Insert line items
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
                
                android.util.Log.d("QuickBillViewModel", "Inserted ${lineItems.size} line items")
                
                // Reduce stock and mark IMEIs as sold
                currentState.cartItems.forEach { cartItem ->
                    // Skip unlisted products (negative IDs)
                    if (cartItem.product.productId < 0) {
                        return@forEach
                    }
                    
                    // If this cart item has an IMEI, mark it as sold
                    if (cartItem.imeiId != null) {
                        database.productIMEIDao().markAsSold(
                            imeiId = cartItem.imeiId,
                            soldDate = now,
                            soldPrice = cartItem.product.sellingPrice,
                            transactionId = transactionId,
                            updatedAt = now
                        )
                        // Sync stock with available IMEIs
                        database.productDao().syncStockWithAvailableIMEIs(cartItem.product.productId)
                    } else {
                        // Regular stock reduction
                        database.productDao().reduceStock(
                            productId = cartItem.product.productId,
                            quantity = cartItem.quantity
                        )
                    }
                }
                
                android.util.Log.d("QuickBillViewModel", "Stock updated successfully")
                
                // Clear cart after successful transaction
                clearCart()
                
                // Notify success
                onComplete(transactionId)
                
                android.util.Log.d("QuickBillViewModel", "Transaction completed successfully")
                
            } catch (e: Exception) {
                android.util.Log.e("QuickBillViewModel", "Error completing transaction", e)
                e.printStackTrace()
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
    
    /**
     * Generate and save both owner and customer copies automatically
     */
    suspend fun generateAndSaveBothCopies(transactionId: Long): Pair<File?, File?> {
        val customerCopy = generateInvoicePdf(transactionId, customerCopy = true)
        val ownerCopy = generateInvoicePdf(transactionId, customerCopy = false)
        return Pair(customerCopy, ownerCopy)
    }
    
    private suspend fun generateInvoicePdf(transactionId: Long, customerCopy: Boolean = true): File? {
        return try {
            val transaction = database.transactionDao().getTransactionById(transactionId) ?: return null
            val lineItems = database.transactionLineItemDao().getLineItemsByTransactionIdSync(transactionId) ?: emptyList()
            val shopSettings = getShopSettings()
            val gstConfig = getGSTConfiguration()
            
            // Convert TransactionLineItem to TransactionItem for PDF generator
            val items = lineItems.map { lineItem ->
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
                    notes = null
                )
            }
            
            pdfGenerator.generateInvoicePdf(
                transaction = transaction,
                items = items,
                shopName = shopSettings.shopName,
                shopAddress = shopSettings.shopAddress,
                shopPhone = shopSettings.shopPhone,
                shopGst = shopSettings.shopGst,
                customerCopy = customerCopy,
                gstConfig = gstConfig
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private suspend fun getShopSettings(): ShopSettings {
        val settingsDao = database.appSettingDao()
        return ShopSettings(
            shopName = settingsDao.getSettingByKey("shop_name")?.settingValue ?: "My Shop",
            shopAddress = settingsDao.getSettingByKey("shop_address")?.settingValue ?: "",
            shopPhone = settingsDao.getSettingByKey("shop_phone")?.settingValue ?: "",
            shopGst = settingsDao.getSettingByKey("gst_number")?.settingValue ?: ""
        )
    }
    
    private suspend fun getGSTConfiguration(): GSTConfiguration {
        val settingsDao = database.appSettingDao()
        val gstModeStr = settingsDao.getSettingByKey("gst_mode")?.settingValue ?: "FULL_GST"
        val gstMode = try {
            com.billme.app.data.local.entity.GSTMode.valueOf(gstModeStr)
        } catch (e: Exception) {
            com.billme.app.data.local.entity.GSTMode.FULL_GST
        }
        val gstRate = settingsDao.getSettingByKey("default_gst_rate")?.settingValue?.toDoubleOrNull() ?: 18.0
        val gstNumber = settingsDao.getSettingByKey("gst_number")?.settingValue
        val stateCode = settingsDao.getSettingByKey("state_code")?.settingValue
        val now = Clock.System.now()
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
     * Share invoice via intent chooser
     */
    fun shareInvoice(file: File) {
        try {
            val intent = fileManager.createShareIntent(file)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Open invoice in external PDF viewer
     */
    fun viewInvoice(file: File) {
        val intent = fileManager.openPdfInViewer(file)
        context.startActivity(intent)
    }
    
    /**
     * Print invoice using Android Print Framework
     */
    fun printInvoice(file: File) {
        fileManager.printPdf(file, "Invoice")
    }
    
    private data class ShopSettings(
        val shopName: String,
        val shopAddress: String,
        val shopPhone: String,
        val shopGst: String
    )
}
