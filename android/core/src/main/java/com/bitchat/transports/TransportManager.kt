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
        // Prefer WiFi Direct when available, otherwise fall back to Bluetooth
        val wifi = transports.firstOrNull { it.transportType == TransportType.WIFI_DIRECT && it.isAvailable }
        val ble = transports.firstOrNull { it.transportType == TransportType.BLUETOOTH && it.isAvailable }

        when {
            wifi != null -> wifi.send(packet, toPeer)
            ble != null -> ble.send(packet, toPeer)
            else -> transports.firstOrNull { it.isAvailable }?.send(packet, toPeer)
        }
    }
}
