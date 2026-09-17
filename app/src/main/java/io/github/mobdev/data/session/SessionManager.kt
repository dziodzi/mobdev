package io.github.mobdev.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionManager {
    @Volatile
    var authToken: String? = null
        private set

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized: SharedFlow<Unit> = _unauthorized.asSharedFlow()

    fun setToken(token: String) {
        authToken = token
    }

    fun clearToken() {
        authToken = null
    }

    fun notifyUnauthorized() {
        clearToken()
        _unauthorized.tryEmit(Unit)
    }
}