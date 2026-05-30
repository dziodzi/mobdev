package io.github.mobdev.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LocalCache(context: Context) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val dir = File(context.filesDir, "cache").also { it.mkdirs() }

    private fun safeKey(name: String) = name.replace(Regex("[^a-zA-Z0-9]"), "_")

    fun saveChannels(channels: List<String>) {
        File(dir, "channels.json").writeText(json.encodeToString(channels))
    }

    fun loadChannels(): List<String>? {
        val f = File(dir, "channels.json")
        if (!f.exists()) return null
        return try {
            json.decodeFromString<List<String>>(f.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun saveMessages(channel: String, messages: List<Message>) {
        File(dir, "msg_${safeKey(channel)}.json").writeText(json.encodeToString(messages))
    }

    fun loadMessages(channel: String): List<Message> {
        val f = File(dir, "msg_${safeKey(channel)}.json")
        if (!f.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Message>>(f.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePending(list: List<PendingMessage>) {
        File(dir, "pending.json").writeText(json.encodeToString(list))
    }

    fun loadPending(): List<PendingMessage> {
        val f = File(dir, "pending.json")
        if (!f.exists()) return emptyList()
        return try {
            json.decodeFromString<List<PendingMessage>>(f.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }
}
