package com.billme.app.ui.screen.quickbill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.data.local.entity.Product
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickBillScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: QuickBillViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    var showSuccessDialog by remember { mutableStateOf(false) }
    var transactionId by remember { mutableStateOf<Long?>(null) }
    var showUnlistedProductDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Quick Bill",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (uiState.cartItems.isNotEmpty()) {
                            Text(
                                text = "${uiState.cartItems.size} items • Total: ₹${formatCurrency(uiState.total)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    AnimatedIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    if (uiState.cartItems.isNotEmpty()) {
                        NotificationBadge(
                            count = uiState.cartItems.size,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isLandscape = maxWidth > maxHeight
            
            if (isLandscape) {
                // Two-column layout for tablets/landscape
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Left: Product search and cart
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Search Bar
                        EnhancedSearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            searchResults = searchResults,
                            onProductSelected = viewModel::addToCart,
                            onAddUnlistedProduct = { showUnlistedProductDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                        
                        // Cart Items
                        CartItemsList(
                            cartItems = uiState.cartItems,
                            onIncrease = viewModel::increaseQuantity,
                            onDecrease = viewModel::decreaseQuantity,
                            onRemove = viewModel::removeFromCart,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Right: Billing summary and payment
                    Surface(
                        modifier = Modifier
                            .width(400.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            EnhancedBillingSummary(
                                subtotal = uiState.subtotal,
                                discount = uiState.discount,
                                gstAmount = uiState.gstAmount,
                                total = uiState.total,
                                profit = uiState.profit,
                                onDiscountChange = viewModel::updateDiscount,
                                modifier = Modifier.weight(1f)
                            )
                            
                            EnhancedPaymentButtons(
                                enabled = uiState.cartItems.isNotEmpty(),
                                onPaymentSelected = { method ->
                                    viewModel.completeTransaction(method.name) { txnId ->
                                        transactionId = txnId
                                        showSuccessDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Single-column layout for portrait/phones
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Search Bar
                    EnhancedSearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        searchResults = searchResults,
                        onProductSelected = viewModel::addToCart,
                        onAddUnlistedProduct = { showUnlistedProductDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    
                    // Cart Items
                    CartItemsList(
                        cartItems = uiState.cartItems,
                        onIncrease = viewModel::increaseQuantity,
                        onDecrease = viewModel::decreaseQuantity,
                        onRemove = viewModel::removeFromCart,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Billing Summary (Compact)
                    CompactBillingSummary(
                        subtotal = uiState.subtotal,
                        discount = uiState.discount,
                        total = uiState.total,
                        onDiscountChange = viewModel::updateDiscount
                    )
                    
                    // Payment Methods
                    EnhancedPaymentButtons(
                        enabled = uiState.cartItems.isNotEmpty(),
                        onPaymentSelected = { method ->
                            viewModel.completeTransaction(method.name) { txnId ->
                                transactionId = txnId
                                showSuccessDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Success Dialog
    transactionId?.let { txnId ->
        if (showSuccessDialog) {
            var customerPdfFile by remember { mutableStateOf<java.io.File?>(null) }
            var ownerPdfFile by remember { mutableStateOf<java.io.File?>(null) }
            var pdfGenerated by remember { mutableStateOf(false) }
            
            // Auto-generate both copies when dialog opens (only once per transaction)
            LaunchedEffect(txnId, showSuccessDialog) {
                if (!pdfGenerated && showSuccessDialog) {
                    val (customerCopy, ownerCopy) = viewModel.generateAndSaveBothCopies(txnId)
                    customerPdfFile = customerCopy
                    ownerPdfFile = ownerCopy
                    pdfGenerated = true
                }
            }
            
            TransactionSuccessDialog(
                transactionId = txnId,
                total = uiState.total,
                onDismiss = {
                    showSuccessDialog = false
                    viewModel.clearCart()
                },
                onPrintReceipt = {
                    viewModel.printReceipt(txnId)
                },
                onShareInvoice = {
                    customerPdfFile?.let { file ->
                        viewModel.shareInvoice(file)
                    }
                },
                onViewInvoice = {
                    customerPdfFile?.let { file ->
                        viewModel.viewInvoice(file)
                    }
                },
                onPrintInvoice = {
                    customerPdfFile?.let { file ->
                        viewModel.printInvoice(file)
                    }
                }
            )
        }
    }
    
    // Unlisted Product Dialog
    if (showUnlistedProductDialog) {
        UnlistedProductDialog(
            onDismiss = { showUnlistedProductDialog = false },
            onAdd = { productName, price, quantity ->
                viewModel.addUnlistedProduct(productName, price, quantity)
                showUnlistedProductDialog = false
            }
        )
    }
    
    // IMEI Selection Dialog
    uiState.productForIMEISelection?.let { product ->
        if (uiState.showIMEISelection) {
            val availableIMEIs by produceState<List<com.billme.app.data.local.entity.ProductIMEI>>(emptyList(), product.productId) {
                value = viewModel.getAvailableIMEIs(product.productId)
            }
            
            IMEISelectionDialog(
                productName = product.productName,
                availableIMEIs = availableIMEIs,
                onDismiss = viewModel::dismissIMEISelection,
                onIMEISelected = { imei ->
                    viewModel.addToCartWithIMEI(product, imei.imeiId)
                    viewModel.dismissIMEISelection()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<Product>,
    onProductSelected: (Product) -> Unit,
    onAddUnlistedProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    expanded = it.isNotEmpty()
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search products...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Add Unlisted Product Button
            IconButton(
                onClick = onAddUnlistedProduct,
                modifier = Modifier
                    .size(56.dp)
                    .padding(top = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Unlisted Product",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Search Results Dropdown
        if (expanded && searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn {
                    items(searchResults) { product ->
                        SearchResultItem(
                            product = product,
                            onClick = {
                                onProductSelected(product)
                                onQueryChange("")
                                expanded = false
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    product: Product,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.productName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${product.brand} • ${product.model}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Stock: ${product.currentStock}",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.currentStock > product.minStockLevel) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
        }
        Text(
            text = formatCurrency(product.sellingPrice),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 5.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.productName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cartItem.product.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatCurrency(cartItem.product.sellingPrice)} × ${cartItem.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quantity Controls
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "${cartItem.quantity}",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(32.dp),
                        enabled = cartItem.quantity < cartItem.product.currentStock
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Remove Button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                // Total Price
                Text(
                    text = formatCurrency(cartItem.totalPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 80.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyCartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Cart is empty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Search and add products to start billing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingSummary(
    subtotal: BigDecimal,
    discount: BigDecimal,
    gstAmount: BigDecimal,
    total: BigDecimal,
    profit: BigDecimal,
    onDiscountChange: (BigDecimal) -> Unit
) {
    var discountText by remember { mutableStateOf("") }
    var showDiscountDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow("Subtotal", formatCurrency(subtotal))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Discount", style = MaterialTheme.typography.bodyMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "- ${formatCurrency(discount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(
                        onClick = { showDiscountDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit discount",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            SummaryRow("GST (18%)", formatCurrency(gstAmount))
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatCurrency(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            SummaryRow(
                "Profit",
                formatCurrency(profit),
                valueColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    
    if (showDiscountDialog) {
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Enter Discount") },
            text = {
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("Discount Amount") },
                    prefix = { Text("₹") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val discountValue = discountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        onDiscountChange(discountValue)
                        showDiscountDialog = false
                        discountText = ""
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun PaymentMethodButtons(
    enabled: Boolean,
    onPaymentSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onPaymentSelected("CASH") },
            modifier = Modifier.weight(1f),
            enabled = enabled
        ) {
            Icon(Icons.Default.Money, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cash")
        }
        
        Button(
            onClick = { onPaymentSelected("CARD") },
            modifier = Modifier.weight(1f),
            enabled = enabled
        ) {
            Icon(Icons.Default.CreditCard, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Card")
        }
        
        Button(
            onClick = { onPaymentSelected("UPI") },
            modifier = Modifier.weight(1f),
            enabled = enabled
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("UPI")
        }
    }
}

@Composable
fun TransactionSuccessDialog(
    transactionId: Long?,
    total: BigDecimal,
    onDismiss: () -> Unit,
    onPrintReceipt: () -> Unit,
    onShareInvoice: () -> Unit = {},
    onViewInvoice: () -> Unit = {},
    onPrintInvoice: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Transaction Successful!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                transactionId?.let {
                    Text("Transaction ID: #$it", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = formatCurrency(total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShareInvoice,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onViewInvoice,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrintInvoice,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print PDF", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onPrintReceipt,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Receipt", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun formatCurrency(amount: BigDecimal): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.format(amount.toDouble())
    } catch (e: Exception) {
        // Fallback to simple formatting
        "₹${amount.setScale(2, java.math.RoundingMode.HALF_UP)}"
    }
}

fun formatCurrency(amount: Double): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.format(amount)
    } catch (e: Exception) {
        // Fallback to simple formatting
        "₹${"%.2f".format(amount)}"
    }
}

@Composable
fun UnlistedProductDialog(
    onDismiss: () -> Unit,
    onAdd: (productName: String, price: BigDecimal, quantity: Int) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var showError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Unlisted Product") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (showError) {
                    Text(
                        "Please fill all fields with valid values",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceValue = price.toBigDecimalOrNull()
                    val quantityValue = quantity.toIntOrNull()
                    
                    if (productName.isBlank() || priceValue == null || quantityValue == null || quantityValue <= 0) {
                        showError = true
                    } else {
                        onAdd(productName, priceValue, quantityValue)
                    }
                }
            ) {
                Text("Add to Cart")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Enhanced Responsive Components
@Composable
fun EnhancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<Product>,
    onProductSelected: (Product) -> Unit,
    onAddUnlistedProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchBar(
        query, onQueryChange, searchResults, onProductSelected, onAddUnlistedProduct, modifier
    )
}

@Composable
fun CartItemsList(
    cartItems: List<CartItem>,
    onIncrease: (Product) -> Unit,
    onDecrease: (Product) -> Unit,
    onRemove: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (cartItems.isEmpty()) {
            item {
                EmptyCartPlaceholder()
            }
        } else {
            items(
                items = cartItems,
                key = { it.product.productId }
            ) { cartItem ->
                EnhancedCartItemCard(
                    cartItem = cartItem,
                    onIncrease = { onIncrease(cartItem.product) },
                    onDecrease = { onDecrease(cartItem.product) },
                    onRemove = { onRemove(cartItem.product) }
                )
            }
        }
    }
}

@Composable
fun EnhancedCartItemCard(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cartItem.product.brand} • ${cartItem.product.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(cartItem.product.sellingPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledIconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.widthIn(min = 40.dp)
                    ) {
                        Text(
                            text = "${cartItem.quantity}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    FilledIconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Text(
                    text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(cartItem.totalPrice)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedBillingSummary(
    subtotal: BigDecimal,
    discount: BigDecimal,
    gstAmount: BigDecimal,
    total: BigDecimal,
    profit: BigDecimal,
    onDiscountChange: (BigDecimal) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Bill Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BillRow("Subtotal", subtotal)
                BillRow("GST (18%)", gstAmount, color = MaterialTheme.colorScheme.tertiary)
                if (discount > BigDecimal.ZERO) {
                    BillRow("Discount", discount, color = MaterialTheme.colorScheme.error, prefix = "-")
                }
                HorizontalDivider(thickness = 2.dp)
                BillRow("Total", total, isTotal = true)
                if (profit > BigDecimal.ZERO) {
                    BillRow("Profit", profit, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
fun CompactBillingSummary(
    subtotal: BigDecimal,
    discount: BigDecimal,
    total: BigDecimal,
    onDiscountChange: (BigDecimal) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(subtotal)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (discount > BigDecimal.ZERO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "-₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(discount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 2.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total:",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(total)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun BillRow(
    label: String,
    amount: BigDecimal,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    isTotal: Boolean = false,
    prefix: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
        Text(
            text = "$prefix₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(amount)}",
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EnhancedPaymentButtons(
    enabled: Boolean,
    onPaymentSelected: (com.billme.app.data.local.entity.PaymentMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { onPaymentSelected(com.billme.app.data.local.entity.PaymentMethod.CASH) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Paid, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pay Cash", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onPaymentSelected(com.billme.app.data.local.entity.PaymentMethod.CARD) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = enabled
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Card")
            }
            
            OutlinedButton(
                onClick = { onPaymentSelected(com.billme.app.data.local.entity.PaymentMethod.UPI) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = enabled
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("UPI")
            }
            
            OutlinedButton(
                onClick = { onPaymentSelected(com.billme.app.data.local.entity.PaymentMethod.EMI) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = enabled
            ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("EMI")
            }
        }
    }
}
