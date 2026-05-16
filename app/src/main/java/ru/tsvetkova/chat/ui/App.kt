package ru.tsvetkova.chat.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.tsvetkova.chat.ChatApplication
import ru.tsvetkova.chat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatApp(app: ChatApplication) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(app))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when {
                        state.stage != AuthStage.Authorized -> stringResource(R.string.app_name)
                        state.openImage != null -> stringResource(R.string.title_image)
                        !isLandscape && state.openChannel != null -> state.openChannel.orEmpty()
                        else -> stringResource(R.string.title_chats)
                    }
                    Text(title)
                },
                navigationIcon = {
                    val showBack = state.stage == AuthStage.Authorized && (
                        state.openImage != null || state.openChannel != null
                        )
                    if (showBack) {
                        IconButton(onClick = { handleBack(state, viewModel) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
                actions = {
                    if (state.stage == AuthStage.Authorized &&
                        state.openChannel == null &&
                        state.openImage == null
                    ) {
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(R.string.action_logout),
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        when (state.stage) {
            AuthStage.Booting -> BootingScreen(innerPadding)
            AuthStage.NeedsLogin -> LoginScreen(
                padding = innerPadding,
                inProgress = state.loginInProgress,
                error = state.loginError,
                onSubmit = viewModel::submitLogin,
                onDismissError = viewModel::dismissLoginError,
            )
            AuthStage.Authorized -> AuthorizedContent(
                padding = innerPadding,
                state = state,
                isLandscape = isLandscape,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun BootingScreen(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthorizedContent(
    padding: PaddingValues,
    state: AppState,
    isLandscape: Boolean,
    viewModel: AppViewModel,
) {
    BackHandler(enabled = state.openImage != null || state.openChannel != null) {
        handleBack(state, viewModel)
    }

    val image = state.openImage
    if (image != null && !isLandscape) {
        ImageScreen(path = image, onClose = viewModel::closeImage)
        return
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                ChatsScreen(
                    padding = PaddingValues(0.dp),
                    channels = state.channels,
                    loading = state.channelsLoading,
                    selectedChannel = state.openChannel,
                    onSelect = viewModel::openChat,
                )
            }
            VerticalDivider()
            Box(modifier = Modifier.weight(2f).fillMaxSize()) {
                val openImg = state.openImage
                val openCh = state.openChannel
                when {
                    openImg != null -> ImageScreen(path = openImg, onClose = viewModel::closeImage)
                    openCh != null -> MessagesScreen(
                        padding = PaddingValues(0.dp),
                        channel = openCh,
                        currentUser = state.user,
                        messages = state.messages,
                        loading = state.messagesLoading,
                        sending = state.sending,
                        onSend = viewModel::sendMessage,
                        onOpenImage = viewModel::openImage,
                        onLoadOlder = viewModel::loadMoreOlder,
                    )
                    else -> SelectChatPlaceholder()
                }
            }
        }
    } else {
        val openCh = state.openChannel
        if (openCh == null) {
            ChatsScreen(
                padding = padding,
                channels = state.channels,
                loading = state.channelsLoading,
                selectedChannel = null,
                onSelect = viewModel::openChat,
            )
        } else {
            MessagesScreen(
                padding = padding,
                channel = openCh,
                currentUser = state.user,
                messages = state.messages,
                loading = state.messagesLoading,
                sending = state.sending,
                onSend = viewModel::sendMessage,
                onOpenImage = viewModel::openImage,
                onLoadOlder = viewModel::loadMoreOlder,
            )
        }
    }
}

@Composable
private fun SelectChatPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.select_chat))
    }
}

private fun handleBack(state: AppState, viewModel: AppViewModel) {
    when {
        state.openImage != null -> viewModel.closeImage()
        state.openChannel != null -> viewModel.closeChat()
    }
}
