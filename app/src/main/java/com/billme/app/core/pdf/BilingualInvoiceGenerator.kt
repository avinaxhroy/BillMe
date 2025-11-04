package com.billme.app.core.pdf

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced PDF Invoice Generator with Bilingual Support (English/Hindi)
 */
@Singleton
class BilingualInvoiceGenerator @Inject constructor() {
    
    companion object {
        private val BRAND_COLOR = DeviceRgb(33, 150, 243) // Material Blue #2196F3
        private val LIGHT_GRAY = DeviceRgb(245, 245, 245)
        private val DARK_GRAY = DeviceRgb(100, 100, 100)
    }
    
    data class InvoiceData(
        val invoiceNumber: String,
        val invoiceDate: String,
        val shopName: String,
        val shopAddress: String,
        val shopPhone: String,
        val shopGSTIN: String?,
        val customerName: String,
        val customerPhone: String,
        val customerAddress: String?,
        val items: List<InvoiceItem>,
        val subtotal: BigDecimal,
        val discount: BigDecimal,
        val gstAmount: BigDecimal,
        val total: BigDecimal,
        val paymentMethod: String,
        val applyGST: Boolean
    )
    
    data class InvoiceItem(
        val name: String,
        val hsnCode: String?,
        val quantity: Int,
        val rate: BigDecimal,
        val amount: BigDecimal,
        val gstRate: Double = 18.0
    )
    
