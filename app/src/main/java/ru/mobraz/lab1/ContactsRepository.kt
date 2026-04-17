package ru.mobraz.lab1

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract

class ContactsRepository(private val context: Context) {

    @SuppressLint("Range")
    fun fetchAllContactSummaries(): List<ContactSummary> {
        val results = mutableListOf<ContactSummary>()
        val seenIds = HashSet<Long>()

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ),
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

            while (cursor.moveToNext()) {
                val id = if (idIndex >= 0) cursor.getLong(idIndex) else continue
                if (!seenIds.add(id)) continue

                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                results.add(
                    ContactSummary(
                        id = id,
                        name = name?.takeIf { it.isNotBlank() } ?: "—",
                    )
                )
            }
        }

        return results
    }

    @SuppressLint("Range")
    fun fetchContactDetails(contactId: Long): ContactDetails? {
        var name: String? = null
        var phone: String? = null
        var email: String? = null

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ),
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } else {
                return null
            }
        }

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                phone = if (phoneIndex >= 0) cursor.getString(phoneIndex) else null
            }
        }

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                email = if (emailIndex >= 0) cursor.getString(emailIndex) else null
            }
        }

        return ContactDetails(
            id = contactId,
            name = name?.takeIf { it.isNotBlank() } ?: "—",
            phoneNumber = phone,
            email = email,
        )
    }
}
