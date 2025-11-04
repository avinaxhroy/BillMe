package com.billme.app.ui.component

import android.Manifest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.camera.view.PreviewView
import com.billme.app.core.scanner.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

/**
 * Unified IMEI Scanner Dialog
 * 
 * A single, reusable dialog that handles all IMEI scanning scenarios:
 * - Single IMEI scanning
 * - Dual IMEI scanning (IMEI1 & IMEI2)
 * - Bulk IMEI scanning (multiple devices)
 * - Auto mode (smart detection)
 * 
 * Features:
 * - Real-time camera preview
 * - Smart IMEI detection
 * - Duplicate checking
 * - Manual IMEI entry option
 * - Torch/flash toggle
 * - Beautiful, intuitive UI
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UnifiedIMEIScannerDialog(
    onDismiss: () -> Unit,
    onIMEIScanned: (List<ScannedIMEIData>) -> Unit,
    scanMode: IMEIScanMode = IMEIScanMode.AUTO,
    scanner: UnifiedIMEIScanner,
    showManualEntry: Boolean = true,
    title: String? = null
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    
    val scanState by scanner.scanState.collectAsState()
    val isTorchOn by scanner.isTorchOn.collectAsState()
    
    var showManualEntryDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Track previous scan count to detect new scans
    var previousScanCount by remember { mutableIntStateOf(0) }
    
    // Helper function to perform haptic feedback
    fun performScanHapticFeedback() {
        try {
            // First try Compose haptic feedback
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            
            // Then add stronger vibration feedback with a pattern
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            }
            
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Double pulse pattern for success feedback
                        val timings = longArrayOf(0, 80, 50, 80)
                        val amplitudes = intArrayOf(0, 255, 0, 255)
                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        vib.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        // Double vibration pattern for older devices
                        val pattern = longArrayOf(0, 80, 50, 80)
                        vib.vibrate(pattern, -1)
                    }
                }
            }
        } catch (e: Exception) {
            // Silently ignore if vibration fails
            e.printStackTrace()
        }
    }
    
    // Trigger haptic feedback on successful scan
    LaunchedEffect(scanState) {
        if (scanState is IMEIScanState.Scanning) {
            val state = scanState as IMEIScanState.Scanning
            // Trigger haptic on any new IMEI (removed the previousScanCount > 0 check)
            if (state.scannedIMEIs.size > previousScanCount) {
                // New IMEI(s) detected - provide haptic feedback
                performScanHapticFeedback()
            }
            previousScanCount = state.scannedIMEIs.size
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            scanner.stopScanning()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopAppBar(
                    title = { 
                        Text(
                            title ?: when (scanMode) {
                                IMEIScanMode.SINGLE -> "Scan IMEI"
                                IMEIScanMode.DUAL -> "Scan Dual IMEI"
                                IMEIScanMode.BULK -> "Bulk IMEI Scan"
                                IMEIScanMode.AUTO -> "Smart IMEI Scanner"
                            }
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    actions = {
                        // Flash toggle
                        IconButton(
                            onClick = { scanner.toggleTorch() },
                            enabled = scanState is IMEIScanState.Scanning
                        ) {
                            Icon(
                                if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                "Toggle Flash"
                            )
                        }
                        
                        // Manual entry option
                        if (showManualEntry && scanMode != IMEIScanMode.SINGLE) {
                            IconButton(onClick = { showManualEntryDialog = true }) {
                                Icon(Icons.Default.Edit, "Manual Entry")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                
                // Camera permission check
                if (!cameraPermissionState.status.isGranted) {
                    PermissionRequired(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                } else {
                    // Main content - Show camera preview for all states
                    Box(modifier = Modifier.weight(1f)) {
                        // Always show the scanning view with camera preview
                        ScanningView(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            scanner = scanner,
                            scanMode = scanMode,
                            scannedIMEIs = when (val state = scanState) {
                                is IMEIScanState.Scanning -> state.scannedIMEIs
                                else -> emptyList()
                            },
                            message = when (val state = scanState) {
                                is IMEIScanState.Idle -> "Initializing camera..."
                                is IMEIScanState.Scanning -> state.message
                                else -> ""
                            }
                        )
                        
                        // Overlay success or error state on top of camera preview
                        when (val state = scanState) {
                            is IMEIScanState.Success -> {
                                SuccessView(
                                    imeis = state.imeis,
                                    message = state.message,
                                    onComplete = { onIMEIScanned(state.imeis) },
                                    onRescan = { 
                                        scanner.retryScan()
                                    }
                                )
                            }
                            
                            is IMEIScanState.Error -> {
                                ErrorView(
                                    message = state.message,
                                    onRetry = { scanner.retryScan() },
                                    onDismiss = onDismiss
                                )
                            }
                            
                            else -> { /* Idle or Scanning - camera preview is visible */ }
                        }
                    }
                }
            }
        }
    }
    
    // Manual entry dialog
    if (showManualEntryDialog) {
        ManualIMEIEntryDialog(
            onDismiss = { showManualEntryDialog = false },
            onIMEIEntered = { imei ->
                coroutineScope.launch {
                    scanner.addManualIMEI(imei)
                }
                showManualEntryDialog = false
            }
        )
    }
}

