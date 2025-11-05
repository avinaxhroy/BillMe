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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Backup & Restore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${backups.size} backups available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        
        // Track shown messages to prevent duplicate snackbars
        var lastShownSuccessTime by remember { mutableStateOf(0L) }
        var lastShownErrorMessage by remember { mutableStateOf<String?>(null) }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            
            // Backup Status Card - Matching the reference image style
            item {
                val lastBackup = backups.firstOrNull()
                val currentTime = remember { System.currentTimeMillis() }
                val isRecent = lastBackup?.let { 
                    (currentTime - it.timestamp) < 24 * 60 * 60 * 1000 // Less than 24 hours
                } ?: false
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecent) Color(0xFFE8F5E9) else Color(0xFFFFF3E0) // Green if recent, amber if old
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isRecent) Color(0xFFC8E6C9) else Color(0xFFFFE0B2), // Match card color
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    if (lastBackup != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isRecent) Color(0xFF4CAF50) else Color(0xFFF57C00), // Green if recent, orange if old
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Backup Status",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecent) Color(0xFF1B5E20) else Color(0xFFE65100)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (lastBackup != null) {
                                    val timeDiff = currentTime - lastBackup.timestamp
                                    val hours = timeDiff / (60 * 60 * 1000)
                                    val days = timeDiff / (24 * 60 * 60 * 1000)
                                    
                                    when {
                                        hours < 1 -> "Last backup: Just now"
                                        hours < 24 -> "Last backup: ${hours}h ago"
                                        days == 1L -> "Last backup: Yesterday"
                                        days < 7 -> "Last backup: ${days} days ago"
                                        else -> "Last backup: ${lastBackup.formattedDate}"
                                    }
                                } else {
                                    "No backups yet"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isRecent) Color(0xFF4CAF50) else Color(0xFFF57C00),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Progress Indicator - Only show when in progress
            item {
                when (val progress = backupProgress) {
                    is EnhancedBackupService.BackupProgress.InProgress -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    progress.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                LinearProgressIndicator(
                                    progress = { progress.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                )
                                Text(
                                    "${progress.progress.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is EnhancedBackupService.BackupProgress.Success -> {
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
            
            // Daily Auto-Backup Toggle - Matching reference image style
            item {
                var autoBackupEnabled by remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    autoBackupEnabled = viewModel.isAutoBackupEnabled()
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Daily Auto-Backup",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Auto backup once daily",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { enabled ->
                                autoBackupEnabled = enabled
                                viewModel.setAutoBackupEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2196F3),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE0E0E0)
                            )
                        )
                    }
                }
            }
            
            // Cloud Drive Sync - Matching reference image style
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Cloud Drive Sync",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (driveSignedIn) "Connected to Google Drive" 
                                    else "Not connected",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (driveSignedIn) 
                                        Color(0xFF2196F3)
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (driveSignedIn) 
                                    Color(0xFFE3F2FD) // Light blue like in image
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        if (driveSignedIn) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                                        contentDescription = if (driveSignedIn) "Connected" else "Not connected",
                                        tint = if (driveSignedIn) 
                                            Color(0xFF2196F3)
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        
                        if (driveSignedIn) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncToGoogleDrive() },
                                    modifier = Modifier.weight(1f),
                                    enabled = backupProgress !is EnhancedBackupService.BackupProgress.InProgress && driveUploadStatus !is DriveUploadStatus.Uploading,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    if (driveUploadStatus is DriveUploadStatus.Uploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CloudUpload, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (driveUploadStatus is DriveUploadStatus.Uploading) "Uploading..." else "Upload",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                
                                OutlinedButton(
                                    onClick = { viewModel.signOutFromDrive() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Disconnect",
                                        style = MaterialTheme.typography.labelLarge
                                    )
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
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Login, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Sign in with Google",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
            
            // Quick Actions Section - Matching reference image style
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Primary Actions Row - Matching image style
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.createBackup() },
                                modifier = Modifier.weight(1f),
                                enabled = backupProgress !is EnhancedBackupService.BackupProgress.InProgress,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3) // Blue like in image
                                ),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Backup Now",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Button(
                                onClick = { viewModel.createAndShareBackup() },
                                modifier = Modifier.weight(1f),
                                enabled = backupProgress !is EnhancedBackupService.BackupProgress.InProgress,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE3F2FD) // Light blue like in image
                                ),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(22.dp),
                                    tint = Color(0xFF2196F3)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Share",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                            }
                        }
                        
                        // Restore from Drive button - Matching image style
                        if (driveSignedIn) {
                            Button(
                                onClick = { viewModel.restoreLatestFromGoogleDrive() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = backupProgress !is EnhancedBackupService.BackupProgress.InProgress,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF2196F3)
                                ),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Icon(
                                    Icons.Default.History, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Restore Latest from Drive",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
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
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Available Backups List - Matching reference image style
            if (backups.isNotEmpty()) {
                items(
                    items = backups,
                    key = { it.file.absolutePath }
                ) { backup ->
                    ImprovedBackupCard(
                        backup = backup,
                        onRestoreClick = { showRestoreDialog = backup },
                        onDeleteClick = { showDeleteDialog = backup },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                
                // Bottom spacing
                item {
                    Spacer(Modifier.height(24.dp))
                }
            } else {
                item {
                    EmptyBackupsView()
                }
            }
        }
    }
    
    // Restore Confirmation Dialog - Improved design
    showRestoreDialog?.let { backup ->
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            icon = {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Restore Backup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to restore this backup?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Backup: ${backup.formattedDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Size: ${backup.formattedSize}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "This will replace all current data. The app will need to restart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(backup)
                        showRestoreDialog = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreDialog = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Delete Confirmation Dialog - Improved design
    showDeleteDialog?.let { backup ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Delete Backup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to delete this backup?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Backup: ${backup.formattedDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Size: ${backup.formattedSize}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackup(backup)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// Improved Backup Card - Matching reference image style exactly
@Composable
private fun ImprovedBackupCard(
    backup: BackupInfo,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top section with icon, info, and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon - Cloud for auto backup, Phone for manual
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE3F2FD), // Light blue like in image
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                if (backup.isAutoBackup) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Backup Info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            backup.formattedDate,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            backup.formattedSize,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Full-width Restore Button - Matching image style
            Button(
                onClick = onRestoreClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE3F2FD), // Light blue like in image
                    contentColor = Color(0xFF2196F3)
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyBackupsView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(96.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "No Backups Yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "Create your first backup to secure your data",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
