package com.alittleapp.feature_utils.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var length by remember { mutableStateOf(16f) }
    var useUppercase by remember { mutableStateOf(true) }
    var useLowercase by remember { mutableStateOf(true) }
    var useNumbers by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    fun generate() {
        copied = false
        val chars = buildString {
            if (useUppercase) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (useLowercase) append("abcdefghijklmnopqrstuvwxyz")
            if (useNumbers) append("0123456789")
            if (useSymbols) append("!@#\$%^&*()-_=+[]{}|;:,.<>?")
        }
        if (chars.isEmpty()) { password = ""; return }
        val rng = SecureRandom()
        password = (1..length.toInt()).map { chars[rng.nextInt(chars.length)] }.joinToString("")
    }

    LaunchedEffect(Unit) { generate() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Generator") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Password display
            OutlinedTextField(
                value = password,
                onValueChange = {},
                readOnly = true,
                label = { Text("Generated Password") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Password", password))
                            copied = true
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy")
                        }
                        IconButton(onClick = ::generate) {
                            Icon(Icons.Default.Refresh, "Regenerate")
                        }
                    }
                }
            )
            if (copied) {
                Text("Copied to clipboard!", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
            }

            // Length slider
            Column {
                Text("Length: ${length.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(value = length, onValueChange = { length = it; generate() },
                    valueRange = 8f..64f, steps = 55)
            }

            // Character set options
            @Composable
            fun OptionRow(label: String, checked: Boolean, onToggle: () -> Unit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label)
                    Switch(checked = checked, onCheckedChange = { onToggle(); generate() })
                }
            }

            OptionRow("Uppercase (A-Z)", useUppercase) { useUppercase = !useUppercase }
            OptionRow("Lowercase (a-z)", useLowercase) { useLowercase = !useLowercase }
            OptionRow("Numbers (0-9)", useNumbers) { useNumbers = !useNumbers }
            OptionRow("Symbols (!@#…)", useSymbols) { useSymbols = !useSymbols }

            // Strength indicator
            val strength = when {
                password.length >= 20 && useSymbols && useNumbers -> "Strong"
                password.length >= 12 -> "Medium"
                password.isNotEmpty() -> "Weak"
                else -> ""
            }
            if (strength.isNotEmpty()) {
                val color = when (strength) {
                    "Strong" -> MaterialTheme.colorScheme.primary
                    "Medium" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                Text("Strength: $strength", color = color, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
