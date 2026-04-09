package com.alittleapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alittleapp.data.db.entity.NoteEntity
import com.alittleapp.data.db.entity.TaskEntity
import com.alittleapp.data.repository.NoteRepository
import com.alittleapp.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val recentNotes: List<NoteEntity> = emptyList(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val pendingTaskCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    noteRepo: NoteRepository,
    taskRepo: TaskRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        noteRepo.getRecentNotes(4),
        taskRepo.getTodayTasks(),
        taskRepo.getPendingTasks()
    ) { notes, todayTasks, pending ->
        DashboardUiState(
            recentNotes = notes,
            todayTasks = todayTasks.take(5),
            pendingTaskCount = pending.count { !it.isCompleted }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
