package ru.tsvetkova.chat.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages", primaryKeys = ["channel", "id"])
data class MessageEntity(
    val channel: String,
    val id: Long,
    @ColumnInfo(name = "from_user") val from: String,
    val time: Long?,
    val text: String?,
    val imageLink: String?,
)

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE channel = :channel ORDER BY id ASC")
    fun observeChannel(channel: String): Flow<List<MessageEntity>>

    @Query("SELECT MIN(id) FROM messages WHERE channel = :channel")
    suspend fun oldestId(channel: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE channel = :channel")
    suspend fun clearChannel(channel: String)
}

fun MessageEntity.toMessage(): Message = Message(
    id = id,
    from = from,
    to = channel,
    time = time,
    data = when {
        imageLink != null -> MessageData(image = ImagePayload(link = imageLink))
        text != null -> MessageData(text = TextPayload(text = text))
        else -> null
    },
)

fun Message.toEntity(channel: String): MessageEntity? {
    val rowId = id ?: return null
    return MessageEntity(
        channel = channel,
        id = rowId,
        from = from,
        time = time,
        text = data?.text?.text,
        imageLink = data?.image?.link,
    )
}
