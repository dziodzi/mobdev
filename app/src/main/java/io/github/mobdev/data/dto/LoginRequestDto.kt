package io.github.mobdev.data.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val name: String,
    val pwd: String,
)