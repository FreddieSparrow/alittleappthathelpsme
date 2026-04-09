package com.alittleapp.feature_utils.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.*

private enum class CalcMode { NORMAL, SCIENTIFIC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(listOf<String>()) }
    var mode by remember { mutableStateOf(CalcMode.NORMAL) }
    var newInput by remember { mutableStateOf(true) }

    fun onNumber(n: String) {
        display = if (newInput || display == "0") { newInput = false; n } else display + n
    }

    fun onDecimal() {
        if (newInput) { display = "0."; newInput = false; return }
        if (!display.contains('.')) display += "."
    }

    fun onOperator(op: String) {
        expression = "$display $op"
        newInput = true
    }

    fun calculate() {
        val expr = expression
        if (expr.isBlank()) return
        val parts = expr.trim().split(" ")
        if (parts.size < 2) return
        val a = parts[0].toDoubleOrNull() ?: return
        val op = parts[1]
        val b = display.toDoubleOrNull() ?: return
        val result = when (op) {
            "+" -> a + b; "-" -> a - b; "×" -> a * b
            "÷" -> if (b != 0.0) a / b else Double.NaN
            "%" -> a % b; "^" -> a.pow(b); else -> b
        }
        val resultStr = if (result == result.toLong().toDouble()) result.toLong().toString()
        else "%.10g".format(result)
        history = listOf("$a $op $b = $resultStr") + history.take(49)
        expression = ""
        display = resultStr
        newInput = true
    }

    fun onScientific(fn: String) {
        val v = display.toDoubleOrNull() ?: return
        val result = when (fn) {
            "sin" -> sin(Math.toRadians(v)); "cos" -> cos(Math.toRadians(v))
            "tan" -> tan(Math.toRadians(v)); "√" -> sqrt(v)
            "x²" -> v.pow(2); "log" -> log10(v); "ln" -> ln(v)
            "1/x" -> if (v != 0.0) 1.0 / v else Double.NaN
            "π" -> PI; "e" -> E; "!" -> factorial(v.toLong()).toDouble()
            else -> v
        }
        display = if (result == result.toLong().toDouble()) result.toLong().toString()
        else "%.10g".format(result)
        newInput = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculator") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    TextButton(onClick = { mode = if (mode == CalcMode.NORMAL) CalcMode.SCIENTIFIC else CalcMode.NORMAL }) {
                        Text(if (mode == CalcMode.NORMAL) "SCI" else "BASIC")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Display
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    if (expression.isNotBlank()) {
                        Text(expression, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    Text(display, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.End,
                        maxLines = 1)
                }
            }

            // Scientific row
            if (mode == CalcMode.SCIENTIFIC) {
                Row(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("sin", "cos", "tan", "√", "x²", "log", "ln", "1/x", "π", "e").forEach { fn ->
                        FilledTonalButton(
                            onClick = { onScientific(fn) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) { Text(fn, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Numpad
            val rows = listOf(
                listOf("C", "±", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "⌫", "=")
            )

            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { key ->
                            val isOperator = key in listOf("÷", "×", "-", "+", "=", "^")
                            val isAction = key in listOf("C", "±", "%", "⌫")
                            Button(
                                onClick = {
                                    when (key) {
                                        in "0".."9" -> onNumber(key)
                                        "." -> onDecimal()
                                        "C" -> { display = "0"; expression = ""; newInput = true }
                                        "±" -> display = if (display.startsWith("-")) display.drop(1) else "-$display"
                                        "⌫" -> display = if (display.length > 1) display.dropLast(1) else "0"
                                        "=" -> calculate()
                                        else -> onOperator(key)
                                    }
                                },
                                modifier = Modifier.weight(if (key == "0") 1f else 1f).aspectRatio(1f),
                                colors = if (key == "=") ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                else if (isOperator) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                else if (isAction) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                else ButtonDefaults.filledTonalButtonColors()
                            ) { Text(key, style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                }
            }

            // History
            if (history.isNotEmpty()) {
                HorizontalDivider()
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(history) { entry ->
                        Text(entry, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

private fun factorial(n: Long): Long {
    if (n <= 1) return 1
    if (n > 20) return Long.MAX_VALUE
    return n * factorial(n - 1)
}
