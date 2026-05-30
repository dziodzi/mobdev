package io.github.mobdev

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactsRepository {

    @SuppressLint("InlinedApi")
    suspend fun loadContacts(contentResolver: ContentResolver): List<Contact> =
        withContext(Dispatchers.IO) {
            val contactsMap = mutableMapOf<Long, MutableContact>()

            val phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )

            phoneCursor?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: "Без имени"
                    val phone = cursor.getString(phoneIdx)

                    if (!contactsMap.containsKey(id)) {
                        contactsMap[id] = MutableContact(id, name, phone, null, null, null)
                    }
                }
            }

            val emailCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                null, null, null
            )

            emailCursor?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val email = cursor.getString(emailIdx)
                    contactsMap[id]?.email = email
                }
            }

            val birthdayCursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Event.START_DATE
                ),
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?",
                arrayOf(
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
                ),
                null
            )

            birthdayCursor?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val dateIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val birthday = cursor.getString(dateIdx)
                    if (!birthday.isNullOrEmpty()) {
                        contactsMap[id]?.birthday = birthday
                    }
                }
            }

            val noteCursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Note.NOTE
                ),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
                null
            )

            noteCursor?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val noteIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val note = cursor.getString(noteIdx)
                    if (!note.isNullOrEmpty()) {
                        contactsMap[id]?.note = note
                    }
                }
            }

            return@withContext contactsMap.values.map { it.toContact() }.sortedBy { it.name }
        }

    private data class MutableContact(
        val id: Long,
        val name: String,
        var phoneNumber: String?,
        var email: String?,
        var birthday: String?,
        var note: String?
    ) {
        fun toContact() = Contact(id, name, phoneNumber, email, birthday, note)
    }
}