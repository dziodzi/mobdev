package io.github.mobdev

data class Contact(
    val id: Long,
    val name: String,
    val phoneNumber: String?,
    val email: String?,
    val birthday: String?,
    val note: String?
)