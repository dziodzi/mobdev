package io.github.mobdev.data.mapper

import io.github.mobdev.data.domain.ChatMessage
import io.github.mobdev.data.domain.MessageContent
import io.github.mobdev.data.dto.MessageDto

fun MessageDto.toDomain(): ChatMessage? {
    val messageId = id?.toString() ?: return null
    val channel = to ?: return null
    val content = when {
        data.Text != null -> MessageContent.Text(data.Text.text)
        data.Image?.link != null -> MessageContent.Image(data.Image.link)
        else -> return null
    }
    return ChatMessage(
        id = messageId,
        from = from,
        to = channel,
        content = content,
        time = time,
    )
}

fun messageIdAsLong(id: String): Long = id.toLongOrNull() ?: 0L