@Composable
private fun PermissionRequired(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "To scan IMEI numbers, we need access to your camera",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Permission")
        }
    }
}

@Composable
private fun ScanningView(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    scanner: UnifiedIMEIScanner,
    scanMode: IMEIScanMode,
    scannedIMEIs: List<ScannedIMEIData>,
    message: String
) {
    // Create the PreviewView once and reuse it
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    
    // Start scanning once when first composed
    LaunchedEffect(Unit) {
        // Reset scanner to clear old data before starting new scan
        scanner.resetScanner()
        scanner.startScanning(context, lifecycleOwner, previewView, scanMode)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview - square for better proportions
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.TopCenter)
        )
        
        // Scanning overlay
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scanning frame area (square to match camera)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(24.dp)
            ) {
                // Scanning frame border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                
                // Corner markers
                ScanningCornerMarkers()
            }
            
            // Complete Scan Button - Right below scanner preview
            if (scanMode == IMEIScanMode.BULK && scannedIMEIs.isNotEmpty()) {
                Button(
                    onClick = { 
                        scanner.completeBulkScan()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle, 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Complete Scan (${scannedIMEIs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Spacer to push info panel to bottom
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom info panel - now without the button
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column {
                    ScanningInfoPanel(
                        scannedIMEIs = scannedIMEIs,
                        message = message,
                        scanMode = scanMode,
                        showButton = false // Don't show button in panel anymore
                    )
                    
                    // Add navigation bar spacing
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun ScanningCornerMarkers() {
    val cornerSize = 40.dp
    val cornerWidth = 4.dp
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(cornerSize)
                    .height(cornerWidth)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(cornerWidth)
                    .height(cornerSize)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(cornerSize)
                    .height(cornerWidth)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(cornerWidth)
                    .height(cornerSize)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Bottom-left corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(cornerSize)
                    .height(cornerWidth)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(cornerWidth)
                    .height(cornerSize)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerSize)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(cornerSize)
                    .height(cornerWidth)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(cornerWidth)
                    .height(cornerSize)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun ScanningInfoPanel(
    scannedIMEIs: List<ScannedIMEIData>,
    message: String,
    scanMode: IMEIScanMode,
    showButton: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Scanning status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Scanned IMEIs list
            if (scannedIMEIs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Detected IMEIs (${scannedIMEIs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Scrollable list with max height constraint
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    scannedIMEIs.forEach { imeiData ->
                        ScannedIMEIItem(imeiData)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedIMEIItem(imeiData: ScannedIMEIData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        Icon(
            imageVector = when {
                imeiData.isDuplicate -> Icons.Default.Warning
                imeiData.isValid -> Icons.Default.CheckCircle
                else -> Icons.Default.Error
            },
            contentDescription = null,
            tint = when {
                imeiData.isDuplicate -> MaterialTheme.colorScheme.error
                imeiData.isValid -> Color(0xFF4CAF50)
                else -> MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // IMEI text
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position badge - show before IMEI number
                if (imeiData.position != IMEIPosition.SINGLE) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (imeiData.position) {
                            IMEIPosition.IMEI1 -> MaterialTheme.colorScheme.primaryContainer
                            IMEIPosition.IMEI2 -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            text = when (imeiData.position) {
                                IMEIPosition.IMEI1 -> "IMEI 1"
                                IMEIPosition.IMEI2 -> "IMEI 2"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (imeiData.position) {
                                IMEIPosition.IMEI1 -> MaterialTheme.colorScheme.onPrimaryContainer
                                IMEIPosition.IMEI2 -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Text(
                    text = imeiData.formattedIMEI,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            if (imeiData.isDuplicate) {
                Text(
                    text = "Duplicate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SuccessView(
    imeis: List<ScannedIMEIData>,
    message: String,
    onComplete: () -> Unit,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF4CAF50)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Scan Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Scanned IMEIs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                items(imeis) { imeiData ->
                    ScannedIMEIItem(imeiData)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use ${if (imeis.size > 1) "These IMEIs" else "This IMEI"}")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onRescan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Again")
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Scanning Error",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualIMEIEntryDialog(
    onDismiss: () -> Unit,
    onIMEIEntered: (String) -> Unit
) {
    var imeiText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter IMEI Manually") },
        text = {
            Column {
                OutlinedTextField(
                    value = imeiText,
                    onValueChange = { 
                        imeiText = it.filter { char -> char.isDigit() }.take(15)
                        errorMessage = null
                    },
                    label = { Text("IMEI Number") },
                    placeholder = { Text("15 digits") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (imeiText.length == 15) {
                        onIMEIEntered(imeiText)
                    } else {
                        errorMessage = "IMEI must be exactly 15 digits"
                    }
                },
                enabled = imeiText.length == 15
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
