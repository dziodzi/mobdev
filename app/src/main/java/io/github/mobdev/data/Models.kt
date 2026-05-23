package io.github.mobdev.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val name: String,
    val pwd: String
)

@Serializable
data class LoginResponse(
    val token: String
)

@Serializable
data class Message(
    val id: Long? = null,
    val from: String,
    val to: String? = null,
    val data: MessageData,
    val time: Long? = null
)

@Serializable
data class MessageData(
    @SerialName("Text") val text: TextData? = null,
    @SerialName("Image") val image: ImageData? = null
)

@Serializable
data class TextData(val text: String)

@Serializable
data class ImageData(val link: String? = null)