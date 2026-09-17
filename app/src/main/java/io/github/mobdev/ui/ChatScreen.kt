package io.github.mobdev.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.mobdev.R
import io.github.mobdev.data.domain.ChatMessage
import io.github.mobdev.data.domain.MessageContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    state: ChatUiState,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedChannel = state.selectedChannel
    val listState = rememberLazyListState()

    LaunchedEffect(selectedChannel, state.messages.lastOrNull()?.id) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::closeChannel) {
                Text(
                    text = stringResource(R.string.back_button),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedChannel ?: stringResource(R.string.choose_chat),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.message_count, state.messages.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { viewModel.loadMessages() },
                enabled = selectedChannel != null && !state.isLoading,
            ) {
                Text(text = stringResource(R.string.refresh_button))
            }
        }

        if (selectedChannel == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.choose_chat_left))
            }
            return@Column
        }

        if (state.error != null) {
            Text(
                text = state.error.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.hasMoreMessages && state.messages.isNotEmpty()) {
                item(key = LOAD_MORE_KEY) {
                    Button(
                        onClick = viewModel::loadOlderMessages,
                        enabled = !state.isLoadingMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val textRes = if (state.isLoadingMore) {
                            R.string.loading_more
                        } else {
                            R.string.load_more
                        }
                        Text(text = stringResource(textRes))
                    }
                }
            }

            items(state.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isMine = viewModel.isMyMessage(message.from),
                    imageUrl = { path -> viewModel.imageUrl(path, fullResolution = false) },
                    onImageClick = viewModel::openImage,
                )
            }
        }

        MessageInput(
            text = state.messageText,
            onTextChange = viewModel::onMessageTextChange,
            onSendClick = viewModel::sendMessage,
            enabled = !state.isLoading,
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    imageUrl: (String) -> String,
    onImageClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.from,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (val content = message.content) {
                    is MessageContent.Text -> {
                        Text(
                            text = content.text,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    is MessageContent.Image -> {
                        AsyncImage(
                            model = imageUrl(content.link),
                            contentDescription = stringResource(R.string.image_content_description),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(220.dp)
                                .clickable { onImageClick(content.link) },
                        )
                    }

                    MessageContent.Unknown -> {
                        Text(text = stringResource(R.string.unsupported_message))
                    }
                }

                if (message.time != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.time.toReadableTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(text = stringResource(R.string.message_hint)) },
            enabled = enabled,
            maxLines = 4,
        )

        Button(
            onClick = onSendClick,
            enabled = enabled && text.isNotBlank(),
        ) {
            Text(text = stringResource(R.string.send_button))
        }
    }
}

private fun Long.toReadableTime(): String {
    return SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(this))
}

private const val LOAD_MORE_KEY = "load_more"
