package io.github.mobdev.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val name: String,
    val pwd: String,
)

data class SendMessageRequest(
    val from: String,
    val to: String,
    val data: MessageData,
)

data class Message(
    val id: String = "",
    val from: String,
    val to: String? = "1@channel",
    val data: MessageData,
    val time: String? = null,
)

data class MessageData(
    @SerializedName("Text") val text: TextContent? = null,
    @SerializedName("Image") val image: ImageContent? = null,
)

data class TextContent(val text: String)

data class ImageContent(val link: String? = null)
