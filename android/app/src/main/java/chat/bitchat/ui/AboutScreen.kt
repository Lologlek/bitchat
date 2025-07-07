package chat.bitchat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text("bitchat", style = MaterialTheme.typography.headlineMedium)
            Text("Secure mesh chat", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
