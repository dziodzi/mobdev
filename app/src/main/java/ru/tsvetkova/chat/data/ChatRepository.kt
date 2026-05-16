package ru.tsvetkova.chat.data

import retrofit2.HttpException

class UnauthorizedException : Exception()

class ChatRepository(
    private val api: ChatApi,
    private val auth: AuthHolder,
) {

    suspend fun login(name: String, pwd: String): String = guard {
        val raw = api.login(LoginRequest(name, pwd)).string()
        val token = raw.trim().trim('"')
        auth.token = token
        token
    }

    suspend fun logout() {
        runCatching { api.logout() }
        auth.token = null
    }

    suspend fun channels(): List<String> = guard { api.channels() }

    suspend fun messages(channel: String, lastKnownId: Long): List<Message> = guard {
        api.channelMessages(name = channel, limit = 20, lastKnownId = lastKnownId)
    }

    suspend fun olderMessages(channel: String, beforeId: Long): List<Message> = guard {
        api.channelMessages(name = channel, limit = 20, lastKnownId = beforeId, reverse = true)
    }

    suspend fun send(from: String, to: String, text: String): Unit = guard {
        api.sendMessage(
            Message(
                from = from,
                to = to,
                data = MessageData(text = TextPayload(text)),
            )
        )
    }

    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        if (e.code() == 401) {
            auth.token = null
            throw UnauthorizedException()
        } else {
            throw e
        }
    }
}
