package com.alittleapp.feature_transfer.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption helpers for file transfer.
 *
 * Protocol:
 *  1. Sender generates a random 256-bit AES key via [generateKey].
 *  2. Key is encoded as Base64 via [keyToBase64] and shared with the receiver via QR code.
 *  3. For each file chunk, [encrypt] produces: [IV (12 bytes)] + [ciphertext + GCM tag (16 bytes)].
 *  4. Receiver decodes the key via [keyFromBase64] and calls [decrypt] on each chunk.
 */
object AesCrypto {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128

    /** Generate a new random AES-256 session key. */
    fun generateKey(): SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(KEY_SIZE_BITS, SecureRandom())
        return kg.generateKey()
    }

    /** Encode a key to a URL-safe Base64 string for QR sharing. */
    fun keyToBase64(key: SecretKey): String =
        Base64.encodeToString(key.encoded, Base64.URL_SAFE or Base64.NO_WRAP)

    /** Decode a Base64 string back to a SecretKey. */
    fun keyFromBase64(b64: String): SecretKey {
        val bytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
        require(bytes.size == 32) { "Invalid key length: expected 32 bytes (AES-256)" }
        return SecretKeySpec(bytes, "AES")
    }

    /**
     * Encrypt [plaintext] using [key].
     * Returns: IV (12 bytes) || ciphertext+tag.
     */
    fun encrypt(key: SecretKey, plaintext: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    /**
     * Decrypt data produced by [encrypt].
     * Input: IV (12 bytes) || ciphertext+tag.
     */
    fun decrypt(key: SecretKey, data: ByteArray): ByteArray {
        require(data.size > IV_SIZE_BYTES) { "Ciphertext too short" }
        val iv = data.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = data.copyOfRange(IV_SIZE_BYTES, data.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    /**
     * Build the QR payload string that encodes transfer metadata + key.
     * Format: "alatransfer://<host>:<port>?key=<base64key>&filename=<name>&size=<bytes>"
     */
    fun buildQrPayload(host: String, port: Int, key: SecretKey, fileName: String, fileSize: Long): String {
        val encodedKey = keyToBase64(key)
        val encodedName = Base64.encodeToString(fileName.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        return "alatransfer://$host:$port?key=$encodedKey&filename=$encodedName&size=$fileSize"
    }

    /** Parse a QR payload string. Returns null if invalid. */
    fun parseQrPayload(qrContent: String): QrPayload? {
        return try {
            val uri = android.net.Uri.parse(qrContent)
            if (uri.scheme != "alatransfer") return null
            val host = uri.host ?: return null
            val port = uri.port.takeIf { it > 0 } ?: return null
            val keyB64 = uri.getQueryParameter("key") ?: return null
            val nameB64 = uri.getQueryParameter("filename") ?: return null
            val size = uri.getQueryParameter("size")?.toLongOrNull() ?: return null
            val fileName = String(Base64.decode(nameB64, Base64.URL_SAFE or Base64.NO_WRAP))
            QrPayload(host, port, keyFromBase64(keyB64), fileName, size)
        } catch (e: Exception) {
            null
        }
    }
}

data class QrPayload(
    val host: String,
    val port: Int,
    val key: javax.crypto.SecretKey,
    val fileName: String,
    val fileSize: Long
)
