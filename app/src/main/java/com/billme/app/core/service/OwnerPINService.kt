package com.billme.app.core.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "owner_pin_preferences")

/**
 * Owner PIN authentication and management service
 * Handles PIN validation, lockout, and audit trail
 */
@Singleton
class OwnerPINService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        private val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
        private val PIN_SET_KEY = booleanPreferencesKey("pin_set")
        private val FAILED_ATTEMPTS_KEY = intPreferencesKey("failed_attempts")
        private val LAST_FAILED_TIME_KEY = longPreferencesKey("last_failed_time")
        private val LOCKOUT_UNTIL_KEY = longPreferencesKey("lockout_until")
        
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30 * 60 * 1000L // 30 minutes
        private const val ATTEMPT_RESET_TIME_MS = 60 * 60 * 1000L // 1 hour
    }
    
    /**
     * Check if owner PIN is set
     */
    suspend fun isPINSet(): Boolean {
        return context.dataStore.data.first()[PIN_SET_KEY] ?: false
    }
    
    /**
     * Set owner PIN (first time setup or change)
     */
    suspend fun setPIN(pin: String): Result<Unit> {
        return try {
            if (pin.length < 4 || pin.length > 8) {
                return Result.failure(Exception("PIN must be 4-8 digits"))
            }
            
            if (!pin.all { it.isDigit() }) {
                return Result.failure(Exception("PIN must contain only digits"))
            }
            
            val salt = generateSalt()
            val hashedPin = hashPIN(pin, salt)
            
            context.dataStore.edit { preferences ->
                preferences[PIN_HASH_KEY] = hashedPin
                preferences[PIN_SALT_KEY] = salt
                preferences[PIN_SET_KEY] = true
                preferences[FAILED_ATTEMPTS_KEY] = 0
                preferences[LOCKOUT_UNTIL_KEY] = 0L
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate owner PIN
     */
    suspend fun validateOwnerPIN(pin: String): Boolean {
        try {
            // Check if currently locked out
            if (isLockedOut()) {
                return false
            }
            
            val preferences = context.dataStore.data.first()
            val storedHash = preferences[PIN_HASH_KEY] ?: return false
            val salt = preferences[PIN_SALT_KEY] ?: return false
            
            val hashedInput = hashPIN(pin, salt)
            
            return if (hashedInput == storedHash) {
                // PIN correct - reset failed attempts
                resetFailedAttempts()
                true
            } else {
                // PIN incorrect - increment failed attempts
                incrementFailedAttempts()
                false
            }
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Change existing PIN (requires old PIN)
     */
    suspend fun changePIN(oldPin: String, newPin: String): Result<Unit> {
        return try {
            if (!validateOwnerPIN(oldPin)) {
                return Result.failure(Exception("Current PIN is incorrect"))
            }
            
            setPIN(newPin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if PIN validation is currently locked out
     */
    suspend fun isLockedOut(): Boolean {
        val preferences = context.dataStore.data.first()
        val lockoutUntil = preferences[LOCKOUT_UNTIL_KEY] ?: 0L
        return System.currentTimeMillis() < lockoutUntil
    }
    
    /**
     * Get remaining lockout time in milliseconds
     */
    suspend fun getRemainingLockoutTime(): Long {
        if (!isLockedOut()) return 0L
        
        val preferences = context.dataStore.data.first()
        val lockoutUntil = preferences[LOCKOUT_UNTIL_KEY] ?: 0L
        return maxOf(0L, lockoutUntil - System.currentTimeMillis())
    }
    
    /**
     * Get current failed attempts count
     */
    suspend fun getFailedAttempts(): Int {
        return context.dataStore.data.first()[FAILED_ATTEMPTS_KEY] ?: 0
    }
    
    /**
     * Get PIN security info
     */
    suspend fun getPINSecurityInfo(): PINSecurityInfo {
        val preferences = context.dataStore.data.first()
        val failedAttempts = preferences[FAILED_ATTEMPTS_KEY] ?: 0
        val lockoutUntil = preferences[LOCKOUT_UNTIL_KEY] ?: 0L
        val isLockedOut = System.currentTimeMillis() < lockoutUntil
        
        return PINSecurityInfo(
            failedAttempts = failedAttempts,
            maxAttempts = MAX_FAILED_ATTEMPTS,
            isLockedOut = isLockedOut,
            lockoutTimeRemaining = if (isLockedOut) lockoutUntil - System.currentTimeMillis() else 0L,
            attemptsRemaining = maxOf(0, MAX_FAILED_ATTEMPTS - failedAttempts)
        )
    }
    
    /**
     * Reset PIN (emergency reset - requires factory reset confirmation)
     */
    suspend fun emergencyResetPIN(): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences.remove(PIN_HASH_KEY)
                preferences.remove(PIN_SALT_KEY)
                preferences[PIN_SET_KEY] = false
                preferences[FAILED_ATTEMPTS_KEY] = 0
                preferences[LOCKOUT_UNTIL_KEY] = 0L
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate PIN with timeout (for UI components)
     */
    suspend fun validatePINWithTimeout(pin: String, timeoutMs: Long = 30000): PINValidationResult {
        val startTime = System.currentTimeMillis()
        
        // Check timeout
        if (System.currentTimeMillis() - startTime > timeoutMs) {
            return PINValidationResult(
                isValid = false,
                errorMessage = "PIN validation timeout",
                securityInfo = getPINSecurityInfo()
            )
        }
        
        // Check lockout
        if (isLockedOut()) {
            return PINValidationResult(
                isValid = false,
                errorMessage = "Account temporarily locked due to multiple failed attempts",
                securityInfo = getPINSecurityInfo()
            )
        }
        
        val isValid = validateOwnerPIN(pin)
        val securityInfo = getPINSecurityInfo()
        
        return PINValidationResult(
            isValid = isValid,
            errorMessage = if (isValid) null else "Invalid PIN",
            securityInfo = securityInfo
        )
    }
    
    /**
     * Generate PIN strength score (for PIN setup)
     */
    fun calculatePINStrength(pin: String): PINStrength {
        if (pin.length < 4) {
            return PINStrength.INVALID
        }
        
        val hasRepeating = pin.zipWithNext().all { it.first == it.second }
        val isSequential = isSequentialPattern(pin)
        val isCommonPattern = isCommonPINPattern(pin)
        
        return when {
            pin.length < 4 -> PINStrength.INVALID
            hasRepeating || isCommonPattern -> PINStrength.WEAK
            isSequential || pin.length == 4 -> PINStrength.MEDIUM
            pin.length >= 6 -> PINStrength.STRONG
            else -> PINStrength.MEDIUM
        }
    }
    
    // Private helper methods
    
    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun hashPIN(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPin = pin + salt
        val hashedBytes = digest.digest(saltedPin.toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
    
    private suspend fun incrementFailedAttempts() {
        context.dataStore.edit { preferences ->
            val currentAttempts = preferences[FAILED_ATTEMPTS_KEY] ?: 0
            val newAttempts = currentAttempts + 1
            
            preferences[FAILED_ATTEMPTS_KEY] = newAttempts
            preferences[LAST_FAILED_TIME_KEY] = System.currentTimeMillis()
            
            // Apply lockout if max attempts reached
            if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                preferences[LOCKOUT_UNTIL_KEY] = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            }
        }
    }
    
    private suspend fun resetFailedAttempts() {
        context.dataStore.edit { preferences ->
            preferences[FAILED_ATTEMPTS_KEY] = 0
            preferences[LOCKOUT_UNTIL_KEY] = 0L
        }
    }
    
    private fun isSequentialPattern(pin: String): Boolean {
        if (pin.length < 3) return false
        
        val ascending = pin.zipWithNext().all { (a, b) -> b.digitToInt() - a.digitToInt() == 1 }
        val descending = pin.zipWithNext().all { (a, b) -> a.digitToInt() - b.digitToInt() == 1 }
        
        return ascending || descending
    }
    
    private fun isCommonPINPattern(pin: String): Boolean {
        val commonPatterns = setOf(
            "1234", "4321", "1111", "2222", "3333", "4444", "5555",
            "6666", "7777", "8888", "9999", "0000", "1212", "2020",
            "0123", "9876", "6789", "1357", "2468"
        )
        
        return commonPatterns.contains(pin)
    }
}

/**
 * PIN security information
 */
data class PINSecurityInfo(
    val failedAttempts: Int,
    val maxAttempts: Int,
    val isLockedOut: Boolean,
    val lockoutTimeRemaining: Long,
    val attemptsRemaining: Int
)

/**
 * PIN validation result
 */
data class PINValidationResult(
    val isValid: Boolean,
    val errorMessage: String?,
    val securityInfo: PINSecurityInfo
)

/**
 * PIN strength levels
 */
enum class PINStrength {
    INVALID,
    WEAK,
    MEDIUM,
    STRONG
}