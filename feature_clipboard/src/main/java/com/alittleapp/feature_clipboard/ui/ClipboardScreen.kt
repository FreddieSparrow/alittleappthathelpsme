package com.alittleapp.feature_clipboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.data.db.entity.ClipboardEntity
import com.alittleapp.feature_clipboard.ClipboardViewModel
import com.alittleapp.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(viewModel: ClipboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clipboard") },
                actions = {
                    IconButton(onClick = viewModel::addFromSystemClipboard) {
                        Icon(Icons.Default.ContentPaste, "Paste from clipboard")
                    }
                    IconButton(onClick = viewModel::clearUnpinned) {
                        Icon(Icons.Default.DeleteSweep, "Clear unpinned")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add text")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-clear unpinned", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f))
                Switch(checked = state.autoClearEnabled, onCheckedChange = viewModel::setAutoClear)
            }
            HorizontalDivider()

            if (state.items.isEmpty()) {
                EmptyState("No clipboard history.\nTap paste icon to grab current clipboard, or + to add.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(state.items, key = { it.id }) { item ->
                        ClipboardItemRow(
                            item = item,
                            onCopy = { viewModel.copyToClipboard(item) },
                            onPin = { viewModel.togglePin(item) },
                            onDelete = { viewModel.delete(item) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddClipboardDialog(
            onAdd = { text -> viewModel.addManual(text); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipboardItemRow(
    item: ClipboardEntity,
    onCopy: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(item.content, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium)
        },
        leadingContent = {
            if (item.isPinned) Icon(Icons.Default.PushPin, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = { onCopy(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.isPinned) "Unpin" else "Pin") },
                        onClick = { onPin(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PushPin, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        },
        modifier = Modifier.combinedClickable(onClick = onCopy, onLongClick = { showMenu = true })
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun AddClipboardDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Clipboard") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it },
                placeholder = { Text("Enter text") }, modifier = Modifier.fillMaxWidth().height(120.dp))
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onAdd(text) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
