package io.github.mobdev.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached copy of the channel list, so it can be shown while offline.
 * `position` preserves the order returned by the server.
 */
@Entity(tableName = "cached_channels")
data class ChannelEntity(
    @PrimaryKey val name: String,
    val position: Int,
)
