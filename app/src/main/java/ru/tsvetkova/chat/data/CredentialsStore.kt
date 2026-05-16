package ru.tsvetkova.chat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "credentials")

data class Credentials(val name: String, val pwd: String)

class CredentialsStore(private val context: Context) {

    private val nameKey = stringPreferencesKey("name")
    private val pwdKey = stringPreferencesKey("pwd")

    suspend fun load(): Credentials? = context.dataStore.data
        .map { prefs ->
            val n = prefs[nameKey]
            val p = prefs[pwdKey]
            if (n != null && p != null) Credentials(n, p) else null
        }
        .first()

    suspend fun save(creds: Credentials) {
        context.dataStore.edit { prefs ->
            prefs[nameKey] = creds.name
            prefs[pwdKey] = creds.pwd
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(nameKey)
            prefs.remove(pwdKey)
        }
    }
}
