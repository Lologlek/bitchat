package bitchat

import java.util.Date
import java.util.UUID

enum class MessageType(val value: Byte) {
    ANNOUNCE(0x01),
    KEY_EXCHANGE(0x02),
    LEAVE(0x03),
    MESSAGE(0x04),
    FRAGMENT_START(0x05),
    FRAGMENT_CONTINUE(0x06),
    FRAGMENT_END(0x07),
    ROOM_ANNOUNCE(0x08),
    ROOM_RETENTION(0x09),
    DELIVERY_ACK(0x0A),
    DELIVERY_STATUS_REQUEST(0x0B),
    READ_RECEIPT(0x0C)
}

object SpecialRecipients {
    val BROADCAST: ByteArray = ByteArray(8) { 0xFF.toByte() }
}

data class BitchatPacket(
    val version: Byte = 1,
    val type: Byte,
    val senderID: ByteArray,
    val recipientID: ByteArray? = null,
    val timestamp: Long,
    val payload: ByteArray,
    val signature: ByteArray? = null,
    var ttl: Byte
) {
    constructor(type: Byte, ttl: Byte, senderID: String, payload: ByteArray) :
        this(
            version = 1,
            type = type,
            senderID = senderID.toByteArray(Charsets.UTF_8),
            recipientID = null,
            timestamp = System.currentTimeMillis(),
            payload = payload,
            signature = null,
            ttl = ttl
        )

    fun toBinaryData(): ByteArray? = BinaryProtocol.encode(this)

    companion object {
        fun from(data: ByteArray): BitchatPacket? = BinaryProtocol.decode(data)
    }
}

sealed class DeliveryStatus {
    object Sending : DeliveryStatus()
    object Sent : DeliveryStatus()
    data class Delivered(val to: String, val at: Date) : DeliveryStatus()
    data class Read(val by: String, val at: Date) : DeliveryStatus()
    data class Failed(val reason: String) : DeliveryStatus()
    data class PartiallyDelivered(val reached: Int, val total: Int) : DeliveryStatus()
}

data class BitchatMessage(
    val id: String = UUID.randomUUID().toString(),
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
    val isEncrypted: Boolean = false,
    var deliveryStatus: DeliveryStatus? = if (isPrivate) DeliveryStatus.Sending else null
) {
    fun toBinaryPayload(): ByteArray? = BinaryProtocol.run { this@BitchatMessage.toBinaryPayload() }

    companion object {
        fun fromBinaryPayload(data: ByteArray): BitchatMessage? = BinaryProtocol.messageFromBinaryPayload(data)
    }
}
