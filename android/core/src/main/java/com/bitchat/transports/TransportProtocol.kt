//
// TransportProtocol.kt
// bitchat Android
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

package com.bitchat.transports

import com.bitchat.model.BitchatPacket
import com.bitchat.model.PeerInfo

interface TransportProtocol {
    val transportType: TransportType
    val isAvailable: Boolean
    val currentPeers: List<PeerInfo>

    fun startDiscovery()
    fun stopDiscovery()
    fun send(packet: BitchatPacket, toPeer: String?)
    fun setDelegate(delegate: TransportDelegate)
}
