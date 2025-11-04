package com.billme.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.data.local.entity.GSTConfiguration
import com.billme.app.data.local.entity.GSTRate
import com.billme.app.data.repository.GSTRepository
import com.billme.app.ui.screen.GSTSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for GST Settings Screen
 */
@HiltViewModel
class GSTSettingsViewModel @Inject constructor(
    private val gstRepository: GSTRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GSTSettingsUiState(isLoading = true))
    val uiState: StateFlow<GSTSettingsUiState> = _uiState.asStateFlow()
    
    private var currentConfiguration: GSTConfiguration? = null
    
    init {
        loadGSTSettings()
    }
    
    private fun loadGSTSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Load configuration and rates in parallel
                val configurationFlow = gstRepository.getCurrentGSTConfiguration()
                val ratesFlow = gstRepository.getAllActiveGSTRates()
                
                combine(configurationFlow, ratesFlow) { config, rates ->
                    GSTSettingsUiState(
                        configuration = config,
                        gstRates = rates,
                        isLoading = false,
                        error = null
                    )
                }.collect { state ->
                    currentConfiguration = state.configuration
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load GST settings"
                )
            }
        }
    }
    
    fun updateConfiguration(configuration: GSTConfiguration) {
        currentConfiguration = configuration
        _uiState.value = _uiState.value.copy(configuration = configuration)
    }
    
    fun saveConfiguration() {
        val configuration = currentConfiguration ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                gstRepository.saveGSTConfiguration(configuration)
                
                // Initialize default rates if this is the first time setting up GST
                if (configuration.configId == 0L) {
                    gstRepository.initializeDefaultGSTRates()
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to save GST configuration"
                )
            }
        }
    }
    
    fun addGSTRate(rate: GSTRate) {
        viewModelScope.launch {
            try {
                gstRepository.addGSTRate(rate)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to add GST rate"
                )
            }
        }
    }
    
    fun updateGSTRate(rate: GSTRate) {
        viewModelScope.launch {
            try {
                gstRepository.updateGSTRate(rate)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update GST rate"
                )
            }
        }
    }
    
    fun deleteGSTRate(rateId: Long) {
        viewModelScope.launch {
            try {
                gstRepository.deleteGSTRate(rateId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete GST rate"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}