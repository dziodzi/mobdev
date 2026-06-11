package io.github.mobdev.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A text message the user tried to send while offline (or while the
 * request failed due to a network error). Stored on disk so it survives
 * process death and gets sent automatically once the network is back.
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val channel: String,
    val from: String,
    val text: String,
    val createdAt: Long,
)
