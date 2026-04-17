package dev.mobdev.contactlist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ScreenBackground = Color(0xFFF2F2F7)
private val SectionHeaderColor = Color(0xFF6D6D72)
private val ChevronColor = Color(0xFFC7C7CC)
private val AvatarBackground = Color(0xFFE5E5EA)
private val SecondaryTextColor = Color(0xFF8E8E93)

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

@Composable
private fun ContactsApp() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selected by remember { mutableStateOf<Contact?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contacts = withContext(Dispatchers.IO) {
                context.applicationContext.fetchAllContacts()
            }
        } else {
            contacts = emptyList()
        }
    }

    if (selected != null) {
        val contact = selected!!
        BackHandler { selected = null }
        ContactDetailScreen(
            contact = contact,
            onBack = { selected = null },
        )
    } else {
        ContactListScreen(
            hasPermission = hasPermission,
            contacts = contacts,
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onOpenContact = { selected = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactListScreen(
    hasPermission: Boolean,
    contacts: List<Contact>,
    onRequestPermission: () -> Unit,
    onOpenContact: (Contact) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ScreenBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ScreenBackground,
                ),
            )
        },
    ) { paddingValues ->
        Box(
            Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            when {
                !hasPermission -> NoPermissionContent(onRequestPermission)
                contacts.isEmpty() -> EmptyContactsContent()
                else -> GroupedContactList(contacts, onOpenContact)
            }
        }
    }
}

@Composable
private fun NoPermissionContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.no_permission_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.no_permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryTextColor,
        )
        Spacer(modifier = Modifier.size(24.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.request_permission))
        }
    }
}

@Composable
private fun EmptyContactsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_list),
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryTextColor,
        )
    }
}

@Composable
private fun GroupedContactList(
    contacts: List<Contact>,
    onOpenContact: (Contact) -> Unit,
) {
    val grouped = remember(contacts) {
        contacts
            .groupBy { sectionKey(it.name) }
            .toSortedMap(sectionKeyComparator)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        grouped.forEach { (section, rows) ->
            stickyHeader {
                SectionHeader(title = section)
            }
            itemsIndexed(
                items = rows,
                key = { index, contact ->
                    "${section}_${contact.name.orEmpty()}_${contact.phoneNumber.orEmpty()}_$index"
                },
            ) { _, contact ->
                ContactRow(contact = contact, onClick = { onOpenContact(contact) })
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color = Color(0xFFC6C6C8),
                )
            }
        }
    }
}

private fun sectionKey(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isEmpty()) return "#"
    val first = trimmed.first()
    return if (first.isLetter()) {
        first.uppercaseChar().toString()
    } else {
        "#"
    }
}

private val sectionKeyComparator = Comparator<String> { a, b ->
    val aHash = a == "#"
    val bHash = b == "#"
    when {
        aHash && !bHash -> 1
        !aHash && bHash -> -1
        else -> a.compareTo(b)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBackground)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = SectionHeaderColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ContactRow(
    contact: Contact,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(name = contact.name)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: stringResource(R.string.unknown_contact),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = contact.phoneNumber?.trim().orEmpty()
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    color = SecondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = stringResource(R.string.list_row_chevron),
            color = ChevronColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun InitialsAvatar(name: String?) {
    val initials = remember(name) { initialsFromName(name) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AvatarBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF3A3A3C),
        )
    }
}

private fun initialsFromName(name: String?): String {
    val parts = name?.trim()?.split("\\s+".toRegex()).orEmpty().filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) {
        val word = parts[0]
        return word.take(2).uppercase()
    }
    val first = parts[0].first().uppercaseChar()
    val second = parts[1].first().uppercaseChar()
    return "$first$second"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailScreen(
    contact: Contact,
    onBack: () -> Unit,
) {
    val backDescription = stringResource(R.string.back_content_description)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ScreenBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = contact.name?.trim().takeUnless { it.isNullOrEmpty() }
                            ?: stringResource(R.string.unknown_contact),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = stringResource(R.string.back_symbol),
                            modifier = Modifier.semantics { contentDescription = backDescription },
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ScreenBackground,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            DetailCard(
                label = stringResource(R.string.phone_label),
                value = contact.phoneNumber?.trim().takeUnless { it.isNullOrEmpty() },
            )
            Spacer(modifier = Modifier.size(12.dp))
            DetailCard(
                label = stringResource(R.string.email_label),
                value = contact.email?.trim().takeUnless { it.isNullOrEmpty() },
            )
        }
    }
}

@Composable
private fun DetailCard(
    label: String,
    value: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SecondaryTextColor,
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = value ?: stringResource(R.string.not_specified),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
