package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Entity for storing signature images
 * Stores the file path of the signature image for display on invoices
 */
@Entity(tableName = "signatures")
data class Signature(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "signature_id")
    val signatureId: Long = 0,
    
    @ColumnInfo(name = "signature_name")
    val signatureName: String, // e.g., "Owner", "Manager", "Authorized"
    
    @ColumnInfo(name = "signature_file_path")
    val signatureFilePath: String, // Path to the signature image file
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true, // Whether this signature should be printed on invoices
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
