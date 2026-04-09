package com.alittleapp.feature_vault

import java.util.Date
import java.util.UUID

enum class VaultEntryType { PASSWORD, NOTE, SECRET }

data class VaultEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: VaultEntryType,
    val title: String,
    // Stored encrypted on disk; decrypted in memory only when vault is unlocked
    val encryptedContent: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
