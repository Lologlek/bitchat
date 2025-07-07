package chat.bitchat.service

import android.content.Context
import android.bluetooth.BluetoothAdapter
import com.bitchat.model.BitchatPacket
import com.bitchat.model.PeerInfo
import com.bitchat.transports.BluetoothTransport
import com.bitchat.transports.TransportDelegate as LowLevelDelegate
import com.bitchat.transports.TransportManager
import chat.bitchat.model.BitchatMessage
import bitchat.BitchatMessage as CommonMessage

class TransportManagerLayer(private val context: Context) : TransportLayer, LowLevelDelegate {
    private val manager = TransportManager()
    private val bluetooth = BluetoothTransport(context)
    private val peers = mutableSetOf<String>()
    private var delegate: TransportDelegate? = null
    private val senderId: ByteArray =
        BluetoothAdapter.getDefaultAdapter()?.address?.toByteArray() ?: ByteArray(0)

    init {
        manager.registerTransport(bluetooth)
    }

    override fun start() {
        bluetooth.setDelegate(this)
        bluetooth.startDiscovery()
    }

    override fun stop() {
        bluetooth.stopDiscovery()
    }

    override fun sendMessage(message: BitchatMessage, peerId: String?) {
        val common = message.toCommon()
        val payload = common.toBinaryPayload() ?: return
        val packet = BitchatPacket(
            type = 0x04,
            senderID = senderId,
            recipientID = peerId?.toByteArray(),
            payload = payload,
            ttl = 7
        )
        manager.sendOptimal(packet, peerId)
    }

    override fun setDelegate(delegate: TransportDelegate) {
        this.delegate = delegate
    }

    // Low level delegate
    override fun onPeerDiscovered(peer: PeerInfo) {
        peers.add(peer.id)
        delegate?.onPeersUpdated(peers.toList())
    }

    override fun onPeerLost(peer: PeerInfo) {
        peers.remove(peer.id)
        delegate?.onPeersUpdated(peers.toList())
    }

    override fun onPacketReceived(packet: BitchatPacket, fromPeer: PeerInfo?) {
        val common = CommonMessage.fromBinaryPayload(packet.payload) ?: return
        delegate?.onMessageReceived(common.toAppModel())
    }

    // Conversion helpers
    private fun BitchatMessage.toCommon(): CommonMessage = CommonMessage(
        id = id,
        sender = sender,
        content = content,
        timestamp = timestamp,
        isRelay = isRelay,
        originalSender = originalSender,
        isPrivate = isPrivate,
        recipientNickname = recipientNickname,
        senderPeerID = senderPeerID,
        mentions = mentions,
        room = room,
        encryptedContent = encryptedContent,
        isEncrypted = isEncrypted,
        deliveryStatus = deliveryStatus?.let { null }
    )

    private fun CommonMessage.toAppModel(): BitchatMessage = BitchatMessage(
        id = id,
        sender = sender,
        content = content,
        timestamp = timestamp,
        isRelay = isRelay,
        originalSender = originalSender,
        isPrivate = isPrivate,
        recipientNickname = recipientNickname,
        senderPeerID = senderPeerID,
        mentions = mentions,
        room = room,
        encryptedContent = encryptedContent,
        isEncrypted = isEncrypted,
        deliveryStatus = deliveryStatus
    )
}
