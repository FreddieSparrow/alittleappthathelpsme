package com.alittleapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 0,           // 0=normal, 1=high, 2=urgent
    val dueDate: Date? = null,
    val tags: String = "",
    val createdAt: Date = Date()
)
