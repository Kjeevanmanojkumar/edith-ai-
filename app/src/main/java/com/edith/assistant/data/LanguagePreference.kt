package com.edith.assistant.data

enum class LanguagePreference(
    val displayName: String,
    val description: String
) {
    AUTO("Auto Detect", "Follows whatever language you speak seamlessly"),
    HINDI("Hindi", "Hindi (हिन्दी) responses only"),
    HINGLISH("Hinglish", "Natural conversational Hindi written in English script"),
    ENGLISH("English", "Clear and natural English");

    companion object {
        fun fromName(name: String): LanguagePreference {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}
