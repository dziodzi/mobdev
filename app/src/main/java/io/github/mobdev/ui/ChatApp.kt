package io.github.mobdev.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChatApp(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler {
        when {
            state.openedImagePath != null -> viewModel.closeImage()
            state.selectedChannel != null -> viewModel.closeChannel()
            else -> (context as? Activity)?.finish()
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !state.isLoggedIn -> {
                        LoginScreen(
                            state = state,
                            onUsernameChange = viewModel::onUsernameChange,
                            onPasswordChange = viewModel::onPasswordChange,
                            onLoginClick = viewModel::login,
                            onErrorDismiss = viewModel::clearError,
                        )
                    }

                    isLandscape -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            ChannelListScreen(
                                state = state,
                                onChannelClick = viewModel::openChannel,
                                onRefreshClick = viewModel::loadChannels,
                                onLogoutClick = viewModel::logout,
                                modifier = Modifier
                                    .weight(0.35f)
                                    .fillMaxHeight(),
                            )

                            ChatScreen(
                                state = state,
                                viewModel = viewModel,
                                modifier = Modifier
                                    .weight(0.65f)
                                    .fillMaxHeight(),
                            )
                        }
                    }

                    state.selectedChannel == null -> {
                        ChannelListScreen(
                            state = state,
                            onChannelClick = viewModel::openChannel,
                            onRefreshClick = viewModel::loadChannels,
                            onLogoutClick = viewModel::logout,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    else -> {
                        ChatScreen(
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                state.openedImagePath?.let { openedImagePath ->
                    FullscreenImageViewer(
                        imageUrl = viewModel.imageUrl(openedImagePath, fullResolution = true),
                        onClose = viewModel::closeImage,
                    )
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
