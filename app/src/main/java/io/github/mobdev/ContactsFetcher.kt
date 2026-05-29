package io.github.mobdev

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log

private fun Cursor.getStringOrNull(columnIndex: Int): String? {
    if (columnIndex < 0) return null
    return getString(columnIndex)
}

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    Log.d("FETCH", "fetchAllContacts called")

    val emails = HashMap<Long, String>()
    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
        ),
        null,
        null,
        null,
    )?.use { cursor ->
        val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val emailCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            if (!emails.containsKey(id)) {
                emails[id] = cursor.getStringOrNull(emailCol)
            }
        }
    }

    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
    )?.use { cursor ->
        if (cursor == null) return emptyList()
        return buildList {
            val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idCol)
                val name = cursor.getStringOrNull(nameCol)
                val phoneNumber = cursor.getStringOrNull(phoneCol)
                val email = emails[contactId]
                add(Contact(name, phoneNumber, email))
            }
        }
    }

    return emptyList()
}
