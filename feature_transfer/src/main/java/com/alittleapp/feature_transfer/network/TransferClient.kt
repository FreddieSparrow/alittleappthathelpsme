package com.alittleapp.feature_transfer.network

import com.alittleapp.feature_transfer.crypto.AesCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.File
import java.net.Socket
import javax.crypto.SecretKey

/**
 * Receiver-side TCP client.
 * Connects to [host]:[port], reads AES-GCM encrypted chunks, decrypts and writes to [outputFile].
 */
class TransferClient(
    private val host: String,
    private val port: Int,
    private val key: SecretKey,
    private val outputFile: File,
    private val expectedSize: Long,
    private val onProgress: (Float) -> Unit = {},
    private val onDone: (File) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    suspend fun receive() = withContext(Dispatchers.IO) {
        try {
            Socket(host, port).use { socket ->
                val input = DataInputStream(socket.getInputStream())
                var received = 0L

                outputFile.outputStream().buffered().use { fos ->
                    while (true) {
                        val chunkLen = input.readInt()
                        if (chunkLen == 0) break      // end-of-stream sentinel

                        val encrypted = ByteArray(chunkLen)
                        input.readFully(encrypted)

                        val plain = AesCrypto.decrypt(key, encrypted)
                        fos.write(plain)
                        received += plain.size
                        if (expectedSize > 0) onProgress(received.toFloat() / expectedSize)
                    }
                }
            }
            onDone(outputFile)
        } catch (e: Exception) {
            outputFile.delete()
            onError(e.message ?: "Transfer failed")
        }
    }
}
