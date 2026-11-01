package com.edith.assistant.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.data.NovaOrbStyle
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.fx.NovaOrb
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.depthSurface
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.NovaOrbState

@Composable
fun OrbStyleScreen(
    appState: NovaAppState,
    onBack: () -> Unit
) {
    val colors = NovaTheme.colors
    var previewState by remember { mutableStateOf(NovaOrbState.LISTENING) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NovaTopBar(title = "3D Orb Style", onBack = onBack)

            Spacer(modifier = Modifier.height(16.dp))

            // State toggle chips (Idle / Listening / Speaking)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                listOf(
                    NovaOrbState.IDLE to "Idle",
                    NovaOrbState.LISTENING to "Listening",
                    NovaOrbState.SPEAKING to "Speaking"
                ).forEach { (st, label) ->
                    val isSelected = previewState == st
                    val chipBg = if (isSelected) colors.accent else colors.surfaceRaised
                    val textColor = if (isSelected) colors.onAccent else colors.textPrimary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(chipBg)
                            .clickable { previewState = st }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Large Live Preview
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                edithOrb3D(
                    state = previewState,
                    size = 230.dp,
                    amplitude = if (previewState == NovaOrbState.SPEAKING) 0.45f else 0f,
                    style = appState.orbStyle,
                    assistantName = appState.assistantName
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = appState.orbStyle.displayName,
                style = MaterialTheme.typography.displayMedium.copy(
                    color = colors.textPrimary,
                    fontSize = 28.sp
                )
            )
            Text(
                text = appState.orbStyle.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Horizontal Carousel of the 7 styles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NovaOrbStyle.entries.forEach { style ->
                    val isSelected = appState.orbStyle == style
                    val scale = if (isSelected) 1.08f else 0.94f

                    NovaGlassCard(
                        modifier = Modifier
                            .testTag("orb_style_${style.name.lowercase()}")
                            .width(130.dp)
                            .scale(scale),
                        elevation = if (isSelected) 10.dp else 4.dp,
                        onClick = { appState.applyOrbStyle(style) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            NovaOrb(
                                state = previewState,
                                size = 64.dp,
                                amplitude = 0f,
                                style = style,
                                assistantName = appState.assistantName
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = style.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isSelected) colors.accent else colors.textPrimary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
