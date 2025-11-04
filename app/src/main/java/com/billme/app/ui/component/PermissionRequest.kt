package com.billme.app.ui.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Composable that provides permission request functionality for signature file access
 * 
 * This composable manages the permission request lifecycle and provides callbacks
 * for success and failure scenarios.
 */
@Composable
fun rememberSignaturePermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): PermissionLauncherWrapper {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allPermissionsGranted = permissions.values.all { it }
            if (allPermissionsGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    )
    
    return PermissionLauncherWrapper(
        permissionLauncher = permissionLauncher,
        context = context
    )
}

/**
 * Wrapper class to hold the permission launcher
 * This allows returning the launcher from the composable
 */
data class PermissionLauncherWrapper(
    val permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    val context: android.content.Context
) {
    fun requestPermissions(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }
}
