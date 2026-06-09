package com.helpmethen.chat.data.model

data class SendMessageDto(
    val from: String,
    val to: String,
    val data: MessageDataDto
)
