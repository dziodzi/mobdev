package io.github.mobdev.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.mobdev.data.api.AuthInterceptor
import io.github.mobdev.data.api.ChatApiService
import io.github.mobdev.data.dto.LoginRequestDto
import io.github.mobdev.data.dto.MessageDataDto
import io.github.mobdev.data.dto.OutgoingMessageDto
import io.github.mobdev.data.dto.TextPayloadDto
import io.github.mobdev.data.mapper.messageIdAsLong
import io.github.mobdev.data.mapper.toDomain
import io.github.mobdev.data.session.SessionManager
import io.github.mobdev.data.domain.ChatMessage
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class ChatRepository private constructor(
    private val api: ChatApiService,
    private val sessionManager: SessionManager,
) {

    val unauthorized = sessionManager.unauthorized

    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val response = api.login(LoginRequestDto(name = username, pwd = password))
        if (!response.isSuccessful) {
            throw HttpException(response.code())
        }
        val token = response.body()?.string()?.trim().orEmpty()
        if (token.isBlank()) {
            throw IOException("empty token")
        }
        sessionManager.setToken(token)
    }

    suspend fun logout(): Result<Unit> = runCatching {
        api.logout()
        sessionManager.clearToken()
    }

    suspend fun fetchChannels(): Result<List<String>> = runCatching {
        api.getChannels()
    }

    suspend fun fetchMessages(
        channel: String,
        lastKnownId: Long = INITIAL_LAST_KNOWN_ID,
        reverse: Boolean = true,
        limit: Int = PAGE_SIZE,
    ): Result<List<ChatMessage>> = runCatching {
        api.getChannelMessages(
            channelName = channel,
            limit = limit,
            lastKnownId = lastKnownId,
            reverse = reverse,
        )
            .mapNotNull { it.toDomain() }
            .sortedBy { messageIdAsLong(it.id) }
    }

    suspend fun sendTextMessage(
        username: String,
        channel: String,
        text: String,
    ): Result<Unit> = runCatching {
        val body = OutgoingMessageDto(
            from = username,
            to = channel,
            data = MessageDataDto(
                Text = TextPayloadDto(text = text),
                Image = null,
            ),
        )
        val response = api.sendMessage(body)
        if (!response.isSuccessful) {
            throw HttpException(response.code())
        }
    }

    fun imageUrl(path: String, fullResolution: Boolean): String {
        val segment = if (fullResolution) "img" else "thumb"
        val parts = path.trim().trimStart('/').split('/').filter { it.isNotEmpty() }
        val builder = BASE_URL.toHttpUrl().newBuilder().addPathSegment(segment)
        parts.forEach { builder.addPathSegment(it) }
        return builder.build().toString()
    }

    class HttpException(val code: Int) : IOException("HTTP $code")

    companion object {
        const val BASE_URL = "https://faerytea.name/"
        const val PAGE_SIZE = 20
        const val INITIAL_LAST_KNOWN_ID = 9_999_999_999_999L

        fun create(sessionManager: SessionManager): ChatRepository {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(
                    AuthInterceptor(
                        tokenProvider = { sessionManager.authToken },
                        onUnauthorized = sessionManager::notifyUnauthorized,
                    ),
                )
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return ChatRepository(
                api = retrofit.create(ChatApiService::class.java),
                sessionManager = sessionManager,
            )
        }
    }
}