package io.github.mobdev

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import androidx.core.database.getStringOrNull

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    Log.d("FETCH", "fetchAllContacts called")
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    ).use { cursor ->
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
