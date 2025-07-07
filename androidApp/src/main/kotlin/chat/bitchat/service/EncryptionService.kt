package chat.bitchat.service

object EncryptionService {
    fun encrypt(content: String, key: ByteArray): ByteArray {
        // Placeholder for real encryption
        return content.toByteArray()
    }

    fun decrypt(data: ByteArray, key: ByteArray): String {
        // Placeholder for real decryption
        return String(data)
    }
}
