package com.billme.app.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.data.local.entity.GSTMode
import com.billme.app.data.local.entity.GSTType
import com.billme.app.core.util.GSTValidator
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlexibleGSTSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: GSTSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Flexible GST System",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Configure GST calculation modes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    AnimatedIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp), // Space for bottom button
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Flexible GST System (FGHS)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configure how GST is calculated and displayed on invoices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // GST Mode Selection
            item {
                GSTModeSelectionCard(
                    selectedMode = uiState.gstMode,
                    onModeSelected = { viewModel.updateGSTMode(it) }
                )
            }
            
            // GST Number (shown for all modes except NO_GST)
            if (uiState.gstMode != GSTMode.NO_GST) {
                item {
                    GSTNumberCard(
                        gstNumber = uiState.gstNumber,
                        onGstNumberChange = { viewModel.updateGSTNumber(it) }
                    )
                }
            }
            
            // GST Rate and Type (shown only for modes that apply GST)
            if (uiState.gstMode.shouldApplyGST()) {
                item {
                    GSTRateCard(
                        defaultRate = uiState.defaultGstRate,
                        onRateChange = { viewModel.updateDefaultGSTRate(it) }
                    )
                }
                
                item {
                    GSTTypeCard(
                        gstType = uiState.gstType,
                        onTypeSelected = { viewModel.updateGSTType(it) },
                        stateCode = uiState.stateCode,
                        onStateCodeChange = { viewModel.updateStateCode(it) }
                    )
                }
            }
            
            // Success/Error Messages
            if (uiState.successMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.successMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            if (uiState.errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom Save Button
        Button(
            onClick = { viewModel.saveSettings() },
            enabled = uiState.isModified && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp,
                disabledElevation = 0.dp
            ),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (uiState.isLoading) "Saving..." else "Save Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        }
    }
}

@Composable
fun GSTModeSelectionCard(
    selectedMode: GSTMode,
    onModeSelected: (GSTMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GST Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GSTMode.entries.forEach { mode ->
                    GSTModeOption(
                        mode = mode,
                        selected = selectedMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                }
            }
        }
    }
}

@Composable
fun GSTModeOption(
    mode: GSTMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = if (selected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.getDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mode.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun GSTNumberCard(
    gstNumber: String,
    onGstNumberChange: (String) -> Unit
) {
    // Use the proper GSTValidator for validation
    val validationResult = remember(gstNumber) {
        GSTValidator.validateGSTINWithDetails(gstNumber)
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GSTIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = gstNumber,
                onValueChange = { newValue ->
                    // Only allow alphanumeric characters and limit to 15
                    val filtered = newValue.take(15).filter { it.isLetterOrDigit() }
                    onGstNumberChange(filtered.uppercase())
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GST Identification Number") },
                placeholder = { Text("22AAAAA0000A1Z5") },
                leadingIcon = {
                    Icon(Icons.Default.Tag, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = gstNumber.isNotEmpty() && !validationResult.isValid,
                supportingText = {
                    when {
                        gstNumber.isEmpty() -> Text("Optional field")
                        gstNumber.length < 15 -> Text("${gstNumber.length}/15 characters")
                        !validationResult.isValid -> Text(
                            validationResult.errorMessage ?: "Invalid GSTIN format", 
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> {
                            Column {
                                Text("✓ Valid GSTIN", color = MaterialTheme.colorScheme.primary)
                                if (validationResult.stateName != null) {
                                    Text(
                                        "State: ${validationResult.stateName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            )
            Text(
                text = "Enter your 15-digit GSTIN (format: 2 digits state + 10 digits PAN + 1 entity + 1 checksum + 1 amendment)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
@Composable
fun GSTRateCard(
    defaultRate: String,
    onRateChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GST Rate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = defaultRate,
                onValueChange = onRateChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Default GST Rate (%)") },
                leadingIcon = {
                    Icon(Icons.Default.Percent, contentDescription = null)
                },
                suffix = { Text("%") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickRateButton("5", defaultRate, onRateChange)
                QuickRateButton("12", defaultRate, onRateChange)
                QuickRateButton("18", defaultRate, onRateChange)
                QuickRateButton("28", defaultRate, onRateChange)
            }
        }
    }
}

@Composable
fun RowScope.QuickRateButton(
    rate: String,
    currentRate: String,
    onRateChange: (String) -> Unit
) {
    FilledTonalButton(
        onClick = { onRateChange(rate) },
        modifier = Modifier.weight(1f),
        colors = if (currentRate == rate) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        }
    ) {
        Text("$rate%")
    }
}

@Composable
fun GSTTypeCard(
    gstType: GSTType,
    onTypeSelected: (GSTType) -> Unit,
    stateCode: String,
    onStateCodeChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GST Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // CGST + SGST Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = gstType == GSTType.CGST_SGST,
                            onClick = { onTypeSelected(GSTType.CGST_SGST) },
                            role = Role.RadioButton
                        ),
                    colors = if (gstType == GSTType.CGST_SGST) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        CardDefaults.cardColors()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = gstType == GSTType.CGST_SGST,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CGST + SGST",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "For intra-state transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // IGST Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = gstType == GSTType.IGST,
                            onClick = { onTypeSelected(GSTType.IGST) },
                            role = Role.RadioButton
                        ),
                    colors = if (gstType == GSTType.IGST) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        CardDefaults.cardColors()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = gstType == GSTType.IGST,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "IGST",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "For inter-state transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // State Code (for CGST+SGST)
            if (gstType == GSTType.CGST_SGST) {
                OutlinedTextField(
                    value = stateCode,
                    onValueChange = onStateCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("State Code") },
                    placeholder = { Text("27 (for Maharashtra)") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}
