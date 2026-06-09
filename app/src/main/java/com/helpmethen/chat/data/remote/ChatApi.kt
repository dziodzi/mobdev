package com.helpmethen.chat.data.remote

import com.helpmethen.chat.data.model.LoginRequest
import com.helpmethen.chat.data.model.MessageDto
import com.helpmethen.chat.data.model.SendMessageDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<String>

    @GET("channels")
    suspend fun getChannels(): List<String>

    @GET("channel/{channelName}")
    suspend fun getChannelMessages(
        @Path("channelName") channelName: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String? = null,
        @Query("reverse") reverse: Boolean = false
    ) : List<MessageDto>

    @POST("messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: SendMessageDto
    ): Response<String>

    @POST("logout")
    suspend fun logout(
        @Header("X-Auth-Token") token: String,
    ): Response<Unit>
}
