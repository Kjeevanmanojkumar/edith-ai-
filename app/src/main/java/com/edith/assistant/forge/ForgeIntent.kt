package com.edith.assistant.forge

object ForgeIntent {
    private val WEBSITE_NOUNS = setOf(
        "website", "web page", "webpage", "landing page", "landingpage",
        "site", "web app", "webapp", "portfolio", "storefront"
    )

    private val BUILD_VERBS = setOf(
        "build", "create", "make", "generate", "code", "design", "develop",
        "banao", "bana", "banaye", "banado", "kholo", "ready", "craft", "forge"
    )

    private val QUESTION_WORDS = setOf(
        "how", "what", "why", "when", "where", "can you explain", "kya", "kyun", "kaise"
    )

    fun extract(text: String): String? {
        val lower = text.lowercase().trim()
        if (lower.isEmpty()) return null

        // Ignore pure questions about websites
        if (lower.endsWith("?") && QUESTION_WORDS.any { lower.startsWith(it) }) {
            return null
        }

        val hasNoun = WEBSITE_NOUNS.any { lower.contains(it) }
        val hasVerb = BUILD_VERBS.any { lower.contains(it) }

        if (!hasNoun || !hasVerb) return null

        // Return extracted prompt
        return text.trim()
    }
}
