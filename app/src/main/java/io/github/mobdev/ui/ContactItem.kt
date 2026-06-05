package io.github.mobdev.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mobdev.R
import io.github.mobdev.data.Contact

private const val EMPTY_VALUE_PLACEHOLDER = "-"

@Composable
fun ContactItem(contact: Contact) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = contact.name ?: EMPTY_VALUE_PLACEHOLDER)

            if (expanded) {
                ContactField(
                    label = stringResource(R.string.phone_label),
                    value = contact.phoneNumber,
                    topPadding = 8.dp
                )
                ContactField(
                    label = stringResource(R.string.email_label),
                    value = contact.email,
                    topPadding = 4.dp
                )
            }
        }
    }
}

@Composable
private fun ContactField(
    label: String,
    value: String?,
    topPadding: Dp,
) {
    Text(
        text = "$label: ${value ?: EMPTY_VALUE_PLACEHOLDER}",
        modifier = Modifier.padding(top = topPadding)
    )
}