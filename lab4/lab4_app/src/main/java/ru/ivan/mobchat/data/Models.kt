package ru.ivan.mobchat.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val name: String,
    val pwd: String
)

data class ChatMessage(
    val id: String? = null,
    val from: String,
    val to: String = DEFAULT_CHANNEL,
    val data: MessageData,
    val time: String? = null
) {
    companion object {
        const val DEFAULT_CHANNEL = "1@channel"
    }
}

data class MessageData(
    @SerializedName("Text") val text: TextPayload? = null,
    @SerializedName("Image") val image: ImagePayload? = null
)

data class TextPayload(val text: String)

data class ImagePayload(val link: String? = null)
