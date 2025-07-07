package chat.bitchat.service

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Simple AES-GCM encryption service used by the Compose demo.
 * Keys are persisted using EncryptedSharedPreferences so that
 * the same key can be reused across launches similar to iOS
 * Keychain usage in the Swift implementation.
 */
object EncryptionService {

    private const val PREF_NAME = "bitchat_encryption"
    private const val KEY_PREFIX = "aes_"

    private var prefs: SharedPreferences? = null

    /**
     * Initializes the encrypted preferences backing store. This must be called
     * before any key management or encryption operations.
     */
    fun initialize(context: Context) {
        if (prefs != null) return

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Retrieve an existing AES key for the given alias or create a new
     * random 256‑bit key if none exists.
     */
    fun getOrCreateKey(alias: String): ByteArray {
        val stored = prefs?.getString(KEY_PREFIX + alias, null)
        if (stored != null) {
            return Base64.decode(stored, Base64.NO_WRAP)
        }

        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        prefs?.edit()?.putString(
            KEY_PREFIX + alias,
            Base64.encodeToString(key, Base64.NO_WRAP)
        )?.apply()
        return key
    }

    fun deleteKey(alias: String) {
        prefs?.edit()?.remove(KEY_PREFIX + alias)?.apply()
    }

    /**
     * Encrypt the provided string using AES/GCM with the supplied key.
     * The returned byte array is compatible with CryptoKit's combined
     * representation used on iOS (12 byte IV + ciphertext + 16 byte tag).
     */
    fun encrypt(content: String, key: ByteArray): ByteArray {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(key, KeyProperties.KEY_ALGORITHM_AES)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherText = cipher.doFinal(content.toByteArray(Charsets.UTF_8))
        return iv + cipherText
    }

    /**
     * Decrypt data that was produced by [encrypt].
     */
    fun decrypt(data: ByteArray, key: ByteArray): String {
        require(data.size > 12) { "Invalid data" }

        val iv = data.sliceArray(0 until 12)
        val cipherText = data.sliceArray(12 until data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(key, KeyProperties.KEY_ALGORITHM_AES)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val plain = cipher.doFinal(cipherText)
        return String(plain, Charsets.UTF_8)
    }
}
