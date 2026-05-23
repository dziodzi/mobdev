package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mobdev.data.AuthStore
import io.github.mobdev.ui.ChannelsScreen
import io.github.mobdev.ui.ImageScreen
import io.github.mobdev.ui.LoginScreen
import io.github.mobdev.ui.MessagesScreen
import io.github.mobdev.ui.theme.KekTheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KekTheme {
                val wsc = calculateWindowSizeClass(this)
                val isWide = wsc.widthSizeClass == WindowWidthSizeClass.Expanded ||
                        wsc.widthSizeClass == WindowWidthSizeClass.Medium
                AppRoot(isWide = isWide)
            }
        }
    }
}

private fun enc(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8.name())
private fun dec(s: String): String = URLDecoder.decode(s, StandardCharsets.UTF_8.name())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(isWide: Boolean) {
    val ctx = LocalContext.current
    val store = remember { AuthStore(ctx) }

    var loggedIn by rememberSaveable {
        mutableStateOf(!store.token.isNullOrBlank() && !store.username.isNullOrBlank())
    }

    // Общее состояние, которое переживает поворот
    var openChannel by rememberSaveable { mutableStateOf<String?>(null) }
    var openImage by rememberSaveable { mutableStateOf<String?>(null) }

    androidx.activity.compose.BackHandler(
        enabled = loggedIn && (openImage != null || openChannel != null)
    ) {
        when {
            openImage != null -> openImage = null
            openChannel != null -> openChannel = null
        }
    }

    if (!loggedIn) {
        LoginScreen(onLoggedIn = { loggedIn = true })
        return
    }

    val onLogout: () -> Unit = {
        openChannel = null
        openImage = null
        loggedIn = false
    }

    if (isWide) {
        // Landscape: master-detail
        Row(Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(320.dp).fillMaxSize()) {
                ChannelsScreen(
                    selectedChannel = openChannel,
                    onChannelClick = {
                        openChannel = it
                        openImage = null
                    },
                    onLogout = onLogout,
                    onUnauthorized = onLogout
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                when {
                    openImage != null -> ImageScreen(
                        link = openImage!!,
                        onBack = { openImage = null }
                    )
                    openChannel != null -> MessagesScreen(
                        channel = openChannel!!,
                        onBack = null,
                        onImageClick = { openImage = it },
                        onUnauthorized = onLogout
                    )
                    else -> Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "◆",
                                    fontSize = 64.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.select_chat),
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Portrait: один экран в зависимости от состояния
        when {
            openImage != null -> ImageScreen(
                link = openImage!!,
                onBack = { openImage = null }
            )
            openChannel != null -> MessagesScreen(
                channel = openChannel!!,
                onBack = { openChannel = null },
                onImageClick = { openImage = it },
                onUnauthorized = onLogout
            )
            else -> ChannelsScreen(
                onChannelClick = { openChannel = it },
                onLogout = onLogout,
                onUnauthorized = onLogout
            )
        }
    }
}