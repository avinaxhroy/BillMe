package com.billme.app.data.repository

import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.entity.Signature
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user signatures
 */
@Singleton
class SignatureRepository @Inject constructor(
    private val database: BillMeDatabase
) {
    
    /**
     * Add a new signature
     */
    suspend fun addSignature(
        signatureName: String,
        signatureFilePath: String,
        setAsActive: Boolean = false
    ): Long {
        val now = Clock.System.now()
        
        // If this should be active, deactivate others
        if (setAsActive) {
            database.signatureDao().deactivateAllSignatures()
        }
        
        val signature = Signature(
            signatureName = signatureName,
            signatureFilePath = signatureFilePath,
            isActive = setAsActive,
            createdAt = now,
            updatedAt = now
        )
        
        return database.signatureDao().insertSignature(signature)
    }
    
    /**
     * Get all signatures
     */
    suspend fun getAllSignatures(): List<Signature> {
        return database.signatureDao().getAllSignatures()
    }
    
    /**
     * Get all signatures as Flow
     */
    fun getAllSignaturesFlow(): Flow<List<Signature>> {
        return database.signatureDao().getAllSignaturesFlow()
    }
    
    /**
     * Get active signature for printing on invoices
     */
    suspend fun getActiveSignature(): Signature? {
        return database.signatureDao().getActiveSignature()
    }
    
    /**
     * Get active signature as Flow
     */
    fun getActiveSignatureFlow(): Flow<Signature?> {
        return database.signatureDao().getActiveSignatureFlow()
    }
    
    /**
     * Set a signature as active
     */
    suspend fun setSignatureAsActive(signatureId: Long) {
        // Deactivate all
        database.signatureDao().deactivateAllSignatures()
        
        // Get and update the selected one
        val signature = database.signatureDao().getSignatureById(signatureId)
        if (signature != null) {
            val updatedSignature = signature.copy(
                isActive = true,
                updatedAt = Clock.System.now()
            )
            database.signatureDao().updateSignature(updatedSignature)
        }
    }
    
    /**
     * Delete a signature
     */
    suspend fun deleteSignature(signatureId: Long) {
        val signature = database.signatureDao().getSignatureById(signatureId)
        if (signature != null) {
            database.signatureDao().deleteSignature(signature)
        }
    }
    
    /**
     * Update signature name
     */
    suspend fun updateSignatureName(signatureId: Long, newName: String) {
        val signature = database.signatureDao().getSignatureById(signatureId)
        if (signature != null) {
            val updatedSignature = signature.copy(
                signatureName = newName,
                updatedAt = Clock.System.now()
            )
            database.signatureDao().updateSignature(updatedSignature)
        }
    }
}
