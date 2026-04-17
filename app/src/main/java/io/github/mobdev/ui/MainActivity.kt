package io.github.mobdev.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mobdev.data.Contact
import io.github.mobdev.ui.screens.ContactDetailScreen
import io.github.mobdev.ui.screens.ContactListScreen
import io.github.mobdev.ui.theme.MobdevTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobdevTheme {
                ContactsApp()
            }
        }
    }
}

@Composable
private fun ContactsApp(viewModel: ContactsViewModel = viewModel()) {
    val context = LocalContext.current

    var hasPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadContacts()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    if (selectedContact != null) {
        ContactDetailScreen(
            contact = selectedContact!!,
            onBack = { selectedContact = null }
        )
    } else {
        ContactListScreen(
            hasPermission = hasPermission,
            uiState = uiState,
            onContactClick = { selectedContact = it },
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        )
    }
}
