package io.github.mobdev.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.mobdev.R
import io.github.mobdev.data.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contact: Contact,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contact_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow(
                    label = stringResource(R.string.name_label),
                    value = contact.name ?: stringResource(R.string.unknown_name)
                )
                HorizontalDivider()
                DetailRow(
                    label = stringResource(R.string.phone_label),
                    value = contact.phoneNumber ?: stringResource(R.string.no_phone)
                )
                HorizontalDivider()
                DetailRow(
                    label = stringResource(R.string.email_label),
                    value = contact.email ?: stringResource(R.string.no_email)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    ListItem(
        overlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(value) }
    )
}
