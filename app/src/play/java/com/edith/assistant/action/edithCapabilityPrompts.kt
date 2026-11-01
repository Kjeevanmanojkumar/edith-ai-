package com.edith.assistant.action

object edithCapabilityPrompts {
    fun toolInstruction(assistantName: String): String {
        return """
Capabilities & Tools:
You have access to tools to help the user on their Android device:
- build_website(prompt): Create modern 3D animated websites in the Website Forge.
- open_app(app_name): Open applications installed on the user's phone.
- call_contact(contact_name): Make phone calls to saved contacts or phone numbers.
- message_contact(contact_name, message, platform): Open SMS or WhatsApp composer with pre-filled text so the user can tap Send.
Note: In this Play edition, $assistantName cannot read or control the screen and cannot automatically post to social apps.
""".trimIndent()
    }
}
