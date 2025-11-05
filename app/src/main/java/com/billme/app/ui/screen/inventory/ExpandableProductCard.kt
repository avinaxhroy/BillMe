package com.billme.app.ui.screen.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.ProductIMEI
import com.billme.app.data.local.entity.IMEIStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpandableProductCard(
    product: Product,
    imeis: List<ProductIMEI>?,
    isExpanded: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleExpand: () -> Unit,
    onToggleSelection: () -> Unit = {},
    onEditProduct: () -> Unit,
    onDeleteProduct: () -> Unit,
    onAddIMEI: () -> Unit,
    onEditIMEI: (ProductIMEI) -> Unit,
    onDeleteIMEI: (ProductIMEI) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    // For IMEI-tracked products (smartphones, laptops, tablets, TVs), show IMEI count
    // For other products (accessories, chargers, etc.), show currentStock
    val hasIMEITracking = !imeis.isNullOrEmpty()
    val availableCount = if (hasIMEITracking) {
        imeis.count { it.status == IMEIStatus.AVAILABLE }
    } else {
        product.currentStock
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = if (isSelectionMode) onToggleSelection else onToggleExpand),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompactLayout = maxWidth < 360.dp
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompactLayout) 14.dp else 16.dp)
            ) {
                // Product Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Selection checkbox
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() },
                            modifier = Modifier.padding(end = if (isCompactLayout) 8.dp else 12.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.productName,
                            style = if (isCompactLayout) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Brand: ${product.brand}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Color and Variant Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(if (isCompactLayout) 6.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!product.color.isNullOrBlank()) {
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            product.color,
                                            style = MaterialTheme.typography.labelMedium
                                        ) 
                                    },
                                    leadingIcon = {
                                        Surface(
                                            modifier = Modifier.size(8.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {}
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = null
                                )
                            }
                            
                            if (!product.variant.isNullOrBlank()) {
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            product.variant,
                                            style = MaterialTheme.typography.labelMedium
                                        ) 
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isCompactLayout) 24.dp else 28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(if (isCompactLayout) 12.dp else 16.dp))
                
                // Product Info Row - Responsive layout with modern design
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactLayout) 10.dp else 12.dp)
                ) {
                    // Stock Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (availableCount > 0) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else 
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isCompactLayout) 8.dp else 10.dp, vertical = if (isCompactLayout) 10.dp else 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Stock",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$availableCount",
                                style = if (isCompactLayout) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (availableCount > 0) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Cost Price Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isCompactLayout) 8.dp else 10.dp, vertical = if (isCompactLayout) 10.dp else 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Cost",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCompactCurrency(product.costPrice),
                                style = if (isCompactLayout) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Selling Price Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isCompactLayout) 8.dp else 10.dp, vertical = if (isCompactLayout) 10.dp else 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Selling",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCompactCurrency(product.sellingPrice),
                                style = if (isCompactLayout) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Expanded Content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // Action Buttons - Modern design
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditProduct,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Edit, 
                                null, 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Edit",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        OutlinedButton(
                            onClick = onDeleteProduct,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Delete, 
                                null, 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Delete",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Add IMEI Button - Full width for products that need IMEI tracking
                    val needsIMEITracking = product.category.contains("phone", ignoreCase = true) ||
                            product.category.contains("smartphone", ignoreCase = true) ||
                            product.category.contains("laptop", ignoreCase = true) ||
                            product.category.contains("tablet", ignoreCase = true) ||
                            product.category.contains("tv", ignoreCase = true) ||
                            product.category.contains("television", ignoreCase = true) ||
                            hasIMEITracking
                    
                    if (needsIMEITracking) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddIMEI,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                null, 
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Add IMEI",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // IMEIs List - Only show for products with IMEI tracking
                    if (hasIMEITracking) {
                        if (imeis.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "No IMEIs added yet",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "IMEIs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${imeis.size}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            imeis.forEach { imei ->
                                IMEIItemCard(
                                    imei = imei,
                                    onEdit = { onEditIMEI(imei) },
                                    onDelete = { onDeleteIMEI(imei) },
                                    dateFormat = dateFormat,
                                    currencyFormat = currencyFormat
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun IMEIItemCard(
    imei: ProductIMEI,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (imei.status) {
                IMEIStatus.AVAILABLE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                IMEIStatus.SOLD -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                IMEIStatus.RESERVED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                IMEIStatus.DAMAGED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                IMEIStatus.RETURNED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when (imei.status) {
                IMEIStatus.AVAILABLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                IMEIStatus.SOLD -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                IMEIStatus.RESERVED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                IMEIStatus.DAMAGED -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                IMEIStatus.RETURNED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // IMEI 1
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IMEI 1:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = imei.imeiNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    
                    // IMEI 2
                    if (imei.imei2Number != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "IMEI 2:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = imei.imei2Number,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    
                    // Serial Number
                    if (imei.serialNumber != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "S/N:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = imei.serialNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    
                    // Box Number
                    if (imei.boxNumber != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Box:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = imei.boxNumber,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (imei.status) {
                        IMEIStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
                        IMEIStatus.SOLD -> MaterialTheme.colorScheme.surfaceVariant
                        IMEIStatus.RESERVED -> MaterialTheme.colorScheme.tertiary
                        IMEIStatus.DAMAGED -> MaterialTheme.colorScheme.error
                        IMEIStatus.RETURNED -> MaterialTheme.colorScheme.secondary
                    }
                ) {
                    Text(
                        text = imei.status.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (imei.status) {
                            IMEIStatus.AVAILABLE -> MaterialTheme.colorScheme.onPrimary
                            IMEIStatus.SOLD -> MaterialTheme.colorScheme.onSurfaceVariant
                            IMEIStatus.RESERVED -> MaterialTheme.colorScheme.onTertiary
                            IMEIStatus.DAMAGED -> MaterialTheme.colorScheme.onError
                            IMEIStatus.RETURNED -> MaterialTheme.colorScheme.onSecondary
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bottom Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormat.format(Date(imei.purchaseDate.toEpochMilliseconds())),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCompactCurrency(imei.purchasePrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Action Buttons - Only for available IMEIs
                if (imei.status == IMEIStatus.AVAILABLE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                "Edit",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "Delete",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete IMEI") },
            text = { Text("Are you sure you want to delete this IMEI? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
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

// Helper function to format currency in compact form
private fun formatCompactCurrency(value: java.math.BigDecimal): String {
    val amount = value.toDouble()
    return when {
        amount >= 10000000 -> "₹%.1fCr".format(amount / 10000000)
        amount >= 100000 -> "₹%.1fL".format(amount / 100000)
        amount >= 1000 -> "₹%.1fk".format(amount / 1000)
        else -> "₹%.0f".format(amount)
    }
}
