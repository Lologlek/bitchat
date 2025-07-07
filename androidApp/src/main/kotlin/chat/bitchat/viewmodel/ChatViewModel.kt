package chat.bitchat.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import chat.bitchat.model.BitchatMessage
import chat.bitchat.model.DeliveryStatus
import chat.bitchat.service.TransportDelegate
import chat.bitchat.service.TransportLayer
import chat.bitchat.service.EncryptionService
import java.util.Date

class ChatViewModel(
    private val transport: TransportLayer,
    private val encryptionService: EncryptionService? = null
) : ViewModel(), TransportDelegate {

    var nickname by mutableStateOf("user" + (1000..9999).random())
    val messages = mutableStateListOf<BitchatMessage>()
    val connectedPeers = mutableStateListOf<String>()
    val privateChats = mutableStateMapOf<String, MutableList<BitchatMessage>>()
    var selectedPrivateChatPeer by mutableStateOf<String?>(null)

    init {
        transport.setDelegate(this)
        transport.start()
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        selectedPrivateChatPeer?.let { peerId ->
            sendPrivateMessage(content, peerId)
            return
        }

        val message = BitchatMessage(
            sender = nickname,
            content = content,
            timestamp = Date(),
            isRelay = false,
            senderPeerID = null
        )
        messages.add(message)
        transport.sendMessage(message, null)
    }

    fun sendPrivateMessage(content: String, peerId: String) {
        if (content.isBlank()) return
        val encrypted = encryptionService?.encrypt(content.toByteArray(), peerId)
        val message = BitchatMessage(
            sender = nickname,
            content = if (encrypted == null) content else "",
            timestamp = Date(),
            isRelay = false,
            isPrivate = true,
            recipientNickname = peerId,
            deliveryStatus = DeliveryStatus.Sending,
            encryptedContent = encrypted,
            isEncrypted = encrypted != null
        )
        val list = privateChats.getOrPut(peerId) { mutableListOf() }
        list.add(message)
        transport.sendMessage(message, peerId)
    }

    override fun onMessageReceived(message: BitchatMessage) {
        val decoded = if (
            message.isEncrypted &&
            message.encryptedContent != null &&
            message.senderPeerID != null &&
            encryptionService != null
        ) {
            val plain = encryptionService.decrypt(message.encryptedContent, message.senderPeerID)
            message.copy(
                content = String(plain),
                encryptedContent = null,
                isEncrypted = false
            )
        } else message

        if (decoded.isPrivate) {
            val list = privateChats.getOrPut(decoded.senderPeerID ?: decoded.sender) { mutableListOf() }
            list.add(decoded)
        } else {
            messages.add(decoded)
        }
    }

    override fun onPeersUpdated(peers: List<String>) {
        connectedPeers.clear()
        connectedPeers.addAll(peers)
    }
}
