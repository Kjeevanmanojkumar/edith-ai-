package com.edith.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun EdithTheme(
    mode: String = "DARK",
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (mode.uppercase()) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemInDarkTheme()
        else -> true // default DARK
    }

    val customColors = if (darkTheme) DarkNovaColors else LightNovaColors

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = customColors.accent,
            onPrimary = customColors.onAccent,
            secondary = customColors.accentSecondary,
            background = customColors.background,
            surface = customColors.surface,
            onBackground = customColors.textPrimary,
            onSurface = customColors.textPrimary,
            error = customColors.error
        )
    } else {
        lightColorScheme(
            primary = customColors.accent,
            onPrimary = customColors.onAccent,
            secondary = customColors.accentSecondary,
            background = customColors.background,
            surface = customColors.surface,
            onBackground = customColors.textPrimary,
            onSurface = customColors.textPrimary,
            error = customColors.error
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalNovaColors provides customColors,
        LocalNovaReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = EdithTypography,
            content = content
        )
    }
}
