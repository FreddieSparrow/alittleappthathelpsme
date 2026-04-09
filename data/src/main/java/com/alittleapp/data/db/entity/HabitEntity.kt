package com.alittleapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: Long = 0xFF6650A4,   // stored as ARGB long
    val streakCount: Int = 0,
    val createdAt: Date = Date()
)

@Entity(tableName = "habit_completions")
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val completedAt: Date = Date()
)
