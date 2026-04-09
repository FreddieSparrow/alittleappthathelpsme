package com.alittleapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "clipboard_items")
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val isPinned: Boolean = false,
    val createdAt: Date = Date()
)
