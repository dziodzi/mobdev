package io.github.mobdev.data

import io.github.mobdev.api.ChatApi
import io.github.mobdev.data.local.ChannelEntity
import io.github.mobdev.data.local.ChatDao
import io.github.mobdev.data.local.PendingMessageEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

class ChatRepository(
    private val api: ChatApi,
    private val sessionManager: SessionManager,
    private val dao: ChatDao,
    private val networkMonitor: NetworkMonitor,
) {

    val networkStatus: Flow<Boolean> = networkMonitor.isOnline

    fun isOnline(): Boolean = networkMonitor.hasInternetConnection()

    suspend fun login(name: String, pwd: String): Result<Unit> = runCatching {
        val token = api.login(LoginRequest(name, pwd)).trim()
        sessionManager.token = token
        sessionManager.saveCredentials(name, pwd)
    }.mapHttpError()

    suspend fun logout() {
        runCatching { api.logout() }
        sessionManager.clearToken()
        dao.clearCache()
    }

    suspend fun getCachedChannels(): List<String> = dao.getChannels()

    suspend fun getChannels(): Result<List<String>> {
        if (!isOnline()) {
            val cached = dao.getChannels()
            return if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(IOException("Network unavailable"))
            }
        }

        return runCatching {
            val channels = api.getChannels()
            dao.insertChannels(channels.map { ChannelEntity(it) })
            channels
        }.recoverCatching { error ->
            if (error.isNetworkIssue()) {
                val cached = dao.getChannels()
                if (cached.isNotEmpty()) cached else throw error
            } else {
                throw error
            }
        }.mapHttpError()
    }

    suspend fun getChannelMessages(
        channel: String,
        includePending: Boolean = true,
    ): List<ChatMessage> {
        val cached = dao.getMessages(channel).map(MessageMapper::fromEntity)
        if (!includePending) return cached
        val pending = getPendingAsMessages(channel)
        return MessageMapper.mergeMessages(cached, pending)
    }

    suspend fun getLatestMessages(channel: String): Result<List<ChatMessage>> {
        val cached = getChannelMessages(channel)

        if (!isOnline()) {
            return if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(IOException("Network unavailable"))
            }
        }

        return runCatching {
            val remote = api.getChannelMessages(
                channelName = channel,
                limit = PAGE_SIZE,
                lastKnownId = LATEST_MARKER,
                reverse = true,
            )
            cacheMessages(channel, remote)
            getChannelMessages(channel)
        }.recoverCatching { error ->
            if (error.isNetworkIssue() && cached.isNotEmpty()) {
                cached
            } else {
                throw error
            }
        }.mapHttpError()
    }

    suspend fun getOlderMessages(channel: String, beforeId: Long): Result<List<ChatMessage>> {
        if (!isOnline()) {
            return loadOlderFromCache(channel, beforeId)
        }

        return runCatching {
            val remote = api.getChannelMessages(
                channelName = channel,
                limit = PAGE_SIZE,
                lastKnownId = beforeId.toString(),
                reverse = true,
            )
            cacheMessages(channel, remote)
            remote
        }.recoverCatching { error ->
            if (error.isNetworkIssue()) {
                loadOlderFromCache(channel, beforeId).getOrThrow()
            } else {
                throw error
            }
        }.mapHttpError()
    }

    suspend fun hasMoreCachedMessages(channel: String, oldestId: Long?): Boolean {
        if (oldestId == null) return false
        return dao.countMessagesBefore(channel, oldestId) > 0
    }

    suspend fun sendTextMessage(channel: String, text: String): Result<Unit> {
        if (!isOnline()) {
            dao.insertPending(PendingMessageEntity(channel = channel, text = text))
            return Result.success(Unit)
        }
        return sendTextMessageRemote(channel, text)
    }

    suspend fun flushPendingMessages(): Result<Unit> = runCatching {
        if (!isOnline()) return@runCatching
        for (pending in dao.getPendingMessages()) {
            sendTextMessageRemote(pending.channel, pending.text)
                .onSuccess { dao.deletePending(pending.id) }
                .onFailure { error ->
                    if (error.isNetworkIssue()) return@runCatching
                    throw error
                }
        }
        Unit
    }.mapHttpError()

    private suspend fun sendTextMessageRemote(channel: String, text: String): Result<Unit> =
        runCatching {
            val username = sessionManager.username
                ?: throw IllegalStateException("Not logged in")
            api.sendMessage(SendTextMessageRequest.create(username, channel, text))
            Unit
        }.mapHttpError()

    private suspend fun loadOlderFromCache(
        channel: String,
        beforeId: Long,
    ): Result<List<ChatMessage>> = runCatching {
        dao.getMessagesBefore(channel, beforeId, PAGE_SIZE)
            .map(MessageMapper::fromEntity)
            .sortedBy { it.id }
    }

    private suspend fun cacheMessages(channel: String, messages: List<ChatMessage>) {
        val entities = messages.mapNotNull { MessageMapper.toEntity(it, channel) }
        if (entities.isNotEmpty()) {
            dao.insertMessages(entities)
        }
    }

    private suspend fun getPendingAsMessages(channel: String): List<ChatMessage> {
        val username = sessionManager.username ?: return emptyList()
        return dao.getPendingMessages(channel).map { pending ->
            MessageMapper.fromPending(pending, username)
        }
    }

    private fun <T> Result<T>.mapHttpError(): Result<T> = this.onFailure { error ->
        if (error is HttpException && error.code() == 401) {
            sessionManager.clearToken()
        }
    }

    private fun Throwable.isNetworkIssue(): Boolean =
        this is IOException || toChatError() is ChatError.Network

    companion object {
        const val PAGE_SIZE = 20
        private const val LATEST_MARKER = "999999999"
    }
}

sealed class ChatError {
    data object Unauthorized : ChatError()
    data object InvalidCredentials : ChatError()
    data class Network(val message: String) : ChatError()
    data class Unknown(val message: String) : ChatError()
}

fun Throwable.toChatError(): ChatError = when (this) {
    is HttpException -> when (code()) {
        401 -> ChatError.Unauthorized
        403 -> ChatError.InvalidCredentials
        else -> ChatError.Unknown(message ?: "HTTP ${code()}")
    }
    is IOException -> ChatError.Network(message ?: "Network error")
    else -> ChatError.Unknown(message ?: "Unknown error")
}
