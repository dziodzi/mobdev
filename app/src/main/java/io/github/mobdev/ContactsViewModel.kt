package io.github.mobdev

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private var loaded = false

    fun loadContacts(context: Context) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _contacts.value = withContext(Dispatchers.IO) {
                context.applicationContext.fetchAllContacts()
            }
        }
    }
}
