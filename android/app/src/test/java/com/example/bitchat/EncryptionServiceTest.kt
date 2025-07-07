package com.example.bitchat

import org.junit.Assert.*
import org.junit.Test

class EncryptionServiceTest {
    @Test
    fun encryptDecrypt() {
        val service = EncryptionService()
        val key = ByteArray(32) { 1 }
        service.addPeerSecret("peer", key)
        val plain = "secret".toByteArray()
        val encrypted = service.encrypt(plain, "peer")
        val decrypted = service.decrypt(encrypted, "peer")
        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun signVerify() {
        val service = EncryptionService()
        val data = "hello".toByteArray()
        val sig = service.sign(data)
        assertTrue(service.verify(sig, data))
    }
}
