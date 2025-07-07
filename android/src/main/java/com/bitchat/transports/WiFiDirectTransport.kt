//
// WiFiDirectTransport.kt
// bitchat Android
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

package com.bitchat.transports

import com.bitchat.model.BitchatPacket
import com.bitchat.model.PeerInfo

/** Placeholder implementation for future WiFi Direct transport */
class WiFiDirectTransport : TransportProtocol {
    override val transportType = TransportType.WIFI_DIRECT
    override val isAvailable: Boolean
        get() = false
    override val currentPeers: List<PeerInfo>
        get() = emptyList()

    override fun startDiscovery() {
        // TODO: Implement WiFi Direct discovery
    }

    override fun stopDiscovery() {
        // TODO: Implement stop logic
    }

    override fun send(packet: BitchatPacket, toPeer: String?) {
        // TODO: Implement send over WiFi Direct
    }

    override fun setDelegate(delegate: TransportDelegate) {
        // No-op for now
    }
}
