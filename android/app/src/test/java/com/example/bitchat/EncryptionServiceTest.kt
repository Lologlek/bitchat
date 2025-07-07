package com.example.bitchat

import chat.bitchat.service.EncryptionService
import org.junit.Assert.*
import org.junit.Test

class EncryptionServiceTest {
    @Test
    fun encryptDecrypt() {
        val key = ByteArray(32) { 1 }
        val encrypted = EncryptionService.encrypt("secret", key)
        val decrypted = EncryptionService.decrypt(encrypted, key)
        assertEquals("secret", decrypted)
    }
}
