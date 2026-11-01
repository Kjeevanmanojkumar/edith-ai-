package com.edith.assistant.data

import android.content.Context
import android.content.SharedPreferences

object NovaPreferences {
    private const val PREFS_NAME = "nova_prefs"

    object Keys {
        const val THEME_MODE = "theme_mode"
        const val REDUCED_MOTION = "reduced_motion"
        const val ORB_STYLE = "orb_style"
        const val PERSONALITY = "personality"
        const val USER_NAME = "user_name"
        const val VOICE_INPUT_ENABLED = "voice_input_enabled"
        const val WAKE_WORD_ENABLED = "wake_word_enabled"
        const val LANGUAGE = "language"
        const val HAS_ONBOARDED = "has_onboarded"
        const val VOICE_NAME = "voice_name"
        const val RESPONSE_STYLE = "response_style"
        const val SPEECH_SPEED = "speech_speed"
        const val VOICE_VOLUME = "voice_volume"
        const val ASSISTANT_NAME = "assistant_name"
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getString(context: Context, key: String, default: String = ""): String {
        return getPrefs(context).getString(key, default) ?: default
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
        return getPrefs(context).getBoolean(key, default)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getFloat(context: Context, key: String, default: Float = 1.0f): Float {
        return getPrefs(context).getFloat(key, default)
    }

    fun putFloat(context: Context, key: String, value: Float) {
        getPrefs(context).edit().putFloat(key, value).apply()
    }
}
