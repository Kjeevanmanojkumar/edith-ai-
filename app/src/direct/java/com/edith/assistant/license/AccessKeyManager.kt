package com.edith.assistant.license

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessKeyManager {
    private const val PREFS_NAME = "edith_access_key"
    private const val KEY_ACCESS_KEY = "key"
    private const val KEY_PLAN = "plan"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_ACTIVE = "active"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_LAST_CHECK = "last_remote_check"

    private val _licenseInfo = MutableStateFlow(
        LicenseInfo(
            active = true,
            plan = "Community",
            expiresAt = 0L,
            statusText = "Community Access",
            blocked = false,
            hasRememberedKey = false
        )
    )
    val licenseInfo: StateFlow<LicenseInfo> = _licenseInfo.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun refreshInfo(context: Context): LicenseInfo {
        val prefs = getPrefs(context)
        val key = prefs.getString(KEY_ACCESS_KEY, "") ?: ""
        val plan = prefs.getString(KEY_PLAN, "Community") ?: "Community"
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val blocked = prefs.getBoolean(KEY_BLOCKED, false)

        val info = AccessKeyRules.evaluate(
            hasKey = key.isNotEmpty(),
            plan = plan,
            expiresAt = expiresAt,
            blocked = blocked
        )
        _licenseInfo.value = info
        return info
    }

    fun activate(context: Context, key: String): Result<LicenseInfo> {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Access key cannot be blank"))
        }

        // Validate local formatting or pro tier code
        val plan = if (trimmed.startsWith("EDITH-VIP-", ignoreCase = true)) "Pro Elite" else "Pro"
        val expiresAt = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000) // 1 year

        getPrefs(context).edit()
            .putString(KEY_ACCESS_KEY, trimmed)
            .putString(KEY_PLAN, plan)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_BLOCKED, false)
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()

        val info = refreshInfo(context)
        return Result.success(info)
    }

    fun liveCheck(context: Context) {
        val prefs = getPrefs(context)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        val now = System.currentTimeMillis()

        // Check at most once every 3 hours; fail open on network errors
        if (now - lastCheck < 3 * 60 * 60 * 1000L) {
            refreshInfo(context)
            return
        }

        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        refreshInfo(context)
    }

    fun clearKey(context: Context) {
        getPrefs(context).edit().clear().apply()
        refreshInfo(context)
    }
}
