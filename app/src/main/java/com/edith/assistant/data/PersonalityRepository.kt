package com.edith.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.edith.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_settings")

class PersonalityRepository private constructor(private val context: Context) {

    private object PreferencesKeys {
        val PERSONALITY = stringPreferencesKey("personality")
        val LANGUAGE = stringPreferencesKey("language")
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val VOICE_NAME = stringPreferencesKey("voice_name")
    }

    val personalityFlow: Flow<Personality> = context.dataStore.data.map { prefs ->
        val saved = prefs[PreferencesKeys.PERSONALITY]
            ?: NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name)
        Personality.fromName(saved)
    }

    val languageFlow: Flow<LanguagePreference> = context.dataStore.data.map { prefs ->
        val saved = prefs[PreferencesKeys.LANGUAGE]
            ?: NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name)
        LanguagePreference.fromName(saved)
    }

    val assistantNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val saved = prefs[PreferencesKeys.ASSISTANT_NAME]
            ?: NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
        saved.ifBlank { AssistantBrand.NAME }
    }

    val voiceNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val saved = prefs[PreferencesKeys.VOICE_NAME]
            ?: NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede")
        saved.ifBlank { "Aoede" }
    }

    suspend fun getPersonality(): Personality {
        return try {
            personalityFlow.first()
        } catch (_: Exception) {
            val fallback = NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name)
            Personality.fromName(fallback)
        }
    }

    suspend fun getLanguage(): LanguagePreference {
        return try {
            languageFlow.first()
        } catch (_: Exception) {
            val fallback = NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name)
            LanguagePreference.fromName(fallback)
        }
    }

    suspend fun getAssistantName(): String {
        return try {
            assistantNameFlow.first()
        } catch (_: Exception) {
            val fallback = NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
            fallback.ifBlank { AssistantBrand.NAME }
        }
    }

    suspend fun getVoiceName(): String {
        return try {
            voiceNameFlow.first()
        } catch (_: Exception) {
            val fallback = NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede")
            fallback.ifBlank { "Aoede" }
        }
    }

    suspend fun setPersonality(personality: Personality) {
        NovaPreferences.putString(context, NovaPreferences.Keys.PERSONALITY, personality.name)
        try {
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.PERSONALITY] = personality.name
            }
        } catch (_: Exception) {}
        VoiceSessionManager.refreshInstructions(context)
    }

    suspend fun setLanguage(language: LanguagePreference) {
        NovaPreferences.putString(context, NovaPreferences.Keys.LANGUAGE, language.name)
        try {
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.LANGUAGE] = language.name
            }
        } catch (_: Exception) {}
        VoiceSessionManager.refreshInstructions(context)
    }

    suspend fun setAssistantName(name: String) {
        val cleanName = name.trim().ifBlank { AssistantBrand.NAME }
        NovaPreferences.putString(context, NovaPreferences.Keys.ASSISTANT_NAME, cleanName)
        try {
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.ASSISTANT_NAME] = cleanName
            }
        } catch (_: Exception) {}
        VoiceSessionManager.refreshInstructions(context)
    }

    suspend fun setVoiceName(voice: String) {
        val cleanVoice = voice.trim().ifBlank { "Aoede" }
        NovaPreferences.putString(context, NovaPreferences.Keys.VOICE_NAME, cleanVoice)
        try {
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.VOICE_NAME] = cleanVoice
            }
        } catch (_: Exception) {}
        VoiceSessionManager.refreshInstructions(context)
    }

    companion object {
        @Volatile
        private var instance: PersonalityRepository? = null

        fun getInstance(context: Context): PersonalityRepository {
            return instance ?: synchronized(this) {
                instance ?: PersonalityRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
