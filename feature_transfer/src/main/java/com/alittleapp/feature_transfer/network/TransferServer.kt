package com.alittleapp.feature_transfer.network

import com.alittleapp.feature_transfer.crypto.AesCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import javax.crypto.SecretKey

/**
 * Sender-side TCP server.
 *
 * Transfer wire format (all big-endian):
 *  [4 bytes: encrypted chunk length N]
 *  [N bytes: AES-GCM encrypted chunk]
 *  ... repeated until all data is sent ...
 *  [4 bytes: 0] — end-of-stream sentinel
 *
 * The key was shared out-of-band via QR code.
 * No plaintext data ever travels over the network.
 */
class TransferServer(
    private val file: File,
    private val key: SecretKey,
    private val port: Int = 0,              // 0 = OS assigns free port
    private val onProgress: (Float) -> Unit = {},
    private val onDone: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private var serverSocket: ServerSocket? = null
    val listenPort: Int get() = serverSocket?.localPort ?: -1

    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(port)
            val client: Socket = serverSocket!!.accept()
            client.use { socket ->
                val out = DataOutputStream(socket.getOutputStream())
                val total = file.length()
                var sent = 0L
                val buffer = ByteArray(CHUNK_SIZE)

                file.inputStream().use { fis ->
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        val chunk = buffer.copyOf(read)
                        val encrypted = AesCrypto.encrypt(key, chunk)
                        out.writeInt(encrypted.size)
                        out.write(encrypted)
                        sent += read
                        onProgress(sent.toFloat() / total)
                    }
                }
                out.writeInt(0)  // end sentinel
                out.flush()
            }
            onDone()
        } catch (e: SocketException) {
            // Normal shutdown via stop()
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        } finally {
            serverSocket?.close()
        }
    }

    fun stop() {
        serverSocket?.close()
    }

    companion object {
        private const val CHUNK_SIZE = 64 * 1024  // 64 KB chunks
    }
}
