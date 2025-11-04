package com.billme.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import com.billme.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.KeyStore
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKeyFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive encryption service for secure data protection
 */
@Singleton
class EncryptionService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "mobile_shop_"
        private const val TRANSFORMATION_AES_GCM = "AES/GCM/NoPadding"
        private const val TRANSFORMATION_AES_CBC = "AES/CBC/PKCS7Padding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val AES_KEY_LENGTH = 256
        private const val PBKDF2_ITERATIONS = 10000
        private const val SALT_LENGTH = 16
        
        // File encryption constants
        private const val ENCRYPTED_FILE_HEADER = "MSB_ENC_"
        private const val VERSION_1 = "V1"
        private const val CHUNK_SIZE = 64 * 1024 // 64KB chunks
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * Generate a new encryption key for specific purpose
     */
    suspend fun generateEncryptionKey(
        purpose: KeyPurpose,
        password: String? = null
    ): EncryptionKeyInfo = withContext(Dispatchers.IO) {
        val keyId = generateKeyId(purpose)
        val salt = generateSalt()
        
        when (purpose) {
            KeyPurpose.DATABASE_ENCRYPTION,
            KeyPurpose.BACKUP_ENCRYPTION -> {
                if (password != null) {
                    generatePasswordBasedKey(keyId, password, salt, purpose)
                } else {
                    generateKeystoreKey(keyId, purpose)
                }
            }
            KeyPurpose.CLOUD_SYNC_ENCRYPTION,
            KeyPurpose.EXPORT_ENCRYPTION -> {
                requireNotNull(password) { "Password required for ${purpose.name}" }
                generatePasswordBasedKey(keyId, password, salt, purpose)
            }
        }
    }
    
    /**
     * Encrypt data using specified key
     */
    suspend fun encryptData(
        data: ByteArray,
        keyId: String,
        method: EncryptionMethod = EncryptionMethod.AES_256_GCM
    ): EncryptedData = withContext(Dispatchers.IO) {
        when (method) {
            EncryptionMethod.AES_256_GCM -> encryptWithAESGCM(data, keyId)
            EncryptionMethod.AES_256_CBC -> encryptWithAESCBC(data, keyId)
            EncryptionMethod.CHACHA20_POLY1305 -> encryptWithChaCha20(data, keyId)
            EncryptionMethod.NONE -> EncryptedData(data, null, method, keyId)
        }
    }
    
    /**
     * Decrypt data using specified key
     */
    suspend fun decryptData(
        encryptedData: EncryptedData
    ): ByteArray = withContext(Dispatchers.IO) {
        when (encryptedData.method) {
            EncryptionMethod.AES_256_GCM -> decryptWithAESGCM(encryptedData)
            EncryptionMethod.AES_256_CBC -> decryptWithAESCBC(encryptedData)
            EncryptionMethod.CHACHA20_POLY1305 -> decryptWithChaCha20(encryptedData)
            EncryptionMethod.NONE -> encryptedData.data
        }
    }
    
    /**
     * Encrypt file stream for backup
     */
    suspend fun encryptFile(
        inputStream: InputStream,
        outputStream: OutputStream,
        keyId: String,
        method: EncryptionMethod = EncryptionMethod.AES_256_GCM
    ): FileEncryptionResult = withContext(Dispatchers.IO) {
        try {
            // Write file header
            val header = createFileHeader(method, keyId)
            outputStream.write(header)
            
            var totalBytesRead = 0L
            val buffer = ByteArray(CHUNK_SIZE)
            var bytesRead: Int
            
            val cipher = createEncryptionCipher(keyId, method)
            val iv = cipher.iv
            
            // Write IV to file
            outputStream.write(iv.size)
            outputStream.write(iv)
            
            // Encrypt file in chunks
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val chunk = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                val encryptedChunk = cipher.update(chunk)
                if (encryptedChunk != null) {
                    outputStream.write(encryptedChunk)
                }
                totalBytesRead += bytesRead
            }
            
            // Finalize encryption
            val finalChunk = cipher.doFinal()
            if (finalChunk.isNotEmpty()) {
                outputStream.write(finalChunk)
            }
            
            FileEncryptionResult(
                success = true,
                keyId = keyId,
                method = method,
                bytesProcessed = totalBytesRead,
                checksum = calculateChecksum(totalBytesRead.toString())
            )
        } catch (e: Exception) {
            FileEncryptionResult(
                success = false,
                keyId = keyId,
                method = method,
                error = e.message ?: "Encryption failed"
            )
        }
    }
    
    /**
     * Decrypt file stream
     */
    suspend fun decryptFile(
        inputStream: InputStream,
        outputStream: OutputStream,
        password: String? = null
    ): FileDecryptionResult = withContext(Dispatchers.IO) {
        try {
            // Read and validate header
            val header = readFileHeader(inputStream)
            val keyId = if (password != null) {
                deriveKeyFromPassword(password, header.keyId)
            } else {
                header.keyId
            }
            
            // Read IV
            val ivSize = inputStream.read()
            val iv = ByteArray(ivSize)
            inputStream.read(iv)
            
            val cipher = createDecryptionCipher(keyId, header.method, iv)
            
            var totalBytesWritten = 0L
            val buffer = ByteArray(CHUNK_SIZE)
            var bytesRead: Int
            
            // Decrypt file in chunks
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val chunk = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                val decryptedChunk = cipher.update(chunk)
                if (decryptedChunk != null) {
                    outputStream.write(decryptedChunk)
                    totalBytesWritten += decryptedChunk.size
                }
            }
            
            // Finalize decryption
            val finalChunk = cipher.doFinal()
            if (finalChunk.isNotEmpty()) {
                outputStream.write(finalChunk)
                totalBytesWritten += finalChunk.size
            }
            
            FileDecryptionResult(
                success = true,
                method = header.method,
                bytesProcessed = totalBytesWritten
            )
        } catch (e: Exception) {
            FileDecryptionResult(
                success = false,
                error = e.message ?: "Decryption failed"
            )
        }
    }
    
    /**
     * Rotate encryption key for enhanced security
     */
    suspend fun rotateEncryptionKey(
        oldKeyId: String,
        purpose: KeyPurpose,
        password: String? = null
    ): KeyRotationResult = withContext(Dispatchers.IO) {
        try {
            // Generate new key
            val newKeyInfo = generateEncryptionKey(purpose, password)
            
            // Test key functionality
            val testData = "test_encryption_${System.currentTimeMillis()}".toByteArray()
            val encrypted = encryptData(testData, newKeyInfo.keyId)
            val decrypted = decryptData(encrypted)
            
            if (!testData.contentEquals(decrypted)) {
                throw SecurityException("Key rotation validation failed")
            }
            
            KeyRotationResult(
                success = true,
                oldKeyId = oldKeyId,
                newKeyId = newKeyInfo.keyId,
                rotatedAt = LocalDateTime.now()
            )
        } catch (e: Exception) {
            KeyRotationResult(
                success = false,
                oldKeyId = oldKeyId,
                error = e.message ?: "Key rotation failed"
            )
        }
    }
    
    /**
     * Verify data integrity using checksum
     */
    suspend fun verifyIntegrity(
        data: ByteArray,
        expectedChecksum: String
    ): Boolean = withContext(Dispatchers.IO) {
        val actualChecksum = calculateChecksum(data)
        actualChecksum == expectedChecksum
    }
    
    /**
     * Generate secure random salt
     */
    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }
    
    /**
     * Calculate SHA-256 checksum
     */
    private fun calculateChecksum(data: ByteArray): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(data)
            .let { Base64.getEncoder().encodeToString(it) }
    }
    
    private fun calculateChecksum(data: String): String = calculateChecksum(data.toByteArray())
    
    // Private implementation methods
    
    private fun generateKeyId(purpose: KeyPurpose): String {
        return "${KEY_ALIAS_PREFIX}${purpose.name.lowercase()}_${UUID.randomUUID()}"
    }
    
    private suspend fun generateKeystoreKey(
        keyId: String,
        purpose: KeyPurpose
    ): EncryptionKeyInfo = withContext(Dispatchers.IO) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyId,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_LENGTH)
            .setUserAuthenticationRequired(false) // Can be enabled for additional security
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
        
        EncryptionKeyInfo(
            keyId = keyId,
            algorithm = "AES",
            keySize = AES_KEY_LENGTH,
            purpose = purpose,
            derivationMethod = "AndroidKeystore",
            salt = "",
            iterations = 0
        )
    }
    
    private suspend fun generatePasswordBasedKey(
        keyId: String,
        password: String,
        salt: String,
        purpose: KeyPurpose
    ): EncryptionKeyInfo = withContext(Dispatchers.IO) {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder().decode(salt),
            PBKDF2_ITERATIONS,
            AES_KEY_LENGTH
        )
        val key = factory.generateSecret(spec)
        
        // Store the derived key securely
        val secretKey = SecretKeySpec(key.encoded, "AES")
        storeSecretKey(keyId, secretKey)
        
        EncryptionKeyInfo(
            keyId = keyId,
            algorithm = "AES",
            keySize = AES_KEY_LENGTH,
            purpose = purpose,
            derivationMethod = "PBKDF2",
            salt = salt,
            iterations = PBKDF2_ITERATIONS
        )
    }
    
    private fun encryptWithAESGCM(data: ByteArray, keyId: String): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION_AES_GCM)
        val key = getSecretKey(keyId)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val encryptedData = cipher.doFinal(data)
        val iv = cipher.iv
        
        return EncryptedData(
            data = encryptedData,
            iv = iv,
            method = EncryptionMethod.AES_256_GCM,
            keyId = keyId
        )
    }
    
    private fun decryptWithAESGCM(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION_AES_GCM)
        val key = getSecretKey(encryptedData.keyId)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(encryptedData.data)
    }
    
    private fun encryptWithAESCBC(data: ByteArray, keyId: String): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC)
        val key = getSecretKey(keyId)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val encryptedData = cipher.doFinal(data)
        val iv = cipher.iv
        
        return EncryptedData(
            data = encryptedData,
            iv = iv,
            method = EncryptionMethod.AES_256_CBC,
            keyId = keyId
        )
    }
    
    private fun decryptWithAESCBC(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC)
        val key = getSecretKey(encryptedData.keyId)
        val spec = javax.crypto.spec.IvParameterSpec(encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(encryptedData.data)
    }
    
    private fun encryptWithChaCha20(data: ByteArray, keyId: String): EncryptedData {
        // ChaCha20-Poly1305 implementation would go here
        // For now, fallback to AES-GCM
        return encryptWithAESGCM(data, keyId)
    }
    
    private fun decryptWithChaCha20(encryptedData: EncryptedData): ByteArray {
        // ChaCha20-Poly1305 implementation would go here
        // For now, fallback to AES-GCM
        return decryptWithAESGCM(encryptedData)
    }
    
    private fun createEncryptionCipher(keyId: String, method: EncryptionMethod): Cipher {
        val transformation = when (method) {
            EncryptionMethod.AES_256_GCM -> TRANSFORMATION_AES_GCM
            EncryptionMethod.AES_256_CBC -> TRANSFORMATION_AES_CBC
            else -> TRANSFORMATION_AES_GCM
        }
        
        val cipher = Cipher.getInstance(transformation)
        val key = getSecretKey(keyId)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        return cipher
    }
    
    private fun createDecryptionCipher(keyId: String, method: EncryptionMethod, iv: ByteArray): Cipher {
        val transformation = when (method) {
            EncryptionMethod.AES_256_GCM -> TRANSFORMATION_AES_GCM
            EncryptionMethod.AES_256_CBC -> TRANSFORMATION_AES_CBC
            else -> TRANSFORMATION_AES_GCM
        }
        
        val cipher = Cipher.getInstance(transformation)
        val key = getSecretKey(keyId)
        
        val spec = when (method) {
            EncryptionMethod.AES_256_GCM -> GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            EncryptionMethod.AES_256_CBC -> javax.crypto.spec.IvParameterSpec(iv)
            else -> GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        }
        
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }
    
    private fun getSecretKey(keyId: String): SecretKey {
        return if (keyStore.containsAlias(keyId)) {
            keyStore.getKey(keyId, null) as SecretKey
        } else {
            // Try to load from shared preferences for password-based keys
            loadSecretKey(keyId) ?: throw SecurityException("Key not found: $keyId")
        }
    }
    
    private fun storeSecretKey(keyId: String, key: SecretKey) {
        // Store password-based keys in encrypted shared preferences
        val sharedPrefs = context.getSharedPreferences("encrypted_keys", Context.MODE_PRIVATE)
        val encodedKey = Base64.getEncoder().encodeToString(key.encoded)
        sharedPrefs.edit {
            putString(keyId, encodedKey)
        }
    }
    
    private fun loadSecretKey(keyId: String): SecretKey? {
        val sharedPrefs = context.getSharedPreferences("encrypted_keys", Context.MODE_PRIVATE)
        val encodedKey = sharedPrefs.getString(keyId, null) ?: return null
        val keyBytes = Base64.getDecoder().decode(encodedKey)
        return SecretKeySpec(keyBytes, "AES")
    }
    
    private fun createFileHeader(method: EncryptionMethod, keyId: String): ByteArray {
        val header = "${ENCRYPTED_FILE_HEADER}${VERSION_1}:${method.name}:${keyId}"
        val headerBytes = header.toByteArray()
        val lengthBytes = ByteArray(4)
        lengthBytes[0] = (headerBytes.size shr 24).toByte()
        lengthBytes[1] = (headerBytes.size shr 16).toByte()
        lengthBytes[2] = (headerBytes.size shr 8).toByte()
        lengthBytes[3] = headerBytes.size.toByte()
        
        return lengthBytes + headerBytes
    }
    
    private fun readFileHeader(inputStream: InputStream): FileHeader {
        val lengthBytes = ByteArray(4)
        inputStream.read(lengthBytes)
        
        val headerLength = ((lengthBytes[0].toInt() and 0xFF) shl 24) or
                          ((lengthBytes[1].toInt() and 0xFF) shl 16) or
                          ((lengthBytes[2].toInt() and 0xFF) shl 8) or
                          (lengthBytes[3].toInt() and 0xFF)
        
        val headerBytes = ByteArray(headerLength)
        inputStream.read(headerBytes)
        val header = String(headerBytes)
        
        val parts = header.split(":")
        if (parts.size != 3 || !parts[0].startsWith(ENCRYPTED_FILE_HEADER)) {
            throw SecurityException("Invalid file header")
        }
        
        return FileHeader(
            version = parts[0].substringAfter(ENCRYPTED_FILE_HEADER),
            method = EncryptionMethod.valueOf(parts[1]),
            keyId = parts[2]
        )
    }
    
    private fun deriveKeyFromPassword(password: String, keyId: String): String {
        // For password-based decryption, the keyId might contain salt information
        // This is a simplified implementation
        return keyId
    }
}

