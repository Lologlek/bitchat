package chat.bitchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import chat.bitchat.service.TransportLayer
import chat.bitchat.viewmodel.ChatViewModel
import chat.bitchat.ui.MainScreen

class MainActivity(private val transport: TransportLayer) : ComponentActivity() {
    private val viewModel = ChatViewModel(transport)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel)
        }
    }
}
