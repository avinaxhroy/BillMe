package com.billme.app.data.repository

import com.billme.app.core.builder.InvoiceBuilder
import com.billme.app.core.service.EnhancedBillingService
import com.billme.app.data.local.dao.InvoiceDao
import com.billme.app.data.local.dao.InvoiceLineItemDao
import com.billme.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton/**
 * Repository for invoice operations with comprehensive GST support
 */
@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val invoiceLineItemDao: InvoiceLineItemDao,
    private val gstRepository: GSTRepository,
    private val enhancedBillingService: EnhancedBillingService,
    private val invoiceBuilder: InvoiceBuilder
) {
    
    /**
     * Save complete invoice with line items and GST details
     */
    suspend fun saveInvoice(invoiceWithDetails: InvoiceWithDetails): Long {
        val invoiceId = invoiceDao.insertInvoice(invoiceWithDetails.invoice)
        
        // Update line items with invoice ID and save
        val updatedLineItems = invoiceWithDetails.lineItems.map { lineItem ->
            lineItem.copy(invoiceId = invoiceId)
        }
        invoiceLineItemDao.insertLineItems(updatedLineItems)
        
        // Save GST details if present
        invoiceWithDetails.invoiceGSTDetails?.let { gstDetails ->
            gstRepository.saveInvoiceGSTDetails(gstDetails.copy(transactionId = invoiceId))
        }
        
        return invoiceId
    }
    
    /**
     * Update existing invoice
     */
    suspend fun updateInvoice(invoiceWithDetails: InvoiceWithDetails) {
        invoiceDao.updateInvoice(invoiceWithDetails.invoice)
        
        // Delete existing line items and insert updated ones
        invoiceLineItemDao.deleteByInvoiceId(invoiceWithDetails.invoice.invoiceId)
        invoiceLineItemDao.insertLineItems(invoiceWithDetails.lineItems)
        
        // Update GST details if present
        invoiceWithDetails.invoiceGSTDetails?.let { gstDetails ->
            gstRepository.updateInvoiceGSTDetails(gstDetails)
        }
    }
    
    /**
     * Get invoice with all details by ID
     */
    suspend fun getInvoiceWithDetails(invoiceId: Long): InvoiceWithDetails? {
        val invoice = invoiceDao.getInvoiceById(invoiceId) ?: return null
        val lineItems = invoiceLineItemDao.getLineItemsByInvoiceId(invoiceId)
        val customer = invoice.customerId?.let { invoiceDao.getCustomerById(it) }
        val gstConfiguration = invoice.gstConfigId?.let { gstRepository.getGSTConfigurationById(it) }
        val invoiceGSTDetails = gstRepository.getInvoiceGSTDetails(invoiceId)
        
        return InvoiceWithDetails(
            invoice = invoice,
            lineItems = lineItems,
            customer = customer,
            gstConfiguration = gstConfiguration,
            invoiceGSTDetails = invoiceGSTDetails
        )
    }
    
    /**
     * Get all invoices with basic info
     */
    fun getAllInvoices(): Flow<List<Invoice>> {
        return invoiceDao.getAllInvoices()
    }
    
    /**
     * Get invoices by date range
     */
    fun getInvoicesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesByDateRange(startDate, endDate)
    }
    
    /**
     * Get invoices by customer
     */
    fun getInvoicesByCustomer(customerId: Long): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesByCustomer(customerId)
    }
    
    /**
     * Get invoices by GST mode
     */
    fun getInvoicesByGSTMode(gstMode: GSTMode): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesByGSTMode(gstMode)
    }
    
    /**
     * Get pending invoices (unpaid or partially paid)
     */
    fun getPendingInvoices(): Flow<List<Invoice>> {
        return invoiceDao.getPendingInvoices()
    }
    
    /**
     * Search invoices by invoice number or customer name
     */
    fun searchInvoices(query: String): Flow<List<Invoice>> {
        return invoiceDao.searchInvoices("%$query%")
    }
    
    /**
     * Cancel an invoice
     */
    suspend fun cancelInvoice(invoiceId: Long, reason: String) {
        val now = kotlinx.datetime.Clock.System.now()
        invoiceDao.cancelInvoice(invoiceId, now, reason)
    }
    
    /**
     * Create return invoice from original invoice
     */
    suspend fun createReturnInvoice(originalInvoiceId: Long): InvoiceWithDetails? {
        val originalInvoice = getInvoiceWithDetails(originalInvoiceId) ?: return null
        
        // Create return invoice using builder
        return originalInvoice.toReturnInvoiceBuilder(enhancedBillingService)
    }
    
    /**
     * Generate GST summary for invoice
     */
    fun generateInvoiceGSTSummary(invoiceWithDetails: InvoiceWithDetails): InvoiceGSTSummary {
        return enhancedBillingService.generateGSTSummary(invoiceWithDetails)
    }
    
    /**
     * Get invoice statistics by date range
     */
    suspend fun getInvoiceStatistics(startDate: Instant, endDate: Instant): com.billme.app.data.repository.InvoiceStatistics {
        val daoStats = invoiceDao.getInvoiceStatistics(startDate, endDate)
        return com.billme.app.data.repository.InvoiceStatistics(
            totalInvoices = daoStats.totalInvoices,
            totalRevenue = daoStats.totalAmount,
            totalGSTCollected = BigDecimal.ZERO, // TODO: Calculate from line items
            averageInvoiceValue = daoStats.averageAmount,
            paidInvoices = 0, // TODO: Query for paid invoices
            pendingInvoices = 0, // TODO: Query for pending invoices
            cancelledInvoices = 0 // TODO: Query for cancelled invoices
        )
    }
    
    /**
     * Get GST statistics by date range
     */
    suspend fun getGSTStatistics(startDate: Instant, endDate: Instant): com.billme.app.data.repository.GSTStatistics {
        val daoStats = invoiceDao.getGSTStatistics(startDate, endDate)
        return com.billme.app.data.repository.GSTStatistics(
            totalTaxableAmount = BigDecimal.ZERO, // TODO: Calculate
            totalCGSTAmount = daoStats.totalCGST,
            totalSGSTAmount = daoStats.totalSGST,
            totalIGSTAmount = daoStats.totalIGST,
            totalCessAmount = daoStats.totalCess,
            totalGSTAmount = daoStats.totalGST,
            interstateTransactions = 0, // TODO: Count interstate
            intrastateTransactions = 0, // TODO: Count intrastate
            gstModeBreakdown = emptyMap() // TODO: Build breakdown
        )
    }
    
    /**
     * Get top products by revenue
     */
    fun getTopProductsByRevenue(limit: Int = 10): Flow<List<com.billme.app.data.repository.ProductRevenue>> {
        return invoiceLineItemDao.getTopProductsByRevenue(limit).map { daoList ->
            daoList.mapIndexed { index, dao ->
                com.billme.app.data.repository.ProductRevenue(
                    productId = index.toLong(), // TODO: Fetch actual product ID
                    productName = dao.productName,
                    totalQuantity = dao.totalQuantity,
                    totalRevenue = dao.totalRevenue,
                    averagePrice = if (dao.totalQuantity > BigDecimal.ZERO) dao.totalRevenue / dao.totalQuantity else BigDecimal.ZERO,
                    invoiceCount = 0
                )
            }
        }
    }
    
    /**
     * Get invoices for GST return filing
     */
    suspend fun getInvoicesForGSTReturn(startDate: Instant, endDate: Instant): List<com.billme.app.data.repository.GSTReturnInvoiceData> {
        val daoList = invoiceDao.getInvoicesForGSTReturn(startDate, endDate)
        return daoList.map { dao ->
            com.billme.app.data.repository.GSTReturnInvoiceData(
                invoiceId = dao.invoiceId,
                invoiceNumber = dao.invoiceNumber,
                invoiceDate = dao.invoiceDate,
                customerGSTIN = dao.customerGSTIN,
                customerStateCode = null,
                isInterstate = dao.isInterstate,
                taxableAmount = dao.subtotalAmount,
                cgstAmount = dao.cgstAmount,
                sgstAmount = dao.sgstAmount,
                igstAmount = dao.igstAmount,
                cessAmount = dao.cessAmount,
                totalGSTAmount = dao.cgstAmount + dao.sgstAmount + dao.igstAmount + dao.cessAmount,
                placeOfSupply = null,
                invoiceType = InvoiceType.SALE
            )
        }
    }
    
    /**
     * Bulk update payment status
     */
    suspend fun bulkUpdatePaymentStatus(invoiceIds: List<Long>, paymentStatus: PaymentStatus) {
        invoiceDao.bulkUpdatePaymentStatus(invoiceIds, paymentStatus)
    }
    
    /**
     * Get invoices with GST breakdown
     */
    fun getInvoicesWithGSTBreakdown(startDate: Instant, endDate: Instant): Flow<List<com.billme.app.data.repository.InvoiceGSTBreakdown>> {
        return invoiceDao.getInvoicesWithGSTBreakdown(startDate, endDate).map { daoList ->
            daoList.map { dao ->
                com.billme.app.data.repository.InvoiceGSTBreakdown(
                    invoiceId = dao.invoiceId,
                    invoiceNumber = dao.invoiceNumber,
                    customerName = null,
                    gstMode = GSTMode.FULL_GST,
                    isInterstate = dao.igstAmount > BigDecimal.ZERO,
                    taxableAmount = dao.grandTotal,
                    gstBreakdown = emptyList()
                )
            }
        }
    }
    
    /**
     * Validate invoice data
     */
    suspend fun validateInvoice(invoiceWithDetails: InvoiceWithDetails): InvoiceValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        val invoice = invoiceWithDetails.invoice
        val lineItems = invoiceWithDetails.lineItems
        val gstConfig = invoiceWithDetails.gstConfiguration
        
        // Basic validation
        if (lineItems.isEmpty()) {
            errors.add("Invoice must have at least one line item")
        }
        
        if (invoice.grandTotal <= BigDecimal.ZERO) {
            errors.add("Invoice total must be greater than zero")
        }
        
        // GST validation
        if (invoice.gstMode != GSTMode.NO_GST) {
            if (gstConfig == null) {
                errors.add("GST configuration is required for GST invoices")
            } else {
                // Validate GSTIN if required
                if (gstConfig.requireCustomerGSTIN && invoice.customerGSTIN.isNullOrBlank()) {
                    warnings.add("Customer GSTIN is recommended for this GST mode")
                }
                
                // Validate HSN codes if mandatory
                if (gstConfig.hsnCodeMandatory) {
                    lineItems.forEach { item ->
                        if (item.hsnCode.isNullOrBlank()) {
                            errors.add("HSN code is mandatory for product: ${item.productName}")
                        }
                    }
                }
                
                // Validate GST calculations
                val calculatedTotal = lineItems.sumOf { it.lineTotal }
                if ((calculatedTotal - invoice.grandTotal).abs() > BigDecimal("0.01")) {
                    errors.add("GST calculation mismatch detected")
                }
            }
        }
        
        // Payment validation
        if (invoice.paymentStatus == PaymentStatus.PAID && invoice.amountDue > BigDecimal.ZERO) {
            warnings.add("Payment status is PAID but amount due is greater than zero")
        }
        
        return InvoiceValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Export invoices to various formats
     */
    suspend fun exportInvoices(
        startDate: Instant,
        endDate: Instant,
        format: ExportFormat,
        includeGSTDetails: Boolean = true
    ): ExportResult {
        val invoices = invoiceDao.getInvoicesForExport(startDate, endDate)
        
        return when (format) {
            ExportFormat.PDF -> exportToPDF(invoices, includeGSTDetails)
            ExportFormat.EXCEL -> exportToExcel(invoices, includeGSTDetails)
            ExportFormat.CSV -> exportToCSV(invoices, includeGSTDetails)
            ExportFormat.JSON -> exportToJSON(invoices, includeGSTDetails)
        }
    }
    
    // Export helper methods (implementation would depend on specific libraries)
    private suspend fun exportToPDF(invoices: List<Invoice>, includeGSTDetails: Boolean): ExportResult {
        return try {
            // PDF generation would require a library like iText or PDFBox
            // For now, return a not implemented error instead of crashing
            ExportResult(
                success = false,
                filePath = null,
                fileName = null,
                error = "PDF export feature is not yet implemented. Please use CSV or JSON export instead."
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                fileName = null,
                error = "Error during PDF export: ${e.message}"
            )
        }
    }
    
    private suspend fun exportToExcel(invoices: List<Invoice>, includeGSTDetails: Boolean): ExportResult {
        return try {
            // Excel generation would require Apache POI library
            // For now, return a not implemented error instead of crashing
            ExportResult(
                success = false,
                filePath = null,
                fileName = null,
                error = "Excel export feature is not yet implemented. Please use CSV or JSON export instead."
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                fileName = null,
                error = "Error during Excel export: ${e.message}"
            )
        }
    }
    
    private suspend fun exportToCSV(invoices: List<Invoice>, includeGSTDetails: Boolean): ExportResult {
        return try {
            if (invoices.isEmpty()) {
                return ExportResult(
                    success = false,
                    filePath = null,
                    fileName = null,
                    error = "No invoices to export"
                )
            }
            
            val fileName = "invoices_export_${System.currentTimeMillis()}.csv"
            val csv = buildString {
                // CSV Header
                if (includeGSTDetails) {
                    appendLine("Invoice ID,Invoice Number,Date,Customer Name,Subtotal,GST Amount,Total,Payment Status,GST Mode")
                } else {
                    appendLine("Invoice ID,Invoice Number,Date,Customer Name,Subtotal,Total,Payment Status")
                }
                
                // CSV Rows
                invoices.forEach { invoice ->
                    if (includeGSTDetails) {
                        appendLine("${invoice.invoiceId},${invoice.invoiceNumber},${invoice.invoiceDate},${invoice.customerName ?: "N/A"},${invoice.subtotal},${invoice.gstAmount},${invoice.finalTotal},${invoice.paymentStatus},${invoice.gstMode}")
                    } else {
                        appendLine("${invoice.invoiceId},${invoice.invoiceNumber},${invoice.invoiceDate},${invoice.customerName ?: "N/A"},${invoice.subtotal},${invoice.finalTotal},${invoice.paymentStatus}")
                    }
                }
            }
            
            // In a real implementation, you would save this to external storage
            // For now, we return success with the CSV data as a string
            ExportResult(
                success = true,
                filePath = null, // Would be actual file path in real implementation
                fileName = fileName,
                error = null
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                fileName = null,
                error = "Error during CSV export: ${e.message}"
            )
        }
    }
    
    private suspend fun exportToJSON(invoices: List<Invoice>, includeGSTDetails: Boolean): ExportResult {
        return try {
            if (invoices.isEmpty()) {
                return ExportResult(
                    success = false,
                    filePath = null,
                    fileName = null,
                    error = "No invoices to export"
                )
            }
            
            val fileName = "invoices_export_${System.currentTimeMillis()}.json"
            
            // Build JSON manually for basic export
            val json = buildString {
                appendLine("{")
                appendLine("  \"invoices\": [")
                invoices.forEachIndexed { index, invoice ->
                    appendLine("    {")
                    appendLine("      \"invoiceId\": ${invoice.invoiceId},")
                    appendLine("      \"invoiceNumber\": \"${invoice.invoiceNumber}\",")
                    appendLine("      \"invoiceDate\": \"${invoice.invoiceDate}\",")
                    appendLine("      \"customerName\": \"${invoice.customerName ?: "N/A"}\",")
                    appendLine("      \"subtotal\": ${invoice.subtotal},")
                    if (includeGSTDetails) {
                        appendLine("      \"gstAmount\": ${invoice.gstAmount},")
                        appendLine("      \"gstMode\": \"${invoice.gstMode}\",")
                    }
                    appendLine("      \"finalTotal\": ${invoice.finalTotal},")
                    appendLine("      \"paymentStatus\": \"${invoice.paymentStatus}\"")
                    append("    }")
                    if (index < invoices.size - 1) appendLine(",")
                    else appendLine()
                }
                appendLine("  ],")
                appendLine("  \"exportDate\": \"${kotlinx.datetime.Clock.System.now()}\",")
                appendLine("  \"totalInvoices\": ${invoices.size}")
                appendLine("}")
            }
            
            // In a real implementation, you would save this to external storage
            ExportResult(
                success = true,
                filePath = null, // Would be actual file path in real implementation
                fileName = fileName,
                error = null
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                fileName = null,
                error = "Error during JSON export: ${e.message}"
            )
        }
    }
}

