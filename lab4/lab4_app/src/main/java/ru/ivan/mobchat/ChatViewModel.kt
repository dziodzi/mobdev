package ru.ivan.mobchat

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.ivan.mobchat.data.ApiException
import ru.ivan.mobchat.data.ApiFactory
import ru.ivan.mobchat.data.CachedChatState
import ru.ivan.mobchat.data.ChatApi
import ru.ivan.mobchat.data.ChatMessage
import ru.ivan.mobchat.data.ChatRepository
import ru.ivan.mobchat.data.MessageData
import ru.ivan.mobchat.data.NetworkMonitor
import ru.ivan.mobchat.data.OfflineCache
import ru.ivan.mobchat.data.TextPayload
import java.io.IOException
import java.util.UUID

data class ChatUiState(
    val authenticated: Boolean = false,
    val username: String = "",
    val selectedChannel: String? = null,
    val channels: List<String> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val loginLoading: Boolean = false,
    val channelsLoading: Boolean = false,
    val messagesLoading: Boolean = false,
    val sending: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val draft: String = "",
    val channelsError: Boolean = false,
    val messagesError: Boolean = false,
    val offline: Boolean = false,
    val sentMessageId: String? = null
)

sealed interface UiEvent {
    data object InvalidCredentials : UiEvent
    data object NetworkError : UiEvent
    data object ServerError : UiEvent
    data object Unauthorized : UiEvent
}

class Event<out T>(private val content: T) {
    private var handled = false
    fun getContentIfNotHandled(): T? = if (handled) null else content.also { handled = true }
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("chat_session", 0)
    private val offlineCache = OfflineCache(application)
    @Volatile private var token: String? = null
    private val repository = ChatRepository(ApiFactory.create { token })
    private var messagesByChannel: Map<String, List<ChatMessage>> = emptyMap()
    private var pendingMessages: List<ChatMessage> = emptyList()
    private var latestKnownIds: Map<String, String> = emptyMap()
    private var drafts: Map<String, String> = emptyMap()
    private var exhaustedChannels: Set<String> = emptySet()
    private var loadingChannels: Set<String> = emptySet()
    private var sendingChannels: Set<String> = emptySet()
    private var channelsLoaded = false
    private var session = 0
    private var savedName: String? = null
    private var savedPassword: String? = null
    private lateinit var networkMonitor: NetworkMonitor
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableLiveData(ChatUiState())
    val state: LiveData<ChatUiState> = _state
    private val _events = MutableLiveData<Event<UiEvent>>()
    val events: LiveData<Event<UiEvent>> = _events

    init {
        val cached = offlineCache.read()
        val name = preferences.getString("name", null)
        val password = preferences.getString("password", null)
        savedName = name
        savedPassword = password
        messagesByChannel = cached.messages
        pendingMessages = cached.pendingMessages
        channelsLoaded = cached.channels.isNotEmpty()
        val hasCachedSession = !name.isNullOrBlank() &&
            (cached.username.isNotBlank() || cached.channels.isNotEmpty() || cached.messages.isNotEmpty())
        _state.value = ChatUiState(
            authenticated = hasCachedSession,
            username = cached.username.ifBlank { name.orEmpty() },
            channels = cached.channels,
            offline = !isNetworkAvailable()
        )
        networkMonitor = NetworkMonitor(
            application,
            onAvailable = { mainHandler.post(::onNetworkAvailable) },
            onLost = {
                if (!networkMonitor.isOnline()) {
                    mainHandler.post { update { it.copy(offline = true) } }
                }
            }
        )
        networkMonitor.register()
        if (!name.isNullOrBlank() && !password.isNullOrBlank()) login(name, password)
    }

