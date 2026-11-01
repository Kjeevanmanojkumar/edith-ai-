package com.edith.assistant.action

import android.content.Context
import android.content.Intent
import android.provider.Settings

class NovaAccessibilityService {
    companion object {
        val instance: NovaAccessibilityService? = null
        fun isEnabled(context: Context): Boolean = false
        fun openAccessibilitySettings(context: Context) {
            // No-op in Play flavor
        }
    }
}
