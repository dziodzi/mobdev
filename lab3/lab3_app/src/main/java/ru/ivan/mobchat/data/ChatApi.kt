package ru.ivan.mobchat.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>

    @GET("channels")
    suspend fun channels(): Response<List<String>>

    @GET("channel/{channel}")
    suspend fun messages(
        @Path("channel") channel: String,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("lastKnownId") lastKnownId: String? = null,
        @Query("reverse") reverse: Boolean = false
    ): Response<List<ChatMessage>>

    @POST("messages")
    suspend fun sendMessage(@Body message: ChatMessage): Response<ResponseBody>

    @POST("logout")
    suspend fun logout(@retrofit2.http.Header("X-Auth-Token") token: String): Response<ResponseBody>

    companion object {
        const val PAGE_SIZE = 20
    }
}

class TokenInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request().newBuilder().apply {
            if (chain.request().header(AUTH_HEADER) == null) {
                tokenProvider()?.let { header(AUTH_HEADER, it) }
            }
        }.build()
        return chain.proceed(request)
    }

    companion object {
        private const val AUTH_HEADER = "X-Auth-Token"
    }
}

object ApiFactory {
    const val BASE_URL = "https://faerytea.name/"

    fun create(tokenProvider: () -> String?): ChatApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(tokenProvider))
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApi::class.java)
    }
}

class ApiException(val statusCode: Int) : Exception()

class ChatRepository(private val api: ChatApi) {
    suspend fun login(name: String, password: String): String {
        val response = api.login(LoginRequest(name, password))
        return response.bodyOrThrow().string().trim().trim('"')
    }

    suspend fun channels(): List<String> = api.channels().bodyOrThrow()

    suspend fun messages(channel: String): List<ChatMessage> =
        api.messages(channel = channel, lastKnownId = Long.MAX_VALUE.toString(), reverse = true).bodyOrThrow()

    suspend fun newerMessages(channel: String, afterId: String): List<ChatMessage> =
        api.messages(channel = channel, lastKnownId = afterId).bodyOrThrow()

    suspend fun olderMessages(channel: String, beforeId: String): List<ChatMessage> =
        api.messages(channel = channel, lastKnownId = beforeId, reverse = true).bodyOrThrow()

    suspend fun send(message: ChatMessage): String =
        api.sendMessage(message).bodyOrThrow().string().trim().trim('"')

    suspend fun logout(token: String) {
        val response = api.logout(token)
        response.body()?.close()
        if (!response.isSuccessful) throw ApiException(response.code())
    }

    private fun <T> Response<T>.bodyOrThrow(): T {
        if (!isSuccessful) {
            errorBody()?.close()
            throw ApiException(code())
        }
        return body() ?: throw ApiException(code())
    }
}
