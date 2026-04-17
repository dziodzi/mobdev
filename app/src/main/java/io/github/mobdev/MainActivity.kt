package io.github.mobdev

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.database.getStringOrNull
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import io.github.mobdev.ui.theme.MobdevTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Contact(
    val name: String?,
    val phones: List<String>,
    val emails: List<String>
)

private data class MutableContact(
    var name: String?,
    val phones: MutableList<String> = mutableListOf(),
    val emails: MutableList<String> = mutableListOf()
)
@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    val contactsMap = linkedMapOf<Long, MutableContact>()

    contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        ),
        null, null,
        "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val name = cursor.getStringOrNull(
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            )
            contactsMap[id] = MutableContact(name = name)
        }
    }

    // 2. Телефоны
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null, null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(idIdx)
            val number = cursor.getStringOrNull(numIdx) ?: continue
            contactsMap[contactId]?.phones?.add(number)
        }
    }

    // 3. Email
    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        ),
        null, null, null
    )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val addrIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(idIdx)
            val address = cursor.getStringOrNull(addrIdx) ?: continue
            contactsMap[contactId]?.emails?.add(address)
        }
    }

    return contactsMap.values.map {
        Contact(
            name = it.name,
            phones = it.phones.toList(),
            emails = it.emails.toList()
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobdevTheme {
                ContactsApp()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsApp() {
    val permissionState = rememberPermissionState(Manifest.permission.READ_CONTACTS)
    var alreadyAsked by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_contacts)) })
        }
    ) { pvs ->
        Box(Modifier.padding(pvs).fillMaxSize()) {
            val status = permissionState.status
            when {
                status is PermissionStatus.Granted -> {
                    ContactsList()
                }
                !alreadyAsked -> {
                    NotAskedScreen(
                        onRequest = {
                            alreadyAsked = true
                            permissionState.launchPermissionRequest()
                        }
                    )
                }
                status is PermissionStatus.Denied && status.shouldShowRationale -> {
                    DeniedScreen(
                        onRequest = { permissionState.launchPermissionRequest() }
                    )
                }
                else -> {
                    PermanentlyDeniedScreen()
                }
            }
        }
    }
}

@Composable
fun NotAskedScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.permission_rationale))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}

@Composable
fun DeniedScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.permission_denied))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) {
            Text(stringResource(R.string.permission_request_again))
        }
    }
}

@Composable
fun PermanentlyDeniedScreen() {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.permission_denied_permanently))
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ctx.packageName, null)
            )
            ctx.startActivity(intent)
        }) {
            Text(stringResource(R.string.permission_open_settings))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsList() {
    val ctx = LocalContext.current
    var contacts by rememberSaveable { mutableStateOf<List<Contact>?>(null) }
    var selected by remember { mutableStateOf<Contact?>(null) }

    LaunchedEffect(Unit) {
        if (contacts == null) {
            contacts = withContext(Dispatchers.IO) { ctx.fetchAllContacts() }
        }
    }

    val list = contacts
    when {
        list == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        list.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_contacts))
            }
        }
        else -> {
            LazyColumn(Modifier.fillMaxSize()) {
                items(list) { contact ->
                    ContactRow(contact) { selected = contact }
                    HorizontalDivider()
                }
            }
        }
    }

    selected?.let { contact ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    text = contact.name ?: stringResource(R.string.no_name),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.label_phone))
                if (contact.phones.isEmpty()) {
                    Text(stringResource(R.string.not_specified))
                } else {
                    contact.phones.forEach { phone ->
                        Text(phone)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(stringResource(R.string.label_email))
                if (contact.emails.isEmpty()) {
                    Text(stringResource(R.string.not_specified))
                } else {
                    contact.emails.forEach { email ->
                        Text(email)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Text(
        text = contact.name ?: stringResource(R.string.no_name),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}