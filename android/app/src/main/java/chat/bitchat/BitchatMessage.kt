package chat.bitchat

import java.util.Date

data class BitchatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val content: String,
    val timestamp: Date,
    val isRelay: Boolean,
    val originalSender: String? = null,
    val isPrivate: Boolean = false,
    val recipientNickname: String? = null,
    val senderPeerID: String? = null,
    val mentions: List<String>? = null,
    val room: String? = null,
    val encryptedContent: ByteArray? = null,
    val isEncrypted: Boolean = false
)
