package com.alirzw.lab2.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.alirzw.lab2.R
import com.alirzw.lab2.data.models.Contact
import com.alirzw.lab2.data.repository.ContactsRepository
import com.alirzw.lab2.ui.components.AddEditContactDialog
import com.alirzw.lab2.ui.components.ContactDetailCard
import com.alirzw.lab2.ui.components.ContactListItem
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val repository = remember { ContactsRepository(context) }

    // States
    var permissionGranted by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var contacts by rememberSaveable { mutableStateOf<List<Contact>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedContact by rememberSaveable { mutableStateOf<Contact?>(null) }
    var showDetailCard by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        permissionDenied = !permissionGranted
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            contacts = repository.fetchAllContacts()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { it.name?.contains(searchQuery, ignoreCase = true) == true }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (permissionGranted) {
                FloatingActionButton(
                    onClick = {
                        if (showDetailCard && selectedContact != null) {
                            showDetailCard = false
                            showEditDialog = true
                        } else {
                            showAddDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    AnimatedContent(
                        targetState = showDetailCard && selectedContact != null,
                        label = "fab_icon"
                    ) { isEditing ->
                        Icon(
                            imageVector = if (isEditing) Icons.Filled.Edit else Icons.Filled.Add,
                            contentDescription = if (isEditing) "Edit" else "Add"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { focusManager.clearFocus() }
        ) {
            when {
                permissionDenied && !permissionGranted -> {
                    PermissionDeniedView(onRequestPermission = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                    })
                }
                else -> {
                    ContactsContent(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        filteredContacts = filteredContacts,
                        onContactClick = { contact ->
                            focusManager.clearFocus()
                            selectedContact = contact
                            showDetailCard = true
                        },
                        onFavoriteToggle = { contact ->
                            contacts = contacts.map {
                                if (it.id == contact.id) it.copy(isFavorite = !it.isFavorite)
                                else it
                            }
                            if (selectedContact?.id == contact.id) {
                                selectedContact = selectedContact?.copy(isFavorite = !contact.isFavorite)
                            }
                        }
                    )
                }
            }

            // Dialogs and overlays
            ContactDetailOverlay(
                visible = showDetailCard && selectedContact != null,
                contact = selectedContact,
                onClose = {
                    showDetailCard = false
                    selectedContact = null
                },
                onDeleteRequest = { showDeleteDialog = true },
                onFavoriteToggle = { contact ->
                    contacts = contacts.map {
                        if (it.id == contact.id) it.copy(isFavorite = !it.isFavorite)
                        else it
                    }
                    selectedContact = selectedContact?.copy(isFavorite = !contact.isFavorite)
                }
            )

            if (showAddDialog) {
                AddEditContactDialog(
                    title = stringResource(R.string.add_contact),
                    initialName = "",
                    initialPhone = "",
                    initialEmail = "",
                    onConfirm = { name, phone, email ->
                        scope.launch {
                            repository.insertContact(name, phone, email)
                            contacts = repository.fetchAllContacts()
                            showAddDialog = false
                        }
                    },
                    onDismiss = { showAddDialog = false }
                )
            }

            if (showEditDialog && selectedContact != null) {
                val editing = selectedContact!!
                AddEditContactDialog(
                    title = stringResource(R.string.edit_contact),
                    initialName = editing.name ?: "",
                    initialPhone = editing.phoneNumber ?: "",
                    initialEmail = editing.email ?: "",
                    onConfirm = { name, phone, email ->
                        scope.launch {
                            val updated = editing.copy(
                                name = name.ifBlank { null },
                                phoneNumber = phone.ifBlank { null },
                                email = email.ifBlank { null }
                            )
                            repository.updateContact(updated)
                            contacts = repository.fetchAllContacts()
                            selectedContact = updated
                            showEditDialog = false
                            showDetailCard = true
                        }
                    },
                    onDismiss = {
                        showEditDialog = false
                        showDetailCard = true
                    }
                )
            }

            if (showDeleteDialog && selectedContact != null) {
                val toDelete = selectedContact!!
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.delete_contact)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.delete_confirmation,
                                toDelete.name ?: stringResource(R.string.unknown_name)
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    repository.deleteContact(toDelete.id)
                                    contacts = repository.fetchAllContacts()
                                    showDeleteDialog = false
                                    showDetailCard = false
                                    selectedContact = null

                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.contact_deleted,
                                            toDelete.name ?: context.getString(R.string.unknown_name)),
                                        actionLabel = context.getString(R.string.undo),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        ) {
                            Text(stringResource(R.string.confirm_delete), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionDeniedView(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_denied),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}

@Composable
fun ContactsContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredContacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onFavoriteToggle: (Contact) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.search_contacts)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                    }
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )

        if (filteredContacts.isEmpty()) {
            EmptyStateView(searchQuery.isNotBlank())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredContacts, key = { it.id }) { contact ->
                    ContactListItem(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onFavoriteToggle = { onFavoriteToggle(contact) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isSearching: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Filled.SearchOff else Icons.Filled.PersonOff,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isSearching) stringResource(R.string.no_results) else stringResource(R.string.no_contacts),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ContactDetailOverlay(
    visible: Boolean,
    contact: Contact?,
    onClose: () -> Unit,
    onDeleteRequest: () -> Unit,
    onFavoriteToggle: (Contact) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClose() }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 4 },
        exit = fadeOut() + slideOutVertically { it / 4 }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            contact?.let {
                ContactDetailCard(
                    contact = it,
                    onClose = onClose,
                    onDeleteRequest = onDeleteRequest,
                    onFavoriteToggle = { onFavoriteToggle(it) }
                )
            }
        }
    }
}