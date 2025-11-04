package com.billme.app.ui.screen.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.math.BigDecimal

/**
 * Dialog for editing product selling price
 */
@Composable
fun PriceEditDialog(
    productName: String,
    currentPrice: BigDecimal,
    costPrice: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var priceText by remember { mutableStateOf(currentPrice.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Calculate profit margin
    val calculatedPrice = priceText.toBigDecimalOrNull()
    val profitMargin = if (calculatedPrice != null && calculatedPrice > BigDecimal.ZERO) {
        val profit = calculatedPrice.subtract(costPrice)
        val margin = profit.divide(costPrice, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
        margin.setScale(2, java.math.RoundingMode.HALF_UP)
    } else {
        BigDecimal.ZERO
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
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
                    Text(
                        text = "Edit Selling Price",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                // Product Name
                Text(
                    text = productName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Cost Price Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cost Price:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "₹${costPrice.setScale(2, java.math.RoundingMode.HALF_UP)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                HorizontalDivider()
                
                // Price Input
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        error = when {
                            it.isBlank() -> "Price cannot be empty"
                            it.toBigDecimalOrNull() == null -> "Invalid price format"
                            it.toBigDecimalOrNull()!! <= BigDecimal.ZERO -> "Price must be greater than 0"
                            it.toBigDecimalOrNull()!! <= costPrice -> "Price should be higher than cost price"
                            else -> null
                        }
                    },
                    label = { Text("Selling Price") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = if (error != null) {
                        { Text(error!!, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Profit Margin Display
                if (calculatedPrice != null && calculatedPrice > costPrice) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Profit Margin",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${profitMargin}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Profit",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "₹${calculatedPrice.subtract(costPrice).setScale(2, java.math.RoundingMode.HALF_UP)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newPrice = priceText.toBigDecimalOrNull()
                            if (newPrice != null && newPrice > BigDecimal.ZERO) {
                                onConfirm(newPrice)
                                onDismiss()
                            }
                        },
                        enabled = error == null && priceText.toBigDecimalOrNull() != null
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
