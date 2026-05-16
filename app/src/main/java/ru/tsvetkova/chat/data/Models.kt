package ru.tsvetkova.chat.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val name: String,
    val pwd: String,
)

@Serializable
data class TextPayload(val text: String)

@Serializable
data class ImagePayload(val link: String? = null)

@Serializable
data class MessageData(
    @SerialName("Text") val text: TextPayload? = null,
    @SerialName("Image") val image: ImagePayload? = null,
)

@Serializable
data class Message(
    val id: String? = null,
    val from: String,
    val to: String? = null,
    val data: MessageData,
    val time: String? = null,
)
