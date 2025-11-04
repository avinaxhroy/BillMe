package com.billme.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.setupPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "setup_preferences")

/**
 * Repository for managing first-time setup preferences
 * Tracks whether the initial setup wizard has been completed
 */
@Singleton
class SetupPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.setupPreferencesDataStore
    
    companion object {
        private val SETUP_COMPLETED_KEY = booleanPreferencesKey("setup_completed")
        private val SETUP_COMPLETED_TIMESTAMP_KEY = longPreferencesKey("setup_completed_timestamp")
        private val SETUP_VERSION_KEY = longPreferencesKey("setup_version")
        
        private const val CURRENT_SETUP_VERSION = 1L
    }
    
    /**
     * Flow of setup completion status
     */
    val isSetupCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SETUP_COMPLETED_KEY] ?: false
    }
    
    /**
     * Check if setup is completed synchronously
     */
    suspend fun isSetupCompletedSync(): Boolean {
        return isSetupCompleted.first()
    }
    
    /**
     * Get setup completion timestamp
     */
    suspend fun getSetupCompletedTimestamp(): Long? {
        return dataStore.data.first()[SETUP_COMPLETED_TIMESTAMP_KEY]
    }
    
    /**
     * Get setup version
     */
    suspend fun getSetupVersion(): Long {
        return dataStore.data.first()[SETUP_VERSION_KEY] ?: 0L
    }
    
    /**
     * Mark setup as completed
     */
    suspend fun markSetupCompleted() {
        dataStore.edit { preferences ->
            preferences[SETUP_COMPLETED_KEY] = true
            preferences[SETUP_COMPLETED_TIMESTAMP_KEY] = System.currentTimeMillis()
            preferences[SETUP_VERSION_KEY] = CURRENT_SETUP_VERSION
        }
    }
    
    /**
     * Reset setup (for testing or re-onboarding)
     */
    suspend fun resetSetup() {
        dataStore.edit { preferences ->
            preferences.remove(SETUP_COMPLETED_KEY)
            preferences.remove(SETUP_COMPLETED_TIMESTAMP_KEY)
            preferences.remove(SETUP_VERSION_KEY)
        }
    }
    
    /**
     * Check if setup needs upgrade (for future versions)
     */
    suspend fun needsSetupUpgrade(): Boolean {
        val currentVersion = getSetupVersion()
        return currentVersion < CURRENT_SETUP_VERSION
    }
}
