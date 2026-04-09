package com.alittleapp.data.repository

import com.alittleapp.data.db.dao.HabitDao
import com.alittleapp.data.db.entity.HabitCompletionEntity
import com.alittleapp.data.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(private val dao: HabitDao) {

    fun getAllHabits(): Flow<List<HabitEntity>> = dao.getAllHabits()

    suspend fun saveHabit(habit: HabitEntity): Long {
        return if (habit.id == 0L) dao.insertHabit(habit)
        else { dao.updateHabit(habit); habit.id }
    }

    suspend fun deleteHabit(habit: HabitEntity) = dao.deleteHabit(habit)

    suspend fun toggleTodayCompletion(habit: HabitEntity) {
        val (start, end) = todayRange()
        if (dao.isCompletedToday(habit.id, start, end)) {
            dao.deleteCompletionToday(habit.id, start, end)
            dao.updateHabit(habit.copy(streakCount = maxOf(0, habit.streakCount - 1)))
        } else {
            dao.insertCompletion(HabitCompletionEntity(habitId = habit.id))
            dao.updateHabit(habit.copy(streakCount = habit.streakCount + 1))
        }
    }

    suspend fun isCompletedToday(habitId: Long): Boolean {
        val (start, end) = todayRange()
        return dao.isCompletedToday(habitId, start, end)
    }

    private fun todayRange(): Pair<Date, Date> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.time
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start to cal.time
    }
}
