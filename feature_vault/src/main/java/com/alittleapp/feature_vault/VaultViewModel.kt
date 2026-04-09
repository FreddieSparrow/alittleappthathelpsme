package com.alittleapp.feature_vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alittleapp.data.preferences.AppPreferences
import com.alittleapp.feature_vault.crypto.VaultCrypto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VaultUiState {
    object PinSetup : VaultUiState()
    object Locked : VaultUiState()
    data class Unlocked(val entries: List<VaultEntry>) : VaultUiState()
    data class Error(val message: String) : VaultUiState()
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repo: VaultRepository,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<VaultUiState>(VaultUiState.Locked)
    val state: StateFlow<VaultUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.vaultPinHash.first().let { pinHash ->
                _state.value = if (pinHash.isNullOrBlank()) VaultUiState.PinSetup else VaultUiState.Locked
            }
        }
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            prefs.setVaultPinHash(VaultCrypto.hashPin(pin))
            _state.value = VaultUiState.Unlocked(repo.loadEntries())
        }
    }

    fun unlock(pin: String) {
        viewModelScope.launch {
            val storedHash = prefs.vaultPinHash.first() ?: return@launch
            if (VaultCrypto.verifyPin(pin, storedHash)) {
                _state.value = VaultUiState.Unlocked(repo.loadEntries())
            } else {
                _state.value = VaultUiState.Error("Incorrect PIN")
                kotlinx.coroutines.delay(1500)
                _state.value = VaultUiState.Locked
            }
        }
    }

    fun lock() { _state.value = VaultUiState.Locked }

    fun addEntry(type: VaultEntryType, title: String, content: String) {
        viewModelScope.launch {
            val encrypted = VaultCrypto.encrypt(content)
            val entry = VaultEntry(type = type, title = title, encryptedContent = encrypted)
            repo.addEntry(entry)
            _state.value = VaultUiState.Unlocked(repo.loadEntries())
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            repo.deleteEntry(id)
            _state.value = VaultUiState.Unlocked(repo.loadEntries())
        }
    }

    fun decryptEntry(entry: VaultEntry): String {
        return try { VaultCrypto.decrypt(entry.encryptedContent) }
        catch (e: Exception) { "Decryption failed: ${e.message}" }
    }
}
