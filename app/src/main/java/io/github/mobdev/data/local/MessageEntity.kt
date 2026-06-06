package io.github.mobdev.data.local

import androidx.room.Entity

@Entity(
    tableName = "messages",
    primaryKeys = ["channel", "messageId"],
)
data class MessageEntity(
    val channel: String,
    val messageId: Long,
    val fromUser: String,
    val toUser: String?,
    val text: String?,
    val imageLink: String?,
    val time: Long?,
)
