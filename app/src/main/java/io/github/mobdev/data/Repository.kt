package io.github.mobdev.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object Repository {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: Api = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(Api::class.java)

    suspend fun login(name: String, pwd: String): String {
        val body = api.login(LoginRequest(name, pwd)).string().trim()
        // Сервер может вернуть либо просто токен строкой, либо JSON {"token":"..."}
        return when {
            body.startsWith("{") -> {
                val parsed = json.decodeFromString<LoginResponse>(body)
                parsed.token
            }
            body.startsWith("\"") -> body.trim('"')
            else -> body
        }
    }
    private var cachedChannels: List<String>? = null

    suspend fun channels(forceRefresh: Boolean = false): List<String> {
        if (!forceRefresh && cachedChannels != null) return cachedChannels!!
        val result = api.channels()
        cachedChannels = result
        return result
    }

    suspend fun messages(channel: String, lastKnownId: Long = 0, reverse: Boolean = false): List<Message> {
        return api.channelMessages(channel, limit = 20, lastKnownId = lastKnownId, reverse = reverse)
    }

    suspend fun sendText(token: String, from: String, to: String, text: String): String {
        val body = api.sendMessage(
            token,
            Message(from = from, to = to, data = MessageData(text = TextData(text)))
        )
        return body.string().trim().trim('"')
    }

    suspend fun logout(token: String) {
        try {
            api.logout(token)
        } catch (_: Exception) {
        }
    }

    fun isUnauthorized(e: Throwable): Boolean {
        return e is HttpException && e.code() == 401
    }
}