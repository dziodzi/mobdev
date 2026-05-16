package ru.tsvetkova.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.tsvetkova.chat.ChatApplication
import ru.tsvetkova.chat.data.ChatRepository
import ru.tsvetkova.chat.data.Credentials
import ru.tsvetkova.chat.data.CredentialsStore
import ru.tsvetkova.chat.data.Message
import ru.tsvetkova.chat.data.UnauthorizedException

enum class AuthStage { Booting, NeedsLogin, Authorized }

data class AppState(
    val stage: AuthStage = AuthStage.Booting,
    val user: String? = null,
    val loginError: String? = null,
    val loginInProgress: Boolean = false,
    val channels: List<String> = emptyList(),
    val channelsLoading: Boolean = false,
    val openChannel: String? = null,
    val messages: List<Message> = emptyList(),
    val messagesLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val sending: Boolean = false,
    val openImage: String? = null,
)

class AppViewModel(
    private val repository: ChatRepository,
    private val credentials: CredentialsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val saved = credentials.load()
            if (saved == null) {
                _state.update { it.copy(stage = AuthStage.NeedsLogin) }
                return@launch
            }
            tryLogin(saved.name, saved.pwd, persist = false)
        }
    }

    fun submitLogin(name: String, pwd: String) {
        if (name.isBlank() || pwd.isBlank()) return
        viewModelScope.launch {
            tryLogin(name.trim(), pwd, persist = true)
        }
    }

    private suspend fun tryLogin(name: String, pwd: String, persist: Boolean) {
        _state.update { it.copy(loginInProgress = true, loginError = null) }
        try {
            repository.login(name, pwd)
            if (persist) credentials.save(Credentials(name, pwd))
            _state.update {
                it.copy(
                    stage = AuthStage.Authorized,
                    user = name,
                    loginInProgress = false,
                    loginError = null,
                )
            }
            loadChannels()
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    stage = AuthStage.NeedsLogin,
                    loginInProgress = false,
                    loginError = "bad_credentials",
                )
            }
        }
    }

    fun dismissLoginError() {
        _state.update { it.copy(loginError = null) }
    }

    fun loadChannels() {
        viewModelScope.launch {
            _state.update { it.copy(channelsLoading = true) }
            try {
                val list = repository.channels()
                _state.update { it.copy(channels = list, channelsLoading = false) }
            } catch (e: UnauthorizedException) {
                forceRelogin()
            } catch (e: Exception) {
                _state.update { it.copy(channelsLoading = false) }
            }
        }
    }

    fun openChat(channel: String) {
        val current = _state.value
        if (current.openChannel == channel) return
        _state.update {
            it.copy(
                openChannel = channel,
                messages = emptyList(),
                canLoadMore = true,
                openImage = null,
            )
        }
        refreshMessages(channel)
    }

    fun closeChat() {
        _state.update { it.copy(openChannel = null, messages = emptyList(), openImage = null) }
    }

    private fun refreshMessages(channel: String) {
        viewModelScope.launch {
            _state.update { it.copy(messagesLoading = true) }
            try {
                val fresh = repository.messages(channel, lastKnownId = 0L)
                _state.update {
                    if (it.openChannel != channel) it else it.copy(
                        messages = fresh.sortedBy { m -> m.id?.toLongOrNull() ?: 0L },
                        messagesLoading = false,
                        canLoadMore = fresh.size >= 20,
                    )
                }
            } catch (e: UnauthorizedException) {
                forceRelogin()
            } catch (e: Exception) {
                _state.update { it.copy(messagesLoading = false) }
            }
        }
    }

    fun loadMoreOlder() {
        val s = _state.value
        val channel = s.openChannel ?: return
        if (s.messagesLoading || !s.canLoadMore) return
        val oldestId = s.messages.firstOrNull()?.id?.toLongOrNull() ?: return
        viewModelScope.launch {
            _state.update { it.copy(messagesLoading = true) }
            try {
                val older = repository.olderMessages(channel, beforeId = oldestId)
                _state.update {
                    if (it.openChannel != channel) it else it.copy(
                        messages = (older + it.messages).distinctBy { m -> m.id }
                            .sortedBy { m -> m.id?.toLongOrNull() ?: 0L },
                        messagesLoading = false,
                        canLoadMore = older.size >= 20,
                    )
                }
            } catch (e: UnauthorizedException) {
                forceRelogin()
            } catch (e: Exception) {
                _state.update { it.copy(messagesLoading = false) }
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val s = _state.value
        val channel = s.openChannel ?: return
        val user = s.user ?: return
        viewModelScope.launch {
            _state.update { it.copy(sending = true) }
            try {
                repository.send(from = user, to = channel, text = trimmed)
                _state.update { it.copy(sending = false) }
                refreshLatest(channel)
            } catch (e: UnauthorizedException) {
                forceRelogin()
            } catch (e: Exception) {
                _state.update { it.copy(sending = false) }
            }
        }
    }

    private suspend fun refreshLatest(channel: String) {
        try {
            val newest = repository.messages(channel, lastKnownId = 0L)
            _state.update {
                if (it.openChannel != channel) it else it.copy(
                    messages = newest.sortedBy { m -> m.id?.toLongOrNull() ?: 0L },
                    canLoadMore = newest.size >= 20,
                )
            }
        } catch (_: Exception) {
        }
    }

    fun openImage(path: String) {
        _state.update { it.copy(openImage = path) }
    }

    fun closeImage() {
        _state.update { it.copy(openImage = null) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            credentials.clear()
            _state.value = AppState(stage = AuthStage.NeedsLogin)
        }
    }

    private fun forceRelogin() {
        viewModelScope.launch {
            credentials.clear()
            _state.value = AppState(stage = AuthStage.NeedsLogin, loginError = "session_expired")
        }
    }

    companion object {
        fun factory(app: ChatApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(app.repository, app.credentials) }
        }
    }
}
