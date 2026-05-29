package io.github.mobdev

import java.io.Serializable

data class Contact(
    val name: String?,
    val phoneNumber: String?,
    val email: String?,
) : Serializable
