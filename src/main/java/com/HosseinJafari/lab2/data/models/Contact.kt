package com.alirzw.lab2.data.models

data class Contact(
    val id: String,
    val name: String?,
    val phoneNumber: String?,
    val email: String?,
    val isFavorite: Boolean = false
)