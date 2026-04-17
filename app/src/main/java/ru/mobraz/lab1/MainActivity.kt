package ru.mobraz.lab1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ContactsApp()
            }
        }
    }
}

@Composable
private fun ContactsApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "contacts") {
        composable("contacts") {
            ContactsScreen(
                onOpenDetails = { contactId ->
                    navController.navigate("details/$contactId")
                }
            )
        }

        composable(
            route = "details/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: return@composable
            ContactDetailsScreen(
                contactId = contactId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsScreen(onOpenDetails: (Long) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { ContactsRepository(context) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var refreshToken by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf(emptyList<ContactSummary>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            refreshToken++
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) {
            onDispose { }
        } else {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    refreshToken++
                }
            }

            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer,
            )
            context.contentResolver.registerContentObserver(
                ContactsContract.Data.CONTENT_URI,
                true,
                observer,
            )

            onDispose {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    LaunchedEffect(hasPermission, refreshToken) {
        if (hasPermission) {
            isLoading = true
            contacts = repository.fetchAllContactSummaries()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.contacts_screen_title)) }
            )
        }
    ) { innerPadding ->
        when {
            !hasPermission -> {
                PermissionRequiredContent(
                    innerPadding = innerPadding,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }

            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 24.dp))
                }
            }

            contacts.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = stringResource(id = R.string.no_contacts))
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = contacts, key = { it.id }) { contact ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDetails(contact.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(text = stringResource(id = R.string.details))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    innerPadding: PaddingValues,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.permission_required_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(id = R.string.permission_required_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRequestPermission) {
                Text(stringResource(id = R.string.request_permission))
            }
            Button(onClick = onOpenSettings) {
                Text(stringResource(id = R.string.open_settings))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsScreen(contactId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { ContactsRepository(context) }

    var detailsRefreshToken by remember { mutableIntStateOf(0) }
    var details by remember { mutableStateOf<ContactDetails?>(null) }

    DisposableEffect(contactId) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                detailsRefreshToken++
            }
        }

        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            observer,
        )
        context.contentResolver.registerContentObserver(
            ContactsContract.Data.CONTENT_URI,
            true,
            observer,
        )

        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    LaunchedEffect(contactId, detailsRefreshToken) {
        details = repository.fetchContactDetails(contactId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.details_screen_title)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val unknown = stringResource(id = R.string.unknown_value)
            val current = details

            Text(
                text = stringResource(
                    id = R.string.contact_name,
                    current?.name ?: unknown,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    id = R.string.contact_phone,
                    current?.phoneNumber ?: unknown,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    id = R.string.contact_email,
                    current?.email ?: unknown,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) {
                Text(text = stringResource(id = R.string.back))
            }
        }
    }
}
