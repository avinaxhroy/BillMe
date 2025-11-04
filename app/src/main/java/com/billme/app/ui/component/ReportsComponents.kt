package com.billme.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.core.util.formatCurrency
import com.billme.app.core.util.formatPercentage
import com.billme.app.core.util.formatLocale
import com.billme.app.data.model.*
import com.billme.app.ui.viewmodel.ReportUiState
import com.billme.app.ui.viewmodel.ReportsViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Main Reports Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val currentReportType by viewModel.currentReportType.collectAsState()
    val currentFilters by viewModel.currentFilters.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header with report type selection and actions
        ReportsHeader(
            currentReportType = currentReportType,
            onReportTypeChange = { viewModel.setReportType(it) },
            onShowFilters = { showFilterDialog = true },
            onGenerateReport = { viewModel.generateReport() },
            onShowExport = { showExportDialog = true },
            canExport = uiState is ReportUiState.Success,
            isLoading = uiState is ReportUiState.Loading
        )

        // Report content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (uiState) {
                is ReportUiState.Idle -> {
                    ReportsIdleState(
                        onGenerateReport = { viewModel.generateReport() }
                    )
                }
                is ReportUiState.Loading -> {
                    ReportsLoadingState()
                }
                is ReportUiState.Success -> {
                    val successState = uiState as ReportUiState.Success
                    ReportsSuccessState(
                        reportData = successState.data,
                        onExport = { format -> viewModel.exportCurrentReport(format) }
                    )
                }
                is ReportUiState.Error -> {
                    val errorState = uiState as ReportUiState.Error
                    ReportsErrorState(
                        error = errorState.message,
                        onRetry = { viewModel.generateReport() }
                    )
                }
            }
        }
    }

    // Filter dialog
    if (showFilterDialog) {
        ReportFiltersDialog(
            currentFilters = currentFilters,
            onFiltersChange = { updatedFilters ->
                viewModel.updateFilters { updatedFilters }
                updatedFilters
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    // Export dialog
    val currentUiState = uiState
    if (showExportDialog && currentUiState is ReportUiState.Success) {
        ExportDialog(
            reportData = currentUiState.data,
            exportState = exportState,
            onExport = { format -> viewModel.exportCurrentReport(format) },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun ReportsHeader(
    currentReportType: ReportType,
    onReportTypeChange: (ReportType) -> Unit,
    onShowFilters: () -> Unit,
    onGenerateReport: () -> Unit,
    onShowExport: () -> Unit,
    canExport: Boolean,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Business Reports",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onShowFilters) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(
                        onClick = onShowExport,
                        enabled = canExport
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = if (canExport) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Report type selector
            ReportTypeSelector(
                currentType = currentReportType,
                onTypeChange = onReportTypeChange
            )

            // Generate button
            Button(
                onClick = onGenerateReport,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Generate Report")
            }
        }
    }
}

@Composable
private fun ReportTypeSelector(
    currentType: ReportType,
    onTypeChange: (ReportType) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ReportType.values().filter { it != ReportType.CUSTOM && it != ReportType.INVENTORY && it != ReportType.CUSTOMER }) { type ->
            FilterChip(
                selected = currentType == type,
                onClick = { onTypeChange(type) },
                label = {
                    Text(
                        text = when (type) {
                            ReportType.SALES -> "Sales"
                            ReportType.PROFIT -> "Profit"
                            ReportType.STOCK_AGING -> "Stock Aging"
                            ReportType.TAX -> "Tax"
                            ReportType.COMBINED -> "Combined"
                            else -> type.name
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        when (type) {
                            ReportType.SALES -> Icons.AutoMirrored.Filled.TrendingUp
                            ReportType.PROFIT -> Icons.Default.AttachMoney
                            ReportType.STOCK_AGING -> Icons.Default.Inventory
                            ReportType.TAX -> Icons.Default.Receipt
                            ReportType.COMBINED -> Icons.Default.Assessment
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun ReportsIdleState(
    onGenerateReport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Assessment,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Generate Business Reports",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select report type and filters, then generate detailed reports with insights and recommendations.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGenerateReport
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Report")
        }
    }
}

@Composable
private fun ReportsLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Generating Report...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please wait while we analyze your data and generate comprehensive insights.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportsSuccessState(
    reportData: ReportData,
    onExport: (ExportFormat) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report header
        item {
            ReportHeaderCard(reportData)
        }

        // Report summary
        item {
            ReportSummaryCard(reportData.summary)
        }

        // Report content based on type
        when (reportData.reportType) {
            ReportType.SALES -> {
                reportData.salesReport?.let { salesReport ->
                    item {
                        SalesReportContent(salesReport)
                    }
                }
            }
            ReportType.PROFIT -> {
                reportData.profitReport?.let { profitReport ->
                    item {
                        ProfitReportContent(profitReport)
                    }
                }
            }
            ReportType.STOCK_AGING -> {
                reportData.stockAgingReport?.let { stockAgingReport ->
                    item {
                        StockAgingReportContent(stockAgingReport)
                    }
                }
            }
            ReportType.TAX -> {
                reportData.taxReport?.let { taxReport ->
                    item {
                        TaxReportContent(taxReport)
                    }
                }
            }
            ReportType.COMBINED -> {
                reportData.salesReport?.let { salesReport ->
                    item {
                        SalesReportContent(salesReport)
                    }
                }
                reportData.profitReport?.let { profitReport ->
                    item {
                        ProfitReportContent(profitReport)
                    }
                }
                reportData.stockAgingReport?.let { stockAgingReport ->
                    item {
                        StockAgingReportContent(stockAgingReport)
                    }
                }
                reportData.taxReport?.let { taxReport ->
                    item {
                        TaxReportContent(taxReport)
                    }
                }
            }
            else -> {
                // Custom or other report types would be handled here
            }
        }

        // Insights and recommendations
        if (reportData.summary.keyInsights.isNotEmpty() || reportData.summary.recommendations.isNotEmpty()) {
            item {
                InsightsAndRecommendationsCard(
                    insights = reportData.summary.keyInsights,
                    recommendations = reportData.summary.recommendations
                )
            }
        }

        // Export actions
        item {
            ExportActionsCard(
                onExport = onExport
            )
        }
    }
}

@Composable
private fun ReportsErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Report Generation Failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun ReportHeaderCard(reportData: ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = reportData.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Generated: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(reportData.generatedAt.toInstant(kotlinx.datetime.TimeZone.currentSystemDefault()).toEpochMilliseconds()))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Period: ${reportData.dateRange.startDate} to ${reportData.dateRange.endDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(summary: ReportSummary) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Executive Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryMetricItem(
                        label = "Total Revenue",
                        value = summary.totalRevenue.formatCurrency(),
                        icon = Icons.Default.AttachMoney
                    )
                }
                item {
                    SummaryMetricItem(
                        label = "Total Profit",
                        value = summary.totalProfit.formatCurrency(),
                        icon = Icons.AutoMirrored.Filled.TrendingUp
                    )
                }
                item {
                    SummaryMetricItem(
                        label = "Profit Margin",
                        value = summary.profitMargin.formatPercentage(),
                        icon = Icons.Default.Percent
                    )
                }
                item {
                    SummaryMetricItem(
                        label = "Records",
                        value = summary.totalRecords.toString(),
                        icon = Icons.Default.Receipt
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesReportContent(salesReport: SalesReport) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sales Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Sales overview metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn(
                    label = "Total Sales",
                    value = salesReport.totalSales.formatCurrency()
                )
                MetricColumn(
                    label = "Units Sold",
                    value = salesReport.totalUnits.toString()
                )
                MetricColumn(
                    label = "Transactions",
                    value = salesReport.transactionCount.toString()
                )
                MetricColumn(
                    label = "Avg Order Value",
                    value = salesReport.averageOrderValue.formatCurrency()
                )
            }

            // Category breakdown
            if (salesReport.categoryWiseSales.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Top Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                salesReport.categoryWiseSales.take(5).forEach { category ->
                    CategorySalesRow(category)
                }
            }

            // Top products
            if (salesReport.topSellingProducts.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Top Products",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                salesReport.topSellingProducts.take(5).forEach { product ->
                    TopProductRow(product)
                }
            }
        }
    }
}

@Composable
private fun ProfitReportContent(profitReport: ProfitReport) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Profit Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Profit overview metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn(
                    label = "Revenue",
                    value = profitReport.totalRevenue.formatCurrency()
                )
                MetricColumn(
                    label = "Cost",
                    value = profitReport.totalCost.formatCurrency()
                )
                MetricColumn(
                    label = "Profit",
                    value = profitReport.totalProfit.formatCurrency()
                )
                MetricColumn(
                    label = "Margin",
                    value = profitReport.profitMargin.formatPercentage()
                )
            }

            // Category profit analysis
            if (profitReport.categoryWiseProfit.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Category Performance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                profitReport.categoryWiseProfit.take(5).forEach { category ->
                    CategoryProfitRow(category)
                }
            }
        }
    }
}

