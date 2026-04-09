package com.alittleapp.feature_transfer

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alittleapp.feature_transfer.crypto.AesCrypto
import com.alittleapp.feature_transfer.crypto.QrPayload
import com.alittleapp.feature_transfer.network.TransferClient
import com.alittleapp.feature_transfer.network.TransferServer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class TransferState {
    object Idle : TransferState()
    object GeneratingQr : TransferState()
    data class WaitingForReceiver(val qrContent: String, val port: Int) : TransferState()
    data class Sending(val progress: Float) : TransferState()
    data class Receiving(val progress: Float) : TransferState()
    data class Done(val message: String) : TransferState()
    data class Error(val message: String) : TransferState()
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<TransferState>(TransferState.Idle)
    val state: StateFlow<TransferState> = _state.asStateFlow()

    private var server: TransferServer? = null

    /** Called by sender: picks a file, starts server, generates QR payload */
    fun prepareSend(fileUri: Uri) {
        viewModelScope.launch {
            _state.value = TransferState.GeneratingQr
            try {
                val file = copyUriToCache(fileUri) ?: run {
                    _state.value = TransferState.Error("Cannot read file")
                    return@launch
                }
                val key = AesCrypto.generateKey()
                val localIp = getLocalIpAddress()

                // Start server on a random free port
                val srv = TransferServer(
                    file = file,
                    key = key,
                    onProgress = { p -> _state.value = TransferState.Sending(p) },
                    onDone = { _state.value = TransferState.Done("File sent successfully.") },
                    onError = { msg -> _state.value = TransferState.Error(msg) }
                )
                server = srv

                // Launch server in background (it blocks waiting for one connection)
                launch { srv.start() }

                // Wait briefly for the port to be assigned
                kotlinx.coroutines.delay(100)
                val port = srv.listenPort
                val qr = AesCrypto.buildQrPayload(localIp, port, key, file.name, file.length())
                _state.value = TransferState.WaitingForReceiver(qr, port)

            } catch (e: Exception) {
                _state.value = TransferState.Error(e.message ?: "Error")
            }
        }
    }

    /** Called by receiver: scans QR code, connects and receives the file */
    fun receiveFromQr(qrContent: String, saveDir: File) {
        viewModelScope.launch {
            val payload: QrPayload = AesCrypto.parseQrPayload(qrContent)
                ?: run { _state.value = TransferState.Error("Invalid QR code"); return@launch }

            _state.value = TransferState.Receiving(0f)
            val outputFile = File(saveDir, payload.fileName)

            val client = TransferClient(
                host = payload.host,
                port = payload.port,
                key = payload.key,
                outputFile = outputFile,
                expectedSize = payload.fileSize,
                onProgress = { p -> _state.value = TransferState.Receiving(p) },
                onDone = { f -> _state.value = TransferState.Done("Saved to ${f.name}") },
                onError = { msg -> _state.value = TransferState.Error(msg) }
            )
            client.receive()
        }
    }

    fun reset() {
        server?.stop()
        server = null
        _state.value = TransferState.Idle
    }

    override fun onCleared() {
        server?.stop()
        super.onCleared()
    }

    private fun copyUriToCache(uri: Uri): File? {
        return try {
            val cr = context.contentResolver
            val name = cr.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "transfer_file"
            } ?: "transfer_file"
            val cacheFile = File(context.cacheDir, name)
            cr.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            cacheFile
        } catch (e: Exception) { null }
    }

    @Suppress("DEPRECATION")
    private fun getLocalIpAddress(): String {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
    }
}
