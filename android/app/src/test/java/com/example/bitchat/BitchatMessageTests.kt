package com.example.bitchat

import bitchat.BitchatMessage
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class BitchatMessageTests {
    @Test
    fun messageEncodingDecoding() {
        val message = BitchatMessage(
            sender = "testuser",
            content = "Hello, World!",
            timestamp = Date(),
            isRelay = false,
            originalSender = null,
            isPrivate = false,
            recipientNickname = null,
            senderPeerID = "peer123",
            mentions = listOf("alice", "bob")
        )

        val encoded = message.toBinaryPayload()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatMessage.fromBinaryPayload(it) }
        assertNotNull(decoded)
        decoded!!
        assertEquals(message.sender, decoded.sender)
        assertEquals(message.content, decoded.content)
        assertEquals(message.isPrivate, decoded.isPrivate)
        assertEquals(2, decoded.mentions?.size)
        assertTrue(decoded.mentions!!.contains("alice"))
        assertTrue(decoded.mentions!!.contains("bob"))
    }

    @Test
    fun roomMessage() {
        val roomMessage = BitchatMessage(
            sender = "alice",
            content = "Hello #general",
            timestamp = Date(),
            isRelay = false,
            originalSender = null,
            isPrivate = false,
            recipientNickname = null,
            senderPeerID = "alice123",
            mentions = null,
            room = "#general"
        )

        val encoded = roomMessage.toBinaryPayload()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatMessage.fromBinaryPayload(it) }
        assertNotNull(decoded)
        assertEquals("#general", decoded!!.room)
        assertEquals(roomMessage.content, decoded.content)
    }

    @Test
    fun encryptedRoomMessage() {
        val encryptedData = byteArrayOf(1,2,3,4,5,6,7,8)
        val encryptedMessage = BitchatMessage(
            sender = "bob",
            content = "",
            timestamp = Date(),
            isRelay = false,
            originalSender = null,
            isPrivate = false,
            recipientNickname = null,
            senderPeerID = "bob456",
            mentions = null,
            room = "#secret",
            encryptedContent = encryptedData,
            isEncrypted = true
        )

        val encoded = encryptedMessage.toBinaryPayload()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatMessage.fromBinaryPayload(it) }
        assertNotNull(decoded)
        decoded!!
        assertTrue(decoded.isEncrypted)
        assertArrayEquals(encryptedData, decoded.encryptedContent)
        assertEquals("#secret", decoded.room)
        assertEquals("", decoded.content)
    }

    @Test
    fun privateMessage() {
        val privateMessage = BitchatMessage(
            sender = "alice",
            content = "This is private",
            timestamp = Date(),
            isRelay = false,
            originalSender = null,
            isPrivate = true,
            recipientNickname = "bob",
            senderPeerID = "alicePeer"
        )

        val encoded = privateMessage.toBinaryPayload()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatMessage.fromBinaryPayload(it) }
        assertNotNull(decoded)
        decoded!!
        assertTrue(decoded.isPrivate)
        assertEquals("bob", decoded.recipientNickname)
    }

    @Test
    fun relayMessage() {
        val relayMessage = BitchatMessage(
            sender = "charlie",
            content = "Relayed message",
            timestamp = Date(),
            isRelay = true,
            originalSender = "alice",
            isPrivate = false
        )

        val encoded = relayMessage.toBinaryPayload()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatMessage.fromBinaryPayload(it) }
        assertNotNull(decoded)
        decoded!!
        assertTrue(decoded.isRelay)
        assertEquals("alice", decoded.originalSender)
    }

    @Test
    fun emptyContent() {
        val emptyMessage = BitchatMessage(
            sender = "user",
            content = "",
            timestamp = Date(),
            isRelay = false,
            originalSender = null
        )

        val encoded = emptyMessage.toBinaryPayload()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatMessage.fromBinaryPayload(it) }
        assertNotNull(decoded)
        assertEquals("", decoded!!.content)
    }

    @Test
    fun longContent() {
        val longContent = "A".repeat(1000)
        val longMessage = BitchatMessage(
            sender = "user",
            content = longContent,
            timestamp = Date(),
            isRelay = false,
            originalSender = null
        )

        val encoded = longMessage.toBinaryPayload()
        assertNotNull(encoded)
        val decoded = encoded?.let { BitchatMessage.fromBinaryPayload(it) }
        assertNotNull(decoded)
        assertEquals(longContent, decoded!!.content)
    }
}
