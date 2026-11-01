package com.edith.assistant.license

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessKeyManager {
    private val _licenseInfo = MutableStateFlow(
        LicenseInfo(
            active = true,
            plan = "Play Edition",
            expiresAt = 0L,
            statusText = "Unlocked",
            blocked = false,
            hasRememberedKey = false
        )
    )
    val licenseInfo: StateFlow<LicenseInfo> = _licenseInfo.asStateFlow()

    fun refreshInfo(context: Context): LicenseInfo = _licenseInfo.value

    fun activate(context: Context, key: String): Result<LicenseInfo> = Result.success(_licenseInfo.value)

    fun liveCheck(context: Context) {
        // No-op in Play flavor
    }

    fun clearKey(context: Context) {
        // No-op in Play flavor
    }
}
