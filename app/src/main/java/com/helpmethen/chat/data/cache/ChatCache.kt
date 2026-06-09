package com.helpmethen.chat.data.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.helpmethen.chat.data.model.MessageDto

object ChatCache {
    private const val PREFS_NAME = "chat_cache"
    private const val KEY_CHANNELS = "channels"
    private const val KEY_LAST_PREVIEWS = "last_previews"
    private const val KEY_MESSAGES_PREFIX = "messages_"

    private val gson = Gson()

    fun saveChannels(context: Context, channels: List<String>) {
        prefs(context).edit()
            .putString(KEY_CHANNELS, gson.toJson(channels))
            .apply()
    }

    fun loadChannels(context: Context): List<String> {
        val json = prefs(context).getString(KEY_CHANNELS, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return runCatching { gson.fromJson<List<String>>(json, type) }.getOrDefault(emptyList())
    }

    fun saveLastMessagePreviews(context: Context, previews: Map<String, String>) {
        prefs(context).edit()
            .putString(KEY_LAST_PREVIEWS, gson.toJson(previews))
            .apply()
    }

    fun loadLastMessagePreviews(context: Context): Map<String, String> {
        val json = prefs(context).getString(KEY_LAST_PREVIEWS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return runCatching { gson.fromJson<Map<String, String>>(json, type) }.getOrDefault(emptyMap())
    }

    fun saveMessages(context: Context, channel: String, messages: List<MessageDto>) {
        prefs(context).edit()
            .putString(KEY_MESSAGES_PREFIX + channel, gson.toJson(messages.withoutDuplicates()))
            .apply()
    }

    fun loadMessages(context: Context, channel: String): List<MessageDto> {
        val json = prefs(context).getString(KEY_MESSAGES_PREFIX + channel, null) ?: return emptyList()
        val type = object : TypeToken<List<MessageDto>>() {}.type
        return runCatching {
            gson.fromJson<List<MessageDto>>(json, type).withoutDuplicates()
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun List<MessageDto>.withoutDuplicates(): List<MessageDto> {
        return distinctBy { message -> message.id }
    }
}
