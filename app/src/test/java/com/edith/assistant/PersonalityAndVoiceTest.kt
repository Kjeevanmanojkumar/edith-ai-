package com.edith.assistant

import com.edith.assistant.data.LanguagePreference
import com.edith.assistant.data.Personality
import com.edith.assistant.voice.ConversationMemory
import com.edith.assistant.voice.DetectedLanguage
import com.edith.assistant.voice.LanguageDetector
import com.edith.assistant.voice.MemoryRole
import com.edith.assistant.voice.PersonalityPromptBuilder
import com.edith.assistant.voice.ToolCallDeduplicator
import com.edith.assistant.voice.VoiceLatencyTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityAndVoiceTest {

    @Test
    fun testLanguageDetector() {
        // Devanagari detection -> HINDI
        assertEquals(DetectedLanguage.HINDI, LanguageDetector.detect("नमस्ते, आप कैसे हैं?"))
        assertEquals(DetectedLanguage.HINDI, LanguageDetector.detect("क्या चल रहा है"))

        // Hinglish marker detection
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("kya haal hai yaar"))
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("mujhe ek website chahiye bhai"))

        // English detection
        assertEquals(DetectedLanguage.ENGLISH, LanguageDetector.detect("what's the weather today"))
        assertEquals(DetectedLanguage.ENGLISH, LanguageDetector.detect("build a new modern website"))

        // Blank -> UNKNOWN
        assertEquals(DetectedLanguage.UNKNOWN, LanguageDetector.detect("   "))
        assertEquals(DetectedLanguage.UNKNOWN, LanguageDetector.detect(""))
    }

    @Test
    fun testPersonalityPromptBuilderCombinations() {
        // Verify every combination of Personality x LanguagePreference builds successfully
        for (p in Personality.entries) {
            for (l in LanguagePreference.entries) {
                val prompt = PersonalityPromptBuilder.build(
                    personality = p,
                    language = l,
                    recap = null,
                    assistantName = "edith"
                )
                assertTrue("Prompt must contain assistant name", prompt.contains("edith"))
                assertTrue("Prompt must contain personality instruction", prompt.contains(p.systemInstruction))
                assertFalse("Recap must not appear when null", prompt.contains("Recap:"))
            }
        }
    }

    @Test
    fun testPersonalityPromptBuilderWithRecap() {
        val sampleRecap = "User: Hello\nYou: Hi there!"
        val prompt = PersonalityPromptBuilder.build(
            personality = Personality.FRIENDLY,
            language = LanguagePreference.AUTO,
            recap = sampleRecap,
            assistantName = "edith"
        )
        assertTrue(prompt.contains("This is a continuing conversation — do not greet again."))
        assertTrue(prompt.contains(sampleRecap))
    }

    @Test
    fun testConversationMemoryRollingWindow() {
        val mem = ConversationMemory(maxTurns = 4)
        mem.addTurn(MemoryRole.USER, "Turn 1")
        mem.addTurn(MemoryRole.ASSISTANT, "Turn 2")
        mem.addTurn(MemoryRole.USER, "Turn 3")
        mem.addTurn(MemoryRole.ASSISTANT, "Turn 4")
        mem.addTurn(MemoryRole.USER, "Turn 5")

        val turns = mem.getTurns()
        assertEquals(4, turns.size)
        assertEquals("Turn 2", turns[0].text)
        assertEquals("Turn 5", turns[3].text)

        val recap = mem.recap()
        assertNotNull(recap)
        assertTrue(recap!!.contains("Turn 5"))
        assertFalse(recap.contains("Turn 1"))

        mem.clear()
        assertNull(mem.recap())
    }

    @Test
    fun testToolCallDeduplicator() {
        val deduplicator = ToolCallDeduplicator(maxSize = 10)
        assertTrue(deduplicator.isNew("call_1"))
        assertFalse(deduplicator.isNew("call_1"))
        assertTrue(deduplicator.isNew("call_2"))
        assertFalse(deduplicator.isNew("call_2"))
        assertEquals(2, deduplicator.size())

        deduplicator.clear()
        assertEquals(0, deduplicator.size())
        assertTrue(deduplicator.isNew("call_1"))
    }

    @Test
    fun testVoiceLatencyTracker() {
        val tracker = VoiceLatencyTracker()
        tracker.markUserSpeechEnd(1000L)

        // In-order audio chunk gives valid delta
        val delta = tracker.markFirstAudioChunk(1450L)
        assertEquals(450L, delta)
        assertEquals(450L, tracker.lastTurnLatencyMs.value)

        // Subsequent chunk in same turn should be ignored
        val duplicateDelta = tracker.markFirstAudioChunk(1600L)
        assertEquals(-1L, duplicateDelta)

        // Reconnect counting
        tracker.incrementReconnect()
        assertEquals(1, tracker.reconnectCount.value)
        tracker.resetReconnect()
        assertEquals(0, tracker.reconnectCount.value)
    }
}
