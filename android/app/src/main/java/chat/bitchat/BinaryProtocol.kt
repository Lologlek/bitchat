package chat.bitchat

import java.nio.ByteBuffer
import java.nio.ByteOrder

object BinaryProtocol {
    const val HEADER_SIZE = 13
    const val SENDER_ID_SIZE = 8
    const val RECIPIENT_ID_SIZE = 8
    const val SIGNATURE_SIZE = 64

    object Flags {
        const val HAS_RECIPIENT: Byte = 0x01
        const val HAS_SIGNATURE: Byte = 0x02
        const val IS_COMPRESSED: Byte = 0x04
    }

    fun encode(packet: BitchatPacket): ByteArray {
        val payload = packet.payload
        val payloadLength = payload.size
        val buffer = ByteBuffer.allocate(HEADER_SIZE + SENDER_ID_SIZE + payloadLength + if (packet.recipientID != null) RECIPIENT_ID_SIZE else 0 + if (packet.signature != null) SIGNATURE_SIZE else 0)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.put(packet.version)
        buffer.put(packet.type)
        buffer.put(packet.ttl)
        buffer.putLong(packet.timestamp)
        var flags = 0
        if (packet.recipientID != null) flags = flags or Flags.HAS_RECIPIENT.toInt()
        if (packet.signature != null) flags = flags or Flags.HAS_SIGNATURE.toInt()
        buffer.put(flags.toByte())
        buffer.putShort(payloadLength.toShort())
        val sender = packet.senderID.copyOfRange(0, SENDER_ID_SIZE)
        buffer.put(sender)
        packet.recipientID?.let {
            val recipient = it.copyOfRange(0, RECIPIENT_ID_SIZE)
            buffer.put(recipient)
        }
        buffer.put(payload)
        packet.signature?.let {
            buffer.put(it.copyOfRange(0, SIGNATURE_SIZE))
        }
        return buffer.array()
    }

    fun decode(data: ByteArray): BitchatPacket? {
        if (data.size < HEADER_SIZE + SENDER_ID_SIZE) return null
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        val version = buffer.get()
        if (version.toInt() != 1) return null
        val type = buffer.get()
        val ttl = buffer.get()
        val timestamp = buffer.long
        val flags = buffer.get().toInt()
        val payloadLength = buffer.short.toInt() and 0xFFFF
        val senderID = ByteArray(SENDER_ID_SIZE)
        buffer.get(senderID)
        val hasRecipient = (flags and Flags.HAS_RECIPIENT.toInt()) != 0
        val recipientID = if (hasRecipient) ByteArray(RECIPIENT_ID_SIZE).also { buffer.get(it) } else null
        val payload = ByteArray(payloadLength)
        buffer.get(payload)
        val hasSignature = (flags and Flags.HAS_SIGNATURE.toInt()) != 0
        val signature = if (hasSignature) ByteArray(SIGNATURE_SIZE).also { buffer.get(it) } else null
        return BitchatPacket(
            type = type,
            senderID = senderID,
            recipientID = recipientID,
            timestamp = timestamp,
            payload = payload,
            signature = signature,
            ttl = ttl
        )
    }
}
