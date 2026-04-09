package com.alittleapp.feature_webui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebUiState(
    val isLive: Boolean = false,
    val url: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WebUiViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val server: WebUiServer
) : ViewModel() {

    private val _state = MutableStateFlow(WebUiState())
    val state: StateFlow<WebUiState> = _state.asStateFlow()

    fun goLive() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val port = server.start()
                val ip = server.getLocalIp(context)
                val url = "http://$ip:$port"
                _state.update { it.copy(isLive = true, url = url, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLive = false, isLoading = false, error = "Failed to start: ${e.message}") }
            }
        }
    }

    fun stopLive() {
        server.stop()
        _state.update { it.copy(isLive = false, url = "") }
    }

    fun openInBrowser() {
        val url = _state.value.url.ifBlank { return }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun onCleared() { server.stop(); super.onCleared() }
}
