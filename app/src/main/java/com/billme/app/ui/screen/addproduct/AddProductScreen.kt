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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Product Image Section
                ProductImageSection(
                    imageUri = uiState.imageUri,
                    onImageClick = { imagePickerLauncher.launch("image/*") }
                )
                
                // Scanner Actions Card
                ScannerActionsCard(
                    onBarcodeClick = { viewModel.toggleBarcodeScanner() },
                    onIMEIClick = { viewModel.toggleIMEIScanner() },
                    onMultiIMEIClick = { viewModel.toggleMultiIMEIScanner() },
                    onOCRClick = { ocrImageLauncher.launch("image/*") }
                )
                
                // Basic Information
                ModernGradientCard(
                    title = "Basic Information",
                    icon = Icons.Default.Info
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.productName,
                            onValueChange = viewModel::setProductName,
                            label = { Text("Product Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.productName.isBlank() && uiState.errorMessage != null,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.brand,
                                onValueChange = viewModel::setBrand,
                                label = { Text("Brand") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            OutlinedTextField(
                                value = uiState.model,
                                onValueChange = viewModel::setModel,
                                label = { Text("Model") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.color,
                                onValueChange = viewModel::updateColor,
                                label = { Text("Color") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            OutlinedTextField(
                                value = uiState.variant,
                                onValueChange = viewModel::updateVariant,
                                label = { Text("Variant") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isCategoryDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
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
                ModernGradientCard(
                    title = "IMEI & Barcode",
                    icon = Icons.Default.QrCodeScanner
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.imei1,
                            onValueChange = viewModel::setIMEI1,
                            label = { Text("IMEI 1 (15 digits)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                        
                        OutlinedTextField(
                            value = uiState.imei2,
                            onValueChange = viewModel::setIMEI2,
                            label = { Text("IMEI 2 (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                        Button(
                            onClick = { viewModel.toggleMultiIMEIScanner() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bulk IMEI Scanner")
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
                        
                        OutlinedTextField(
                            value = uiState.barcode,
                            onValueChange = viewModel::setBarcode,
                            label = { Text("Barcode") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                ModernGradientCard(
                    title = "Pricing",
                    icon = Icons.Default.AttachMoney
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.mrp,
                            onValueChange = viewModel::setMRP,
                            label = { Text("MRP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("₹") }
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.costPrice,
                                onValueChange = viewModel::setCostPrice,
                                label = { Text("Cost Price *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                prefix = { Text("₹") },
                                isError = uiState.costPrice.isBlank() && uiState.errorMessage != null
                            )
                            
                            OutlinedTextField(
                                value = uiState.sellingPrice,
                                onValueChange = viewModel::setSellingPrice,
                                label = { Text("Selling Price *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                prefix = { Text("₹") },
                                isError = uiState.sellingPrice.isBlank() && uiState.errorMessage != null
                            )
                        }
                        
                        OutlinedTextField(
                            value = uiState.currentStock,
                            onValueChange = viewModel::setStockQuantity,
                            label = { Text("Stock Quantity *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.currentStock.isBlank() && uiState.errorMessage != null
                        )
                    }
                }
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

@Composable
private fun ScannerActionsCard(
    onBarcodeClick: () -> Unit,
    onIMEIClick: () -> Unit,
    onMultiIMEIClick: () -> Unit,
    onOCRClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onBarcodeClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text("Barcode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                FilledTonalButton(
                    onClick = onIMEIClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text("IMEI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onMultiIMEIClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text("Multi IMEI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                FilledTonalButton(
                    onClick = onOCRClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text("OCR Scan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