@Composable
private fun StockAgingReportContent(stockAgingReport: StockAgingReport) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Stock Aging Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Stock overview metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn(
                    label = "Stock Value",
                    value = stockAgingReport.totalStockValue.formatCurrency()
                )
                MetricColumn(
                    label = "Items",
                    value = stockAgingReport.totalItems.toString()
                )
                MetricColumn(
                    label = "Avg Age",
                    value = "${stockAgingReport.averageAge.formatLocale("%.0f")} days"
                )
            }

            // Age distribution
            if (stockAgingReport.agingCategories.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Age Distribution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                stockAgingReport.agingCategories.forEach { category ->
                    StockAgingRow(category)
                }
            }

            // Dead stock items
            if (stockAgingReport.deadStockItems.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Dead Stock Alert (${stockAgingReport.deadStockItems.size} items)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )

                stockAgingReport.deadStockItems.take(3).forEach { item ->
                    DeadStockRow(item)
                }

                if (stockAgingReport.deadStockItems.size > 3) {
                    Text(
                        text = "... and ${stockAgingReport.deadStockItems.size - 3} more items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TaxReportContent(taxReport: TaxReport) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tax Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Tax overview metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn(
                    label = "Taxable Revenue",
                    value = taxReport.totalTaxableRevenue.formatCurrency()
                )
                MetricColumn(
                    label = "Tax Collected",
                    value = taxReport.totalTaxCollected.formatCurrency()
                )
                MetricColumn(
                    label = "Tax Rate",
                    value = if (taxReport.totalTaxableRevenue > java.math.BigDecimal.ZERO) {
                        ((taxReport.totalTaxCollected / taxReport.totalTaxableRevenue).toDouble() * 100).formatPercentage()
                    } else "0%"
                )
            }

            // GST breakdown
            HorizontalDivider()
            Text(
                text = "GST Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (taxReport.gstBreakdown.cgst > java.math.BigDecimal.ZERO) {
                    MetricColumn(
                        label = "CGST",
                        value = taxReport.gstBreakdown.cgst.formatCurrency()
                    )
                }
                if (taxReport.gstBreakdown.sgst > java.math.BigDecimal.ZERO) {
                    MetricColumn(
                        label = "SGST",
                        value = taxReport.gstBreakdown.sgst.formatCurrency()
                    )
                }
                if (taxReport.gstBreakdown.igst > java.math.BigDecimal.ZERO) {
                    MetricColumn(
                        label = "IGST",
                        value = taxReport.gstBreakdown.igst.formatCurrency()
                    )
                }
            }

            // Tax rate breakdown
            if (taxReport.taxRateWiseBreakdown.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Tax Rates",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                taxReport.taxRateWiseBreakdown.forEach { breakdown ->
                    TaxRateRow(breakdown)
                }
            }
        }
    }
}

