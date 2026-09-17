package io.github.mobdev.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.R
import io.github.mobdev.data.mapper.messageIdAsLong
import io.github.mobdev.data.repository.ChatRepository
import io.github.mobdev.data.session.CredentialsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val credentialsStore: CredentialsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = credentialsStore.savedCredentials.first()
            if (saved != null) {
                _state.update {
                    it.copy(
                        username = saved.username,
                        password = saved.password,
                    )
                }
                loginInternal(saved.username, saved.password, saveCredentials = false)
            }
        }

        viewModelScope.launch {
            repository.unauthorized.collect {
                credentialsStore.clear()
                _state.value = ChatUiState(error = UiText(R.string.error_session_expired))
            }
        }
    }

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onMessageTextChange(value: String) {
        _state.update { it.copy(messageText = value, error = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun login() {
        val username = _state.value.username.trim()
        val password = _state.value.password

        if (username.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = UiText(R.string.error_empty_credentials)) }
            return
        }

        viewModelScope.launch {
            loginInternal(username, password, saveCredentials = true)
        }
    }

    private suspend fun loginInternal(
        username: String,
        password: String,
        saveCredentials: Boolean,
    ) {
        _state.update { it.copy(isLoading = true, error = null) }

        repository.login(username, password)
            .onSuccess {
                if (saveCredentials) {
                    credentialsStore.save(username, password)
                }
                _state.update {
                    it.copy(
                        username = username,
                        password = password,
                        isLoggedIn = true,
                        isLoading = false,
                        error = null,
                    )
                }
                loadChannels()
            }
            .onFailure { throwable ->
                if (throwable is ChatRepository.HttpException && throwable.code == HTTP_UNAUTHORIZED) {
                    credentialsStore.clear()
                }
                _state.update {
                    it.copy(
                        isLoggedIn = false,
                        isLoading = false,
                        error = throwable.toReadableMessage(isLogin = true),
                    )
                }
            }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            credentialsStore.clear()
            _state.value = ChatUiState()
        }
    }

    fun loadChannels() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.fetchChannels()
                .onSuccess { channels ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            channels = channels,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.toReadableMessage(),
                        )
                    }
                }
        }
    }

    fun openChannel(channel: String) {
        if (_state.value.selectedChannel == channel) return

        _state.update {
            it.copy(
                selectedChannel = channel,
                messages = emptyList(),
                hasMoreMessages = true,
                messageText = "",
                error = null,
            )
        }
        loadMessages(channel)
    }

    fun closeChannel() {
        _state.update {
            it.copy(
                selectedChannel = null,
                messages = emptyList(),
                hasMoreMessages = true,
                messageText = "",
                openedImagePath = null,
                error = null,
            )
        }
    }

    fun loadMessages(channel: String? = null) {
        val targetChannel = channel ?: _state.value.selectedChannel ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.fetchMessages(channel = targetChannel)
                .onSuccess { messages ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            hasMoreMessages = messages.size == ChatRepository.PAGE_SIZE,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.toReadableMessage(),
                        )
                    }
                }
        }
    }

    fun loadOlderMessages() {
        val current = _state.value
        val channel = current.selectedChannel ?: return
        val firstMessageId = current.messages.minOfOrNull { messageIdAsLong(it.id) } ?: return
        if (current.isLoadingMore || !current.hasMoreMessages) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, error = null) }

            repository.fetchMessages(
                channel = channel,
                lastKnownId = firstMessageId,
                reverse = true,
            )
                .onSuccess { olderMessages ->
                    _state.update { oldState ->
                        val merged = (olderMessages + oldState.messages)
                            .distinctBy { it.id }
                            .sortedBy { messageIdAsLong(it.id) }

                        oldState.copy(
                            isLoadingMore = false,
                            messages = merged,
                            hasMoreMessages = olderMessages.size == ChatRepository.PAGE_SIZE,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            error = throwable.toReadableMessage(),
                        )
                    }
                }
        }
    }

    fun sendMessage() {
        val current = _state.value
        val channel = current.selectedChannel ?: return
        val text = current.messageText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.sendTextMessage(
                username = current.username.trim(),
                channel = channel,
                text = text,
            )
                .onSuccess {
                    _state.update { it.copy(messageText = "", isLoading = false) }
                    loadMessages(channel)
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.toReadableMessage(),
                        )
                    }
                }
        }
    }

    fun openImage(path: String) {
        _state.update { it.copy(openedImagePath = path) }
    }

    fun closeImage() {
        _state.update { it.copy(openedImagePath = null) }
    }

    fun imageUrl(path: String, fullResolution: Boolean = false): String {
        return repository.imageUrl(path, fullResolution)
    }

    fun isMyMessage(from: String): Boolean {
        return from == _state.value.username.trim()
    }

    private fun Throwable.toReadableMessage(isLogin: Boolean = false): UiText {
        return when (this) {
            is ChatRepository.HttpException -> when {
                isLogin && code == HTTP_UNAUTHORIZED -> UiText(R.string.error_invalid_credentials)
                code == HTTP_UNAUTHORIZED -> UiText(R.string.error_session_expired)
                else -> UiText(R.string.error_http, listOf(code))
            }

            else -> UiText(R.string.error_unknown)
        }
    }

    class Factory(
        private val repository: ChatRepository,
        private val credentialsStore: CredentialsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(repository, credentialsStore) as T
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
