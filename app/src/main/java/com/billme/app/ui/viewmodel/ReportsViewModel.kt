package com.billme.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.service.ReportExportService
import com.billme.app.core.service.ReportGenerationService
import com.billme.app.core.service.ExportResult
import com.billme.app.data.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import javax.inject.Inject

sealed class ReportUiState {
    object Idle : ReportUiState()
    object Loading : ReportUiState()
    data class Success(val data: ReportData) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportGenerationService: ReportGenerationService,
    private val reportExportService: ReportExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _currentFilters = MutableStateFlow(
        run {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val thirtyDaysAgo = today.minus(30, kotlinx.datetime.DateTimeUnit.DAY)
            ReportFilters(
                dateRange = DateRange(
                    startDate = thirtyDaysAgo,
                    endDate = today,
                    preset = DateRangePreset.CUSTOM
                )
            )
        }
    )
    val currentFilters: StateFlow<ReportFilters> = _currentFilters.asStateFlow()

    private val _currentReportType = MutableStateFlow(ReportType.SALES)
    val currentReportType: StateFlow<ReportType> = _currentReportType.asStateFlow()

    private var currentJob: Job? = null

    fun setReportType(type: ReportType) {
        _currentReportType.value = type
    }

    fun updateFilters(update: (ReportFilters) -> ReportFilters) {
        _currentFilters.value = update(_currentFilters.value)
    }

    fun generateReport() {
        val type = _currentReportType.value
        val filters = _currentFilters.value
        currentJob?.cancel()
        _uiState.value = ReportUiState.Loading
        currentJob = viewModelScope.launch {
            try {
                val data = reportGenerationService.generateCompleteReport(type, filters)
                _uiState.value = ReportUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(e.message ?: "Failed to generate report")
            }
        }
    }

    fun exportCurrentReport(format: ExportFormat) {
        val state = _uiState.value
        if (state !is ReportUiState.Success) return
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting(format)
            when (val result = reportExportService.exportReport(state.data, format)) {
                is ExportResult.Success -> _exportState.value = ExportState.Success(result.filePath, result.mimeType)
                is ExportResult.Error -> _exportState.value = ExportState.Error(result.message)
            }
        }
    }
}

sealed class ExportState {
    object Idle : ExportState()
    data class Exporting(val format: ExportFormat) : ExportState()
    data class Success(val filePath: String, val mimeType: String) : ExportState()
    data class Error(val message: String) : ExportState()
}
