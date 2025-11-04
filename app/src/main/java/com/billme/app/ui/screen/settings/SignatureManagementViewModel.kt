package com.billme.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billme.app.data.local.entity.Signature
import com.billme.app.data.repository.SignatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignatureManagementUiState(
    val signatures: List<Signature> = emptyList(),
    val activeSignature: Signature? = null,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val showDeleteConfirmation: Long? = null // ID of signature to delete
)

@HiltViewModel
class SignatureManagementViewModel @Inject constructor(
    private val signatureRepository: SignatureRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SignatureManagementUiState())
    val uiState: StateFlow<SignatureManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadSignatures()
        loadActiveSignature()
    }
    
    private fun loadSignatures() {
        viewModelScope.launch {
            signatureRepository.getAllSignaturesFlow().collect { signatures ->
                _uiState.update {
                    it.copy(signatures = signatures)
                }
            }
        }
    }
    
    private fun loadActiveSignature() {
        viewModelScope.launch {
            signatureRepository.getActiveSignatureFlow().collect { activeSignature ->
                _uiState.update {
                    it.copy(activeSignature = activeSignature)
                }
            }
        }
    }
    
    fun addSignature(
        signatureName: String,
        signatureFilePath: String,
        setAsActive: Boolean = false
    ) {
        if (signatureName.isBlank() || signatureFilePath.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Signature name and file path are required") }
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                signatureRepository.addSignature(signatureName, signatureFilePath, setAsActive)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Signature added successfully"
                    )
                }
                
                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(successMessage = null) }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to add signature: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun setSignatureAsActive(signatureId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                signatureRepository.setSignatureAsActive(signatureId)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Signature set as active"
                    )
                }
                
                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(successMessage = null) }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to set signature as active: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun showDeleteConfirmation(signatureId: Long) {
        _uiState.update { it.copy(showDeleteConfirmation = signatureId) }
    }
    
    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = null) }
    }
    
    fun deleteSignature(signatureId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, showDeleteConfirmation = null) }
                
                signatureRepository.deleteSignature(signatureId)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Signature deleted successfully"
                    )
                }
                
                // Clear success message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(successMessage = null) }
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete signature: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
