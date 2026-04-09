package com.alittleapp.feature_timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class TimerMode { STOPWATCH, COUNTDOWN, POMODORO }
enum class TimerStatus { IDLE, RUNNING, PAUSED, DONE }

private const val POMODORO_WORK_SECS = 25 * 60
private const val POMODORO_SHORT_BREAK_SECS = 5 * 60
private const val POMODORO_LONG_BREAK_SECS = 15 * 60

data class TimerUiState(
    val mode: TimerMode = TimerMode.STOPWATCH,
    val status: TimerStatus = TimerStatus.IDLE,
    val elapsedMs: Long = 0L,
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val pomodoroPhase: String = "Work",
    val pomodoroRound: Int = 1,
    val countdownHours: Int = 0,
    val countdownMinutes: Int = 0,
    val countdownSeconds: Int = 0,
    val laps: List<Long> = emptyList()
)

@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var startTime = 0L
    private var pausedElapsed = 0L
    private var countdownRemaining = 0L

    fun setMode(mode: TimerMode) {
        stop()
        _state.value = TimerUiState(mode = mode)
    }

    fun setCountdownTime(h: Int, m: Int, s: Int) {
        _state.update { it.copy(countdownHours = h, countdownMinutes = m, countdownSeconds = s) }
    }

    fun start() {
        val s = _state.value
        when (s.mode) {
            TimerMode.STOPWATCH -> startStopwatch()
            TimerMode.COUNTDOWN -> startCountdown()
            TimerMode.POMODORO -> startPomodoro()
        }
    }

    fun pause() {
        timerJob?.cancel()
        val s = _state.value
        pausedElapsed = s.elapsedMs
        countdownRemaining = s.remainingMs
        _state.update { it.copy(status = TimerStatus.PAUSED) }
    }

    fun resume() {
        val s = _state.value
        when (s.mode) {
            TimerMode.STOPWATCH -> {
                startTime = System.currentTimeMillis() - pausedElapsed
                startTick { System.currentTimeMillis() - startTime }
            }
            TimerMode.COUNTDOWN, TimerMode.POMODORO -> {
                startTime = System.currentTimeMillis()
                startCountdownTick(countdownRemaining)
            }
        }
        _state.update { it.copy(status = TimerStatus.RUNNING) }
    }

    fun stop() {
        timerJob?.cancel()
        pausedElapsed = 0L
        countdownRemaining = 0L
        _state.update { it.copy(status = TimerStatus.IDLE, elapsedMs = 0, remainingMs = 0, laps = emptyList()) }
    }

    fun lap() {
        val s = _state.value
        if (s.mode == TimerMode.STOPWATCH && s.status == TimerStatus.RUNNING) {
            _state.update { it.copy(laps = listOf(s.elapsedMs) + it.laps) }
        }
    }

    private fun startStopwatch() {
        startTime = System.currentTimeMillis() - pausedElapsed
        startTick { System.currentTimeMillis() - startTime }
        _state.update { it.copy(status = TimerStatus.RUNNING) }
    }

    private fun startCountdown() {
        val s = _state.value
        val totalMs = ((s.countdownHours * 3600 + s.countdownMinutes * 60 + s.countdownSeconds) * 1000).toLong()
        if (totalMs <= 0) return
        countdownRemaining = totalMs
        startTime = System.currentTimeMillis()
        _state.update { it.copy(status = TimerStatus.RUNNING, totalMs = totalMs, remainingMs = totalMs) }
        startCountdownTick(totalMs)
    }

    private fun startPomodoro() {
        val s = _state.value
        val phaseMs = when (s.pomodoroPhase) {
            "Work" -> POMODORO_WORK_SECS * 1000L
            "Short Break" -> POMODORO_SHORT_BREAK_SECS * 1000L
            else -> POMODORO_LONG_BREAK_SECS * 1000L
        }
        countdownRemaining = phaseMs
        startTime = System.currentTimeMillis()
        _state.update { it.copy(status = TimerStatus.RUNNING, totalMs = phaseMs, remainingMs = phaseMs) }
        startCountdownTick(phaseMs)
    }

    private fun startTick(elapsed: () -> Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(elapsedMs = elapsed()) }
                delay(50)
            }
        }
    }

    private fun startCountdownTick(initialMs: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val remaining = initialMs - (System.currentTimeMillis() - startTime)
                if (remaining <= 0) {
                    _state.update { it.copy(remainingMs = 0, status = TimerStatus.DONE) }
                    onTimerDone()
                    break
                }
                _state.update { it.copy(remainingMs = remaining) }
                delay(50)
            }
        }
    }

    private fun onTimerDone() {
        val s = _state.value
        if (s.mode == TimerMode.POMODORO) {
            val (nextPhase, nextRound) = when {
                s.pomodoroPhase == "Work" && s.pomodoroRound % 4 == 0 -> "Long Break" to s.pomodoroRound
                s.pomodoroPhase == "Work" -> "Short Break" to s.pomodoroRound
                else -> "Work" to (s.pomodoroRound + 1)
            }
            _state.update { it.copy(pomodoroPhase = nextPhase, pomodoroRound = nextRound, status = TimerStatus.IDLE) }
        }
    }

    override fun onCleared() { timerJob?.cancel(); super.onCleared() }
}

fun Long.toTimeString(): String {
    val totalSec = this / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val ms = (this % 1000) / 10
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d.%02d".format(m, s, ms)
}
