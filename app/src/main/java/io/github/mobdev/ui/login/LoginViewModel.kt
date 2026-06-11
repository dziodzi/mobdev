package io.github.mobdev.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    val tokenStorage = TokenStorage(app)
    private val repo = ChatRepository()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun hasSavedCredentials(): Boolean =
        tokenStorage.savedUsername != null && tokenStorage.savedPassword != null

    fun autoLogin(onSuccess: () -> Unit) {
        val username = tokenStorage.savedUsername ?: return
        val password = tokenStorage.savedPassword ?: return
        login(username, password, onSuccess)
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("field_required")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val token = repo.login(username, password)
                tokenStorage.token = token
                tokenStorage.savedUsername = username
                tokenStorage.savedPassword = password
                _uiState.value = LoginUiState.Success
                onSuccess()
            } catch (e: HttpException) {
                _uiState.value = if (e.code() == 401) {
                    LoginUiState.Error("invalid_credentials")
                } else {
                    LoginUiState.Error("http_${e.code()}: ${e.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e::class.simpleName + ": " + e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = LoginUiState.Idle
    }
}
