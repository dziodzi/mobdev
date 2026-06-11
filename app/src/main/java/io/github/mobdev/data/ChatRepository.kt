package io.github.mobdev.data

import retrofit2.HttpException

class Unauthorized401Exception : Exception("Unauthorized")

class ChatRepository {

    private val api = RetrofitClient.api

    suspend fun login(username: String, password: String): String {
        val body = api.login(LoginRequest(username, password))
        return body.string().trim()
    }

    suspend fun getChannels(token: String): List<String> = wrapAuth {
        api.getChannels(token)
    }

    suspend fun getMessages(
        token: String,
        channel: String,
        lastKnownId: Long = Long.MAX_VALUE,
        limit: Int = 20,
    ): List<Message> = wrapAuth {
        api.getMessages(token, channel, limit = limit, lastKnownId = lastKnownId, reverse = true)
    }

    suspend fun sendMessage(token: String, username: String, channel: String, text: String) = wrapAuth {
        val message = SendMessageRequest(
            from = username,
            to = channel,
            data = MessageData(text = TextContent(text)),
        )
        api.sendMessage(token, message).string()
    }

    suspend fun logout(token: String) {
        runCatching { api.logout(token) }
    }

    private suspend fun <T> wrapAuth(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 401) throw Unauthorized401Exception()
            throw e
        }
    }
}
