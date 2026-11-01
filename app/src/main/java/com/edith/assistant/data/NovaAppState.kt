package com.edith.assistant.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ConversationSummary(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
    val turnsCount: Int = 0
)

class NovaAppState(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var themeMode by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.THEME_MODE, "DARK")
    )
        private set

    var reducedMotion by mutableStateOf(
        NovaPreferences.getBoolean(context, NovaPreferences.Keys.REDUCED_MOTION, false)
    )
        private set

    var orbStyle by mutableStateOf(
        NovaOrbStyle.fromName(NovaPreferences.getString(context, NovaPreferences.Keys.ORB_STYLE, NovaOrbStyle.NOVA.name))
    )
        private set

    var personality by mutableStateOf(
        Personality.fromName(NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name))
    )
        private set

    var assistantName by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME).ifBlank { AssistantBrand.NAME }
    )
        private set

    var userName by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.USER_NAME, "User")
    )
        private set

    var language by mutableStateOf(
        LanguagePreference.fromName(NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name))
    )
        private set

    var voiceName by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede")
    )
        private set

    var speechSpeed by mutableFloatStateOf(
        NovaPreferences.getFloat(context, NovaPreferences.Keys.SPEECH_SPEED, 1.0f)
    )
        private set

    var voiceVolume by mutableFloatStateOf(
        NovaPreferences.getFloat(context, NovaPreferences.Keys.VOICE_VOLUME, 1.0f)
    )
        private set

    var hasOnboarded by mutableStateOf(
        NovaPreferences.getBoolean(context, NovaPreferences.Keys.HAS_ONBOARDED, false)
    )
        private set

    var conversations by mutableStateOf<List<ConversationSummary>>(emptyList())
        private set

    fun applyThemeMode(mode: String) {
        themeMode = mode
        NovaPreferences.putString(context, NovaPreferences.Keys.THEME_MODE, mode)
    }

    fun applyReducedMotion(enabled: Boolean) {
        reducedMotion = enabled
        NovaPreferences.putBoolean(context, NovaPreferences.Keys.REDUCED_MOTION, enabled)
    }

    fun applyOrbStyle(style: NovaOrbStyle) {
        orbStyle = style
        NovaPreferences.putString(context, NovaPreferences.Keys.ORB_STYLE, style.name)
    }

    fun applyPersonality(newPersonality: Personality) {
        personality = newPersonality
        NovaPreferences.putString(context, NovaPreferences.Keys.PERSONALITY, newPersonality.name)
        scope.launch(Dispatchers.IO) {
            PersonalityRepository.getInstance(context).setPersonality(newPersonality)
        }
    }

    fun applyAssistantName(name: String) {
        val clean = name.trim().ifBlank { AssistantBrand.NAME }
        assistantName = clean
        NovaPreferences.putString(context, NovaPreferences.Keys.ASSISTANT_NAME, clean)
        scope.launch(Dispatchers.IO) {
            PersonalityRepository.getInstance(context).setAssistantName(clean)
        }
    }

    fun applyUserName(name: String) {
        val clean = name.trim().ifBlank { "User" }
        userName = clean
        NovaPreferences.putString(context, NovaPreferences.Keys.USER_NAME, clean)
    }

    fun applyLanguage(newLanguage: LanguagePreference) {
        language = newLanguage
        NovaPreferences.putString(context, NovaPreferences.Keys.LANGUAGE, newLanguage.name)
        scope.launch(Dispatchers.IO) {
            PersonalityRepository.getInstance(context).setLanguage(newLanguage)
        }
    }

    fun applyVoiceName(newVoiceName: String) {
        val clean = newVoiceName.trim().ifBlank { "Aoede" }
        voiceName = clean
        NovaPreferences.putString(context, NovaPreferences.Keys.VOICE_NAME, clean)
        scope.launch(Dispatchers.IO) {
            PersonalityRepository.getInstance(context).setVoiceName(clean)
        }
    }

    fun applySpeechSpeed(speed: Float) {
        speechSpeed = speed
        NovaPreferences.putFloat(context, NovaPreferences.Keys.SPEECH_SPEED, speed)
    }

    fun applyVoiceVolume(volume: Float) {
        voiceVolume = volume
        NovaPreferences.putFloat(context, NovaPreferences.Keys.VOICE_VOLUME, volume)
    }

    fun applyHasOnboarded(onboarded: Boolean) {
        hasOnboarded = onboarded
        NovaPreferences.putBoolean(context, NovaPreferences.Keys.HAS_ONBOARDED, onboarded)
    }

    fun addConversation(conversation: ConversationSummary) {
        conversations = listOf(conversation) + conversations
    }

    fun clearConversations() {
        conversations = emptyList()
    }
}
