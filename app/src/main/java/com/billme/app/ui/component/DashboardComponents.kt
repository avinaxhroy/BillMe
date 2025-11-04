package com.billme.app.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.billme.app.core.util.formatCurrency
import com.billme.app.core.util.formatCompact
import com.billme.app.core.util.formatPercentage
import com.billme.app.data.model.*
import com.billme.app.ui.viewmodel.DashboardViewMode
import java.math.BigDecimal

/**
 * Main Dashboard Screen Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardData: DashboardData?,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onDateRangeChange: (DateRange) -> Unit,
    onViewModeChange: (DashboardViewMode) -> Unit,
    currentViewMode: DashboardViewMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header with filters and refresh
        DashboardHeader(
            onRefresh = onRefresh,
            onDateRangeChange = onDateRangeChange,
            currentViewMode = currentViewMode,
            onViewModeChange = onViewModeChange,
            isRefreshing = isLoading
        )
        
        when {
            isLoading && dashboardData == null -> {
                LoadingState()
            }
            error != null -> {
                ErrorState(
                    error = error,
                    onRetry = onRefresh
                )
            }
            dashboardData != null -> {
                DashboardContent(
                    dashboardData = dashboardData,
                    viewMode = currentViewMode
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    onRefresh: () -> Unit,
    onDateRangeChange: (DateRange) -> Unit,
    currentViewMode: DashboardViewMode,
    onViewModeChange: (DashboardViewMode) -> Unit,
    isRefreshing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title and refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Business Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Overview of your business",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // View mode tabs
            ViewModeSelector(
                currentMode = currentViewMode,
                onModeChange = onViewModeChange
            )
        }
    }
}

@Composable
private fun ViewModeSelector(
    currentMode: DashboardViewMode,
    onModeChange: (DashboardViewMode) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DashboardViewMode.values()) { mode ->
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                label = {
                    Text(
                        text = when (mode) {
                            DashboardViewMode.OVERVIEW -> "Overview"
                            DashboardViewMode.SALES -> "Sales"
                            DashboardViewMode.INVENTORY -> "Inventory"
                            DashboardViewMode.CUSTOMERS -> "Customers"
                            DashboardViewMode.PROFITS -> "Profits"
                            DashboardViewMode.ANALYTICS -> "Analytics"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        when (mode) {
                            DashboardViewMode.OVERVIEW -> Icons.Default.Dashboard
                            DashboardViewMode.SALES -> Icons.AutoMirrored.Filled.TrendingUp
                            DashboardViewMode.INVENTORY -> Icons.Default.Inventory
                            DashboardViewMode.CUSTOMERS -> Icons.Default.People
                            DashboardViewMode.PROFITS -> Icons.Default.AttachMoney
                            DashboardViewMode.ANALYTICS -> Icons.Default.Analytics
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
private fun DashboardContent(
    dashboardData: DashboardData,
    viewMode: DashboardViewMode
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (viewMode) {
            DashboardViewMode.OVERVIEW -> {
                // Business Metrics Cards
                item {
                    BusinessMetricsSection(dashboardData.businessMetrics)
                }
                
                // Sales Chart
                item {
                    SalesChartSection(dashboardData.salesAnalytics)
                }
                
                // Top Selling Models
                item {
                    TopSellingModelsSection(dashboardData.topSellingModels)
                }
                
                // Stock Alerts
                item {
                    StockAlertsSection(dashboardData.stockAlerts)
                }
                
                // Recent Transactions
                item {
                    RecentTransactionsSection(dashboardData.recentTransactions)
                }
            }
            
            DashboardViewMode.SALES -> {
                item {
                    SalesAnalyticsSection(dashboardData.salesAnalytics)
                }
                item {
                    TopSellingModelsSection(dashboardData.topSellingModels)
                }
            }
            
            DashboardViewMode.INVENTORY -> {
                item {
                    InventoryMetricsSection(dashboardData.inventoryMetrics)
                }
                item {
                    StockAlertsSection(dashboardData.stockAlerts)
                }
            }
            
            DashboardViewMode.CUSTOMERS -> {
                item {
                    // Customer analytics would go here
                    Text("Customer Analytics - Coming Soon")
                }
            }
            
            DashboardViewMode.PROFITS -> {
                item {
                    // Profit analysis would go here
                    Text("Profit Analysis - Coming Soon")
                }
            }
            
            DashboardViewMode.ANALYTICS -> {
                item {
                    // Advanced analytics would go here
                    Text("Advanced Analytics - Coming Soon")
                }
            }
        }
        
        // GST Summary (always show if available)
        if (dashboardData.gstSummary.todayGSTCollected > BigDecimal.ZERO || 
            dashboardData.gstSummary.monthlyGSTCollected > BigDecimal.ZERO) {
            item {
                GSTSummarySection(dashboardData.gstSummary)
            }
        }
    }
}

@Composable
private fun BusinessMetricsSection(businessMetrics: BusinessMetrics) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Today's Performance",
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        
        // Today's metrics
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MetricCard(
                    title = "Sales",
                    value = businessMetrics.todaysSales.formatCurrency(),
                    icon = Icons.Default.AttachMoney,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                MetricCard(
                    title = "Profit",
                    value = businessMetrics.todaysProfit.formatCurrency(),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                MetricCard(
                    title = "Orders",
                    value = businessMetrics.todaysTransactions.toString(),
                    icon = Icons.Default.ShoppingCart,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            item {
                MetricCard(
                    title = "AOV",
                    value = businessMetrics.averageOrderValue.formatCurrency(),
                    icon = Icons.Default.Receipt,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Monthly comparison
        SectionHeader(
            title = "This Month",
            icon = Icons.Default.CalendarMonth
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ComparisonCard(
                    title = "Monthly Sales",
                    value = businessMetrics.monthlySales.formatCurrency(),
                    subtitle = "${businessMetrics.monthlyTransactions} orders"
                )
            }
            item {
                ComparisonCard(
                    title = "Monthly Profit",
                    value = businessMetrics.monthlyProfit.formatCurrency(),
                    subtitle = "Profit margin"
                )
            }
            item {
                AlertCard(
                    title = "Stock Alerts",
                    criticalCount = businessMetrics.outOfStockItems,
                    warningCount = businessMetrics.lowStockItems
                )
            }
        }
    }
}

@Composable
private fun SalesChartSection(salesAnalytics: SalesAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Sales Trend",
                icon = Icons.AutoMirrored.Filled.ShowChart
            )
            
            // Trend indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendIndicator(salesAnalytics.salesTrend)
                Text(
                    text = salesAnalytics.salesTrend.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Simple chart placeholder (would use actual charting library)
            SalesChartPlaceholder(salesAnalytics.dailySales)
        }
    }
}

@Composable
private fun TopSellingModelsSection(topModels: List<TopSellingModel>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Top Selling Models",
                icon = Icons.Default.Star
            )
            
            topModels.take(5).forEach { model ->
                TopSellingModelItem(model)
                if (model != topModels.take(5).last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StockAlertsSection(alerts: List<StockAlert>) {
    if (alerts.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Stock Alerts",
                icon = Icons.Default.Warning,
                actionText = "View All"
            )
            
            alerts.take(3).forEach { alert ->
                StockAlertItem(alert)
            }
            
            if (alerts.size > 3) {
                Text(
                    text = "and ${alerts.size - 3} more alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(transactions: List<RecentTransaction>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Recent Transactions",
                icon = Icons.Default.Receipt,
                actionText = "View All"
            )
            
            transactions.take(5).forEach { transaction ->
                RecentTransactionItem(transaction)
                if (transaction != transactions.take(5).last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun GSTSummarySection(gstSummary: GSTSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "GST Summary",
                icon = Icons.Default.Receipt
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GSTMetricItem(
                    label = "Today's GST",
                    amount = gstSummary.todayGSTCollected,
                    modifier = Modifier.weight(1f)
                )
                GSTMetricItem(
                    label = "Monthly GST",
                    amount = gstSummary.monthlyGSTCollected,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // GST breakdown
            if (gstSummary.cgstAmount > BigDecimal.ZERO || 
                gstSummary.sgstAmount > BigDecimal.ZERO ||
                gstSummary.igstAmount > BigDecimal.ZERO) {
                
                HorizontalDivider()
                
                Text(
                    text = "GST Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (gstSummary.cgstAmount > BigDecimal.ZERO) {
                        GSTComponentItem("CGST", gstSummary.cgstAmount, Modifier.weight(1f))
                    }
                    if (gstSummary.sgstAmount > BigDecimal.ZERO) {
                        GSTComponentItem("SGST", gstSummary.sgstAmount, Modifier.weight(1f))
                    }
                    if (gstSummary.igstAmount > BigDecimal.ZERO) {
                        GSTComponentItem("IGST", gstSummary.igstAmount, Modifier.weight(1f))
                    }
                    if (gstSummary.cessAmount > BigDecimal.ZERO) {
                        GSTComponentItem("Cess", gstSummary.cessAmount, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Helper Components

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionText)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlertCard(
    title: String,
    criticalCount: Int,
    warningCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (criticalCount > 0) {
                MaterialTheme.colorScheme.errorContainer
            } else if (warningCount > 0) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                if (criticalCount > 0) Icons.Default.Error else Icons.Default.Warning,
                contentDescription = null,
                tint = if (criticalCount > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
            if (criticalCount > 0) {
                Text(
                    text = "$criticalCount critical",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (warningCount > 0) {
                Text(
                    text = "$warningCount warnings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun TrendIndicator(trend: SalesTrend) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            when (trend.direction) {
                TrendDirection.UP -> Icons.AutoMirrored.Filled.TrendingUp
                TrendDirection.DOWN -> Icons.AutoMirrored.Filled.TrendingDown
                else -> Icons.AutoMirrored.Filled.TrendingFlat
            },
            contentDescription = null,
            tint = when (trend.direction) {
                TrendDirection.UP -> Color(0xFF4CAF50)
                TrendDirection.DOWN -> Color(0xFFFF5722)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = "${if (trend.percentage >= 0) "+" else ""}${trend.percentage.formatPercentage()}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = when (trend.direction) {
                TrendDirection.UP -> Color(0xFF4CAF50)
                TrendDirection.DOWN -> Color(0xFFFF5722)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun TopSellingModelItem(model: TopSellingModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = model.rank.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${model.brand} • ${model.unitsSold} sold",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = model.revenue.formatCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TrendIndicator(
                SalesTrend(model.salesTrend, 0.0, "", "")
            )
        }
    }
}

@Composable
private fun StockAlertItem(alert: StockAlert) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                when (alert.severity) {
                    AlertSeverity.CRITICAL -> Icons.Default.Error
                    AlertSeverity.HIGH -> Icons.Default.Warning
                    AlertSeverity.MEDIUM -> Icons.Default.Info
                    AlertSeverity.LOW -> Icons.Default.NotificationsNone
                },
                contentDescription = null,
                tint = when (alert.severity) {
                    AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                    AlertSeverity.HIGH -> Color(0xFFFF9800)
                    AlertSeverity.MEDIUM -> MaterialTheme.colorScheme.primary
                    AlertSeverity.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${alert.currentStock} in stock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Badge(
            containerColor = when (alert.alertType) {
                StockAlertType.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                StockAlertType.LOW_STOCK -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.secondary
            }
        ) {
            Text(
                text = when (alert.alertType) {
                    StockAlertType.OUT_OF_STOCK -> "OUT"
                    StockAlertType.LOW_STOCK -> "LOW"
                    StockAlertType.OVERSTOCK -> "HIGH"
                    StockAlertType.DEAD_STOCK -> "DEAD"
                    StockAlertType.EXPIRING -> "EXP"
                }
            )
        }
    }
}

@Composable
private fun RecentTransactionItem(transaction: RecentTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.customerName ?: transaction.customerPhone ?: "Walk-in Customer",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${transaction.itemCount} items • ${transaction.paymentMethod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = transaction.amount.formatCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Profit: ${transaction.profit.formatCurrency()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun GSTMetricItem(
    label: String,
    amount: BigDecimal,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = amount.formatCurrency(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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
private fun GSTComponentItem(
    component: String,
    amount: BigDecimal,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = amount.formatCurrency(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = component,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SalesChartPlaceholder(dailySales: List<DashboardDailySalesData>) {
    // Simple bar chart representation
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (dailySales.isEmpty()) {
                Text(
                    text = "No sales data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Simple chart representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailySales.takeLast(7).forEach { sale ->
                        val maxSales = dailySales.maxOfOrNull { it.sales } ?: BigDecimal.ONE
                        val height = if (maxSales > BigDecimal.ZERO) {
                            ((sale.sales / maxSales).toFloat() * 100).coerceAtLeast(10f)
                        } else 10f
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(height.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Text(
                                text = sale.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// Additional sections for different view modes
@Composable
private fun SalesAnalyticsSection(salesAnalytics: SalesAnalytics) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SalesChartSection(salesAnalytics)
        
        // Category breakdown
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Sales by Category",
                    icon = Icons.Default.Category
                )
                
                salesAnalytics.salesByCategory.forEach { category ->
                    CategorySalesItem(category)
                }
            }
        }
    }
}

@Composable
private fun InventoryMetricsSection(inventoryMetrics: InventoryMetrics) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stock value overview
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Stock Valuation",
                    icon = Icons.Default.Inventory2
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StockMetricItem(
                        "Total Value",
                        inventoryMetrics.totalStockValue.formatCurrency(),
                        Modifier.weight(1f)
                    )
                    StockMetricItem(
                        "Total Items",
                        inventoryMetrics.totalStockQuantity.toString(),
                        Modifier.weight(1f)
                    )
                    StockMetricItem(
                        "Turnover Rate",
                        "${inventoryMetrics.stockTurnoverRate}x",
                        Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Category-wise stock
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Stock by Category",
                    icon = Icons.Default.Category
                )
                
                inventoryMetrics.categoryWiseStock.forEach { category ->
                    CategoryStockItem(category)
                }
            }
        }
    }
}

@Composable
private fun CategorySalesItem(category: CategorySalesData) {
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
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = category.sales.formatCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TrendIndicator(
                SalesTrend(category.trend, 0.0, "", "")
            )
        }
    }
}

@Composable
private fun StockMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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
private fun CategoryStockItem(category: CategoryStockData) {
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
                text = "${category.totalQuantity} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = category.totalValue.formatCurrency(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (category.lowStockItems > 0 || category.outOfStockItems > 0) {
                Text(
                    text = "${category.lowStockItems + category.outOfStockItems} alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading dashboard...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = "Failed to load dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}