package io.github.mobdev.data

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.database.getStringOrNull

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        ),
        null, null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    ).use { cursor: Cursor? ->
        if (cursor == null) return emptyList()
        return buildList {
            while (cursor.moveToNext()) {
                val name = cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                )
                val phoneNumber = cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                val email = cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                )
                add(Contact(name, phoneNumber, email))
            }
        }
    }
}
