package com.billme.app.ui.screen.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billme.app.core.scanner.UnifiedIMEIScanner
import com.billme.app.core.scanner.IMEIScanMode
import com.billme.app.ui.component.UnifiedIMEIScannerDialog
import java.math.BigDecimal

@Composable
fun AddIMEIDialog(
    productName: String,
    defaultPurchasePrice: BigDecimal,
    onDismiss: () -> Unit,
    onAdd: (
        imeiNumber: String,
        imei2Number: String?,
        serialNumber: String?,
        purchasePrice: BigDecimal,
        boxNumber: String?,
        warrantyCardNumber: String?,
        notes: String?
    ) -> Unit,
    onBulkAdd: (List<Pair<String, String?>>) -> Unit = {},
    unifiedIMEIScanner: UnifiedIMEIScanner
) {
    var imeiNumber by remember { mutableStateOf("") }
    var imei2Number by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf(defaultPurchasePrice.toString()) }
    var boxNumber by remember { mutableStateOf("") }
    var warrantyCardNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showIMEIScanDialog by remember { mutableStateOf(false) }
    var showIMEI2ScanDialog by remember { mutableStateOf(false) }
    var showBulkScanDialog by remember { mutableStateOf(false) }
    var scanForIMEI2 by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("Add New IMEI")
                Text(
                    productName,
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
                // Bulk Scan Button
                OutlinedButton(
                    onClick = { showBulkScanDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Bulk Scan Multiple Phones",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                Text(
                    "Or add single IMEI manually:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = imeiNumber,
                    onValueChange = { imeiNumber = it },
                    label = { Text("IMEI Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && imeiNumber.isBlank(),
                    trailingIcon = {
                        IconButton(onClick = { showIMEIScanDialog = true }) {
                            Icon(Icons.Default.QrCodeScanner, "Scan IMEI")
                        }
                    }
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
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    label = { Text("Purchase Price *") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹") },
                    singleLine = true,
                    isError = showError && purchasePrice.toBigDecimalOrNull() == null
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
                
                if (showError) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (imeiNumber.isBlank()) {
                        showError = true
                        errorMessage = "IMEI number is required"
                        return@Button
                    }
                    
                    val price = purchasePrice.toBigDecimalOrNull()
                    if (price == null) {
                        showError = true
                        errorMessage = "Invalid purchase price"
                        return@Button
                    }
                    
                    onAdd(
                        imeiNumber.trim(),
                        imei2Number.trim().takeIf { it.isNotBlank() },
                        serialNumber.trim().takeIf { it.isNotBlank() },
                        price,
                        boxNumber.trim().takeIf { it.isNotBlank() },
                        warrantyCardNumber.trim().takeIf { it.isNotBlank() },
                        notes.trim().takeIf { it.isNotBlank() }
                    )
                }
            ) {
                Text("Add IMEI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Unified IMEI Scanner Dialog - Single/Dual IMEI
    if (showIMEIScanDialog || showIMEI2ScanDialog) {
        UnifiedIMEIScannerDialog(
            onDismiss = { 
                showIMEIScanDialog = false
                showIMEI2ScanDialog = false
                scanForIMEI2 = false
            },
            onIMEIScanned = { imeis ->
                if (imeis.isNotEmpty()) {
                    when {
                        scanForIMEI2 -> {
                            // Scanning for IMEI 2
                            imei2Number = imeis.first().imei
                        }
                        imeis.size == 1 -> {
                            // Single IMEI scanned
                            imeiNumber = imeis.first().imei
                        }
                        imeis.size >= 2 -> {
                            // Dual IMEI scanned
                            imeiNumber = imeis[0].imei
                            imei2Number = imeis[1].imei
                        }
                    }
                }
                showIMEIScanDialog = false
                showIMEI2ScanDialog = false
                scanForIMEI2 = false
            },
            scanMode = if (showIMEI2ScanDialog) IMEIScanMode.SINGLE else IMEIScanMode.AUTO,
            scanner = unifiedIMEIScanner,
            showManualEntry = true,
            title = if (showIMEI2ScanDialog) "Scan IMEI 2" else "Scan IMEI"
        ).also {
            if (showIMEI2ScanDialog) scanForIMEI2 = true
        }
    }
    
    // Bulk IMEI Scanner Dialog
    if (showBulkScanDialog) {
        UnifiedIMEIScannerDialog(
            onDismiss = { showBulkScanDialog = false },
            onIMEIScanned = { imeis ->
                // Extract just the IMEI strings from the scanned list
                // Use same logic as AddProduct screen
                if (imeis.isNotEmpty()) {
                    val imeiStrings = imeis.map { it.imei }
                    
                    // Pair up IMEIs: every 2 consecutive IMEIs = 1 device (IMEI1, IMEI2)
                    val groupedByPhone = mutableListOf<Pair<String, String?>>()
                    var i = 0
                    
                    while (i < imeiStrings.size) {
                        val imei1 = imeiStrings[i]
                        val imei2 = if (i + 1 < imeiStrings.size) imeiStrings[i + 1] else null
                        
                        groupedByPhone.add(Pair(imei1, imei2))
                        
                        // Move by 2 if we have a pair, or by 1 if only single IMEI left
                        i += if (imei2 != null) 2 else 1
                    }
                    
                    // Send all grouped phones to bulk add
                    if (groupedByPhone.isNotEmpty()) {
                        onBulkAdd(groupedByPhone)
                    }
                }
                showBulkScanDialog = false
            },
            scanMode = IMEIScanMode.BULK,
            scanner = unifiedIMEIScanner,
            showManualEntry = true,
            title = "Bulk Scan - Add Multiple Phones"
        )
    }
}
