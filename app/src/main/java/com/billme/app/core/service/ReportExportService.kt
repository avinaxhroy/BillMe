package com.billme.app.core.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.billme.app.core.util.formatCurrency
import com.billme.app.core.util.formatPercentage
import com.billme.app.core.util.formatLocale
import com.billme.app.data.model.*
// Note: html2pdf imports removed - using placeholder implementation
// import com.itextpdf.html2pdf.ConverterProperties
// import com.itextpdf.html2pdf.HtmlConverter
// import com.itextpdf.html2pdf.css.CssConstants
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for exporting reports to PDF and CSV formats with professional formatting
 */
@Singleton
class ReportExportService @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val COMPANY_NAME = "Mobile Shop Pro"
        private const val PDF_MARGIN = 36f
        private const val TABLE_CELL_PADDING = 8f
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        private val CSV_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    /**
     * Export report data to specified format
     */
    suspend fun exportReport(
        reportData: ReportData,
        format: ExportFormat,
        includeLogo: Boolean = true,
        includeCharts: Boolean = true
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            return@withContext when (format) {
                ExportFormat.PDF -> generatePdfReport(reportData, includeLogo, includeCharts)
                ExportFormat.CSV -> generateCsvReport(reportData)
                ExportFormat.EXCEL -> generateExcelReport(reportData)
                ExportFormat.JSON -> generateJsonReport(reportData)
            }
        } catch (e: Exception) {
            return@withContext ExportResult.Error("Failed to export report: ${e.message}")
        }
    }

    /**
     * Generate professional PDF report
     */
    private suspend fun generatePdfReport(
        reportData: ReportData,
        includeLogo: Boolean,
        includeCharts: Boolean
    ): ExportResult {
        val fileName = generateFileName(reportData, "pdf")
        val outputFile = File(context.cacheDir, fileName)

        try {
            val pdfWriter = PdfWriter(FileOutputStream(outputFile))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4)
            document.setMargins(PDF_MARGIN, PDF_MARGIN, PDF_MARGIN, PDF_MARGIN)

            // Set up fonts
            val titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            val headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

            // Add header
            addPdfHeader(document, reportData, titleFont, headerFont, regularFont, includeLogo)

            // Add report content based on type
            when (reportData.reportType) {
                ReportType.SALES -> addSalesReportContent(document, reportData.salesReport!!, headerFont, regularFont)
                ReportType.PROFIT -> addProfitReportContent(document, reportData.profitReport!!, headerFont, regularFont)
                ReportType.STOCK_AGING -> addStockAgingReportContent(document, reportData.stockAgingReport!!, headerFont, regularFont)
                ReportType.TAX -> addTaxReportContent(document, reportData.taxReport!!, headerFont, regularFont)
                ReportType.COMBINED -> addCombinedReportContent(document, reportData, headerFont, regularFont)
                else -> addSummaryContent(document, reportData.summary, headerFont, regularFont)
            }

            // Add summary and insights
            addReportSummary(document, reportData.summary, headerFont, regularFont)

            // Add footer
            addPdfFooter(document, regularFont)

            document.close()

            return ExportResult.Success(
                filePath = outputFile.absolutePath,
                fileName = fileName,
                fileSize = outputFile.length(),
                mimeType = "application/pdf"
            )

        } catch (e: Exception) {
            return ExportResult.Error("PDF generation failed: ${e.message}")
        }
    }

    /**
     * Generate CSV report with proper formatting
     */
    private suspend fun generateCsvReport(reportData: ReportData): ExportResult {
        val fileName = generateFileName(reportData, "csv")
        val outputFile = File(context.cacheDir, fileName)

        try {
            val csvContent = StringBuilder()

            // Header information
            csvContent.append("Report Type,${reportData.reportType}\n")
            csvContent.append("Title,${reportData.title}\n")
            val generatedAtTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
            csvContent.append("Generated At,$generatedAtTime\n")
            csvContent.append("Date Range,${reportData.dateRange.startDate} to ${reportData.dateRange.endDate}\n")
            csvContent.append("\n")

            // Add report-specific CSV content
            when (reportData.reportType) {
                ReportType.SALES -> addSalesCsvContent(csvContent, reportData.salesReport!!)
                ReportType.PROFIT -> addProfitCsvContent(csvContent, reportData.profitReport!!)
                ReportType.STOCK_AGING -> addStockAgingCsvContent(csvContent, reportData.stockAgingReport!!)
                ReportType.TAX -> addTaxCsvContent(csvContent, reportData.taxReport!!)
                ReportType.COMBINED -> addCombinedCsvContent(csvContent, reportData)
                else -> addSummaryCsvContent(csvContent, reportData.summary)
            }

            // Write to file
            outputFile.writeText(csvContent.toString())

            return ExportResult.Success(
                filePath = outputFile.absolutePath,
                fileName = fileName,
                fileSize = outputFile.length(),
                mimeType = "text/csv"
            )

        } catch (e: Exception) {
            return ExportResult.Error("CSV generation failed: ${e.message}")
        }
    }

    /**
     * Generate Excel report (placeholder - would use Apache POI)
     */
    private suspend fun generateExcelReport(reportData: ReportData): ExportResult {
        // This would use Apache POI library for Excel generation
        // For now, return CSV as fallback
        return generateCsvReport(reportData)
    }

    /**
     * Generate JSON export
     */
    private suspend fun generateJsonReport(reportData: ReportData): ExportResult {
        val fileName = generateFileName(reportData, "json")
        val outputFile = File(context.cacheDir, fileName)

        try {
            // This would use a JSON serialization library like Gson or kotlinx.serialization
            val jsonContent = "{\n  \"message\": \"JSON export not yet implemented\"\n}"
            outputFile.writeText(jsonContent)

            return ExportResult.Success(
                filePath = outputFile.absolutePath,
                fileName = fileName,
                fileSize = outputFile.length(),
                mimeType = "application/json"
            )

        } catch (e: Exception) {
            return ExportResult.Error("JSON generation failed: ${e.message}")
        }
    }

    // PDF Content Generation Methods

    private fun addPdfHeader(
        document: Document,
        reportData: ReportData,
        titleFont: com.itextpdf.kernel.font.PdfFont,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont,
        includeLogo: Boolean
    ) {
        // Company header
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 60f, 20f)))
        headerTable.setWidth(UnitValue.createPercentValue(100f))

        // Logo placeholder (would be actual logo)
        val logoCell = Cell()
        if (includeLogo) {
            logoCell.add(Paragraph("LOGO").setFont(headerFont).setFontSize(12f))
        }
        headerTable.addCell(logoCell)

        // Company info
        val companyCell = Cell()
        companyCell.add(Paragraph(COMPANY_NAME).setFont(titleFont).setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
        companyCell.add(Paragraph("Business Report").setFont(headerFont).setFontSize(12f).setTextAlignment(TextAlignment.CENTER))
        headerTable.addCell(companyCell)

        // Date
        val dateCell = Cell()
        dateCell.add(Paragraph("Generated on").setFont(regularFont).setFontSize(10f))
        val formattedDate = DATE_FORMAT.format(LocalDateTime.now())
        dateCell.add(Paragraph(formattedDate).setFont(regularFont).setFontSize(10f))
        headerTable.addCell(dateCell)

        document.add(headerTable)
        document.add(Paragraph(" "))
        document.add(Paragraph(" "))

        // Report title and details
        document.add(Paragraph(reportData.title)
            .setFont(titleFont)
            .setFontSize(16f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(10f))

        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
        detailsTable.setWidth(UnitValue.createPercentValue(100f))

        detailsTable.addCell(createDetailCell("Report Type:", reportData.reportType.toString(), headerFont, regularFont))
        detailsTable.addCell(createDetailCell("Date Range:", "${reportData.dateRange.startDate} to ${reportData.dateRange.endDate}", headerFont, regularFont))

        document.add(detailsTable)
        document.add(Paragraph(" "))
    }

    private fun addSalesReportContent(
        document: Document,
        salesReport: SalesReport,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Sales overview
        document.add(createSectionHeader("Sales Overview", headerFont))
        
        val overviewTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
        overviewTable.setWidth(UnitValue.createPercentValue(100f))

        overviewTable.addHeaderCell(createHeaderCell("Total Sales", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Total Units", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Transactions", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Avg Order Value", headerFont))

        overviewTable.addCell(createDataCell(salesReport.totalSales.formatCurrency(), regularFont))
        overviewTable.addCell(createDataCell(salesReport.totalUnits.toString(), regularFont))
        overviewTable.addCell(createDataCell(salesReport.transactionCount.toString(), regularFont))
        overviewTable.addCell(createDataCell(salesReport.averageOrderValue.formatCurrency(), regularFont))

        document.add(overviewTable)
        document.add(Paragraph(" "))

        // Category-wise sales
        if (salesReport.categoryWiseSales.isNotEmpty()) {
            document.add(createSectionHeader("Category-wise Sales", headerFont))
            
            val categoryTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 25f, 20f, 25f)))
            categoryTable.setWidth(UnitValue.createPercentValue(100f))

            categoryTable.addHeaderCell(createHeaderCell("Category", headerFont))
            categoryTable.addHeaderCell(createHeaderCell("Sales", headerFont))
            categoryTable.addHeaderCell(createHeaderCell("Units", headerFont))
            categoryTable.addHeaderCell(createHeaderCell("Percentage", headerFont))

            salesReport.categoryWiseSales.take(10).forEach { category ->
                categoryTable.addCell(createDataCell(category.category, regularFont))
                categoryTable.addCell(createDataCell(category.sales.formatCurrency(), regularFont))
                categoryTable.addCell(createDataCell(category.units.toString(), regularFont))
                categoryTable.addCell(createDataCell(category.percentage.formatPercentage(), regularFont))
            }

            document.add(categoryTable)
            document.add(Paragraph(" "))
        }

        // Top selling products
        if (salesReport.topSellingProducts.isNotEmpty()) {
            document.add(createSectionHeader("Top Selling Products", headerFont))
            
            val productTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 20f, 20f, 20f)))
            productTable.setWidth(UnitValue.createPercentValue(100f))

            productTable.addHeaderCell(createHeaderCell("Product", headerFont))
            productTable.addHeaderCell(createHeaderCell("Sales", headerFont))
            productTable.addHeaderCell(createHeaderCell("Units", headerFont))
            productTable.addHeaderCell(createHeaderCell("Contribution", headerFont))

            salesReport.topSellingProducts.take(10).forEach { product ->
                productTable.addCell(createDataCell(product.productName, regularFont))
                productTable.addCell(createDataCell(product.sales.formatCurrency(), regularFont))
                productTable.addCell(createDataCell(product.units.toString(), regularFont))
                productTable.addCell(createDataCell(product.contribution.formatPercentage(), regularFont))
            }

            document.add(productTable)
            document.add(Paragraph(" "))
        }
    }

    private fun addProfitReportContent(
        document: Document,
        profitReport: ProfitReport,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Profit overview
        document.add(createSectionHeader("Profit Overview", headerFont))
        
        val overviewTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
        overviewTable.setWidth(UnitValue.createPercentValue(100f))

        overviewTable.addHeaderCell(createHeaderCell("Total Revenue", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Total Cost", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Total Profit", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Profit Margin", headerFont))

        overviewTable.addCell(createDataCell(profitReport.totalRevenue.formatCurrency(), regularFont))
        overviewTable.addCell(createDataCell(profitReport.totalCost.formatCurrency(), regularFont))
        overviewTable.addCell(createDataCell(profitReport.totalProfit.formatCurrency(), regularFont))
        overviewTable.addCell(createDataCell(profitReport.profitMargin.formatPercentage(), regularFont))

        document.add(overviewTable)
        document.add(Paragraph(" "))

        // Category-wise profit
        if (profitReport.categoryWiseProfit.isNotEmpty()) {
            document.add(createSectionHeader("Category-wise Profit Analysis", headerFont))
            
            val categoryTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 20f, 20f, 15f, 15f)))
            categoryTable.setWidth(UnitValue.createPercentValue(100f))

            categoryTable.addHeaderCell(createHeaderCell("Category", headerFont))
            categoryTable.addHeaderCell(createHeaderCell("Revenue", headerFont))
            categoryTable.addHeaderCell(createHeaderCell("Cost", headerFont))
            categoryTable.addHeaderCell(createHeaderCell("Profit", headerFont))
            categoryTable.addHeaderCell(createHeaderCell("Margin %", headerFont))

            profitReport.categoryWiseProfit.forEach { category ->
                categoryTable.addCell(createDataCell(category.category, regularFont))
                categoryTable.addCell(createDataCell(category.revenue.formatCurrency(), regularFont))
                categoryTable.addCell(createDataCell(category.cost.formatCurrency(), regularFont))
                categoryTable.addCell(createDataCell(category.profit.formatCurrency(), regularFont))
                categoryTable.addCell(createDataCell(category.margin.formatPercentage(), regularFont))
            }

            document.add(categoryTable)
        }
    }

    private fun addStockAgingReportContent(
        document: Document,
        stockAgingReport: StockAgingReport,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Stock aging overview
        document.add(createSectionHeader("Stock Aging Overview", headerFont))
        
        val overviewTable = Table(UnitValue.createPercentArray(floatArrayOf(33f, 33f, 34f)))
        overviewTable.setWidth(UnitValue.createPercentValue(100f))

        overviewTable.addHeaderCell(createHeaderCell("Total Stock Value", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Total Items", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Average Age", headerFont))

        overviewTable.addCell(createDataCell(stockAgingReport.totalStockValue.formatCurrency(), regularFont))
        overviewTable.addCell(createDataCell(stockAgingReport.totalItems.toString(), regularFont))
        overviewTable.addCell(createDataCell("${stockAgingReport.averageAge.formatLocale("%.0f")} days", regularFont))

        document.add(overviewTable)
        document.add(Paragraph(" "))

        // Aging categories
        document.add(createSectionHeader("Stock Age Distribution", headerFont))
        
        val agingTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
        agingTable.setWidth(UnitValue.createPercentValue(100f))

        agingTable.addHeaderCell(createHeaderCell("Age Category", headerFont))
        agingTable.addHeaderCell(createHeaderCell("Item Count", headerFont))
        agingTable.addHeaderCell(createHeaderCell("Stock Value", headerFont))
        agingTable.addHeaderCell(createHeaderCell("Percentage", headerFont))

        stockAgingReport.agingCategories.forEach { category ->
            agingTable.addCell(createDataCell("${category.category.name} (${category.category.minDays}-${category.category.maxDays} days)", regularFont))
            agingTable.addCell(createDataCell(category.itemCount.toString(), regularFont))
            agingTable.addCell(createDataCell(category.stockValue.formatCurrency(), regularFont))
            agingTable.addCell(createDataCell(category.percentage.formatPercentage(), regularFont))
        }

        document.add(agingTable)
        document.add(Paragraph(" "))

        // Dead stock items
        if (stockAgingReport.deadStockItems.isNotEmpty()) {
            document.add(createSectionHeader("Dead Stock Items", headerFont))
            
            val deadStockTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 20f, 20f, 20f)))
            deadStockTable.setWidth(UnitValue.createPercentValue(100f))

            deadStockTable.addHeaderCell(createHeaderCell("Product", headerFont))
            deadStockTable.addHeaderCell(createHeaderCell("Days w/o Sale", headerFont))
            deadStockTable.addHeaderCell(createHeaderCell("Stock Value", headerFont))
            deadStockTable.addHeaderCell(createHeaderCell("Action", headerFont))

            stockAgingReport.deadStockItems.take(10).forEach { item ->
                deadStockTable.addCell(createDataCell(item.productName, regularFont))
                deadStockTable.addCell(createDataCell(item.daysWithoutSale.toString(), regularFont))
                deadStockTable.addCell(createDataCell(item.stockValue.formatCurrency(), regularFont))
                deadStockTable.addCell(createDataCell(item.recommendedAction.toString(), regularFont))
            }

            document.add(deadStockTable)
        }
    }

    private fun addTaxReportContent(
        document: Document,
        taxReport: TaxReport,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Tax overview
        document.add(createSectionHeader("Tax Summary", headerFont))
        
        val overviewTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
        overviewTable.setWidth(UnitValue.createPercentValue(100f))

        overviewTable.addHeaderCell(createHeaderCell("Taxable Revenue", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Non-Taxable Revenue", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Total Tax Collected", headerFont))
        overviewTable.addHeaderCell(createHeaderCell("Tax Rate", headerFont))

        val totalRevenue = taxReport.totalTaxableRevenue + taxReport.totalNonTaxableRevenue
        val taxRate = if (totalRevenue > java.math.BigDecimal.ZERO) {
            (taxReport.totalTaxCollected / totalRevenue).toDouble() * 100
        } else 0.0

        overviewTable.addCell(createDataCell(taxReport.totalTaxableRevenue.formatCurrency(), regularFont))
        overviewTable.addCell(createDataCell(taxReport.totalNonTaxableRevenue.formatCurrency(), regularFont))
        overviewTable.addCell(createDataCell(taxReport.totalTaxCollected.formatCurrency(), regularFont))
        overviewTable.addCell(createDataCell(taxRate.formatPercentage(), regularFont))

        document.add(overviewTable)
        document.add(Paragraph(" "))

        // GST breakdown
        document.add(createSectionHeader("GST Breakdown", headerFont))
        
        val gstTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 20f, 20f, 20f, 20f)))
        gstTable.setWidth(UnitValue.createPercentValue(100f))

        gstTable.addHeaderCell(createHeaderCell("CGST", headerFont))
        gstTable.addHeaderCell(createHeaderCell("SGST", headerFont))
        gstTable.addHeaderCell(createHeaderCell("IGST", headerFont))
        gstTable.addHeaderCell(createHeaderCell("Cess", headerFont))
        gstTable.addHeaderCell(createHeaderCell("Total", headerFont))

        gstTable.addCell(createDataCell(taxReport.gstBreakdown.cgst.formatCurrency(), regularFont))
        gstTable.addCell(createDataCell(taxReport.gstBreakdown.sgst.formatCurrency(), regularFont))
        gstTable.addCell(createDataCell(taxReport.gstBreakdown.igst.formatCurrency(), regularFont))
        gstTable.addCell(createDataCell(taxReport.gstBreakdown.cess.formatCurrency(), regularFont))
        gstTable.addCell(createDataCell(taxReport.gstBreakdown.total.formatCurrency(), regularFont))

        document.add(gstTable)
        document.add(Paragraph(" "))

        // Tax rate wise breakdown
        if (taxReport.taxRateWiseBreakdown.isNotEmpty()) {
            document.add(createSectionHeader("Tax Rate-wise Breakdown", headerFont))
            
            val taxRateTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 30f, 25f, 25f)))
            taxRateTable.setWidth(UnitValue.createPercentValue(100f))

            taxRateTable.addHeaderCell(createHeaderCell("Tax Rate", headerFont))
            taxRateTable.addHeaderCell(createHeaderCell("Taxable Amount", headerFont))
            taxRateTable.addHeaderCell(createHeaderCell("Tax Amount", headerFont))
            taxRateTable.addHeaderCell(createHeaderCell("Transactions", headerFont))

            taxReport.taxRateWiseBreakdown.forEach { breakdown ->
                taxRateTable.addCell(createDataCell("${breakdown.taxRate}%", regularFont))
                taxRateTable.addCell(createDataCell(breakdown.taxableAmount.formatCurrency(), regularFont))
                taxRateTable.addCell(createDataCell(breakdown.taxAmount.formatCurrency(), regularFont))
                taxRateTable.addCell(createDataCell(breakdown.transactionCount.toString(), regularFont))
            }

            document.add(taxRateTable)
        }
    }

    private fun addCombinedReportContent(
        document: Document,
        reportData: ReportData,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        // Add sections from each report type
        reportData.salesReport?.let { addSalesReportContent(document, it, headerFont, regularFont) }
        reportData.profitReport?.let { addProfitReportContent(document, it, headerFont, regularFont) }
        reportData.stockAgingReport?.let { addStockAgingReportContent(document, it, headerFont, regularFont) }
        reportData.taxReport?.let { addTaxReportContent(document, it, headerFont, regularFont) }
    }

    private fun addSummaryContent(
        document: Document,
        summary: ReportSummary,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        document.add(createSectionHeader("Report Summary", headerFont))
        
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
        summaryTable.setWidth(UnitValue.createPercentValue(100f))

        summaryTable.addCell(createDetailCell("Total Records:", summary.totalRecords.toString(), headerFont, regularFont))
        summaryTable.addCell(createDetailCell("Total Revenue:", summary.totalRevenue.formatCurrency(), headerFont, regularFont))
        summaryTable.addCell(createDetailCell("Total Profit:", summary.totalProfit.formatCurrency(), headerFont, regularFont))
        summaryTable.addCell(createDetailCell("Profit Margin:", summary.profitMargin.formatPercentage(), headerFont, regularFont))

        document.add(summaryTable)
    }

    private fun addReportSummary(
        document: Document,
        summary: ReportSummary,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        document.add(Paragraph(" "))
        document.add(createSectionHeader("Key Insights", headerFont))

        if (summary.keyInsights.isNotEmpty()) {
            summary.keyInsights.forEach { insight ->
                document.add(Paragraph("• $insight").setFont(regularFont).setFontSize(11f))
            }
        } else {
            document.add(Paragraph("No specific insights available for this report period.").setFont(regularFont).setFontSize(11f))
        }

        document.add(Paragraph(" "))
        document.add(createSectionHeader("Recommendations", headerFont))

        if (summary.recommendations.isNotEmpty()) {
            summary.recommendations.forEach { recommendation ->
                document.add(Paragraph("• $recommendation").setFont(regularFont).setFontSize(11f))
            }
        } else {
            document.add(Paragraph("No specific recommendations available.").setFont(regularFont).setFontSize(11f))
        }
    }

    private fun addPdfFooter(
        document: Document,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ) {
        document.add(Paragraph(" "))
        document.add(Paragraph(" "))
        
        val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
        footerTable.setWidth(UnitValue.createPercentValue(100f))

        val leftFooter = Cell()
        leftFooter.add(Paragraph("Generated by $COMPANY_NAME").setFont(regularFont).setFontSize(9f))
        footerTable.addCell(leftFooter)

        val rightFooter = Cell()
        val generatedTime = DATE_FORMAT.format(LocalDateTime.now())
        rightFooter.add(Paragraph("Report generated on $generatedTime")
            .setFont(regularFont)
            .setFontSize(9f)
            .setTextAlignment(TextAlignment.RIGHT))
        footerTable.addCell(rightFooter)

        document.add(footerTable)
    }

    // CSV Content Generation Methods

    private fun addSalesCsvContent(csvContent: StringBuilder, salesReport: SalesReport) {
        csvContent.append("SALES OVERVIEW\n")
        csvContent.append("Metric,Value\n")
        csvContent.append("Total Sales,${salesReport.totalSales}\n")
        csvContent.append("Total Units,${salesReport.totalUnits}\n")
        csvContent.append("Transaction Count,${salesReport.transactionCount}\n")
        csvContent.append("Average Order Value,${salesReport.averageOrderValue}\n")
        csvContent.append("\n")

        if (salesReport.categoryWiseSales.isNotEmpty()) {
            csvContent.append("CATEGORY-WISE SALES\n")
            csvContent.append("Category,Sales,Units,Profit,Percentage\n")
            salesReport.categoryWiseSales.forEach { category ->
                csvContent.append("${category.category},${category.sales},${category.units},${category.profit},${category.percentage}\n")
            }
            csvContent.append("\n")
        }

        if (salesReport.topSellingProducts.isNotEmpty()) {
            csvContent.append("TOP SELLING PRODUCTS\n")
            csvContent.append("Rank,Product Name,Brand,Category,Sales,Units,Contribution\n")
            salesReport.topSellingProducts.forEach { product ->
                csvContent.append("${product.rank},${product.productName},${product.brand},${product.category},${product.sales},${product.units},${product.contribution}\n")
            }
            csvContent.append("\n")
        }

        if (salesReport.dailySales.isNotEmpty()) {
            csvContent.append("DAILY SALES DATA\n")
            csvContent.append("Date,Sales,Units,Transactions,Average Order Value\n")
            salesReport.dailySales.forEach { daily ->
                csvContent.append("${daily.date},${daily.sales},${daily.units},${daily.transactions},${daily.averageOrderValue}\n")
            }
            csvContent.append("\n")
        }
    }

    private fun addProfitCsvContent(csvContent: StringBuilder, profitReport: ProfitReport) {
        csvContent.append("PROFIT OVERVIEW\n")
        csvContent.append("Metric,Value\n")
        csvContent.append("Total Revenue,${profitReport.totalRevenue}\n")
        csvContent.append("Total Cost,${profitReport.totalCost}\n")
        csvContent.append("Total Profit,${profitReport.totalProfit}\n")
        csvContent.append("Profit Margin,${profitReport.profitMargin}\n")
        csvContent.append("\n")

        if (profitReport.categoryWiseProfit.isNotEmpty()) {
            csvContent.append("CATEGORY-WISE PROFIT\n")
            csvContent.append("Category,Revenue,Cost,Profit,Margin,Rank\n")
            profitReport.categoryWiseProfit.forEach { category ->
                csvContent.append("${category.category},${category.revenue},${category.cost},${category.profit},${category.margin},${category.rank}\n")
            }
            csvContent.append("\n")
        }

        if (profitReport.dailyProfit.isNotEmpty()) {
            csvContent.append("DAILY PROFIT DATA\n")
            csvContent.append("Date,Revenue,Cost,Profit,Margin\n")
            profitReport.dailyProfit.forEach { daily ->
                csvContent.append("${daily.date},${daily.revenue},${daily.cost},${daily.profit},${daily.margin}\n")
            }
            csvContent.append("\n")
        }
    }

    private fun addStockAgingCsvContent(csvContent: StringBuilder, stockAgingReport: StockAgingReport) {
        csvContent.append("STOCK AGING OVERVIEW\n")
        csvContent.append("Metric,Value\n")
        csvContent.append("Total Stock Value,${stockAgingReport.totalStockValue}\n")
        csvContent.append("Total Items,${stockAgingReport.totalItems}\n")
        csvContent.append("Average Age (days),${stockAgingReport.averageAge}\n")
        csvContent.append("\n")

        csvContent.append("AGING CATEGORIES\n")
        csvContent.append("Category,Age Range,Item Count,Stock Value,Percentage\n")
        stockAgingReport.agingCategories.forEach { category ->
            csvContent.append("${category.category.name},${category.category.minDays}-${category.category.maxDays},${category.itemCount},${category.stockValue},${category.percentage}\n")
        }
        csvContent.append("\n")

        if (stockAgingReport.deadStockItems.isNotEmpty()) {
            csvContent.append("DEAD STOCK ITEMS\n")
            csvContent.append("Product Name,Days Without Sale,Stock Value,Recommended Action\n")
            stockAgingReport.deadStockItems.forEach { item ->
                csvContent.append("${item.productName},${item.daysWithoutSale},${item.stockValue},${item.recommendedAction}\n")
            }
            csvContent.append("\n")
        }
    }

    private fun addTaxCsvContent(csvContent: StringBuilder, taxReport: TaxReport) {
        csvContent.append("TAX OVERVIEW\n")
        csvContent.append("Metric,Value\n")
        csvContent.append("Taxable Revenue,${taxReport.totalTaxableRevenue}\n")
        csvContent.append("Non-Taxable Revenue,${taxReport.totalNonTaxableRevenue}\n")
        csvContent.append("Total Tax Collected,${taxReport.totalTaxCollected}\n")
        csvContent.append("Taxable Transactions,${taxReport.taxableTransactions}\n")
        csvContent.append("Non-Taxable Transactions,${taxReport.nonTaxableTransactions}\n")
        csvContent.append("\n")

        csvContent.append("GST BREAKDOWN\n")
        csvContent.append("Component,Amount\n")
        csvContent.append("CGST,${taxReport.gstBreakdown.cgst}\n")
        csvContent.append("SGST,${taxReport.gstBreakdown.sgst}\n")
        csvContent.append("IGST,${taxReport.gstBreakdown.igst}\n")
        csvContent.append("Cess,${taxReport.gstBreakdown.cess}\n")
        csvContent.append("Total,${taxReport.gstBreakdown.total}\n")
        csvContent.append("\n")

        if (taxReport.taxRateWiseBreakdown.isNotEmpty()) {
            csvContent.append("TAX RATE BREAKDOWN\n")
            csvContent.append("Tax Rate,Taxable Amount,Tax Amount,Transaction Count,Percentage\n")
            taxReport.taxRateWiseBreakdown.forEach { breakdown ->
                csvContent.append("${breakdown.taxRate},${breakdown.taxableAmount},${breakdown.taxAmount},${breakdown.transactionCount},${breakdown.percentage}\n")
            }
            csvContent.append("\n")
        }
    }

    private fun addCombinedCsvContent(csvContent: StringBuilder, reportData: ReportData) {
        reportData.salesReport?.let { addSalesCsvContent(csvContent, it) }
        reportData.profitReport?.let { addProfitCsvContent(csvContent, it) }
        reportData.stockAgingReport?.let { addStockAgingCsvContent(csvContent, it) }
        reportData.taxReport?.let { addTaxCsvContent(csvContent, it) }
    }

    private fun addSummaryCsvContent(csvContent: StringBuilder, summary: ReportSummary) {
        csvContent.append("SUMMARY\n")
        csvContent.append("Metric,Value\n")
        csvContent.append("Total Records,${summary.totalRecords}\n")
        csvContent.append("Total Revenue,${summary.totalRevenue}\n")
        csvContent.append("Total Profit,${summary.totalProfit}\n")
        csvContent.append("Total Tax,${summary.totalTax}\n")
        csvContent.append("Average Order Value,${summary.averageOrderValue}\n")
        csvContent.append("Profit Margin,${summary.profitMargin}\n")
        csvContent.append("\n")

        if (summary.keyInsights.isNotEmpty()) {
            csvContent.append("KEY INSIGHTS\n")
            summary.keyInsights.forEach { insight ->
                csvContent.append("\"${insight.replace("\"", "\"\"")}\"\n")
            }
            csvContent.append("\n")
        }

        if (summary.recommendations.isNotEmpty()) {
            csvContent.append("RECOMMENDATIONS\n")
            summary.recommendations.forEach { recommendation ->
                csvContent.append("\"${recommendation.replace("\"", "\"\"")}\"\n")
            }
            csvContent.append("\n")
        }
    }

    // Helper Methods

    private fun createSectionHeader(title: String, headerFont: com.itextpdf.kernel.font.PdfFont): Paragraph {
        return Paragraph(title)
            .setFont(headerFont)
            .setFontSize(14f)
            .setMarginTop(10f)
            .setMarginBottom(8f)
            .setFontColor(DeviceRgb(0, 51, 102))
    }

    private fun createHeaderCell(text: String, headerFont: com.itextpdf.kernel.font.PdfFont): Cell {
        return Cell()
            .add(Paragraph(text).setFont(headerFont).setFontSize(10f))
            .setBackgroundColor(DeviceRgb(240, 240, 240))
            .setPadding(TABLE_CELL_PADDING)
            .setTextAlignment(TextAlignment.CENTER)
    }

    private fun createDataCell(text: String, regularFont: com.itextpdf.kernel.font.PdfFont): Cell {
        return Cell()
            .add(Paragraph(text).setFont(regularFont).setFontSize(9f))
            .setPadding(TABLE_CELL_PADDING)
            .setTextAlignment(TextAlignment.LEFT)
    }

    private fun createDetailCell(
        label: String,
        value: String,
        headerFont: com.itextpdf.kernel.font.PdfFont,
        regularFont: com.itextpdf.kernel.font.PdfFont
    ): Cell {
        return Cell()
            .add(Paragraph(label).setFont(headerFont).setFontSize(10f))
            .add(Paragraph(value).setFont(regularFont).setFontSize(10f))
            .setPadding(TABLE_CELL_PADDING)
    }

    private fun generateFileName(reportData: ReportData, extension: String): String {
        val dateStr = CSV_DATE_FORMAT.format(LocalDateTime.now())
        val reportType = reportData.reportType.toString().lowercase()
        return "${reportType}_report_${dateStr}.$extension"
    }
}

/**
 * Result of export operation
 */
sealed class ExportResult {
    data class Success(
        val filePath: String,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String
    ) : ExportResult()

    data class Error(
        val message: String
    ) : ExportResult()
}