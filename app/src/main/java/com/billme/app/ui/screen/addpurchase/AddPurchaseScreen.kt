package com.billme.app.ui.screen.addpurchase

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.core.util.formatLocale
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddPurchaseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Add New Product",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add inventory purchase",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
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
                    TextButton(
                        onClick = { viewModel.clearForm() },
                        enabled = !uiState.isLoading
                    ) {
                        Text("Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveProduct(onNavigateBack) },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    text = { Text("Save Product") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Information Section
                item {
                    SectionHeader("Product Information")
                }
                
                item {
                    OutlinedTextField(
                        value = uiState.productName,
                        onValueChange = viewModel::onProductNameChange,
                        label = { Text("Product Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isLoading
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.brand,
                            onValueChange = viewModel::onBrandChange,
                            label = { Text("Brand *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !uiState.isLoading,
                            trailingIcon = {
                                if (uiState.existingBrands.isNotEmpty()) {
                                    IconButton(onClick = { showBrandDialog = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Brand")
                                    }
                                }
                            }
                        )
                        
                        OutlinedTextField(
                            value = uiState.model,
                            onValueChange = viewModel::onModelChange,
                            label = { Text("Model *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !uiState.isLoading
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = uiState.category,
                        onValueChange = viewModel::onCategoryChange,
                        label = { Text("Category *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        trailingIcon = {
                            if (uiState.existingCategories.isNotEmpty()) {
                                IconButton(onClick = { showCategoryDialog = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                                }
                            }
                        }
                    )
                }
                
                // IMEI & Barcode Section
                item {
                    SectionHeader("Identification")
                }
                
                item {
                    OutlinedTextField(
                        value = uiState.imei1,
                        onValueChange = viewModel::onImei1Change,
                        label = { Text("IMEI 1 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !uiState.isLoading,
                        isError = uiState.imei1Error != null,
                        supportingText = uiState.imei1Error?.let { { Text(it) } },
                        leadingIcon = {
                            Icon(Icons.Default.Smartphone, contentDescription = null)
                        }
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = uiState.imei2,
                        onValueChange = viewModel::onImei2Change,
                        label = { Text("IMEI 2 (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !uiState.isLoading,
                        isError = uiState.imei2Error != null,
                        supportingText = uiState.imei2Error?.let { { Text(it) } },
                        leadingIcon = {
                            Icon(Icons.Default.Smartphone, contentDescription = null)
                        }
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = uiState.barcode,
                        onValueChange = viewModel::onBarcodeChange,
                        label = { Text("Barcode (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        leadingIcon = {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                        }
                    )
                }
                
                // Pricing Section
                item {
                    SectionHeader("Pricing")
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.costPrice,
                            onValueChange = viewModel::onCostPriceChange,
                            label = { Text("Cost Price *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            enabled = !uiState.isLoading,
                            isError = uiState.costPriceError != null,
                            supportingText = uiState.costPriceError?.let { { Text(it) } },
                            prefix = { Text("₹") },
                            leadingIcon = {
                                Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                            }
                        )
                        
                        OutlinedTextField(
                            value = uiState.sellingPrice,
                            onValueChange = viewModel::onSellingPriceChange,
                            label = { Text("Selling Price *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            enabled = !uiState.isLoading,
                            isError = uiState.sellingPriceError != null,
                            supportingText = uiState.sellingPriceError?.let { { Text(it) } },
                            prefix = { Text("₹") },
                            leadingIcon = {
                                Icon(Icons.Default.Sell, contentDescription = null)
                            }
                        )
                    }
                }
                
                // Profit Display
                if (uiState.profitAmount.toDouble() != 0.0) {
                    item {
                        ProfitCard(
                            profitAmount = uiState.profitAmount.toDouble(),
                            profitPercentage = uiState.profitPercentage
                        )
                    }
                }
                
                // Stock Section
                item {
                    SectionHeader("Stock Information")
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.currentStock,
                            onValueChange = viewModel::onCurrentStockChange,
                            label = { Text("Current Stock *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !uiState.isLoading,
                            leadingIcon = {
                                Icon(Icons.Default.Inventory, contentDescription = null)
                            }
                        )
                        
                        OutlinedTextField(
                            value = uiState.minStockLevel,
                            onValueChange = viewModel::onMinStockLevelChange,
                            label = { Text("Min Stock Level *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !uiState.isLoading,
                            leadingIcon = {
                                Icon(Icons.Default.Warning, contentDescription = null)
                            }
                        )
                    }
                }
                
                // Description Section
                item {
                    SectionHeader("Additional Details")
                }
                
                item {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 4,
                        enabled = !uiState.isLoading
                    )
                }
                
                // Save Button
                item {
                    Button(
                        onClick = { 
                            viewModel.saveProduct {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = uiState.isValid && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Product")
                        }
                    }
                }
                
                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        // Error Snackbar
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Could show a Snackbar here
            }
            
            AlertDialog(
                onDismissRequest = viewModel::dismissError,
                icon = {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissError) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Category Selection Dialog
        if (showCategoryDialog) {
            SelectionDialog(
                title = "Select Category",
                items = uiState.existingCategories,
                onItemSelected = {
                    viewModel.onCategoryChange(it)
                    showCategoryDialog = false
                },
                onDismiss = { showCategoryDialog = false }
            )
        }
        
        // Brand Selection Dialog
        if (showBrandDialog) {
            SelectionDialog(
                title = "Select Brand",
                items = uiState.existingBrands,
                onItemSelected = {
                    viewModel.onBrandChange(it)
                    showBrandDialog = false
                },
                onDismiss = { showBrandDialog = false }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun ProfitCard(
    profitAmount: Double,
    profitPercentage: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (profitAmount >= 0) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Profit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = formatCurrency(profitAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiary
            ) {
                Text(
                    text = "${profitPercentage.formatLocale("%.1f")}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SelectionDialog(
    title: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(items.size) { index ->
                    TextButton(
                        onClick = { onItemSelected(items[index]) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = items[index],
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (index < items.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}
