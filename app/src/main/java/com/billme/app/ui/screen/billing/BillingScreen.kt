package com.billme.app.ui.screen.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.data.local.entity.Product
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*
import com.billme.app.core.scanner.UnifiedIMEIScanner
import com.billme.app.core.scanner.IMEIScanMode
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    onNavigateBack: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel(),
    unifiedIMEIScanner: UnifiedIMEIScanner
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    var showSuccessDialog by remember { mutableStateOf(false) }
    var transactionId by remember { mutableStateOf<Long?>(null) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showSubtotalEditDialog by remember { mutableStateOf(false) }
    var showTotalEditDialog by remember { mutableStateOf(false) }
    var showIMEIScanDialog by remember { mutableStateOf(false) }
    var showTransliterationDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var transliteratedName by remember { mutableStateOf("") }
    var transliteratedAddress by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Create Bill",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showIMEIScanDialog = true }) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan IMEI"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        // Use a Column with proper layout management for responsiveness
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Customer Details, Search, and IMEI Chip in a scrollable container
            // These will scroll together if screen height is limited
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false) // Don't force fill, allow shrinking
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Customer Details Section
                item {
                    CustomerDetailsSection(
                        customerName = uiState.customerName,
                        customerPhone = uiState.customerPhone,
                        customerAddress = uiState.customerAddress,
                        onCustomerNameChange = viewModel::updateCustomerName,
                        onCustomerPhoneChange = viewModel::updateCustomerPhone,
                        onCustomerAddressChange = viewModel::updateCustomerAddress,
                        onPreviewHindiClick = {
                            // Generate Hindi preview with specialized methods
                            transliteratedName = com.billme.app.core.util.HindiTransliterator.transliterateFullName(uiState.customerName)
                            transliteratedAddress = com.billme.app.core.util.HindiTransliterator.transliterateAddress(uiState.customerAddress)
                            showTransliterationDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                item {
                    HorizontalDivider()
                }
                
                // Search Bar
                item {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        searchResults = searchResults,
                        onProductSelected = viewModel::addToCart,
                        onScanClick = { showIMEIScanDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Quick IMEI Scan Chip - More subtle
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Cart Items Header
                if (uiState.cartItems.isEmpty()) {
                    item {
                        EmptyCartPlaceholder()
                    }
                } else {
                    item {
                        Text(
                            text = "Cart Items (${uiState.cartItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(
                        items = uiState.cartItems,
                        key = { it.product.productId }
                    ) { cartItem ->
                        var showPriceEditDialog by remember { mutableStateOf(false) }
                        
                        // Fetch IMEI data if this cart item has an IMEI ID
                        val imeiData by produceState<com.billme.app.data.local.entity.ProductIMEI?>(null, cartItem.imeiId) {
                            value = cartItem.imeiId?.let { 
                                viewModel.getIMEIById(it)
                            }
                        }
                        
                        CartItemCard(
                            cartItem = cartItem,
                            imeiData = imeiData,
                            onIncrease = { viewModel.increaseQuantity(cartItem.product) },
                            onDecrease = { viewModel.decreaseQuantity(cartItem.product) },
                            onRemove = { viewModel.removeFromCart(cartItem.product) },
                            onEditPrice = { showPriceEditDialog = true }
                        )
                        
                        if (showPriceEditDialog) {
                            com.billme.app.ui.screen.inventory.PriceEditDialog(
                                productName = cartItem.product.productName,
                                currentPrice = cartItem.effectivePrice,
                                costPrice = cartItem.product.costPrice,
                                onDismiss = { showPriceEditDialog = false },
                                onConfirm = { newPrice ->
                                    viewModel.updateItemPrice(cartItem.product.productId, newPrice)
                                    showPriceEditDialog = false
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            // Fixed Bottom Section - Summary and Payment
            // This stays visible at the bottom regardless of scroll
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider()
                    
                    // Billing Summary
                    BillingSummary(
                        subtotal = uiState.subtotal,
                        discount = uiState.discount,
                        gstAmount = uiState.gstAmount,
                        total = uiState.total,
                        profit = uiState.profit,
                        onDiscountClick = { showDiscountDialog = true },
                        onSubtotalEdit = if (uiState.cartItems.isNotEmpty()) {
                            { showSubtotalEditDialog = true }
                        } else null,
                        onTotalEdit = if (uiState.cartItems.isNotEmpty()) {
                            { showTotalEditDialog = true }
                        } else null
                    )
                    
                    // Payment Methods
                    PaymentMethodButtons(
                        enabled = uiState.cartItems.isNotEmpty() && uiState.customerName.isNotBlank(),
                        onPaymentSelected = { method ->
                            viewModel.completeTransaction(method) { txnId ->
                                transactionId = txnId
                                showSuccessDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Discount Dialog
    if (showDiscountDialog) {
        DiscountDialog(
            currentDiscount = uiState.discount,
            maxDiscount = uiState.subtotal,
            onDismiss = { showDiscountDialog = false },
            onConfirm = { discount ->
                viewModel.updateDiscount(discount)
                showDiscountDialog = false
            }
        )
    }
    
    // Subtotal Edit Dialog
    if (showSubtotalEditDialog) {
        SubtotalEditDialog(
            currentSubtotal = uiState.subtotal,
            onDismiss = { showSubtotalEditDialog = false },
            onConfirm = { newSubtotal ->
                viewModel.updateSubtotal(newSubtotal)
                showSubtotalEditDialog = false
            }
        )
    }
    
    // Total Edit Dialog
    if (showTotalEditDialog) {
        TotalEditDialog(
            currentTotal = uiState.total,
            currentDiscount = uiState.discount,
            onDismiss = { showTotalEditDialog = false },
            onConfirm = { newTotal ->
                viewModel.updateTotal(newTotal)
                showTotalEditDialog = false
            }
        )
    }
    
    // Hindi Transliteration Preview Dialog
    if (showTransliterationDialog) {
        HindiTransliterationDialog(
            originalName = uiState.customerName,
            originalAddress = uiState.customerAddress,
            transliteratedName = transliteratedName,
            transliteratedAddress = transliteratedAddress,
            onDismiss = { showTransliterationDialog = false },
            onConfirm = { editedName, editedAddress ->
                transliteratedName = editedName
                transliteratedAddress = editedAddress
                // Store the corrected Hindi data in the viewModel for later use in PDF generation
                viewModel.updateHindiCustomerName(editedName)
                viewModel.updateHindiCustomerAddress(editedAddress)
                showTransliterationDialog = false
            }
        )
    }
    
    // Payment Method Dialog
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Select Payment Method") },
            text = {
                Column {
                    listOf("CASH", "EMI", "UPI", "OTHER").forEach { method ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.completeTransaction(method) { id ->
                                        transactionId = id
                                        showSuccessDialog = true
                                        showPaymentDialog = false
                                    }
                                }
                        ) {
                            Text(
                                text = method,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                    try {
                        val (customerCopy, ownerCopy) = viewModel.generateAndSaveBothCopies(txnId)
                        customerPdfFile = customerCopy
                        ownerPdfFile = ownerCopy
                        pdfGenerated = true
                    } catch (e: Exception) {
                        android.util.Log.e("BillingScreen", "Error generating invoice PDFs", e)
                        e.printStackTrace()
                        // Mark as generated to prevent infinite retry
                        pdfGenerated = true
                    }
                }
            }
            
            TransactionSuccessDialog(
                transactionId = txnId,
                customerName = uiState.customerName,
                total = uiState.total,
                onDismiss = {
                    showSuccessDialog = false
                    viewModel.clearCart()
                    onNavigateBack()
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
    
    // IMEI Quick Scan Dialog for Fast Billing
    if (showIMEIScanDialog) {
        UnifiedIMEIScannerDialog(
            onDismiss = { showIMEIScanDialog = false },
            onIMEIScanned = { imeis ->
                // For each scanned IMEI, find and add the product to cart
                imeis.forEach { imeiData ->
                    viewModel.addProductByIMEI(imeiData.imei)
                }
                showIMEIScanDialog = false
            },
            scanMode = IMEIScanMode.AUTO,
            scanner = unifiedIMEIScanner,
            showManualEntry = true,
            title = "Scan IMEI for Fast Billing"
        )
    }
}

@Composable
fun CustomerDetailsSection(
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onCustomerAddressChange: (String) -> Unit,
    onPreviewHindiClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enableHindiTransliteration: Boolean = false
) {
    var enableHindiTransliteration by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Compact Hindi toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "हिंदी",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (enableHindiTransliteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = enableHindiTransliteration,
                        onCheckedChange = { enableHindiTransliteration = it },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            
            // Customer Name Field - Cleaner design
            OutlinedTextField(
                value = customerName,
                onValueChange = onCustomerNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Customer Name") },
                placeholder = { Text("Enter name") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            
            // Live Hindi preview if enabled
            if (enableHindiTransliteration && customerName.isNotBlank() && 
                !com.billme.app.core.util.HindiTransliterator.containsHindi(customerName)) {
                val hindiPreview = com.billme.app.core.util.HindiTransliterator.transliterateFullName(customerName)
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = hindiPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Phone with contact icon
            OutlinedTextField(
                value = customerPhone,
                onValueChange = onCustomerPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone Number") },
                placeholder = { Text("Enter phone") },
                trailingIcon = {
                    Icon(
                        Icons.Default.ContactPhone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            
            // Address Field - More compact
            OutlinedTextField(
                value = customerAddress,
                onValueChange = onCustomerAddressChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Address (Optional)") },
                placeholder = { Text("Enter address") },
                minLines = 2,
                maxLines = 2,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            
            // Live Hindi preview for address if enabled
            if (enableHindiTransliteration && customerAddress.isNotBlank() && 
                !com.billme.app.core.util.HindiTransliterator.containsHindi(customerAddress)) {
                val hindiPreview = com.billme.app.core.util.HindiTransliterator.transliterateAddress(customerAddress)
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = hindiPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Preview Hindi button - Only show when transliteration is enabled
            if (enableHindiTransliteration && customerName.isNotBlank()) {
                OutlinedButton(
                    onClick = onPreviewHindiClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Translate, 
                        contentDescription = null, 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Preview & Edit Hindi",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
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
    onScanClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Products Header
        Text(
            text = "Products",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search Field with icons
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        expanded = it.isNotEmpty()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search product or scan IMEI") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onScanClick) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )
                
                // Search Results Dropdown
                if (expanded && searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
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
                                if (product != searchResults.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "Brand: ${product.brand}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("•", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Model: ${product.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Compact Color and Variant badges
            if (!product.color.isNullOrBlank() || !product.variant.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (!product.color.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "🎨 ${product.color}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!product.variant.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "💾 ${product.variant}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Price and Stock
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${NumberFormat.getInstance().format(product.sellingPrice)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Stock: ${product.currentStock}",
                style = MaterialTheme.typography.labelSmall,
                color = if (product.currentStock > 0) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
fun EmptyCartPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cart is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = "Search and add products to cart",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun CartItemCard(
    cartItem: com.billme.app.ui.screen.billing.CartItem,
    imeiData: com.billme.app.data.local.entity.ProductIMEI? = null,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onEditPrice: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Product Name and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cartItem.product.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Brand: ${cartItem.product.brand}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Model: ${cartItem.product.model}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Color and Variant Row
            if (!cartItem.product.color.isNullOrBlank() || !cartItem.product.variant.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    if (!cartItem.product.color.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "🎨 ${cartItem.product.color}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (!cartItem.product.variant.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "💾 ${cartItem.product.variant}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Display IMEI information if available
            if (imeiData != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "IMEI 1:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = imeiData.imeiNumber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    if (!imeiData.imei2Number.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "IMEI 2:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = imeiData.imei2Number,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bottom Row: Price, Quantity Controls, Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price with edit button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Price:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${NumberFormat.getInstance().format(cartItem.effectivePrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onEditPrice,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Price",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Quantity Controls - More compact
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        IconButton(
                            onClick = onDecrease,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Text(
                            text = cartItem.quantity.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 24.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        IconButton(
                            onClick = onIncrease,
                            modifier = Modifier.size(32.dp),
                            enabled = cartItem.quantity < cartItem.product.currentStock
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(16.dp),
                                tint = if (cartItem.quantity < cartItem.product.currentStock) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BillingSummary(
    subtotal: BigDecimal,
    discount: BigDecimal,
    gstAmount: BigDecimal,
    total: BigDecimal,
    profit: BigDecimal,
    onDiscountClick: () -> Unit,
    onSubtotalEdit: (() -> Unit)? = null,
    onTotalEdit: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Subtotal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Subtotal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹${NumberFormat.getInstance().format(subtotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (onSubtotalEdit != null) {
                    IconButton(
                        onClick = onSubtotalEdit,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Subtotal",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Discount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Discount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (discount > BigDecimal.ZERO) "-₹${NumberFormat.getInstance().format(discount)}" else "-₹0.00",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (discount > BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
                IconButton(
                    onClick = onDiscountClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Discount",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // GST Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GST (18%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${NumberFormat.getInstance().format(gstAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        
        // Total
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹${NumberFormat.getInstance().format(total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onTotalEdit != null) {
                    IconButton(
                        onClick = onTotalEdit,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Total",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: BigDecimal, compact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "₹${NumberFormat.getInstance().format(amount)}",
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PaymentMethodButtons(
    enabled: Boolean,
    onPaymentSelected: (String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaymentButton(
                text = "Cash",
                icon = Icons.Default.Money,
                enabled = enabled,
                isSelected = selectedMethod == "CASH",
                onClick = { 
                    selectedMethod = "CASH"
                    onPaymentSelected("CASH") 
                },
                modifier = Modifier.weight(1f)
            )
            PaymentButton(
                text = "Card",
                icon = Icons.Default.CreditCard,
                enabled = enabled,
                isSelected = selectedMethod == "CARD",
                onClick = { 
                    selectedMethod = "CARD"
                    onPaymentSelected("CARD") 
                },
                modifier = Modifier.weight(1f)
            )
            // UPI button with custom text logo
            UpiPaymentButton(
                enabled = enabled,
                isSelected = selectedMethod == "UPI",
                onClick = { 
                    selectedMethod = "UPI"
                    onPaymentSelected("UPI") 
                },
                modifier = Modifier.weight(1f)
            )
            PaymentButton(
                text = "EMI",
                icon = Icons.Default.AccountBalance,
                enabled = enabled,
                isSelected = selectedMethod == "EMI",
                onClick = { 
                    selectedMethod = "EMI"
                    onPaymentSelected("EMI") 
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PaymentButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(32.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun UpiPaymentButton(
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Custom UPI logo using text
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                else 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "UPI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "UPI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun DiscountDialog(
    currentDiscount: BigDecimal,
    maxDiscount: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var discountText by remember { mutableStateOf(currentDiscount.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Apply Discount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = discountText,
                    onValueChange = {
                        discountText = it
                        errorMessage = null
                    },
                    label = { Text("Discount Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Max discount:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${NumberFormat.getInstance().format(maxDiscount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val discount = discountText.toBigDecimalOrNull()
                    when {
                        discount == null -> errorMessage = "Invalid amount"
                        discount < BigDecimal.ZERO -> errorMessage = "Discount cannot be negative"
                        discount > maxDiscount -> errorMessage = "Discount exceeds subtotal"
                        else -> onConfirm(discount)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SubtotalEditDialog(
    currentSubtotal: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var subtotalText by remember { mutableStateOf(currentSubtotal.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Edit Subtotal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Adjust the subtotal amount manually. This will override the calculated cart total.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = subtotalText,
                    onValueChange = {
                        subtotalText = it
                        errorMessage = null
                    },
                    label = { Text("New Subtotal") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Subtotal:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "₹${NumberFormat.getInstance().format(currentSubtotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newSubtotal = subtotalText.toBigDecimalOrNull()
                    when {
                        newSubtotal == null -> errorMessage = "Invalid amount"
                        newSubtotal <= BigDecimal.ZERO -> errorMessage = "Subtotal must be greater than zero"
                        else -> onConfirm(newSubtotal)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun TotalEditDialog(
    currentTotal: BigDecimal,
    currentDiscount: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var totalText by remember { mutableStateOf(currentTotal.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Edit Total Amount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Set the final total amount. Subtotal and GST will be automatically recalculated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = totalText,
                    onValueChange = {
                        totalText = it
                        errorMessage = null
                    },
                    label = { Text("Total Amount") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Current Total:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "₹${NumberFormat.getInstance().format(currentTotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (currentDiscount > BigDecimal.ZERO) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Current Discount:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "₹${NumberFormat.getInstance().format(currentDiscount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newTotal = totalText.toBigDecimalOrNull()
                    when {
                        newTotal == null -> errorMessage = "Invalid amount"
                        newTotal <= BigDecimal.ZERO -> errorMessage = "Total must be greater than zero"
                        else -> onConfirm(newTotal)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun TransactionSuccessDialog(
    transactionId: Long?,
    customerName: String,
    total: BigDecimal,
    onDismiss: () -> Unit,
    onPrintReceipt: () -> Unit,
    onShareInvoice: () -> Unit = {},
    onViewInvoice: () -> Unit = {},
    onPrintInvoice: () -> Unit = {}
) {
    var isGeneratingPdf by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Bill Created Successfully!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                transactionId?.let {
                    Text(
                        text = "Transaction ID: #$it",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = "Customer: $customerName",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Total: ₹${NumberFormat.getInstance().format(total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Invoice Actions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Share Invoice Button
                Button(
                    onClick = {
                        isGeneratingPdf = true
                        onShareInvoice()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGeneratingPdf
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGeneratingPdf) "Generating..." else "Share Invoice")
                }
                
                // Print Invoice Button  
                OutlinedButton(
                    onClick = onPrintInvoice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print Invoice")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View Invoice Button
                    OutlinedButton(
                        onClick = onViewInvoice,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View")
                    }
                    
                    // Print Receipt (Thermal)
                    OutlinedButton(
                        onClick = {
                            onPrintReceipt()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thermal")
                    }
                }
                
                // Done Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    )
}

@Composable
fun HindiTransliterationDialog(
    originalName: String,
    originalAddress: String,
    transliteratedName: String,
    transliteratedAddress: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var editedName by remember { mutableStateOf(transliteratedName) }
    var editedAddress by remember { mutableStateOf(transliteratedAddress) }
    
    // Get confidence scores
    val nameConfidence = remember(originalName) {
        if (originalName.isNotBlank()) {
            com.billme.app.core.util.HindiTransliterator.getConfidenceScore(originalName)
        } else 1.0f
    }
    val addressConfidence = remember(originalAddress) {
        if (originalAddress.isNotBlank()) {
            com.billme.app.core.util.HindiTransliterator.getConfidenceScore(originalAddress)
        } else 1.0f
    }
    
    // Get alternative suggestions
    val nameSuggestions = remember(originalName) {
        if (originalName.isNotBlank()) {
            com.billme.app.core.util.HindiTransliterator.getSuggestionsWithConfidence(originalName, isAddress = false)
        } else emptyList()
    }
    val addressSuggestions = remember(originalAddress) {
        if (originalAddress.isNotBlank()) {
            com.billme.app.core.util.HindiTransliterator.getSuggestionsWithConfidence(originalAddress, isAddress = true)
        } else emptyList()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hindi Transliteration Preview")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // NAME SECTION
                if (originalName.isNotBlank()) {
                    Column {
                        Text(
                            text = "Original Name:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = originalName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Confidence indicator
                        Spacer(Modifier.height(4.dp))
                        ConfidenceIndicator(nameConfidence)
                    }
                    
                    // Transliterated Name (Editable)
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Hindi Name (Edit if needed)") },
                        supportingText = { 
                            Text(
                                if (nameConfidence > 0.7f) 
                                    "✓ High confidence transliteration" 
                                else 
                                    "⚠ Please verify - low confidence"
                            )
                        },
                        singleLine = true
                    )
                    
                    // Show alternative suggestions if confidence is low
                    if (nameConfidence < 0.7f && nameSuggestions.size > 1) {
                        Text(
                            text = "Alternative Suggestions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        nameSuggestions.drop(1).take(2).forEach { (suggestion, confidence) ->
                            SuggestionChip(
                                suggestion = suggestion,
                                confidence = confidence,
                                onClick = { editedName = suggestion },
                                isSelected = editedName == suggestion
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                // ADDRESS SECTION
                if (originalAddress.isNotBlank()) {
                    Column {
                        Text(
                            text = "Original Address:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = originalAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Confidence indicator
                        Spacer(Modifier.height(4.dp))
                        ConfidenceIndicator(addressConfidence)
                    }
                    
                    // Transliterated Address (Editable)
                    OutlinedTextField(
                        value = editedAddress,
                        onValueChange = { editedAddress = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Hindi Address (Edit if needed)") },
                        supportingText = { 
                            Text(
                                if (addressConfidence > 0.7f) 
                                    "✓ High confidence transliteration" 
                                else 
                                    "⚠ Please verify - low confidence"
                            )
                        },
                        minLines = 2,
                        maxLines = 3
                    )
                    
                    // Show alternative suggestions if confidence is low
                    if (addressConfidence < 0.7f && addressSuggestions.size > 1) {
                        Text(
                            text = "Alternative Suggestions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        addressSuggestions.drop(1).take(2).forEach { (suggestion, confidence) ->
                            SuggestionChip(
                                suggestion = suggestion,
                                confidence = confidence,
                                onClick = { editedAddress = suggestion },
                                isSelected = editedAddress == suggestion
                            )
                        }
                    }
                }
                
                // Help text
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "This Hindi text will appear on the invoice. Edit or select an alternative if the transliteration is incorrect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(editedName, editedAddress) }
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Use This")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfidenceIndicator(confidence: Float) {
    val color = when {
        confidence >= 0.8f -> Color(0xFF4CAF50) // Green
        confidence >= 0.6f -> Color(0xFFFFC107) // Amber
        else -> Color(0xFFFF9800) // Orange
    }
    val label = when {
        confidence >= 0.8f -> "High Accuracy"
        confidence >= 0.6f -> "Medium Accuracy"
        else -> "Low Accuracy - Please Verify"
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LinearProgressIndicator(
            progress = { confidence },
            modifier = Modifier
                .width(100.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SuggestionChip(
    suggestion: String,
    confidence: Float,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    Surface(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = suggestion,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
