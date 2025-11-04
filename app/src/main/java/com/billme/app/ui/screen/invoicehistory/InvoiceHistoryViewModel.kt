package com.billme.app.ui.screen.invoicehistory

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.pdf.InvoiceFileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class InvoiceHistoryUiState(
    val invoices: List<InvoiceFile> = emptyList(),
    val filteredInvoices: List<InvoiceFile> = emptyList(),
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val sortBy: SortBy = SortBy.DATE_DESC,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Advanced filter options
    val showAdvancedFilters: Boolean = false,
    val dateRangeStart: Date? = null,
    val dateRangeEnd: Date? = null,
    val minAmount: Double = 0.0,
    val maxAmount: Double = 100000.0,
    val selectedCustomerFilter: String = "",
    val selectedPaymentStatus: PaymentStatusFilter = PaymentStatusFilter.ALL,
    // Filter statistics
    val totalAmount: Double = 0.0,
    val averageAmount: Double = 0.0,
    // Bulk export
    val isBulkExporting: Boolean = false,
    val bulkExportProgress: Int = 0,
    val bulkExportTotal: Int = 0,
    val bulkExportMessage: String? = null
)

data class InvoiceFile(
    val file: File,
    val transactionNumber: String,
    val date: Date,
    val type: InvoiceType,
    val formattedDate: String,
    val fileSize: String,
    // Additional metadata for advanced filtering
    val amount: Double = 0.0,
    val customerName: String = "",
    val paymentStatus: String = "Unknown"
)

enum class InvoiceType {
    CUSTOMER, OWNER
}

enum class FilterType {
    ALL, CUSTOMER, OWNER, TODAY, THIS_WEEK, THIS_MONTH, CUSTOM_RANGE
}

enum class SortBy {
    DATE_DESC, DATE_ASC, SIZE_DESC, SIZE_ASC, AMOUNT_DESC, AMOUNT_ASC
}

enum class PaymentStatusFilter {
    ALL, PAID, PENDING, PARTIAL, OVERDUE
}

