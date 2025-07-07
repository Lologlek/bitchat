package chat.bitchat.service

import chat.bitchat.model.BitchatMessage

interface TransportLayer {
    fun start()
    fun stop()
    fun sendMessage(message: BitchatMessage, peerId: String? = null)
    fun setDelegate(delegate: TransportDelegate)
}

interface TransportDelegate {
    fun onMessageReceived(message: BitchatMessage)
    fun onPeersUpdated(peers: List<String>)
}
