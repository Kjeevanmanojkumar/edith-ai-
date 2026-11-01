package com.edith.assistant.voice

enum class DetectedLanguage {
    HINDI,
    HINGLISH,
    ENGLISH,
    UNKNOWN
}

object LanguageDetector {
    private val HINGLISH_MARKERS = setOf(
        "hai", "hain", "ho", "kya", "kyun", "kaise", "nahi", "haan",
        "mujhe", "tumhe", "aap", "mera", "meri", "mere", "yaar", "bhai",
        "accha", "acha", "theek", "matlab", "karna", "karo", "raha", "rahi",
        "rahe", "chahiye", "batao", "suno", "dekho", "abhi", "phir", "lekin",
        "bahut", "thoda", "kuch", "sab", "wala", "wali", "wale", "kahan",
        "kab", "hum", "tum", "kaun", "aisa", "waise", "hoga", "hogi", "dena",
        "bolo", "samjha", "samjhe", "namaste", "shukriya", "dost"
    )

    fun detect(text: String): DetectedLanguage {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return DetectedLanguage.UNKNOWN

        // Check for Devanagari Unicode block (U+0900..U+097F)
        for (char in trimmed) {
            if (char.code in 0x0900..0x097F) {
                return DetectedLanguage.HINDI
            }
        }

        // Split on non-alphabetic chars
        val words = trimmed.lowercase().split(Regex("[^a-zA-Z']+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return DetectedLanguage.UNKNOWN

        var hits = 0
        for (w in words) {
            if (HINGLISH_MARKERS.contains(w)) {
                hits++
            }
        }

        val totalWords = words.size
        return if (totalWords <= 4) {
            if (hits >= 1) DetectedLanguage.HINGLISH else DetectedLanguage.ENGLISH
        } else {
            val ratio = hits.toDouble() / totalWords
            if (hits >= 2 || ratio >= 0.15) {
                DetectedLanguage.HINGLISH
            } else {
                DetectedLanguage.ENGLISH
            }
        }
    }
}
