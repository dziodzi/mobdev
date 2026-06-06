package io.github.mobdev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.R
import io.github.mobdev.api.ApiClient
import io.github.mobdev.data.ChatError
import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.NetworkMonitor
import io.github.mobdev.data.SessionManager
import io.github.mobdev.data.local.ChatDatabase
import io.github.mobdev.data.toChatError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository: ChatRepository

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        val api = ApiClient.create(sessionManager) {
            viewModelScope.launch { handleUnauthorized() }
        }
        val dao = ChatDatabase.create(application).chatDao()
        val networkMonitor = NetworkMonitor(application)
        repository = ChatRepository(api, sessionManager, dao, networkMonitor)

        observeNetwork()

        if (sessionManager.hasSavedCredentials) {
            _uiState.update {
                it.copy(
                    username = sessionManager.username.orEmpty(),
                    password = sessionManager.password.orEmpty(),
                    skipLoginScreen = true,
                    isLoading = true,
                )
            }
            autoLogin()
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            repository.networkStatus.collect { online ->
                val wasOffline = !_uiState.value.isOnline
                _uiState.update { it.copy(isOnline = online) }
                if (online && wasOffline) {
                    onNetworkRestored()
                }
            }
        }
    }

    private fun onNetworkRestored() {
        viewModelScope.launch {
            repository.flushPendingMessages()
            if (!_uiState.value.channelsLoaded) {
                loadChannelsIfNeeded(force = true)
            } else {
                repository.getChannels()
            }
            refreshCurrentChannel()
        }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorDialogMessage = null) }
            repository.login(state.username, state.password)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            skipLoginScreen = true,
                            screen = AppScreen.CHATS,
                        )
                    }
                    loadChannelsIfNeeded(force = true)
                }
                .onFailure { error -> handleError(error, isLogin = true) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = ChatUiState(
                username = sessionManager.username.orEmpty(),
                password = sessionManager.password.orEmpty(),
                screen = AppScreen.LOGIN,
            )
        }
    }

    fun loadChannelsIfNeeded(force: Boolean = false) {
        if (_uiState.value.channelsLoaded && !force) return
        viewModelScope.launch {
            val cached = repository.getCachedChannels()
            if (cached.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        channels = cached,
                        channelsLoaded = true,
                        screen = if (it.screen == AppScreen.LOGIN) AppScreen.CHATS else it.screen,
                        isLoading = false,
                    )
                }
            }

            if (!repository.isOnline()) {
                if (cached.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false) }
                }
                return@launch
            }

            if (cached.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }

            repository.getChannels()
                .onSuccess { channels ->
                    _uiState.update {
                        it.copy(
                            channels = channels,
                            isLoading = false,
                            channelsLoaded = true,
                            screen = if (it.screen == AppScreen.LOGIN) AppScreen.CHATS else it.screen,
                        )
                    }
                }
                .onFailure { error -> handleError(error, hasCache = cached.isNotEmpty()) }
        }
    }

    fun selectChannel(channel: String) {
        val current = _uiState.value
        if (current.selectedChannel == channel &&
            current.messagesLoadedForChannel == channel &&
            current.screen != AppScreen.LOGIN
        ) {
            if (current.screen != AppScreen.MESSAGES && current.screen != AppScreen.IMAGE) {
                _uiState.update { it.copy(screen = AppScreen.MESSAGES) }
            }
            return
        }

        viewModelScope.launch {
            val cached = repository.getChannelMessages(channel)
            val hasMore = computeHasMore(channel, cached)
            _uiState.update {
                it.copy(
                    selectedChannel = channel,
                    messages = cached,
                    hasMoreMessages = hasMore,
                    messagesLoadedForChannel = channel,
                    screen = AppScreen.MESSAGES,
                    imageLink = null,
                    isLoading = cached.isEmpty() && repository.isOnline(),
                )
            }
            if (repository.isOnline()) {
                loadMessages(refresh = true)
            }
        }
    }

    fun loadMessages(refresh: Boolean = false) {
        val channel = _uiState.value.selectedChannel ?: return
        if (!refresh && _uiState.value.messagesLoadedForChannel == channel) return
        viewModelScope.launch {
            val showLoading = _uiState.value.messages.isEmpty()
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true) }
            }
            repository.getLatestMessages(channel)
                .onSuccess { messages ->
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            isLoading = false,
                            hasMoreMessages = computeHasMore(channel, messages),
                            messagesLoadedForChannel = channel,
                        )
                    }
                }
                .onFailure { error ->
                    handleError(error, hasCache = _uiState.value.messages.isNotEmpty())
                }
        }
    }

    fun loadMoreMessages() {
        val state = _uiState.value
        val channel = state.selectedChannel ?: return
        if (state.isLoadingMore || !state.hasMoreMessages || state.messages.isEmpty()) return

        val oldestId = state.messages.first().id
        if (oldestId < 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.getOlderMessages(channel, oldestId)
                .onSuccess { older ->
                    _uiState.update { current ->
                        val merged = (older + current.messages)
                            .distinctBy { it.id }
                            .sortedBy { it.id }
                        current.copy(
                            messages = merged,
                            isLoadingMore = false,
                            hasMoreMessages = computeHasMore(channel, merged, older.size),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingMore = false) }
                    handleError(error, hasCache = state.messages.isNotEmpty())
                }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val channel = _uiState.value.selectedChannel ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            repository.sendTextMessage(channel, trimmed)
                .onSuccess {
                    val messages = repository.getChannelMessages(channel)
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            isSending = false,
                            messagesLoadedForChannel = channel,
                        )
                    }
                    if (repository.isOnline()) {
                        refreshCurrentChannel()
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSending = false) }
                    handleError(error)
                }
        }
    }

    private fun refreshCurrentChannel() {
        val channel = _uiState.value.selectedChannel ?: return
        viewModelScope.launch {
            repository.getLatestMessages(channel)
                .onSuccess { messages ->
                    _uiState.update { current ->
                        current.copy(
                            messages = messages,
                            hasMoreMessages = computeHasMore(channel, messages),
                            messagesLoadedForChannel = channel,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private suspend fun computeHasMore(
        channel: String,
        messages: List<ChatMessage>,
        lastBatchSize: Int = ChatRepository.PAGE_SIZE,
    ): Boolean {
        val oldestServerId = messages.firstOrNull { it.id > 0 }?.id ?: return false
        return lastBatchSize >= ChatRepository.PAGE_SIZE ||
            repository.hasMoreCachedMessages(channel, oldestServerId)
    }

    fun openImage(link: String) {
        _uiState.update { it.copy(screen = AppScreen.IMAGE, imageLink = link) }
    }

    fun closeImage() {
        _uiState.update { it.copy(screen = AppScreen.MESSAGES, imageLink = null) }
    }

    fun navigateToChats() {
        _uiState.update {
            it.copy(
                screen = AppScreen.CHATS,
                selectedChannel = null,
                imageLink = null,
            )
        }
    }

    fun closeChatInLandscape() {
        _uiState.update {
            it.copy(
                selectedChannel = null,
                messages = emptyList(),
                messagesLoadedForChannel = null,
                imageLink = null,
                screen = AppScreen.CHATS,
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorDialogMessage = null) }
    }

    private fun autoLogin() {
        val name = sessionManager.username.orEmpty()
        val pwd = sessionManager.password.orEmpty()
        viewModelScope.launch {
            if (!repository.isOnline() && sessionManager.token != null) {
                enterChatsFromCache()
                return@launch
            }

            repository.login(name, pwd)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            skipLoginScreen = true,
                            screen = AppScreen.CHATS,
                        )
                    }
                    loadChannelsIfNeeded(force = true)
                }
                .onFailure { error ->
                    if (error.toChatError() is ChatError.Network && sessionManager.token != null) {
                        enterChatsFromCache()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                skipLoginScreen = false,
                                screen = AppScreen.LOGIN,
                            )
                        }
                        handleError(error, isLogin = true)
                    }
                }
        }
    }

    private suspend fun enterChatsFromCache() {
        val cached = repository.getCachedChannels()
        _uiState.update {
            it.copy(
                isLoading = false,
                skipLoginScreen = true,
                screen = AppScreen.CHATS,
                channels = cached,
                channelsLoaded = cached.isNotEmpty(),
                isOnline = false,
            )
        }
    }

    private fun handleUnauthorized() {
        _uiState.update {
            ChatUiState(
                username = sessionManager.username.orEmpty(),
                password = sessionManager.password.orEmpty(),
                screen = AppScreen.LOGIN,
                skipLoginScreen = false,
                errorDialogMessage = getApplication<Application>().getString(R.string.error_session_expired),
            )
        }
    }

    private fun handleError(
        error: Throwable,
        isLogin: Boolean = false,
        hasCache: Boolean = false,
    ) {
        val app = getApplication<Application>()
        when (val chatError = error.toChatError()) {
            ChatError.Unauthorized -> {
                if (isLogin) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            skipLoginScreen = false,
                            screen = AppScreen.LOGIN,
                            errorDialogMessage = app.getString(R.string.error_invalid_credentials),
                        )
                    }
                } else {
                    handleUnauthorized()
                }
            }
            ChatError.InvalidCredentials -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        skipLoginScreen = false,
                        screen = AppScreen.LOGIN,
                        errorDialogMessage = app.getString(R.string.error_invalid_credentials),
                    )
                }
            }
            is ChatError.Network -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorDialogMessage = if (hasCache) {
                            null
                        } else {
                            app.getString(R.string.error_network)
                        },
                    )
                }
            }
            is ChatError.Unknown -> {
                val message = if (isLogin) {
                    app.getString(R.string.error_invalid_credentials)
                } else if (hasCache) {
                    null
                } else {
                    chatError.message
                }
                _uiState.update {
                    it.copy(isLoading = false, isLoadingMore = false, errorDialogMessage = message)
                }
            }
        }
    }
}
