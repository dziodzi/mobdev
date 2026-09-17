package io.github.mobdev.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageDto(
    val id: Long?,
    val from: String,
    val to: String?,
    val data: MessageDataDto,
    val time: Long?,
)

@JsonClass(generateAdapter = true)
data class MessageDataDto(
    @Json(name = "Text")
    val Text: TextPayloadDto?,
    @Json(name = "Image")
    val Image: ImagePayloadDto?,
)

@JsonClass(generateAdapter = true)
data class TextPayloadDto(
    val text: String,
)

@JsonClass(generateAdapter = true)
data class ImagePayloadDto(
    val link: String?,
)
