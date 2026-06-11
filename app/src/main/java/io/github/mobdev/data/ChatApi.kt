package io.github.mobdev.data

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): ResponseBody

    @GET("channels")
    suspend fun getChannels(
        @Header("X-Auth-Token") token: String,
    ): List<String>

    @GET("channel/{channel}")
    suspend fun getMessages(
        @Header("X-Auth-Token") token: String,
        @Path("channel") channel: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: Long = Long.MAX_VALUE,
        @Query("reverse") reverse: Boolean = true,
    ): List<Message>

    @POST("messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: SendMessageRequest,
    ): ResponseBody

    @POST("logout")
    suspend fun logout(@Header("X-Auth-Token") token: String): ResponseBody
}
