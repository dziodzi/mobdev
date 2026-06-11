package io.github.mobdev.ui

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mobdev.ui.image.ImageScreen
import io.github.mobdev.ui.login.LoginScreen
import io.github.mobdev.ui.login.LoginViewModel
import io.github.mobdev.ui.main.MainScreen
import io.github.mobdev.ui.main.MainViewModel

private const val ROUTE_LOGIN = "login"
private const val ROUTE_CHANNELS = "channels"
private const val ROUTE_CHAT = "chat/{channel}"
private const val ROUTE_IMAGE = "image/{path}"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // In landscape mode, always stay on "channels" route (two-pane handles chat)
    // Back from channels in landscape → exit (default behavior when BackHandler disabled)

    NavHost(navController = navController, startDestination = ROUTE_LOGIN) {

        composable(ROUTE_LOGIN) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(ROUTE_CHANNELS) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(ROUTE_CHANNELS) {
            if (isLandscape) {
                // Landscape: two-pane, image navigates on top
                MainScreen(
                    viewModel = mainViewModel,
                    onLogout = {
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onImageClick = { path ->
                        navController.navigate("image/${Uri.encode(path)}")
                    },
                    onNavigateToChat = { /* not used in landscape */ },
                    isPortrait = false,
                )
            } else {
                // Portrait: channel list
                MainScreen(
                    viewModel = mainViewModel,
                    onLogout = {
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onImageClick = { path ->
                        navController.navigate("image/${Uri.encode(path)}")
                    },
                    onNavigateToChat = { channel ->
                        navController.navigate("chat/${Uri.encode(channel)}")
                    },
                    isPortrait = true,
                    showChatInline = false,
                )
            }
        }

        composable(
            route = ROUTE_CHAT,
            arguments = listOf(navArgument("channel") { type = NavType.StringType }),
        ) { backStackEntry ->
            val channel = Uri.decode(backStackEntry.arguments?.getString("channel") ?: "")

            BackHandler {
                mainViewModel.clearSelectedChannel()
                navController.popBackStack()
            }

            // Portrait: chat screen
            MainScreen(
                viewModel = mainViewModel,
                onLogout = {
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onImageClick = { path ->
                    navController.navigate("image/${Uri.encode(path)}")
                },
                onNavigateToChat = { /* not used here */ },
                isPortrait = true,
                showChatInline = true,
                chatChannel = channel,
            )
        }

        composable(
            route = ROUTE_IMAGE,
            arguments = listOf(navArgument("path") { type = NavType.StringType }),
        ) { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
            ImageScreen(
                imagePath = path,
                onClose = { navController.popBackStack() },
            )
        }
    }
}
