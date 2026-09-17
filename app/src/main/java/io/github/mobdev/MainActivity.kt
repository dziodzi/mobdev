package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.github.mobdev.data.repository.ChatRepository
import io.github.mobdev.data.session.CredentialsStore
import io.github.mobdev.data.session.SessionManager
import io.github.mobdev.ui.ChatApp
import io.github.mobdev.ui.ChatViewModel

class MainActivity : ComponentActivity() {

    private val sessionManager by lazy { SessionManager() }
    private val credentialsStore by lazy { CredentialsStore(applicationContext) }
    private val repository by lazy { ChatRepository.create(sessionManager) }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.Factory(repository, credentialsStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatApp(viewModel = viewModel)
        }
    }
}
