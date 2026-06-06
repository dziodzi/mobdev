package io.github.mobdev.api

import io.github.mobdev.data.ChatMessage
import io.github.mobdev.data.LoginRequest
import io.github.mobdev.data.SendTextMessageRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): String

    @POST("logout")
    suspend fun logout()

    @GET("channels")
    suspend fun getChannels(): List<String>

    @GET("channel/{channelName}")
    suspend fun getChannelMessages(
        @Path("channelName") channelName: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String? = null,
        @Query("reverse") reverse: Boolean? = null,
    ): List<ChatMessage>

    @POST("messages")
    suspend fun sendMessage(@Body request: SendTextMessageRequest): String
}
