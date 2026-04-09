package com.alittleapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alittleapp.data.db.converters.DateConverters
import com.alittleapp.data.db.dao.NoteDao
import com.alittleapp.data.db.dao.TaskDao
import com.alittleapp.data.db.dao.HabitDao
import com.alittleapp.data.db.dao.ClipboardDao
import com.alittleapp.data.db.entity.NoteEntity
import com.alittleapp.data.db.entity.TaskEntity
import com.alittleapp.data.db.entity.HabitEntity
import com.alittleapp.data.db.entity.HabitCompletionEntity
import com.alittleapp.data.db.entity.ClipboardEntity

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        ClipboardEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun clipboardDao(): ClipboardDao
}
