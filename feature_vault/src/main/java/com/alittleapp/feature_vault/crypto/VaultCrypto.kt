package com.alittleapp.feature_vault.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Vault encryption using Android Keystore-backed AES-256-GCM.
 *
 * The key never leaves the Keystore — it is hardware-backed on devices with a secure element.
 * Data is encrypted with AES-256-GCM; the IV is prepended to the ciphertext.
 *
 * PIN authentication: the PIN is hashed (SHA-256) and stored in DataStore.
 * The Keystore key can optionally require biometric authentication (user-auth-bound key).
 */
object VaultCrypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "vault_master_key"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val GCM_TAG_BITS = 128

    /** Get or create the vault master key in Android Keystore. */
    private fun getOrCreateKey(requireBiometric: Boolean = false): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        ks.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .apply {
                if (requireBiometric) {
                    setUserAuthenticationRequired(true)
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                }
            }
            .build()
        kg.init(spec)
        return kg.generateKey()
    }

    /** Encrypt [plaintext] string. Returns Base64-encoded ciphertext (IV + data). */
    fun encrypt(plaintext: String, requireBiometric: Boolean = false): String {
        val key = getOrCreateKey(requireBiometric)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** Decrypt a Base64-encoded ciphertext produced by [encrypt]. */
    fun decrypt(encoded: String): String {
        val key = getOrCreateKey()
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /** Hash a PIN using SHA-256. Store the result; compare on unlock. */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(("vault_salt_$pin").toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, storedHash: String): Boolean = hashPin(pin) == storedHash
}
