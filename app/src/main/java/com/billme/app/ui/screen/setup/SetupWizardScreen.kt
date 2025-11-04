package com.billme.app.ui.screen.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.data.local.entity.GSTMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupWizardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Wizard") },
                navigationIcon = {
                    if (uiState.currentStep != SetupStep.BUSINESS_INFO) {
                        IconButton(onClick = { viewModel.goToPreviousStep() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { uiState.completionProgress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
            
            // Step indicator
            StepIndicator(
                currentStep = uiState.currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Animated content for steps
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState.stepNumber > initialState.stepNumber) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "step_transition"
            ) { step ->
                when (step) {
                    SetupStep.RESTORE_OPTION -> RestoreOptionStep(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSetupComplete = onSetupComplete
                    )
                    SetupStep.BUSINESS_INFO -> BusinessInfoStep(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    SetupStep.GST_CONFIG -> GstConfigStep(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    SetupStep.PRICING_SETTINGS -> PricingSettingsStep(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    SetupStep.REVIEW -> ReviewStep(
                        uiState = uiState,
                        onEditStep = { viewModel.goToStep(it) }
                    )
                }
            }
            
            // Error message
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Navigation buttons
            NavigationButtons(
                currentStep = uiState.currentStep,
                isLoading = uiState.isLoading,
                onNext = { viewModel.goToNextStep() },
                onComplete = { viewModel.completeSetup(onSetupComplete) }
            )
        }
    }
}

@Composable
fun StepIndicator(
    currentStep: SetupStep,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SetupStep.entries.forEach { step ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Step circle
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        step == currentStep -> MaterialTheme.colorScheme.primary
                        step.stepNumber < currentStep.stepNumber -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = step.stepNumber.toString(),
                            color = when {
                                step == currentStep -> MaterialTheme.colorScheme.onPrimary
                                step.stepNumber < currentStep.stepNumber -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Step label
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = if (step == currentStep) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun BusinessInfoStep(
    uiState: SetupWizardUiState,
    viewModel: SetupWizardViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Tell us about your business",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "This information will appear on your invoices and receipts",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Business Name
        OutlinedTextField(
            value = uiState.businessName,
            onValueChange = { viewModel.onBusinessNameChange(it) },
            label = { Text("Business Name *") },
            placeholder = { Text("My Mobile Shop") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.businessNameError != null,
            supportingText = {
                uiState.businessNameError?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(Icons.Default.Store, contentDescription = null)
            }
        )
        
        // Business Address
        OutlinedTextField(
            value = uiState.businessAddress,
            onValueChange = { viewModel.onBusinessAddressChange(it) },
            label = { Text("Business Address") },
            placeholder = { Text("123 Main Street, City") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
        )
        
        // Phone Number
        OutlinedTextField(
            value = uiState.businessPhone,
            onValueChange = { viewModel.onBusinessPhoneChange(it) },
            label = { Text("Phone Number") },
            placeholder = { Text("9876543210") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.phoneError != null,
            supportingText = {
                uiState.phoneError?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null)
            }
        )
        
        // Email
        OutlinedTextField(
            value = uiState.businessEmail,
            onValueChange = { viewModel.onBusinessEmailChange(it) },
            label = { Text("Email") },
            placeholder = { Text("shop@example.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.emailError != null,
            supportingText = {
                uiState.emailError?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            }
        )
    }
}

@Composable
fun GstConfigStep(
    uiState: SetupWizardUiState,
    viewModel: SetupWizardViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Configure Tax Settings",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Set up GST and tax configuration for your business",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // GST Enabled Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable GST",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Apply GST to invoices and bills",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.gstEnabled,
                    onCheckedChange = { viewModel.onGstEnabledChange(it) }
                )
            }
        }
        
        if (uiState.gstEnabled) {
            // GST Number
            OutlinedTextField(
                value = uiState.gstNumber,
                onValueChange = { viewModel.onGstNumberChange(it) },
                label = { Text("GST Number (GSTIN)") },
                placeholder = { Text("22AAAAA0000A1Z5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.gstNumberError != null,
                supportingText = {
                    if (uiState.gstNumberError != null) {
                        Text(uiState.gstNumberError!!)
                    } else {
                        Text("15-character GST identification number (optional)")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = {
                    Icon(Icons.Default.Badge, contentDescription = null)
                }
            )
            
            // GST Mode
            Text(
                text = "GST Mode",
                style = MaterialTheme.typography.titleMedium
            )
            
            GSTMode.entries.forEach { mode ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.gstMode == mode) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    onClick = { viewModel.onGstModeChange(mode) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.gstMode == mode,
                            onClick = { viewModel.onGstModeChange(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.getDisplayName(),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = mode.getDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Default GST Rate
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Default GST Rate: ${uiState.defaultGstRate}%",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Common rates: 5%, 12%, 18%, 28%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PricingSettingsStep(
    uiState: SetupWizardUiState,
    viewModel: SetupWizardViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.PriceChange,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Pricing Preferences",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Configure how prices work in your shop",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Price includes GST
        SettingCard(
            title = "GST-Inclusive Pricing",
            description = "Selling price includes GST (final customer price)",
            checked = uiState.priceIncludesGst,
            onCheckedChange = { viewModel.onPriceIncludesGstChange(it) },
            icon = Icons.Default.AttachMoney
        )
        
        // Allow price edit
        SettingCard(
            title = "Allow Price Editing",
            description = "Edit prices while creating bills",
            checked = uiState.allowPriceEdit,
            onCheckedChange = { viewModel.onAllowPriceEditChange(it) },
            icon = Icons.Default.Edit
        )
        
        // Track cost history
        SettingCard(
            title = "Track Cost Price History",
            description = "Store multiple purchase prices for better profit tracking",
            checked = uiState.trackCostHistory,
            onCheckedChange = { viewModel.onTrackCostHistoryChange(it) },
            icon = Icons.Default.History
        )
        
        // Show GST breakdown
        if (uiState.gstEnabled) {
            SettingCard(
                title = "Show GST Breakdown",
                description = "Display CGST, SGST, IGST details on invoices",
                checked = uiState.showGstBreakdown,
                onCheckedChange = { viewModel.onShowGstBreakdownChange(it) },
                icon = Icons.Default.Receipt
            )
        }
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun ReviewStep(
    uiState: SetupWizardUiState,
    onEditStep: (SetupStep) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Review Your Settings",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Please review your configuration before completing setup",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Business Information
        ReviewSection(
            title = "Business Information",
            items = listOf(
                "Name" to uiState.businessName,
                "Address" to uiState.businessAddress.ifBlank { "Not provided" },
                "Phone" to uiState.businessPhone.ifBlank { "Not provided" },
                "Email" to uiState.businessEmail.ifBlank { "Not provided" }
            ),
            onEdit = { onEditStep(SetupStep.BUSINESS_INFO) }
        )
        
        // GST Configuration
        ReviewSection(
            title = "Tax Configuration",
            items = buildList {
                add("GST Enabled" to if (uiState.gstEnabled) "Yes" else "No")
                if (uiState.gstEnabled) {
                    add("GST Number" to uiState.gstNumber.ifBlank { "Not provided" })
                    add("GST Mode" to uiState.gstMode.getDisplayName())
                    add("Default Rate" to "${uiState.defaultGstRate}%")
                }
            },
            onEdit = { onEditStep(SetupStep.GST_CONFIG) }
        )
        
        // Pricing Settings
        ReviewSection(
            title = "Pricing Settings",
            items = listOf(
                "GST-Inclusive Pricing" to if (uiState.priceIncludesGst) "Yes" else "No",
                "Allow Price Editing" to if (uiState.allowPriceEdit) "Yes" else "No",
                "Track Cost History" to if (uiState.trackCostHistory) "Yes" else "No",
                "Show GST Breakdown" to if (uiState.showGstBreakdown) "Yes" else "No"
            ),
            onEdit = { onEditStep(SetupStep.PRICING_SETTINGS) }
        )
    }
}

@Composable
fun ReviewSection(
    title: String,
    items: List<Pair<String, String>>,
    onEdit: () -> Unit
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }
            
            HorizontalDivider()
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationButtons(
    currentStep: SetupStep,
    isLoading: Boolean,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (currentStep) {
                SetupStep.REVIEW -> {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isLoading) "Completing..." else "Complete Setup")
                        if (!isLoading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreOptionStep(
    uiState: SetupWizardUiState,
    viewModel: SetupWizardViewModel,
    onSetupComplete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // File picker launcher
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Convert URI to File and restore
            val tempFile = java.io.File(context.cacheDir, "restore_backup.zip")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.restoreFromBackup(tempFile)
            } catch (e: Exception) {
                android.util.Log.e("SetupWizard", "Failed to read backup file", e)
            }
        }
    }
    
    // Google Sign-In launcher for Drive restore
    val signInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            viewModel.continueRestoreAfterSignIn(account)
        } catch (e: Exception) {
            android.util.Log.e("SetupWizard", "Google Sign-In failed", e)
        }
    }
    
    // Trigger sign-in when needed
    androidx.compose.runtime.LaunchedEffect(uiState.needsGoogleSignIn) {
        if (uiState.needsGoogleSignIn) {
            signInLauncher.launch(viewModel.getGoogleSignInIntent())
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Backup,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to BillMe",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Choose how you'd like to get started",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Restore from backup option
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { filePickerLauncher.launch("application/zip") },
            enabled = !uiState.isRestoring
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Restore from Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Import your previous data, settings, and invoices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Restore from Google Drive option
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.restoreFromGoogleDrive() },
            enabled = !uiState.isRestoring
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Restore from Google Drive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Import your latest backup from Google Drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Start fresh option
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.chooseStartFresh() },
            enabled = !uiState.isRestoring
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Fresh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set up your business from scratch with guided steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
        
        // Restore progress indicator
        if (uiState.isRestoring) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = uiState.restoreProgress ?: "Restoring...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Success message after restore
        if (!uiState.isRestoring && uiState.restoreProgress != null && uiState.errorMessage == null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Restore Completed!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Your data has been successfully restored. You can now start using the app.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onSetupComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get Started")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Info text
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = "Backup files contain:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "• All products and inventory\n• Transactions and invoices\n• Customer and supplier data\n• Settings and preferences\n• Digital signatures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
