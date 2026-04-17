package dev.mobdev.contactlist

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract

@SuppressLint("MissingPermission")
fun Context.fetchAllContacts(): List<Contact> {
    val emailsByContactId = loadFirstEmailPerContact()
    val result = ArrayList<Contact>()
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
    ).use { cursor ->
        if (cursor == null) return emptyList()
        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        while (cursor.moveToNext()) {
            val name = cursor.readString(nameIdx)
            val phoneNumber = cursor.readString(numberIdx)
            val contactId = if (contactIdIdx >= 0) cursor.getLong(contactIdIdx) else -1L
            val email = if (contactId >= 0) emailsByContactId[contactId] else null
            result.add(Contact(name, phoneNumber, email))
        }
    }
    return result
}

private fun Context.loadFirstEmailPerContact(): Map<Long, String> {
    val map = LinkedHashMap<Long, String>()
    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
        ),
        null,
        null,
        null,
    ).use { cursor ->
        if (cursor == null) return map
        val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val addressIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (cursor.moveToNext()) {
            if (idIdx < 0 || addressIdx < 0) continue
            val id = cursor.getLong(idIdx)
            val address = cursor.readString(addressIdx) ?: continue
            if (!map.containsKey(id)) {
                map[id] = address
            }
        }
    }
    return map
}

private fun Cursor.readString(columnIndex: Int): String? {
    if (columnIndex < 0) return null
    return getString(columnIndex)
}
