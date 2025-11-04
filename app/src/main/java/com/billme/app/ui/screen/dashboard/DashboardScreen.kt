package com.billme.app.ui.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToQuickBill: () -> Unit,
    onNavigateToBilling: () -> Unit = {},
    onNavigateToInventory: () -> Unit,
    onNavigateToAddPurchase: () -> Unit,
    onNavigateToAddProduct: () -> Unit = {},
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInvoiceHistory: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = uiState.shopName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mobile Shop Management",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    AnimatedIconButton(
                        icon = Icons.Default.Search,
                        onClick = onNavigateToInventory,
                        contentDescription = "Search Products"
                    )
                    AnimatedIconButton(
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings,
                        contentDescription = "Settings"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToQuickBill,
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = Elevations.FAB,
                    pressedElevation = Elevations.FABPressed,
                    hoveredElevation = Elevations.FABHovered
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Bill",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                GradientLoadingIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Enhanced Today's Sales Stats with Gradient
                item {
                    EnhancedStatsCard(
                        title = "Today's Sales",
                        value = "${uiState.currencySymbol}${formatCurrency(uiState.dailySales)}",
                        subtitle = "${uiState.dailyTransactions} transactions • Profit: ${uiState.currencySymbol}${formatCurrency(uiState.dailyProfit)}",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        gradient = AppGradients.PrimaryGradient
                    )
                }
                
                // Glassmorphic Quick Actions Grid
                item {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                item {
                    ActionButtonsGrid(
                        onQuickBill = onNavigateToQuickBill,
                        onBilling = onNavigateToBilling,
                        onAddProduct = onNavigateToAddProduct,
                        onInventory = onNavigateToInventory,
                        onReports = onNavigateToReports,
                        onInvoiceHistory = onNavigateToInvoiceHistory,
                        onBackup = onNavigateToBackup
                    )
                }
                
                // Recent Transactions
                item {
                    SectionHeader(
                        title = "Recent Transactions",
                        actionText = "View All →",
                        onActionClick = onNavigateToInvoiceHistory
                    )
                }
                
                items(uiState.recentTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        currencySymbol = uiState.currencySymbol
                    )
                }
                
                // Low Stock Alerts
                if (uiState.lowStockCount > 0) {
                    item {
                        SectionHeader(
                            title = "Low Stock Alerts (${uiState.lowStockCount})",
                            actionText = "Manage →",
                            onActionClick = onNavigateToInventory
                        )
                    }
                    
                    items(uiState.lowStockProducts) { product ->
                        LowStockItem(
                            product = product,
                            currencySymbol = uiState.currencySymbol
                        )
                    }
                }
            }
        }
    }
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            viewModel.clearError()
        }
    }
}

@Composable
private fun ActionButtonsGrid(
    onQuickBill: () -> Unit,
    onBilling: () -> Unit,
    onAddProduct: () -> Unit,
    onInventory: () -> Unit,
    onReports: () -> Unit,
    onInvoiceHistory: () -> Unit,
    onBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionTile(
                title = "Quick Bill",
                icon = Icons.Default.PhoneAndroid,
                onClick = onQuickBill,
                modifier = Modifier.weight(1f),
                iconColor = Primary
            )
            QuickActionTile(
                title = "Create Bill",
                icon = Icons.Default.Receipt,
                onClick = onBilling,
                modifier = Modifier.weight(1f),
                iconColor = Secondary
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionTile(
                title = "Add Product",
                icon = Icons.Default.AddBox,
                onClick = onAddProduct,
                modifier = Modifier.weight(1f),
                iconColor = Tertiary
            )
            QuickActionTile(
                title = "Inventory",
                icon = Icons.Default.Inventory,
                onClick = onInventory,
                modifier = Modifier.weight(1f),
                iconColor = Info
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionTile(
                title = "Reports",
                icon = Icons.Default.Analytics,
                onClick = onReports,
                modifier = Modifier.weight(1f),
                iconColor = Warning
            )
            QuickActionTile(
                title = "Invoices",
                icon = Icons.Default.History,
                onClick = onInvoiceHistory,
                modifier = Modifier.weight(1f),
                iconColor = ChartPurple
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionTile(
                title = "Backup",
                icon = Icons.Default.Backup,
                onClick = onBackup,
                modifier = Modifier.weight(1f),
                iconColor = Success
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: com.billme.app.data.local.entity.Transaction,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevations.Card),
        shape = CustomShapes.cardMedium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.transactionNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatDateTime(transaction.transactionDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    transaction.customerName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$currencySymbol${formatCurrency(transaction.grandTotal)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LowStockItem(
    product: com.billme.app.data.local.entity.Product,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevations.Card),
        shape = CustomShapes.cardMedium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.currentStock} units remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatCurrency(amount: java.math.BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    return formatter.format(amount)
}

private fun formatDateTime(instant: kotlinx.datetime.Instant): String {
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return dateFormat.format(Date(instant.toEpochMilliseconds()))
}
