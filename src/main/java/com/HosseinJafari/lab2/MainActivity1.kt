package com.alirzw.lab2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import java.util.UUID
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch

// ── Data

data class Contact(
    val id: String,
    val name: String?,
    val phoneNumber: String?,
    val email: String?,
    val isFavorite: Boolean = false
)

// ── Contacts Provider helper

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {

    val contactsMap = mutableMapOf<String, Contact>()

    // 📞 1-получение номер и имя пользователя
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    )?.use { cursor ->

        while (cursor.moveToNext()) {

            val id = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            ).takeIf { it >= 0 }?.let { cursor.getString(it) } ?: continue

            val name = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ).takeIf { it >= 0 }?.let { cursor.getString(it) }

            val phone = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ).takeIf { it >= 0 }?.let { cursor.getString(it) }

            contactsMap[id] = Contact(
                id = id,
                name = name,
                phoneNumber = phone,
                email = null
            )
        }
    }

    // 📧 2-получение почти пользователя
    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        null, null, null, null
    )?.use { cursor ->

        while (cursor.moveToNext()) {

            val id = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID
            ).takeIf { it >= 0 }?.let { cursor.getString(it) } ?: continue

            val email = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ).takeIf { it >= 0 }?.let { cursor.getString(it) }

            val existing = contactsMap[id]

            if (existing != null) {
                contactsMap[id] = existing.copy(email = email)
            }
        }
    }

    return contactsMap.values.toList()
}

// ── Theme

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF42A5F5),
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E)
)

@Composable
fun ContactsAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

// ── Activity

class MainActivity1 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactsAppTheme {
                ContactsApp()
            }
        }
    }
}

// ── Root composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsApp() {

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // — Permission —
    var permissionGranted by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var hasLoadedContacts by rememberSaveable { mutableStateOf(false) }

    // — Data —
    var contacts by rememberSaveable { mutableStateOf<List<Contact>>(emptyList()) }
    var recentlyDeleted by rememberSaveable { mutableStateOf<Contact?>(null) }

    // — UI —
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedContact by rememberSaveable { mutableStateOf<Contact?>(null) }
    var showDetailCard by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { granted ->
//        permissionGranted = granted
//        permissionDenied = !granted
//    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val readGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        val writeGranted = permissions[Manifest.permission.WRITE_CONTACTS] == true

        permissionGranted = readGranted && writeGranted
        permissionDenied = !permissionGranted
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && !hasLoadedContacts) {
            contacts = context.fetchAllContacts()
            hasLoadedContacts = true
        }
    }

