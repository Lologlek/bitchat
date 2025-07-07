//
// TransportDelegate.kt
// bitchat Android
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

package com.bitchat.transports

import com.bitchat.model.BitchatPacket
import com.bitchat.model.PeerInfo

interface TransportDelegate {
    fun onPeerDiscovered(peer: PeerInfo)
    fun onPeerLost(peer: PeerInfo)
    fun onPacketReceived(packet: BitchatPacket, fromPeer: PeerInfo?)
}
