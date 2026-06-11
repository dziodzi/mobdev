package io.github.mobdev.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.Message
import io.github.mobdev.data.TokenStorage
import io.github.mobdev.data.Unauthorized401Exception
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MainUiEvent {
    object Unauthorized : MainUiEvent()
    data class Error(val message: String) : MainUiEvent()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val tokenStorage = TokenStorage(app)
    private val repo = ChatRepository()

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

    private val _event = MutableStateFlow<MainUiEvent?>(null)
    val event: StateFlow<MainUiEvent?> = _event.asStateFlow()

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
                _messages.value = result.reversed()
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

    fun loadMoreMessages() {
        val channel = _selectedChannel.value ?: return
        val token = tokenStorage.token ?: return
        if (_isLoadingMore.value || !_hasMoreMessages.value) return
        val oldestId = _messages.value.firstOrNull()?.id?.toLongOrNull() ?: return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val result = repo.getMessages(token, channel, lastKnownId = oldestId)
                val older = result.reversed()
                _messages.value = older + _messages.value
                _hasMoreMessages.value = result.size >= 20
            } catch (e: Unauthorized401Exception) {
                handleUnauthorized()
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
            try {
                repo.sendMessage(token, username, channel, text)
                // Reload to show new message
                val result = repo.getMessages(token, channel, lastKnownId = Long.MAX_VALUE)
                _messages.value = result.reversed()
            } catch (e: Unauthorized401Exception) {
                handleUnauthorized()
            } catch (e: Exception) {
                _event.value = MainUiEvent.Error(e.message ?: "")
            }
        }
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
