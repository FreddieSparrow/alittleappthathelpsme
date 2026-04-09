package com.alittleapp.feature_tasks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.data.db.entity.HabitEntity
import com.alittleapp.data.db.entity.TaskEntity
import com.alittleapp.feature_tasks.TaskFilter
import com.alittleapp.feature_tasks.TaskViewModel
import com.alittleapp.ui.components.EmptyState
import com.alittleapp.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TaskViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddTask by remember { mutableStateOf(false) }
    var showAddHabit by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tasks & Habits") })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(onClick = { showAddHabit = true }) {
                    Icon(Icons.Default.Loop, contentDescription = "Add habit")
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(onClick = { showAddTask = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add task")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter chips
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.values().forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                // Habits section
                if (uiState.habits.isNotEmpty()) {
                    item { SectionHeader("Daily Habits") }
                    items(uiState.habits, key = { "habit_${it.id}" }) { habit ->
                        HabitRow(
                            habit = habit,
                            isCompletedToday = uiState.completedHabitIds.contains(habit.id),
                            onToggle = { viewModel.toggleHabitToday(habit) },
                            onDelete = { viewModel.deleteHabit(habit) }
                        )
                    }
                }

                // Tasks section
                item { SectionHeader("Tasks") }
                if (uiState.tasks.isEmpty()) {
                    item { EmptyState("No tasks here.", modifier = Modifier.height(120.dp)) }
                } else {
                    items(uiState.tasks, key = { "task_${it.id}" }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { viewModel.toggleTask(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddTask) {
        AddItemDialog(
            title = "New Task",
            hint = "Task title",
            onConfirm = { text -> viewModel.addTask(text); showAddTask = false },
            onDismiss = { showAddTask = false }
        )
    }

    if (showAddHabit) {
        AddItemDialog(
            title = "New Habit",
            hint = "Habit name",
            onConfirm = { text -> viewModel.addHabit(text); showAddHabit = false },
            onDismiss = { showAddHabit = false }
        )
    }
}

@Composable
private fun TaskRow(task: TaskEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = task.title,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent = {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun HabitRow(
    habit: HabitEntity,
    isCompletedToday: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(habit.name) },
        supportingContent = { Text("🔥 ${habit.streakCount} day streak") },
        leadingContent = {
            Checkbox(checked = isCompletedToday, onCheckedChange = { onToggle() })
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun AddItemDialog(
    title: String,
    hint: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(hint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
