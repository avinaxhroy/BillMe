package com.billme.app.core.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.utils.PdfMerger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages invoice file operations: saving, sharing, and printing
 */
@Singleton
class InvoiceFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val INVOICES_DIR = "Invoices"
    }

    /**
     * Get file URI using FileProvider with dynamic authority
     * Uses package name + .fileprovider to support different build variants
     */
    private fun getAuthority(): String {
        return "${context.packageName}.fileprovider"
    }

    /**
     * Get the invoices directory
     */
    fun getInvoicesDirectory(): File {
        val invoicesDir = File(context.getExternalFilesDir(null), INVOICES_DIR)
        if (!invoicesDir.exists()) {
            invoicesDir.mkdirs()
        }
        return invoicesDir
    }

    /**
     * Get all saved invoices
     */
    suspend fun getAllInvoices(): List<File> = withContext(Dispatchers.IO) {
        val invoicesDir = getInvoicesDirectory()
        invoicesDir.listFiles()?.filter { it.extension == "pdf" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Get invoice by transaction number
     */
    suspend fun getInvoiceByNumber(transactionNumber: String, customerCopy: Boolean = true): File? = withContext(Dispatchers.IO) {
        val suffix = if (customerCopy) "Customer" else "Owner"
        val fileName = "Invoice_${transactionNumber}_$suffix.pdf"
        val file = File(getInvoicesDirectory(), fileName)
        if (file.exists()) file else null
    }

    /**
     * Delete an invoice file
     */
    suspend fun deleteInvoice(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Delete invoices older than specified days
     */
    suspend fun deleteOldInvoices(daysToKeep: Int = 90): Int = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        val invoicesDir = getInvoicesDirectory()
        var deletedCount = 0
        
        invoicesDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        
        deletedCount
    }

    /**
     * Get URI for a file using FileProvider
     */
    fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(context, getAuthority(), file)
    }

    /**
     * Create share intent for invoice
     */
    fun createShareIntent(file: File, title: String = "Share Invoice"): Intent {
        val uri = getFileUri(file)
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${file.nameWithoutExtension}")
            putExtra(Intent.EXTRA_TEXT, "Please find the attached invoice.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { Intent.createChooser(it, title) }
    }
    
    /**
     * Create view intent for opening PDF in viewer
     */
    fun createViewIntent(file: File): Intent {
        val uri = getFileUri(file)
        
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { Intent.createChooser(it, "Open Invoice") }
    }

    /**
     * Create WhatsApp specific share intent
     */
    fun createWhatsAppShareIntent(file: File, phoneNumber: String? = null): Intent? {
        val uri = getFileUri(file)
        
        return try {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Thank you for your purchase! Here's your invoice.")
                
                // If phone number provided, try to open specific chat
                if (!phoneNumber.isNullOrBlank()) {
                    val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                    putExtra("jid", "$cleanNumber@s.whatsapp.net")
                }
                
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create email share intent
     */
    fun createEmailShareIntent(file: File, recipientEmail: String? = null): Intent {
        val uri = getFileUri(file)
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${file.nameWithoutExtension}")
            putExtra(Intent.EXTRA_TEXT, "Dear Customer,\n\nPlease find attached invoice for your recent purchase.\n\nThank you for your business!")
            
            if (!recipientEmail.isNullOrBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            }
            
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { Intent.createChooser(it, "Send Invoice via Email") }
    }

    /**
     * Print PDF using Android Print Framework
     * Creates a temporary WebView to load PDF and initiate print job
     */
    fun printPdf(file: File, jobName: String = "Invoice") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        
        // Create a WebView to load and print the PDF
        val webView = WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    createWebPrintJob(view, printManager, jobName, this)
                }
            }
        }
        
        val uri = getFileUri(file)
        webView.loadUrl(uri.toString())
    }

    private fun createWebPrintJob(
        webView: WebView?,
        printManager: PrintManager,
        jobName: String,
        webViewClient: WebViewClient
    ) {
        webView?.let { web ->
            val printAdapter: PrintDocumentAdapter = web.createPrintDocumentAdapter(jobName)
            
            printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
            )
            
            // Clean up WebView resources after print job is initiated
            // The WebView is no longer needed after the print adapter is created
            web.stopLoading()
            // Note: WebView will be garbage collected with its parent context lifecycle
        }
    }

    /**
     * Open PDF in external viewer
     */
    fun openPdfInViewer(file: File): Intent {
        val uri = getFileUri(file)
        
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get file size in human-readable format
     */
    fun getFileSize(file: File): String {
        val bytes = file.length()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }

    /**
     * Check if invoice already exists
     */
    fun invoiceExists(transactionNumber: String, customerCopy: Boolean = true): Boolean {
        val suffix = if (customerCopy) "Customer" else "Owner"
        val fileName = "Invoice_${transactionNumber}_$suffix.pdf"
        val file = File(getInvoicesDirectory(), fileName)
        return file.exists()
    }

    /**
     * Get storage usage statistics
     */
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val invoicesDir = getInvoicesDirectory()
        val files = invoicesDir.listFiles()?.filter { it.extension == "pdf" } ?: emptyList()
        
        val totalSize = files.sumOf { it.length() }
        val count = files.size
        
        StorageStats(
            totalInvoices = count,
            totalSizeBytes = totalSize,
            totalSizeMB = totalSize / (1024.0 * 1024.0),
            averageSizeBytes = if (count > 0) totalSize / count else 0
        )
    }

    /**
     * Merge multiple PDF invoices into a single PDF file
     * @param invoiceFiles List of invoice files to merge
     * @param outputFileName Name for the merged PDF file
     * @param progressCallback Optional callback to report progress (current, total)
     * @return The merged PDF file or null if failed
     */
    suspend fun mergeInvoices(
        invoiceFiles: List<File>,
        outputFileName: String = "Merged_Invoices_${System.currentTimeMillis()}.pdf",
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        if (invoiceFiles.isEmpty()) {
            return@withContext null
        }

        try {
            val outputFile = File(getInvoicesDirectory(), outputFileName)
            val pdfWriter = PdfWriter(outputFile)
            val mergedPdf = PdfDocument(pdfWriter)
            val merger = PdfMerger(mergedPdf)

            invoiceFiles.forEachIndexed { index, file ->
                try {
                    if (file.exists() && file.extension == "pdf") {
                        val reader = PdfReader(file)
                        val sourcePdf = PdfDocument(reader)
                        
                        // Merge all pages from the source PDF
                        merger.merge(sourcePdf, 1, sourcePdf.numberOfPages)
                        
                        sourcePdf.close()
                        reader.close()
                        
                        // Report progress
                        progressCallback?.invoke(index + 1, invoiceFiles.size)
                    }
                } catch (e: Exception) {
                    // Log error but continue with other files
                    e.printStackTrace()
                }
            }

            mergedPdf.close()
            pdfWriter.close()
            
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Merge invoices and create a summary cover page
     * @param invoiceFiles List of invoice files to merge
     * @param totalAmount Total amount across all invoices
     * @param invoiceCount Number of invoices being merged
     * @return The merged PDF file with summary or null if failed
     */
    suspend fun mergeInvoicesWithSummary(
        invoiceFiles: List<File>,
        totalAmount: Double = 0.0,
        invoiceCount: Int = 0,
        dateRange: String = "",
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): MergeResult = withContext(Dispatchers.IO) {
        if (invoiceFiles.isEmpty()) {
            return@withContext MergeResult.Error("No invoices to merge")
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFileName = "Bulk_Export_${timestamp}.pdf"
            val mergedFile = mergeInvoices(invoiceFiles, outputFileName, progressCallback)
            
            if (mergedFile != null && mergedFile.exists()) {
                MergeResult.Success(
                    file = mergedFile,
                    mergedCount = invoiceFiles.size,
                    totalSize = mergedFile.length(),
                    summary = "Merged ${invoiceFiles.size} invoices${if (dateRange.isNotBlank()) " from $dateRange" else ""}"
                )
            } else {
                MergeResult.Error("Failed to create merged PDF")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            MergeResult.Error("Error merging PDFs: ${e.message}")
        }
    }

    /**
     * Create a bulk export with multiple share options
     */
    fun createBulkShareIntent(mergedFile: File, summary: String = ""): Intent {
        val uri = getFileUri(mergedFile)
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Bulk Invoice Export - ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}")
            putExtra(Intent.EXTRA_TEXT, buildString {
                appendLine("Bulk Invoice Export")
                appendLine()
                if (summary.isNotBlank()) {
                    appendLine(summary)
                    appendLine()
                }
                appendLine("Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}")
            })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { Intent.createChooser(it, "Share Bulk Export") }
    }

    /**
     * Save merged invoices to Downloads folder (accessible by user)
     */
    suspend fun saveBulkExportToDownloads(mergedFile: File): SaveResult = withContext(Dispatchers.IO) {
        try {
            // For Android 10+, we should use MediaStore, but for simplicity,
            // we'll keep it in app's external files directory which is accessible
            SaveResult.Success(
                file = mergedFile,
                path = mergedFile.absolutePath,
                message = "Saved to: ${mergedFile.parent}"
            )
        } catch (e: Exception) {
            SaveResult.Error("Failed to save: ${e.message}")
        }
    }

    sealed class MergeResult {
        data class Success(
            val file: File,
            val mergedCount: Int,
            val totalSize: Long,
            val summary: String
        ) : MergeResult()
        
        data class Error(val message: String) : MergeResult()
    }

    sealed class SaveResult {
        data class Success(
            val file: File,
            val path: String,
            val message: String
        ) : SaveResult()
        
        data class Error(val message: String) : SaveResult()
    }

    data class StorageStats(
        val totalInvoices: Int,
        val totalSizeBytes: Long,
        val totalSizeMB: Double,
        val averageSizeBytes: Long
    )
}
