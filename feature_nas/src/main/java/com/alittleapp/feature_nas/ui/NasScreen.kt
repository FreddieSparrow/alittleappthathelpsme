package com.alittleapp.feature_nas.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.feature_nas.NasUiState
import com.alittleapp.feature_nas.NasViewModel
import com.alittleapp.feature_nas.webdav.NasEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NasScreen(viewModel: NasViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadFile(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NAS Browser")
                        if (state.isConnected) {
                            Text(state.currentPath, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    if (state.isConnected) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, "Up")
                        }
                    }
                },
                actions = {
                    if (state.isConnected) {
                        IconButton(onClick = { filePicker.launch("*/*") }) {
                            Icon(Icons.Default.Upload, "Upload")
                        }
                        IconButton(onClick = { showNewFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, "New folder")
                        }
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.LinkOff, "Disconnect")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Progress bars
            state.uploadProgress?.let {
                LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth())
                Text("Uploading… ${(it * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            state.downloadProgress?.let {
                LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth())
                Text("Downloading… ${(it * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Error / status snackbar-style
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { viewModel.clearError() }) { Icon(Icons.Default.Close, "Dismiss") }
                    }
                }
            }

            if (!state.isConnected) {
                NasConnectForm(state, viewModel)
            } else {
                NasFileList(state, viewModel)
            }
        }
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { name -> viewModel.createDirectory(name); showNewFolderDialog = false },
            onDismiss = { showNewFolderDialog = false }
        )
    }
}

@Composable
private fun NasConnectForm(state: NasUiState, viewModel: NasViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Storage, null, modifier = Modifier.align(Alignment.CenterHorizontally).size(48.dp),
            tint = MaterialTheme.colorScheme.primary)
        Text("Connect to UGREEN NAS", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("Enter your NAS WebDAV details. No data is sent to any external server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        HorizontalDivider()
        OutlinedTextField(value = state.host, onValueChange = viewModel::setHost,
            label = { Text("NAS IP Address (e.g. 192.168.1.100)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = state.port, onValueChange = viewModel::setPort,
                label = { Text("Port") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = state.davPath, onValueChange = viewModel::setDavPath,
                label = { Text("WebDAV path") }, modifier = Modifier.weight(2f), singleLine = true)
        }
        OutlinedTextField(value = state.username, onValueChange = viewModel::setUsername,
            label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.password, onValueChange = viewModel::setPassword,
            label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation())
        Button(
            onClick = { viewModel.connect() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.host.isNotBlank() && !state.isLoading
        ) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else { Icon(Icons.Default.Link, null); Spacer(Modifier.width(8.dp)); Text("Connect") }
        }
    }
}

@Composable
private fun NasFileList(state: NasUiState, viewModel: NasViewModel) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn {
        items(state.entries, key = { it.path }) { entry ->
            NasEntryRow(entry,
                onOpen = { if (entry.isDirectory) viewModel.navigateInto(entry) else viewModel.downloadFile(entry) },
                onDelete = { viewModel.deleteEntry(entry) }
            )
        }
    }
}

@Composable
private fun NasEntryRow(entry: NasEntry, onOpen: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            if (!entry.isDirectory) Text(formatSize(entry.size), style = MaterialTheme.typography.labelSmall)
        },
        leadingContent = {
            Icon(if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                null, tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (entry.isDirectory) "Open" else "Download") },
                        onClick = { onOpen(); showMenu = false },
                        leadingIcon = { Icon(if (entry.isDirectory) Icons.Default.FolderOpen else Icons.Default.Download, null) }
                    )
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun NewFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                placeholder = { Text("Folder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
    else -> "%.2f GB".format(bytes / (1024f * 1024f * 1024f))
}
