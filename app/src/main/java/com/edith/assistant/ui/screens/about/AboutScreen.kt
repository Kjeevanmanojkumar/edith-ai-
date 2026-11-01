package com.edith.assistant.ui.screens.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.AssistantBrand
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.NovaOrbState

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = NovaTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NovaTopBar(title = "About", onBack = onBack)

            Spacer(modifier = Modifier.height(24.dp))

            edithOrb3D(
                state = NovaOrbState.LISTENING,
                size = 120.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = AssistantBrand.FULL_NAME,
                style = MaterialTheme.typography.displayMedium.copy(color = colors.textPrimary)
            )
            Text(
                text = "Version 1.0.0 (Build 2026)",
                style = MaterialTheme.typography.labelSmall.copy(color = colors.accent)
            )

            Spacer(modifier = Modifier.height(24.dp))

            NovaGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    Text(
                        text = "Design Identity",
                        style = MaterialTheme.typography.titleMedium.copy(color = colors.accent)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"A pearl of light in the night\". Deep indigo background, single warm marigold accent, with real-time 3D GPU-shaded orb physics and Website Forge generation.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textPrimary,
                            lineHeight = 22.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Real-Time Engine",
                        style = MaterialTheme.typography.titleMedium.copy(color = colors.accent)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Powered by Google Gemini Live full-duplex multimodal bidirectional WebSockets with live barge-in support and Hindi/Hinglish/English brain switching.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textPrimary,
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }
    }
}
