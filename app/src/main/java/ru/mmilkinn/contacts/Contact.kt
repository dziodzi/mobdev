package ru.mmilkinn.contacts
 // Знак ? у string значит что значение может быть null
data class Contact(
    val name: String?,
    val phoneNumber: String?,
    val email: String?
)
