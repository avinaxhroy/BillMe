package com.billme.app.ui.screen.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.data.local.entity.Product
import com.billme.app.core.scanner.UnifiedIMEIScanner
import com.billme.app.core.scanner.IMEIScanMode
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*
import com.billme.app.core.util.formatLocale
import com.billme.app.core.util.formatCompactCurrency
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddPurchase: () -> Unit,
    onNavigateToAddProduct: () -> Unit = {},
    viewModel: InventoryViewModel = hiltViewModel(),
    unifiedIMEIScanner: UnifiedIMEIScanner
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showSortMenu by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showStockEditDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var showIMEIScanDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAddIMEIDialog by remember { mutableStateOf(false) }
    var showEditIMEIDialog by remember { mutableStateOf(false) }
    var showEditProductDialog by remember { mutableStateOf(false) }
    var selectedIMEI by remember { mutableStateOf<com.billme.app.data.local.entity.ProductIMEI?>(null) }
    
    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                // Selection mode toolbar
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                "${uiState.selectedProductIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Bulk actions available",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection mode")
                        }
                    },
                    actions = {
                        // Select All
                        IconButton(onClick = { viewModel.selectAllProducts() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        
                        // Delete Selected
                        IconButton(onClick = { viewModel.deleteSelectedProducts() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                // Normal mode toolbar - Modern clean design
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                "Inventory",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Manage your products with ease.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // More Actions
                        Box {
                            IconButton(onClick = { showActionsMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Actions")
                            }
                            
                            DropdownMenu(
                                expanded = showActionsMenu,
                                onDismissRequest = { showActionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Bulk Import") },
                                    onClick = {
                                        showBulkImportDialog = true
                                        showActionsMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Upload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Inventory") },
                                    onClick = {
                                        showExportDialog = true
                                        showActionsMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Download, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Selection Mode") },
                                    onClick = {
                                        viewModel.enterSelectionMode()
                                        showActionsMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                // Add Product FAB - Large and prominent
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddProduct,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    expanded = true,
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    text = {
                        Text(
                            "Add Product",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        // Single scrollable LazyColumn containing everything
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Statistics Cards
            item {
                InventoryStatistics(
                    totalProducts = uiState.totalProducts,
                    lowStockCount = uiState.lowStockCount,
                    totalValue = uiState.totalValue
                )
            }
            
            // Search Bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            // Category Filter
            item {
                CategoryFilter(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            // Low Stock Filter Chip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.showLowStockOnly,
                        onClick = viewModel::toggleLowStockFilter,
                        label = { Text("Low Stock Only", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.showLowStockOnly) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.error,
                            selectedLabelColor = Color.White
                        )
                    )
                    
                    Text(
                        text = "${uiState.filteredProducts.size} items",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Product List or Empty Placeholder
            if (uiState.filteredProducts.isEmpty()) {
                item {
                    EmptyInventoryPlaceholder()
                }
            } else {
                items(
                    items = uiState.filteredProducts,
                    key = { it.productId }
                ) { product ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ExpandableProductCard(
                            product = product,
                            imeis = uiState.productIMEIs[product.productId],
                            isExpanded = uiState.expandedProductId == product.productId,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = uiState.selectedProductIds.contains(product.productId),
                            onToggleExpand = { 
                                viewModel.toggleProductExpansion(product.productId)
                            },
                            onToggleSelection = {
                                viewModel.toggleProductSelection(product.productId)
                            },
                            onEditProduct = {
                                selectedProduct = product
                                showEditProductDialog = true
                            },
                            onDeleteProduct = {
                                viewModel.deleteProduct(product)
                            },
                            onAddIMEI = {
                                selectedProduct = product
                                showAddIMEIDialog = true
                            },
                            onEditIMEI = { imei ->
                                selectedIMEI = imei
                                showEditIMEIDialog = true
                            },
                            onDeleteIMEI = { imei ->
                                viewModel.deleteIMEI(imei)
                            }
                        )
                    }
                }
                
                // Bottom padding for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Sort Menu
        if (showSortMenu) {
            SortDialog(
                currentSort = uiState.sortBy,
                onSortSelected = {
                    viewModel.onSortByChanged(it)
                    showSortMenu = false
                },
                onDismiss = { showSortMenu = false }
            )
        }
        
        // Product Details Dialog
        selectedProduct?.let { product ->
            ProductDetailsDialog(
                product = product,
                onDismiss = { selectedProduct = null },
                onEditStock = { 
                    showStockEditDialog = true
                },
                onDeactivate = {
                    viewModel.deactivateProduct(product.productId)
                    selectedProduct = null
                }
            )
        }
        
        // Stock Edit Dialog
        if (showStockEditDialog && selectedProduct != null) {
            StockEditDialog(
                product = selectedProduct!!,
                onDismiss = { showStockEditDialog = false },
                onConfirm = { newStock, reason, notes ->
                    viewModel.updateProductStock(selectedProduct!!, newStock, reason, notes)
                    showStockEditDialog = false
                    selectedProduct = null
                }
            )
        }
        
        // Bulk Import Dialog - TODO: Implement BulkImportDialog
        if (showBulkImportDialog) {
            AlertDialog(
                onDismissRequest = { showBulkImportDialog = false },
                title = { Text("Bulk Import") },
                text = { Text("Bulk import feature is under development. You can import products from CSV/Excel files.") },
                confirmButton = {
                    TextButton(onClick = { showBulkImportDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // IMEI Scan Dialog - Use Unified Scanner for searching
        if (showIMEIScanDialog) {
            UnifiedIMEIScannerDialog(
                onDismiss = { showIMEIScanDialog = false },
                onIMEIScanned = { imeis ->
                    // Handle scanned IMEIs - search for products with these IMEIs
                    imeis.firstOrNull()?.let { imeiData ->
                        viewModel.onSearchQueryChange(imeiData.imei)
                    }
                    showIMEIScanDialog = false
                },
                scanMode = IMEIScanMode.SINGLE,
                scanner = unifiedIMEIScanner,
                showManualEntry = true,
                title = "Scan IMEI to Search"
            )
        }
        
        // Export Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Inventory") },
                text = { Text("Export feature is under development. You will be able to export inventory to CSV/Excel files.") },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Add IMEI Dialog
        if (showAddIMEIDialog && selectedProduct != null) {
            AddIMEIDialog(
                productName = selectedProduct!!.productName,
                defaultPurchasePrice = selectedProduct!!.costPrice,
                onDismiss = { 
                    showAddIMEIDialog = false
                    selectedProduct = null
                },
                onAdd = { imeiNumber, imei2Number, serialNumber, purchasePrice, boxNumber, warrantyCardNumber, notes ->
                    viewModel.addIMEIToProduct(
                        productId = selectedProduct!!.productId,
                        imeiNumber = imeiNumber,
                        imei2Number = imei2Number,
                        serialNumber = serialNumber,
                        purchasePrice = purchasePrice,
                        boxNumber = boxNumber,
                        warrantyCardNumber = warrantyCardNumber,
                        notes = notes
                    )
                    showAddIMEIDialog = false
                    selectedProduct = null
                },
                onBulkAdd = { imeiPairs ->
                    // Handle bulk IMEI addition
                    viewModel.addBulkIMEIsToProduct(
                        productId = selectedProduct!!.productId,
                        imeiPairs = imeiPairs,
                        defaultPurchasePrice = selectedProduct!!.costPrice
                    ) { addedCount ->
                        // Show success message (optional - could add a toast/snackbar)
                        // For now, just close dialog - ViewModel will update UI state
                    }
                    showAddIMEIDialog = false
                    selectedProduct = null
                },
                unifiedIMEIScanner = unifiedIMEIScanner
            )
        }
        
        // Edit IMEI Dialog
        if (showEditIMEIDialog && selectedIMEI != null) {
            EditIMEIDialog(
                imei = selectedIMEI!!,
                onDismiss = {
                    showEditIMEIDialog = false
                    selectedIMEI = null
                },
                onSave = { updatedIMEI ->
                    viewModel.updateIMEI(updatedIMEI)
                    showEditIMEIDialog = false
                    selectedIMEI = null
                },
                scanner = unifiedIMEIScanner
            )
        }
        
        // Edit Product Dialog
        if (showEditProductDialog && selectedProduct != null) {
            EditProductDialog(
                product = selectedProduct!!,
                categories = uiState.categories,
                onDismiss = {
                    showEditProductDialog = false
                    selectedProduct = null
                },
                onSave = { productName, brand, model, color, variant, category, costPrice, sellingPrice, currentStock, minStockLevel, description ->
                    viewModel.updateProduct(
                        productId = selectedProduct!!.productId,
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
                        description = description
                    )
                    showEditProductDialog = false
                    selectedProduct = null
                },
                onDelete = {
                    viewModel.deleteProduct(selectedProduct!!)
                    showEditProductDialog = false
                    selectedProduct = null
                }
            )
        }
    }
}

@Composable
fun InventoryStatistics(
    totalProducts: Int,
    lowStockCount: Int,
    totalValue: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Products Card - Large featured card
        Card(
            modifier = Modifier
                .weight(1.5f)
                .height(140.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = totalProducts.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Products",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Small cards column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Low Stock Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (lowStockCount > 0) 
                        Color(0xFFFEE2E2)
                    else 
                        Color(0xFFE0F2FE)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (lowStockCount > 0) 
                            Color(0xFFEF4444).copy(alpha = 0.2f)
                        else 
                            Color(0xFF0EA5E9).copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (lowStockCount > 0) 
                                    Color(0xFFDC2626)
                                else 
                                    Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = lowStockCount.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (lowStockCount > 0) 
                                Color(0xFFDC2626)
                            else 
                                Color(0xFF0284C7)
                        )
                        Text(
                            text = "Low Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Total Value Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFDCFCE7)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = formatCompactCurrency(totalValue),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Total Value",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Helper function to format currency in compact form
private fun formatCompactCurrency(value: Double): String {
    return when {
        value >= 10000000 -> {
            val crores = value / 10000000
            if (crores >= 100) "₹%.0fCr".format(crores) else "₹%.1fCr".format(crores)
        }
        value >= 100000 -> {
            val lakhs = value / 100000
            if (lakhs >= 100) "₹%.0fL".format(lakhs) else "₹%.1fL".format(lakhs)
        }
        value >= 1000 -> {
            val thousands = value / 1000
            if (thousands >= 100) "₹%.0fK".format(thousands) else "₹%.1fK".format(thousands)
        }
        else -> "₹%.0f".format(value)
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = modifier
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
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
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { 
            Text(
                "Search products...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { 
                    Text(
                        category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (category == selectedCategory) FontWeight.SemiBold else FontWeight.Normal
                    ) 
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = category == selectedCategory,
                    borderColor = if (category == selectedCategory) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                )
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Brand and Model
                Text(
                    text = "${product.brand} • ${product.model}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                // Color and Variant Row
                if (!product.color.isNullOrBlank() || !product.variant.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!product.color.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = product.color,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        if (!product.variant.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = product.variant,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stock Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (product.isLowStock) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (product.isLowStock) Icons.Default.Warning else Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (product.isLowStock) 
                                    MaterialTheme.colorScheme.onErrorContainer 
                                else 
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Stock: ${product.currentStock}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (product.isLowStock) 
                                    MaterialTheme.colorScheme.onErrorContainer 
                                else 
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatCurrency(product.sellingPrice.toDouble()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cost: ${formatCurrency(product.costPrice.toDouble())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Profit: ${formatCurrency(product.profitAmount.toDouble())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyInventoryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No products found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Try adjusting your search or filters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SortDialog(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by") },
        text = {
            Column {
                SortOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (option) {
                                SortOption.NAME -> "Name"
                                SortOption.STOCK -> "Stock Level"
                                SortOption.PRICE -> "Price"
                                SortOption.BRAND -> "Brand"
                                SortOption.RECENTLY_ADDED -> "Recently Added"
                            }
                        )
                        if (option == currentSort) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ProductDetailsDialog(
    product: Product,
    onDismiss: () -> Unit,
    onEditStock: () -> Unit = {},
    onDeactivate: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = product.productName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Brand", product.brand)
                DetailRow("Model", product.model)
                if (!product.color.isNullOrBlank()) {
                    DetailRow("Color", product.color)
                }
                if (!product.variant.isNullOrBlank()) {
                    DetailRow("Variant", product.variant)
                }
                DetailRow("Category", product.category)
                DetailRow("IMEI", product.imei1)
                product.imei2?.let {
                    DetailRow("IMEI 2", it)
                }
                product.barcode?.let {
                    DetailRow("Barcode", it)
                }
                HorizontalDivider()
                DetailRow("Cost Price", formatCurrency(product.costPrice.toDouble()))
                DetailRow("Selling Price", formatCurrency(product.sellingPrice.toDouble()))
                DetailRow("Profit", formatCurrency(product.profitAmount.toDouble()))
                DetailRow("Profit %", "${product.profitPercentage.formatLocale("%.1f")}%")
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        DetailRow("Current Stock", product.currentStock.toString())
                        DetailRow("Min Stock Level", product.minStockLevel.toString())
                        DetailRow(
                            "Status",
                            if (product.isLowStock) "Low Stock" else "In Stock",
                    valueColor = if (product.isLowStock) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            onEditStock()
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Edit, "Edit Stock")
                    }
                }
                product.description?.let {
                    HorizontalDivider()
                    Text(
                        text = "Description:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        onEditStock()
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit Stock")
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Deactivate")
                }
            }
        }
    )
    
    if (showDeleteConfirm) {
        ModernConfirmDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Deactivate Product?",
            message = "This product will be removed from active inventory. You can reactivate it later if needed.",
            confirmText = "Deactivate",
            onConfirm = {
                onDeactivate()
                showDeleteConfirm = false
            },
            isDestructive = true
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

fun formatCurrency(amount: Double): String {
    return amount.formatCompactCurrency()
}
