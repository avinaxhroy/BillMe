package com.billme.app.ui.screen.invoicehistory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.ui.screen.invoicehistory.FilterType
import com.billme.app.ui.screen.invoicehistory.SortBy
import com.billme.app.ui.screen.invoicehistory.PaymentStatusFilter
import com.billme.app.ui.screen.invoicehistory.InvoiceFile
import com.billme.app.ui.screen.invoicehistory.InvoiceType
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: InvoiceHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val invoices = state.filteredInvoices
    val searchQuery = state.searchQuery
    val currentFilter = state.filterType
    val currentSort = state.sortBy
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var invoiceToDelete by remember { mutableStateOf<InvoiceFile?>(null) }
    
    // Handle bulk export completion messages
    LaunchedEffect(state.isBulkExporting, state.bulkExportMessage) {
        if (!state.isBulkExporting && !state.bulkExportMessage.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(
                message = state.bulkExportMessage ?: "",
                duration = SnackbarDuration.Long
            )
        }
    }
    
    Scaffold(
        topBar = {
            ModernLargeTopAppBar(
                title = "Invoice History",
                subtitle = "${invoices.size} invoices",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                useGradient = true,
                gradientColors = listOf(Secondary, SecondaryLight),
                actions = {
                    // Bulk Export Menu
                    var showBulkMenu by remember { mutableStateOf(false) }
                    
                    if (invoices.isNotEmpty()) {
                        IconButton(onClick = { showBulkMenu = true }) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Bulk Export",
                                tint = Color.White
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showBulkMenu,
                            onDismissRequest = { showBulkMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export & Share All") },
                                onClick = {
                                    viewModel.exportFilteredInvoices()
                                    showBulkMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Print All") },
                                onClick = {
                                    viewModel.printFilteredInvoices()
                                    showBulkMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Save Merged PDF") },
                                onClick = {
                                    viewModel.saveFilteredInvoices()
                                    showBulkMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                            )
                        }
                    }
                    
                    AnimatedIconButton(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        onClick = { showSortMenu = true },
                        contentDescription = "Sort"
                    )
                    
                    AnimatedIconButton(
                        icon = Icons.Default.DeleteSweep,
                        onClick = { showDeleteAllDialog = true },
                        contentDescription = "Delete Old"
                    )
                    
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Date (Newest First)") },
                            onClick = {
                                viewModel.setSortBy(SortBy.DATE_DESC)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (currentSort == SortBy.DATE_DESC) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Date (Oldest First)") },
                            onClick = {
                                viewModel.setSortBy(SortBy.DATE_ASC)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (currentSort == SortBy.DATE_ASC) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Amount (Highest)") },
                            onClick = {
                                viewModel.setSortBy(SortBy.AMOUNT_DESC)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (currentSort == SortBy.AMOUNT_DESC) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Amount (Lowest)") },
                            onClick = {
                                viewModel.setSortBy(SortBy.AMOUNT_ASC)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (currentSort == SortBy.AMOUNT_ASC) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Size (Largest)") },
                            onClick = {
                                viewModel.setSortBy(SortBy.SIZE_DESC)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (currentSort == SortBy.SIZE_DESC) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Size (Smallest)") },
                            onClick = {
                                viewModel.setSortBy(SortBy.SIZE_ASC)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (currentSort == SortBy.SIZE_ASC) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by transaction number or date...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Bulk Export Progress Indicator
            if (state.isBulkExporting) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Exporting Invoices...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${state.bulkExportProgress}/${state.bulkExportTotal}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { 
                                if (state.bulkExportTotal > 0) {
                                    state.bulkExportProgress.toFloat() / state.bulkExportTotal
                                } else 0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (!state.bulkExportMessage.isNullOrEmpty()) {
                            Text(
                                text = state.bulkExportMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Advanced Filters Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { viewModel.toggleAdvancedFilters() }
                ) {
                    Icon(
                        if (state.showAdvancedFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Advanced Filters")
                }
                
                if (state.filteredInvoices.isNotEmpty()) {
                    Text(
                        "${state.filteredInvoices.size} invoices • Total: ₹${String.format("%.2f", state.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Advanced Filters Panel
            AnimatedVisibility(visible = state.showAdvancedFilters) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Advanced Filters",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Amount Range Filter
                        Column {
                            Text(
                                "Amount Range: ₹${state.minAmount.toInt()} - ₹${state.maxAmount.toInt()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.minAmount.toInt().toString(),
                                    onValueChange = { 
                                        it.toDoubleOrNull()?.let { min -> 
                                            viewModel.setAmountRange(min, state.maxAmount) 
                                        }
                                    },
                                    label = { Text("Min") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = state.maxAmount.toInt().toString(),
                                    onValueChange = { 
                                        it.toDoubleOrNull()?.let { max -> 
                                            viewModel.setAmountRange(state.minAmount, max) 
                                        }
                                    },
                                    label = { Text("Max") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                        
                        // Customer Name Filter
                        OutlinedTextField(
                            value = state.selectedCustomerFilter,
                            onValueChange = { viewModel.setCustomerFilter(it) },
                            label = { Text("Customer Name") },
                            placeholder = { Text("Filter by customer...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        
                        // Payment Status Filter
                        Column {
                            Text(
                                "Payment Status",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = state.selectedPaymentStatus == PaymentStatusFilter.ALL,
                                        onClick = { viewModel.setPaymentStatusFilter(PaymentStatusFilter.ALL) },
                                        label = { Text("All") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = state.selectedPaymentStatus == PaymentStatusFilter.PAID,
                                        onClick = { viewModel.setPaymentStatusFilter(PaymentStatusFilter.PAID) },
                                        label = { Text("Paid") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = state.selectedPaymentStatus == PaymentStatusFilter.PENDING,
                                        onClick = { viewModel.setPaymentStatusFilter(PaymentStatusFilter.PENDING) },
                                        label = { Text("Pending") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = state.selectedPaymentStatus == PaymentStatusFilter.PARTIAL,
                                        onClick = { viewModel.setPaymentStatusFilter(PaymentStatusFilter.PARTIAL) },
                                        label = { Text("Partial") }
                                    )
                                }
                            }
                        }
                        
                        // Clear Filters Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.clearAllFilters() }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Clear All Filters")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = currentFilter == FilterType.ALL,
                        onClick = { viewModel.setFilter(FilterType.ALL) },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == FilterType.CUSTOMER,
                        onClick = { viewModel.setFilter(FilterType.CUSTOMER) },
                        label = { Text("Customer Invoices") }
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == FilterType.OWNER,
                        onClick = { viewModel.setFilter(FilterType.OWNER) },
                        label = { Text("Owner Invoices") }
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == FilterType.TODAY,
                        onClick = { viewModel.setFilter(FilterType.TODAY) },
                        label = { Text("Today") },
                        leadingIcon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == FilterType.THIS_WEEK,
                        onClick = { viewModel.setFilter(FilterType.THIS_WEEK) },
                        label = { Text("This Week") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == FilterType.THIS_MONTH,
                        onClick = { viewModel.setFilter(FilterType.THIS_MONTH) },
                        label = { Text("This Month") },
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Invoice List
            if (invoices.isEmpty()) {
                EmptyStateView(
                    hasSearchQuery = searchQuery.isNotEmpty(),
                    hasFilter = currentFilter != FilterType.ALL
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = invoices,
                        key = { it.file.absolutePath }
                    ) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            onShareClick = { viewModel.shareInvoice(invoice) },
                            onDeleteClick = { invoiceToDelete = invoice },
                            onInvoiceClick = { viewModel.viewInvoice(invoice) }
                        )
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    invoiceToDelete?.let { invoice ->
        ModernConfirmDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = "Delete Invoice",
            message = "Are you sure you want to delete invoice ${invoice.transactionNumber}? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteInvoice(invoice)
                invoiceToDelete = null
            },
            isDestructive = true
        )
    }
    
    // Delete All Dialog
    if (showDeleteAllDialog) {
        var daysInput by remember { mutableStateOf("30") }
        
        ModernInputDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = "Delete Old Invoices",
            label = "Days",
            placeholder = "30",
            initialValue = daysInput,
            confirmText = "Delete",
            onConfirm = { days ->
                val daysInt = days.toIntOrNull() ?: 30
                viewModel.deleteOldInvoices(daysInt)
                showDeleteAllDialog = false
            }
        )
    }
}

@Composable
private fun InvoiceCard(
    invoice: InvoiceFile,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onInvoiceClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInvoiceClick),
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
                    Text(
                        invoice.transactionNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        invoice.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show customer name if available
                    if (invoice.customerName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                invoice.customerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Invoice Type Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (invoice.type) {
                            InvoiceType.CUSTOMER -> MaterialTheme.colorScheme.primaryContainer
                            InvoiceType.OWNER -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            invoice.type.name,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Show amount if available
                    if (invoice.amount > 0.0) {
                        Text(
                            "₹${String.format("%.2f", invoice.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        invoice.fileSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Payment Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (invoice.paymentStatus.lowercase()) {
                        "paid" -> MaterialTheme.colorScheme.tertiaryContainer
                        "pending" -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        invoice.paymentStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onShareClick) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
                
                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    hasSearchQuery: Boolean,
    hasFilter: Boolean
) {
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
                Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                when {
                    hasSearchQuery -> "No invoices found"
                    hasFilter -> "No invoices in this filter"
                    else -> "No invoices yet"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                when {
                    hasSearchQuery -> "Try a different search term"
                    hasFilter -> "Try changing the filter"
                    else -> "Generated invoices will appear here"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
