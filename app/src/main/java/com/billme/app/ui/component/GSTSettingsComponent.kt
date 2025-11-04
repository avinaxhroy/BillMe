package com.billme.app.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.billme.app.core.util.GSTValidator
import com.billme.app.core.util.GSTINValidationResult
import com.billme.app.data.local.entity.GSTMode
import com.billme.app.data.local.entity.GSTRateCategory
import com.billme.app.data.local.entity.GSTConfiguration

/**
 * GST Settings Configuration Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GSTSettingsComponent(
    gstConfig: GSTConfiguration?,
    onConfigChange: (GSTConfiguration) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    var shopGSTIN by remember(gstConfig) { mutableStateOf(gstConfig?.shopGSTIN ?: "") }
    var shopLegalName by remember(gstConfig) { mutableStateOf(gstConfig?.shopLegalName ?: "") }
    var shopTradeName by remember(gstConfig) { mutableStateOf(gstConfig?.shopTradeName ?: "") }
    var defaultGSTMode by remember(gstConfig) { mutableStateOf(gstConfig?.defaultGSTMode ?: GSTMode.NO_GST) }
    var allowPerInvoiceMode by remember(gstConfig) { mutableStateOf(gstConfig?.allowPerInvoiceMode ?: false) }
    var defaultGSTRate by remember(gstConfig) { mutableStateOf(gstConfig?.defaultGSTRate?.toString() ?: "18.0") }
    var showGSTINOnInvoice by remember(gstConfig) { mutableStateOf(gstConfig?.showGSTINOnInvoice ?: true) }
    var showGSTSummary by remember(gstConfig) { mutableStateOf(gstConfig?.showGSTSummary ?: true) }
    var includeGSTInPrice by remember(gstConfig) { mutableStateOf(gstConfig?.includeGSTInPrice ?: false) }
    var roundOffGST by remember(gstConfig) { mutableStateOf(gstConfig?.roundOffGST ?: true) }
    var enableGSTValidation by remember(gstConfig) { mutableStateOf(gstConfig?.enableGSTValidation ?: true) }
    var requireCustomerGSTIN by remember(gstConfig) { mutableStateOf(gstConfig?.requireCustomerGSTIN ?: false) }
    var autoDetectInterstate by remember(gstConfig) { mutableStateOf(gstConfig?.autoDetectInterstate ?: true) }
    var hsnCodeMandatory by remember(gstConfig) { mutableStateOf(gstConfig?.hsnCodeMandatory ?: false) }
    
    // GSTIN validation state
    var gstinValidation by remember { mutableStateOf<GSTINValidationResult?>(null) }
    var showGSTModeHelp by remember { mutableStateOf(false) }
    
    // Validate GSTIN when it changes
    LaunchedEffect(shopGSTIN) {
        if (shopGSTIN.isNotBlank()) {
            gstinValidation = GSTValidator.validateGSTINWithDetails(shopGSTIN)
        } else {
            gstinValidation = null
        }
    }
    
    // Update configuration when values change
    LaunchedEffect(
        shopGSTIN, shopLegalName, shopTradeName, defaultGSTMode, allowPerInvoiceMode,
        defaultGSTRate, showGSTINOnInvoice, showGSTSummary, includeGSTInPrice,
        roundOffGST, enableGSTValidation, requireCustomerGSTIN, autoDetectInterstate,
        hsnCodeMandatory
    ) {
        val rate = defaultGSTRate.toDoubleOrNull() ?: 18.0
        val now = kotlinx.datetime.Clock.System.now()
        
        val updatedConfig = GSTConfiguration(
            configId = gstConfig?.configId ?: 0L,
            shopGSTIN = shopGSTIN.ifBlank { null },
            shopLegalName = shopLegalName.ifBlank { null },
            shopTradeName = shopTradeName.ifBlank { null },
            shopStateCode = gstinValidation?.stateCode,
            shopStateName = gstinValidation?.stateName,
            defaultGSTMode = defaultGSTMode,
            allowPerInvoiceMode = allowPerInvoiceMode,
            defaultGSTRate = rate,
            defaultGSTCategory = when (rate) {
                0.0 -> GSTRateCategory.EXEMPT
                5.0 -> GSTRateCategory.GST_5
                12.0 -> GSTRateCategory.GST_12
                18.0 -> GSTRateCategory.GST_18
                28.0 -> GSTRateCategory.GST_28
                else -> GSTRateCategory.CUSTOM
            },
            showGSTINOnInvoice = showGSTINOnInvoice,
            showGSTSummary = showGSTSummary,
            includeGSTInPrice = includeGSTInPrice,
            roundOffGST = roundOffGST,
            enableGSTValidation = enableGSTValidation,
            requireCustomerGSTIN = requireCustomerGSTIN,
            autoDetectInterstate = autoDetectInterstate,
            hsnCodeMandatory = hsnCodeMandatory,
            createdAt = gstConfig?.createdAt ?: now,
            updatedAt = now
        )
        
        onConfigChange(updatedConfig)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GST Mode Selection
        GSTModeSelectionCard(
            selectedMode = defaultGSTMode,
            onModeChange = { defaultGSTMode = it },
            onHelpClick = { showGSTModeHelp = true }
        )
        
        // Shop GST Information
        if (defaultGSTMode != GSTMode.NO_GST) {
            GSTShopInformationCard(
                shopGSTIN = shopGSTIN,
                onGSTINChange = { shopGSTIN = it },
                shopLegalName = shopLegalName,
                onLegalNameChange = { shopLegalName = it },
                shopTradeName = shopTradeName,
                onTradeNameChange = { shopTradeName = it },
                gstinValidation = gstinValidation
            )
        }
        
        // GST Calculation Settings
        if (defaultGSTMode != GSTMode.NO_GST) {
            GSTCalculationSettingsCard(
                defaultGSTRate = defaultGSTRate,
                onGSTRateChange = { defaultGSTRate = it },
                includeGSTInPrice = includeGSTInPrice,
                onIncludeGSTChange = { includeGSTInPrice = it },
                roundOffGST = roundOffGST,
                onRoundOffChange = { roundOffGST = it }
            )
        }
        
        // Invoice Display Settings
        GSTInvoiceSettingsCard(
            gstMode = defaultGSTMode,
            showGSTINOnInvoice = showGSTINOnInvoice,
            onShowGSTINChange = { showGSTINOnInvoice = it },
            showGSTSummary = showGSTSummary,
            onShowSummaryChange = { showGSTSummary = it },
            allowPerInvoiceMode = allowPerInvoiceMode,
            onAllowPerInvoiceChange = { allowPerInvoiceMode = it }
        )
        
        // Compliance Settings
        if (defaultGSTMode != GSTMode.NO_GST) {
            GSTComplianceSettingsCard(
                enableGSTValidation = enableGSTValidation,
                onValidationChange = { enableGSTValidation = it },
                requireCustomerGSTIN = requireCustomerGSTIN,
                onRequireCustomerChange = { requireCustomerGSTIN = it },
                autoDetectInterstate = autoDetectInterstate,
                onAutoDetectChange = { autoDetectInterstate = it },
                hsnCodeMandatory = hsnCodeMandatory,
                onHSNMandatoryChange = { hsnCodeMandatory = it }
            )
        }
        
        // Save Button
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        text = "Save GST Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val validationErrors = validateConfiguration(gstinValidation, defaultGSTMode, shopGSTIN)
                    if (validationErrors.isNotEmpty()) {
                        Text(
                            text = "${validationErrors.size} error(s) found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Configuration is valid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Button(
                    onClick = onSave,
                    enabled = validateConfiguration(gstinValidation, defaultGSTMode, shopGSTIN).isEmpty()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
        
        // Bottom spacing
        Spacer(modifier = Modifier.height(80.dp))
    }
    
    // GST Mode Help Dialog
    if (showGSTModeHelp) {
        GSTModeHelpDialog(
            onDismiss = { showGSTModeHelp = false }
        )
    }
}

@Composable
private fun GSTModeSelectionCard(
    selectedMode: GSTMode,
    onModeChange: (GSTMode) -> Unit,
    onHelpClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Text(
                    text = "GST Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = onHelpClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.Help,
                        contentDescription = "GST Mode Help",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            GSTMode.entries.forEach { mode ->
                GSTModeOption(
                    mode = mode,
                    isSelected = selectedMode == mode,
                    onClick = { onModeChange(mode) }
                )
            }
        }
    }
}

@Composable
private fun GSTModeOption(
    mode: GSTMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (title, description) = when (mode) {
        GSTMode.FULL_GST -> "Full GST" to "Show complete GST breakdown (CGST/SGST/IGST) on invoice"
        GSTMode.PARTIAL_GST -> "Partial GST (Owner Only)" to "Hide GST from customer, visible in owner's reports only"
        GSTMode.GST_REFERENCE -> "GST Reference" to "Show only GSTIN on invoice, no tax calculations displayed"
        GSTMode.NO_GST -> "No GST" to "Disable GST completely for all transactions"
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
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
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GSTShopInformationCard(
    shopGSTIN: String,
    onGSTINChange: (String) -> Unit,
    shopLegalName: String,
    onLegalNameChange: (String) -> Unit,
    shopTradeName: String,
    onTradeNameChange: (String) -> Unit,
    gstinValidation: GSTINValidationResult?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Shop GST Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // GSTIN Input
            OutlinedTextField(
                value = shopGSTIN,
                onValueChange = { value ->
                    // Convert to uppercase and limit to 15 characters
                    val cleaned = value.uppercase().filter { it.isLetterOrDigit() }.take(15)
                    onGSTINChange(cleaned)
                },
                label = { Text("Shop GSTIN *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("22AAAAA0000A1Z5") },
                isError = gstinValidation?.isValid == false,
                supportingText = {
                    when {
                        gstinValidation?.isValid == true -> {
                            Text(
                                text = "✓ Valid GSTIN - ${gstinValidation.stateName}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        gstinValidation?.isValid == false -> {
                            Text(
                                text = gstinValidation.errorMessage ?: "Invalid GSTIN",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        shopGSTIN.isNotBlank() -> {
                            Text(
                                text = "${shopGSTIN.length}/15 characters",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                trailingIcon = {
                    when {
                        gstinValidation?.isValid == true -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        gstinValidation?.isValid == false -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Invalid",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
            
            // Display formatted GSTIN if valid
            gstinValidation?.takeIf { it.isValid }?.formattedGSTIN?.let { formatted ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "Formatted: $formatted",
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            OutlinedTextField(
                value = shopLegalName,
                onValueChange = onLegalNameChange,
                label = { Text("Legal Business Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = shopTradeName,
                onValueChange = onTradeNameChange,
                label = { Text("Trade Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun GSTCalculationSettingsCard(
    defaultGSTRate: String,
    onGSTRateChange: (String) -> Unit,
    includeGSTInPrice: Boolean,
    onIncludeGSTChange: (Boolean) -> Unit,
    roundOffGST: Boolean,
    onRoundOffChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GST Calculation Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = defaultGSTRate,
                onValueChange = { value ->
                    // Allow only decimal numbers
                    if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        onGSTRateChange(value)
                    }
                },
                label = { Text("Default GST Rate (%)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text("%") }
            )
            
            SettingsSwitch(
                title = "Include GST in Product Price",
                description = "GST is included in selling price (Inclusive pricing)",
                checked = includeGSTInPrice,
                onCheckedChange = onIncludeGSTChange
            )
            
            SettingsSwitch(
                title = "Round Off GST Amount",
                description = "Round the final amount to nearest rupee",
                checked = roundOffGST,
                onCheckedChange = onRoundOffChange
            )
        }
    }
}

@Composable
private fun GSTInvoiceSettingsCard(
    gstMode: GSTMode,
    showGSTINOnInvoice: Boolean,
    onShowGSTINChange: (Boolean) -> Unit,
    showGSTSummary: Boolean,
    onShowSummaryChange: (Boolean) -> Unit,
    allowPerInvoiceMode: Boolean,
    onAllowPerInvoiceChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Invoice Display Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            SettingsSwitch(
                title = "Show GSTIN on Invoice",
                description = "Display shop GSTIN on customer invoice",
                checked = showGSTINOnInvoice,
                onCheckedChange = onShowGSTINChange
            )
            
            if (gstMode == GSTMode.FULL_GST) {
                SettingsSwitch(
                    title = "Show GST Summary",
                    description = "Display detailed GST breakdown on invoice",
                    checked = showGSTSummary,
                    onCheckedChange = onShowSummaryChange
                )
            }
            
            SettingsSwitch(
                title = "Allow Per-Invoice Mode Change",
                description = "Allow changing GST mode for individual invoices",
                checked = allowPerInvoiceMode,
                onCheckedChange = onAllowPerInvoiceChange
            )
        }
    }
}

@Composable
private fun GSTComplianceSettingsCard(
    enableGSTValidation: Boolean,
    onValidationChange: (Boolean) -> Unit,
    requireCustomerGSTIN: Boolean,
    onRequireCustomerChange: (Boolean) -> Unit,
    autoDetectInterstate: Boolean,
    onAutoDetectChange: (Boolean) -> Unit,
    hsnCodeMandatory: Boolean,
    onHSNMandatoryChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GST Compliance Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            SettingsSwitch(
                title = "Enable GST Validation",
                description = "Validate GSTIN format and checksum",
                checked = enableGSTValidation,
                onCheckedChange = onValidationChange
            )
            
            SettingsSwitch(
                title = "Require Customer GSTIN",
                description = "Make customer GSTIN mandatory for all invoices",
                checked = requireCustomerGSTIN,
                onCheckedChange = onRequireCustomerChange
            )
            
            SettingsSwitch(
                title = "Auto-Detect Interstate",
                description = "Automatically determine CGST/SGST vs IGST based on GSTIN",
                checked = autoDetectInterstate,
                onCheckedChange = onAutoDetectChange
            )
            
            SettingsSwitch(
                title = "HSN Code Mandatory",
                description = "Require HSN code for all products",
                checked = hsnCodeMandatory,
                onCheckedChange = onHSNMandatoryChange
            )
        }
    }
}

@Composable
private fun GSTModeHelpDialog(onDismiss: () -> Unit) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Help,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GST Mode Guide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                listOf(
                    "Full GST" to "Shows complete GST breakdown (CGST/SGST/IGST) on customer invoice. Use this for B2B transactions where customers need detailed tax information.",
                    "Partial GST" to "Calculates GST internally for reports but hides it from customer invoice. Useful for B2C where customers don't need GST details.",
                    "GST Reference" to "Shows only your GSTIN on invoice without tax calculations. Good for businesses that want to show GST registration without detailed breakdown.",
                    "No GST" to "Completely disables GST. Use only if you're not GST registered or for non-taxable goods."
                ).forEach { (title, description) ->
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Got it")
                }
            }
        }
    }
}

private fun validateConfiguration(
    gstinValidation: GSTINValidationResult?,
    gstMode: GSTMode,
    shopGSTIN: String
): List<String> {
    val errors = mutableListOf<String>()
    
    if (gstMode != GSTMode.NO_GST) {
        if (shopGSTIN.isBlank()) {
            errors.add("Shop GSTIN is required when GST mode is not NO_GST")
        } else if (gstinValidation?.isValid != true) {
            errors.add("Invalid shop GSTIN format")
        }
    }
    
    return errors
}