package io.github.mobdev.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.Message
import io.github.mobdev.data.NetworkMonitor
import io.github.mobdev.data.TokenStorage
import io.github.mobdev.data.Unauthorized401Exception
import io.github.mobdev.data.db.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class MainUiEvent {
    object Unauthorized : MainUiEvent()
    data class Error(val message: String) : MainUiEvent()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val tokenStorage = TokenStorage(app)
    private val repo = ChatRepository(AppDatabase.getInstance(app))
    private val networkMonitor = NetworkMonitor(app)

    // Channels
    private val _channels = MutableStateFlow<List<String>>(emptyList())
    val channels: StateFlow<List<String>> = _channels.asStateFlow()

    // Selected channel (shared between portrait nav and landscape pane)
    private val _selectedChannel = MutableStateFlow<String?>(null)
    val selectedChannel: StateFlow<String?> = _selectedChannel.asStateFlow()

    // Messages
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    // Network status, used to show an offline banner and decide whether
    // outgoing messages can be sent right away or must be queued.
    private val _isOnline = MutableStateFlow(networkMonitor.hasActiveConnection())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _event = MutableStateFlow<MainUiEvent?>(null)
    val event: StateFlow<MainUiEvent?> = _event.asStateFlow()

    init {
        observeNetwork()
    }

    /** Watches connectivity and re-syncs everything as soon as the network comes back. */
    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                val cameBackOnline = online && !_isOnline.value
                _isOnline.value = online
                if (cameBackOnline) {
                    onNetworkRestored()
                }
            }
        }
    }

    private fun onNetworkRestored() {
        val token = tokenStorage.token ?: return
        viewModelScope.launch {
            try {
                repo.flushPendingMessages(token)
            } catch (e: Unauthorized401Exception) {
                handleUnauthorized()
                return@launch
            } catch (e: Exception) {
                // Still offline / server unreachable - the queue stays for next time.
            }
            loadChannels()
            _selectedChannel.value?.let { refreshMessages(it) }
        }
    }

    fun loadChannels() {
        val token = tokenStorage.token ?: return
        viewModelScope.launch {
            try {
                _channels.value = repo.getChannels(token)
            } catch (e: Unauthorized401Exception) {
                handleUnauthorized()
            } catch (e: Exception) {
                _event.value = MainUiEvent.Error(e.message ?: "")
            }
        }
    }

    fun selectChannel(channel: String) {
        if (_selectedChannel.value == channel) return
        _selectedChannel.value = channel
        _messages.value = emptyList()
        _hasMoreMessages.value = true
        loadMessages(channel)
    }

    fun clearSelectedChannel() {
        _selectedChannel.value = null
    }

    private fun loadMessages(channel: String) {
        val token = tokenStorage.token ?: return
        viewModelScope.launch {
            _isLoadingMessages.value = true
            try {
                val result = repo.getMessages(token, channel, lastKnownId = Long.MAX_VALUE)
                applyMessages(channel, result.reversed())
                _hasMoreMessages.value = result.size >= 20
            } catch (e: Unauthorized401Exception) {
                handleUnauthorized()
            } catch (e: Exception) {
                _event.value = MainUiEvent.Error(e.message ?: "")
            } finally {
                _isLoadingMessages.value = false
            }
        }
    }

    /** Silent reload used after sending a message or reconnecting - no error dialogs. */
    private fun refreshMessages(channel: String) {
        val token = tokenStorage.token ?: return
        viewModelScope.launch {
            try {
                val result = repo.getMessages(token, channel, lastKnownId = Long.MAX_VALUE)
                applyMessages(channel, result.reversed())
                _hasMoreMessages.value = result.size >= 20
            } catch (e: Unauthorized401Exception) {
                handleUnauthorized()
            } catch (e: Exception) {
                // Network still down - keep showing what we already have.
            }
        }
    }

    /** Combines server messages with locally queued ones, removing duplicates. */
    private suspend fun applyMessages(channel: String, serverMessages: List<Message>) {
        val pending = repo.getPendingMessages(channel)
        _messages.value = (serverMessages + pending).distinctBy { it.id }
    }

    fun loadMoreMessages() {
        val channel = _selectedChannel.value ?: return
        val token = tokenStorage.token ?: return
        if (_isLoadingMore.value || !_hasMoreMessages.value) return
        val oldestId = _messages.value.firstOrNull { !it.pending }?.id?.toLongOrNull() ?: return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val result = repo.getMessages(token, channel, lastKnownId = oldestId)
                val older = result.reversed()
                _messages.value = (older + _messages.value).distinctBy { it.id }
                _hasMoreMessages.value = result.size >= 20
            } catch (e: Unauthorized401Exception) {
                handleUnauthorized()
            } catch (e: IOException) {
                // Offline - nothing older to show, try again later.
            } catch (e: Exception) {
                _event.value = MainUiEvent.Error(e.message ?: "")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        val channel = _selectedChannel.value ?: return
        val token = tokenStorage.token ?: return
        val username = tokenStorage.savedUsername ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            if (_isOnline.value) {
                try {
                    repo.sendMessage(token, username, channel, text)
                    refreshMessages(channel)
                } catch (e: Unauthorized401Exception) {
                    handleUnauthorized()
                } catch (e: IOException) {
                    // Network dropped right as we tried to send - queue it instead.
                    queueMessage(channel, username, text)
                } catch (e: Exception) {
                    _event.value = MainUiEvent.Error(e.message ?: "")
                }
            } else {
                queueMessage(channel, username, text)
            }
        }
    }

    /** Persists the message and shows it immediately with a "pending" marker. */
    private suspend fun queueMessage(channel: String, username: String, text: String) {
        repo.queueMessage(channel, username, text)
        val pending = repo.getPendingMessages(channel)
        _messages.value = (_messages.value.filterNot { it.pending } + pending).distinctBy { it.id }
    }

    fun logout(onDone: () -> Unit) {
        val token = tokenStorage.token ?: run { onDone(); return }
        viewModelScope.launch {
            repo.logout(token)
            tokenStorage.clear()
            _channels.value = emptyList()
            _selectedChannel.value = null
            _messages.value = emptyList()
            onDone()
        }
    }

    fun consumeEvent() {
        _event.value = null
    }

    private fun handleUnauthorized() {
        tokenStorage.clear()
        _event.value = MainUiEvent.Unauthorized
    }
}
