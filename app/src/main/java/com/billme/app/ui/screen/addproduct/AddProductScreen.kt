package com.billme.app.ui.screen.addproduct

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.billme.app.core.scanner.UnifiedIMEIScanner
import com.billme.app.core.scanner.IMEIScanMode
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInventory: () -> Unit = {},
    viewModel: AddProductViewModel = hiltViewModel(),
    unifiedIMEIScanner: UnifiedIMEIScanner
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setProductImage(it) }
    }
    
    // OCR image picker launcher
    val ocrImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.extractDataFromImage(it) }
    }
    
    // Show snackbar for messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }
    
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearErrorMessage()
        }
    }
    
    // Navigate to inventory on successful save
    LaunchedEffect(uiState.productSaved) {
        if (uiState.productSaved) {
            // Navigate back after successful product addition
            onNavigateToInventory()
            viewModel.clearForm()  // Clear form after navigation
        }
    }
    
    Scaffold(
        topBar = {
            ModernTopAppBar(
                title = "Add Product",
                subtitle = "Fill product details",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                useGradient = true,
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ModernExtendedFAB(
                text = "Save Product",
                icon = Icons.Default.Save,
                onClick = { viewModel.validateAndSave {} },
                expanded = true,
                useGradient = true
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        if (MaterialTheme.colorScheme.background == MaterialTheme.colorScheme.surface) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.background
                        }
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Product Image Section
                ModernProductImageSection(
                    imageUri = uiState.imageUri,
                    onImageClick = { imagePickerLauncher.launch("image/*") }
                )
                
                // Scanner Actions Card
                ModernScannerActionsCard(
                    onBarcodeClick = { viewModel.toggleBarcodeScanner() },
                    onIMEIClick = { viewModel.toggleIMEIScanner() },
                    onMultiIMEIClick = { viewModel.toggleMultiIMEIScanner() },
                    onOCRClick = { ocrImageLauncher.launch("image/*") }
                )
                
                // Basic Information
                ModernSectionCard(
                    title = "Basic Information",
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF2196F3)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernTextField(
                            value = uiState.productName,
                            onValueChange = viewModel::setProductName,
                            label = "Product Name *",
                            isError = uiState.productName.isBlank() && uiState.errorMessage != null,
                            leadingIcon = Icons.Default.Inventory2
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernTextField(
                                value = uiState.brand,
                                onValueChange = viewModel::setBrand,
                                label = "Brand",
                                modifier = Modifier.weight(1f),
                                leadingIcon = Icons.Default.Label
                            )
                            
                            ModernTextField(
                                value = uiState.model,
                                onValueChange = viewModel::setModel,
                                label = "Model",
                                modifier = Modifier.weight(1f),
                                leadingIcon = Icons.Default.PhoneAndroid
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernTextField(
                                value = uiState.color,
                                onValueChange = viewModel::updateColor,
                                label = "Color",
                                modifier = Modifier.weight(1f),
                                leadingIcon = Icons.Default.Palette
                            )
                            
                            ModernTextField(
                                value = uiState.variant,
                                onValueChange = viewModel::updateVariant,
                                label = "Variant",
                                modifier = Modifier.weight(1f),
                                leadingIcon = Icons.Default.Category
                            )
                        }
                        
                        // Category Dropdown
                        ExposedDropdownMenuBox(
                            expanded = uiState.isCategoryDropdownExpanded,
                            onExpandedChange = { viewModel.toggleCategoryDropdown() }
                        ) {
                            OutlinedTextField(
                                value = uiState.category,
                                onValueChange = {},
                                label = { Text("Category") },
                                readOnly = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isCategoryDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = uiState.isCategoryDropdownExpanded,
                                onDismissRequest = { viewModel.toggleCategoryDropdown() }
                            ) {
                                uiState.availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            viewModel.updateCategory(category)
                                            viewModel.toggleCategoryDropdown()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // IMEI & Barcode
                ModernSectionCard(
                    title = "IMEI & Barcode",
                    icon = Icons.Default.QrCodeScanner,
                    iconColor = Color(0xFF9C27B0)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernTextField(
                            value = uiState.imei1,
                            onValueChange = viewModel::setIMEI1,
                            label = "IMEI 1 (15 digits)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = Icons.Default.PhoneAndroid,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.toggleIMEIScanner() }) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan IMEI",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                        
                        ModernTextField(
                            value = uiState.imei2,
                            onValueChange = viewModel::setIMEI2,
                            label = "IMEI 2 (Optional)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = Icons.Default.PhoneAndroid,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.toggleIMEIScanner() }) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan IMEI",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                        
                        // Bulk IMEI Scanner Button
                        FilledTonalButton(
                            onClick = { viewModel.toggleMultiIMEIScanner() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Bulk IMEI Scanner", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        
                        // Show added IMEI pairs from bulk scanner
                        if (uiState.additionalIMEIPairs.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
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
                                        Column {
                                            Text(
                                                "Additional IMEI Pairs",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "${uiState.additionalIMEIPairs.size} pair(s) • All will be added to inventory",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.clearAdditionalIMEIs() }
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear all",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    
                                    // Show all IMEI pairs (no limit)
                                    uiState.additionalIMEIPairs.forEachIndexed { index, pair ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            elevation = CardDefaults.cardElevation(2.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        "Pair ${index + 1}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                    
                                                    // IMEI 1
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            "IMEI 1:",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            pair.imei1,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    
                                                    // IMEI 2 (if exists)
                                                    pair.imei2?.let { imei2 ->
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                "IMEI 2:",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                            Text(
                                                                imei2,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    } ?: run {
                                                        Text(
                                                            "IMEI 2: (Single IMEI)",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                }
                                                
                                                IconButton(
                                                    onClick = { viewModel.removeAdditionalIMEIPair(index) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Remove pair",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Info message
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Each pair will create a separate inventory entry",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        ModernTextField(
                            value = uiState.barcode,
                            onValueChange = viewModel::setBarcode,
                            label = "Barcode",
                            leadingIcon = Icons.Default.QrCode,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.toggleBarcodeScanner() }) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
                
                // Pricing
                ModernSectionCard(
                    title = "Pricing",
                    icon = Icons.Default.AttachMoney,
                    iconColor = Color(0xFF00897B)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernTextField(
                            value = uiState.mrp,
                            onValueChange = viewModel::setMRP,
                            label = "MRP",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = Icons.Default.CurrencyRupee
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernTextField(
                                value = uiState.costPrice,
                                onValueChange = viewModel::setCostPrice,
                                label = "Cost Price *",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                leadingIcon = Icons.Default.LocalOffer,
                                isError = uiState.costPrice.isBlank() && uiState.errorMessage != null
                            )
                            
                            ModernTextField(
                                value = uiState.sellingPrice,
                                onValueChange = viewModel::setSellingPrice,
                                label = "Selling Price *",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                leadingIcon = Icons.Default.Sell,
                                isError = uiState.sellingPrice.isBlank() && uiState.errorMessage != null
                            )
                        }
                        
                        ModernTextField(
                            value = uiState.currentStock,
                            onValueChange = viewModel::setStockQuantity,
                            label = "Stock Quantity *",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = Icons.Default.Inventory,
                            isError = uiState.currentStock.isBlank() && uiState.errorMessage != null
                        )
                    }
                }
                
                // Bottom spacing for FAB
                Spacer(modifier = Modifier.height(80.dp))
            }
            
            // OCR Processing Overlay
            if (uiState.isProcessingOCR) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Text(
                                "Processing Invoice...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Scanner Dialogs
    if (uiState.showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { viewModel.toggleBarcodeScanner() },
            onBarcodeScanned = { barcode ->
                viewModel.setBarcode(barcode)
                viewModel.toggleBarcodeScanner()
            }
        )
    }
    
    if (uiState.showIMEIScanner) {
        UnifiedIMEIScannerDialog(
            onDismiss = { viewModel.toggleIMEIScanner() },
            onIMEIScanned = { imeis ->
                when {
                    imeis.isEmpty() -> {}
                    imeis.size == 1 -> viewModel.setIMEI1(imeis[0].imei)
                    imeis.size >= 2 -> {
                        viewModel.setIMEI1(imeis[0].imei)
                        viewModel.setIMEI2(imeis[1].imei)
                    }
                }
                viewModel.toggleIMEIScanner()
            },
            scanMode = IMEIScanMode.AUTO,
            scanner = unifiedIMEIScanner,
            showManualEntry = true
        )
    }
    
    // Bulk IMEI Scanner Dialog
    if (uiState.showBulkIMEIDialog) {
        UnifiedIMEIScannerDialog(
            onDismiss = { viewModel.hideBulkIMEIDialog() },
            onIMEIScanned = { imeis ->
                // Extract just the IMEI strings from the scanned list
                val imeiStrings = imeis.map { it.imei }
                viewModel.onBulkIMEIsAdded(imeiStrings)
            },
            scanMode = IMEIScanMode.BULK,
            scanner = unifiedIMEIScanner,
            showManualEntry = true,
            title = "Bulk IMEI Scanner"
        )
    }
    
    // Existing Product Dialog
    if (uiState.showExistingProductDialog && uiState.existingProduct != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExistingProductDialog() },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Product Already Exists") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A product with this brand and model already exists:")
                    Text(
                        text = "${uiState.existingProduct?.brand} ${uiState.existingProduct?.model}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!uiState.existingProduct?.color.isNullOrBlank()) {
                        Text("Color: ${uiState.existingProduct?.color}")
                    }
                    if (!uiState.existingProduct?.variant.isNullOrBlank()) {
                        Text("Variant: ${uiState.existingProduct?.variant}")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Would you like to add IMEI/stock to the existing product or create a new variant?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissExistingProductDialog()
                        // Navigate to inventory or show add IMEI dialog
                        onNavigateToInventory()
                    }
                ) {
                    Text("Add IMEI to Existing")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExistingProductDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModernProductImageSection(
    imageUri: Uri?,
    onImageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onImageClick),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (imageUri != null) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Product Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(40.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(100.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Add Product Image",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Tap to select from gallery",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernScannerActionsCard(
    onBarcodeClick: () -> Unit,
    onIMEIClick: () -> Unit,
    onMultiIMEIClick: () -> Unit,
    onOCRClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF9800).copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernActionButton(
                    onClick = onBarcodeClick,
                    icon = Icons.Default.QrCodeScanner,
                    label = "Barcode",
                    backgroundColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                
                ModernActionButton(
                    onClick = onIMEIClick,
                    icon = Icons.Default.PhoneAndroid,
                    label = "IMEI",
                    backgroundColor = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernActionButton(
                    onClick = onMultiIMEIClick,
                    icon = Icons.Default.QrCode2,
                    label = "Multi IMEI",
                    backgroundColor = Color(0xFFE0F2F1),
                    iconColor = Color(0xFF00897B),
                    modifier = Modifier.weight(1f)
                )
                
                ModernActionButton(
                    onClick = onOCRClick,
                    icon = Icons.Default.DocumentScanner,
                    label = "OCR Scan",
                    backgroundColor = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = backgroundColor
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = iconColor
            )
        }
    }
}

@Composable
private fun ModernSectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Content
            content()
        }
    }
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else null,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun ProductImageSection(
    imageUri: Uri?,
    onImageClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onImageClick),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Product Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        "Tap to add product image",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Recommended: 500x500px",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
