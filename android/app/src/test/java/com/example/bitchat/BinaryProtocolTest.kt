package com.example.bitchat

import bitchat.BitchatPacket
import bitchat.BinaryProtocol
import bitchat.MessageType
import bitchat.SpecialRecipients
import org.junit.Assert.*
import org.junit.Test

class BinaryProtocolTest {
    @Test
    fun encodeDecodePacket() {
        val packet = BitchatPacket(
            type = MessageType.MESSAGE.value,
            senderID = "sender".toByteArray(),
            recipientID = "recipient".toByteArray(),
            timestamp = System.currentTimeMillis(),
            payload = "Hello, World!".toByteArray(),
            signature = null,
            ttl = 5
        )

        val encoded = packet.toBinaryData()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatPacket.from(it) }
        assertNotNull(decoded)
        decoded!!
        assertEquals(packet.version, decoded.version)
        assertEquals(packet.type, decoded.type)
        assertEquals(packet.ttl, decoded.ttl)
        assertEquals(packet.timestamp, decoded.timestamp)
        assertArrayEquals(packet.payload, decoded.payload)
    }

    @Test
    fun decodeInvalidPacket() {
        assertNull(BitchatPacket.from(ByteArray(5)))
        val invalid = ByteArray(20)
        invalid[0] = 2
        assertNull(BitchatPacket.from(invalid))
    }

    @Test
    fun broadcastPacket() {
        val packet = BitchatPacket(
            type = MessageType.MESSAGE.value,
            senderID = "sender".toByteArray(),
            recipientID = SpecialRecipients.BROADCAST,
            timestamp = System.currentTimeMillis(),
            payload = "Broadcast message".toByteArray(),
            signature = null,
            ttl = 3
        )

        val encoded = packet.toBinaryData()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatPacket.from(it) }
        assertNotNull(decoded)
        assertArrayEquals(SpecialRecipients.BROADCAST, decoded!!.recipientID)
    }

    @Test
    fun packetWithSignature() {
        val signature = ByteArray(64) { 0xAB.toByte() }
        val packet = BitchatPacket(
            type = MessageType.MESSAGE.value,
            senderID = "sender".toByteArray(),
            recipientID = "recipient".toByteArray(),
            timestamp = System.currentTimeMillis(),
            payload = "Signed message".toByteArray(),
            signature = signature,
            ttl = 5
        )

        val encoded = packet.toBinaryData()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatPacket.from(it) }
        assertNotNull(decoded)
        assertNotNull(decoded!!.signature)
        assertArrayEquals(signature, decoded.signature)
    }
}
