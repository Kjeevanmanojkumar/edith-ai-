package com.edith.assistant.ui.screens.debug

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.EchoGuard
import com.edith.assistant.voice.VoiceSessionManager

@Composable
fun DebugScreen(onBack: () -> Unit) {
    val colors = NovaTheme.colors

    val state by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val muted by VoiceSessionManager.muted.collectAsState()
    val statusText by VoiceSessionManager.statusText.collectAsState()
    val isActive by VoiceSessionManager.isActive.collectAsState()
    val personality by VoiceSessionManager.currentPersonality.collectAsState()
    val language by VoiceSessionManager.currentLanguage.collectAsState()
    val detectedLanguage by VoiceSessionManager.detectedLanguage.collectAsState()
    val vadActive by VoiceSessionManager.vadActive.collectAsState()

    val latencyMs by VoiceSessionManager.latencyTracker.lastTurnLatencyMs.collectAsState()
    val reconnects by VoiceSessionManager.latencyTracker.reconnectCount.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            NovaTopBar(title = "Voice Pipeline Debugger", onBack = onBack)

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection State Card
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Session Active",
                                style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) colors.lagoon else colors.error)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DebugRow("State Machine", state.name)
                        DebugRow("Status Message", statusText)
                        DebugRow("Reconnect Attempts", reconnects.toString())
                    }
                }

                // Audio Telemetry Card
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Audio & Echo Telemetry",
                            style = MaterialTheme.typography.titleMedium.copy(color = colors.accent)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DebugRow("VAD Speaking Active", if (vadActive) "YES" else "SILENT")
                        DebugRow("Mic RMS Amplitude", String.format("%.3f", amplitude))
                        DebugRow("User Muted", muted.toString())
                        DebugRow("EchoGuard Mic Muted", EchoGuard.isMicMuted().toString())
                        DebugRow("Last Turn Latency", "${latencyMs} ms")
                    }
                }

                // Intelligence & Language Card
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Language & Personality Brain",
                            style = MaterialTheme.typography.titleMedium.copy(color = colors.accent)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DebugRow("Configured Personality", personality.displayName)
                        DebugRow("Target Language", language.displayName)
                        DebugRow("Auto-Detected Language", detectedLanguage.name)
                        DebugRow("Memory Turns Count", VoiceSessionManager.memory.getTurns().size.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    val colors = NovaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(color = colors.textPrimary)
        )
    }
}
