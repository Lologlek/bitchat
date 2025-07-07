package chat.bitchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ChatViewModel = viewModel()
            ChatScreen(viewModel)
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages = viewModel.messages.collectAsState()
    MaterialTheme {
        Surface {
            for (m in messages.value) {
                Text(text = "${m.sender}: ${m.content}")
            }
        }
    }
}

@Preview
@Composable
fun PreviewChat() {
    val vm = ChatViewModel()
    ChatScreen(vm)
}
