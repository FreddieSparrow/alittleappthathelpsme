package com.alittleapp.feature_nas

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alittleapp.feature_nas.webdav.NasConfig
import com.alittleapp.feature_nas.webdav.NasEntry
import com.alittleapp.feature_nas.webdav.WebDavClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class NasUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val currentPath: String = "/",
    val entries: List<NasEntry> = emptyList(),
    val uploadProgress: Float? = null,   // null = not uploading
    val downloadProgress: Float? = null,
    val error: String? = null,
    val statusMessage: String? = null,
    // Connection form
    val host: String = "",
    val port: String = "5005",
    val username: String = "",
    val password: String = "",
    val davPath: String = "/dav"
)

@HiltViewModel
class NasViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NasUiState())
    val state: StateFlow<NasUiState> = _state.asStateFlow()

    private var client: WebDavClient? = null
    private val pathStack = ArrayDeque<String>()

    // Form field updaters
    fun setHost(v: String) = _state.update { it.copy(host = v, error = null) }
    fun setPort(v: String) = _state.update { it.copy(port = v) }
    fun setUsername(v: String) = _state.update { it.copy(username = v) }
    fun setPassword(v: String) = _state.update { it.copy(password = v) }
    fun setDavPath(v: String) = _state.update { it.copy(davPath = v) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun connect() {
        val s = _state.value
        val baseUrl = "http://${s.host.trim()}:${s.port.trim()}${s.davPath.trim()}"
        client?.close()
        client = WebDavClient(NasConfig(baseUrl, s.username, s.password))
        pathStack.clear()
        browseDirectory("/")
    }

    fun disconnect() {
        client?.close()
        client = null
        pathStack.clear()
        _state.update { NasUiState(host = it.host, port = it.port, username = it.username, davPath = it.davPath) }
    }

    fun browseDirectory(path: String) {
        val cl = client ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            cl.listDirectory(path).fold(
                onSuccess = { entries ->
                    _state.update { it.copy(
                        isConnected = true, isLoading = false,
                        currentPath = path, entries = entries
                    )}
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, isConnected = false,
                        error = "Cannot connect: ${e.message}") }
                }
            )
        }
    }

    fun navigateInto(entry: NasEntry) {
        if (!entry.isDirectory) return
        pathStack.addLast(_state.value.currentPath)
        browseDirectory(entry.path)
    }

    fun navigateUp() {
        if (pathStack.isEmpty()) return
        val prev = pathStack.removeLast()
        browseDirectory(prev)
    }

    fun downloadFile(entry: NasEntry) {
        val cl = client ?: return
        viewModelScope.launch {
            val dir = File(context.getExternalFilesDir(null), "NAS Downloads")
            dir.mkdirs()
            val localFile = File(dir, entry.name)
            _state.update { it.copy(downloadProgress = 0f, error = null) }
            cl.downloadFile(entry.path, localFile) { p ->
                _state.update { it.copy(downloadProgress = p) }
            }.fold(
                onSuccess = { _state.update { it.copy(downloadProgress = null,
                    statusMessage = "Downloaded: ${entry.name}") } },
                onFailure = { e -> _state.update { it.copy(downloadProgress = null,
                    error = "Download failed: ${e.message}") } }
            )
        }
    }

    fun uploadFile(uri: Uri) {
        val cl = client ?: return
        viewModelScope.launch {
            val cr = context.contentResolver
            val name = cr.query(uri, arrayOf("_display_name"), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else "upload_${System.currentTimeMillis()}"
            } ?: "upload_${System.currentTimeMillis()}"
            val tempFile = File(context.cacheDir, name)
            cr.openInputStream(uri)?.use { it.copyTo(tempFile.outputStream()) }

            val remotePath = "${_state.value.currentPath.trimEnd('/')}/$name"
            _state.update { it.copy(uploadProgress = 0f, error = null) }
            cl.uploadFile(tempFile, remotePath) { p ->
                _state.update { it.copy(uploadProgress = p) }
            }.fold(
                onSuccess = {
                    tempFile.delete()
                    _state.update { it.copy(uploadProgress = null, statusMessage = "Uploaded: $name") }
                    browseDirectory(_state.value.currentPath)
                },
                onFailure = { e ->
                    tempFile.delete()
                    _state.update { it.copy(uploadProgress = null, error = "Upload failed: ${e.message}") }
                }
            )
        }
    }

    fun deleteEntry(entry: NasEntry) {
        val cl = client ?: return
        viewModelScope.launch {
            cl.delete(entry.path).fold(
                onSuccess = { browseDirectory(_state.value.currentPath) },
                onFailure = { e -> _state.update { it.copy(error = "Delete failed: ${e.message}") } }
            )
        }
    }

    fun createDirectory(name: String) {
        val cl = client ?: return
        viewModelScope.launch {
            val path = "${_state.value.currentPath.trimEnd('/')}/$name"
            cl.createDirectory(path).fold(
                onSuccess = { browseDirectory(_state.value.currentPath) },
                onFailure = { e -> _state.update { it.copy(error = "Create folder failed: ${e.message}") } }
            )
        }
    }

    override fun onCleared() { client?.close(); super.onCleared() }
}
