//
// TransportManager.kt
// bitchat Android
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

package com.bitchat.transports

import com.bitchat.model.BitchatPacket

class TransportManager {
    private val transports: MutableList<TransportProtocol> = mutableListOf()
    private val routingTable: MutableMap<String, TransportType> = mutableMapOf()

    fun registerTransport(transport: TransportProtocol) {
        transports.add(transport)
    }

    fun sendOptimal(packet: BitchatPacket, toPeer: String?) {
        // Simple strategy: use first available transport
        val transport = transports.firstOrNull { it.isAvailable }
        transport?.send(packet, toPeer)
    }
}
