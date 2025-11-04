package com.billme.app.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.billme.app.data.local.entity.GSTRate
import com.billme.app.data.local.entity.GSTRateCategory
import com.billme.app.core.util.formatLocale
import kotlinx.datetime.LocalDate

/**
 * GST Rate Management Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSTRateManagementComponent(
    gstRates: List<GSTRate>,
    onAddRate: (GSTRate) -> Unit,
    onUpdateRate: (GSTRate) -> Unit,
    onDeleteRate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRate by remember { mutableStateOf<GSTRate?>(null) }
    var selectedCategory by remember { mutableStateOf<GSTRateCategory?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GST Rate Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            FilledTonalButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Rate")
            }
        }
        
        // Category Filter
        GSTCategoryFilter(
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it }
        )
        
        // GST Rates List
        val filteredRates = if (selectedCategory != null) {
            val categoryStr = selectedCategory!!.name
            gstRates.filter { it.category == categoryStr }
        } else {
            gstRates
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRates, key = { it.rateId }) { rate ->
                GSTRateCard(
                    gstRate = rate,
                    onEdit = { editingRate = rate },
                    onDelete = { onDeleteRate(rate.rateId) }
                )
            }
            
            if (filteredRates.isEmpty()) {
                item {
                    EmptyGSTRatesMessage(
                        selectedCategory = selectedCategory,
                        onAddRate = { showAddDialog = true }
                    )
                }
            }
        }
    }
    
    // Add/Edit Rate Dialog
    if (showAddDialog || editingRate != null) {
        GSTRateDialog(
            existingRate = editingRate,
            onSave = { rate ->
                if (editingRate != null) {
                    onUpdateRate(rate)
                    editingRate = null
                } else {
                    onAddRate(rate)
                    showAddDialog = false
                }
            },
            onDismiss = {
                showAddDialog = false
                editingRate = null
            }
        )
    }
}

@Composable
private fun GSTCategoryFilter(
    selectedCategory: GSTRateCategory?,
    onCategoryChange: (GSTRateCategory?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Filter by Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("All") }
                )
                
                GSTRateCategory.values().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GSTRateCard(
    gstRate: GSTRate,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = gstRate.gstCategory.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val totalRate = (gstRate.cgstRate ?: 0.0) + (gstRate.sgstRate ?: 0.0) + (gstRate.igstRate ?: 0.0) + gstRate.cessRate
                    Text(
                        text = "Total Rate: ${totalRate.formatLocale("%.2f")}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Rate Breakdown
            if ((gstRate.cgstRate ?: 0.0) > 0 || (gstRate.sgstRate ?: 0.0) > 0) {
                GSTRateBreakdown(
                    label = "Intra-State (CGST + SGST)",
                    cgst = gstRate.cgstRate ?: 0.0,
                    sgst = gstRate.sgstRate ?: 0.0,
                    total = (gstRate.cgstRate ?: 0.0) + (gstRate.sgstRate ?: 0.0)
                )
            }
            
            if ((gstRate.igstRate ?: 0.0) > 0) {
                GSTRateBreakdown(
                    label = "Inter-State (IGST)",
                    igst = gstRate.igstRate ?: 0.0,
                    total = gstRate.igstRate ?: 0.0
                )
            }
            
            if (gstRate.cessRate > 0) {
                Text(
                    text = "Cess: ${gstRate.cessRate}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Effective Period
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "From: ${gstRate.effectiveFrom}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (gstRate.effectiveTo != null) {
                    Text(
                        text = "To: ${gstRate.effectiveTo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GSTRateBreakdown(
    label: String,
    cgst: Double = 0.0,
    sgst: Double = 0.0,
    igst: Double = 0.0,
    total: Double
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (cgst > 0) {
                    Text(
                        text = "CGST: ${cgst}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                if (sgst > 0) {
                    Text(
                        text = "SGST: ${sgst}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                if (igst > 0) {
                    Text(
                        text = "IGST: ${igst}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "Total: ${total}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyGSTRatesMessage(
    selectedCategory: GSTRateCategory?,
    onAddRate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = if (selectedCategory != null) {
                    "No GST rates configured for ${selectedCategory.displayName}"
                } else {
                    "No GST rates configured"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Add your first GST rate to get started with tax calculations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(onClick = onAddRate) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add GST Rate")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GSTRateDialog(
    existingRate: GSTRate?,
    onSave: (GSTRate) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = existingRate != null
    val scrollState = rememberScrollState()
    
    var category by remember { mutableStateOf(existingRate?.gstCategory ?: GSTRateCategory.GST_18) }
    var cgstRate by remember { mutableStateOf(existingRate?.cgstRate?.toString() ?: "9.0") }
    var sgstRate by remember { mutableStateOf(existingRate?.sgstRate?.toString() ?: "9.0") }
    var igstRate by remember { mutableStateOf(existingRate?.igstRate?.toString() ?: "18.0") }
    var cessRate by remember { mutableStateOf(existingRate?.cessRate?.toString() ?: "0.0") }
    var effectiveFrom by remember { mutableStateOf(existingRate?.effectiveFrom?.toString() ?: kotlinx.datetime.Clock.System.now().toString()) }
    var effectiveTo by remember { mutableStateOf(existingRate?.effectiveTo?.toString() ?: "") }
    var description by remember { mutableStateOf(existingRate?.description ?: "") }
    
    // Calculate total rate
    val totalRate = remember(cgstRate, sgstRate, igstRate, cessRate) {
        val cgst = cgstRate.toDoubleOrNull() ?: 0.0
        val sgst = sgstRate.toDoubleOrNull() ?: 0.0
        val igst = igstRate.toDoubleOrNull() ?: 0.0
        val cess = cessRate.toDoubleOrNull() ?: 0.0
        maxOf(cgst + sgst, igst) + cess
    }
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit GST Rate" else "Add GST Rate",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                HorizontalDivider()
                
                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = category.displayName,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            GSTRateCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        category = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Rate Input Fields
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Tax Rates (%)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cgstRate,
                            onValueChange = { value ->
                                if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                    cgstRate = value
                                }
                            },
                            label = { Text("CGST") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            suffix = { Text("%") }
                        )
                        
                        OutlinedTextField(
                            value = sgstRate,
                            onValueChange = { value ->
                                if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                    sgstRate = value
                                }
                            },
                            label = { Text("SGST") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            suffix = { Text("%") }
                        )
                    }
                    
                    OutlinedTextField(
                        value = igstRate,
                        onValueChange = { value ->
                            if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                igstRate = value
                            }
                        },
                        label = { Text("IGST (Inter-State)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("%") }
                    )
                    
                    OutlinedTextField(
                        value = cessRate,
                        onValueChange = { value ->
                            if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                cessRate = value
                            }
                        },
                        label = { Text("Cess (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("%") }
                    )
                }
                
                // Total Rate Display
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
                        Text(
                            text = "Total GST Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "${totalRate}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Effective Period
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Effective Period",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    OutlinedTextField(
                        value = effectiveFrom,
                        onValueChange = { effectiveFrom = it },
                        label = { Text("Effective From (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("2024-01-01") }
                    )
                    
                    OutlinedTextField(
                        value = effectiveTo,
                        onValueChange = { effectiveTo = it },
                        label = { Text("Effective To (Optional, YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Leave blank if still active") }
                    )
                }
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    placeholder = { Text("Additional notes about this rate") }
                )
                
                HorizontalDivider()
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            try {
                                // Parse dates
                                val parsedEffectiveFrom = try {
                                    kotlinx.datetime.Instant.parse(effectiveFrom)
                                } catch (e: Exception) {
                                    kotlinx.datetime.Clock.System.now()
                                }
                                
                                val parsedEffectiveTo = if (effectiveTo.isNotBlank()) {
                                    try {
                                        kotlinx.datetime.Instant.parse(effectiveTo)
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else null
                                
                                val rate = GSTRate(
                                    rateId = existingRate?.rateId ?: 0L,
                                    category = category.name,
                                    gstRate = totalRate,
                                    gstCategory = category,
                                    cgstRate = cgstRate.toDoubleOrNull(),
                                    sgstRate = sgstRate.toDoubleOrNull(),
                                    igstRate = igstRate.toDoubleOrNull(),
                                    cessRate = cessRate.toDoubleOrNull() ?: 0.0,
                                    effectiveFrom = parsedEffectiveFrom,
                                    effectiveTo = parsedEffectiveTo,
                                    description = description.ifBlank { null },
                                    createdAt = existingRate?.createdAt ?: kotlinx.datetime.Clock.System.now(),
                                    updatedAt = kotlinx.datetime.Clock.System.now()
                                )
                                onSave(rate)
                            } catch (e: Exception) {
                                // Handle validation errors
                            }
                        }
                    ) {
                        Text(if (isEditing) "Update" else "Save")
                    }
                }
            }
        }
    }
}

private val GSTRateCategory.displayName: String
    get() = when (this) {
        GSTRateCategory.EXEMPT -> "Exempt (0%)"
        GSTRateCategory.GST_5 -> "GST 5%"
        GSTRateCategory.GST_12 -> "GST 12%"
        GSTRateCategory.GST_18 -> "GST 18%"
        GSTRateCategory.GST_28 -> "GST 28%"
        GSTRateCategory.CUSTOM -> "Custom Rate"
    }