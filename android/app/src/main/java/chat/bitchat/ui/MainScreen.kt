package chat.bitchat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.bitchat.viewmodel.ChatViewModel

@Composable
fun MainScreen(viewModel: ChatViewModel) {
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.messages) { msg ->
                Text("${'$'}{msg.sender}: ${'$'}{msg.content}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                modifier = Modifier.weight(1f),
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Message") }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                viewModel.sendMessage(message)
                message = ""
            }) {
                Text("Send")
            }
        }
    }
}
