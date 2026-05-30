package ru.tsvetkova.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.tsvetkova.chat.ui.ChatApp
import ru.tsvetkova.chat.ui.theme.ChatLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ChatApplication
        setContent {
            ChatLabTheme {
                ChatApp(app)
            }
        }
    }
}
