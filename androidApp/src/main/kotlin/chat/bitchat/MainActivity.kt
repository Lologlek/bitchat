package chat.bitchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import chat.bitchat.service.TransportLayer
import chat.bitchat.service.TransportManagerLayer
import chat.bitchat.service.EncryptionService
import chat.bitchat.viewmodel.ChatViewModel
import chat.bitchat.ui.MainScreen

class MainActivity : ComponentActivity() {
    private lateinit var transport: TransportLayer
    private lateinit var viewModel: ChatViewModel
    private lateinit var encryptionKey: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EncryptionService.initialize(this)
        encryptionKey = EncryptionService.getOrCreateKey("default")

        transport = TransportManagerLayer(this)
        viewModel = ChatViewModel(transport, encryptionKey)

        setContent {
            MainScreen(viewModel)
        }
    }
}
