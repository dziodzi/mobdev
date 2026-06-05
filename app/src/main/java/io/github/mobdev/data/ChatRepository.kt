package io.github.mobdev.data

import io.github.mobdev.api.ChatApi
import retrofit2.HttpException
import java.io.IOException

class ChatRepository(
    private val api: ChatApi,
    private val sessionManager: SessionManager,
) {

    suspend fun login(name: String, pwd: String): Result<Unit> = runCatching {
        val token = api.login(LoginRequest(name, pwd)).trim()
        sessionManager.token = token
        sessionManager.saveCredentials(name, pwd)
    }.mapHttpError()

    suspend fun logout() {
        runCatching { api.logout() }
        sessionManager.clearToken()
    }

    suspend fun getChannels(): Result<List<String>> = runCatching {
        api.getChannels()
    }.mapHttpError()

    suspend fun getLatestMessages(channel: String): Result<List<ChatMessage>> = runCatching {
        api.getChannelMessages(
            channelName = channel,
            limit = PAGE_SIZE,
            lastKnownId = LATEST_MARKER,
            reverse = true,
        )
    }.mapHttpError()

    suspend fun getOlderMessages(channel: String, beforeId: Long): Result<List<ChatMessage>> =
        runCatching {
            api.getChannelMessages(
                channelName = channel,
                limit = PAGE_SIZE,
                lastKnownId = beforeId.toString(),
                reverse = true,
            )
        }.mapHttpError()

    suspend fun sendTextMessage(channel: String, text: String): Result<Unit> = runCatching {
        val username = sessionManager.username
            ?: throw IllegalStateException("Not logged in")
        api.sendMessage(SendTextMessageRequest.create(username, channel, text))
        Unit
    }.mapHttpError()

    private fun <T> Result<T>.mapHttpError(): Result<T> = this.onFailure { error ->
        if (error is HttpException && error.code() == 401) {
            sessionManager.clearToken()
        }
    }

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
