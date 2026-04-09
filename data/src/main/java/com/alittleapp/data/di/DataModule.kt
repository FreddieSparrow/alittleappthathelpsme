package com.alittleapp.data.di

import android.content.Context
import androidx.room.Room
import com.alittleapp.data.db.AppDatabase
import com.alittleapp.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "alittleapp.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()
    @Provides fun provideClipboardDao(db: AppDatabase): ClipboardDao = db.clipboardDao()
}
