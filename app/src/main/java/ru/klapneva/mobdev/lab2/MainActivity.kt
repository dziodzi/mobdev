package ru.klapneva.mobdev.lab2

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class Contact(val name: String, val phone: String)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ContactsScreen()
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            recreate()
        }
    }

    @Composable
    fun ContactsScreen() {
        val context = LocalContext.current
        var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
        var selectedContact by remember { mutableStateOf<Contact?>(null) }
        val hasPermission = checkPermission()

        LaunchedEffect(hasPermission) {
            if (hasPermission) {
                contacts = getContacts(context)
            }
        }

        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Нет разрешения на чтение контактов")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { requestPermission() }) {
                        Text("Запросить разрешение")
                    }
                }
            }
            selectedContact != null -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(onClick = { selectedContact = null }) {
                        Text("← Назад")
                    }
                    Card(modifier = Modifier.padding(top = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Имя: ${selectedContact?.name ?: "Не указано"}")
                            Text("Телефон: ${selectedContact?.phone ?: "Не указан"}")
                        }
                    }
                }
            }
            contacts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Контакты не найдены")
                }
            }
            else -> {
                LazyColumn {
                    items(contacts) { contact ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { selectedContact = contact }
                        ) {
                            Text(
                                text = contact.name,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getContacts(context: Context): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                if (nameIndex >= 0 && phoneIndex >= 0) {
                    val name = it.getString(nameIndex) ?: "Без имени"
                    val phone = it.getString(phoneIndex) ?: ""
                    contacts.add(Contact(name, phone))
                }
            }
        }

        return contacts
    }
}