package ru.tsvetkova.chat.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("login")
    suspend fun login(@Body body: LoginRequest): ResponseBody

    @POST("logout")
    suspend fun logout()

    @GET("channels")
    suspend fun channels(): List<String>

    @GET("channel/{name}")
    suspend fun channelMessages(
        @Path("name") name: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: Long = 0,
        @Query("reverse") reverse: Boolean = false,
    ): List<Message>

    @POST("messages")
    suspend fun sendMessage(@Body message: Message): ResponseBody

    @Multipart
    @POST("messages")
    suspend fun sendImage(
        @Part("msg") msg: RequestBody,
        @Part picture: MultipartBody.Part,
    ): ResponseBody
}
