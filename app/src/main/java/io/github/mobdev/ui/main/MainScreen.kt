package io.github.mobdev.ui.main

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mobdev.R
import io.github.mobdev.ui.channels.ChannelListPane
import io.github.mobdev.ui.chat.ChatPane

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    onImageClick: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,   // portrait: push chat screen
    isPortrait: Boolean,
    showChatInline: Boolean = false,       // portrait: we're already on chat screen
    chatChannel: String? = null,           // portrait: channel passed via nav arg
) {
    val channels by viewModel.channels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoadingMessages by viewModel.isLoadingMessages.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMoreMessages.collectAsState()
    val event by viewModel.event.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Handle events
    LaunchedEffect(event) {
        when (event) {
            is MainUiEvent.Unauthorized -> {
                viewModel.consumeEvent()
                onLogout()
            }
            is MainUiEvent.Error -> viewModel.consumeEvent()
            null -> Unit
        }
    }

    // Load channels once
    LaunchedEffect(Unit) {
        viewModel.loadChannels()
    }

    // Portrait: if we come here with a channel arg, select it in VM
    LaunchedEffect(chatChannel) {
        if (chatChannel != null && selectedChannel != chatChannel) {
            viewModel.selectChannel(chatChannel)
        }
    }

    val errorEvent = event as? MainUiEvent.Error
    if (errorEvent != null) {
        AlertDialog(
            onDismissRequest = { viewModel.consumeEvent() },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(errorEvent.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.consumeEvent() }) {
                    Text(stringResource(R.string.ok_button))
                }
            },
        )
    }

    if (isLandscape) {
        // ── LANDSCAPE: two-pane ──────────────────────────────────────
        BackHandler(enabled = selectedChannel != null) {
            viewModel.clearSelectedChannel()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedChannel ?: stringResource(R.string.channels_title)) },
                    actions = {
                        IconButton(onClick = { viewModel.logout(onLogout) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = stringResource(R.string.logout_button),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {
                ChannelListPane(
                    channels = channels,
                    selectedChannel = selectedChannel,
                    onChannelClick = { viewModel.selectChannel(it) },
                    modifier = Modifier.width(240.dp).fillMaxHeight(),
                )
                VerticalDivider()
                if (selectedChannel != null) {
                    ChatPane(
                        messages = messages,
                        isLoading = isLoadingMessages,
                        isLoadingMore = isLoadingMore,
                        hasMore = hasMore,
                        currentUsername = viewModel.tokenStorage.savedUsername,
                        onLoadMore = { viewModel.loadMoreMessages() },
                        onSend = { viewModel.sendMessage(it) },
                        onImageClick = onImageClick,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.select_chat)) }
                }
            }
        }
    } else if (showChatInline) {
        // ── PORTRAIT: chat screen ───────────────────────────────────
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedChannel ?: "") },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.clearSelectedChannel()
                            onLogout.let { /* handled by caller via BackHandler */ }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_button),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            ChatPane(
                messages = messages,
                isLoading = isLoadingMessages,
                isLoadingMore = isLoadingMore,
                hasMore = hasMore,
                currentUsername = viewModel.tokenStorage.savedUsername,
                onLoadMore = { viewModel.loadMoreMessages() },
                onSend = { viewModel.sendMessage(it) },
                onImageClick = onImageClick,
                modifier = Modifier.padding(paddingValues),
            )
        }
    } else {
        // ── PORTRAIT: channel list screen ────────────────────────────
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.channels_title)) },
                    actions = {
                        IconButton(onClick = { viewModel.logout(onLogout) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = stringResource(R.string.logout_button),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            ChannelListPane(
                channels = channels,
                selectedChannel = null,
                onChannelClick = { channel ->
                    viewModel.selectChannel(channel)
                    onNavigateToChat(channel)
                },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}
