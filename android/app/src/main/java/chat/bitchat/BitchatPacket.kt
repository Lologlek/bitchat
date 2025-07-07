package chat.bitchat

data class BitchatPacket(
    val version: Byte = 1,
    val type: Byte,
    val senderID: ByteArray,
    val recipientID: ByteArray?,
    val timestamp: Long,
    val payload: ByteArray,
    val signature: ByteArray?,
    val ttl: Byte
)
