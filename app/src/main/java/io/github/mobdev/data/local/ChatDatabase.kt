package io.github.mobdev.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChannelEntity::class,
        MessageEntity::class,
        PendingMessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        fun create(context: Context): ChatDatabase =
            Room.databaseBuilder(context, ChatDatabase::class.java, "chat_cache.db")
                .build()
    }
}
