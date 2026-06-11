package io.github.mobdev.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {

    // Newest-first, matching the shape of the live API's `reverse=true` response
    // so the ViewModel can treat both the same way (it reverses to oldest-first for display).
    @Query("SELECT * FROM cached_messages WHERE channel = :channel ORDER BY CAST(id AS INTEGER) DESC")
    suspend fun getMessagesForChannel(channel: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)
}

@Dao
interface ChannelDao {

    @Query("SELECT name FROM cached_channels ORDER BY position ASC")
    suspend fun getChannels(): List<String>

    @Query("DELETE FROM cached_channels")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)
}

@Dao
interface PendingMessageDao {

    @Insert
    suspend fun insert(message: PendingMessageEntity): Long

    @Query("SELECT * FROM pending_messages WHERE channel = :channel ORDER BY createdAt ASC")
    suspend fun getForChannel(channel: String): List<PendingMessageEntity>

    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingMessageEntity>

    @Delete
    suspend fun delete(message: PendingMessageEntity)
}
