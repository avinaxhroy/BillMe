package com.billme.app.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.billme.app.data.local.entity.*
import com.billme.app.core.util.formatCurrency
import com.billme.app.core.util.formatPercentage
import java.math.BigDecimal

/**
 * Complete invoice display component with GST visibility controls
 */
@Composable
fun InvoiceDisplayComponent(
    invoiceWithDetails: InvoiceWithDetails,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    onPrint: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    val invoice = invoiceWithDetails.invoice
    val scrollState = rememberScrollState()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Invoice Header
            InvoiceHeader(
                invoice = invoice,
                gstConfig = invoiceWithDetails.gstConfiguration
            )
            
            HorizontalDivider()
            
            // Customer Information
            if (invoice.customerName != null || invoice.customerPhone != null) {
                CustomerSection(invoice = invoice)
                HorizontalDivider()
            }
            
            // Line Items
            LineItemsSection(
                lineItems = invoiceWithDetails.lineItems,
                gstMode = invoice.gstMode
            )
            
            HorizontalDivider()
            
            // GST Summary (if visible)
            if (shouldShowGSTSummary(invoice)) {
                GSTSummarySection(invoiceWithDetails = invoiceWithDetails)
                HorizontalDivider()
            }
            
            // Invoice Totals
            InvoiceTotalsSection(
                invoice = invoice,
                showGSTBreakdown = shouldShowGSTBreakdown(invoice)
            )
            
            // Amount in Words
            if (!invoice.amountInWords.isNullOrBlank()) {
                AmountInWordsSection(invoice.amountInWords!!)
            }
            
            // Payment Information
            if (invoice.paymentStatus != PaymentStatus.PAID || invoice.amountDue > BigDecimal.ZERO) {
                PaymentSection(invoice = invoice)
                HorizontalDivider()
            }
            
            // Terms and Conditions
            if (!invoice.termsAndConditions.isNullOrBlank()) {
                TermsSection(invoice.termsAndConditions!!)
            }
            
            // Notes
            if (!invoice.notes.isNullOrBlank()) {
                NotesSection(invoice.notes!!)
            }
            
            // Action Buttons
            if (showActions) {
                ActionButtonsSection(
                    onPrint = onPrint,
                    onShare = onShare,
                    onEdit = onEdit,
                    onCancel = onCancel,
                    isCancelled = invoice.isCancelled
                )
            }
        }
    }
}

