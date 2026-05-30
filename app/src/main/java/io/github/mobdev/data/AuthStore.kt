package io.github.mobdev.data

import android.content.Context
import androidx.core.content.edit

class AuthStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var username: String?
        get() = prefs.getString("username", null)
        set(value) = prefs.edit { putString("username", value) }

    var password: String?
        get() = prefs.getString("password", null)
        set(value) = prefs.edit { putString("password", value) }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit { putString("token", value) }

    fun clear() {
        prefs.edit { clear() }
    }
}