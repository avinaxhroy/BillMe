package com.billme.app.ui.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billme.app.ui.component.*
import com.billme.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
                    Text(
                        text = uiState.shopName.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
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
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Modern Today's Sales Card with Chart
                item {
                    ModernSalesCard(
                        title = "Today's Sales",
                        value = "${uiState.currencySymbol}${formatCurrency(uiState.dailySales)}",
                        percentage = formatPercentage(uiState.salesPercentageChange),
                        isPositive = uiState.salesPercentageChange >= 0,
                        transactions = uiState.dailyTransactions,
                        salesData = uiState.last7DaysSales
                    )
                }
                
                // Quick Actions Section Header
                item {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // Quick Actions Grid - Inspired by Image
                item {
                    ModernActionButtonsGrid(
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
                if (uiState.recentTransactions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = onNavigateToInvoiceHistory) {
                                Text(
                                    text = "View All",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    items(uiState.recentTransactions.take(5)) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            currencySymbol = uiState.currencySymbol
                        )
                    }
                }
                
                // Low Stock Alerts
                if (uiState.lowStockCount > 0) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    item {
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
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Low Stock Alerts",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "${uiState.lowStockCount}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    
                    items(uiState.lowStockProducts.take(5)) { product ->
                        LowStockItem(
                            product = product,
                            currencySymbol = uiState.currencySymbol
                        )
                    }
                    
                    item {
                        TextButton(
                            onClick = onNavigateToInventory,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "View All Low Stock Items",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
private fun ModernActionButtonsGrid(
    onQuickBill: () -> Unit,
    onBilling: () -> Unit,
    onAddProduct: () -> Unit,
    onInventory: () -> Unit,
    onReports: () -> Unit,
    onInvoiceHistory: () -> Unit,
    onBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Row 1: Quick Bill & Create Bill
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ModernActionCard(
                title = "Quick Bill",
                icon = Icons.Default.PhoneAndroid,
                onClick = onQuickBill,
                backgroundColor = Color(0xFFE1D4F5),
                iconColor = Color(0xFF7B2CBF),
                modifier = Modifier.weight(1f)
            )
            ModernActionCard(
                title = "Create Bill",
                icon = Icons.Default.Receipt,
                onClick = onBilling,
                backgroundColor = Color(0xFFD1F4E0),
                iconColor = Color(0xFF2D9560),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 2: Add Product & Inventory
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ModernActionCard(
                title = "Add Product",
                icon = Icons.Default.AddShoppingCart,
                onClick = onAddProduct,
                backgroundColor = Color(0xFFCCE7F5),
                iconColor = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )
            ModernActionCard(
                title = "Inventory",
                icon = Icons.Default.Inventory2,
                onClick = onInventory,
                backgroundColor = Color(0xFFCCE7F5),
                iconColor = Color(0xFF0277BD),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 3: Reports & Invoices
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ModernActionCard(
                title = "Reports",
                icon = Icons.Default.BarChart,
                onClick = onReports,
                backgroundColor = Color(0xFFFFF3E0),
                iconColor = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
            ModernActionCard(
                title = "Invoices",
                icon = Icons.Default.Description,
                onClick = onInvoiceHistory,
                backgroundColor = Color(0xFFF8E1F4),
                iconColor = Color(0xFFC2185B),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 4: Backup & (future action)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ModernActionCard(
                title = "Backup",
                icon = Icons.Default.Backup,
                onClick = onBackup,
                backgroundColor = Color(0xFFE8F5E9),
                iconColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            // Empty space for symmetry
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModernActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    val coroutineScope = rememberCoroutineScope()
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
                // Reset after a delay
                coroutineScope.launch {
                    kotlinx.coroutines.delay(100)
                    isPressed = false
                }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon in a rounded container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Title at the bottom
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2C2C2C), // Dark color for visibility on all backgrounds
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModernSalesCard(
    title: String,
    value: String,
    percentage: String,
    isPositive: Boolean,
    transactions: Int,
    salesData: List<Double> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Value and Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Percentage Badge - Only show if not zero
                if (percentage.isNotEmpty() && percentage != "0.0%") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE53935),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = percentage,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFE53935)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Simple Chart Representation with real data
            SimpleTrendChart(
                salesData = salesData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
    }
}

@Composable
private fun SimpleTrendChart(
    salesData: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Use real sales data or fallback to demo data
    val points = if (salesData.isNotEmpty() && salesData.size >= 2) {
        // Normalize the data to 0-1 range for display
        val maxSales = salesData.maxOrNull() ?: 1.0
        val minSales = salesData.minOrNull() ?: 0.0
        val range = maxSales - minSales
        
        if (range > 0) {
            salesData.map { sale ->
                ((sale - minSales) / range * 0.7f + 0.15f).toFloat() // Scale to 0.15-0.85 range
            }
        } else {
            // All values are the same
            List(salesData.size) { 0.5f }
        }
    } else {
        // Demo data if no real data available
        listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.6f, 0.8f, 0.7f)
    }
    
    // Line chart using Canvas
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        
        val path = Path()
        val width = size.width
        val height = size.height
        
        // Create smooth curve
        val stepX = if (points.size > 1) width / (points.size - 1) else width
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Draw filled area under curve
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF4CAF50).copy(alpha = 0.3f),
                    Color(0xFF4CAF50).copy(alpha = 0.05f)
                )
            )
        )
        
        // Draw line
        drawPath(
            path = path,
            color = Color(0xFF4CAF50),
            style = Stroke(width = 4f)
        )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
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
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    transaction.customerName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = transaction.transactionNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatDateTime(transaction.transactionDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$currencySymbol${formatCurrency(transaction.grandTotal)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
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
            containerColor = Color(0xFFFFF8F8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
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
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${product.currentStock} units",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            text = "• Low Stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
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

private fun formatPercentage(percentage: Double): String {
    val sign = if (percentage >= 0) "+" else ""
    return "$sign%.1f%%".format(percentage)
}
