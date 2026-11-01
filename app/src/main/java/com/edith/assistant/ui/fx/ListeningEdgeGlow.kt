package com.edith.assistant.ui.fx

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.edith.assistant.voice.NovaOrbState
import com.edith.assistant.voice.VoiceSessionManager

@Composable
fun ListeningEdgeGlow(modifier: Modifier = Modifier) {
    val isActive by VoiceSessionManager.isActive.collectAsState()
    val state by VoiceSessionManager.state.collectAsState()

    val glowColor = when (state) {
        NovaOrbState.LISTENING -> Color(0xFF3FE0D0) // lagoon
        NovaOrbState.SPEAKING -> Color(0xFFFF6FAE)  // pink
        NovaOrbState.THINKING -> Color(0xFFFFB547)  // marigold
        NovaOrbState.ERROR -> Color(0xFFFF7A85)     // red
        else -> Color(0xFF8B5CF6)                   // violet resting
    }

    AnimatedVisibility(
        visible = isActive && state != NovaOrbState.IDLE,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(400))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .border(
                    width = 4.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            glowColor.copy(alpha = 0.85f),
                            glowColor.copy(alpha = 0.35f),
                            glowColor.copy(alpha = 0.85f)
                        )
                    ),
                    shape = androidx.compose.ui.graphics.RectangleShape
                )
        )
    }
}

object OverlayEdgeGlowController {
    fun show(context: Context) {
        // Safe no-op unless system alert overlay is granted
    }

    fun hide(context: Context) {
        // Safe no-op
    }
}
