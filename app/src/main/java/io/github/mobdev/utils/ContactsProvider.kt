package io.github.mobdev.utils

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import io.github.mobdev.data.Contact

private val contactProjection = arrayOf(
    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
    ContactsContract.CommonDataKinds.Phone.NUMBER,
    ContactsContract.CommonDataKinds.Email.ADDRESS,
)

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        contactProjection,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    ).use { cursor: Cursor? ->
        if (cursor == null) return emptyList()

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    Contact(
                        name = cursor.getStringOrNull(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                        phoneNumber = cursor.getStringOrNull(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        email = cursor.getStringOrNull(ContactsContract.CommonDataKinds.Email.ADDRESS),
                    ),
                )
            }
        }
    }
}

private fun Cursor.getStringOrNull(columnName: String): String? {
    val columnIndex = getColumnIndex(columnName)
    return if (columnIndex >= 0) getString(columnIndex) else null
}