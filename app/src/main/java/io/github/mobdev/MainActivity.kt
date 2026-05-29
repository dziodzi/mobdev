package io.github.mobdev

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ContactsApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: ContactsViewModel = viewModel()

    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPermissionDenied by viewModel.isPermissionDenied.collectAsState()

    val permissionGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.resetPermissionDenied()
            coroutineScope.launch {
                viewModel.loadContacts(context.contentResolver)
            }
        } else {
            viewModel.setPermissionDenied()
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && contacts == null && !isLoading && !isPermissionDenied) {
            viewModel.loadContacts(context.contentResolver)
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.loading_contacts))
                    }
                }

                // Список контактов
                !contacts.isNullOrEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Шапка с количеством контактов
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.total_contacts, contacts!!.size),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // Список контактов
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(contacts!!) { contact ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clickable { selectedContact = contact },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Text(
                                        text = contact.name,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }

                // Нет разрешения или разрешение отклонено
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.permission_required))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.permission_read_contacts))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                            Text(stringResource(R.string.permission_grant_button))
                        }
                        if (isPermissionDenied) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.permission_denied_message),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedContact != null) {
        AlertDialog(
            onDismissRequest = { selectedContact = null },
            title = { Text(stringResource(R.string.contact_info_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.contact_name, selectedContact?.name ?: ""))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.contact_phone,
                            selectedContact?.phoneNumber
                                ?: stringResource(R.string.contact_not_specified)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.contact_email,
                            selectedContact?.email ?: stringResource(R.string.contact_not_specified)
                        )
                    )
                    if (!selectedContact?.birthday.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.contact_birthday,
                                selectedContact?.birthday ?: ""
                            )
                        )
                    }
                    if (!selectedContact?.note.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.contact_note, selectedContact?.note ?: ""))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedContact = null }) {
                    Text(stringResource(R.string.dialog_close))
                }
            }
        )
    }
}