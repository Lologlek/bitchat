package chat.bitchat

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionService {
    private val keyPair: KeyPair
    private val peers: MutableMap<String, SecretKey> = mutableMapOf()

    init {
        Security.addProvider(BouncyCastleProvider())
        val kpg = KeyPairGenerator.getInstance("X25519", "BC")
        keyPair = kpg.generateKeyPair()
    }

    fun publicKey(): ByteArray = keyPair.public.encoded

    fun addPeer(id: String, publicKey: ByteArray) {
        val kp = KeyPairGenerator.getInstance("X25519", "BC").generateKeyPair()
        val ka = KeyAgreement.getInstance("X25519", "BC")
        ka.init(keyPair.private)
        ka.doPhase(java.security.spec.X509EncodedKeySpec(publicKey).let { spec ->
            java.security.KeyFactory.getInstance("X25519", "BC").generatePublic(spec)
        }, true)
        val secret = ka.generateSecret()
        val key = SecretKeySpec(secret.copyOf(32), "AES")
        peers[id] = key
    }

    fun encrypt(id: String, data: ByteArray): ByteArray {
        val key = peers[id] ?: throw IllegalStateException("No key")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decrypt(id: String, data: ByteArray): ByteArray {
        val key = peers[id] ?: throw IllegalStateException("No key")
        val iv = data.sliceArray(0 until 12)
        val content = data.sliceArray(12 until data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(content)
    }
}
