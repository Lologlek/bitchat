package bitchat

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.Date

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

    fun encode(packet: BitchatPacket): ByteArray? {
        val buffer = ByteArrayOutputStream()
        var payload = packet.payload
        var originalSize: Int? = null
        var compressed = false

        if (CompressionUtil.shouldCompress(payload)) {
            CompressionUtil.compress(payload)?.let {
                payload = it
                originalSize = packet.payload.size
                compressed = true
            }
        }

        buffer.write(packet.version.toInt())
        buffer.write(packet.type.toInt())
        buffer.write(packet.ttl.toInt())

        val tsBuf = ByteBuffer.allocate(8)
        tsBuf.putLong(packet.timestamp)
        buffer.write(tsBuf.array())

        var flags: Byte = 0
        if (packet.recipientID != null) flags = (flags or Flags.HAS_RECIPIENT)
        if (packet.signature != null) flags = (flags or Flags.HAS_SIGNATURE)
        if (compressed) flags = (flags or Flags.IS_COMPRESSED)
        buffer.write(flags.toInt())

        val payloadSize = payload.size + if (compressed) 2 else 0
        val lenBuf = ByteBuffer.allocate(2)
        lenBuf.putShort(payloadSize.toShort())
        buffer.write(lenBuf.array())

        val senderBytes = packet.senderID.copyOf(SENDER_ID_SIZE)
        buffer.write(senderBytes)

        packet.recipientID?.let {
            val recBytes = it.copyOf(RECIPIENT_ID_SIZE)
            buffer.write(recBytes)
        }

        if (compressed && originalSize != null) {
            val sizeBuf = ByteBuffer.allocate(2)
            sizeBuf.putShort(originalSize.toShort())
            buffer.write(sizeBuf.array())
        }
        buffer.write(payload)

        packet.signature?.let {
            buffer.write(it.copyOf(SIGNATURE_SIZE))
        }

        return buffer.toByteArray()
    }

    fun decode(data: ByteArray): BitchatPacket? {
        if (data.size < HEADER_SIZE + SENDER_ID_SIZE) return null
        var offset = 0
        val version = data[offset]; offset += 1
        if (version.toInt() != 1) return null
        val type = data[offset]; offset += 1
        val ttl = data[offset]; offset += 1

        val timestamp = ByteBuffer.wrap(data, offset, 8).long; offset += 8

        val flags = data[offset]; offset += 1
        val hasRecipient = flags.toInt() and Flags.HAS_RECIPIENT.toInt() != 0
        val hasSignature = flags.toInt() and Flags.HAS_SIGNATURE.toInt() != 0
        val isCompressed = flags.toInt() and Flags.IS_COMPRESSED.toInt() != 0

        val payloadLength = ByteBuffer.wrap(data, offset, 2).short.toInt() and 0xFFFF; offset += 2

        var expected = HEADER_SIZE + SENDER_ID_SIZE + payloadLength
        if (hasRecipient) expected += RECIPIENT_ID_SIZE
        if (hasSignature) expected += SIGNATURE_SIZE
        if (data.size < expected) return null

        val senderID = data.copyOfRange(offset, offset + SENDER_ID_SIZE); offset += SENDER_ID_SIZE

        var recipientID: ByteArray? = null
        if (hasRecipient) {
            recipientID = data.copyOfRange(offset, offset + RECIPIENT_ID_SIZE)
            offset += RECIPIENT_ID_SIZE
        }

        val payload: ByteArray
        if (isCompressed) {
            if (payloadLength < 2 || offset + payloadLength > data.size) return null
            val originalSize = ByteBuffer.wrap(data, offset, 2).short.toInt() and 0xFFFF
            offset += 2
            val compressedData = data.copyOfRange(offset, offset + payloadLength - 2)
            offset += payloadLength - 2
            payload = CompressionUtil.decompress(compressedData, originalSize) ?: return null
        } else {
            if (offset + payloadLength > data.size) return null
            payload = data.copyOfRange(offset, offset + payloadLength)
            offset += payloadLength
        }

        var signature: ByteArray? = null
        if (hasSignature && offset + SIGNATURE_SIZE <= data.size) {
            signature = data.copyOfRange(offset, offset + SIGNATURE_SIZE)
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

    fun BitchatMessage.toBinaryPayload(): ByteArray? {
        val out = ByteArrayOutputStream()
        var flags: Byte = 0
        if (isRelay) flags = (flags or 0x01)
        if (isPrivate) flags = (flags or 0x02)
        if (originalSender != null) flags = (flags or 0x04)
        if (recipientNickname != null) flags = (flags or 0x08)
        if (senderPeerID != null) flags = (flags or 0x10)
        if (mentions != null && mentions!!.isNotEmpty()) flags = (flags or 0x20)
        if (room != null) flags = (flags or 0x40)
        if (isEncrypted) flags = (flags or 0x80.toByte())
        out.write(flags.toInt())

        val ts = ByteBuffer.allocate(8).putLong(timestamp.time).array()
        out.write(ts)

        val idBytes = id.toByteArray(Charsets.UTF_8)
        out.write(idBytes.size.coerceAtMost(255))
        out.write(idBytes, 0, idBytes.size.coerceAtMost(255))

        val senderBytes = sender.toByteArray(Charsets.UTF_8)
        out.write(senderBytes.size.coerceAtMost(255))
        out.write(senderBytes, 0, senderBytes.size.coerceAtMost(255))

        val contentBytes = if (isEncrypted && encryptedContent != null) encryptedContent!! else content.toByteArray(Charsets.UTF_8)
        val len = contentBytes.size.coerceAtMost(65535)
        val lenBuf = ByteBuffer.allocate(2).putShort(len.toShort()).array()
        out.write(lenBuf)
        out.write(contentBytes, 0, len)

        originalSender?.let {
            val data = it.toByteArray(Charsets.UTF_8)
            out.write(data.size.coerceAtMost(255))
            out.write(data, 0, data.size.coerceAtMost(255))
        }

        recipientNickname?.let {
            val data = it.toByteArray(Charsets.UTF_8)
            out.write(data.size.coerceAtMost(255))
            out.write(data, 0, data.size.coerceAtMost(255))
        }

        senderPeerID?.let {
            val data = it.toByteArray(Charsets.UTF_8)
            out.write(data.size.coerceAtMost(255))
            out.write(data, 0, data.size.coerceAtMost(255))
        }

        mentions?.let { list ->
            out.write(list.size.coerceAtMost(255))
            for (m in list.take(255)) {
                val mdata = m.toByteArray(Charsets.UTF_8)
                out.write(mdata.size.coerceAtMost(255))
                out.write(mdata, 0, mdata.size.coerceAtMost(255))
            }
        }

        room?.let {
            val data = it.toByteArray(Charsets.UTF_8)
            out.write(data.size.coerceAtMost(255))
            out.write(data, 0, data.size.coerceAtMost(255))
        }

        return out.toByteArray()
    }

    fun messageFromBinaryPayload(data: ByteArray): BitchatMessage? {
        var offset = 0
        if (data.size < 13) return null

        val flags = data[offset]; offset += 1
        val isRelay = flags.toInt() and 0x01 != 0
        val isPrivate = flags.toInt() and 0x02 != 0
        val hasOriginal = flags.toInt() and 0x04 != 0
        val hasRecipientNick = flags.toInt() and 0x08 != 0
        val hasSenderPeer = flags.toInt() and 0x10 != 0
        val hasMentions = flags.toInt() and 0x20 != 0
        val hasRoom = flags.toInt() and 0x40 != 0
        val isEncrypted = flags.toInt() and 0x80 != 0

        if (offset + 8 > data.size) return null
        val timestamp = Date(ByteBuffer.wrap(data, offset, 8).long); offset += 8

        if (offset >= data.size) return null
        val idLen = data[offset].toInt() and 0xFF; offset += 1
        if (offset + idLen > data.size) return null
        val id = String(data, offset, idLen, Charsets.UTF_8); offset += idLen

        if (offset >= data.size) return null
        val senderLen = data[offset].toInt() and 0xFF; offset += 1
        if (offset + senderLen > data.size) return null
        val sender = String(data, offset, senderLen, Charsets.UTF_8); offset += senderLen

        if (offset + 2 > data.size) return null
        val contentLen = ByteBuffer.wrap(data, offset, 2).short.toInt() and 0xFFFF; offset += 2
        if (offset + contentLen > data.size) return null

        val content: String
        val encryptedContent: ByteArray?
        if (isEncrypted) {
            encryptedContent = data.copyOfRange(offset, offset + contentLen)
            content = ""
        } else {
            content = String(data, offset, contentLen, Charsets.UTF_8)
            encryptedContent = null
        }
        offset += contentLen

        var originalSender: String? = null
        if (hasOriginal && offset < data.size) {
            val len = data[offset].toInt() and 0xFF; offset += 1
            if (offset + len > data.size) return null
            originalSender = String(data, offset, len, Charsets.UTF_8); offset += len
        }

        var recipientNickname: String? = null
        if (hasRecipientNick && offset < data.size) {
            val len = data[offset].toInt() and 0xFF; offset += 1
            if (offset + len > data.size) return null
            recipientNickname = String(data, offset, len, Charsets.UTF_8); offset += len
        }

        var senderPeerID: String? = null
        if (hasSenderPeer && offset < data.size) {
            val len = data[offset].toInt() and 0xFF; offset += 1
            if (offset + len > data.size) return null
            senderPeerID = String(data, offset, len, Charsets.UTF_8); offset += len
        }

        var mentions: MutableList<String>? = null
        if (hasMentions && offset < data.size) {
            val count = data[offset].toInt() and 0xFF; offset += 1
            mentions = mutableListOf()
            for (i in 0 until count) {
                if (offset >= data.size) break
                val len = data[offset].toInt() and 0xFF; offset += 1
                if (offset + len > data.size) return null
                val mention = String(data, offset, len, Charsets.UTF_8)
                mentions.add(mention)
                offset += len
            }
        }

        var room: String? = null
        if (hasRoom && offset < data.size) {
            val len = data[offset].toInt() and 0xFF; offset += 1
            if (offset + len > data.size) return null
            room = String(data, offset, len, Charsets.UTF_8); offset += len
        }

        return BitchatMessage(
            id = id,
            sender = sender,
            content = content,
            timestamp = timestamp,
            isRelay = isRelay,
            originalSender = originalSender,
            isPrivate = isPrivate,
            recipientNickname = recipientNickname,
            senderPeerID = senderPeerID,
            mentions = mentions,
            room = room,
            encryptedContent = encryptedContent,
            isEncrypted = isEncrypted,
            deliveryStatus = null
        )
    }
}