@Composable
private fun InsightsAndRecommendationsCard(
    insights: List<String>,
    recommendations: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (insights.isNotEmpty()) {
                Text(
                    text = "Key Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                insights.forEach { insight ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = insight,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (recommendations.isNotEmpty()) {
                if (insights.isNotEmpty()) {
                    HorizontalDivider()
                }

                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                recommendations.forEach { recommendation ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportActionsCard(
    onExport: (ExportFormat) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Export Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onExport(ExportFormat.PDF) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF")
                }

                OutlinedButton(
                    onClick = { onExport(ExportFormat.CSV) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CSV")
                }
            }
        }
    }
}

// Filter Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFiltersDialog(
    currentFilters: ReportFilters,
    onFiltersChange: (ReportFilters) -> ReportFilters,
    onDismiss: () -> Unit
) {
    var tempFilters by remember { mutableStateOf(currentFilters) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Report Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Date range
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleMedium
                )

                // Date range presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(
                        DateRangePreset.TODAY,
                        DateRangePreset.THIS_WEEK,
                        DateRangePreset.THIS_MONTH,
                        DateRangePreset.THIS_QUARTER,
                        DateRangePreset.THIS_YEAR
                    )) { preset ->
                        FilterChip(
                            selected = tempFilters.dateRange.preset == preset,
                            onClick = { 
                                val now = kotlinx.datetime.Clock.System.now()
                                val today = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                                tempFilters = tempFilters.copy(
                                    dateRange = DateRange(
                                        startDate = today,
                                        endDate = today,
                                        preset = preset
                                    )
                                )
                            },
                            label = {
                                Text(
                                    text = when (preset) {
                                        DateRangePreset.TODAY -> "Today"
                                        DateRangePreset.THIS_WEEK -> "This Week"
                                        DateRangePreset.THIS_MONTH -> "This Month"
                                        DateRangePreset.THIS_QUARTER -> "This Quarter"
                                        DateRangePreset.THIS_YEAR -> "This Year"
                                        else -> "Custom"
                                    }
                                )
                            }
                        )
                    }
                }

                // Tax filter
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaxableFilter.values().forEach { filter ->
                        FilterChip(
                            selected = tempFilters.taxable == filter,
                            onClick = { tempFilters = tempFilters.copy(taxable = filter) },
                            label = {
                                Text(
                                    text = when (filter) {
                                        TaxableFilter.ALL -> "All"
                                        TaxableFilter.TAXABLE_ONLY -> "Taxable Only"
                                        TaxableFilter.NON_TAXABLE_ONLY -> "Non-Taxable Only"
                                    }
                                )
                            }
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onFiltersChange(tempFilters)
                            onDismiss()
                        }
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

// Export Dialog
@Composable
private fun ExportDialog(
    reportData: ReportData,
    exportState: com.billme.app.ui.viewmodel.ExportState,
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Export Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                when (exportState) {
                    is com.billme.app.ui.viewmodel.ExportState.Idle -> {
                        Text("Choose export format:")

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportFormat.values().forEach { format ->
                                OutlinedButton(
                                    onClick = { onExport(format) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        when (format) {
                                            ExportFormat.PDF -> Icons.Default.PictureAsPdf
                                            ExportFormat.CSV -> Icons.Default.TableChart
                                            ExportFormat.EXCEL -> Icons.Default.GridOn
                                            ExportFormat.JSON -> Icons.Default.DataObject
                                        },
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export as ${format.name}")
                                }
                            }
                        }
                    }
                    is com.billme.app.ui.viewmodel.ExportState.Exporting -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator()
                            Text("Exporting as ${exportState.format.name}...")
                        }
                    }
                    is com.billme.app.ui.viewmodel.ExportState.Success -> {
                        Text(
                            text = "Export completed successfully!",
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = {
                                // Handle file sharing/opening
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Report")
                        }
                    }
                    is com.billme.app.ui.viewmodel.ExportState.Error -> {
                        Text(
                            text = "Export failed: ${exportState.message}",
                            color = MaterialTheme.colorScheme.error
                        )

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }

                if (exportState !is com.billme.app.ui.viewmodel.ExportState.Exporting) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

// Helper Components

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CategorySalesRow(category: CategorySalesData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${category.units} units • ${category.percentage.formatPercentage()} of total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = category.sales.formatCurrency(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TopProductRow(product: ProductSalesData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${product.rank} ${product.productName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${product.brand} • ${product.units} units",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = product.sales.formatCurrency(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CategoryProfitRow(category: CategoryProfitData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Revenue: ${category.revenue.formatCurrency()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = category.profit.formatCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${category.margin.formatPercentage()} margin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StockAgingRow(category: StockAgingCategory) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${category.category.name} (${category.category.minDays}-${category.category.maxDays} days)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${category.itemCount} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = category.stockValue.formatCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = category.percentage.formatPercentage(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DeadStockRow(item: DeadStockItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.daysWithoutSale} days without sale",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.stockValue.formatCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.recommendedAction.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun TaxRateRow(breakdown: TaxRateBreakdown) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${breakdown.taxRate}% Tax Rate",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${breakdown.transactionCount} transactions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = breakdown.taxAmount.formatCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "on ${breakdown.taxableAmount.formatCurrency()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}