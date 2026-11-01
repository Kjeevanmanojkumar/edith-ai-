package com.edith.assistant.action

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.net.URLEncoder

object CommIntents {

    fun placeCall(context: Context, phoneNumber: String): String {
        val cleanNumber = phoneNumber.trim()
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            if (hasCallPermission) "calling" else "dialer_opened_no_permission"
        } catch (_: Exception) {
            "failed_to_open_dialer"
        }
    }

    fun openSmsComposer(context: Context, phoneNumber: String, message: String): String {
        val cleanNumber = phoneNumber.trim()
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "composer_opened"
        } catch (_: Exception) {
            "failed_to_open_sms"
        }
    }

    fun openWhatsAppComposer(context: Context, phoneNumber: String, message: String): String {
        val digits = phoneNumber.filter { it.isDigit() }
        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (_: Exception) {
            message
        }
        val uri = if (digits.isNotEmpty()) {
            Uri.parse("https://wa.me/$digits?text=$encodedMessage")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Check if WhatsApp or WhatsApp Business is installed
        val pm = context.packageManager
        val waInstalled = isPackageInstalled(pm, "com.whatsapp")
        val wbInstalled = isPackageInstalled(pm, "com.whatsapp.w4b")

        if (waInstalled) {
            intent.setPackage("com.whatsapp")
        } else if (wbInstalled) {
            intent.setPackage("com.whatsapp.w4b")
        }

        return try {
            context.startActivity(intent)
            "composer_opened"
        } catch (_: Exception) {
            // Fallback to opening regular SMS
            openSmsComposer(context, phoneNumber, message)
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
