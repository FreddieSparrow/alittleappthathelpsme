package com.alittleapp.feature_notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alittleapp.data.db.entity.NoteEntity
import com.alittleapp.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotesUiState> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllNotes()
            else repository.searchNotes(query)
        }
        .map { notes -> NotesUiState(notes = notes, searchQuery = _searchQuery.value) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesUiState(isLoading = true))

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun saveNote(title: String, content: String, id: Long = 0, tags: String = "") {
        viewModelScope.launch {
            repository.saveNote(NoteEntity(id = id, title = title, content = content, tags = tags))
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.saveNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { repository.deleteNote(note) }
    }
}
