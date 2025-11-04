package com.billme.app.ui.screen.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billme.app.data.local.entity.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: Product,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        productName: String,
        brand: String,
        model: String,
        color: String?,
        variant: String?,
        category: String,
        costPrice: java.math.BigDecimal,
        sellingPrice: java.math.BigDecimal,
        currentStock: Int,
        minStockLevel: Int,
        description: String?
    ) -> Unit,
    onDelete: () -> Unit = {}
) {
    var productName by remember { mutableStateOf(product.productName) }
    var brand by remember { mutableStateOf(product.brand) }
    var model by remember { mutableStateOf(product.model) }
    var color by remember { mutableStateOf(product.color ?: "") }
    var variant by remember { mutableStateOf(product.variant ?: "") }
    var category by remember { mutableStateOf(product.category) }
    var costPrice by remember { mutableStateOf(product.costPrice.toString()) }
    var sellingPrice by remember { mutableStateOf(product.sellingPrice.toString()) }
    var currentStock by remember { mutableStateOf(product.currentStock.toString()) }
    var minStockLevel by remember { mutableStateOf(product.minStockLevel.toString()) }
    var description by remember { mutableStateOf(product.description ?: "") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && productName.isBlank()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = showError && brand.isBlank()
                    )
                    
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = showError && model.isBlank()
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = color,
                        onValueChange = { color = it },
                        label = { Text("Color") },
                        placeholder = { Text("e.g., Crimson Art") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = variant,
                        onValueChange = { variant = it },
                        label = { Text("Variant") },
                        placeholder = { Text("e.g., 8GB/256GB") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth(),
                        singleLine = true
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.filter { it != "All" }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost Price *") },
                        prefix = { Text("₹") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = showError && costPrice.toBigDecimalOrNull() == null
                    )
                    
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Selling Price *") },
                        prefix = { Text("₹") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = showError && sellingPrice.toBigDecimalOrNull() == null
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text("Current Stock *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = showError && currentStock.toIntOrNull() == null
                    )
                    
                    OutlinedTextField(
                        value = minStockLevel,
                        onValueChange = { minStockLevel = it },
                        label = { Text("Min Stock Level") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                if (showError) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        // Validation
                        if (productName.isBlank() || brand.isBlank() || model.isBlank()) {
                            showError = true
                            errorMessage = "Required fields cannot be empty"
                            return@Button
                        }
                        
                        val cost = costPrice.toBigDecimalOrNull()
                        val selling = sellingPrice.toBigDecimalOrNull()
                        val stock = currentStock.toIntOrNull() ?: 0
                        val minStock = minStockLevel.toIntOrNull() ?: 1
                        
                        if (cost == null || selling == null) {
                            showError = true
                            errorMessage = "Invalid price values"
                            return@Button
                        }
                        
                        if (stock < 0) {
                            showError = true
                            errorMessage = "Stock cannot be negative"
                            return@Button
                        }
                        
                        onSave(
                            productName.trim(),
                            brand.trim(),
                            model.trim(),
                            color.trim().takeIf { it.isNotBlank() },
                            variant.trim().takeIf { it.isNotBlank() },
                            category,
                            cost,
                            selling,
                            stock,
                            minStock,
                            description.trim().takeIf { it.isNotBlank() }
                        )
                    }
                ) {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
    
    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Product?") },
            text = { Text("This will permanently delete the product. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
