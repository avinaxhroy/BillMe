package com.billme.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.Transaction
import com.billme.app.data.repository.ProductRepository
import com.billme.app.data.repository.SettingsRepository
import com.billme.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load today's summary
                val dailySummary = transactionRepository.getDailySummary()
                
                // Load yesterday's summary for comparison
                val yesterdaySummary = transactionRepository.getYesterdaySummary()
                
                // Calculate percentage change
                val percentageChange = calculatePercentageChange(
                    dailySummary.sales.toDouble(),
                    yesterdaySummary.sales.toDouble()
                )
                
                // Get sales data for the last 7 days for the chart
                val last7DaysSales = transactionRepository.getLast7DaysSales()
                
                val shopName = settingsRepository.getShopName()
                val currencySymbol = settingsRepository.getCurrencySymbol()
                
                // Get low stock count
                val lowStockCount = productRepository.getLowStockCount()
                
                // Combine flows for real-time updates
                combine(
                    transactionRepository.getRecentTransactions(5),
                    productRepository.getLowStockProducts()
                ) { recentTransactions, lowStockProducts ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        shopName = shopName,
                        currencySymbol = currencySymbol,
                        dailySales = dailySummary.sales,
                        dailyProfit = dailySummary.profit,
                        dailyTransactions = dailySummary.transactionCount,
                        yesterdaySales = yesterdaySummary.sales,
                        salesPercentageChange = percentageChange,
                        last7DaysSales = last7DaysSales,
                        recentTransactions = recentTransactions,
                        lowStockProducts = lowStockProducts.take(5), // Show up to 5
                        lowStockCount = lowStockCount,
                        error = null
                    )
                }.collect { /* Flow is handled above */ }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    private fun calculatePercentageChange(current: Double, previous: Double): Double {
        if (previous == 0.0) {
            return if (current > 0.0) 100.0 else 0.0
        }
        return ((current - previous) / previous) * 100.0
    }
    
    fun refreshData() {
        loadDashboardData()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val shopName: String = "Mobile Shop Pro",
    val currencySymbol: String = "₹",
    val dailySales: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val yesterdaySales: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val dailyProfit: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val dailyTransactions: Int = 0,
    val salesPercentageChange: Double = 0.0,
    val last7DaysSales: List<Double> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val lowStockCount: Int = 0,
    val error: String? = null
)