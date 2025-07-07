package chat.bitchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import chat.bitchat.service.TransportLayer
import chat.bitchat.service.TransportManagerLayer
import chat.bitchat.viewmodel.ChatViewModel
import chat.bitchat.ui.MainScreen

class MainActivity : ComponentActivity() {
    private lateinit var transport: TransportLayer
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transport = TransportManagerLayer(this)
        viewModel = ChatViewModel(transport)

        setContent {
            MainScreen(viewModel)
        }
    }
}
