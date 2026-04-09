package com.alittleapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.data.db.entity.NoteEntity
import com.alittleapp.data.db.entity.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToNotes: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToUtils: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToNas: () -> Unit,
    onNavigateToVault: () -> Unit = {},
    onNavigateToTimer: () -> Unit = {},
    onNavigateToClipboard: () -> Unit = {},
    onNavigateToWebUi: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick actions
            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    item { QuickActionChip(Icons.Default.Note, "Notes") { onNavigateToNotes() } }
                    item { QuickActionChip(Icons.Default.CheckCircle, "Tasks") { onNavigateToTasks() } }
                    item { QuickActionChip(Icons.Default.Tune, "Tools") { onNavigateToUtils() } }
                    item { QuickActionChip(Icons.Default.Send, "Transfer") { onNavigateToTransfer() } }
                    item { QuickActionChip(Icons.Default.Storage, "NAS") { onNavigateToNas() } }
                    item { QuickActionChip(Icons.Default.Lock, "Vault") { onNavigateToVault() } }
                    item { QuickActionChip(Icons.Default.Timer, "Timers") { onNavigateToTimer() } }
                    item { QuickActionChip(Icons.Default.ContentPaste, "Clipboard") { onNavigateToClipboard() } }
                    item { QuickActionChip(Icons.Default.Wifi, "Go Live") { onNavigateToWebUi() } }
                }
            }

            // Today's tasks
            item {
                SectionCard(
                    title = "Today's Tasks",
                    icon = Icons.Default.Today,
                    badge = state.todayTasks.size.takeIf { it > 0 }?.toString(),
                    onSeeAll = onNavigateToTasks
                ) {
                    if (state.todayTasks.isEmpty()) {
                        Text("No tasks for today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    } else {
                        state.todayTasks.forEach { task -> TaskRowCompact(task) }
                    }
                }
            }

            // Recent notes
            item {
                SectionCard(
                    title = "Recent Notes",
                    icon = Icons.Default.Description,
                    onSeeAll = onNavigateToNotes
                ) {
                    if (state.recentNotes.isEmpty()) {
                        Text("No notes yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.recentNotes) { note -> NoteCardCompact(note) }
                        }
                    }
                }
            }

            // Stats overview
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Pending tasks",
                        value = state.pendingTaskCount.toString(),
                        icon = Icons.Default.Pending
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Notes",
                        value = state.recentNotes.size.let { if (it >= 4) "4+" else it.toString() },
                        icon = Icons.Default.Notes
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    ElevatedFilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    onSeeAll: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                badge?.let {
                    Badge { Text(it) }
                    Spacer(Modifier.width(8.dp))
                }
                onSeeAll?.let {
                    TextButton(onClick = it, contentPadding = PaddingValues(0.dp)) { Text("See all") }
                }
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun TaskRowCompact(task: TaskEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            null,
            tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            task.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NoteCardCompact(note: NoteEntity) {
    ElevatedCard(modifier = Modifier.width(160.dp).height(100.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (note.title.isNotBlank()) {
                Text(note.title, style = MaterialTheme.typography.labelLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
            }
            Text(note.content, style = MaterialTheme.typography.bodySmall,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String, icon: ImageVector) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
