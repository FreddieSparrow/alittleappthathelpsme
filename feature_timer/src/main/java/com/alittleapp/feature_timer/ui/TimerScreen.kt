package com.alittleapp.feature_timer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alittleapp.feature_timer.TimerMode
import com.alittleapp.feature_timer.TimerStatus
import com.alittleapp.feature_timer.TimerViewModel
import com.alittleapp.feature_timer.toTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Timers") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Mode tabs
            TabRow(selectedTabIndex = state.mode.ordinal) {
                TimerMode.values().forEachIndexed { index, mode ->
                    Tab(
                        selected = state.mode.ordinal == index,
                        onClick = { viewModel.setMode(mode) },
                        text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            when (state.mode) {
                TimerMode.STOPWATCH -> StopwatchPanel(state, viewModel)
                TimerMode.COUNTDOWN -> CountdownPanel(state, viewModel)
                TimerMode.POMODORO -> PomodoroPanel(state, viewModel)
            }
        }
    }
}

@Composable
private fun StopwatchPanel(
    state: com.alittleapp.feature_timer.TimerUiState,
    viewModel: TimerViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(state.elapsedMs.toTimeString(), fontSize = 56.sp, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state.status) {
                TimerStatus.IDLE -> {
                    Button(onClick = viewModel::start) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Start") }
                }
                TimerStatus.RUNNING -> {
                    OutlinedButton(onClick = viewModel::lap) { Text("Lap") }
                    Button(onClick = viewModel::pause) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(4.dp)); Text("Pause") }
                }
                TimerStatus.PAUSED -> {
                    OutlinedButton(onClick = viewModel::stop) { Text("Reset") }
                    Button(onClick = viewModel::resume) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Resume") }
                }
                else -> {}
            }
        }
        if (state.laps.isNotEmpty()) {
            HorizontalDivider()
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(state.laps.size) { i ->
                    Text("Lap ${state.laps.size - i}  ${state.laps[i].toTimeString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun CountdownPanel(
    state: com.alittleapp.feature_timer.TimerUiState,
    viewModel: TimerViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val progress = if (state.totalMs > 0) state.remainingMs.toFloat() / state.totalMs else 1f
        val color = MaterialTheme.colorScheme.primary

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth / 2
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(strokeWidth), topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.remainingMs.toTimeString(), fontSize = 32.sp)
                if (state.status == TimerStatus.DONE) Text("Done!", color = MaterialTheme.colorScheme.primary)
            }
        }

        if (state.status == TimerStatus.IDLE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Hours" to state.countdownHours, "Min" to state.countdownMinutes, "Sec" to state.countdownSeconds)
                    .forEachIndexed { i, (label, value) ->
                        OutlinedTextField(
                            value = value.toString(), onValueChange = { v ->
                                val n = v.toIntOrNull()?.coerceIn(0, if (i == 0) 99 else 59) ?: return@OutlinedTextField
                                when (i) {
                                    0 -> viewModel.setCountdownTime(n, state.countdownMinutes, state.countdownSeconds)
                                    1 -> viewModel.setCountdownTime(state.countdownHours, n, state.countdownSeconds)
                                    2 -> viewModel.setCountdownTime(state.countdownHours, state.countdownMinutes, n)
                                }
                            },
                            label = { Text(label) }, modifier = Modifier.weight(1f), singleLine = true
                        )
                    }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state.status) {
                TimerStatus.IDLE, TimerStatus.DONE ->
                    Button(onClick = viewModel::start) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Start") }
                TimerStatus.RUNNING -> {
                    OutlinedButton(onClick = viewModel::stop) { Text("Reset") }
                    Button(onClick = viewModel::pause) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(4.dp)); Text("Pause") }
                }
                TimerStatus.PAUSED -> {
                    OutlinedButton(onClick = viewModel::stop) { Text("Reset") }
                    Button(onClick = viewModel::resume) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Resume") }
                }
            }
        }
    }
}

@Composable
private fun PomodoroPanel(
    state: com.alittleapp.feature_timer.TimerUiState,
    viewModel: TimerViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(
            containerColor = when (state.pomodoroPhase) {
                "Work" -> MaterialTheme.colorScheme.primaryContainer
                "Short Break" -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )) {
            Text(state.pomodoroPhase, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }
        Text("Round ${state.pomodoroRound}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(state.remainingMs.toTimeString(), fontSize = 56.sp, textAlign = TextAlign.Center)
        if (state.totalMs > 0) LinearProgressIndicator(
            progress = { state.remainingMs.toFloat() / state.totalMs },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state.status) {
                TimerStatus.IDLE, TimerStatus.DONE ->
                    Button(onClick = viewModel::start, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp))
                        Text(if (state.status == TimerStatus.DONE) "Next Phase" else "Start ${state.pomodoroPhase}")
                    }
                TimerStatus.RUNNING ->
                    Button(onClick = viewModel::pause, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Pause, null); Spacer(Modifier.width(4.dp)); Text("Pause")
                    }
                TimerStatus.PAUSED ->
                    Button(onClick = viewModel::resume, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Resume")
                    }
            }
        }
        if (state.status != TimerStatus.IDLE) {
            TextButton(onClick = viewModel::stop) { Text("Reset") }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Pomodoro Technique", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text("25 min Work → 5 min Break → repeat × 4 → 15 min Long Break",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
