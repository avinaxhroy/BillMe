package com.billme.app.ui.screen.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.core.database.DatabaseInitializer
import com.billme.app.core.database.DatabaseStats
import com.billme.app.data.local.DatabaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DatabaseManagementUiState(
    val stats: DatabaseStats? = null,
    val isLoading: Boolean = false,
    val lastBackupTime: String? = null,
    val backupInProgress: Boolean = false,
    val restoreInProgress: Boolean = false,
    val resetInProgress: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class DatabaseManagementViewModel @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val databaseInitializer: DatabaseInitializer
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DatabaseManagementUiState())
    val uiState: StateFlow<DatabaseManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadDatabaseStats()
    }
    
    fun loadDatabaseStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val stats = databaseInitializer.getDatabaseStats()
                _uiState.value = _uiState.value.copy(
                    stats = stats,
                    isLoading = false,
                    error = if (!stats.isHealthy) stats.error else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load stats: ${e.message}"
                )
            }
        }
    }
    
    fun backupDatabase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                backupInProgress = true,
                message = null,
                error = null
            )
            
            val result = databaseManager.backupDatabase()
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    backupInProgress = false,
                    message = "Backup created successfully",
                    lastBackupTime = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm")
                        .format(java.util.Date())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    backupInProgress = false,
                    error = "Backup failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun restoreDatabase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                restoreInProgress = true,
                message = null,
                error = null
            )
            
            val result = databaseManager.restoreFromBackup()
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    restoreInProgress = false,
                    message = "Database restored successfully"
                )
                loadDatabaseStats()
            } else {
                _uiState.value = _uiState.value.copy(
                    restoreInProgress = false,
                    error = "Restore failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun repairDatabase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                message = null,
                error = null
            )
            
            val result = databaseManager.repairDatabase()
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Database repaired successfully"
                )
                loadDatabaseStats()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Repair failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun resetDatabase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                resetInProgress = true,
                message = null,
                error = null
            )
            
            val result = databaseInitializer.resetDatabase()
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    resetInProgress = false,
                    message = "Database reset to factory settings"
                )
                loadDatabaseStats()
            } else {
                _uiState.value = _uiState.value.copy(
                    resetInProgress = false,
                    error = "Reset failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun checkHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val health = databaseManager.checkDatabaseHealth()
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = if (health.isHealthy) "Database is healthy ✓" else "Issues detected",
                error = if (!health.isHealthy) health.message else null
            )
            
            loadDatabaseStats()
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}
