package ru.tsvetkova.chat.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.tsvetkova.chat.ChatApplication
import ru.tsvetkova.chat.data.ChatRepository
import ru.tsvetkova.chat.data.ConnectivityObserver
import ru.tsvetkova.chat.data.Credentials
import ru.tsvetkova.chat.data.CredentialsStore
import ru.tsvetkova.chat.data.Message
import ru.tsvetkova.chat.data.UnauthorizedException
import retrofit2.HttpException

enum class AuthStage { Booting, NeedsLogin, Authorized }

data class PendingText(
    val localId: Long,
    val from: String,
    val text: String,
)

data class AppState(
    val stage: AuthStage = AuthStage.Booting,
    val user: String? = null,
    val loginError: String? = null,
    val loginInProgress: Boolean = false,
    val channels: List<String> = emptyList(),
    val channelsLoading: Boolean = false,
    val openChannel: String? = null,
    val messages: List<Message> = emptyList(),
    val pending: List<PendingText> = emptyList(),
    val messagesLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val sending: Boolean = false,
    val openImage: String? = null,
    val online: Boolean = true,
)

class AppViewModel(
    private val repository: ChatRepository,
    private val credentials: CredentialsStore,
    private val connectivity: ConnectivityObserver,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeConnectivity()
        autoFlush()
        bootstrap()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivity.isOnline.distinctUntilChanged().collect { online ->
                val wasOffline = !_state.value.online
                _state.update { it.copy(online = online) }
                if (online && wasOffline) {
                    val s = _state.value
                    if (s.stage == AuthStage.Authorized && s.channels.isEmpty()) {
                        loadChannels()
                    }
                }
            }
        }
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
        } catch (e: UnauthorizedException) {
            setLoginError("bad_credentials")
        } catch (e: HttpException) {
            setLoginError("bad_credentials")
        } catch (e: Exception) {
            setLoginError(if (_state.value.online) "bad_credentials" else "network")
        }
    }

    private fun setLoginError(code: String) {
        _state.update {
            it.copy(
                stage = AuthStage.NeedsLogin,
                loginInProgress = false,
                loginError = code,
            )
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
                pending = emptyList(),
                canLoadMore = true,
                openImage = null,
            )
        }
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                repository.observeChannel(channel),
                repository.observePending(channel),
            ) { cached, queued -> cached to queued }.collect { (cached, queued) ->
                if (_state.value.openChannel == channel) {
                    _state.update {
                        it.copy(
                            messages = cached,
                            pending = queued.map { p -> PendingText(p.localId, p.from, p.text) },
                        )
                    }
                }
            }
        }
        syncLatest(channel)
    }

    fun closeChat() {
        observeJob?.cancel()
        observeJob = null
        _state.update {
            it.copy(
                openChannel = null,
                messages = emptyList(),
                pending = emptyList(),
                openImage = null,
            )
        }
    }

    private fun syncLatest(channel: String) {
        if (!_state.value.online) return
        viewModelScope.launch {
            _state.update { it.copy(messagesLoading = true) }
            try {
                val count = repository.refreshLatest(channel)
                _state.update {
                    if (it.openChannel != channel) it
                    else it.copy(messagesLoading = false, canLoadMore = count >= 20)
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
        if (s.messagesLoading || !s.canLoadMore || !s.online) return
        viewModelScope.launch {
            _state.update { it.copy(messagesLoading = true) }
            try {
                val count = repository.loadOlder(channel)
                _state.update {
                    if (it.openChannel != channel) it
                    else it.copy(messagesLoading = false, canLoadMore = count >= 20)
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
            if (!s.online) {
                repository.queueText(channel, user, trimmed)
                return@launch
            }
            _state.update { it.copy(sending = true) }
            try {
                repository.send(from = user, to = channel, text = trimmed)
                _state.update { it.copy(sending = false) }
                syncLatest(channel)
            } catch (e: UnauthorizedException) {
                forceRelogin()
            } catch (e: Exception) {
                repository.queueText(channel, user, trimmed)
                _state.update { it.copy(sending = false) }
            }
        }
    }

    private fun autoFlush() {
        viewModelScope.launch {
            combine(
                connectivity.isOnline,
                repository.observeAllPending(),
                _state.map { it.stage == AuthStage.Authorized }.distinctUntilChanged(),
            ) { online, pending, authorized ->
                online && authorized && pending.isNotEmpty()
            }.distinctUntilChanged().collectLatest { ready ->
                if (!ready) return@collectLatest
                while (true) {
                    if (_state.value.stage != AuthStage.Authorized) break
                    val result = runCatching { repository.flushPending() }
                    if (result.isSuccess) break
                    if (result.exceptionOrNull() is UnauthorizedException) {
                        forceRelogin()
                        break
                    }
                    delay(3000)
                }
                _state.value.openChannel?.let { syncLatest(it) }
            }
        }
    }

    fun sendPicture(uri: Uri, caption: String) {
        val s = _state.value
        if (!s.online) return
        val channel = s.openChannel ?: return
        val user = s.user ?: return
        viewModelScope.launch {
            _state.update { it.copy(sending = true) }
            try {
                val resolver = appContext.contentResolver
                val payload = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: run {
                    _state.update { it.copy(sending = false) }
                    return@launch
                }
                val mime = resolver.getType(uri) ?: "image/jpeg"
                val extension = when {
                    mime.endsWith("png") -> "png"
                    mime.endsWith("webp") -> "webp"
                    mime.endsWith("gif") -> "gif"
                    else -> "jpg"
                }
                val fileName = "upload_${System.currentTimeMillis()}.$extension"
                repository.sendImage(
                    from = user,
                    to = channel,
                    caption = caption.trim(),
                    bytes = payload,
                    mime = mime,
                    fileName = fileName,
                )
                _state.update { it.copy(sending = false) }
                syncLatest(channel)
            } catch (e: UnauthorizedException) {
                forceRelogin()
            } catch (e: Exception) {
                _state.update { it.copy(sending = false) }
            }
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
            observeJob?.cancel()
            observeJob = null
            _state.value = AppState(stage = AuthStage.NeedsLogin, online = _state.value.online)
        }
    }

    private fun forceRelogin() {
        viewModelScope.launch {
            credentials.clear()
            observeJob?.cancel()
            observeJob = null
            _state.value = AppState(
                stage = AuthStage.NeedsLogin,
                loginError = "session_expired",
                online = _state.value.online,
            )
        }
    }

    companion object {
        fun factory(app: ChatApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel(
                    app.repository,
                    app.credentials,
                    app.connectivity,
                    app.applicationContext,
                )
            }
        }
    }
}
