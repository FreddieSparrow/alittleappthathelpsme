package com.alittleapp.data.db.dao

import androidx.room.*
import com.alittleapp.data.db.entity.HabitEntity
import com.alittleapp.data.db.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedAt DESC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND completedAt >= :startOfDay AND completedAt < :endOfDay)")
    suspend fun isCompletedToday(habitId: Long, startOfDay: Date, endOfDay: Date): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Insert
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completedAt >= :startOfDay AND completedAt < :endOfDay")
    suspend fun deleteCompletionToday(habitId: Long, startOfDay: Date, endOfDay: Date)
}
