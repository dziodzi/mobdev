package io.github.mobdev.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putString(KEY_TOKEN, value) }

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit { putString(KEY_PASSWORD, value) }

    val hasSavedCredentials: Boolean
        get() = !username.isNullOrBlank() && !password.isNullOrBlank()

    fun saveCredentials(name: String, pwd: String) {
        prefs.edit {
            putString(KEY_USERNAME, name)
            putString(KEY_PASSWORD, pwd)
        }
    }

    fun clearToken() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    fun clearAll() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "chat_session"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}
