package com.billme.app.core.service

import com.billme.app.data.model.*
import com.billme.app.data.repository.ReportsRepository
import com.billme.app.core.util.formatLocale
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for advanced report generation with computational analysis and chart data preparation
 */
@Singleton
class ReportGenerationService @Inject constructor(
    private val reportsRepository: ReportsRepository
) {

    /**
     * Generate complete report with all sections and chart data
     */
    suspend fun generateCompleteReport(
        reportType: ReportType,
        filters: ReportFilters
    ): ReportData = coroutineScope {
        val reportId = UUID.randomUUID().toString()
        val title = generateReportTitle(reportType, filters)
        
        when (reportType) {
            ReportType.SALES -> generateSalesReportData(reportId, title, filters)
            ReportType.PROFIT -> generateProfitReportData(reportId, title, filters)
            ReportType.STOCK_AGING -> generateStockAgingReportData(reportId, title, filters)
            ReportType.TAX -> generateTaxReportData(reportId, title, filters)
            ReportType.COMBINED -> generateCombinedReportData(reportId, title, filters)
            ReportType.CUSTOM -> generateCustomReportData(reportId, title, filters)
            else -> throw IllegalArgumentException("Unsupported report type: $reportType")
        }
    }

    /**
     * Generate sales report with comprehensive analysis
     */
    private suspend fun generateSalesReportData(
        reportId: String,
        title: String,
        filters: ReportFilters
    ): ReportData = coroutineScope {
        val salesReport = reportsRepository.generateSalesReport(filters)
        val chartData = generateSalesChartData(salesReport)
        val summary = generateSalesReportSummary(salesReport, filters)
        val insights = analyzeSalesInsights(salesReport)
        val recommendations = generateSalesRecommendations(salesReport)

        ReportData(
            reportId = reportId,
            reportType = ReportType.SALES,
            title = title,
            dateRange = filters.dateRange,
            filters = filters,
            generatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            salesReport = salesReport,
            summary = summary.copy(
                keyInsights = insights,
                recommendations = recommendations
            )
        )
    }

    /**
     * Generate profit report with margin analysis
     */
    private suspend fun generateProfitReportData(
        reportId: String,
        title: String,
        filters: ReportFilters
    ): ReportData = coroutineScope {
        val profitReport = reportsRepository.generateProfitReport(filters)
        val chartData = generateProfitChartData(profitReport)
        val summary = generateProfitReportSummary(profitReport, filters)
        val insights = analyzeProfitInsights(profitReport)
        val recommendations = generateProfitRecommendations(profitReport)

        ReportData(
            reportId = reportId,
            reportType = ReportType.PROFIT,
            title = title,
            dateRange = filters.dateRange,
            filters = filters,
            generatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            profitReport = profitReport,
            summary = summary.copy(
                keyInsights = insights,
                recommendations = recommendations
            )
        )
    }

    /**
     * Generate stock aging report with inventory analysis
     */
    private suspend fun generateStockAgingReportData(
        reportId: String,
        title: String,
        filters: ReportFilters
    ): ReportData = coroutineScope {
        val stockAgingReport = reportsRepository.generateStockAgingReport(filters)
        val chartData = generateStockAgingChartData(stockAgingReport)
        val summary = generateStockAgingReportSummary(stockAgingReport, filters)
        val insights = analyzeStockAgingInsights(stockAgingReport)
        val recommendations = generateStockAgingRecommendations(stockAgingReport)

        ReportData(
            reportId = reportId,
            reportType = ReportType.STOCK_AGING,
            title = title,
            dateRange = filters.dateRange,
            filters = filters,
            generatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            stockAgingReport = stockAgingReport,
            summary = summary.copy(
                keyInsights = insights,
                recommendations = recommendations
            )
        )
    }

    /**
     * Generate tax report with GST analysis
     */
    private suspend fun generateTaxReportData(
        reportId: String,
        title: String,
        filters: ReportFilters
    ): ReportData = coroutineScope {
        val taxReport = reportsRepository.generateTaxReport(filters)
        val chartData = generateTaxChartData(taxReport)
        val summary = generateTaxReportSummary(taxReport, filters)
        val insights = analyzeTaxInsights(taxReport)
        val recommendations = generateTaxRecommendations(taxReport)

        ReportData(
            reportId = reportId,
            reportType = ReportType.TAX,
            title = title,
            dateRange = filters.dateRange,
            filters = filters,
            generatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            taxReport = taxReport,
            summary = summary.copy(
                keyInsights = insights,
                recommendations = recommendations
            )
        )
    }

    /**
     * Generate combined report with all metrics
     */
    private suspend fun generateCombinedReportData(
        reportId: String,
        title: String,
        filters: ReportFilters
    ): ReportData = coroutineScope {
        // Generate all reports in parallel
        val salesReportDeferred = async { reportsRepository.generateSalesReport(filters) }
        val profitReportDeferred = async { reportsRepository.generateProfitReport(filters) }
        val stockAgingReportDeferred = async { reportsRepository.generateStockAgingReport(filters) }
        val taxReportDeferred = async { reportsRepository.generateTaxReport(filters) }

        val salesReport = salesReportDeferred.await()
        val profitReport = profitReportDeferred.await()
        val stockAgingReport = stockAgingReportDeferred.await()
        val taxReport = taxReportDeferred.await()

        val summary = generateCombinedReportSummary(salesReport, profitReport, stockAgingReport, taxReport, filters)
        val insights = analyzeCombinedInsights(salesReport, profitReport, stockAgingReport, taxReport)
        val recommendations = generateCombinedRecommendations(salesReport, profitReport, stockAgingReport, taxReport)

        ReportData(
            reportId = reportId,
            reportType = ReportType.COMBINED,
            title = title,
            dateRange = filters.dateRange,
            filters = filters,
            generatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            salesReport = salesReport,
            profitReport = profitReport,
            stockAgingReport = stockAgingReport,
            taxReport = taxReport,
            summary = summary.copy(
                keyInsights = insights,
                recommendations = recommendations
            )
        )
    }

    /**
     * Generate custom report based on specific requirements
     */
    private suspend fun generateCustomReportData(
        reportId: String,
        title: String,
        filters: ReportFilters
    ): ReportData = coroutineScope {
        val customMetrics = generateCustomMetrics(filters)
        val chartData = generateCustomChartData(filters)
        val tables = generateCustomTables(filters)
        val kpis = generateCustomKPIs(filters)

        val customReport = CustomReport(
            reportName = title,
            metrics = customMetrics,
            chartData = chartData,
            tables = tables,
            kpis = kpis
        )

        val summary = generateCustomReportSummary(customReport, filters)

        ReportData(
            reportId = reportId,
            reportType = ReportType.CUSTOM,
            title = title,
            dateRange = filters.dateRange,
            filters = filters,
            generatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            customReport = customReport,
            summary = summary
        )
    }

    // Chart Data Generation Methods

    /**
     * Generate comprehensive chart data for sales reports
     */
    private suspend fun generateSalesChartData(salesReport: SalesReport): List<ChartDataSet> {
        return listOf(
            // Daily sales trend
            ChartDataSet(
                chartType = ChartType.LINE,
                title = "Daily Sales Trend",
                xAxisLabel = "Date",
                yAxisLabel = "Sales Amount",
                series = listOf(
                    ChartSeries(
                        name = "Sales",
                        data = salesReport.dailySales.map { 
                            ChartDataPoint(
                                label = it.date.toString(),
                                value = it.sales.toDouble()
                            )
                        }
                    ),
                    ChartSeries(
                        name = "Transactions",
                        data = salesReport.dailySales.map { 
                            ChartDataPoint(
                                label = it.date.toString(),
                                value = it.transactions.toDouble()
                            )
                        }
                    )
                )
            ),

            // Category-wise sales pie chart
            ChartDataSet(
                chartType = ChartType.PIE,
                title = "Sales by Category",
                xAxisLabel = "",
                yAxisLabel = "",
                series = listOf(
                    ChartSeries(
                        name = "Category Sales",
                        data = salesReport.categoryWiseSales.map { 
                            ChartDataPoint(
                                label = it.category,
                                value = it.sales.toDouble()
                            )
                        }
                    )
                )
            ),

            // Brand-wise sales bar chart
            ChartDataSet(
                chartType = ChartType.BAR,
                title = "Sales by Brand",
                xAxisLabel = "Brand",
                yAxisLabel = "Sales Amount",
                series = listOf(
                    ChartSeries(
                        name = "Brand Sales",
                        data = salesReport.brandWiseSales.map { 
                            ChartDataPoint(
                                label = it.brand,
                                value = it.sales.toDouble()
                            )
                        }
                    )
                )
            ),

            // Hourly distribution heatmap
            ChartDataSet(
                chartType = ChartType.COLUMN,
                title = "Hourly Sales Distribution",
                xAxisLabel = "Hour of Day",
                yAxisLabel = "Sales Amount",
                series = listOf(
                    ChartSeries(
                        name = "Hourly Sales",
                        data = salesReport.hourlyDistribution.map { 
                            ChartDataPoint(
                                label = "${it.hour}:00",
                                value = it.sales.toDouble()
                            )
                        }
                    )
                )
            ),

            // Payment method breakdown
            ChartDataSet(
                chartType = ChartType.DONUT,
                title = "Payment Method Distribution",
                xAxisLabel = "",
                yAxisLabel = "",
                series = listOf(
                    ChartSeries(
                        name = "Payment Methods",
                        data = salesReport.paymentMethodBreakdown.map { 
                            ChartDataPoint(
                                label = it.method,
                                value = it.percentage
                            )
                        }
                    )
                )
            )
        )
    }

    /**
     * Generate comprehensive chart data for profit reports
     */
    private suspend fun generateProfitChartData(profitReport: ProfitReport): List<ChartDataSet> {
        return listOf(
            // Daily profit trend
            ChartDataSet(
                chartType = ChartType.AREA,
                title = "Daily Profit Trend",
                xAxisLabel = "Date",
                yAxisLabel = "Amount",
                series = listOf(
                    ChartSeries(
                        name = "Revenue",
                        data = profitReport.dailyProfit.map { 
                            ChartDataPoint(
                                label = it.date.toString(),
                                value = it.revenue.toDouble()
                            )
                        }
                    ),
                    ChartSeries(
                        name = "Cost",
                        data = profitReport.dailyProfit.map { 
                            ChartDataPoint(
                                label = it.date.toString(),
                                value = it.cost.toDouble()
                            )
                        }
                    ),
                    ChartSeries(
                        name = "Profit",
                        data = profitReport.dailyProfit.map { 
                            ChartDataPoint(
                                label = it.date.toString(),
                                value = it.profit.toDouble()
                            )
                        }
                    )
                )
            ),

            // Profit margin by category
            ChartDataSet(
                chartType = ChartType.BAR,
                title = "Profit Margin by Category",
                xAxisLabel = "Category",
                yAxisLabel = "Margin %",
                series = listOf(
                    ChartSeries(
                        name = "Margin",
                        data = profitReport.categoryWiseProfit.map { 
                            ChartDataPoint(
                                label = it.category,
                                value = it.margin
                            )
                        }
                    )
                )
            ),

            // Cost breakdown
            ChartDataSet(
                chartType = ChartType.PIE,
                title = "Cost Breakdown",
                xAxisLabel = "",
                yAxisLabel = "",
                series = listOf(
                    ChartSeries(
                        name = "Costs",
                        data = listOf(
                            ChartDataPoint("Direct Costs", profitReport.costBreakdown.directCosts.toDouble()),
                            ChartDataPoint("Indirect Costs", profitReport.costBreakdown.indirectCosts.toDouble()),
                            ChartDataPoint("Operational Costs", profitReport.costBreakdown.operationalCosts.toDouble())
                        )
                    )
                )
            ),

            // Margin distribution
            ChartDataSet(
                chartType = ChartType.COLUMN,
                title = "Margin Distribution",
                xAxisLabel = "Margin Range",
                yAxisLabel = "Product Count",
                series = listOf(
                    ChartSeries(
                        name = "Products",
                        data = profitReport.marginAnalysis.marginDistribution.map { 
                            ChartDataPoint(
                                label = it.range,
                                value = it.productCount.toDouble()
                            )
                        }
                    )
                )
            )
        )
    }

    /**
     * Generate chart data for stock aging reports
     */
    private suspend fun generateStockAgingChartData(stockAgingReport: StockAgingReport): List<ChartDataSet> {
        return listOf(
            // Stock aging distribution
            ChartDataSet(
                chartType = ChartType.PIE,
                title = "Stock Age Distribution",
                xAxisLabel = "",
                yAxisLabel = "",
                series = listOf(
                    ChartSeries(
                        name = "Age Categories",
                        data = stockAgingReport.agingCategories.map { 
                            ChartDataPoint(
                                label = "${it.category.name} (${it.category.minDays}-${it.category.maxDays} days)",
                                value = it.percentage
                            )
                        }
                    )
                )
            ),

            // Category aging analysis
            ChartDataSet(
                chartType = ChartType.BAR,
                title = "Average Age by Category",
                xAxisLabel = "Category",
                yAxisLabel = "Average Age (Days)",
                series = listOf(
                    ChartSeries(
                        name = "Average Age",
                        data = stockAgingReport.categoryAging.map { 
                            ChartDataPoint(
                                label = it.category,
                                value = it.averageAge
                            )
                        }
                    )
                )
            ),

            // Stock value by age
            ChartDataSet(
                chartType = ChartType.COLUMN,
                title = "Stock Value by Age Category",
                xAxisLabel = "Age Category",
                yAxisLabel = "Stock Value",
                series = listOf(
                    ChartSeries(
                        name = "Stock Value",
                        data = stockAgingReport.agingCategories.map { 
                            ChartDataPoint(
                                label = it.category.name,
                                value = it.stockValue.toDouble()
                            )
                        }
                    )
                )
            ),

            // Turnover rate by category
            ChartDataSet(
                chartType = ChartType.BAR,
                title = "Turnover Rate by Category",
                xAxisLabel = "Category",
                yAxisLabel = "Turnover Rate",
                series = listOf(
                    ChartSeries(
                        name = "Turnover Rate",
                        data = stockAgingReport.stockTurnoverAnalysis.categoryTurnover.map { 
                            ChartDataPoint(
                                label = it.category,
                                value = it.turnoverRate
                            )
                        }
                    )
                )
            )
        )
    }

    /**
     * Generate chart data for tax reports
     */
    private suspend fun generateTaxChartData(taxReport: TaxReport): List<ChartDataSet> {
        return listOf(
            // Taxable vs Non-taxable revenue
            ChartDataSet(
                chartType = ChartType.DONUT,
                title = "Taxable vs Non-Taxable Revenue",
                xAxisLabel = "",
                yAxisLabel = "",
                series = listOf(
                    ChartSeries(
                        name = "Revenue Type",
                        data = listOf(
                            ChartDataPoint("Taxable", taxReport.totalTaxableRevenue.toDouble()),
                            ChartDataPoint("Non-Taxable", taxReport.totalNonTaxableRevenue.toDouble())
                        )
                    )
                )
            ),

            // GST breakdown
            ChartDataSet(
                chartType = ChartType.PIE,
                title = "GST Component Breakdown",
                xAxisLabel = "",
                yAxisLabel = "",
                series = listOf(
                    ChartSeries(
                        name = "GST Components",
                        data = listOf(
                            ChartDataPoint("CGST", taxReport.gstBreakdown.cgst.toDouble()),
                            ChartDataPoint("SGST", taxReport.gstBreakdown.sgst.toDouble()),
                            ChartDataPoint("IGST", taxReport.gstBreakdown.igst.toDouble()),
                            ChartDataPoint("Cess", taxReport.gstBreakdown.cess.toDouble())
                        ).filter { it.value > 0 }
                    )
                )
            ),

            // Tax rate wise breakdown
            ChartDataSet(
                chartType = ChartType.BAR,
                title = "Tax Collection by Rate",
                xAxisLabel = "Tax Rate (%)",
                yAxisLabel = "Tax Amount",
                series = listOf(
                    ChartSeries(
                        name = "Tax Amount",
                        data = taxReport.taxRateWiseBreakdown.map { 
                            ChartDataPoint(
                                label = "${it.taxRate}%",
                                value = it.taxAmount.toDouble()
                            )
                        }
                    )
                )
            ),

            // Monthly tax collection trend
            ChartDataSet(
                chartType = ChartType.LINE,
                title = "Monthly Tax Collection Trend",
                xAxisLabel = "Month",
                yAxisLabel = "Tax Amount",
                series = listOf(
                    ChartSeries(
                        name = "Tax Collected",
                        data = taxReport.monthlyTaxCollection.map { 
                            ChartDataPoint(
                                label = "${it.month}/${it.year}",
                                value = it.taxAmount.toDouble()
                            )
                        }
                    )
                )
            ),

            // Interstate vs Intrastate
            ChartDataSet(
                chartType = ChartType.COLUMN,
                title = "Interstate vs Intrastate Transactions",
                xAxisLabel = "Transaction Type",
                yAxisLabel = "Amount",
                series = listOf(
                    ChartSeries(
                        name = "Revenue",
                        data = listOf(
                            ChartDataPoint("Interstate", taxReport.interstate.totalRevenue.toDouble()),
                            ChartDataPoint("Intrastate", taxReport.intrastate.totalRevenue.toDouble())
                        )
                    ),
                    ChartSeries(
                        name = "Tax",
                        data = listOf(
                            ChartDataPoint("Interstate", taxReport.interstate.totalTax.toDouble()),
                            ChartDataPoint("Intrastate", (taxReport.intrastate.totalCGST + taxReport.intrastate.totalSGST).toDouble())
                        )
                    )
                )
            )
        )
    }

    // Insight Analysis Methods

    /**
     * Analyze sales insights from data patterns
     */
    private fun analyzeSalesInsights(salesReport: SalesReport): List<String> {
        val insights = mutableListOf<String>()

        // Top performing category
        val topCategory = salesReport.categoryWiseSales.maxByOrNull { it.sales }
        topCategory?.let {
            insights.add("${it.category} is the top performing category with ₹${it.sales} in sales (${it.percentage.formatLocale("%.1f")}% of total)")
        }

        // Sales trend analysis
        when (salesReport.salesTrend.overallTrend) {
            TrendDirection.UP -> insights.add("Sales are showing an upward trend with ${salesReport.salesTrend.growthRate.formatLocale("%.1f")}% growth")
            TrendDirection.DOWN -> insights.add("Sales are declining with ${salesReport.salesTrend.growthRate.formatLocale("%.1f")}% decrease - attention needed")
            else -> insights.add("Sales are stable with consistent performance")
        }

        // Peak hours analysis
        val peakHour = salesReport.hourlyDistribution.maxByOrNull { it.sales }
        peakHour?.let {
            insights.add("Peak sales hour is ${it.hour}:00 with ₹${it.sales} in sales")
        }

        // Payment method insights
        val topPaymentMethod = salesReport.paymentMethodBreakdown.maxByOrNull { it.percentage }
        topPaymentMethod?.let {
            insights.add("${it.method} is the preferred payment method (${it.percentage.formatLocale("%.1f")}% of transactions)")
        }

        // Performance metrics insights
        if (salesReport.performance.conversionRate > 0.8) {
            insights.add("Excellent conversion rate of ${(salesReport.performance.conversionRate * 100).formatLocale("%.1f")}%")
        }

        return insights
    }

    /**
     * Analyze profit insights from margin data
     */
    private fun analyzeProfitInsights(profitReport: ProfitReport): List<String> {
        val insights = mutableListOf<String>()

        // Overall profitability
        insights.add("Overall profit margin is ${profitReport.profitMargin.formatLocale("%.1f")}%")

        // Top profitable category
        val topProfitableCategory = profitReport.categoryWiseProfit.maxByOrNull { it.margin }
        topProfitableCategory?.let {
            insights.add("${it.category} has the highest profit margin at ${it.margin.formatLocale("%.1f")}%")
        }

        // Margin trends
        when (profitReport.profitTrend.marginTrend) {
            TrendDirection.UP -> insights.add("Profit margins are improving - good cost management")
            TrendDirection.DOWN -> insights.add("Profit margins are declining - review pricing and costs")
            else -> insights.add("Profit margins are stable")
        }

        // High margin products insight
        if (profitReport.marginAnalysis.highMarginProducts.isNotEmpty()) {
            insights.add("${profitReport.marginAnalysis.highMarginProducts.size} products have above-average margins")
        }

        // Cost analysis
        val totalCosts = profitReport.costBreakdown.directCosts + profitReport.costBreakdown.indirectCosts
        val directPercentage = (profitReport.costBreakdown.directCosts / totalCosts).toDouble() * 100
        insights.add("Direct costs account for ${directPercentage.formatLocale("%.1f")}% of total costs")

        return insights
    }

    /**
     * Analyze stock aging insights
     */
    private fun analyzeStockAgingInsights(stockAgingReport: StockAgingReport): List<String> {
        val insights = mutableListOf<String>()

        // Overall aging
        insights.add("Average stock age is ${stockAgingReport.averageAge.formatLocale("%.0f")} days")

        // Dead stock analysis
        if (stockAgingReport.deadStockItems.isNotEmpty()) {
            val deadStockValue = stockAgingReport.deadStockItems.sumOf { it.stockValue }
            insights.add("${stockAgingReport.deadStockItems.size} items are dead stock worth ₹${deadStockValue}")
        }

        // Fast moving items
        if (stockAgingReport.fastMovingItems.isNotEmpty()) {
            insights.add("${stockAgingReport.fastMovingItems.size} items are fast-moving with high turnover")
        }

        // Category aging
        val oldestCategory = stockAgingReport.categoryAging.maxByOrNull { it.averageAge }
        oldestCategory?.let {
            insights.add("${it.category} has the oldest average stock age at ${it.averageAge.formatLocale("%.0f")} days")
        }

        // Reorder recommendations
        if (stockAgingReport.reorderRecommendations.isNotEmpty()) {
            val criticalReorders = stockAgingReport.reorderRecommendations.count { it.urgency == ReorderUrgency.CRITICAL }
            if (criticalReorders > 0) {
                insights.add("$criticalReorders items need immediate reordering to avoid stockouts")
            }
        }

        return insights
    }

    /**
     * Analyze tax insights
     */
    private fun analyzeTaxInsights(taxReport: TaxReport): List<String> {
        val insights = mutableListOf<String>()

        // Tax collection efficiency
        val totalRevenue = taxReport.totalTaxableRevenue + taxReport.totalNonTaxableRevenue
        val taxablePercentage = (taxReport.totalTaxableRevenue / totalRevenue).toDouble() * 100
        insights.add("${taxablePercentage.formatLocale("%.1f")}% of revenue is taxable")

        // GST collection
        insights.add("Total GST collected: ₹${taxReport.totalTaxCollected}")

        // Tax rate analysis
        val highestTaxRate = taxReport.taxRateWiseBreakdown.maxByOrNull { it.taxRate }
        highestTaxRate?.let {
            insights.add("Highest tax rate applied: ${it.taxRate}% on ₹${it.taxableAmount} revenue")
        }

        // Interstate vs Intrastate
        val interstatePercentage = (taxReport.interstate.totalRevenue / totalRevenue).toDouble() * 100
        insights.add("${interstatePercentage.formatLocale("%.1f")}% of revenue comes from interstate transactions")

        // Tax exempt items
        if (taxReport.taxExemptItems.isNotEmpty()) {
            val exemptValue = taxReport.taxExemptItems.sumOf { it.revenue }
            insights.add("Tax exempt items worth ₹$exemptValue identified")
        }

        return insights
    }

    // Recommendation Generation Methods

    /**
     * Generate sales recommendations based on analysis
     */
    private fun generateSalesRecommendations(salesReport: SalesReport): List<String> {
        val recommendations = mutableListOf<String>()

        // Category performance recommendations
        val underperformingCategories = salesReport.categoryWiseSales.filter { it.trend == TrendDirection.DOWN }
        if (underperformingCategories.isNotEmpty()) {
            recommendations.add("Focus marketing efforts on declining categories: ${underperformingCategories.joinToString(", ") { it.category }}")
        }

        // Peak hours optimization
        val lowPerformanceHours = salesReport.hourlyDistribution.filter { it.percentage < 2.0 }
        if (lowPerformanceHours.isNotEmpty()) {
            recommendations.add("Consider promotional activities during low-traffic hours (${lowPerformanceHours.joinToString(", ") { "${it.hour}:00" }})")
        }

        // Payment method optimization
        val cashPercentage = salesReport.paymentMethodBreakdown.find { it.method.contains("Cash", ignoreCase = true) }?.percentage ?: 0.0
        if (cashPercentage > 50) {
            recommendations.add("Encourage digital payments to reduce cash handling and improve efficiency")
        }

        // Customer retention
        if (salesReport.performance.customerRetention < 0.7) {
            recommendations.add("Implement customer loyalty programs to improve retention rate")
        }

        // Seasonal optimization
        if (salesReport.salesTrend.seasonality.pattern != ReportSeasonalPattern.NONE) {
            recommendations.add("Plan inventory and promotions based on seasonal sales patterns")
        }

        return recommendations
    }

    /**
     * Generate profit recommendations
     */
    private fun generateProfitRecommendations(profitReport: ProfitReport): List<String> {
        val recommendations = mutableListOf<String>()

        // Low margin products
        val lowMarginProducts = profitReport.marginAnalysis.lowMarginProducts
        if (lowMarginProducts.isNotEmpty()) {
            recommendations.add("Review pricing strategy for low-margin products: ${lowMarginProducts.take(3).joinToString(", ") { it.productName }}")
        }

        // Cost optimization
        if (profitReport.profitMargin < 20) {
            recommendations.add("Consider cost optimization strategies to improve profit margins")
        }

        // Category performance
        val lowProfitCategories = profitReport.categoryWiseProfit.filter { it.margin < 15 }
        if (lowProfitCategories.isNotEmpty()) {
            recommendations.add("Focus on improving margins in: ${lowProfitCategories.joinToString(", ") { it.category }}")
        }

        // Payment method costs
        val costlyPaymentMethods = profitReport.profitByPaymentMethod.filter { it.margin < profitReport.profitMargin - 5 }
        if (costlyPaymentMethods.isNotEmpty()) {
            recommendations.add("Review transaction costs for: ${costlyPaymentMethods.joinToString(", ") { it.method }}")
        }

        return recommendations
    }

    /**
     * Generate stock aging recommendations
     */
    private fun generateStockAgingRecommendations(stockAgingReport: StockAgingReport): List<String> {
        val recommendations = mutableListOf<String>()

        // Dead stock actions
        if (stockAgingReport.deadStockItems.isNotEmpty()) {
            recommendations.add("Consider clearance sales or return to supplier for ${stockAgingReport.deadStockItems.size} dead stock items")
        }

        // Reorder priorities
        val criticalReorders = stockAgingReport.reorderRecommendations.filter { it.urgency == ReorderUrgency.CRITICAL }
        if (criticalReorders.isNotEmpty()) {
            recommendations.add("Immediate reorder required for: ${criticalReorders.take(3).joinToString(", ") { it.productName }}")
        }

        // Slow moving items
        if (stockAgingReport.slowMovingItems.isNotEmpty()) {
            recommendations.add("Implement targeted promotions for slow-moving items to improve turnover")
        }

        // Category optimization
        val slowCategories = stockAgingReport.categoryAging.filter { it.averageAge > stockAgingReport.averageAge * 1.5 }
        if (slowCategories.isNotEmpty()) {
            recommendations.add("Review stocking levels for slow-moving categories: ${slowCategories.joinToString(", ") { it.category }}")
        }

        return recommendations
    }

    /**
     * Generate tax recommendations
     */
    private fun generateTaxRecommendations(taxReport: TaxReport): List<String> {
        val recommendations = mutableListOf<String>()

        // Tax optimization
        val highTaxItems = taxReport.taxRateWiseBreakdown.filter { it.taxRate > 18.0 }
        if (highTaxItems.isNotEmpty()) {
            recommendations.add("Review pricing strategy for high-tax items to maintain competitiveness")
        }

        // Interstate vs Intrastate optimization
        if (taxReport.interstate.transactionCount > taxReport.intrastate.transactionCount) {
            recommendations.add("Consider interstate tax implications and optimize logistics costs")
        }

        // GST compliance
        if (taxReport.totalTaxCollected > BigDecimal("500000")) {
            recommendations.add("Ensure timely GST return filing and maintain proper documentation")
        }

        return recommendations
    }

    // Summary Generation Methods

    private fun generateSalesReportSummary(salesReport: SalesReport, filters: ReportFilters): ReportSummary {
        return ReportSummary(
            totalRecords = salesReport.dailySales.size,
            totalRevenue = salesReport.totalSales,
            totalProfit = BigDecimal.ZERO, // Would be calculated if profit data available
            totalTax = BigDecimal.ZERO, // Would be calculated if tax data available
            averageOrderValue = salesReport.averageOrderValue,
            profitMargin = 0.0, // Would be calculated if profit data available
            keyInsights = emptyList(), // Will be populated by analysis methods
            recommendations = emptyList() // Will be populated by recommendation methods
        )
    }

    private fun generateProfitReportSummary(profitReport: ProfitReport, filters: ReportFilters): ReportSummary {
        return ReportSummary(
            totalRecords = profitReport.dailyProfit.size,
            totalRevenue = profitReport.totalRevenue,
            totalProfit = profitReport.totalProfit,
            totalTax = BigDecimal.ZERO,
            averageOrderValue = profitReport.totalRevenue.divide(BigDecimal(profitReport.productWiseProfit.sumOf { it.units }.coerceAtLeast(1)), 2, java.math.RoundingMode.HALF_UP),
            profitMargin = profitReport.profitMargin,
            keyInsights = emptyList(),
            recommendations = emptyList()
        )
    }

    private fun generateStockAgingReportSummary(stockAgingReport: StockAgingReport, filters: ReportFilters): ReportSummary {
        return ReportSummary(
            totalRecords = stockAgingReport.totalItems,
            totalRevenue = BigDecimal.ZERO,
            totalProfit = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            averageOrderValue = BigDecimal.ZERO,
            profitMargin = 0.0,
            keyInsights = emptyList(),
            recommendations = emptyList()
        )
    }

    private fun generateTaxReportSummary(taxReport: TaxReport, filters: ReportFilters): ReportSummary {
        val totalRevenue = taxReport.totalTaxableRevenue + taxReport.totalNonTaxableRevenue
        return ReportSummary(
            totalRecords = taxReport.taxableTransactions + taxReport.nonTaxableTransactions,
            totalRevenue = totalRevenue,
            totalProfit = BigDecimal.ZERO,
            totalTax = taxReport.totalTaxCollected,
            averageOrderValue = if (taxReport.taxableTransactions > 0) {
                taxReport.totalTaxableRevenue.divide(BigDecimal(taxReport.taxableTransactions), 2, java.math.RoundingMode.HALF_UP)
            } else BigDecimal.ZERO,
            profitMargin = 0.0,
            keyInsights = emptyList(),
            recommendations = emptyList()
        )
    }

    private suspend fun generateCombinedReportSummary(
        salesReport: SalesReport,
        profitReport: ProfitReport,
        stockAgingReport: StockAgingReport,
        taxReport: TaxReport,
        filters: ReportFilters
    ): ReportSummary {
        return ReportSummary(
            totalRecords = salesReport.dailySales.size,
            totalRevenue = salesReport.totalSales,
            totalProfit = profitReport.totalProfit,
            totalTax = taxReport.totalTaxCollected,
            averageOrderValue = salesReport.averageOrderValue,
            profitMargin = profitReport.profitMargin,
            keyInsights = emptyList(),
            recommendations = emptyList()
        )
    }

    private fun generateCustomReportSummary(customReport: CustomReport, filters: ReportFilters): ReportSummary {
        return ReportSummary(
            totalRecords = customReport.tables.sumOf { it.rows.size },
            totalRevenue = customReport.metrics.find { it.name.contains("Revenue", ignoreCase = true) }?.value ?: BigDecimal.ZERO,
            totalProfit = customReport.metrics.find { it.name.contains("Profit", ignoreCase = true) }?.value ?: BigDecimal.ZERO,
            totalTax = customReport.metrics.find { it.name.contains("Tax", ignoreCase = true) }?.value ?: BigDecimal.ZERO,
            averageOrderValue = customReport.metrics.find { it.name.contains("Average", ignoreCase = true) }?.value ?: BigDecimal.ZERO,
            profitMargin = 0.0,
            keyInsights = emptyList(),
            recommendations = emptyList()
        )
    }

    // Combined Analysis Methods

    private fun analyzeCombinedInsights(
        salesReport: SalesReport,
        profitReport: ProfitReport,
        stockAgingReport: StockAgingReport,
        taxReport: TaxReport
    ): List<String> {
        val insights = mutableListOf<String>()
        insights.addAll(analyzeSalesInsights(salesReport).take(2))
        insights.addAll(analyzeProfitInsights(profitReport).take(2))
        insights.addAll(analyzeStockAgingInsights(stockAgingReport).take(2))
        insights.addAll(analyzeTaxInsights(taxReport).take(2))
        return insights
    }

    private fun generateCombinedRecommendations(
        salesReport: SalesReport,
        profitReport: ProfitReport,
        stockAgingReport: StockAgingReport,
        taxReport: TaxReport
    ): List<String> {
        val recommendations = mutableListOf<String>()
        recommendations.addAll(generateSalesRecommendations(salesReport).take(2))
        recommendations.addAll(generateProfitRecommendations(profitReport).take(2))
        recommendations.addAll(generateStockAgingRecommendations(stockAgingReport).take(2))
        recommendations.addAll(generateTaxRecommendations(taxReport).take(1))
        return recommendations
    }

    // Custom Report Methods

    private suspend fun generateCustomMetrics(filters: ReportFilters): List<CustomMetric> {
        // This would be implemented based on specific custom requirements
        return emptyList()
    }

    private suspend fun generateCustomChartData(filters: ReportFilters): List<ChartDataSet> {
        // This would be implemented based on specific custom requirements
        return emptyList()
    }

    private suspend fun generateCustomTables(filters: ReportFilters): List<TableData> {
        // This would be implemented based on specific custom requirements
        return emptyList()
    }

    private suspend fun generateCustomKPIs(filters: ReportFilters): List<KPIData> {
        // This would be implemented based on specific custom requirements
        return emptyList()
    }

    // Utility Methods

    private fun generateReportTitle(reportType: ReportType, filters: ReportFilters): String {
        val period = "${filters.dateRange.startDate} to ${filters.dateRange.endDate}"
        return when (reportType) {
            ReportType.SALES -> "Sales Report ($period)"
            ReportType.PROFIT -> "Profit Analysis Report ($period)"
            ReportType.STOCK_AGING -> "Stock Aging Report"
            ReportType.TAX -> "Tax Report ($period)"
            ReportType.COMBINED -> "Comprehensive Business Report ($period)"
            ReportType.CUSTOM -> "Custom Report ($period)"
            else -> "Business Report ($period)"
        }
    }
}