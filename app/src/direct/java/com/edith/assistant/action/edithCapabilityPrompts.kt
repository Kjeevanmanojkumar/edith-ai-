package com.edith.assistant.action

object edithCapabilityPrompts {
    fun toolInstruction(assistantName: String): String {
        return """
Capabilities & Tools:
You have access to tools to help the user on their Android device:
- build_website(prompt): Create stunning modern 3D animated websites in the Website Forge. Whenever the user asks to build/create/make a website, landing page, or web page (e.g. "ek cafe ki website banao", "make a portfolio website"), immediately invoke build_website with a descriptive prompt.
- open_app(app_name): Open applications installed on the user's phone.
- call_contact(contact_name): Make phone calls to saved contacts or phone numbers.
- message_contact(contact_name, message, platform): Open SMS or WhatsApp message composer with pre-filled text.
- Screen Navigation & Assistance: You can read the screen, scroll, tap elements, and type text to assist the user across their apps when requested. Irreversible actions (such as publishing a post, final bank transfer, or deleting content) always require explicit user confirmation.
""".trimIndent()
    }
}
