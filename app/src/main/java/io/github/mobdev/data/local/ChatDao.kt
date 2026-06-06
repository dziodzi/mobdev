package io.github.mobdev.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ChatDao {

    @Query("SELECT name FROM channels ORDER BY name")
    suspend fun getChannels(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query(
        """
        SELECT * FROM messages
        WHERE channel = :channel
        ORDER BY messageId ASC
        """,
    )
    suspend fun getMessages(channel: String): List<MessageEntity>

    @Query(
        """
        SELECT * FROM messages
        WHERE channel = :channel AND messageId < :beforeId
        ORDER BY messageId DESC
        LIMIT :limit
        """,
    )
    suspend fun getMessagesBefore(
        channel: String,
        beforeId: Long,
        limit: Int,
    ): List<MessageEntity>

    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE channel = :channel AND messageId < :beforeId
        """,
    )
    suspend fun countMessagesBefore(channel: String, beforeId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM pending_messages ORDER BY id ASC")
    suspend fun getPendingMessages(): List<PendingMessageEntity>

    @Query("SELECT * FROM pending_messages WHERE channel = :channel ORDER BY id ASC")
    suspend fun getPendingMessages(channel: String): List<PendingMessageEntity>

    @Insert
    suspend fun insertPending(message: PendingMessageEntity): Long

    @Query("DELETE FROM pending_messages WHERE id = :id")
    suspend fun deletePending(id: Long)

    @Transaction
    suspend fun clearCache() {
        clearChannelsInternal()
        clearAllMessagesInternal()
        clearPendingInternal()
    }

    @Query("DELETE FROM channels")
    suspend fun clearChannelsInternal()

    @Query("DELETE FROM messages")
    suspend fun clearAllMessagesInternal()

    @Query("DELETE FROM pending_messages")
    suspend fun clearPendingInternal()
}
