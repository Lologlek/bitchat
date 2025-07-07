package chat.bitchat

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.KeyFactory
import java.security.KeyAgreement
import java.security.spec.NamedParameterSpec
import java.security.spec.XECPublicKeySpec
import java.security.spec.EdECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

class EncryptionService(context: Context) {
    private val privateKey: PrivateKey
    val publicKey: PublicKey
    private val signingPrivateKey: PrivateKey
    val signingPublicKey: PublicKey

    private val peerPublicKeys = ConcurrentHashMap<String, PublicKey>()
    private val peerSigningKeys = ConcurrentHashMap<String, PublicKey>()
    private val peerIdentityKeys = ConcurrentHashMap<String, PublicKey>()
    private val sharedSecrets = ConcurrentHashMap<String, SecretKey>()

    private val identityAlias = "bitchat_identity"
    private val identityPrivateKey: PrivateKey
    val identityPublicKey: PublicKey

    init {
        val x25519Generator = KeyPairGenerator.getInstance("X25519")
        val xPair = x25519Generator.generateKeyPair()
        privateKey = xPair.private
        publicKey = xPair.public

        val edGenerator = KeyPairGenerator.getInstance("Ed25519")
        val signPair = edGenerator.generateKeyPair()
        signingPrivateKey = signPair.private
        signingPublicKey = signPair.public

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(identityAlias)) {
            val priv = keyStore.getKey(identityAlias, null) as PrivateKey
            val pub = keyStore.getCertificate(identityAlias).publicKey
            identityPrivateKey = priv
            identityPublicKey = pub
        } else {
            val gen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_ED25519, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(identityAlias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_NONE)
                .build()
            gen.initialize(spec)
            val pair = gen.generateKeyPair()
            identityPrivateKey = pair.private
            identityPublicKey = pair.public
        }
    }

    fun getCombinedPublicKeyData(): ByteArray {
        val epk = publicKey.encoded.takeLast(32).toByteArray()
        val spk = signingPublicKey.encoded.takeLast(32).toByteArray()
        val idk = identityPublicKey.encoded.takeLast(32).toByteArray()
        return epk + spk + idk
    }

    fun addPeerPublicKey(peerID: String, data: ByteArray) {
        require(data.size == 96) { "Invalid public key data size" }

        val xFactory = KeyFactory.getInstance("X25519")
        val named = NamedParameterSpec("X25519")
        val pkSpec = XECPublicKeySpec(named, data.sliceArray(0 until 32))
        val pub = xFactory.generatePublic(pkSpec)
        peerPublicKeys[peerID] = pub

        val edFactory = KeyFactory.getInstance("Ed25519")
        val signSpec = EdECPublicKeySpec(NamedParameterSpec("Ed25519"), data.sliceArray(32 until 64))
        peerSigningKeys[peerID] = edFactory.generatePublic(signSpec)

        val idSpec = EdECPublicKeySpec(NamedParameterSpec("Ed25519"), data.sliceArray(64 until 96))
        peerIdentityKeys[peerID] = edFactory.generatePublic(idSpec)

        val agreement = KeyAgreement.getInstance("X25519")
        agreement.init(privateKey)
        agreement.doPhase(pub, true)
        val secret = agreement.generateSecret()
        val keyBytes = hkdfSha256(secret, "bitchat-v1".toByteArray(), 32)
        sharedSecrets[peerID] = SecretKeySpec(keyBytes, "AES")
    }

    fun getPeerIdentityKey(peerID: String): ByteArray? =
        peerIdentityKeys[peerID]?.encoded?.takeLast(32)?.toByteArray()

    fun clearPersistentIdentity() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(identityAlias)
    }

    fun encrypt(data: ByteArray, peerID: String): ByteArray {
        val key = sharedSecrets[peerID] ?: throw EncryptionError.NoSharedSecret
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(data)
        return iv + cipherText
    }

    fun decrypt(data: ByteArray, peerID: String): ByteArray {
        val key = sharedSecrets[peerID] ?: throw EncryptionError.NoSharedSecret
        val iv = data.sliceArray(0 until 12)
        val cipherText = data.sliceArray(12 until data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance("Ed25519")
        signature.initSign(signingPrivateKey)
        signature.update(data)
        return signature.sign()
    }

    fun verify(signatureData: ByteArray, data: ByteArray, peerID: String): Boolean {
        val pub = peerSigningKeys[peerID] ?: throw EncryptionError.NoSharedSecret
        val signature = Signature.getInstance("Ed25519")
        signature.initVerify(pub)
        signature.update(data)
        return signature.verify(signatureData)
    }

    private fun hkdfSha256(secret: ByteArray, salt: ByteArray, size: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(secret)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(0x01)
        val okm = mac.doFinal()
        return okm.copyOfRange(0, size)
    }
}

enum class EncryptionError : Exception() {
    NoSharedSecret,
    InvalidPublicKey,
    EncryptionFailed,
    DecryptionFailed
}

