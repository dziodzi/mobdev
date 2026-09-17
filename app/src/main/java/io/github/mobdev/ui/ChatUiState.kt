package io.github.mobdev.ui

import io.github.mobdev.data.domain.ChatMessage

data class ChatUiState(
    val username: String = "",
    val password: String = "",
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val channels: List<String> = emptyList(),
    val selectedChannel: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val hasMoreMessages: Boolean = true,
    val messageText: String = "",
    val openedImagePath: String? = null,
    val error: UiText? = null,
)
