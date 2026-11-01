package com.edith.assistant.action

import android.content.Context
import org.json.JSONObject

object ActionExecutor {

    suspend fun execute(context: Context, name: String, args: JSONObject): JSONObject {
        val result = JSONObject()

        when (name) {
            "open_app" -> {
                val appName = args.optString("app_name", "").trim()
                if (appName.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "App name cannot be empty")
                    return result
                }
                val pkg = AppLauncher.findApp(context, appName)
                if (pkg != null) {
                    val launched = AppLauncher.launch(context, pkg)
                    result.put("status", if (launched) "success" else "error")
                    result.put("message", if (launched) "Opened $appName" else "Failed to launch $appName")
                    result.put("package", pkg)
                } else {
                    result.put("status", "not_found")
                    result.put("message", "Could not find app '$appName' on your device")
                }
            }

            "call_contact" -> {
                val query = args.optString("contact_name", "").trim()
                if (query.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "Contact query cannot be empty")
                    return result
                }

                when (val lookup = ContactResolver.lookup(context, query)) {
                    is ContactLookupResult.Found -> {
                        val callStatus = CommIntents.placeCall(context, lookup.phoneNumber)
                        result.put("status", callStatus)
                        result.put("contact_name", lookup.name)
                        result.put("phone_number", lookup.phoneNumber)
                    }
                    is ContactLookupResult.NoPermission -> {
                        result.put("status", "no_permission")
                        result.put("message", "Contacts permission is required to search saved contacts")
                    }
                    is ContactLookupResult.NotFound -> {
                        result.put("status", "not_found")
                        result.put("message", "Could not find contact '$query'")
                    }
                }
            }

            "message_contact" -> {
                val contactName = args.optString("contact_name", "").trim()
                val message = args.optString("message", "").trim()
                val platform = args.optString("platform", "whatsapp").lowercase()

                val phone = when (val lookup = ContactResolver.lookup(context, contactName)) {
                    is ContactLookupResult.Found -> lookup.phoneNumber
                    else -> contactName
                }

                val status = if (platform == "sms") {
                    CommIntents.openSmsComposer(context, phone, message)
                } else {
                    CommIntents.openWhatsAppComposer(context, phone, message)
                }

                result.put("status", status)
                result.put("contact", contactName)
                result.put("platform", platform)
            }

            else -> {
                result.put("error", "Unknown tool: $name")
            }
        }

        return result
    }
}
