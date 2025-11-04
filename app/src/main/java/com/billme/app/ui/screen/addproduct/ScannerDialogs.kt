package com.billme.app.ui.screen.addproduct

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Barcode Scanner Dialog using ML Kit
 * NOTE: Disabled due to missing ML Kit dependencies
 */
@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    // Stub - ML Kit dependencies not available
}

/**
 * IMEI Scanner Dialog
 * NOTE: Disabled due to missing ML Kit dependencies
 */
@Composable
fun IMEIScannerDialog(
    onDismiss: () -> Unit,
    onIMEIScanned: (String) -> Unit
) {
    // Stub - ML Kit dependencies not available
}

/**
 * Multi-IMEI Scanner Dialog
 * NOTE: Disabled due to missing ML Kit dependencies
 */
@Composable
fun MultiIMEIScannerDialog(
    scannedIMEIs: List<String>,
    onDismiss: () -> Unit,
    onIMEIScanned: (String) -> Unit,
    onComplete: () -> Unit
) {
    // Stub - ML Kit dependencies not available
}

/**
 * Manual Multi-IMEI Entry Dialog
 * Allows users to enter multiple IMEIs manually or scan one by one
 */
@Composable
fun BulkIMEIEntryDialog(
    onDismiss: () -> Unit,
    onIMEIsAdded: (List<String>) -> Unit
) {
    var imeiText by remember { mutableStateOf("") }
    var singleIMEI by remember { mutableStateOf("") }
    val imeiList = remember { mutableStateListOf<String>() }
    var selectedTab by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Bulk IMEI Entry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Row for Bulk vs Sequential entry
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Bulk Entry") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Scan One by One") }
                    )
                }
                
                when (selectedTab) {
                    0 -> {
                        // Bulk Entry Mode
                        Text(
                            "Enter multiple IMEIs (one per line or comma-separated)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedTextField(
                            value = imeiText,
                            onValueChange = { imeiText = it },
                            label = { Text("Enter IMEIs") },
                            placeholder = { Text("356938035643809\n356938035643810\n...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            maxLines = 10,
                            singleLine = false
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Valid IMEIs: ${imeiList.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Button(
                                onClick = {
                                    // Parse and validate IMEIs
                                    val newIMEIs = imeiText
                                        .split(Regex("[,\\n]"))
                                        .map { it.trim() }
                                        .filter { it.length == 15 && it.all { c -> c.isDigit() } }
                                        .distinct()
                                        .filter { it !in imeiList }
                                    
                                    imeiList.addAll(newIMEIs)
                                    imeiText = ""
                                },
                                enabled = imeiText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                    
                    1 -> {
                        // Sequential Scan Mode
                        Text(
                            "Scan or enter IMEIs one at a time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedTextField(
                            value = singleIMEI,
                            onValueChange = { 
                                singleIMEI = it
                                // Auto-add when 15 digits reached
                                if (it.length == 15 && it.all { c -> c.isDigit() }) {
                                    if (it !in imeiList) {
                                        imeiList.add(it)
                                        singleIMEI = ""
                                    } else {
                                        // Duplicate detected, clear anyway
                                        singleIMEI = ""
                                    }
                                }
                            },
                            label = { Text("IMEI ${imeiList.size + 1}") },
                            placeholder = { Text("Scan or type 15 digits") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text(
                                    when {
                                        singleIMEI.isEmpty() -> "Ready to scan"
                                        singleIMEI.length < 15 -> "${15 - singleIMEI.length} digits remaining"
                                        singleIMEI.length == 15 && singleIMEI.all { it.isDigit() } -> "✓ Valid IMEI"
                                        else -> "Invalid IMEI format"
                                    },
                                    color = when {
                                        singleIMEI.length == 15 && singleIMEI.all { it.isDigit() } -> MaterialTheme.colorScheme.tertiary
                                        singleIMEI.isNotEmpty() && (singleIMEI.length != 15 || !singleIMEI.all { it.isDigit() }) -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            isError = singleIMEI.isNotEmpty() && (singleIMEI.length != 15 || !singleIMEI.all { it.isDigit() }),
                            trailingIcon = {
                                if (singleIMEI.isNotEmpty()) {
                                    IconButton(
                                        onClick = { singleIMEI = "" }
                                    ) {
                                        Icon(Icons.Default.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Added: ${imeiList.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Button(
                                onClick = {
                                    if (singleIMEI.length == 15 && singleIMEI.all { it.isDigit() } && singleIMEI !in imeiList) {
                                        imeiList.add(singleIMEI)
                                        singleIMEI = ""
                                    }
                                },
                                enabled = singleIMEI.length == 15 && singleIMEI.all { it.isDigit() } && singleIMEI !in imeiList
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add IMEI")
                            }
                        }
                    }
                }
                
                if (imeiList.isNotEmpty()) {
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Added IMEIs:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        TextButton(
                            onClick = { imeiList.clear() }
                        ) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(imeiList) { index, imei ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "${index + 1}.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        imei,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                                
                                IconButton(
                                    onClick = { imeiList.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (imeiList.isNotEmpty()) {
                        onIMEIsAdded(imeiList.toList())
                        onDismiss()
                    }
                },
                enabled = imeiList.isNotEmpty()
            ) {
                Text("Add ${imeiList.size} IMEI${if (imeiList.size != 1) "s" else ""}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
