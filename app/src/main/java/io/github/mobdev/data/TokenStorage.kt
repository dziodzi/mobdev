package io.github.mobdev.data

import android.content.Context

class TokenStorage(context: Context) {

    private val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var savedUsername: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var savedPassword: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}
