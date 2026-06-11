package io.github.mobdev.ui.chat

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.mobdev.R
import io.github.mobdev.data.Message

private const val BASE_URL = "http://faerytea.name:8008"

@Composable
fun ChatPane(
    messages: List<Message>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    currentUsername: String?,
    onLoadMore: () -> Unit,
    onSend: (String) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }

    val atTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    LaunchedEffect(atTop) {
        if (atTop && hasMore && !isLoadingMore && messages.isNotEmpty()) {
            onLoadMore()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = false,
                ) {
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                        }
                    }
                    if (hasMore && !isLoadingMore) {
                        item {
                            TextButton(
                                onClick = onLoadMore,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.load_more)) }
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        MessageItem(
                            message = message,
                            isMine = message.from == currentUsername,
                            onImageClick = onImageClick,
                        )
                    }
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(stringResource(R.string.message_hint)) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSend(inputText)
                        inputText = ""
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_button),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MessageItem(
    message: Message,
    isMine: Boolean,
    onImageClick: (String) -> Unit,
) {
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val containerColor = if (isMine)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = alignment,
    ) {
        if (!isMine) {
            Text(
                text = message.from,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            val text = message.data.text
            val image = message.data.image
            when {
                text != null -> Text(
                    text = text.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                image != null -> {
                    val thumbUrl = image.link?.let { "$BASE_URL/thumb/$it" }
                    val fullUrl = image.link?.let { "$BASE_URL/img/$it" }
                    val imagePath = image.link
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = stringResource(R.string.image_message),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(200.dp)
                            .then(
                                if (imagePath != null) Modifier.clickable { onImageClick(imagePath) }
                                else Modifier
                            ),
                    )
                }
                else -> Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }
}
