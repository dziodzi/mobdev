package io.github.mobdev.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.mobdev.R
import io.github.mobdev.data.AuthStore
import io.github.mobdev.data.BASE_URL
import io.github.mobdev.data.LocalCache
import io.github.mobdev.data.Message
import io.github.mobdev.data.PendingMessage
import io.github.mobdev.data.Repository
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MessagesScreen(
    channel: String,
    onBack: (() -> Unit)?,
    onImageClick: (String) -> Unit,
    onUnauthorized: () -> Unit,
    isOnline: Boolean
) {
    val ctx = LocalContext.current
    val store = remember { AuthStore(ctx) }
    val cache = remember { LocalCache(ctx) }
    val scope = rememberCoroutineScope()

    var messages by rememberSaveable(channel) { mutableStateOf<List<Message>>(emptyList()) }
    var loading by rememberSaveable(channel) { mutableStateOf(false) }
    var input by rememberSaveable(channel) { mutableStateOf("") }
    var canLoadMore by rememberSaveable(channel) { mutableStateOf(true) }
    var pending by remember(channel) {
        mutableStateOf(cache.loadPending().filter { it.channel == channel })
    }
    val listState = rememberLazyListState()

    suspend fun sendPending() {
        val token = store.token ?: return
        val all = cache.loadPending()
        val forChannel = all.filter { it.channel == channel }
        if (forChannel.isEmpty()) return

        val sentIds = mutableListOf<String>()
        for (p in forChannel) {
            try {
                Repository.sendText(token, p.from, p.channel, p.text)
                sentIds.add(p.localId)
            } catch (e: Exception) {
                if (Repository.isUnauthorized(e)) {
                    store.token = null
                    onUnauthorized()
                    return
                }
                break
            }
        }
        if (sentIds.isNotEmpty()) {
            val remaining = all.filter { it.localId !in sentIds }
            cache.savePending(remaining)
            pending = remaining.filter { it.channel == channel }
        }
    }

    suspend fun loadInitial() {
        loading = true
        try {
            val loaded = Repository.messages(channel, lastKnownId = 0, reverse = false)
            messages = loaded
            canLoadMore = loaded.size >= 20
            cache.saveMessages(channel, messages)
        } catch (e: Exception) {
            if (Repository.isUnauthorized(e)) {
                store.token = null
                onUnauthorized()
            }
        } finally {
            loading = false
        }
    }

    suspend fun loadMore() {
        if (loading || !canLoadMore || messages.isEmpty()) return
        loading = true
        try {
            val newestId = messages.maxOf { it.id ?: 0L }
            val newer = Repository.messages(channel, lastKnownId = newestId, reverse = false)
            if (newer.isEmpty()) {
                canLoadMore = false
            } else {
                messages = (messages + newer).distinctBy { it.id }
                if (newer.size < 20) canLoadMore = false
                cache.saveMessages(channel, messages)
            }
        } catch (e: Exception) {
            if (Repository.isUnauthorized(e)) {
                store.token = null
                onUnauthorized()
            }
        } finally {
            loading = false
        }
    }

    LaunchedEffect(channel) {
        val cached = cache.loadMessages(channel)
        if (cached.isNotEmpty()) messages = cached
    }

    LaunchedEffect(channel, isOnline) {
        if (isOnline) {
            sendPending()
            loadInitial()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text(
                            "←",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        channel.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = channel.removeSuffix("@channel"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (!isOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.offline_banner),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty() && pending.isEmpty() && loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (messages.isEmpty() && pending.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.no_messages),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(messages, key = { it.id ?: (it.from + it.time) }) { msg ->
                            MessageBubble(msg, store.username, onImageClick)
                        }
                        if (canLoadMore && isOnline) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(
                                        onClick = { scope.launch { loadMore() } },
                                        enabled = !loading
                                    ) {
                                        Text(
                                            stringResource(R.string.load_more),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        items(pending, key = { "pending_${it.localId}" }) { p ->
                            PendingBubble(p)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.message_hint),
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            val text = input.trim()
                            if (text.isEmpty()) return@clickable
                            val name = store.username ?: return@clickable

                            if (!isOnline) {
                                val p = PendingMessage(
                                    localId = UUID.randomUUID().toString(),
                                    channel = channel,
                                    from = name,
                                    text = text,
                                    timestamp = System.currentTimeMillis()
                                )
                                val all = cache.loadPending()
                                cache.savePending(all + p)
                                val newPending = pending + p
                                pending = newPending
                                input = ""
                                scope.launch {
                                    val total = messages.size +
                                            (if (canLoadMore && isOnline) 1 else 0) +
                                            newPending.size
                                    if (total > 0) {
                                        listState.animateScrollToItem(total - 1)
                                    }
                                }
                                return@clickable
                            }

                            val token = store.token ?: return@clickable
                            scope.launch {
                                try {
                                    Repository.sendText(token, name, channel, text)
                                    input = ""
                                    var keepLoading = true
                                    while (keepLoading) {
                                        val newestId = messages.maxOfOrNull { it.id ?: 0L } ?: 0L
                                        val newer = Repository.messages(
                                            channel,
                                            lastKnownId = newestId,
                                            reverse = false
                                        )
                                        if (newer.isEmpty()) {
                                            keepLoading = false
                                        } else {
                                            messages = (messages + newer).distinctBy { it.id }
                                            if (newer.size < 20) keepLoading = false
                                        }
                                    }
                                    canLoadMore = false
                                    cache.saveMessages(channel, messages)
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                } catch (e: Exception) {
                                    if (Repository.isUnauthorized(e)) {
                                        store.token = null
                                        onUnauthorized()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "➤",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingBubble(msg: PendingMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                msg.text,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.message_pending),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MessageBubble(msg: Message, currentUser: String?, onImageClick: (String) -> Unit) {
    val isMine = msg.from == currentUser
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val shape = if (isMine) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!isMine) {
                Text(
                    text = msg.from,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
            }
            when {
                msg.data.text != null -> {
                    Text(
                        msg.data.text.text,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }
                msg.data.image?.link != null -> {
                    val link = msg.data.image.link
                    AsyncImage(
                        model = "${BASE_URL}thumb/$link",
                        contentDescription = null,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(link) }
                    )
                }
            }
        }
    }
}
