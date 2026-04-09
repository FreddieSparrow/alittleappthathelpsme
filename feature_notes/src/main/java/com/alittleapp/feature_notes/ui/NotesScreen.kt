package com.alittleapp.feature_notes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.data.db.entity.NoteEntity
import com.alittleapp.feature_notes.NoteViewModel
import com.alittleapp.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NoteViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    if (showEditor) {
        NoteEditorScreen(
            note = editingNote,
            onSave = { title, content, tags ->
                viewModel.saveNote(title, content, editingNote?.id ?: 0, tags)
                showEditor = false
                editingNote = null
            },
            onBack = { showEditor = false; editingNote = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Notes") },
                    actions = {
                        IconButton(onClick = { /* settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { editingNote = null; showEditor = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New note")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearchQuery
                )
                if (uiState.notes.isEmpty() && !uiState.isLoading) {
                    EmptyState("No notes yet. Tap + to create one.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { editingNote = note; showEditor = true },
                                onPin = { viewModel.togglePin(note) },
                                onDelete = { viewModel.deleteNote(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search notes…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
        elevation = CardDefaults.cardElevation(if (note.isPinned) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (note.tags.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = note.tags.split(",").joinToString(" ") { "#${it.trim()}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (note.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                onClick = { onPin(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { onDelete(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }
}