@HiltViewModel
class InvoiceHistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: InvoiceFileManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InvoiceHistoryUiState())
    val uiState: StateFlow<InvoiceHistoryUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    
    init {
        loadInvoices()
    }
    
    fun loadInvoices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val files = fileManager.getAllInvoices()
                val invoiceFiles = files.mapNotNull { file ->
                    parseInvoiceFile(file)
                }
                
                _uiState.update { 
                    it.copy(
                        invoices = invoiceFiles,
                        filteredInvoices = applyFiltersAndSort(invoiceFiles, it.searchQuery, it.filterType, it.sortBy),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load invoices: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun parseInvoiceFile(file: File): InvoiceFile? {
        return try {
            // File name format: Invoice_TXN12345_Customer.pdf or Invoice_TXN12345_Owner.pdf
            // Enhanced format: Invoice_TXN12345_Customer_Amount_CustomerName.pdf (optional)
            val fileName = file.nameWithoutExtension
            val parts = fileName.split("_")
            
            if (parts.size >= 3) {
                val transactionNumber = parts[1]
                val type = if (parts[2].contains("Customer", ignoreCase = true)) {
                    InvoiceType.CUSTOMER
                } else {
                    InvoiceType.OWNER
                }
                
                val date = Date(file.lastModified())
                val formattedDate = dateFormat.format(date)
                val fileSize = formatFileSize(file.length())
                
                // Extract amount and customer name if available
                val amount = if (parts.size > 3) {
                    parts[3].toDoubleOrNull() ?: 0.0
                } else {
                    0.0
                }
                
                val customerName = if (parts.size > 4) {
                    parts.drop(4).joinToString(" ").replace("-", " ")
                } else {
                    ""
                }
                
                // Determine payment status based on file age (mock logic)
                val paymentStatus = when {
                    date.time < System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 -> "Paid"
                    date.time < System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 -> "Pending"
                    else -> "Recent"
                }
                
                InvoiceFile(
                    file = file,
                    transactionNumber = transactionNumber,
                    date = date,
                    type = type,
                    formattedDate = formattedDate,
                    fileSize = fileSize,
                    amount = amount,
                    customerName = customerName,
                    paymentStatus = paymentStatus
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _uiState.update { 
            it.copy(
                searchQuery = query,
                filteredInvoices = applyFiltersAndSort(it.invoices, query, it.filterType, it.sortBy)
            )
        }
    }
    
    fun onFilterChange(filter: FilterType) {
        _uiState.update { 
            it.copy(
                filterType = filter,
                filteredInvoices = applyFiltersAndSort(it.invoices, it.searchQuery, filter, it.sortBy)
            )
        }
    }
    
    fun onSortChange(sort: SortBy) {
        _uiState.update { 
            it.copy(
                sortBy = sort,
                filteredInvoices = applyFiltersAndSort(it.invoices, it.searchQuery, it.filterType, sort)
            )
        }
    }
    
    // Alias methods for compatibility
    fun setSortBy(sort: SortBy) {
        onSortChange(sort)
    }
    
    fun setSearchQuery(query: String) {
        onSearchQueryChange(query)
    }
    
    fun setFilter(filter: FilterType) {
        onFilterChange(filter)
    }
    
    // Advanced filter methods
    fun toggleAdvancedFilters() {
        _uiState.update { it.copy(showAdvancedFilters = !it.showAdvancedFilters) }
    }
    
    fun setDateRange(start: Date?, end: Date?) {
        _uiState.update { 
            val updated = it.copy(
                dateRangeStart = start,
                dateRangeEnd = end,
                filterType = if (start != null || end != null) FilterType.CUSTOM_RANGE else it.filterType
            )
            updated.copy(filteredInvoices = applyAdvancedFilters(updated))
        }
    }
    
    fun setAmountRange(min: Double, max: Double) {
        _uiState.update { 
            val updated = it.copy(minAmount = min, maxAmount = max)
            updated.copy(filteredInvoices = applyAdvancedFilters(updated))
        }
    }
    
    fun setCustomerFilter(customer: String) {
        _uiState.update { 
            val updated = it.copy(selectedCustomerFilter = customer)
            updated.copy(filteredInvoices = applyAdvancedFilters(updated))
        }
    }
    
    fun setPaymentStatusFilter(status: PaymentStatusFilter) {
        _uiState.update { 
            val updated = it.copy(selectedPaymentStatus = status)
            updated.copy(filteredInvoices = applyAdvancedFilters(updated))
        }
    }
    
    fun clearAllFilters() {
        _uiState.update { 
            val cleared = it.copy(
                searchQuery = "",
                filterType = FilterType.ALL,
                dateRangeStart = null,
                dateRangeEnd = null,
                minAmount = 0.0,
                maxAmount = 100000.0,
                selectedCustomerFilter = "",
                selectedPaymentStatus = PaymentStatusFilter.ALL
            )
            cleared.copy(filteredInvoices = applyAdvancedFilters(cleared))
        }
    }
    
    private fun applyAdvancedFilters(state: InvoiceHistoryUiState): List<InvoiceFile> {
        var filtered = state.invoices
        
        // Apply search
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter { invoice ->
                invoice.transactionNumber.contains(state.searchQuery, ignoreCase = true) ||
                invoice.formattedDate.contains(state.searchQuery, ignoreCase = true) ||
                invoice.customerName.contains(state.searchQuery, ignoreCase = true)
            }
        }
        
        // Apply basic filter type
        filtered = when (state.filterType) {
            FilterType.ALL -> filtered
            FilterType.CUSTOMER -> filtered.filter { it.type == InvoiceType.CUSTOMER }
            FilterType.OWNER -> filtered.filter { it.type == InvoiceType.OWNER }
            FilterType.TODAY -> {
                val today = Calendar.getInstance()
                filtered.filter { invoice ->
                    val invoiceDate = Calendar.getInstance().apply { time = invoice.date }
                    today.get(Calendar.YEAR) == invoiceDate.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == invoiceDate.get(Calendar.DAY_OF_YEAR)
                }
            }
            FilterType.THIS_WEEK -> {
                val weekAgo = Calendar.getInstance().apply { 
                    add(Calendar.DAY_OF_YEAR, -7) 
                }
                filtered.filter { it.date.after(weekAgo.time) }
            }
            FilterType.THIS_MONTH -> {
                val thisMonth = Calendar.getInstance()
                filtered.filter { invoice ->
                    val invoiceDate = Calendar.getInstance().apply { time = invoice.date }
                    thisMonth.get(Calendar.YEAR) == invoiceDate.get(Calendar.YEAR) &&
                    thisMonth.get(Calendar.MONTH) == invoiceDate.get(Calendar.MONTH)
                }
            }
            FilterType.CUSTOM_RANGE -> {
                filtered.filter { invoice ->
                    val afterStart = state.dateRangeStart?.let { invoice.date.after(it) || invoice.date == it } ?: true
                    val beforeEnd = state.dateRangeEnd?.let { invoice.date.before(it) || invoice.date == it } ?: true
                    afterStart && beforeEnd
                }
            }
        }
        
        // Apply amount range filter
        if (state.minAmount > 0.0 || state.maxAmount < 100000.0) {
            filtered = filtered.filter { invoice ->
                invoice.amount >= state.minAmount && invoice.amount <= state.maxAmount
            }
        }
        
        // Apply customer filter
        if (state.selectedCustomerFilter.isNotBlank()) {
            filtered = filtered.filter { invoice ->
                invoice.customerName.contains(state.selectedCustomerFilter, ignoreCase = true)
            }
        }
        
        // Apply payment status filter
        if (state.selectedPaymentStatus != PaymentStatusFilter.ALL) {
            filtered = filtered.filter { invoice ->
                when (state.selectedPaymentStatus) {
                    PaymentStatusFilter.PAID -> invoice.paymentStatus.equals("Paid", ignoreCase = true)
                    PaymentStatusFilter.PENDING -> invoice.paymentStatus.equals("Pending", ignoreCase = true)
                    PaymentStatusFilter.PARTIAL -> invoice.paymentStatus.equals("Partial", ignoreCase = true)
                    PaymentStatusFilter.OVERDUE -> invoice.paymentStatus.equals("Overdue", ignoreCase = true)
                    PaymentStatusFilter.ALL -> true
                }
            }
        }
        
        // Apply sort
        val sorted = when (state.sortBy) {
            SortBy.DATE_DESC -> filtered.sortedByDescending { it.date }
            SortBy.DATE_ASC -> filtered.sortedBy { it.date }
            SortBy.SIZE_DESC -> filtered.sortedByDescending { it.file.length() }
            SortBy.SIZE_ASC -> filtered.sortedBy { it.file.length() }
            SortBy.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            SortBy.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }
        
        // Calculate statistics
        val totalAmount = sorted.sumOf { it.amount }
        val averageAmount = if (sorted.isNotEmpty()) totalAmount / sorted.size else 0.0
        
        // Update statistics in state (need to do this separately)
        viewModelScope.launch {
            _uiState.update { 
                it.copy(totalAmount = totalAmount, averageAmount = averageAmount) 
            }
        }
        
        return sorted
    }
    
    private fun applyFiltersAndSort(
        invoices: List<InvoiceFile>,
        query: String,
        filter: FilterType,
        sort: SortBy
    ): List<InvoiceFile> {
        // Use the advanced filter method with current state
        val currentState = _uiState.value.copy(
            invoices = invoices,
            searchQuery = query,
            filterType = filter,
            sortBy = sort
        )
        return applyAdvancedFilters(currentState)
    }
    
    fun shareInvoice(invoice: InvoiceFile) {
        viewModelScope.launch {
            try {
                val intent = fileManager.createShareIntent(invoice.file)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to share: ${e.message}")
                }
            }
        }
    }
    
    fun viewInvoice(invoice: InvoiceFile) {
        viewModelScope.launch {
            try {
                val intent = fileManager.createViewIntent(invoice.file)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to view invoice: ${e.message}")
                }
            }
        }
    }
    
    fun deleteInvoice(invoice: InvoiceFile) {
        viewModelScope.launch {
            try {
                val success = fileManager.deleteInvoice(invoice.file)
                if (success) {
                    loadInvoices() // Reload list
                } else {
                    _uiState.update { 
                        it.copy(errorMessage = "Failed to delete invoice")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Error deleting: ${e.message}")
                }
            }
        }
    }
    
    fun deleteOldInvoices(daysToKeep: Int) {
        viewModelScope.launch {
            try {
                val deletedCount = fileManager.deleteOldInvoices(daysToKeep)
                _uiState.update { 
                    it.copy(errorMessage = "Deleted $deletedCount old invoices")
                }
                loadInvoices()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Error: ${e.message}")
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, bulkExportMessage = null) }
    }
    
    // ============================================
    // Bulk Export Methods
    // ============================================
    
    /**
     * Export all filtered invoices as a single merged PDF
     */
    fun exportFilteredInvoices() {
        viewModelScope.launch {
            val filteredInvoices = _uiState.value.filteredInvoices
            
            if (filteredInvoices.isEmpty()) {
                _uiState.update { 
                    it.copy(errorMessage = "No invoices to export") 
                }
                return@launch
            }
            
            _uiState.update { 
                it.copy(
                    isBulkExporting = true,
                    bulkExportProgress = 0,
                    bulkExportTotal = filteredInvoices.size,
                    bulkExportMessage = "Preparing export..."
                ) 
            }
            
            try {
                val invoiceFiles = filteredInvoices.map { it.file }
                val totalAmount = _uiState.value.totalAmount
                val dateRange = getDateRangeString()
                
                val result = fileManager.mergeInvoicesWithSummary(
                    invoiceFiles = invoiceFiles,
                    totalAmount = totalAmount,
                    invoiceCount = filteredInvoices.size,
                    dateRange = dateRange,
                    progressCallback = { current, total ->
                        _uiState.update { 
                            it.copy(
                                bulkExportProgress = current,
                                bulkExportTotal = total,
                                bulkExportMessage = "Merging invoice $current of $total..."
                            ) 
                        }
                    }
                )
                
                when (result) {
                    is InvoiceFileManager.MergeResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isBulkExporting = false,
                                bulkExportMessage = "Successfully exported ${result.mergedCount} invoices!",
                                errorMessage = null
                            ) 
                        }
                        
                        // Automatically share the merged file
                        val intent = fileManager.createBulkShareIntent(
                            result.file,
                            result.summary
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                    is InvoiceFileManager.MergeResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isBulkExporting = false,
                                errorMessage = result.message
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isBulkExporting = false,
                        errorMessage = "Export failed: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Print all filtered invoices as a single merged PDF
     */
    fun printFilteredInvoices() {
        viewModelScope.launch {
            val filteredInvoices = _uiState.value.filteredInvoices
            
            if (filteredInvoices.isEmpty()) {
                _uiState.update { 
                    it.copy(errorMessage = "No invoices to print") 
                }
                return@launch
            }
            
            _uiState.update { 
                it.copy(
                    isBulkExporting = true,
                    bulkExportMessage = "Preparing for print..."
                ) 
            }
            
            try {
                val invoiceFiles = filteredInvoices.map { it.file }
                
                val result = fileManager.mergeInvoicesWithSummary(
                    invoiceFiles = invoiceFiles,
                    totalAmount = _uiState.value.totalAmount,
                    invoiceCount = filteredInvoices.size,
                    dateRange = getDateRangeString(),
                    progressCallback = { current, total ->
                        _uiState.update { 
                            it.copy(
                                bulkExportProgress = current,
                                bulkExportTotal = total
                            ) 
                        }
                    }
                )
                
                when (result) {
                    is InvoiceFileManager.MergeResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isBulkExporting = false,
                                bulkExportMessage = "Sending to printer..."
                            ) 
                        }
                        
                        fileManager.printPdf(result.file, "Bulk_Invoices_Export")
                        
                        _uiState.update { 
                            it.copy(bulkExportMessage = "Print job sent successfully!") 
                        }
                    }
                    is InvoiceFileManager.MergeResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isBulkExporting = false,
                                errorMessage = result.message
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isBulkExporting = false,
                        errorMessage = "Print failed: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Save merged PDF of filtered invoices
     */
    fun saveFilteredInvoices() {
        viewModelScope.launch {
            val filteredInvoices = _uiState.value.filteredInvoices
            
            if (filteredInvoices.isEmpty()) {
                _uiState.update { 
                    it.copy(errorMessage = "No invoices to save") 
                }
                return@launch
            }
            
            _uiState.update { 
                it.copy(
                    isBulkExporting = true,
                    bulkExportMessage = "Saving..."
                ) 
            }
            
            try {
                val invoiceFiles = filteredInvoices.map { it.file }
                
                val result = fileManager.mergeInvoicesWithSummary(
                    invoiceFiles = invoiceFiles,
                    totalAmount = _uiState.value.totalAmount,
                    invoiceCount = filteredInvoices.size,
                    dateRange = getDateRangeString(),
                    progressCallback = { current, total ->
                        _uiState.update { 
                            it.copy(
                                bulkExportProgress = current,
                                bulkExportTotal = total
                            ) 
                        }
                    }
                )
                
                when (result) {
                    is InvoiceFileManager.MergeResult.Success -> {
                        val saveResult = fileManager.saveBulkExportToDownloads(result.file)
                        
                        when (saveResult) {
                            is InvoiceFileManager.SaveResult.Success -> {
                                _uiState.update { 
                                    it.copy(
                                        isBulkExporting = false,
                                        bulkExportMessage = "Saved successfully!\n${saveResult.message}"
                                    ) 
                                }
                            }
                            is InvoiceFileManager.SaveResult.Error -> {
                                _uiState.update { 
                                    it.copy(
                                        isBulkExporting = false,
                                        errorMessage = saveResult.message
                                    ) 
                                }
                            }
                        }
                    }
                    is InvoiceFileManager.MergeResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isBulkExporting = false,
                                errorMessage = result.message
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isBulkExporting = false,
                        errorMessage = "Save failed: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    private fun getDateRangeString(): String {
        val state = _uiState.value
        return when {
            state.dateRangeStart != null && state.dateRangeEnd != null -> {
                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                "${format.format(state.dateRangeStart)} - ${format.format(state.dateRangeEnd)}"
            }
            state.filterType == FilterType.TODAY -> "Today"
            state.filterType == FilterType.THIS_WEEK -> "This Week"
            state.filterType == FilterType.THIS_MONTH -> "This Month"
            else -> ""
        }
    }
}
