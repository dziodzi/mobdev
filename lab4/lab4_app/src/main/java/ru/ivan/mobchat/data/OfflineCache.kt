package ru.ivan.mobchat.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class CachedChatState(
    val username: String = "",
    val channels: List<String> = emptyList(),
    val messages: Map<String, List<ChatMessage>> = emptyMap(),
    val pendingMessages: List<ChatMessage> = emptyList()
)

class OfflineCache(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun read(): CachedChatState {
        val json = preferences.getString(CACHE_KEY, null) ?: return CachedChatState()
        return runCatching { gson.fromJson<CachedChatState>(json, cacheType) }
            .getOrNull()
            ?: CachedChatState()
    }

    fun write(state: CachedChatState) {
        preferences.edit().putString(CACHE_KEY, gson.toJson(state)).apply()
    }

    fun clear() {
        preferences.edit().remove(CACHE_KEY).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "chat_offline_cache"
        private const val CACHE_KEY = "state"
        private val cacheType = object : TypeToken<CachedChatState>() {}.type
    }
}
