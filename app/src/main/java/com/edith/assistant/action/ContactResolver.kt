package com.edith.assistant.action

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

sealed class ContactLookupResult {
    data class Found(val name: String, val phoneNumber: String) : ContactLookupResult()
    data object NoPermission : ContactLookupResult()
    data object NotFound : ContactLookupResult()
}

object ContactResolver {

    fun isPhoneNumber(query: String): Boolean {
        val digitsOnly = query.filter { it.isDigit() }
        val validCharsOnly = query.all { it.isDigit() || it in "+-(). " }
        return validCharsOnly && digitsOnly.length >= 5
    }

    fun lookup(context: Context, query: String): ContactLookupResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ContactLookupResult.NotFound

        // If query looks like a phone number, return directly
        if (isPhoneNumber(trimmed)) {
            val normalized = trimmed.filter { it.isDigit() || it == '+' }
            return ContactLookupResult.Found(name = trimmed, phoneNumber = normalized)
        }

        // Check READ_CONTACTS permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return ContactLookupResult.NoPermission
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$trimmed%")

        val matches = mutableListOf<Pair<String, String>>() // name to number

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 5"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx) ?: ""
                    val number = it.getString(numIdx) ?: ""
                    if (name.isNotEmpty() && number.isNotEmpty()) {
                        matches.add(name to number)
                    }
                }
            }
        } catch (_: Exception) {
            return ContactLookupResult.NotFound
        }

        if (matches.isEmpty()) {
            return ContactLookupResult.NotFound
        }

        // Rank exact > prefix > contains
        val queryLower = trimmed.lowercase()
        val sorted = matches.sortedBy { (name, _) ->
            val nameLower = name.lowercase()
            when {
                nameLower == queryLower -> 0
                nameLower.startsWith(queryLower) -> 1
                else -> 2
            }
        }

        val best = sorted.first()
        return ContactLookupResult.Found(name = best.first, phoneNumber = best.second)
    }
}
