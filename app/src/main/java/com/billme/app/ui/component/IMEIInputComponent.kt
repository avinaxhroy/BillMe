package com.billme.app.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.billme.app.core.service.*
import com.billme.app.data.local.entity.Product
import com.billme.app.hardware.IMEIScanResult

/**
 * Comprehensive IMEI Input Component with scanner integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IMEIInputComponent(
    imei1: String,
    imei2: String,
    onImei1Change: (String) -> Unit,
    onImei2Change: (String) -> Unit,
    onScanRequested: () -> Unit,
    validationResult: IMEIValidationResult?,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with scan button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IMEI Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Button(
                    onClick = onScanRequested,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Scan",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            // IMEI1 Input
            IMEIField(
                value = imei1,
                onValueChange = onImei1Change,
                label = "IMEI 1 *",
                validation = validationResult?.imei1,
                enabled = enabled && !isLoading,
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
            
            // IMEI2 Input
            IMEIField(
                value = imei2,
                onValueChange = onImei2Change,
                label = "IMEI 2 (Optional)",
                validation = validationResult?.imei2,
                enabled = enabled && !isLoading,
                onDone = { focusManager.clearFocus() }
            )
            
            // Validation Status
            AnimatedVisibility(
                visible = validationResult != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                validationResult?.let { result ->
                    ValidationStatusCard(result)
                }
            }
            
            // Loading indicator
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Validating IMEIs...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Individual IMEI input field with validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IMEIField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validation: IMEIValidation?,
    enabled: Boolean,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val isError = validation?.isValid == false
    val hasConflict = validation?.hasConflict == true
    
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Filter non-digits and limit to 15 characters
            val filtered = newValue.filter { it.isDigit() }.take(15)
            onValueChange(filtered)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = if (label.contains("IMEI 1")) ImeAction.Next else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
            onDone = { onDone() }
        ),
        supportingText = {
            when {
                validation?.errorMessage != null -> {
                    Text(
                        text = validation.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                hasConflict -> {
                    Text(
                        text = "⚠️ IMEI exists: ${validation?.conflictingProduct?.productName}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                value.isNotEmpty() && validation?.formattedIMEI != null -> {
                    Text(
                        text = "Formatted: ${validation.formattedIMEI}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
                value.length in 1..14 -> {
                    Text(
                        text = "${value.length}/15 digits",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingIcon = {
            when {
                validation?.isValid == true && !hasConflict -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Valid",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                isError || hasConflict -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                value.isNotEmpty() && validation == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (hasConflict) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    )
}

/**
 * Validation status card showing overall IMEI validation result
 */
