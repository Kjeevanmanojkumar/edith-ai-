package com.edith.assistant.voice

import com.edith.assistant.action.edithCapabilityPrompts
import com.edith.assistant.data.LanguagePreference
import com.edith.assistant.data.Personality

object PersonalityPromptBuilder {

    fun build(
        personality: Personality,
        language: LanguagePreference,
        detectedLanguage: DetectedLanguage? = null,
        recap: String? = null,
        assistantName: String = "edith"
    ): String {
        val sections = mutableListOf<String>()

        // (a) Base
        sections.add(
            "You are $assistantName, a persistent personal voice assistant living on the user's phone. Keep replies short and conversational, like spoken conversation."
        )

        // (b) Tool Instructions
        sections.add(edithCapabilityPrompts.toolInstruction(assistantName))

        // (c) Personality instruction
        sections.add(personality.systemInstruction)

        // (d) Language policy
        val langInstruction = when (language) {
            LanguagePreference.AUTO -> {
                "Language Policy: Match the user turn-by-turn. If the user speaks Hindi, reply in Hindi. If the user speaks Hinglish, reply in Hinglish. If the user speaks English, reply in English. Never translate what they said; simply reply naturally in the same language they used."
            }
            LanguagePreference.HINDI -> {
                "Language Policy: Always respond in natural, fluent Hindi (हिन्दी)."
            }
            LanguagePreference.HINGLISH -> {
                "Language Policy: Always respond in natural, conversational Hinglish (Hindi written in Roman/English script)."
            }
            LanguagePreference.ENGLISH -> {
                "Language Policy: Always respond in natural, polished, concise English."
            }
        }
        sections.add(langInstruction)

        // (e) Safety
        sections.add(
            "Safety & Style Guidelines: Never claim to be human or have a physical body. Strictly refuse to generate or engage in sexually explicit content. Immediately respect a request to change tone or topic. Vary your phrasing across turns."
        )

        // (f) Recap (only if non-null and not blank)
        if (!recap.isNullOrBlank()) {
            sections.add("This is a continuing conversation — do not greet again. Recap:\n$recap")
        }

        return sections.joinToString("\n\n")
    }
}
