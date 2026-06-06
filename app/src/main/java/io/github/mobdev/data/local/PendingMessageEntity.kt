package io.github.mobdev.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channel: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)
