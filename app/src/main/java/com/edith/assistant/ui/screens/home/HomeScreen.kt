package com.edith.assistant.ui.screens.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.AssistantBrand
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.ui.components.ButtonVariant
import com.edith.assistant.ui.components.NovaButton
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.LocalBottomBarInset
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.depthSurface
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.NovaOrbState
import com.edith.assistant.voice.VoiceSessionManager
import java.util.Calendar

@Composable
fun HomeScreen(
    appState: NovaAppState,
    onOpenVoice: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onTrySaying: (String) -> Unit
) {
    val colors = NovaTheme.colors
    val bottomInset = LocalBottomBarInset.current
    val voiceState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    val samplePhrases = listOf(
        "ek cafe ki website banao",
        "Call Mummy",
        "Open YouTube",
        "Tell me a joke",
        "What's on my screen?",
        "Post on Instagram"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset + 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .depthSurface(CircleShape, elevation = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        edithOrb3D(state = NovaOrbState.LISTENING, size = 26.dp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = AssistantBrand.FULL_NAME,
                        style = MaterialTheme.typography.titleLarge.copy(color = colors.textPrimary)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .depthSurface(CircleShape, elevation = 4.dp)
                            .testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier
                            .size(44.dp)
                            .depthSurface(CircleShape, elevation = 4.dp)
                            .testTag("home_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Greeting in serif font
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$greeting, ${appState.userName}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = colors.textPrimary,
                        fontSize = 32.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${appState.assistantName} is ready to converse",
                    style = MaterialTheme.typography.bodyLarge.copy(color = colors.textSecondary)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Hero 3D Orb (~55% width, centered)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVoice() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    edithOrb3D(
                        state = if (voiceState == NovaOrbState.IDLE) NovaOrbState.LISTENING else voiceState,
                        size = 220.dp,
                        amplitude = amplitude,
                        style = appState.orbStyle,
                        assistantName = appState.assistantName
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Tap me to talk",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = colors.accent,
                            fontStyle = FontStyle.Italic,
                            fontSize = 20.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Talk and Type Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NovaButton(
                    text = "Talk",
                    onClick = onOpenVoice,
                    icon = Icons.Default.Mic,
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.weight(1f),
                    testTag = "talk_button"
                )
                NovaButton(
                    text = "Type",
                    onClick = onOpenChat,
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f),
                    testTag = "type_button"
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // "Try saying" Section
            Text(
                text = "Try saying",
                style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                samplePhrases.forEach { phrase ->
                    NovaGlassCard(
                        modifier = Modifier.width(170.dp),
                        shape = RoundedCornerShape(16.dp),
                        onClick = { onTrySaying(phrase) }
                    ) {
                        Column {
                            Text(
                                text = "💬",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = phrase,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
