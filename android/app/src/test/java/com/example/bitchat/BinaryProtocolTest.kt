package com.example.bitchat

import org.junit.Assert.*
import org.junit.Test

class BinaryProtocolTest {
    @Test
    fun encodeDecodePacket() {
        val packet = BitchatPacket(
            type = 1,
            senderID = "sender".toByteArray(),
            recipientID = "recipient".toByteArray(),
            timestamp = System.currentTimeMillis(),
            payload = "Hello".toByteArray(),
            signature = null,
            ttl = 5
        )
        val encoded = BinaryProtocol.encode(packet)
        val decoded = BinaryProtocol.decode(encoded)
        assertNotNull(decoded)
        decoded!!
        assertArrayEquals(packet.senderID.copyOf(8), decoded.senderID)
        assertArrayEquals(packet.recipientID!!.copyOf(8), decoded.recipientID)
        assertEquals(packet.type, decoded.type)
        assertEquals(packet.ttl, decoded.ttl)
        assertArrayEquals(packet.payload, decoded.payload)
    }

    @Test
    fun decodeInvalidPacket() {
        assertNull(BinaryProtocol.decode(ByteArray(5)))
        val invalid = ByteArray(20)
        invalid[0] = 2
        assertNull(BinaryProtocol.decode(invalid))
    }
}
