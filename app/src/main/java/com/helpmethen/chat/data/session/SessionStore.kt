package com.helpmethen.chat.data.session

import android.content.Context

object SessionStore {
    private const val PREFS_NAME = "chat_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USERNAME = "username"

    fun save(context: Context, token: String, username: String) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun restore(context: Context): SavedSession? {
        val token = prefs(context).getString(KEY_TOKEN, null)
        val username = prefs(context).getString(KEY_USERNAME, null)

        if (token.isNullOrBlank() || username.isNullOrBlank()) {
            return null
        }

        return SavedSession(token, username)
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

data class SavedSession(
    val token: String,
    val username: String
)
