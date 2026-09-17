package io.github.mobdev.data.api

import io.github.mobdev.data.dto.LoginRequestDto
import io.github.mobdev.data.dto.MessageDto
import io.github.mobdev.data.dto.OutgoingMessageDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {

    @POST("login")
    suspend fun login(@Body body: LoginRequestDto): Response<ResponseBody>

    @POST("logout")
    suspend fun logout(): Response<ResponseBody>

    @GET("channels")
    suspend fun getChannels(): List<String>

    @GET("channel/{channelName}")
    suspend fun getChannelMessages(
        @Path("channelName") channelName: String,
        @Query("limit") limit: Int,
        @Query("lastKnownId") lastKnownId: Long,
        @Query("reverse") reverse: Boolean,
    ): List<MessageDto>

    @POST("messages")
    suspend fun sendMessage(@Body message: OutgoingMessageDto): Response<ResponseBody>
}