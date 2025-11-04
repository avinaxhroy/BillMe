package com.billme.app.ui.screen.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billme.app.core.scanner.IMEIScanMode
import com.billme.app.core.scanner.UnifiedIMEIScanner
import com.billme.app.data.local.entity.ProductIMEI
import com.billme.app.ui.component.UnifiedIMEIScannerDialog

@Composable
fun EditIMEIDialog(
    imei: ProductIMEI,
    onDismiss: () -> Unit,
    onSave: (ProductIMEI) -> Unit,
    scanner: UnifiedIMEIScanner
) {
    var imei2Number by remember { mutableStateOf(imei.imei2Number ?: "") }
    var serialNumber by remember { mutableStateOf(imei.serialNumber ?: "") }
    var boxNumber by remember { mutableStateOf(imei.boxNumber ?: "") }
    var warrantyCardNumber by remember { mutableStateOf(imei.warrantyCardNumber ?: "") }
    var notes by remember { mutableStateOf(imei.notes ?: "") }
    var showIMEI2ScanDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("Edit IMEI")
                Text(
                    imei.imeiNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "IMEI numbers and purchase details cannot be edited",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = imei2Number,
                    onValueChange = { imei2Number = it },
                    label = { Text("IMEI 2 (Dual SIM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showIMEI2ScanDialog = true }) {
                            Icon(Icons.Default.QrCodeScanner, "Scan IMEI 2")
                        }
                    }
                )
                
                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Serial Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = boxNumber,
                    onValueChange = { boxNumber = it },
                    label = { Text("Box Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = warrantyCardNumber,
                    onValueChange = { warrantyCardNumber = it },
                    label = { Text("Warranty Card Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = imei.copy(
                        imei2Number = imei2Number.trim().takeIf { it.isNotBlank() },
                        serialNumber = serialNumber.trim().takeIf { it.isNotBlank() },
                        boxNumber = boxNumber.trim().takeIf { it.isNotBlank() },
                        warrantyCardNumber = warrantyCardNumber.trim().takeIf { it.isNotBlank() },
                        notes = notes.trim().takeIf { it.isNotBlank() }
                    )
                    onSave(updated)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // IMEI 2 Scan Dialog
    if (showIMEI2ScanDialog) {
        UnifiedIMEIScannerDialog(
            onDismiss = { showIMEI2ScanDialog = false },
            onIMEIScanned = { scannedList ->
                if (scannedList.isNotEmpty()) {
                    imei2Number = scannedList.first().imei
                }
                showIMEI2ScanDialog = false
            },
            scanMode = IMEIScanMode.SINGLE,
            scanner = scanner
        )
    }
}
