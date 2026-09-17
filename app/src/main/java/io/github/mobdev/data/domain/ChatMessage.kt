package io.github.mobdev.data.domain

sealed interface MessageContent {
    data class Text(val text: String) : MessageContent
    data class Image(val link: String) : MessageContent
    data object Unknown : MessageContent
}

data class ChatMessage(
    val id: String,
    val from: String,
    val to: String?,
    val content: MessageContent,
    val time: Long?,
)