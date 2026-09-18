package ru.ivan.mobchat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.ivan.mobchat.data.ApiException
import ru.ivan.mobchat.data.ApiFactory
import ru.ivan.mobchat.data.ChatApi
import ru.ivan.mobchat.data.ChatMessage
import ru.ivan.mobchat.data.ChatRepository
import ru.ivan.mobchat.data.MessageData
import ru.ivan.mobchat.data.TextPayload
import java.io.IOException

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
    @Volatile private var token: String? = null
    private val repository = ChatRepository(ApiFactory.create { token })
    private var messagesByChannel: Map<String, List<ChatMessage>> = emptyMap()
    private var latestKnownIds: Map<String, String> = emptyMap()
    private var drafts: Map<String, String> = emptyMap()
    private var exhaustedChannels: Set<String> = emptySet()
    private var loadingChannels: Set<String> = emptySet()
    private var sendingChannels: Set<String> = emptySet()
    private var channelsLoaded = false
    private var session = 0

    private val _state = MutableLiveData(ChatUiState())
    val state: LiveData<ChatUiState> = _state
    private val _events = MutableLiveData<Event<UiEvent>>()
    val events: LiveData<Event<UiEvent>> = _events

    init {
        val name = preferences.getString("name", null)
        val password = preferences.getString("password", null)
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
                preferences.edit().putString("name", name.trim()).putString("password", password).apply()
                update { ChatUiState(authenticated = true, username = name.trim()) }
                loadChannels()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation != session) return@launch
                update { it.copy(loginLoading = false) }
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
                update { it.copy(channels = channels, channelsLoading = false) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation != session) return@launch
                update { it.copy(channelsLoading = false, channelsError = true) }
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
                if (_state.value?.selectedChannel == channel) update { it.copy(messagesError = true) }
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
        sendingChannels = sendingChannels + channel
        publishMessages(channel)
        viewModelScope.launch {
            try {
                val outgoing = ChatMessage(
                    from = current.username, to = channel,
                    data = MessageData(text = TextPayload(text.trim()))
                )
                val postedId = repository.send(outgoing)
                if (generation != session) return@launch
                if (postedId.isBlank()) throw IOException()
                val confirmed = outgoing.copy(
                    id = postedId, time = (System.currentTimeMillis() / 1000L).toString()
                )
                messagesByChannel = messagesByChannel + (
                    channel to mergeMessages(messagesByChannel[channel].orEmpty(), listOf(confirmed))
                )
                // Do not clear a newer draft typed while the request was in flight.
                if (drafts[channel] == text) drafts = drafts - channel
                if (_state.value?.selectedChannel == channel) update { it.copy(sentMessageId = postedId) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == session) handleRequestError(error)
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
            error is IOException -> emit(UiEvent.NetworkError)
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
        preferences.edit().clear().apply()
        update { ChatUiState() }
    }

    private fun update(transform: (ChatUiState) -> ChatUiState) {
        _state.value = transform(_state.value ?: ChatUiState())
    }

    private fun emit(event: UiEvent) {
        _events.value = Event(event)
    }
}

internal fun mergeMessages(existing: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> =
    (existing + incoming).associateBy { it.id }.values.sortedWith(
        compareBy<ChatMessage> { it.id?.toLongOrNull() ?: Long.MAX_VALUE }.thenBy { it.id.orEmpty() }
    )
