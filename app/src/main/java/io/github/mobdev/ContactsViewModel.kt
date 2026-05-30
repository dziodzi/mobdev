package io.github.mobdev

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>?>(null)
    val contacts: StateFlow<List<Contact>?> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPermissionDenied = MutableStateFlow(false)
    val isPermissionDenied: StateFlow<Boolean> = _isPermissionDenied.asStateFlow()

    private var loaded = false

    fun loadContacts(contentResolver: ContentResolver) {
        if (loaded) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = ContactsRepository.loadContacts(contentResolver)
                _contacts.value = list
                loaded = true
            } catch (e: Exception) {
                _contacts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setPermissionDenied() {
        _isPermissionDenied.value = true
        _contacts.value = emptyList()
    }

    fun resetPermissionDenied() {
        _isPermissionDenied.value = false
        loaded = false
    }
}