package com.example.bitchat

import bitchat.BitchatPacket
import bitchat.BinaryProtocol
import bitchat.MessageType
import org.junit.Assert.*
import org.junit.Test

class CompressionTests {
    @Test
    fun testCompressionFlagAndDecoding() {
        val payloadString = "A".repeat(200)
        val packet = BitchatPacket(
            type = MessageType.MESSAGE.value,
            senderID = "sender".toByteArray(),
            recipientID = null,
            timestamp = System.currentTimeMillis(),
            payload = payloadString.toByteArray(),
            signature = null,
            ttl = 5
        )

        val encoded = packet.toBinaryData()
        assertNotNull(encoded)
        val flags = encoded!![11]
        assertTrue(flags.toInt() and BinaryProtocol.Flags.IS_COMPRESSED.toInt() != 0)

        val decoded = BitchatPacket.from(encoded)
        assertNotNull(decoded)
        assertArrayEquals(packet.payload, decoded!!.payload)
    }

    @Test
    fun testSmallPayloadNotCompressed() {
        val payload = "Hello".toByteArray()
        val packet = BitchatPacket(
            type = MessageType.MESSAGE.value,
            senderID = "sender".toByteArray(),
            recipientID = null,
            timestamp = System.currentTimeMillis(),
            payload = payload,
            signature = null,
            ttl = 5
        )

        val encoded = packet.toBinaryData()
        assertNotNull(encoded)
        val flags = encoded!![11]
        assertEquals(0, flags.toInt() and BinaryProtocol.Flags.IS_COMPRESSED.toInt())

        val decoded = BitchatPacket.from(encoded)
        assertNotNull(decoded)
        assertArrayEquals(payload, decoded!!.payload)
    }
}
