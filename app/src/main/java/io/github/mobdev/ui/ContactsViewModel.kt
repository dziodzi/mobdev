package io.github.mobdev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.Contact
import io.github.mobdev.data.fetchAllContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ContactsUiState {
    data object Idle : ContactsUiState
    data object Loading : ContactsUiState
    data class Loaded(val contacts: List<Contact>) : ContactsUiState
}

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Idle)
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    fun loadContacts() {
        if (_uiState.value !is ContactsUiState.Idle) return
        _uiState.value = ContactsUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ContactsUiState.Loaded(
                getApplication<Application>().fetchAllContacts()
            )
        }
    }
}
