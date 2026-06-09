package com.helpmethen.chat.data.model

data class MessageDto(
    val id: String,
    val from: String,
    val to: String,
    val data: MessageDataDto,
    val time: String
)

data class MessageDataDto(
    val Text: TextDto? = null,
    val Image: ImageDto? = null
)

data class TextDto(
    val text: String
)

data class ImageDto(
    val link: String?
)
