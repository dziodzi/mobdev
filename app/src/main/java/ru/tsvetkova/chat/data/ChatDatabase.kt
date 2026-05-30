package ru.tsvetkova.chat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, PendingMessageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun messages(): MessageDao

    abstract fun pending(): PendingDao

    companion object {
        fun build(context: Context): ChatDatabase = Room.databaseBuilder(
            context.applicationContext,
            ChatDatabase::class.java,
            "chat.db",
        ).fallbackToDestructiveMigration().build()
    }
}
