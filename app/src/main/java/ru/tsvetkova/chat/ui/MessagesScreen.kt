package ru.tsvetkova.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import ru.tsvetkova.chat.R
import ru.tsvetkova.chat.data.Message

@Composable
fun MessagesScreen(
    padding: PaddingValues,
    channel: String,
    currentUser: String?,
    messages: List<Message>,
    loading: Boolean,
    sending: Boolean,
    onSend: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    onLoadOlder: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(channel) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .filter { it == 0 && messages.isNotEmpty() }
            .collect { onLoadOlder() }
    }

    LaunchedEffect(messages.lastOrNull()?.id) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (messages.isEmpty()) {
                Text(
                    text = stringResource(R.string.messages_empty),
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (loading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                    items(messages, key = { it.id ?: it.hashCode().toString() }) { msg ->
                        MessageBubble(
                            message = msg,
                            isMine = msg.from == currentUser,
                            onOpenImage = onOpenImage,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(stringResource(R.string.messages_input_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 4,
                enabled = !sending,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft)
                        draft = ""
                    }
                },
                enabled = !sending && draft.isNotBlank(),
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.messages_send),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean, onOpenImage: (String) -> Unit) {
    val bg = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (isMine) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Text(
            text = message.from,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .padding(10.dp),
        ) {
            val image = message.data.image?.link
            val text = message.data.text?.text
            when {
                image != null -> {
                    AsyncImage(
                        model = "https://faerytea.name/thumb/$image",
                        contentDescription = stringResource(R.string.messages_image_cd),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(200.dp)
                            .clickable { onOpenImage(image) },
                    )
                }
                text != null -> {
                    Text(text = text)
                }
                else -> {
                    Text(text = stringResource(R.string.messages_unknown))
                }
            }
        }
    }
}
