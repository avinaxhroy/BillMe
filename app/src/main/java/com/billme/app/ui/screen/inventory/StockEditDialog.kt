package com.billme.app.ui.screen.inventory

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.StockAdjustmentReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (newStock: Int, reason: StockAdjustmentReason, notes: String) -> Unit
) {
    var newStock by remember { mutableStateOf(product.currentStock.toString()) }
    var selectedReason by remember { mutableStateOf(StockAdjustmentReason.MANUAL_RECOUNT) }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val stockDifference = (newStock.toIntOrNull() ?: product.currentStock) - product.currentStock
    val isValid = newStock.toIntOrNull() != null && newStock.toInt() >= 0
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Adjust Stock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            product.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                HorizontalDivider()
                
                // Current Stock Display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
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
                                "Current Stock",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                "${product.currentStock} units",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (stockDifference != 0) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (stockDifference > 0) 
                                    MaterialTheme.colorScheme.tertiaryContainer
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (stockDifference > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "${if (stockDifference > 0) "+" else ""}$stockDifference",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // New Stock Input
                OutlinedTextField(
                    value = newStock,
                    onValueChange = { newStock = it },
                    label = { Text("New Stock Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValid,
                    supportingText = {
                        if (!isValid) {
                            Text("Please enter a valid quantity")
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Inventory, null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Reason Selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedReason.getDisplayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Adjustment Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        leadingIcon = {
                            Icon(
                                when (selectedReason) {
                                    StockAdjustmentReason.MANUAL_RECOUNT -> Icons.Default.Checklist
                                    StockAdjustmentReason.DAMAGED -> Icons.Default.BrokenImage
                                    StockAdjustmentReason.THEFT -> Icons.Default.Warning
                                    StockAdjustmentReason.FOUND -> Icons.Default.Search
                                    StockAdjustmentReason.RETURNED -> Icons.AutoMirrored.Filled.KeyboardReturn
                                    StockAdjustmentReason.CORRECTION -> Icons.Default.Edit
                                    else -> Icons.Default.Info
                                },
                                null
                            )
                        }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        StockAdjustmentReason.values().forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason.getDisplayName()) },
                                onClick = {
                                    selectedReason = reason
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add any additional notes...") },
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Notes, null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val finalStock = newStock.toIntOrNull() ?: product.currentStock
                            onConfirm(finalStock, selectedReason, notes.trim())
                        },
                        enabled = isValid && stockDifference != 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Quick stock adjustment buttons
 */
@Composable
fun QuickStockAdjustment(
    currentStock: Int,
    onAdjust: (adjustment: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Quick Adjust:",
            style = MaterialTheme.typography.labelMedium
        )
        
        FilledTonalButton(
            onClick = { onAdjust(-10) },
            enabled = currentStock >= 10,
            modifier = Modifier.height(36.dp)
        ) {
            Text("-10")
        }
        
        FilledTonalButton(
            onClick = { onAdjust(-1) },
            enabled = currentStock >= 1,
            modifier = Modifier.height(36.dp)
        ) {
            Text("-1")
        }
        
        FilledTonalButton(
            onClick = { onAdjust(1) },
            modifier = Modifier.height(36.dp)
        ) {
            Text("+1")
        }
        
        FilledTonalButton(
            onClick = { onAdjust(10) },
            modifier = Modifier.height(36.dp)
        ) {
            Text("+10")
        }
    }
}
