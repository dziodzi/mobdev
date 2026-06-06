package io.github.mobdev.data

import io.github.mobdev.data.local.MessageEntity
import io.github.mobdev.data.local.PendingMessageEntity

object MessageMapper {

    fun toEntity(message: ChatMessage, channel: String): MessageEntity? {
        val data = message.data ?: return null
        return when (data) {
            is MessageData.Text -> MessageEntity(
                channel = channel,
                messageId = message.id,
                fromUser = message.from,
                toUser = message.to,
                text = data.text,
                imageLink = null,
                time = message.time,
            )
            is MessageData.Image -> MessageEntity(
                channel = channel,
                messageId = message.id,
                fromUser = message.from,
                toUser = message.to,
                text = null,
                imageLink = data.link,
                time = message.time,
            )
        }
    }

    fun fromEntity(entity: MessageEntity): ChatMessage {
        val data: MessageData? = when {
            entity.text != null -> MessageData.Text(entity.text)
            entity.imageLink != null -> MessageData.Image(entity.imageLink)
            else -> null
        }
        return ChatMessage(
            id = entity.messageId,
            from = entity.fromUser,
            to = entity.toUser,
            data = data,
            time = entity.time,
        )
    }

    fun fromPending(
        pending: PendingMessageEntity,
        username: String,
    ): ChatMessage = ChatMessage(
        id = -pending.id,
        from = username,
        to = pending.channel,
        data = MessageData.Text(pending.text),
        time = pending.createdAt,
    )

    fun mergeMessages(
        cached: List<ChatMessage>,
        pending: List<ChatMessage>,
    ): List<ChatMessage> = (cached + pending)
        .distinctBy { it.id }
        .sortedBy { it.id }
}
