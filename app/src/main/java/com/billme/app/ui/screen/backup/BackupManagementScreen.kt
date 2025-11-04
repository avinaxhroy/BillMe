package com.billme.app.ui.screen.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.core.backup.BackupInfo
import com.billme.app.core.backup.EnhancedBackupService
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupManagementViewModel = hiltViewModel()
) {
    val backups by viewModel.availableBackups.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val driveSignedIn by viewModel.driveSignedIn.collectAsState()
    val driveUploadStatus by viewModel.driveUploadStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    var showRestoreDialog by remember { mutableStateOf<BackupInfo?>(null) }
    var showDeleteDialog by remember { mutableStateOf<BackupInfo?>(null) }
    
    // Refresh sign-in status when screen is visible
    LaunchedEffect(Unit) {
        viewModel.refreshDriveSignInStatus()
    }
    
    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("BackupScreen", "Sign-in result received. ResultCode: ${result.resultCode}, Data: ${result.data != null}")
        
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            try {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                android.util.Log.d("BackupScreen", "Account retrieved: ${account?.email}")
                viewModel.handleSignInResult(account)
            } catch (e: com.google.android.gms.common.api.ApiException) {
                android.util.Log.e("BackupScreen", "Sign-in API exception: status=${e.statusCode}, message=${e.message}", e)
                viewModel.showSignInError("Error code: ${e.statusCode}")
            } catch (e: Exception) {
                android.util.Log.e("BackupScreen", "Sign-in exception: ${e.message}", e)
                viewModel.showSignInError(e.message ?: "Unknown error")
            }
        } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            android.util.Log.d("BackupScreen", "Sign-in cancelled by user")
            viewModel.showSignInError("Sign-in cancelled")
        } else {
            android.util.Log.e("BackupScreen", "Sign-in failed with result code: ${result.resultCode}")
            viewModel.showSignInError("Sign-in failed")
        }
    }
    
    // File picker launcher for Google Drive restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.restoreFromUri(it, context)
        }
    }
    
    // Handle upload status messages
    LaunchedEffect(driveUploadStatus) {
        when (val status = driveUploadStatus) {
            is DriveUploadStatus.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Successfully uploaded to Google Drive: ${status.fileName}",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetUploadStatus()
            }
            is DriveUploadStatus.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Upload failed: ${status.message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetUploadStatus()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            ModernLargeTopAppBar(
                title = "Backup & Restore",
                subtitle = "${backups.size} backups available",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                useGradient = true,
                gradientColors = listOf(Warning, WarningLight)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ModernExtendedFAB(
                text = "Create Backup",
                icon = Icons.Default.Backup,
                onClick = { viewModel.createBackup() },
                expanded = true,
                useGradient = true,
                gradientColors = listOf(Warning, WarningLight)
            )
        }
    ) { padding ->
        
        // Track shown messages to prevent duplicate snackbars
        var lastShownSuccessTime by remember { mutableStateOf(0L) }
        var lastShownErrorMessage by remember { mutableStateOf<String?>(null) }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            // Modern Progress Indicator - More compact
            item {
                when (val progress = backupProgress) {
                    is EnhancedBackupService.BackupProgress.InProgress -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                ModernLinearProgress(
                                    progress = progress.progress / 100f,
                                    label = progress.message,
                                    showPercentage = true,
                                    gradientColors = listOf(Warning, WarningLight)
                                )
                            }
                        }
                    }
                    is EnhancedBackupService.BackupProgress.Success -> {
                        // Only show snackbar once per success using timestamp
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastShownSuccessTime > 2000) {
                            LaunchedEffect(currentTime) {
                                snackbarHostState.showSnackbar(
                                    message = "Backup successful!",
                                    duration = SnackbarDuration.Short
                                )
                                lastShownSuccessTime = currentTime
                            }
                        }
                    }
                    is EnhancedBackupService.BackupProgress.Error -> {
                        // Only show snackbar once per unique error
                        if (lastShownErrorMessage != progress.message) {
                            LaunchedEffect(progress.message) {
                                snackbarHostState.showSnackbar(
                                    message = progress.message,
                                    duration = SnackbarDuration.Long
                                )
                                lastShownErrorMessage = progress.message
                            }
                        }
                    }
                    else -> { /* Idle */ }
                }
            }
            
            // Auto-Backup Settings - More compact
            item {
                var autoBackupEnabled by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    autoBackupEnabled = viewModel.isAutoBackupEnabled()
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Daily Auto-Backup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Auto backup once daily",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { enabled ->
                                autoBackupEnabled = enabled
                                viewModel.setAutoBackupEnabled(enabled)
                            }
                        )
                    }
                }
            }
            
            // Google Drive Authentication - More compact
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Google Drive Sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (driveSignedIn) "Connected" 
                                    else "Not connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (driveSignedIn) 
                                        MaterialTheme.colorScheme.primary
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            if (driveSignedIn) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Not connected",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        if (driveSignedIn) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncToGoogleDrive() },
                                    modifier = Modifier.weight(1f),
                                    enabled = backupProgress !is EnhancedBackupService.BackupProgress.InProgress && driveUploadStatus !is DriveUploadStatus.Uploading
                                ) {
                                    if (driveUploadStatus is DriveUploadStatus.Uploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (driveUploadStatus is DriveUploadStatus.Uploading) "Uploading..." else "Upload", style = MaterialTheme.typography.labelMedium)
                                }
                                
                                OutlinedButton(
                                    onClick = { viewModel.signOutFromDrive() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Disconnect", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else {
                            Button(
                                onClick = { 
                                    android.util.Log.d("BackupScreen", "Sign-in button clicked")
                                    try {
                                        val intent = viewModel.getSignInIntent()
                                        android.util.Log.d("BackupScreen", "Sign-in intent created, launching...")
                                        signInLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.e("BackupScreen", "Error launching sign-in: ${e.message}", e)
                                        viewModel.showSignInError("Failed to start sign-in: ${e.message}")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sign in with Google", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            
            // Action Cards - Simplified
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.createBackup() },
                                modifier = Modifier.weight(1f),
                                enabled = backupProgress !is EnhancedBackupService.BackupProgress.InProgress
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Backup", style = MaterialTheme.typography.labelMedium)
                            }
                            
                            OutlinedButton(
                                onClick = { viewModel.createAndShareBackup() },
                                modifier = Modifier.weight(1f),
                                enabled = backupProgress !is EnhancedBackupService.BackupProgress.InProgress
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        // Google Drive restore button (only show if signed in)
                        if (driveSignedIn) {
                            OutlinedButton(
                                onClick = { viewModel.restoreLatestFromGoogleDrive() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = backupProgress !is EnhancedBackupService.BackupProgress.InProgress
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Restore Latest from Drive", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            
            // Available Backups Section Header
            item {
                if (backups.isNotEmpty()) {
                    Text(
                        "Available Backups (${backups.size})",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Available Backups List
            if (backups.isNotEmpty()) {
                items(
                    items = backups,
                    key = { it.file.absolutePath }
                ) { backup ->
                    BackupCard(
                        backup = backup,
                        onRestoreClick = { showRestoreDialog = backup },
                        onShareClick = { viewModel.shareBackup(backup) },
                        onDeleteClick = { showDeleteDialog = backup },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            } else {
                item {
                    EmptyBackupsView()
                }
            }
        }
    }
    
    // Restore Confirmation Dialog
    showRestoreDialog?.let { backup ->
        ModernConfirmDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = "Restore Backup",
            message = "Are you sure you want to restore this backup?\n\nCreated: ${backup.formattedDate}\n\n⚠️ This will replace all current data. The app will need to restart.",
            confirmText = "Restore",
            onConfirm = {
                viewModel.restoreBackup(backup)
                showRestoreDialog = null
            }
        )
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { backup ->
        ModernConfirmDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = "Delete Backup",
            message = "Are you sure you want to delete this backup? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteBackup(backup)
                showDeleteDialog = null
            },
            isDestructive = true
        )
    }
}

@Composable
private fun BackupCard(
    backup: BackupInfo,
    onRestoreClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (backup.isAutoBackup) Icons.Default.Schedule else Icons.Default.Backup,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            backup.formattedDate,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        backup.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Backup Type Badge
                if (backup.isAutoBackup) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            "Auto",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRestoreClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restore")
                }
                
                OutlinedButton(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
                
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBackupsView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                "No Backups Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Create your first backup to secure your data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
