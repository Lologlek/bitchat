package chat.bitchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import chat.bitchat.service.TransportLayer
import chat.bitchat.service.TransportManagerLayer
import chat.bitchat.viewmodel.ChatViewModel
import chat.bitchat.ui.MainScreen

class MainActivity : ComponentActivity() {
    private val transport: TransportLayer by lazy { TransportManagerLayer(this) }
    private val viewModel by lazy { ChatViewModel(transport) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel)
        }
    }
}
