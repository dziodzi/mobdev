package io.github.mobdev.data.db

import androidx.room.Entity

/**
 * Cached copy of a server message, scoped to a channel.
 * Used to show previously loaded messages while offline.
 */
@Entity(tableName = "cached_messages", primaryKeys = ["id", "channel"])
data class MessageEntity(
    val id: String,
    val channel: String,
    val from: String,
    val to: String?,
    val text: String?,
    val imageLink: String?,
    val time: String?,
)