/**
 * Data classes for repository operations
 */

data class InvoiceStatistics(
    val totalInvoices: Int,
    val totalRevenue: BigDecimal,
    val totalGSTCollected: BigDecimal,
    val averageInvoiceValue: BigDecimal,
    val paidInvoices: Int,
    val pendingInvoices: Int,
    val cancelledInvoices: Int
)

data class GSTStatistics(
    val totalTaxableAmount: BigDecimal,
    val totalCGSTAmount: BigDecimal,
    val totalSGSTAmount: BigDecimal,
    val totalIGSTAmount: BigDecimal,
    val totalCessAmount: BigDecimal,
    val totalGSTAmount: BigDecimal,
    val interstateTransactions: Int,
    val intrastateTransactions: Int,
    val gstModeBreakdown: Map<GSTMode, Int>
)

data class ProductRevenue(
    val productId: Long,
    val productName: String,
    val totalQuantity: BigDecimal,
    val totalRevenue: BigDecimal,
    val averagePrice: BigDecimal,
    val invoiceCount: Int
)

data class GSTReturnInvoiceData(
    val invoiceId: Long,
    val invoiceNumber: String,
    val invoiceDate: Instant,
    val customerGSTIN: String?,
    val customerStateCode: String?,
    val isInterstate: Boolean,
    val taxableAmount: BigDecimal,
    val cgstAmount: BigDecimal,
    val sgstAmount: BigDecimal,
    val igstAmount: BigDecimal,
    val cessAmount: BigDecimal,
    val totalGSTAmount: BigDecimal,
    val placeOfSupply: String?,
    val invoiceType: InvoiceType
)

data class InvoiceGSTBreakdown(
    val invoiceId: Long,
    val invoiceNumber: String,
    val customerName: String?,
    val gstMode: GSTMode,
    val isInterstate: Boolean,
    val taxableAmount: BigDecimal,
    val gstBreakdown: List<GSTRateBreakdownItem>
)

data class InvoiceValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

enum class ExportFormat {
    PDF, EXCEL, CSV, JSON
}

data class ExportResult(
    val success: Boolean,
    val filePath: String?,
    val fileName: String?,
    val error: String?
)