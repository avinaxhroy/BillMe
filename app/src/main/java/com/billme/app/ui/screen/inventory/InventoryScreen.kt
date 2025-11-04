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
                // Selection mode toolbar with gradient
                ModernTopAppBar(
                    title = "${uiState.selectedProductIds.size} selected",
                    subtitle = "Bulk actions available",
                    navigationIcon = Icons.Default.Close,
                    onNavigationClick = { viewModel.exitSelectionMode() },
                    useGradient = true,
                    gradientColors = listOf(Secondary, SecondaryLight),
                    actions = {
                        // Select All
                        IconButton(onClick = { viewModel.selectAllProducts() }) {
                            Icon(
                                Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = Color.White
                            )
                        }
                        
                        // Delete Selected
                        IconButton(onClick = { viewModel.deleteSelectedProducts() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = Color.White
                            )
                        }
                    }
                )
            } else {
                // Normal mode toolbar - Use compact version
                ModernTopAppBar(
                    title = "Inventory",
                    subtitle = "${uiState.products.size} products",
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = onNavigateBack,
                    useGradient = true,
                    actions = {
                        // More Actions
                        Box {
                            IconButton(onClick = { showActionsMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More Actions",
                                    tint = Color.White
                                )
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Add Purchase FAB
                    ModernFloatingActionButton(
                        onClick = onNavigateToAddPurchase,
                        icon = Icons.Default.ShoppingCart,
                        contentDescription = "Add Purchase"
                    )
                    
                    // Add Product FAB
                    ModernExtendedFAB(
                        text = "Add Product",
                        icon = Icons.Default.Add,
                        onClick = onNavigateToAddProduct,
                        expanded = true,
                        useGradient = true
                    )
                }
            }
        }
    ) { paddingValues ->
        // Single scrollable LazyColumn containing everything
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            
            // Category Filter
            item {
                CategoryFilter(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::onCategorySelected,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            
            // Low Stock Filter Chip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.showLowStockOnly,
                        onClick = viewModel::toggleLowStockFilter,
                        label = { Text("Low Stock Only", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.showLowStockOnly) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    
                    Text(
                        text = "${uiState.filteredProducts.size} items",
                        style = MaterialTheme.typography.bodySmall,
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ModernStatCard(
            title = "Products",
            value = totalProducts.toString(),
            icon = Icons.Default.Inventory,
            modifier = Modifier.weight(1f),
            iconColor = MaterialTheme.colorScheme.primary
        )
        ModernStatCard(
            title = "Low Stock",
            value = lowStockCount.toString(),
            icon = Icons.Default.Warning,
            modifier = Modifier.weight(1f),
            trend = if (lowStockCount > 0) "$lowStockCount" else null,
            trendPositive = lowStockCount == 0,
            iconColor = if (lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        ModernStatCard(
            title = "Value",
            value = formatCurrency(totalValue),
            icon = Icons.Default.CurrencyRupee,
            modifier = Modifier.weight(1f),
            iconColor = Tertiary
        )
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
    ModernSearchField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "Search by name, brand, IMEI, barcode...",
        modifier = modifier
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
            ModernFilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = category
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
    ModernEmptyState(
        icon = Icons.Default.Inventory,
        title = "No products found",
        description = "Try adjusting your search or filters",
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    )
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
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}