    fun login(name: String, password: String) {
        if (name.isBlank() || password.isBlank() || _state.value?.loginLoading == true) return
        val generation = ++session
        update { it.copy(loginLoading = true) }
        viewModelScope.launch {
            try {
                val newToken = repository.login(name.trim(), password)
                if (generation != session) return@launch
                if (newToken.isBlank()) throw IOException()
                token = newToken
                savedName = name.trim()
                savedPassword = password
                preferences.edit().putString("name", name.trim()).putString("password", password).apply()
                update {
                    it.copy(
                        authenticated = true,
                        username = name.trim(),
                        loginLoading = false,
                        offline = false,
                        channelsError = false
                    )
                }
                persistCache()
                loadChannels(force = true)
                sendPendingMessages()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation != session) return@launch
                update { it.copy(loginLoading = false, offline = error is IOException || it.offline) }
                if (error is ApiException && error.statusCode == 401) {
                    clearSession()
                    emit(UiEvent.InvalidCredentials)
                } else handleRequestError(error)
            }
        }
    }

    fun loadChannels(force: Boolean = false) {
        val current = _state.value ?: return
        if (!current.authenticated || current.channelsLoading || channelsLoaded && !force) return
        val generation = session
        update { it.copy(channelsLoading = true, channelsError = false) }
        viewModelScope.launch {
            try {
                val channels = repository.channels().distinct().sorted()
                if (generation != session) return@launch
                channelsLoaded = true
                update { it.copy(channels = channels, channelsLoading = false, offline = false) }
                persistCache()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation != session) return@launch
                update {
                    it.copy(
                        channelsLoading = false,
                        channelsError = it.channels.isEmpty(),
                        offline = error is IOException || it.offline
                    )
                }
                handleRequestError(error)
            }
        }
    }

    fun selectChannel(channel: String) {
        if (_state.value?.authenticated != true) return
        update {
            it.copy(selectedChannel = channel, messagesError = false, sentMessageId = null)
        }
        publishMessages(channel)
        if (channel !in messagesByChannel) loadMessages(channel)
    }

    fun closeChannel() {
        update { it.copy(selectedChannel = null, messages = emptyList(), messagesLoading = false, draft = "") }
    }

    fun updateDraft(channel: String, text: String) {
        if (_state.value?.selectedChannel != channel || _state.value?.draft == text) return
        drafts = drafts + (channel to text)
        update { it.copy(draft = text) }
    }

    fun refreshMessages() {
        _state.value?.selectedChannel?.let { loadMessages(it, refresh = true) }
    }

    // Refresh walks forward in pages of 20, preserving the already loaded history.
    private fun loadMessages(channel: String, refresh: Boolean = false, older: Boolean = false) {
        if (_state.value?.authenticated != true || channel in loadingChannels) return
        val generation = session
        val cached = messagesByChannel[channel].orEmpty()
        loadingChannels = loadingChannels + channel
        if (_state.value?.selectedChannel == channel) update { it.copy(messagesError = false) }
        publishMessages(channel)
        viewModelScope.launch {
            try {
                var cursor = if (older) cached.firstOrNull()?.id else latestKnownIds[channel]
                do {
                    val page = when {
                        older && cursor != null -> repository.olderMessages(channel, cursor)
                        refresh && cursor != null -> repository.newerMessages(channel, cursor)
                        else -> repository.messages(channel)
                    }
                    if (generation != session) return@launch
                    messagesByChannel = messagesByChannel + (
                        channel to mergeMessages(messagesByChannel[channel].orEmpty(), page)
                    )
                    persistCache()
                    if ((!refresh || cursor == null) && page.size < ChatApi.PAGE_SIZE) {
                        exhaustedChannels = exhaustedChannels + channel
                    }
                    publishMessages(channel)
                    val nextCursor = page.mapNotNull { it.id?.toLongOrNull() }.maxOrNull()?.toString()
                    if (!older) {
                        latestKnownIds = latestKnownIds + (channel to (nextCursor ?: cursor ?: "0"))
                    }
                    val continueLoading = refresh && !older && cursor != null &&
                        page.size == ChatApi.PAGE_SIZE && nextCursor != null &&
                        (nextCursor.toLongOrNull() ?: 0) > (cursor.toLongOrNull() ?: 0)
                    cursor = nextCursor
                } while (continueLoading)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation != session) return@launch
                if (_state.value?.selectedChannel == channel) {
                    update {
                        it.copy(
                            messagesError = it.messages.isEmpty(),
                            offline = error is IOException || it.offline
                        )
                    }
                }
                handleRequestError(error)
            } finally {
                if (generation == session) {
                    loadingChannels = loadingChannels - channel
                    publishMessages(channel)
                }
            }
        }
    }

    fun loadOlderMessages() {
        val channel = _state.value?.selectedChannel ?: return
        if (channel in exhaustedChannels || messagesByChannel[channel].isNullOrEmpty()) return
        loadMessages(channel, older = true)
    }

    fun sendMessage(text: String) {
        val current = _state.value ?: return
        val channel = current.selectedChannel ?: return
        if (!current.authenticated || text.isBlank() || channel in sendingChannels) return
        val generation = session
        val localMessage = ChatMessage(
            id = "local-${UUID.randomUUID()}",
            from = current.username,
            to = channel,
            data = MessageData(text = TextPayload(text.trim()))
        )
        messagesByChannel = messagesByChannel + (
            channel to mergeMessages(messagesByChannel[channel].orEmpty(), listOf(localMessage))
        )
        pendingMessages = pendingMessages + localMessage
        if (drafts[channel] == text) drafts = drafts - channel
        persistCache()
        sendingChannels = sendingChannels + channel
        publishMessages(channel)
        viewModelScope.launch {
            try {
                if (token == null) throw IOException()
                val postedId = repository.send(localMessage.copy(id = null))
                if (generation != session) return@launch
                if (postedId.isBlank()) throw IOException()
                confirmPendingMessage(localMessage, postedId)
                if (_state.value?.selectedChannel == channel) update { it.copy(sentMessageId = postedId) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == session) {
                    if (error is IOException) update { it.copy(offline = true) }
                    handleRequestError(error)
                }
            } finally {
                if (generation == session) {
                    sendingChannels = sendingChannels - channel
                    publishMessages(channel)
                }
            }
        }
    }

    fun logout() {
        val oldToken = token
        clearSession()
        viewModelScope.launch {
            try {
                if (oldToken != null) repository.logout(oldToken)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                // The local session is already closed; stale responses cannot restore it.
            }
        }
    }

    private fun onNetworkAvailable() {
        update { it.copy(offline = false) }
        if (savedName != null && savedPassword != null && token == null) {
            login(savedName.orEmpty(), savedPassword.orEmpty())
        } else if (_state.value?.authenticated == true) {
            loadChannels(force = true)
            refreshMessages()
            sendPendingMessages()
        }
    }

    private fun isNetworkAvailable(): Boolean = runCatching {
        val connectivityManager = getApplication<Application>()
            .getSystemService(android.net.ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return@runCatching false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return@runCatching false
        capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)

    private fun sendPendingMessages() {
        if (token == null || pendingMessages.isEmpty()) return
        val generation = session
        viewModelScope.launch {
            for (pending in pendingMessages.toList()) {
                if (generation != session || token == null) return@launch
                val channel = pending.to
                if (channel in sendingChannels) continue
                sendingChannels = sendingChannels + channel
                publishMessages(channel)
                try {
                    val postedId = repository.send(pending.copy(id = null))
                    if (postedId.isBlank()) throw IOException()
                    confirmPendingMessage(pending, postedId)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    if (error is IOException) update { it.copy(offline = true) }
                    break
                } finally {
                    sendingChannels = sendingChannels - channel
                    publishMessages(channel)
                }
            }
        }
    }

    private fun confirmPendingMessage(local: ChatMessage, serverId: String) {
        val confirmed = local.copy(
            id = serverId,
            time = (System.currentTimeMillis() / 1000L).toString()
        )
        val channelMessages = messagesByChannel[local.to].orEmpty().map {
            if (it.id == local.id) confirmed else it
        }
        messagesByChannel = messagesByChannel + (local.to to mergeMessages(emptyList(), channelMessages))
        pendingMessages = pendingMessages.filterNot { it.id == local.id }
        persistCache()
    }

    private fun publishMessages(channel: String) {
        if (_state.value?.selectedChannel != channel) return
        update {
            it.copy(
                messages = messagesByChannel[channel].orEmpty(),
                messagesLoading = channel in loadingChannels,
                sending = channel in sendingChannels,
                draft = drafts[channel].orEmpty(),
                hasMoreMessages = channel !in exhaustedChannels
            )
        }
    }

    private fun handleRequestError(error: Exception) {
        when {
            error is ApiException && error.statusCode == 401 -> {
                clearSession()
                emit(UiEvent.Unauthorized)
            }
            error is IOException -> {
                update { it.copy(offline = true) }
                emit(UiEvent.NetworkError)
            }
            else -> emit(UiEvent.ServerError)
        }
    }

    private fun clearSession() {
        session++
        token = null
        channelsLoaded = false
        messagesByChannel = emptyMap()
        latestKnownIds = emptyMap()
        exhaustedChannels = emptySet()
        loadingChannels = emptySet()
        sendingChannels = emptySet()
        drafts = emptyMap()
        pendingMessages = emptyList()
        offlineCache.clear()
        preferences.edit().clear().apply()
        update { ChatUiState() }
    }

    private fun update(transform: (ChatUiState) -> ChatUiState) {
        _state.value = transform(_state.value ?: ChatUiState())
    }

    private fun emit(event: UiEvent) {
        _events.value = Event(event)
    }

    private fun persistCache() {
        val current = _state.value ?: return
        offlineCache.write(
            CachedChatState(
                username = current.username,
                channels = current.channels,
                messages = messagesByChannel,
                pendingMessages = pendingMessages
            )
        )
    }

    override fun onCleared() {
        if (::networkMonitor.isInitialized) networkMonitor.unregister()
        super.onCleared()
    }
}

internal fun mergeMessages(existing: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> =
    (existing + incoming).associateBy { it.id }.values.sortedWith(
        compareBy<ChatMessage> { it.id?.toLongOrNull() ?: Long.MAX_VALUE }.thenBy { it.id.orEmpty() }
    )
