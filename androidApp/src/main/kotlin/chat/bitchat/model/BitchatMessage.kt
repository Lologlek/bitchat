package chat.bitchat.model

import java.util.Date
import java.util.UUID

data class BitchatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val content: String,
    val timestamp: Date = Date(),
    val isRelay: Boolean,
    val originalSender: String? = null,
    val isPrivate: Boolean = false,
    val recipientNickname: String? = null,
    val senderPeerID: String? = null,
    val mentions: List<String>? = null,
    val room: String? = null,
    val encryptedContent: ByteArray? = null,
    val isEncrypted: Boolean = false,
    var deliveryStatus: DeliveryStatus? = if (isPrivate) DeliveryStatus.Sending else null
)
