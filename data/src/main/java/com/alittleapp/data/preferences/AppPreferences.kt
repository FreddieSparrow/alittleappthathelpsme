package com.alittleapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")            // "system" | "light" | "dark"
        val CLIPBOARD_AUTO_CLEAR_KEY = booleanPreferencesKey("clipboard_auto_clear")
        val CLIPBOARD_MAX_ITEMS_KEY = intPreferencesKey("clipboard_max_items")
        val VAULT_PIN_HASH_KEY = stringPreferencesKey("vault_pin_hash")
        val VAULT_BIOMETRIC_KEY = booleanPreferencesKey("vault_biometric_enabled")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "system" }
    val clipboardAutoClear: Flow<Boolean> = context.dataStore.data.map { it[CLIPBOARD_AUTO_CLEAR_KEY] ?: false }
    val clipboardMaxItems: Flow<Int> = context.dataStore.data.map { it[CLIPBOARD_MAX_ITEMS_KEY] ?: 50 }
    val vaultPinHash: Flow<String?> = context.dataStore.data.map { it[VAULT_PIN_HASH_KEY] }
    val vaultBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[VAULT_BIOMETRIC_KEY] ?: false }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }

    suspend fun setClipboardAutoClear(enabled: Boolean) {
        context.dataStore.edit { it[CLIPBOARD_AUTO_CLEAR_KEY] = enabled }
    }

    suspend fun setVaultPinHash(hash: String) {
        context.dataStore.edit { it[VAULT_PIN_HASH_KEY] = hash }
    }

    suspend fun setVaultBiometric(enabled: Boolean) {
        context.dataStore.edit { it[VAULT_BIOMETRIC_KEY] = enabled }
    }
}
