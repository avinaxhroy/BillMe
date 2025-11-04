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
            ModernTopAppBar(
                title = "Create Bill",
                subtitle = if (uiState.cartItems.isNotEmpty()) "${uiState.cartItems.size} items in cart" else "Add products to cart",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                useGradient = true,
                actions = {
                    // IMEI Quick Scan Button with badge
                    Box {
                        IconButton(onClick = { showIMEIScanDialog = true }) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan IMEI",
                                tint = Color.White
                            )
                        }
                        if (uiState.cartItems.isNotEmpty()) {
                            CounterBadge(
                                count = uiState.cartItems.size,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                            )
                        }
                    }
                }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Quick IMEI Scan Chip
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AssistChip(
                            onClick = { showIMEIScanDialog = true },
                            label = { Text("Quick Add by IMEI Scan") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
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
                        
                        CartItemCard(
                            cartItem = cartItem,
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
                    listOf("CASH", "CARD", "UPI", "OTHER").forEach { method ->
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
                    val (customerCopy, ownerCopy) = viewModel.generateAndSaveBothCopies(txnId)
                    customerPdfFile = customerCopy
                    ownerPdfFile = ownerCopy
                    pdfGenerated = true
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Customer Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Toggle for Hindi transliteration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hindi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = enableHindiTransliteration,
                        onCheckedChange = { enableHindiTransliteration = it }
                    )
                }
            }
            
            // Customer Name Field with Hindi support
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { newValue ->
                        onCustomerNameChange(newValue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { 
                        Text(if (enableHindiTransliteration) "Customer Name * (हिंदी में)" else "Customer Name *") 
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                
                // Live transliteration preview
                if (enableHindiTransliteration && customerName.isNotBlank() && 
                    !com.billme.app.core.util.HindiTransliterator.containsHindi(customerName)) {
                    val hindiPreview = com.billme.app.core.util.HindiTransliterator.transliterateFullName(customerName)
                    val confidence = com.billme.app.core.util.HindiTransliterator.getConfidenceScore(customerName)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = if (confidence > 0.7f) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = hindiPreview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Confidence indicator
                            if (confidence >= 0.7f) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "High confidence",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else if (confidence >= 0.4f) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Medium confidence",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
            
            OutlinedTextField(
                value = customerPhone,
                onValueChange = onCustomerPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone Number (Optional)") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            
            // Customer Address Field with Hindi support
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customerAddress,
                    onValueChange = { newValue ->
                        onCustomerAddressChange(newValue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { 
                        Text(if (enableHindiTransliteration) "Address (Optional) (हिंदी में)" else "Address (Optional)") 
                    },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                
                // Live transliteration preview for address
                if (enableHindiTransliteration && customerAddress.isNotBlank() && 
                    !com.billme.app.core.util.HindiTransliterator.containsHindi(customerAddress)) {
                    val hindiPreview = com.billme.app.core.util.HindiTransliterator.transliterateAddress(customerAddress)
                    val addressConfidence = com.billme.app.core.util.HindiTransliterator.getConfidenceScore(customerAddress)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = if (addressConfidence > 0.7f)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = hindiPreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            // Preview Hindi button
            if (customerName.isNotBlank()) {
                Button(
                    onClick = onPreviewHindiClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Preview Hindi / Edit (हिंदी में देखें)")
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ModernSearchField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    expanded = it.isNotEmpty()
                },
                placeholder = "Search products by name, brand, IMEI...",
                modifier = Modifier.fillMaxWidth()
            )
            
            // Search Results Dropdown
            if (expanded && searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    shape = RoundedCornerShape(12.dp)
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
                            ModernDivider()
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
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.productName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${product.brand} - ${product.model}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Color and Variant Info
            if (!product.color.isNullOrBlank() || !product.variant.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (!product.color.isNullOrBlank()) {
                        Text(
                            text = "🎨 ${product.color}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!product.variant.isNullOrBlank()) {
                        Text(
                            text = "💾 ${product.variant}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            Text(
                text = "Stock: ${product.currentStock}",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.currentStock > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        Text(
            text = "₹${NumberFormat.getInstance().format(product.sellingPrice)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
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
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onEditPrice: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Info - More compact
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.productName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cartItem.product.brand} - ${cartItem.product.model}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Color and Variant
                if (!cartItem.product.color.isNullOrBlank() || !cartItem.product.variant.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        if (!cartItem.product.color.isNullOrBlank()) {
                            Text(
                                text = "🎨 ${cartItem.product.color}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!cartItem.product.variant.isNullOrBlank()) {
                            Text(
                                text = "💾 ${cartItem.product.variant}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${NumberFormat.getInstance().format(cartItem.effectivePrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (cartItem.customPrice != null) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text("Custom", style = MaterialTheme.typography.labelSmall)
                        }
                    }
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
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Quantity Controls - Compact horizontal layout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = cartItem.quantity.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 20.dp)
                )
                
                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(28.dp),
                    enabled = cartItem.quantity < cartItem.product.currentStock
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(18.dp),
                        tint = if (cartItem.quantity < cartItem.product.currentStock) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Total - Compact
            Text(
                text = "₹${NumberFormat.getInstance().format(cartItem.totalPrice)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 60.dp)
            )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Subtotal with optional edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtotal",
                    style = MaterialTheme.typography.bodyMedium
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
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Subtotal",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discount",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "- ₹${NumberFormat.getInstance().format(discount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(
                        onClick = onDiscountClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Discount",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            SummaryRow("GST (18%)", gstAmount, compact = true)
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "₹${NumberFormat.getInstance().format(total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (onTotalEdit != null) {
                        IconButton(
                            onClick = onTotalEdit,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Total",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            if (profit > BigDecimal.ZERO) {
                Text(
                    text = "Profit: ₹${NumberFormat.getInstance().format(profit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.End)
                )
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedMethod != null) {
                    Text(
                        text = "✓ Selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
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
                PaymentButton(
                    text = "UPI",
                    icon = Icons.Default.QrCode2,
                    enabled = enabled,
                    isSelected = selectedMethod == "UPI",
                    onClick = { 
                        selectedMethod = "UPI"
                        onPaymentSelected("UPI") 
                    },
                    modifier = Modifier.weight(1f)
                )
            }
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
        modifier = modifier.height(72.dp),
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
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(28.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
        title = { Text("Apply Discount") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Max discount: ₹${NumberFormat.getInstance().format(maxDiscount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val discount = discountText.toBigDecimalOrNull()
                    when {
                        discount == null -> errorMessage = "Invalid amount"
                        discount < BigDecimal.ZERO -> errorMessage = "Discount cannot be negative"
                        discount > maxDiscount -> errorMessage = "Discount exceeds subtotal"
                        else -> onConfirm(discount)
                    }
                }
            ) {
                Text("Apply")
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
fun SubtotalEditDialog(
    currentSubtotal: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var subtotalText by remember { mutableStateOf(currentSubtotal.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Subtotal / Selling Price") },
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
                    modifier = Modifier.fillMaxWidth()
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                
                Text(
                    text = "⚠️ Note: Manual adjustments will affect profit calculations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newSubtotal = subtotalText.toBigDecimalOrNull()
                    when {
                        newSubtotal == null -> errorMessage = "Invalid amount"
                        newSubtotal <= BigDecimal.ZERO -> errorMessage = "Subtotal must be greater than zero"
                        else -> onConfirm(newSubtotal)
                    }
                }
            ) {
                Text("Update")
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
        title = { Text("Edit Total Amount") },
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
                    modifier = Modifier.fillMaxWidth()
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Current Total:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "₹${NumberFormat.getInstance().format(currentTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "₹${NumberFormat.getInstance().format(currentDiscount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            "Note: Discount will be removed when editing total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newTotal = totalText.toBigDecimalOrNull()
                    when {
                        newTotal == null -> errorMessage = "Invalid amount"
                        newTotal <= BigDecimal.ZERO -> errorMessage = "Total must be greater than zero"
                        else -> onConfirm(newTotal)
                    }
                }
            ) {
                Text("Update")
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
