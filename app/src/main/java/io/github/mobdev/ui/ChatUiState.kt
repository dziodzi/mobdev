package io.github.mobdev.ui

enum class AppScreen {
    LOGIN,
    CHATS,
    MESSAGES,
    IMAGE,
}

data class ChatUiState(
    val screen: AppScreen = AppScreen.LOGIN,
    val username: String = "",
    val password: String = "",
    val channels: List<String> = emptyList(),
    val selectedChannel: String? = null,
    val messages: List<io.github.mobdev.data.ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val imageLink: String? = null,
    val errorDialogMessage: String? = null,
    val skipLoginScreen: Boolean = false,
    val channelsLoaded: Boolean = false,
    val messagesLoadedForChannel: String? = null,
)
