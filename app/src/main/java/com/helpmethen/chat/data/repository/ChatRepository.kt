package com.helpmethen.chat.data.repository

import com.helpmethen.chat.data.model.LoginRequest
import com.helpmethen.chat.data.model.MessageDataDto
import com.helpmethen.chat.data.model.MessageDto
import com.helpmethen.chat.data.model.SendMessageDto
import com.helpmethen.chat.data.model.TextDto
import com.helpmethen.chat.data.remote.ChatApi
import retrofit2.HttpException

class ChatRepository(
    private val api: ChatApi
) {

    suspend fun login(
        username: String,
        password: String
    ): Result<String> {
        return try {
            val response = api.login(
                LoginRequest(
                    name = username,
                    pwd = password
                )
            )

            if (response.isSuccessful) {
                val token = response.body()

                if (token.isNullOrBlank()) {
                    Result.failure(Exception("Empty token"))
                } else {
                    Result.success(token)
                }
            } else if (response.code() == 401) {
                Result.failure(UnauthorizedException())
            } else {
                Result.failure(Exception("Invalid username or password"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getChannels(): Result<List<String>> {
        return try {
            val channels = api.getChannels()
            Result.success(channels)
        } catch (exception: HttpException) {
            if (exception.code() == 401) {
                Result.failure(UnauthorizedException())
            } else {
                Result.failure(exception)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getMessages(channelName: String): Result<List<MessageDto>> {
        return try {
            val messages = api.getChannelMessages(
                channelName = channelName,
                limit = 20,
                lastKnownId = Long.MAX_VALUE.toString(),
                reverse = true
            )
            Result.success(messages.reversed().distinctBy { message -> message.id })
        } catch (exception: HttpException) {
            if (exception.code() == 401) {
                Result.failure(UnauthorizedException())
            } else {
                Result.failure(exception)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getLastMessage(channelName: String): Result<MessageDto?> {
        return try {
            val messages = api.getChannelMessages(
                channelName = channelName,
                limit = 1,
                lastKnownId = Long.MAX_VALUE.toString(),
                reverse = true
            )
            Result.success(messages.firstOrNull())
        } catch (exception: HttpException) {
            if (exception.code() == 401) {
                Result.failure(UnauthorizedException())
            } else {
                Result.failure(exception)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun sendMessage(
        token: String,
        from: String,
        to: String,
        text: String
    ): Result<Unit> {
        return try {
            val response = api.sendMessage(
                token = token,
                message = SendMessageDto(
                    from = from,
                    to = to,
                    data = MessageDataDto(Text = TextDto(text = text))
                )
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else if (response.code() == 401) {
                Result.failure(UnauthorizedException())
            } else {
                Result.failure(Exception("Message was not sent"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun logout(token: String): Result<Unit> {
        return try {
            val response = api.logout(token)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else if (response.code() == 401) {
                Result.failure(UnauthorizedException())
            } else {
                Result.failure(Exception("Logout failed"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
