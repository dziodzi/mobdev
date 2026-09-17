package io.github.mobdev.data.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OutgoingMessageDto(
    val from: String,
    val to: String,
    val data: MessageDataDto,
)