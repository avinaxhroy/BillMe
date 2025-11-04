package com.billme.app.data.local.dao

import androidx.room.*
import com.billme.app.data.local.entity.Signature
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Signature management
 */
@Dao
interface SignatureDao {
    
    /**
     * Insert a new signature
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: Signature): Long
    
    /**
     * Update existing signature
     */
    @Update
    suspend fun updateSignature(signature: Signature)
    
    /**
     * Delete a signature
     */
    @Delete
    suspend fun deleteSignature(signature: Signature)
    
    /**
     * Get all signatures
     */
    @Query("SELECT * FROM signatures ORDER BY created_at DESC")
    suspend fun getAllSignatures(): List<Signature>
    
    /**
     * Get all signatures as Flow
     */
    @Query("SELECT * FROM signatures ORDER BY created_at DESC")
    fun getAllSignaturesFlow(): Flow<List<Signature>>
    
    /**
     * Get active signature (the one to print on invoices)
     */
    @Query("SELECT * FROM signatures WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveSignature(): Signature?
    
    /**
     * Get active signature as Flow
     */
    @Query("SELECT * FROM signatures WHERE is_active = 1 LIMIT 1")
    fun getActiveSignatureFlow(): Flow<Signature?>
    
    /**
     * Get signature by ID
     */
    @Query("SELECT * FROM signatures WHERE signature_id = :signatureId")
    suspend fun getSignatureById(signatureId: Long): Signature?
    
    /**
     * Set a specific signature as active (deactivate others)
     */
    @Query("UPDATE signatures SET is_active = 0 WHERE is_active = 1")
    suspend fun deactivateAllSignatures()
    
    /**
     * Delete all signatures
     */
    @Query("DELETE FROM signatures")
    suspend fun deleteAllSignatures()
    
    /**
     * Get total signature count (for backup)
     */
    @Query("SELECT COUNT(*) FROM signatures")
    suspend fun getSignatureCount(): Int
}
