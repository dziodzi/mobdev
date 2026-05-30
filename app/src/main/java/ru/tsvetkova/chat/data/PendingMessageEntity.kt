package ru.tsvetkova.chat.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val channel: String,
    @ColumnInfo(name = "from_user") val from: String,
    val text: String,
    val createdAt: Long,
)

@Dao
interface PendingDao {

    @Query("SELECT * FROM pending WHERE channel = :channel ORDER BY localId ASC")
    fun observeChannel(channel: String): Flow<List<PendingMessageEntity>>

    @Query("SELECT * FROM pending ORDER BY localId ASC")
    fun observeAll(): Flow<List<PendingMessageEntity>>

    @Query("SELECT * FROM pending ORDER BY localId ASC")
    suspend fun all(): List<PendingMessageEntity>

    @Insert
    suspend fun insert(item: PendingMessageEntity): Long

    @Query("DELETE FROM pending WHERE localId = :localId")
    suspend fun delete(localId: Long)
}