    fun generateInvoice(data: InvoiceData, outputFile: File): Boolean {
        return try {
            val writer = PdfWriter(outputFile)
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4)
            document.setMargins(30f, 30f, 30f, 30f)
            
            // Create fonts
            val boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)
            val regularFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)
            
            // Add Header
            addBilingualHeader(document, data, boldFont, regularFont)
            
            // Add Customer Details
            addCustomerSection(document, data, boldFont, regularFont)
            
            // Add Items Table
            addItemsTable(document, data, boldFont, regularFont)
            
            // Add Totals
            addTotalsSection(document, data, boldFont, regularFont)
            
            // Add Footer
            addBilingualFooter(document, regularFont)
            
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun addBilingualHeader(doc: Document, data: InvoiceData, boldFont: PdfFont, regularFont: PdfFont) {
        // Shop Name - Large
        val shopNamePara = Paragraph(data.shopName)
            .setFont(boldFont)
            .setFontSize(20f)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(BRAND_COLOR)
        doc.add(shopNamePara)
        
        // Address and Contact
        val contactPara = Paragraph()
            .setFont(regularFont)
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
            .add("${data.shopAddress}\n")
            .add("Phone: ${data.shopPhone}")
        
        if (!data.shopGSTIN.isNullOrBlank()) {
            contactPara.add(" | GSTIN: ${data.shopGSTIN}")
        }
        doc.add(contactPara)
        
        // Invoice Title - Bilingual
        doc.add(Paragraph()
            .setFont(boldFont)
            .setFontSize(16f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(15f)
            .add("TAX INVOICE / कर चालान"))
        
        // Invoice Details Table
        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(10f)
        
        // Left side
        detailsTable.addCell(createCell("Invoice No. / चालान नं: ${data.invoiceNumber}", regularFont, false))
        detailsTable.addCell(createCell("Date / तारीख: ${data.invoiceDate}", regularFont, false))
        
        doc.add(detailsTable)
        doc.add(Paragraph().setMarginBottom(10f))
    }
    
    private fun addCustomerSection(doc: Document, data: InvoiceData, boldFont: PdfFont, regularFont: PdfFont) {
        val customerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(SolidBorder(ColorConstants.GRAY, 1f))
        
        val header = Cell()
            .add(Paragraph("Customer Details / ग्राहक विवरण")
                .setFont(boldFont)
                .setFontSize(12f))
            .setBackgroundColor(LIGHT_GRAY)
            .setBorder(Border.NO_BORDER)
            .setPadding(8f)
        customerTable.addCell(header)
        
        val details = Cell()
            .add(Paragraph()
                .setFont(regularFont)
                .setFontSize(10f)
                .add("Name / नाम: ${data.customerName}\n")
                .add("Phone / फोन: ${data.customerPhone}")
                .apply {
                    if (!data.customerAddress.isNullOrBlank()) {
                        add("\nAddress / पता: ${data.customerAddress}")
                    }
                })
            .setBorder(Border.NO_BORDER)
            .setPadding(8f)
        customerTable.addCell(details)
        
        doc.add(customerTable)
        doc.add(Paragraph().setMarginBottom(10f))
    }
    
    private fun addItemsTable(doc: Document, data: InvoiceData, boldFont: PdfFont, regularFont: PdfFont) {
        val columnWidths = if (data.applyGST) {
            floatArrayOf(8f, 35f, 12f, 10f, 15f, 10f, 10f)
        } else {
            floatArrayOf(10f, 45f, 15f, 15f, 15f)
        }
        
        val table = Table(UnitValue.createPercentArray(columnWidths))
            .setWidth(UnitValue.createPercentValue(100f))
        
        // Headers - Bilingual
        val headers = if (data.applyGST) {
            listOf(
                "S.No\nक्र.सं.",
                "Item / Description\nवस्तु / विवरण",
                "HSN Code\nएचएसएन कोड",
                "Qty\nमात्रा",
                "Rate / दर\n(₹)",
                "GST %\nजीएसटी %",
                "Amount\nराशि (₹)"
            )
        } else {
            listOf(
                "S.No\nक्र.सं.",
                "Item / Description\nवस्तु / विवरण",
                "Qty\nमात्रा",
                "Rate / दर (₹)",
                "Amount\nराशि (₹)"
            )
        }
        
        headers.forEach { header ->
            table.addCell(createHeaderCell(header, boldFont))
        }
        
        // Items
        data.items.forEachIndexed { index, item ->
            table.addCell(createCell("${index + 1}", regularFont, true))
            table.addCell(createCell(item.name, regularFont, false))
            
            if (data.applyGST) {
                table.addCell(createCell(item.hsnCode ?: "-", regularFont, true))
            }
            
            table.addCell(createCell("${item.quantity}", regularFont, true))
            table.addCell(createCell(formatAmount(item.rate), regularFont, true))
            
            if (data.applyGST) {
                table.addCell(createCell("${item.gstRate}%", regularFont, true))
            }
            
            table.addCell(createCell(formatAmount(item.amount), regularFont, true))
        }
        
        doc.add(table)
    }
    
    private fun addTotalsSection(doc: Document, data: InvoiceData, boldFont: PdfFont, regularFont: PdfFont) {
        val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginTop(10f)
        
        // Subtotal
        totalsTable.addCell(createTotalRowCell("Subtotal / उप योग:", regularFont, false))
        totalsTable.addCell(createTotalRowCell(formatAmount(data.subtotal), regularFont, true))
        
        // Discount
        if (data.discount > BigDecimal.ZERO) {
            totalsTable.addCell(createTotalRowCell("Discount / छूट:", regularFont, false))
            totalsTable.addCell(createTotalRowCell("- ${formatAmount(data.discount)}", regularFont, true))
        }
        
        // GST
        if (data.applyGST && data.gstAmount > BigDecimal.ZERO) {
            totalsTable.addCell(createTotalRowCell("GST / जीएसटी (18%):", regularFont, false))
            totalsTable.addCell(createTotalRowCell(formatAmount(data.gstAmount), regularFont, true))
        }
        
        // Total
        val totalCell1 = createTotalRowCell("Total Amount / कुल राशि:", boldFont, false)
        totalCell1.setBackgroundColor(LIGHT_GRAY)
        totalsTable.addCell(totalCell1)
        
        val totalCell2 = createTotalRowCell(formatAmount(data.total), boldFont, true)
        totalCell2.setBackgroundColor(LIGHT_GRAY)
        totalsTable.addCell(totalCell2)
        
        doc.add(totalsTable)
        
        // Amount in words
        val amountInWords = numberToWords(data.total.toDouble())
        doc.add(Paragraph()
            .setFont(regularFont)
            .setFontSize(10f)
            .setMarginTop(10f)
            .add("Amount in words / राशि शब्दों में: ")
            .add(Text(amountInWords).setFont(boldFont)))
        
        // Payment Method
        doc.add(Paragraph()
            .setFont(regularFont)
            .setFontSize(10f)
            .add("Payment Method / भुगतान विधि: ${data.paymentMethod}"))
    }
    
    private fun addBilingualFooter(doc: Document, regularFont: PdfFont) {
        doc.add(Paragraph().setMarginTop(20f))
        
        // Terms and Conditions
        val termsBox = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(SolidBorder(DARK_GRAY, 0.5f))
        
        val termsCell = Cell()
            .add(Paragraph("Terms & Conditions / नियम और शर्तें:")
                .setFont(regularFont)
                .setFontSize(9f)
                .setBold())
            .add(Paragraph()
                .setFont(regularFont)
                .setFontSize(8f)
                .add("1. Goods once sold will not be taken back or exchanged.\n")
                .add("   एक बार बेचा गया माल वापस नहीं लिया जाएगा या बदला नहीं जाएगा।\n")
                .add("2. For product warranty and service, please visit respective service center.\n")
                .add("   उत्पाद वारंटी और सेवा के लिए, कृपया संबंधित सेवा केंद्र पर जाएं।\n")
                .add("3. Subject to local jurisdiction only.\n")
                .add("   केवल स्थानीय अधिकार क्षेत्र के अधीन।"))
            .setBorder(Border.NO_BORDER)
            .setPadding(10f)
        
        termsBox.addCell(termsCell)
        doc.add(termsBox)
        
        // Thank you note
        doc.add(Paragraph()
            .setFont(regularFont)
            .setFontSize(11f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(15f)
            .setBold()
            .add("Thank You! / धन्यवाद!"))
        
        doc.add(Paragraph()
            .setFont(regularFont)
            .setFontSize(9f)
            .setTextAlignment(TextAlignment.CENTER)
            .add("This is a computer-generated invoice / यह कंप्यूटर जनित चालान है"))
    }
    
    private fun createHeaderCell(text: String, font: PdfFont): Cell {
        return Cell()
            .add(Paragraph(text).setFont(font).setFontSize(9f).setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(LIGHT_GRAY)
            .setBorder(SolidBorder(ColorConstants.GRAY, 1f))
            .setPadding(6f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }
    
    private fun createCell(text: String, font: PdfFont, center: Boolean): Cell {
        val para = Paragraph(text).setFont(font).setFontSize(9f)
        if (center) para.setTextAlignment(TextAlignment.CENTER)
        
        return Cell()
            .add(para)
            .setBorder(SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
            .setPadding(5f)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
    }
    
    private fun createTotalRowCell(text: String, font: PdfFont, rightAlign: Boolean): Cell {
        val para = Paragraph(text).setFont(font).setFontSize(10f)
        if (rightAlign) para.setTextAlignment(TextAlignment.RIGHT)
        
        return Cell()
            .add(para)
            .setBorder(Border.NO_BORDER)
            .setPadding(5f)
    }
    
    private fun formatAmount(amount: BigDecimal): String {
        return "₹" + amount.setScale(2, RoundingMode.HALF_UP).toString()
    }
    
    private fun numberToWords(number: Double): String {
        val rupees = number.toLong()
        val paise = ((number - rupees) * 100).toInt()
        
        val rupeesWords = convertToWords(rupees)
        val result = if (paise > 0) {
            "$rupeesWords Rupees and ${convertToWords(paise.toLong())} Paise Only"
        } else {
            "$rupeesWords Rupees Only"
        }
        
        return result
    }
    
    private fun convertToWords(number: Long): String {
        if (number == 0L) return "Zero"
        
        val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        val teens = arrayOf("Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
        
        fun helper(n: Long): String {
            return when {
                n < 10 -> ones[n.toInt()]
                n < 20 -> teens[(n - 10).toInt()]
                n < 100 -> tens[(n / 10).toInt()] + (if (n % 10 != 0L) " " + ones[(n % 10).toInt()] else "")
                n < 1000 -> ones[(n / 100).toInt()] + " Hundred" + (if (n % 100 != 0L) " " + helper(n % 100) else "")
                n < 100000 -> helper(n / 1000) + " Thousand" + (if (n % 1000 != 0L) " " + helper(n % 1000) else "")
                n < 10000000 -> helper(n / 100000) + " Lakh" + (if (n % 100000 != 0L) " " + helper(n % 100000) else "")
                else -> helper(n / 10000000) + " Crore" + (if (n % 10000000 != 0L) " " + helper(n % 10000000) else "")
            }
        }
        
        return helper(number).trim()
    }
}
