package com.bitchat.model

import java.nio.ByteBuffer

object BinaryProtocol {
    private const val HEADER_SIZE = 13
    private const val SENDER_ID_SIZE = 8
    private const val RECIPIENT_ID_SIZE = 8
    private const val SIGNATURE_SIZE = 64

    fun encode(packet: BitchatPacket): ByteArray {
        val hasRecipient = packet.recipientID != null
        val hasSignature = packet.signature != null
        val payloadLength = packet.payload.size

        val bufferSize = HEADER_SIZE + SENDER_ID_SIZE +
            (if (hasRecipient) RECIPIENT_ID_SIZE else 0) +
            payloadLength + (if (hasSignature) SIGNATURE_SIZE else 0)
        val buffer = ByteBuffer.allocate(bufferSize)

        buffer.put(packet.version)
        buffer.put(packet.type)
        buffer.put(packet.ttl)
        buffer.putLong(packet.timestamp)

        var flags: Byte = 0
        if (hasRecipient) flags = (flags or 0x01)
        if (hasSignature) flags = (flags or 0x02)
        buffer.put(flags)

        buffer.putShort(payloadLength.toShort())

        val sender = if (packet.senderID.size >= SENDER_ID_SIZE)
            packet.senderID.copyOfRange(0, SENDER_ID_SIZE)
        else
            packet.senderID + ByteArray(SENDER_ID_SIZE - packet.senderID.size)
        buffer.put(sender)

        if (hasRecipient) {
            val rec = packet.recipientID!!
            val recipient = if (rec.size >= RECIPIENT_ID_SIZE)
                rec.copyOfRange(0, RECIPIENT_ID_SIZE)
            else
                rec + ByteArray(RECIPIENT_ID_SIZE - rec.size)
            buffer.put(recipient)
        }

        buffer.put(packet.payload)

        if (hasSignature) {
            val sig = packet.signature!!
            val signature = if (sig.size >= SIGNATURE_SIZE)
                sig.copyOfRange(0, SIGNATURE_SIZE)
            else
                sig + ByteArray(SIGNATURE_SIZE - sig.size)
            buffer.put(signature)
        }

        return buffer.array()
    }

    fun decode(data: ByteArray): BitchatPacket? {
        if (data.size < HEADER_SIZE + SENDER_ID_SIZE) return null
        val buffer = ByteBuffer.wrap(data)
        val version = buffer.get()
        if (version.toInt() != 1) return null
        val type = buffer.get()
        val ttl = buffer.get()
        val timestamp = buffer.long
        val flags = buffer.get()
        val hasRecipient = flags.toInt() and 0x01 != 0
        val hasSignature = flags.toInt() and 0x02 != 0
        val payloadLength = buffer.short.toInt() and 0xFFFF

        val expectedSize = HEADER_SIZE + SENDER_ID_SIZE +
            (if (hasRecipient) RECIPIENT_ID_SIZE else 0) +
            payloadLength + (if (hasSignature) SIGNATURE_SIZE else 0)
        if (data.size < expectedSize) return null

        val senderID = ByteArray(SENDER_ID_SIZE)
        buffer.get(senderID)

        var recipientID: ByteArray? = null
        if (hasRecipient) {
            recipientID = ByteArray(RECIPIENT_ID_SIZE)
            buffer.get(recipientID)
        }

        val payload = ByteArray(payloadLength)
        buffer.get(payload)

        var signature: ByteArray? = null
        if (hasSignature) {
            signature = ByteArray(SIGNATURE_SIZE)
            buffer.get(signature)
        }

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