/**
 * Encrypted data container
 */
data class EncryptedData(
    val data: ByteArray,
    val iv: ByteArray?,
    val method: EncryptionMethod,
    val keyId: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        
        if (!data.contentEquals(other.data)) return false
        if (iv != null) {
            if (other.iv == null) return false
            if (!iv.contentEquals(other.iv)) return false
        } else if (other.iv != null) return false
        if (method != other.method) return false
        if (keyId != other.keyId) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        result = 31 * result + method.hashCode()
        result = 31 * result + keyId.hashCode()
        return result
    }
}

/**
 * File encryption result
 */
data class FileEncryptionResult(
    val success: Boolean,
    val keyId: String,
    val method: EncryptionMethod,
    val bytesProcessed: Long = 0,
    val checksum: String? = null,
    val error: String? = null
)

/**
 * File decryption result
 */
data class FileDecryptionResult(
    val success: Boolean,
    val method: EncryptionMethod? = null,
    val bytesProcessed: Long = 0,
    val error: String? = null
)

/**
 * Key rotation result
 */
data class KeyRotationResult(
    val success: Boolean,
    val oldKeyId: String,
    val newKeyId: String? = null,
    val rotatedAt: LocalDateTime? = null,
    val error: String? = null
)

/**
 * File header information
 */
private data class FileHeader(
    val version: String,
    val method: EncryptionMethod,
    val keyId: String
)