@Composable
private fun ValidationStatusCard(validationResult: IMEIValidationResult) {
    val backgroundColor = when {
        validationResult.hasConflicts -> MaterialTheme.colorScheme.errorContainer
        validationResult.canProceed -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when {
        validationResult.hasConflicts -> MaterialTheme.colorScheme.onErrorContainer
        validationResult.canProceed -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    validationResult.hasConflicts -> Icons.Default.Warning
                    validationResult.canProceed -> Icons.Default.CheckCircle
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        validationResult.hasConflicts -> "IMEI Conflicts Detected"
                        validationResult.canProceed -> "IMEIs Valid"
                        else -> "Validation Issues"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
                
                if (validationResult.conflictDetails.isNotEmpty()) {
                    Text(
                        text = "${validationResult.conflictDetails.size} conflict(s) found",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * IMEI Scanner Result Dialog
 */
@Composable
fun IMEIScanResultDialog(
    scanResult: IMEIScanResult?,
    onAccept: (String?, String?) -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit
) {
    if (scanResult != null) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Result",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Detection method
                    DetectionMethodChip(scanResult.detectionMethod)
                    
                    // IMEI Results
                    if (scanResult.imei1 != null) {
                        IMEIResultItem(
                            label = "IMEI 1",
                            imei = scanResult.imei1,
                            isValid = scanResult.isValid
                        )
                    }
                    
                    if (scanResult.imei2 != null) {
                        IMEIResultItem(
                            label = "IMEI 2",
                            imei = scanResult.imei2,
                            isValid = scanResult.isValid
                        )
                    }
                    
                    if (scanResult.imei1 == null && scanResult.imei2 == null) {
                        Text(
                            text = "No valid IMEIs detected in scanned text",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    
                    // Raw text (expandable)
                    var showRawText by remember { mutableStateOf(false) }
                    
                    TextButton(
                        onClick = { showRawText = !showRawText },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showRawText) "Hide Raw Text" else "Show Raw Text")
                        Icon(
                            if (showRawText) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    
                    if (showRawText) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = scanResult.rawText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRescan,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan")
                        }
                        
                        Button(
                            onClick = { 
                                onAccept(scanResult.imei1, scanResult.imei2)
                                onDismiss()
                            },
                            enabled = scanResult.imei1 != null || scanResult.imei2 != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detection method indicator chip
 */
@Composable
private fun DetectionMethodChip(method: com.billme.app.hardware.IMEIDetectionMethod) {
    val (text, color) = when (method) {
        com.billme.app.hardware.IMEIDetectionMethod.SINGLE_IMEI -> "Single IMEI" to MaterialTheme.colorScheme.primary
        com.billme.app.hardware.IMEIDetectionMethod.DUAL_IMEI_SEPARATED -> "Dual IMEI (Separated)" to MaterialTheme.colorScheme.tertiary
        com.billme.app.hardware.IMEIDetectionMethod.DUAL_IMEI_SEQUENTIAL -> "Dual IMEI (Sequential)" to MaterialTheme.colorScheme.secondary
        com.billme.app.hardware.IMEIDetectionMethod.TEXT_EXTRACTION -> "Text Extraction" to MaterialTheme.colorScheme.primary
        com.billme.app.hardware.IMEIDetectionMethod.MANUAL_PARSE -> "Manual Parse Needed" to MaterialTheme.colorScheme.error
    }
    
    AssistChip(
        onClick = { },
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        )
    )
}

/**
 * Individual IMEI result item
 */
@Composable
private fun IMEIResultItem(
    label: String,
    imei: String,
    isValid: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isValid) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = imei,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * IMEI Conflict Resolution Dialog
 */
@Composable
fun IMEIConflictDialog(
    conflicts: List<IMEIConflict>,
    onResolve: (IMEIConflictAction, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAction by remember { mutableStateOf<IMEIConflictAction?>(null) }
    var ownerPin by remember { mutableStateOf("") }
    var showPinInput by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMEI Conflicts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "${conflicts.size} IMEI conflict(s) detected:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Conflict list
                conflicts.forEach { conflict ->
                    ConflictItem(conflict)
                }
                
                HorizontalDivider()
                
                Text(
                    text = "Choose resolution action:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Action buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConflictActionButton(
                        text = "Block Operation",
                        description = "Cancel and don't proceed",
                        icon = Icons.Default.Block,
                        selected = selectedAction == IMEIConflictAction.BLOCK,
                        onClick = { selectedAction = IMEIConflictAction.BLOCK }
                    )
                    
                    ConflictActionButton(
                        text = "Replace Existing",
                        description = "Deactivate existing products",
                        icon = Icons.Default.SwapHoriz,
                        selected = selectedAction == IMEIConflictAction.REPLACE,
                        onClick = { selectedAction = IMEIConflictAction.REPLACE }
                    )
                    
                    ConflictActionButton(
                        text = "Owner Override",
                        description = "Use owner PIN to override",
                        icon = Icons.Default.Key,
                        selected = selectedAction == IMEIConflictAction.OWNER_OVERRIDE,
                        onClick = { 
                            selectedAction = IMEIConflictAction.OWNER_OVERRIDE
                            showPinInput = true
                        }
                    )
                }
                
                // PIN Input (when override selected)
                AnimatedVisibility(
                    visible = showPinInput && selectedAction == IMEIConflictAction.OWNER_OVERRIDE
                ) {
                    OutlinedTextField(
                        value = ownerPin,
                        onValueChange = { ownerPin = it.filter { char -> char.isDigit() }.take(8) },
                        label = { Text("Owner PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Action buttons
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
                            selectedAction?.let { action ->
                                val pin = if (action == IMEIConflictAction.OWNER_OVERRIDE) ownerPin else null
                                onResolve(action, pin)
                            }
                        },
                        enabled = selectedAction != null && 
                                (selectedAction != IMEIConflictAction.OWNER_OVERRIDE || ownerPin.isNotEmpty()),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Proceed")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictItem(conflict: IMEIConflict) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "IMEI: ${conflict.imei}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Existing Product: ${conflict.conflictingProduct.productName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${conflict.conflictingProduct.brand} ${conflict.conflictingProduct.model}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConflictActionButton(
    text: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (selected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}