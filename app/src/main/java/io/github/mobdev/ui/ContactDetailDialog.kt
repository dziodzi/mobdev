package io.github.mobdev.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mobdev.Contact
import io.github.mobdev.R

@Composable
fun ContactDetailDialog(contact: Contact, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = {
            Text(
                text = contact.name ?: stringResource(R.string.unknown_name),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.phone_label),
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = contact.phoneNumber ?: stringResource(R.string.no_phone))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.email_label),
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = contact.email ?: stringResource(R.string.no_email))
            }
        }
    )
}
