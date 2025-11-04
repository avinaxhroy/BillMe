package com.billme.app.service

import com.billme.app.data.local.entity.TransactionLineItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Serializable line item for draft
 */
@Serializable
data class DraftLineItem(
    val productId: Long,
    val productName: String,
    val imeiSold: String,
    val quantity: Int,
    val unitSellingPrice: Double,
    val lineTotal: Double
)

/**
 * Draft transaction data
 */
@Serializable
data class TransactionDraft(
    val id: String,
    val timestamp: Long,
    val cartItems: List<DraftLineItem>,
    val subtotal: Double,
    val discountPercent: Double,
    val gstPercent: Double,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val notes: String? = null
)

/**
 * Auto-save states
 */
enum class AutoSaveState {
    IDLE,
    SAVING,
    SAVED,
    ERROR
}

/**
 * Auto-save service for transaction drafts
 */
class AutoSaveService(
    private val cacheDir: File,
    private val autoSaveInterval: Long = 5000L // 5 seconds
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private var autoSaveJob: Job? = null
    
    private val _state = MutableStateFlow(AutoSaveState.IDLE)
    val state: StateFlow<AutoSaveState> = _state.asStateFlow()
    
    private val _currentDraft = MutableStateFlow<TransactionDraft?>(null)
    val currentDraft: StateFlow<TransactionDraft?> = _currentDraft.asStateFlow()
    
    private val draftFile = File(cacheDir, "transaction_draft.json")
    
    init {
        // Load existing draft on initialization
        loadDraft()
    }
    
    /**
     * Start auto-save for transaction data
     */
    fun startAutoSave(
        cartItems: List<TransactionLineItem>,
        subtotal: Double,
        discountPercent: Double,
        gstPercent: Double,
        customerName: String? = null,
        customerPhone: String? = null,
        notes: String? = null
    ) {
        // Cancel existing auto-save
        autoSaveJob?.cancel()
        
        autoSaveJob = scope.launch {
            while (true) {
                delay(autoSaveInterval)
                
                if (cartItems.isNotEmpty()) {
                    val draftItems = cartItems.map { item ->
                        DraftLineItem(
                            productId = item.productId,
                            productName = item.productName,
                            imeiSold = item.imeiSold,
                            quantity = item.quantity,
                            unitSellingPrice = item.unitSellingPrice.toDouble(),
                            lineTotal = item.lineTotal.toDouble()
                        )
                    }
                    val draft = TransactionDraft(
                        id = "draft_${System.currentTimeMillis()}",
                        timestamp = System.currentTimeMillis(),
                        cartItems = draftItems,
                        subtotal = subtotal,
                        discountPercent = discountPercent,
                        gstPercent = gstPercent,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        notes = notes
                    )
                    
                    saveDraft(draft)
                }
            }
        }
    }
    
    /**
     * Stop auto-save
     */
    fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }
    
    /**
     * Manually save draft
     */
    fun saveDraft(draft: TransactionDraft) {
        scope.launch {
            try {
                _state.value = AutoSaveState.SAVING
                
                val jsonString = json.encodeToString(draft)
                draftFile.writeText(jsonString)
                
                _currentDraft.value = draft
                _state.value = AutoSaveState.SAVED
                
                // Reset to idle after short delay
                delay(1000)
                _state.value = AutoSaveState.IDLE
                
            } catch (e: Exception) {
                _state.value = AutoSaveState.ERROR
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Load existing draft
     */
    private fun loadDraft() {
        scope.launch {
            try {
                if (draftFile.exists()) {
                    val jsonString = draftFile.readText()
                    val draft = json.decodeFromString<TransactionDraft>(jsonString)
                    _currentDraft.value = draft
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Delete corrupted draft file
                if (draftFile.exists()) {
                    draftFile.delete()
                }
            }
        }
    }
    
    /**
     * Clear current draft
     */
    fun clearDraft() {
        scope.launch {
            try {
                if (draftFile.exists()) {
                    draftFile.delete()
                }
                _currentDraft.value = null
                stopAutoSave()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Check if draft exists
     */
    fun hasDraft(): Boolean {
        return _currentDraft.value != null
    }
    
    /**
     * Get draft age in milliseconds
     */
    fun getDraftAge(): Long? {
        val draft = _currentDraft.value ?: return null
        return System.currentTimeMillis() - draft.timestamp
    }
    
    /**
     * Check if draft is recent (within 24 hours)
     */
    fun isDraftRecent(): Boolean {
        val age = getDraftAge() ?: return false
        return age < 24 * 60 * 60 * 1000L // 24 hours
    }
    
    /**
     * Release resources
     */
    fun release() {
        stopAutoSave()
    }
}