@Composable
private fun InvoiceHeader(
    invoice: Invoice,
    gstConfig: GSTConfiguration?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Invoice Title and Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (invoice.invoiceType) {
                    InvoiceType.SALE -> "INVOICE"
                    InvoiceType.RETURN -> "RETURN INVOICE"
                    InvoiceType.CREDIT_NOTE -> "CREDIT NOTE"
                    InvoiceType.DEBIT_NOTE -> "DEBIT NOTE"
                    InvoiceType.PROFORMA -> "PROFORMA INVOICE"
                    InvoiceType.QUOTATION -> "QUOTATION"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "# ${invoice.invoiceNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Date: ${invoice.invoiceDate.toString().split("T")[0]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (invoice.dueDate != null) {
                    Text(
                        text = "Due: ${invoice.dueDate.toString().split("T")[0]}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Shop GSTIN (if configured to show)
        if (invoice.showGSTIN && !invoice.shopGSTIN.isNullOrBlank()) {
            ShopGSTINDisplay(
                gstin = invoice.shopGSTIN!!,
                stateName = gstConfig?.shopStateName
            )
        }
        
        // GST Mode Badge
        GSTModeBadge(gstMode = invoice.gstMode)
        
        // Cancelled Badge
        if (invoice.isCancelled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "CANCELLED",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun CustomerSection(invoice: Invoice) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Bill To:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            invoice.customerName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            invoice.customerPhone?.let { phone ->
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            invoice.customerAddress?.let { address ->
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Customer GSTIN (if available and configured to show)
            if (invoice.showGSTIN && !invoice.customerGSTIN.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "GSTIN:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = invoice.customerGSTIN!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LineItemsSection(
    lineItems: List<InvoiceLineItem>,
    gstMode: GSTMode
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Items",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(lineItems, key = { it.lineItemId }) { lineItem ->
                LineItemCard(
                    lineItem = lineItem,
                    showGSTDetails = shouldShowGSTInLineItems(gstMode)
                )
            }
        }
    }
}

@Composable
private fun LineItemCard(
    lineItem: InvoiceLineItem,
    showGSTDetails: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Product Name and HSN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lineItem.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (!lineItem.productDescription.isNullOrBlank()) {
                        Text(
                            text = lineItem.productDescription!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // HSN Code
                    if (!lineItem.hsnCode.isNullOrBlank()) {
                        Text(
                            text = "HSN: ${lineItem.hsnCode}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Line Total
                Text(
                    text = lineItem.lineTotal.formatCurrency(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Quantity and Unit Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${lineItem.quantity} ${lineItem.unitOfMeasure} × ${lineItem.unitPrice.formatCurrency()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (lineItem.discountAmount > BigDecimal.ZERO) {
                    Text(
                        text = "Discount: ${lineItem.discountAmount.formatCurrency()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // GST Details (if should be shown)
            if (showGSTDetails && lineItem.totalGSTAmount > BigDecimal.ZERO) {
                HorizontalDivider()
                LineItemGSTDetails(lineItem = lineItem)
            }
        }
    }
}

@Composable
private fun LineItemGSTDetails(lineItem: InvoiceLineItem) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Tax Details",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Taxable Amount:",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = lineItem.taxableAmount.formatCurrency(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
        
        if (lineItem.cgstAmount > BigDecimal.ZERO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CGST (${lineItem.cgstRate.formatPercentage()}):",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = lineItem.cgstAmount.formatCurrency(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        if (lineItem.sgstAmount > BigDecimal.ZERO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SGST (${lineItem.sgstRate.formatPercentage()}):",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = lineItem.sgstAmount.formatCurrency(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        if (lineItem.igstAmount > BigDecimal.ZERO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "IGST (${lineItem.igstRate.formatPercentage()}):",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = lineItem.igstAmount.formatCurrency(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        if (lineItem.cessAmount > BigDecimal.ZERO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cess (${lineItem.cessRate.formatPercentage()}):",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = lineItem.cessAmount.formatCurrency(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun GSTSummarySection(invoiceWithDetails: InvoiceWithDetails) {
    val invoice = invoiceWithDetails.invoice
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GST Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Tax type indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (invoice.isInterstate) "Interstate Transaction (IGST)" else "Intrastate Transaction (CGST + SGST)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            HorizontalDivider()
            
            // GST Breakdown
            if (invoice.cgstAmount > BigDecimal.ZERO) {
                GSTAmountRow(
                    label = "CGST",
                    amount = invoice.cgstAmount,
                    rate = null // We could calculate average rate if needed
                )
            }
            
            if (invoice.sgstAmount > BigDecimal.ZERO) {
                GSTAmountRow(
                    label = "SGST",
                    amount = invoice.sgstAmount,
                    rate = null
                )
            }
            
            if (invoice.igstAmount > BigDecimal.ZERO) {
                GSTAmountRow(
                    label = "IGST",
                    amount = invoice.igstAmount,
                    rate = null
                )
            }
            
            if (invoice.cessAmount > BigDecimal.ZERO) {
                GSTAmountRow(
                    label = "Cess",
                    amount = invoice.cessAmount,
                    rate = null
                )
            }
            
            HorizontalDivider()
            
            // Total GST
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total GST",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = invoice.totalGSTAmount.formatCurrency(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun GSTAmountRow(
    label: String,
    amount: BigDecimal,
    rate: Double?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (rate != null) "$label (${rate.formatPercentage()})" else label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = amount.formatCurrency(),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun InvoiceTotalsSection(
    invoice: Invoice,
    showGSTBreakdown: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Subtotal
            InvoiceAmountRow(
                label = "Subtotal",
                amount = invoice.subtotalAmount
            )
            
            // Discount
            if (invoice.discountAmount > BigDecimal.ZERO) {
                InvoiceAmountRow(
                    label = "Discount",
                    amount = -invoice.discountAmount,
                    isDiscount = true
                )
            }
            
            // Taxable Amount
            InvoiceAmountRow(
                label = "Taxable Amount",
                amount = invoice.taxableAmount
            )
            
            // GST Amount (only if should be shown)
            if (showGSTBreakdown && invoice.totalGSTAmount > BigDecimal.ZERO) {
                InvoiceAmountRow(
                    label = "Total GST",
                    amount = invoice.totalGSTAmount,
                    isHighlight = true
                )
            }
            
            // Round off
            if (invoice.roundOffAmount != BigDecimal.ZERO) {
                InvoiceAmountRow(
                    label = "Round Off",
                    amount = invoice.roundOffAmount,
                    isRoundOff = true
                )
            }
            
            HorizontalDivider(thickness = 2.dp)
            
            // Grand Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GRAND TOTAL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = invoice.grandTotal.formatCurrency(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun InvoiceAmountRow(
    label: String,
    amount: BigDecimal,
    isDiscount: Boolean = false,
    isHighlight: Boolean = false,
    isRoundOff: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Medium else FontWeight.Normal
        )
        Text(
            text = amount.formatCurrency(),
            style = if (isHighlight) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Medium else FontWeight.Normal,
            color = when {
                isDiscount -> MaterialTheme.colorScheme.error
                isRoundOff -> if (amount >= BigDecimal.ZERO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                isHighlight -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun AmountInWordsSection(amountInWords: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Amount in Words:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = amountInWords,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun PaymentSection(invoice: Invoice) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (invoice.paymentStatus) {
                PaymentStatus.PAID -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                PaymentStatus.PENDING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                PaymentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Payment Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Payment Method:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = invoice.paymentMethod.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Payment Status:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = invoice.paymentStatus.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = when (invoice.paymentStatus) {
                        PaymentStatus.PAID -> MaterialTheme.colorScheme.primary
                        PaymentStatus.PENDING -> MaterialTheme.colorScheme.error
                        PaymentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.secondary
                    }
                )
            }
            
            if (invoice.amountPaid > BigDecimal.ZERO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Amount Paid:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = invoice.amountPaid.formatCurrency(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            if (invoice.amountDue > BigDecimal.ZERO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Amount Due:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = invoice.amountDue.formatCurrency(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun TermsSection(terms: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Terms & Conditions:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = terms,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotesSection(notes: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Notes:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionButtonsSection(
    onPrint: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    isCancelled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
    ) {
        onPrint?.let {
            FilledTonalButton(onClick = it) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print")
            }
        }
        
        onShare?.let {
            FilledTonalButton(onClick = it) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
        }
        
        if (!isCancelled) {
            onEdit?.let {
                OutlinedButton(onClick = it) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
            }
            
            onCancel?.let {
                OutlinedButton(
                    onClick = it,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
            }
        }
    }
}

// Helper Composables

@Composable
private fun ShopGSTINDisplay(gstin: String, stateName: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.VerifiedUser,
            contentDescription = "GSTIN",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "GSTIN: $gstin",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
        stateName?.let {
            Text(
                text = "($it)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GSTModeBadge(gstMode: GSTMode) {
    val (text, color) = when (gstMode) {
        GSTMode.FULL_GST -> "Full GST" to MaterialTheme.colorScheme.primaryContainer
        GSTMode.PARTIAL_GST -> "Partial GST" to MaterialTheme.colorScheme.secondaryContainer
        GSTMode.GST_REFERENCE -> "GST Reference" to MaterialTheme.colorScheme.tertiaryContainer
        GSTMode.NO_GST -> "No GST" to MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(colors = CardDefaults.cardColors(containerColor = color)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper Functions

private fun shouldShowGSTSummary(invoice: Invoice): Boolean {
    return invoice.showGSTSummary && 
           invoice.gstMode == GSTMode.FULL_GST && 
           invoice.totalGSTAmount > BigDecimal.ZERO
}

private fun shouldShowGSTBreakdown(invoice: Invoice): Boolean {
    return invoice.gstMode == GSTMode.FULL_GST || 
           invoice.gstMode == GSTMode.PARTIAL_GST
}

private fun shouldShowGSTInLineItems(gstMode: GSTMode): Boolean {
    return gstMode == GSTMode.FULL_GST
}

// Extension functions for formatting (these should be in your util package)
private fun BigDecimal.formatCurrency(): String {
    return "₹${this.setScale(2, java.math.RoundingMode.HALF_UP)}"
}

private fun Double.formatPercentage(): String {
    return "${this}%"
}