package com.billme.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.service.AnalyticsService
import com.billme.app.core.service.*
import com.billme.app.data.model.*
import com.billme.app.data.repository.BusinessMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel for dashboard with real-time data management and analytics
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val businessMetricsRepository: BusinessMetricsRepository,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _refreshState = MutableStateFlow(
        DashboardRefreshState(
            isRefreshing = false,
            lastRefresh = null,
            autoRefreshEnabled = true,
            refreshIntervalMinutes = 5
        )
    )
    val refreshState: StateFlow<DashboardRefreshState> = _refreshState.asStateFlow()

    private val _selectedDateRange = MutableStateFlow(getDefaultDateRange())
    val selectedDateRange: StateFlow<DateRange> = _selectedDateRange.asStateFlow()

    private val _selectedFilters = MutableStateFlow(DashboardFilter(getDefaultDateRange()))
    val selectedFilters: StateFlow<DashboardFilter> = _selectedFilters.asStateFlow()

    init {
        // Load initial dashboard data
        loadDashboardData()
        
        // Set up automatic refresh
        setupAutoRefresh()
        
        // Observe date range changes to reload data
        viewModelScope.launch {
            selectedDateRange
                .collect {
                    loadDashboardData()
                }
        }
    }

    /**
     * Load comprehensive dashboard data
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                _refreshState.value = _refreshState.value.copy(isRefreshing = true, error = null)

                val dateRange = _selectedDateRange.value
                val dashboardData = analyticsService.generateDashboardAnalytics(
                    dateRange = dateRange,
                    includeProjections = true
                )

                _uiState.value = DashboardUiState(
                    dashboardData = dashboardData,
                    isLoading = false,
                    error = null,
                    lastUpdated = Clock.System.now()
                )

                _refreshState.value = _refreshState.value.copy(
                    isRefreshing = false,
                    lastRefresh = Clock.System.now(),
                    error = null
                )

            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to load dashboard data"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                _refreshState.value = _refreshState.value.copy(
                    isRefreshing = false,
                    error = errorMessage
                )
            }
        }
    }

    /**
     * Get real-time business metrics
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBusinessMetricsFlow(): Flow<BusinessMetrics> = 
        refreshState
            .filter { !it.isRefreshing }
            .flatMapLatest { businessMetricsRepository.getBusinessMetrics() }
            .catch { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }

    /**
     * Get recent transactions flow
     */
    fun getRecentTransactionsFlow(): Flow<List<RecentTransaction>> = 
        businessMetricsRepository.getRecentTransactions(10)
            .catch { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }

    /**
     * Refresh dashboard data manually
     */
    fun refresh() {
        loadDashboardData()
    }

    /**
     * Change date range filter
     */
    fun changeDateRange(dateRange: DateRange) {
        _selectedDateRange.value = dateRange
        _selectedFilters.value = _selectedFilters.value.copy(dateRange = dateRange)
    }

    /**
     * Apply date range preset
     */
    fun applyDateRangePreset(preset: DateRangePreset) {
        val dateRange = createDateRangeFromPreset(preset)
        changeDateRange(dateRange)
    }

    /**
     * Update dashboard filters
     */
    fun updateFilters(filters: DashboardFilter) {
        _selectedFilters.value = filters
        if (filters.dateRange != _selectedDateRange.value) {
            _selectedDateRange.value = filters.dateRange
        } else {
            // Reload with new filters
            loadDashboardData()
        }
    }

    /**
     * Toggle auto-refresh
     */
    fun toggleAutoRefresh() {
        val currentState = _refreshState.value
        _refreshState.value = currentState.copy(
            autoRefreshEnabled = !currentState.autoRefreshEnabled
        )
        
        if (currentState.autoRefreshEnabled) {
            setupAutoRefresh()
        }
    }

    /**
     * Update auto-refresh interval
     */
    fun updateRefreshInterval(minutes: Int) {
        _refreshState.value = _refreshState.value.copy(
            refreshIntervalMinutes = minutes
        )
        
        if (_refreshState.value.autoRefreshEnabled) {
            setupAutoRefresh()
        }
    }

    /**
     * Get top-selling models with enhanced analytics
     */
    fun getTopSellingModels(limit: Int = 20): Flow<List<TopSellingModel>> = flow {
        try {
            val models = analyticsService.getEnhancedTopSellingModels(
                dateRange = _selectedDateRange.value,
                limit = limit,
                includeProjections = true
            )
            emit(models)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            emit(emptyList())
        }
    }

    /**
     * Get stock alerts
     */
    fun getStockAlerts(): Flow<List<StockAlert>> = flow {
        try {
            val alerts = businessMetricsRepository.getStockAlerts()
            emit(alerts)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            emit(emptyList())
        }
    }

    /**
     * Perform ABC analysis
     */
    fun performABCAnalysis(): Flow<ABCAnalysisResult> = flow {
        try {
            val analysis = analyticsService.performABCAnalysis()
            emit(analysis)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            emit(ABCAnalysisResult(emptyList(), emptyList(), emptyList(), 0, Clock.System.todayIn(TimeZone.currentSystemDefault())))
        }
    }

    /**
     * Get advanced stock valuation
     */
    fun getAdvancedStockValuation(): Flow<AdvancedStockValuation> = flow {
        try {
            val valuation = analyticsService.calculateAdvancedStockValuation()
            emit(valuation)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    /**
     * Get customer lifetime value analysis
     */
    fun getCustomerLTVAnalysis(): Flow<CustomerLTVAnalysis> = flow {
        try {
            val analysis = analyticsService.analyzeCustomerLifetimeValue()
            emit(analysis)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    /**
     * Get profit analysis
     */
    fun getProfitAnalysis(): Flow<ProfitAnalysis> = flow {
        try {
            val analysis = analyticsService.analyzeProfitability(_selectedDateRange.value)
            emit(analysis)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    /**
     * Get sales projections
     */
    fun getSalesProjections(daysAhead: Int = 30): Flow<SalesProjection> = flow {
        try {
            val projections = analyticsService.predictSales(daysAhead)
            emit(projections)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    /**
     * Get performance comparison
     */
    fun getPerformanceComparison(): Flow<PerformanceComparison> = flow {
        try {
            val currentRange = _selectedDateRange.value
            val previousRange = getPreviousDateRange(currentRange)
            
            val currentMetrics = businessMetricsRepository.getBusinessMetrics().first()
            // Get previous period metrics (implementation would need historical data)
            
            // For now, create a placeholder comparison
            val currentPeriod = PerformanceData(
                periodName = "Current Period",
                sales = currentMetrics.monthlySales,
                profit = currentMetrics.monthlyProfit,
                transactions = currentMetrics.monthlyTransactions,
                customers = currentMetrics.activeCustomers,
                averageOrderValue = currentMetrics.averageOrderValue
            )
            
            val previousPeriod = PerformanceData(
                periodName = "Previous Period",
                sales = currentMetrics.monthlySales * BigDecimal.valueOf(0.9), // Placeholder
                profit = currentMetrics.monthlyProfit * BigDecimal.valueOf(0.85),
                transactions = (currentMetrics.monthlyTransactions * 0.9).toInt(),
                customers = (currentMetrics.activeCustomers * 0.8).toInt(),
                averageOrderValue = currentMetrics.averageOrderValue * BigDecimal.valueOf(1.1)
            )
            
            val growthMetrics = calculateGrowthMetrics(currentPeriod, previousPeriod)
            
            emit(PerformanceComparison(currentPeriod, previousPeriod, growthMetrics))
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _refreshState.value = _refreshState.value.copy(error = null)
    }

    /**
     * Set dashboard view mode
     */
    fun setViewMode(viewMode: DashboardViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = viewMode)
    }

    // Private helper methods

    private fun getDefaultDateRange(): DateRange {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return DateRange(
            startDate = today.minus(DatePeriod(days = 30)),
            endDate = today,
            preset = DateRangePreset.LAST_MONTH
        )
    }

    private fun createDateRangeFromPreset(preset: DateRangePreset): DateRange {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        return when (preset) {
            DateRangePreset.TODAY -> DateRange(today, today, preset)
            DateRangePreset.YESTERDAY -> {
                val yesterday = today.minus(DatePeriod(days = 1))
                DateRange(yesterday, yesterday, preset)
            }
            DateRangePreset.THIS_WEEK -> {
                val startOfWeek = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
                DateRange(startOfWeek, today, preset)
            }
            DateRangePreset.LAST_WEEK -> {
                val lastWeekEnd = today.minus(DatePeriod(days = today.dayOfWeek.ordinal + 1))
                val lastWeekStart = lastWeekEnd.minus(DatePeriod(days = 6))
                DateRange(lastWeekStart, lastWeekEnd, preset)
            }
            DateRangePreset.THIS_MONTH -> {
                val startOfMonth = LocalDate(today.year, today.month, 1)
                DateRange(startOfMonth, today, preset)
            }
            DateRangePreset.LAST_MONTH -> {
                val lastMonth = today.minus(DatePeriod(months = 1))
                val startOfLastMonth = LocalDate(lastMonth.year, lastMonth.month, 1)
                val endOfLastMonth = startOfLastMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
                DateRange(startOfLastMonth, endOfLastMonth, preset)
            }
            DateRangePreset.THIS_QUARTER -> {
                val quarterStart = getQuarterStart(today)
                DateRange(quarterStart, today, preset)
            }
            DateRangePreset.LAST_QUARTER -> {
                val lastQuarterEnd = getQuarterStart(today).minus(DatePeriod(days = 1))
                val lastQuarterStart = getQuarterStart(lastQuarterEnd)
                DateRange(lastQuarterStart, lastQuarterEnd, preset)
            }
            DateRangePreset.THIS_YEAR -> {
                val startOfYear = LocalDate(today.year, 1, 1)
                DateRange(startOfYear, today, preset)
            }
            DateRangePreset.LAST_YEAR -> {
                val lastYear = today.year - 1
                DateRange(LocalDate(lastYear, 1, 1), LocalDate(lastYear, 12, 31), preset)
            }
            DateRangePreset.CUSTOM -> getDefaultDateRange() // Fallback
        }
    }

    private fun getQuarterStart(date: LocalDate): LocalDate {
        val quarter = when (date.monthNumber) {
            in 1..3 -> 1
            in 4..6 -> 4
            in 7..9 -> 7
            else -> 10
        }
        return LocalDate(date.year, quarter, 1)
    }

    private fun setupAutoRefresh() {
        if (!_refreshState.value.autoRefreshEnabled) return
        
        viewModelScope.launch {
            while (_refreshState.value.autoRefreshEnabled) {
                kotlinx.coroutines.delay(_refreshState.value.refreshIntervalMinutes * 60 * 1000L)
                if (_refreshState.value.autoRefreshEnabled) {
                    loadDashboardData()
                }
            }
        }
    }

    private fun getPreviousDateRange(currentRange: DateRange): DateRange {
        val periodLength = currentRange.endDate.toEpochDays() - currentRange.startDate.toEpochDays()
        val previousEnd = currentRange.startDate.minus(DatePeriod(days = 1))
        val previousStart = previousEnd.minus(DatePeriod(days = periodLength.toInt()))
        
        return DateRange(previousStart, previousEnd)
    }

    private fun calculateGrowthMetrics(current: PerformanceData, previous: PerformanceData): GrowthMetrics {
        fun calculateBigDecimalGrowth(current: BigDecimal, previous: BigDecimal): Double {
            return if (previous > BigDecimal.ZERO) {
                ((current - previous) / previous * BigDecimal.valueOf(100.0)).toDouble()
            } else 0.0
        }
        
        fun calculateIntGrowth(current: Int, previous: Int): Double {
            return if (previous > 0) {
                ((current - previous).toDouble() / previous * 100.0)
            } else 0.0
        }
        
        return GrowthMetrics(
            salesGrowth = calculateBigDecimalGrowth(current.sales, previous.sales),
            profitGrowth = calculateBigDecimalGrowth(current.profit, previous.profit),
            transactionGrowth = calculateIntGrowth(current.transactions, previous.transactions),
            customerGrowth = calculateIntGrowth(current.customers, previous.customers),
            aovGrowth = calculateBigDecimalGrowth(current.averageOrderValue, previous.averageOrderValue)
        )
    }
}

/**
 * UI State for Dashboard
 */
data class DashboardUiState(
    val dashboardData: DashboardData? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: kotlinx.datetime.Instant? = null,
    val viewMode: DashboardViewMode = DashboardViewMode.OVERVIEW
)

/**
 * Dashboard view modes
 */
enum class DashboardViewMode {
    OVERVIEW,
    SALES,
    INVENTORY,
    CUSTOMERS,
    PROFITS,
    ANALYTICS
}