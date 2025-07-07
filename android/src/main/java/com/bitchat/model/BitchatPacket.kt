//
// BitchatPacket.kt
// bitchat Android
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

package com.bitchat.model

/** Simple data holder matching the Swift BitchatPacket structure */
data class BitchatPacket(
    val version: Byte = 1,
    val type: Byte,
    val senderID: ByteArray,
    val recipientID: ByteArray? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: ByteArray,
    val signature: ByteArray? = null,
    var ttl: Byte = 7
)
