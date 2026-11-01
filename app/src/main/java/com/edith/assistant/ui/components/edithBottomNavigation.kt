package com.edith.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.depthSurface
import com.edith.assistant.voice.NovaOrbState
import com.edith.assistant.voice.VoiceSessionManager

@Composable
fun edithBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onOpenVoice: () -> Unit
) {
    val colors = NovaTheme.colors
    val voiceState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Glass Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .depthSurface(RoundedCornerShape(32.dp), elevation = 12.dp)
                .background(colors.surfaceRaised.copy(alpha = 0.88f))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentRoute == "home",
                    onClick = { onNavigate("home") },
                    testTag = "nav_home"
                )
                NavItem(
                    label = "Chat",
                    icon = Icons.Default.Chat,
                    isSelected = currentRoute == "chat",
                    onClick = { onNavigate("chat") },
                    testTag = "nav_chat"
                )

                // Empty space in middle for the raised orb
                Box(modifier = Modifier.size(56.dp))

                NavItem(
                    label = "History",
                    icon = Icons.Default.History,
                    isSelected = currentRoute == "history",
                    onClick = { onNavigate("history") },
                    testTag = "nav_history"
                )
                NavItem(
                    label = "Settings",
                    icon = Icons.Default.Settings,
                    isSelected = currentRoute == "settings",
                    onClick = { onNavigate("settings") },
                    testTag = "nav_settings"
                )
            }
        }

        // Center Docked Talk Orb (raised slightly above bar)
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(62.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = colors.accentGlow)
                .clip(CircleShape)
                .background(colors.surfaceRaised)
                .clickable { onOpenVoice() }
                .testTag("nav_center_talk_orb"),
            contentAlignment = Alignment.Center
        ) {
            edithOrb3D(
                state = if (voiceState == NovaOrbState.IDLE) NovaOrbState.LISTENING else voiceState,
                size = 56.dp,
                amplitude = amplitude
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = NovaTheme.colors
    val tint = if (isSelected) colors.accent else colors.textTertiary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = tint,
                fontSize = 11.sp
            )
        )
    }
}
