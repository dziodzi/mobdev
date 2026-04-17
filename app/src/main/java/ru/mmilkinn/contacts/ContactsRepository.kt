package ru.mmilkinn.contacts

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.database.getStringOrNull


@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    Log.d("FETCH", "fetchAllContacts called")

    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    ).use { cursor: Cursor? ->
        if (cursor == null) return emptyList()

        return buildList {
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                )
                val name = cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                )
                val phoneNumber = cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                val email = fetchEmailByContactId(contactId)

                add(Contact(name, phoneNumber, email))
            }
        }
    }
}

@SuppressLint("Range")
private fun Context.fetchEmailByContactId(contactId: Long): String? {
    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    ).use { cursor: Cursor? ->
        if (cursor == null || !cursor.moveToFirst()) return null

        return cursor.getStringOrNull(
            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        )
    }
}
