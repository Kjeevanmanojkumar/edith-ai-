package com.edith.assistant.action

import org.json.JSONArray
import org.json.JSONObject

object edithToolCatalog {

    fun functionDecl(
        name: String,
        description: String,
        properties: Map<String, String>,
        required: List<String>
    ): JSONObject {
        val decl = JSONObject()
        decl.put("name", name)
        decl.put("description", description)

        val parameters = JSONObject()
        parameters.put("type", "OBJECT")

        val props = JSONObject()
        for ((propName, propDesc) in properties) {
            val field = JSONObject()
            field.put("type", "STRING")
            field.put("description", propDesc)
            props.put(propName, field)
        }
        parameters.put("properties", props)

        val req = JSONArray()
        for (r in required) {
            req.put(r)
        }
        parameters.put("required", req)

        decl.put("parameters", parameters)
        return decl
    }

    fun declarations(): JSONArray {
        val list = JSONArray()

        // 1. Website Forge
        list.put(
            functionDecl(
                name = "build_website",
                description = "Open the 3D Website Forge and stream a live modern animated 3D website based on the user's prompt (e.g. cafe, portfolio, landing page).",
                properties = mapOf("prompt" to "Detailed description of the website to generate"),
                required = listOf("prompt")
            )
        )

        // 2. Open App
        list.put(
            functionDecl(
                name = "open_app",
                description = "Launch an application installed on the user's Android device by its name (e.g. YouTube, Spotify, WhatsApp, Settings).",
                properties = mapOf("app_name" to "Name of the app to launch"),
                required = listOf("app_name")
            )
        )

        // 3. Call Contact
        list.put(
            functionDecl(
                name = "call_contact",
                description = "Initiate a phone call to a saved contact name or direct phone number.",
                properties = mapOf("contact_name" to "Name of the contact or phone number to call"),
                required = listOf("contact_name")
            )
        )

        // 4. Message Contact
        list.put(
            functionDecl(
                name = "message_contact",
                description = "Prepare or send a message to a contact via SMS or WhatsApp with pre-filled text.",
                properties = mapOf(
                    "contact_name" to "Name or phone number of the recipient",
                    "message" to "Text content of the message",
                    "platform" to "Platform to use: 'whatsapp' or 'sms'"
                ),
                required = listOf("contact_name", "message")
            )
        )

        // 5. Read Screen
        list.put(
            functionDecl(
                name = "read_screen",
                description = "Read visible interactive elements, titles, and text on the current active screen.",
                properties = emptyMap(),
                required = emptyList()
            )
        )

        // 6. Tap Text
        list.put(
            functionDecl(
                name = "tap_text",
                description = "Tap an interactive button, tab, or element on screen matching the given text label.",
                properties = mapOf("text" to "Text or description of the UI element to tap"),
                required = listOf("text")
            )
        )

        // 7. Type Text
        list.put(
            functionDecl(
                name = "type_text",
                description = "Type text into the currently focused or active input field on screen.",
                properties = mapOf("text" to "Text to type into the active field"),
                required = listOf("text")
            )
        )

        // 8. Scroll Screen
        list.put(
            functionDecl(
                name = "scroll_screen",
                description = "Scroll the current view up, down, left, or right.",
                properties = mapOf("direction" to "Direction to scroll: 'down', 'up', 'left', or 'right'"),
                required = listOf("direction")
            )
        )

        // 9. Go Back
        list.put(
            functionDecl(
                name = "go_back",
                description = "Trigger the Android back button navigation.",
                properties = emptyMap(),
                required = emptyList()
            )
        )

        // 10. Go Home
        list.put(
            functionDecl(
                name = "go_home",
                description = "Navigate back to the device Home screen.",
                properties = emptyMap(),
                required = emptyList()
            )
        )

        // 11. Execute Social Media Task
        list.put(
            functionDecl(
                name = "execute_social_media_task",
                description = "Create a post, story, or reel on Instagram or Facebook. In PUBLISH mode, always pauses for explicit user confirmation before final posting.",
                properties = mapOf(
                    "platform" to "'instagram' or 'facebook'",
                    "action" to "'create_post', 'create_reel', 'create_story', or 'text_post'",
                    "media_uri" to "Content URI of the photo or video if shared",
                    "caption" to "Caption text for the post",
                    "mode" to "'draft' or 'publish'"
                ),
                required = listOf("platform", "action")
            )
        )

        // 12. Social Media Task Control
        list.put(
            functionDecl(
                name = "social_media_task_control",
                description = "Control a social media agent task. Only call 'confirm' after the user has explicitly spoken or typed yes.",
                properties = mapOf(
                    "command" to "'confirm', 'reject', 'resume', 'cancel', or 'status'",
                    "task_id" to "The active task ID"
                ),
                required = listOf("command")
            )
        )

        // 13. Execute WhatsApp Task
        list.put(
            functionDecl(
                name = "execute_whatsapp_task",
                description = "Perform a WhatsApp action such as reading unread notifications or opening a chat.",
                properties = mapOf(
                    "action" to "'send_message', 'read_unread', 'read_chat', 'search_chat'",
                    "contact_query" to "Contact name or phone number",
                    "message" to "Message body to send"
                ),
                required = listOf("action")
            )
        )

        // 14. WhatsApp Task Control
        list.put(
            functionDecl(
                name = "whatsapp_task_control",
                description = "Control or confirm a pending WhatsApp task.",
                properties = mapOf(
                    "command" to "'confirm', 'cancel', 'status'",
                    "task_id" to "Active task ID"
                ),
                required = listOf("command")
            )
        )

        return list
    }
}
