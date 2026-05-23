package io.github.mobdev.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.ResponseBody

const val BASE_URL = "https://faerytea.name/"

interface Api {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): okhttp3.ResponseBody

    @POST("logout")
    suspend fun logout(@Header("X-Auth-Token") token: String)

    @GET("channels")
    suspend fun channels(): List<String>

    @GET("channel/{name}")
    suspend fun channelMessages(
        @Path("name") channel: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: Long = 0,
        @Query("reverse") reverse: Boolean = false
    ): List<Message>

    @POST("messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: Message
    ): ResponseBody
}