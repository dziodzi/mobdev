package com.alirzw.lab2.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.alirzw.lab2.data.models.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun fetchAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, Contact>()

        // Fetch phones
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                ).let { cursor.getString(it) }

                val name = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                ).let { cursor.getString(it) }

                val phone = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ).let { cursor.getString(it) }

                contactsMap[id] = Contact(
                    id = id,
                    name = name,
                    phoneNumber = phone,
                    email = null
                )
            }
        }

        // Fetch emails
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null, null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID
                ).let { cursor.getString(it) }

                val email = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ).let { cursor.getString(it) }

                contactsMap[id] = contactsMap[id]?.copy(email = email) as Contact
            }
        }

        return@withContext contactsMap.values.toList()
    }

    suspend fun insertContact(name: String, phone: String, email: String?) =
        withContext(Dispatchers.IO) {
            val ops = ArrayList<android.content.ContentProviderOperation>()
            val rawContactInsertIndex = ops.size

            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Add name
            ops.add(
                android.content.ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI
                )
                    .withValueBackReference(
                        ContactsContract.Data.RAW_CONTACT_ID,
                        rawContactInsertIndex
                    )
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // Add phone
            ops.add(
                android.content.ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI
                )
                    .withValueBackReference(
                        ContactsContract.Data.RAW_CONTACT_ID,
                        rawContactInsertIndex
                    )
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        phone
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            // Add email if present
            if (!email.isNullOrBlank()) {
                ops.add(
                    android.content.ContentProviderOperation.newInsert(
                        ContactsContract.Data.CONTENT_URI
                    )
                        .withValueBackReference(
                            ContactsContract.Data.RAW_CONTACT_ID,
                            rawContactInsertIndex
                        )
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.ADDRESS,
                            email
                        )
                        .build()
                )
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        }

    suspend fun deleteContact(contactId: String) = withContext(Dispatchers.IO) {
        val uri = ContactsContract.RawContacts.CONTENT_URI
        val where = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
        context.contentResolver.delete(uri, where, arrayOf(contactId))
    }

    suspend fun updateContact(contact: Contact) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // Get raw contact ID
        val rawContactId = resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contact.id),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: return@withContext

        updateContactName(resolver, rawContactId, contact.name)
        updateContactPhone(resolver, rawContactId, contact.phoneNumber)
        updateContactEmail(resolver, rawContactId, contact.email)
    }

    private fun updateContactName(
        resolver: android.content.ContentResolver,
        rawContactId: String,
        name: String?
    ) {
        val where =
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
        val params =
            arrayOf(rawContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

        if (isDataExists(resolver, where, params)) {
            if (!name.isNullOrBlank()) {
                val values = ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                }
                resolver.update(ContactsContract.Data.CONTENT_URI, values, where, params)
            }
        } else if (!name.isNullOrBlank()) {
            val values = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            }
            resolver.insert(ContactsContract.Data.CONTENT_URI, values)
        }
    }

    private fun updateContactPhone(
        resolver: android.content.ContentResolver,
        rawContactId: String,
        phone: String?
    ) {
        val where =
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
        val params = arrayOf(rawContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)

        if (isDataExists(resolver, where, params)) {
            if (!phone.isNullOrBlank()) {
                val values = ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                }
                resolver.update(ContactsContract.Data.CONTENT_URI, values, where, params)
            }
        } else if (!phone.isNullOrBlank()) {
            val values = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                put(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                )
            }
            resolver.insert(ContactsContract.Data.CONTENT_URI, values)
        }
    }

    private fun updateContactEmail(
        resolver: android.content.ContentResolver,
        rawContactId: String,
        email: String?
    ) {
        val where =
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
        val params = arrayOf(rawContactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)

        if (isDataExists(resolver, where, params)) {
            if (!email.isNullOrBlank()) {
                val values = ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                }
                resolver.update(ContactsContract.Data.CONTENT_URI, values, where, params)
            }
        } else if (!email.isNullOrBlank()) {
            val values = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                )
                put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                put(
                    ContactsContract.CommonDataKinds.Email.TYPE,
                    ContactsContract.CommonDataKinds.Email.TYPE_HOME
                )
            }
            resolver.insert(ContactsContract.Data.CONTENT_URI, values)
        }
    }

    private fun isDataExists(
        resolver: android.content.ContentResolver,
        where: String,
        params: Array<String>
    ): Boolean {
        return resolver.query(ContactsContract.Data.CONTENT_URI, null, where, params, null)
            ?.use { cursor ->
                cursor.count > 0
            } ?: false
    }
}