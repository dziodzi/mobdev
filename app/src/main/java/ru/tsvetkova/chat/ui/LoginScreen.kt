package ru.tsvetkova.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.tsvetkova.chat.R

@Composable
fun LoginScreen(
    padding: PaddingValues,
    inProgress: Boolean,
    error: String?,
    onSubmit: (String, String) -> Unit,
    onDismissError: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.login_title),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.login_name)) },
            singleLine = true,
            enabled = !inProgress,
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pwd,
            onValueChange = { pwd = it },
            label = { Text(stringResource(R.string.login_pwd)) },
            singleLine = true,
            enabled = !inProgress,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onSubmit(name, pwd) },
            enabled = !inProgress && name.isNotBlank() && pwd.isNotBlank(),
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
        ) {
            if (inProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.login_submit))
            }
        }
    }

    if (error != null) {
        val message = when (error) {
            "session_expired" -> stringResource(R.string.error_session_expired)
            else -> stringResource(R.string.error_bad_credentials)
        }
        AlertDialog(
            onDismissRequest = onDismissError,
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(message) },
        )
    }
}
