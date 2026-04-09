package com.alittleapp.data.db.dao

import androidx.room.*
import com.alittleapp.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfDay AND dueDate < :endOfDay AND isCompleted = 0")
    fun getTodayTasks(startOfDay: Date, endOfDay: Date): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)
}
