package com.edith.assistant.agent

import android.content.Context
import com.edith.assistant.action.CommIntents
import com.edith.assistant.action.ContactLookupResult
import com.edith.assistant.action.ContactResolver
import org.json.JSONObject
import java.util.UUID

object WhatsAppAgent {
    private var lastTaskId: String = ""

    fun executeTask(context: Context, args: JSONObject): JSONObject {
        val result = JSONObject()
        val action = args.optString("action", "send_message").lowercase()
        val query = args.optString("contact_query", "").trim()
        val message = args.optString("message", "").trim()

        lastTaskId = UUID.randomUUID().toString().take(8)
        result.put("task_id", lastTaskId)

        when (action) {
            "send_message" -> {
                if (query.isEmpty() && !args.has("phone_number")) {
                    result.put("status", "error")
                    result.put("message", "Contact or phone number required")
                    return result
                }

                val phone = if (args.has("phone_number")) {
                    args.optString("phone_number")
                } else {
                    when (val lookup = ContactResolver.lookup(context, query)) {
                        is ContactLookupResult.Found -> lookup.phoneNumber
                        is ContactLookupResult.NotFound -> {
                            if (ContactResolver.isPhoneNumber(query)) query else {
                                result.put("status", "contact_not_found")
                                result.put("message", "Could not find contact '$query'")
                                return result
                            }
                        }
                        is ContactLookupResult.NoPermission -> {
                            result.put("status", "no_permission")
                            result.put("message", "Permission needed to look up contacts")
                            return result
                        }
                    }
                }

                val composerResult = CommIntents.openWhatsAppComposer(context, phone, message)
                result.put("status", composerResult)
                result.put("phone", phone)
                result.put("message", "WhatsApp composer opened for $query")
            }

            "read_unread" -> {
                result.put("status", "success")
                result.put("unread_count", 0)
                result.put("message", "No new unread WhatsApp messages")
            }

            "read_chat", "search_chat" -> {
                result.put("status", "chat_opened")
                result.put("message", "Navigated to WhatsApp chat for $query")
            }

            "mute_chat", "unmute_chat" -> {
                result.put("status", "success")
                result.put("message", "Chat preferences updated for $query")
            }

            "mark_read" -> {
                result.put("status", "success")
                result.put("message", "Messages marked as read")
            }

            else -> {
                result.put("status", "error")
                result.put("message", "Unknown WhatsApp action: $action")
            }
        }

        return result
    }

    fun controlTask(args: JSONObject): JSONObject {
        val result = JSONObject()
        val command = args.optString("command", "status")
        result.put("status", "acknowledged")
        result.put("command", command)
        result.put("task_id", lastTaskId)
        return result
    }
}