//    LaunchedEffect(Unit) {
//        if (!permissionGranted) {
//            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
//        }
//    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS
                )
            )
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
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
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
                            contentDescription = if (isEditing)
                                stringResource(R.string.edit_contact)
                            else
                                stringResource(R.string.add_contact)
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
                // — Permission denied —
                permissionDenied && !permissionGranted -> {
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
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS
                                )
                            )
                        }) {
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                }

                // — Main content —
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_contacts)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = null)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Empty state
                        if (filteredContacts.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (searchQuery.isNotBlank())
                                        Icons.Filled.SearchOff
                                    else
                                        Icons.Filled.PersonOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank())
                                        stringResource(R.string.no_results)
                                    else
                                        stringResource(R.string.no_contacts),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredContacts, key = { it.id }) { contact ->
                                    ContactListItem(
                                        contact = contact,
                                        onClick = {
                                            focusManager.clearFocus()
                                            selectedContact = contact
                                            showDetailCard = true
                                        },
                                        onFavoriteToggle = {
                                            contacts = contacts.map {
                                                if (it.id == contact.id)
                                                    it.copy(isFavorite = !it.isFavorite)
                                                else it
                                            }
                                            if (selectedContact?.id == contact.id) {
                                                selectedContact =
                                                    selectedContact?.copy(isFavorite = !contact.isFavorite)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // — Detail card overlay با انیمیشن —
                    AnimatedVisibility(
                        visible = showDetailCard && selectedContact != null,
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
                                ) {
                                    showDetailCard = false
                                    selectedContact = null
                                }
                        )
                    }

                    AnimatedVisibility(
                        visible = showDetailCard && selectedContact != null,
                        enter = fadeIn() + slideInVertically { it / 4 },
                        exit = fadeOut() + slideOutVertically { it / 4 }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            selectedContact?.let { contact ->
                                ContactDetailCard(
                                    contact = contact,
                                    onClose = {
                                        showDetailCard = false
                                        selectedContact = null
                                    },
                                    onDeleteRequest = { showDeleteDialog = true },
                                    onFavoriteToggle = {
                                        contacts = contacts.map {
                                            if (it.id == contact.id)
                                                it.copy(isFavorite = !it.isFavorite)
                                            else it
                                        }
                                        selectedContact =
                                            selectedContact?.copy(isFavorite = !contact.isFavorite)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // — Add dialog —
            if (showAddDialog) {
                AddEditContactDialog(
                    title = stringResource(R.string.add_contact),
                    initialName = "",
                    initialPhone = "",
                    initialEmail = "",
                    onConfirm = { name, phone, email ->
                        val newContact = Contact(
                            id = UUID.randomUUID().toString(),
                            name = name.ifBlank { null },
                            phoneNumber = phone.ifBlank { null },
                            email = email.ifBlank { null },
                            isFavorite = false
                        )
                        context.insertContact(name, phone, email)
                        contacts = context.fetchAllContacts()
                        showAddDialog = false
                    },
                    onDismiss = { showAddDialog = false }
                )
            }

            // — Edit dialog —
            if (showEditDialog && selectedContact != null) {
                val editing = selectedContact!!
                AddEditContactDialog(
                    title = stringResource(R.string.edit_contact),
                    initialName = editing.name ?: "",
                    initialPhone = editing.phoneNumber ?: "",
                    initialEmail = editing.email ?: "",
                    onConfirm = { name, phone, email ->
                        val updated = editing.copy(
                            name = name.ifBlank { null },
                            phoneNumber = phone.ifBlank { null },
                            email = email.ifBlank { null }
                        )
                        context.updateContact(updated)
                        contacts = context.fetchAllContacts()
                        selectedContact = updated
                        showEditDialog = false
                        showDetailCard = true
                    },
                    onDismiss = {
                        showEditDialog = false
                        showDetailCard = true
                    }
                )
            }

            // — Delete dialog —
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
                                recentlyDeleted = toDelete
                                context.deleteContact(toDelete.id)
                                contacts = context.fetchAllContacts()
                                showDeleteDialog = false
                                showDetailCard = false
                                selectedContact = null

                                // Snackbar با Undo
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(
                                            R.string.contact_deleted,
                                            toDelete.name
                                                ?: context.getString(R.string.unknown_name)
                                        ),
                                        actionLabel = context.getString(R.string.undo),
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        recentlyDeleted?.let { restored ->
                                            contacts = contacts + restored
                                            recentlyDeleted = null
                                        }
                                    }
                                }
                            }
                        ) {
                            Text(
                                stringResource(R.string.confirm_delete),
                                color = MaterialTheme.colorScheme.error
                            )
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

// ── com.alirzw.lab2.data.models.Contact list item

@Composable
fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Person icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name
            Text(
                text = contact.name ?: stringResource(R.string.unknown_name),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Favorite heart — only shown when isFavorite = true
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (contact.isFavorite)
                        Icons.Filled.Favorite
                    else
                        Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (contact.isFavorite)
                        Color.Red
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ── Detail card

@Composable
fun ContactDetailCard(
    contact: Contact,
    onClose: () -> Unit,
    onDeleteRequest: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp))
            .clickable(enabled = false) {}, // absorb clicks
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Avatar circle with first letter
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                val initial =
                    contact.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                Text(
                    text = initial,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name + favorite icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = contact.name ?: stringResource(R.string.unknown_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (contact.isFavorite)
                            Icons.Filled.Favorite
                        else
                            Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (contact.isFavorite)
                            Color.Red
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone
            DetailRow(
                icon = {
                    Icon(
                        Icons.Filled.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = contact.phoneNumber ?: stringResource(R.string.no_phone)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email
            DetailRow(
                icon = {
                    Icon(
                        Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = contact.email ?: stringResource(R.string.no_email)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Delete button
            Button(
                onClick = onDeleteRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.delete_contact),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun DetailRow(icon: @Composable () -> Unit, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Add / Edit dialog

@Composable
fun AddEditContactDialog(
    title: String,
    initialName: String,
    initialPhone: String,
    initialEmail: String,
    onConfirm: (name: String, phone: String, email: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf(initialEmail) }

    // Track user interaction
    var nameTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }

    // Validation
    val isNameValid = name.isNotBlank()
    val isPhoneValid = phone.isNotBlank()
    val isFormValid = isNameValid && isPhoneValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameTouched = true
                    },
                    label = { Text(stringResource(R.string.field_name)) },
                    isError = nameTouched && name.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (nameTouched && name.isBlank()) {
                    Text(
                        text = "Name is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneTouched = true
                    },
                    label = { Text(stringResource(R.string.field_phone)) },
                    isError = phoneTouched && phone.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (phoneTouched && phone.isBlank()) {
                    Text(
                        text = "Phone is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Email (optional)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.field_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(name, phone, email) },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

fun Context.insertContact(name: String, phone: String, email: String?) {
    val ops = ArrayList<android.content.ContentProviderOperation>()

    val rawContactInsertIndex = ops.size

    ops.add(
        android.content.ContentProviderOperation.newInsert(
            ContactsContract.RawContacts.CONTENT_URI
        ).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()
    )

    // Name
    ops.add(
        android.content.ContentProviderOperation.newInsert(
            ContactsContract.Data.CONTENT_URI
        )
            .withValueBackReference(
                ContactsContract.Data.RAW_CONTACT_ID,
                rawContactInsertIndex
            )
            .withValue(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
            .withValue(
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                name
            )
            .build()
    )

    // Phone
    ops.add(
        android.content.ContentProviderOperation.newInsert(
            ContactsContract.Data.CONTENT_URI
        )
            .withValueBackReference(
                ContactsContract.Data.RAW_CONTACT_ID,
                rawContactInsertIndex
            )
            .withValue(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
            )
            .withValue(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                phone
            )
            .withValue(
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            )
            .build()
    )

    // Email (optional)
    if (!email.isNullOrBlank()) {
        ops.add(
            android.content.ContentProviderOperation.newInsert(
                ContactsContract.Data.CONTENT_URI
            )
                .withValueBackReference(
                    ContactsContract.Data.RAW_CONTACT_ID,
                    rawContactInsertIndex
                )
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                )
                .withValue(
                    ContactsContract.CommonDataKinds.Email.ADDRESS,
                    email
                )
                .build()
        )
    }

    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
}


fun Context.deleteContact(contactId: String) {
    val uri = ContactsContract.RawContacts.CONTENT_URI
    val where = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
    val params = arrayOf(contactId)

    contentResolver.delete(uri, where, params)
}


fun Context.updateContact(contact: Contact) {

    val resolver = contentResolver


    val rawContactId = resolver.query(
        ContactsContract.RawContacts.CONTENT_URI,
        arrayOf(ContactsContract.RawContacts._ID),
        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
        arrayOf(contact.id),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: return


    // NAME
    val nameWhere =
        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"

    val nameParams = arrayOf(
        rawContactId,
        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
    )

    val nameExists = resolver.query(
        ContactsContract.Data.CONTENT_URI,
        null,
        nameWhere,
        nameParams,
        null
    )?.use { cursor ->
        cursor.count > 0
    } ?: false

    if (nameExists) {
        val values = android.content.ContentValues().apply {
            put(
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                contact.name
            )
        }

        resolver.update(
            ContactsContract.Data.CONTENT_URI,
            values,
            nameWhere,
            nameParams
        )

    } else if (!contact.name.isNullOrBlank()) {

        val values = android.content.ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
            put(
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                contact.name
            )
        }

        resolver.insert(ContactsContract.Data.CONTENT_URI, values)
    }

    // PHONE
    val phoneWhere =
        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"

    val phoneParams = arrayOf(
        rawContactId,
        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
    )

    val phoneExists = resolver.query(
        ContactsContract.Data.CONTENT_URI,
        null,
        phoneWhere,
        phoneParams,
        null
    )?.use { cursor ->
        cursor.count > 0
    } ?: false

    if (phoneExists) {
        val values = android.content.ContentValues().apply {
            put(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                contact.phoneNumber
            )
        }

        resolver.update(
            ContactsContract.Data.CONTENT_URI,
            values,
            phoneWhere,
            phoneParams
        )

    } else if (!contact.phoneNumber.isNullOrBlank()) {

        val values = android.content.ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
            )
            put(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                contact.phoneNumber
            )
            put(
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            )
        }

        resolver.insert(ContactsContract.Data.CONTENT_URI, values)
    }


    // EMAIL
    val emailWhere =
        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"

    val emailParams = arrayOf(
        rawContactId,
        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
    )

    val emailExists = resolver.query(
        ContactsContract.Data.CONTENT_URI,
        null,
        emailWhere,
        emailParams,
        null
    )?.use { cursor ->
        cursor.count > 0
    } ?: false

    if (emailExists) {
        val values = android.content.ContentValues().apply {
            put(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                contact.email
            )
        }

        resolver.update(
            ContactsContract.Data.CONTENT_URI,
            values,
            emailWhere,
            emailParams
        )

    } else if (!contact.email.isNullOrBlank()) {

        val values = android.content.ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
            )
            put(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                contact.email
            )
            put(
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.TYPE_HOME
            )
        }

        resolver.insert(ContactsContract.Data.CONTENT_URI, values)
    }
}