package com.alittleapp.feature_notes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alittleapp.data.db.entity.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity?,
    onSave: (title: String, content: String, tags: String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var tags by remember { mutableStateOf(note?.tags ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(title, content, tags) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.titleLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Write something…") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("Tags (comma separated)") },
                singleLine = true,
                label = { Text("Tags") }
            )
        }
    }
}
