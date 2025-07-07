package com.example.bitchat

import java.security.KeyPairGenerator
import java.security.KeyPair
import java.security.Signature
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionService {
    private val signingKeyPair: KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val peerKeys: ConcurrentHashMap<String, SecretKey> = ConcurrentHashMap()

    fun addPeerSecret(peerId: String, key: ByteArray) {
        peerKeys[peerId] = SecretKeySpec(key, 0, key.size, "AES")
    }

    fun encrypt(data: ByteArray, peerId: String): ByteArray {
        val key = peerKeys[peerId] ?: throw IllegalArgumentException("No key for peer")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decrypt(data: ByteArray, peerId: String): ByteArray {
        val key = peerKeys[peerId] ?: throw IllegalArgumentException("No key for peer")
        val iv = data.copyOfRange(0, 12)
        val enc = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(enc)
    }

    fun sign(data: ByteArray): ByteArray {
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(signingKeyPair.private)
        sig.update(data)
        return sig.sign()
    }

    fun verify(signature: ByteArray, data: ByteArray): Boolean {
        val sig = Signature.getInstance("Ed25519")
        sig.initVerify(signingKeyPair.public)
        sig.update(data)
        return sig.verify(signature)
    }
}
