package com.alittleapp.data.repository

import com.alittleapp.data.db.dao.TaskDao
import com.alittleapp.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {

    fun getAllTasks(): Flow<List<TaskEntity>> = dao.getAllTasks()

    fun getPendingTasks(): Flow<List<TaskEntity>> = dao.getPendingTasks()

    fun getTodayTasks(): Flow<List<TaskEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.time
        return dao.getTodayTasks(startOfDay, endOfDay)
    }

    fun searchTasks(query: String): Flow<List<TaskEntity>> = dao.searchTasks(query)

    suspend fun saveTask(task: TaskEntity): Long {
        return if (task.id == 0L) dao.insertTask(task)
        else { dao.updateTask(task); task.id }
    }

    suspend fun toggleCompleted(task: TaskEntity) =
        dao.setCompleted(task.id, !task.isCompleted)

    suspend fun deleteTask(task: TaskEntity) = dao.deleteTask(task)
}
