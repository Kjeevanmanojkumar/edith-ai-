package com.edith.assistant.license

data class LicenseInfo(
    val active: Boolean,
    val plan: String,
    val expiresAt: Long,
    val statusText: String,
    val blocked: Boolean,
    val hasRememberedKey: Boolean
)

object AccessKeyRules {
    fun evaluate(
        hasKey: Boolean,
        plan: String,
        expiresAt: Long,
        blocked: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ): LicenseInfo {
        if (!hasKey) {
            return LicenseInfo(
                active = true, // Free Community Access active by default
                plan = "Community",
                expiresAt = 0L,
                statusText = "Community Plan",
                blocked = false,
                hasRememberedKey = false
            )
        }

        if (blocked) {
            return LicenseInfo(
                active = false,
                plan = plan.ifBlank { "Pro" },
                expiresAt = expiresAt,
                statusText = "Key Suspended or Blocked",
                blocked = true,
                hasRememberedKey = true
            )
        }

        if (expiresAt > 0L && currentTime > expiresAt) {
            return LicenseInfo(
                active = false,
                plan = plan.ifBlank { "Pro" },
                expiresAt = expiresAt,
                statusText = "License Expired",
                blocked = false,
                hasRememberedKey = true
            )
        }

        return LicenseInfo(
            active = true,
            plan = plan.ifBlank { "Pro" },
            expiresAt = expiresAt,
            statusText = if (plan.isNotBlank()) "$plan Plan Active" else "Pro Active",
            blocked = false,
            hasRememberedKey = true
        )
    }
}
