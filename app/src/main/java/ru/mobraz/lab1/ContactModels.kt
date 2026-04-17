package ru.mobraz.lab1

data class ContactSummary(
    val id: Long,
    val name: String,
)

data class ContactDetails(
    val id: Long,
    val name: String,
    val phoneNumber: String?,
    val email: String?,
)
