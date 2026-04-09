package com.alittleapp.feature_vault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.feature_vault.VaultEntry
import com.alittleapp.feature_vault.VaultEntryType
import com.alittleapp.feature_vault.VaultUiState
import com.alittleapp.feature_vault.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: VaultViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is VaultUiState.PinSetup -> PinSetupScreen(onSetPin = viewModel::setupPin)
        is VaultUiState.Locked -> PinEntryScreen(onUnlock = viewModel::unlock, isError = false)
        is VaultUiState.Error -> PinEntryScreen(onUnlock = viewModel::unlock, isError = true, error = s.message)
        is VaultUiState.Unlocked -> VaultContentsScreen(entries = s.entries, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupScreen(onSetPin: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Set Vault PIN") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(32.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text("Create a PIN to protect your vault", textAlign = TextAlign.Center)
            OutlinedTextField(
                value = pin, onValueChange = { pin = it; mismatch = false },
                label = { Text("PIN (4–12 digits)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it; mismatch = false },
                label = { Text("Confirm PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = mismatch
            )
            if (mismatch) Text("PINs do not match", color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { if (pin == confirm && pin.length >= 4) onSetPin(pin) else mismatch = true },
                modifier = Modifier.fillMaxWidth(), enabled = pin.isNotBlank() && confirm.isNotBlank()
            ) { Text("Create Vault") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinEntryScreen(onUnlock: (String) -> Unit, isError: Boolean, error: String = "") {
    var pin by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Vault") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(32.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text("Enter your PIN to unlock", textAlign = TextAlign.Center)
            OutlinedTextField(
                value = pin, onValueChange = { pin = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = isError
            )
            if (isError) Text(error, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { onUnlock(pin); pin = "" },
                modifier = Modifier.fillMaxWidth(), enabled = pin.isNotBlank()
            ) { Text("Unlock") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultContentsScreen(entries: List<VaultEntry>, viewModel: VaultViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var revealEntry by remember { mutableStateOf<VaultEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault") },
                actions = {
                    IconButton(onClick = viewModel::lock) {
                        Icon(Icons.Default.LockOpen, "Lock vault")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, "Add entry")
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No entries. Tap + to add secrets, passwords, or notes.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(entries, key = { it.id }) { entry ->
                    VaultEntryRow(entry,
                        onReveal = { revealEntry = entry },
                        onDelete = { viewModel.deleteEntry(entry.id) }
                    )
                }
            }
        }
    }

    if (showAdd) AddVaultEntryDialog(
        onAdd = { type, title, content -> viewModel.addEntry(type, title, content); showAdd = false },
        onDismiss = { showAdd = false }
    )

    revealEntry?.let { entry ->
        val content = remember(entry.id) { viewModel.decryptEntry(entry) }
        AlertDialog(
            onDismissRequest = { revealEntry = null },
            title = { Text(entry.title) },
            text = { SelectionContainer { Text(content) } },
            confirmButton = { TextButton(onClick = { revealEntry = null }) { Text("Done") } }
        )
    }
}

@Composable
private fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer { content() }
}

@Composable
private fun VaultEntryRow(entry: VaultEntry, onReveal: () -> Unit, onDelete: () -> Unit) {
    val icon = when (entry.type) {
        VaultEntryType.PASSWORD -> Icons.Default.Password
        VaultEntryType.NOTE -> Icons.Default.StickyNote2
        VaultEntryType.SECRET -> Icons.Default.Key
    }
    ListItem(
        headlineContent = { Text(entry.title) },
        supportingContent = { Text(entry.type.name.lowercase().replaceFirstChar { it.uppercase() }) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Row {
                IconButton(onClick = onReveal) { Icon(Icons.Default.Visibility, "Reveal") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun AddVaultEntryDialog(
    onAdd: (VaultEntryType, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(VaultEntryType.PASSWORD) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Vault Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VaultEntryType.values().forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t },
                            label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text(if (type == VaultEntryType.NOTE) "Note" else "Secret value") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    visualTransformation = if (type != VaultEntryType.NOTE) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank() && content.isNotBlank()) onAdd(type, title, content) },
                enabled = title.isNotBlank() && content.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
