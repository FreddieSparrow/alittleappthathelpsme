package com.alittleapp.feature_vault

import android.content.Context
import com.alittleapp.feature_vault.crypto.VaultCrypto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val vaultFile = File(context.filesDir, "vault.enc")

    fun loadEntries(): List<VaultEntry> {
        if (!vaultFile.exists()) return emptyList()
        return try {
            val encJson = vaultFile.readText()
            val json = VaultCrypto.decrypt(encJson)
            val type = object : TypeToken<List<VaultEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun saveEntries(entries: List<VaultEntry>) {
        val json = gson.toJson(entries)
        val encrypted = VaultCrypto.encrypt(json)
        vaultFile.writeText(encrypted)
    }

    fun addEntry(entry: VaultEntry) {
        val entries = loadEntries().toMutableList()
        entries.add(entry)
        saveEntries(entries)
    }

    fun deleteEntry(id: String) {
        val entries = loadEntries().filter { it.id != id }
        saveEntries(entries)
    }

    fun updateEntry(updated: VaultEntry) {
        val entries = loadEntries().map { if (it.id == updated.id) updated else it }
        saveEntries(entries)
    }
}
