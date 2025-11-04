package com.billme.app.core.ocr.impl

import com.billme.app.core.ocr.TemplateMatchingService
import com.billme.app.data.model.DocumentType
import com.billme.app.data.model.ExtractedField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Template Matching Service for document recognition
 */
@Singleton
class TemplateMatchingServiceImpl @Inject constructor() : TemplateMatchingService {
    
    override suspend fun detectTemplate(
        documentType: DocumentType,
        fields: Map<String, ExtractedField>
    ): String? = withContext(Dispatchers.Default) {
        // Simple template detection based on extracted fields
        // Can be enhanced with machine learning in future
        
        when (documentType) {
            DocumentType.INVOICE -> detectInvoiceTemplate(fields)
            DocumentType.RECEIPT -> detectReceiptTemplate(fields)
            else -> null
        }
    }
    
    private fun detectInvoiceTemplate(fields: Map<String, ExtractedField>): String? {
        val hasGST = fields.containsKey("gst_number")
        val hasVendor = fields.containsKey("vendor_name")
        val hasInvoiceNumber = fields.containsKey("invoice_number")
        
        return when {
            hasGST && hasVendor && hasInvoiceNumber -> "gst_invoice_standard"
            hasInvoiceNumber && hasVendor -> "simple_invoice"
            else -> "generic_invoice"
        }
    }
    
    private fun detectReceiptTemplate(fields: Map<String, ExtractedField>): String? {
        val hasReceiptNumber = fields.containsKey("receipt_number")
        val hasMerchant = fields.containsKey("merchant_name")
        
        return when {
            hasReceiptNumber && hasMerchant -> "standard_receipt"
            else -> "generic_receipt"
        }
    }
}
