package chat.bitchat

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<BitchatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<String>>(emptyList())
    val connectedPeers = _connectedPeers.asStateFlow()

    val meshService = BluetoothMeshService(this)

    fun addMessage(content: String, sender: String = "me") {
        val message = BitchatMessage(
            sender = sender,
            content = content,
            timestamp = Date(),
            isRelay = false
        )
        _messages.value = _messages.value + message
        meshService.send(message)
    }

    fun receive(message: BitchatMessage) {
        _messages.value = _messages.value + message
    }

    fun updatePeers(peers: List<String>) {
        _connectedPeers.value = peers
    }
}
