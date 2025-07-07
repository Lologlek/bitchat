//
// KeystoreManager.kt
// bitchat
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//
package com.bitchat

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * KeystoreManager mirrors KeychainManager.swift for Android.
 * It stores room passwords and identity keys using EncryptedSharedPreferences.
 */
object KeystoreManager {
    private const val PREF_NAME = "bitchat_keystore"
    private var prefs: SharedPreferences? = null

    /**
     * Initialize the KeystoreManager. Must be called before use.
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

    // region Room Passwords
    fun saveRoomPassword(room: String, password: String) {
        prefs?.edit()?.putString("room_" + room, password)?.apply()
    }

    fun getRoomPassword(room: String): String? {
        return prefs?.getString("room_" + room, null)
    }

    fun deleteRoomPassword(room: String) {
        prefs?.edit()?.remove("room_" + room)?.apply()
    }

    fun getAllRoomPasswords(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        prefs?.all?.forEach { (key, value) ->
            if (key.startsWith("room_") && value is String) {
                map[key.removePrefix("room_")] = value
            }
        }
        return map
    }
    // endregion

    // region Identity Keys
    fun saveIdentityKey(key: String, data: ByteArray) {
        val encoded = Base64.encodeToString(data, Base64.NO_WRAP)
        prefs?.edit()?.putString("identity_" + key, encoded)?.apply()
    }

    fun getIdentityKey(key: String): ByteArray? {
        val encoded = prefs?.getString("identity_" + key, null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }
    // endregion

    // region Cleanup
    fun deleteAllPasswords() {
        val editor = prefs?.edit() ?: return
        prefs?.all?.keys?.forEach { key ->
            if (key.startsWith("room_")) {
                editor.remove(key)
            }
        }
        editor.apply()
    }
    // endregion
}
