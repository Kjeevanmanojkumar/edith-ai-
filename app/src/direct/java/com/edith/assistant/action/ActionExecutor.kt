package com.edith.assistant.action

import android.content.Context
import com.edith.assistant.agent.SocialAgent
import com.edith.assistant.agent.WhatsAppAgent
import kotlinx.coroutines.delay
import org.json.JSONArray
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

                // In direct mode, attempt auto-tapping Send if AccessibilityService is active
                val service = NovaAccessibilityService.instance
                if (service != null && status == "composer_opened") {
                    var sent = false
                    for (tryCount in 0 until 12) {
                        delay(400)
                        if (platform == "whatsapp") {
                            service.tapText("Continue to chat")
                        }
                        if (service.tapText("Send") || service.tapText("भेजें")) {
                            sent = true
                            break
                        }
                    }
                    result.put("status", if (sent) "sent" else "composer_opened_send_not_found")
                } else {
                    result.put("status", status)
                }
                result.put("contact", contactName)
                result.put("platform", platform)
            }

            "read_screen" -> {
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                    result.put("message", "Accessibility permission is required to read screen elements")
                } else {
                    val elements = service.readScreen()
                    result.put("status", "success")
                    val arr = JSONArray()
                    elements.forEach { arr.put(it) }
                    result.put("elements", arr)
                }
            }

            "tap_text" -> {
                val service = NovaAccessibilityService.instance
                val targetText = args.optString("text", "").trim()
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val tapped = service.tapText(targetText)
                    result.put("status", if (tapped) "tapped" else "element_not_found")
                    result.put("target", targetText)
                }
            }

            "type_text" -> {
                val service = NovaAccessibilityService.instance
                val text = args.optString("text", "")
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val typed = service.typeText(text)
                    result.put("status", if (typed) "typed" else "no_focused_field")
                }
            }

            "scroll_screen" -> {
                val service = NovaAccessibilityService.instance
                val direction = args.optString("direction", "down")
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val scrolled = service.scrollScreen(direction)
                    result.put("status", if (scrolled) "scrolled" else "nothing_scrollable")
                }
            }

            "go_back" -> {
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val done = service.performBack()
                    result.put("status", if (done) "done" else "failed")
                }
            }

            "go_home" -> {
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val done = service.performHome()
                    result.put("status", if (done) "done" else "failed")
                }
            }

            "execute_social_media_task" -> {
                return SocialAgent.executeTask(context, args)
            }

            "social_media_task_control" -> {
                return SocialAgent.controlTask(args)
            }

            "execute_whatsapp_task" -> {
                return WhatsAppAgent.executeTask(context, args)
            }

            "whatsapp_task_control" -> {
                return WhatsAppAgent.controlTask(args)
            }

            else -> {
                result.put("error", "Unknown tool: $name")
            }
        }

        return result
    }
}
