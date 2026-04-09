package com.alittleapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String = "",           // comma-separated tags
    val isPinned: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
