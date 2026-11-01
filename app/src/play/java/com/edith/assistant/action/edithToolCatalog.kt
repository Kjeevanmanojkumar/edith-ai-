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
                description = "Open the 3D Website Forge and stream a live modern animated 3D website based on the user's prompt.",
                properties = mapOf("prompt" to "Detailed description of the website to generate"),
                required = listOf("prompt")
            )
        )

        // 2. Open App
        list.put(
            functionDecl(
                name = "open_app",
                description = "Launch an application installed on the user's Android device by its name.",
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
                description = "Open the SMS or WhatsApp composer with pre-filled text so the user can tap Send.",
                properties = mapOf(
                    "contact_name" to "Name or phone number of the recipient",
                    "message" to "Text content of the message",
                    "platform" to "Platform to use: 'whatsapp' or 'sms'"
                ),
                required = listOf("contact_name", "message")
            )
        )

        return list
    }
}
