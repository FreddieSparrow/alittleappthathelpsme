package com.alittleapp.feature_tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alittleapp.data.db.entity.HabitEntity
import com.alittleapp.data.db.entity.TaskEntity
import com.alittleapp.data.repository.HabitRepository
import com.alittleapp.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskFilter { ALL, TODAY, PENDING }

data class TasksUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val completedHabitIds: Set<Long> = emptySet(),
    val filter: TaskFilter = TaskFilter.TODAY,
    val isLoading: Boolean = true
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val habitRepo: HabitRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.TODAY)

    private val _habits = habitRepo.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TasksUiState> = combine(
        _filter.flatMapLatest { filter ->
            when (filter) {
                TaskFilter.ALL -> taskRepo.getAllTasks()
                TaskFilter.TODAY -> taskRepo.getTodayTasks()
                TaskFilter.PENDING -> taskRepo.getPendingTasks()
            }
        },
        _habits,
        _filter
    ) { tasks, habits, filter ->
        TasksUiState(tasks = tasks, habits = habits, filter = filter, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun setFilter(filter: TaskFilter) { _filter.value = filter }

    fun addTask(title: String, description: String = "", priority: Int = 0) {
        viewModelScope.launch {
            taskRepo.saveTask(TaskEntity(title = title, description = description, priority = priority))
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch { taskRepo.toggleCompleted(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepo.deleteTask(task) }
    }

    fun addHabit(name: String, colorArgb: Long = 0xFF6650A4) {
        viewModelScope.launch {
            habitRepo.saveHabit(HabitEntity(name = name, color = colorArgb))
        }
    }

    fun toggleHabitToday(habit: HabitEntity) {
        viewModelScope.launch { habitRepo.toggleTodayCompletion(habit) }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { habitRepo.deleteHabit(habit) }
    }
}
