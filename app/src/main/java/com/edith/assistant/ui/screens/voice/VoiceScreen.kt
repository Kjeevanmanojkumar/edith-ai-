package com.edith.assistant.ui.screens.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.edith.assistant.data.ApiKeyStore
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.ui.components.NovaButton
import com.edith.assistant.ui.fx.ListeningEdgeGlow
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.depthSurface
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.NovaOrbState
import com.edith.assistant.voice.VoiceForegroundService
import com.edith.assistant.voice.VoiceSessionManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceScreen(
    appState: NovaAppState,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val colors = NovaTheme.colors

    val state by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val muted by VoiceSessionManager.muted.collectAsState()
    val statusText by VoiceSessionManager.statusText.collectAsState()

    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordPermission = granted
        if (granted) {
            VoiceForegroundService.start(context)
        }
    }

    val hasKey = ApiKeyStore.hasKey(context)

    LaunchedEffect(hasRecordPermission, hasKey) {
        if (hasRecordPermission && hasKey) {
            VoiceForegroundService.start(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        // Listening edge glow overlay
        ListeningEdgeGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .depthSurface(CircleShape, elevation = 4.dp)
                        .testTag("voice_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Mode",
                        tint = colors.textPrimary
                    )
                }
            }

            // Center Stage (Orb + Status)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 48-bar visualizer ring on SPEAKING state
                    if (state == NovaOrbState.SPEAKING) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val baseR = size.minDimension * 0.44f
                            val numBars = 48
                            for (i in 0 until numBars) {
                                val angle = (i * 2 * PI / numBars).toFloat()
                                val barLen = 6.dp.toPx() + amplitude * 22.dp.toPx() * (if (i % 2 == 0) 1f else 0.6f)
                                val startX = center.x + baseR * cos(angle)
                                val startY = center.y + baseR * sin(angle)
                                val endX = center.x + (baseR + barLen) * cos(angle)
                                val endY = center.y + (baseR + barLen) * sin(angle)

                                drawLine(
                                    color = colors.accent.copy(alpha = 0.8f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 2.5f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    // 3D Orb at center
                    val orbState = when {
                        !hasRecordPermission || !hasKey -> NovaOrbState.ERROR
                        else -> state
                    }

                    edithOrb3D(
                        state = orbState,
                        size = 240.dp,
                        amplitude = amplitude,
                        style = appState.orbStyle,
                        assistantName = appState.assistantName
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Status text in serif italic
                val displayText = when {
                    !hasKey -> "API Key missing — tap below to set key"
                    !hasRecordPermission -> "Microphone permission required"
                    else -> statusText
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = if (!hasKey || !hasRecordPermission) colors.error else colors.textPrimary,
                        fontStyle = FontStyle.Italic,
                        fontSize = 22.sp
                    )
                )

                if (!hasKey) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NovaButton(
                        text = "Open Settings to Add Key",
                        onClick = onOpenSettings,
                        testTag = "voice_add_key_button"
                    )
                } else if (!hasRecordPermission) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NovaButton(
                        text = "Grant Microphone Permission",
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        testTag = "voice_grant_mic_button"
                    )
                }
            }

            // Bottom Frosted Control Row (Mute, Stop, Close)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .depthSurface(RoundedCornerShape(32.dp), elevation = 6.dp)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Toggle
                    IconButton(
                        onClick = { VoiceSessionManager.toggleMute() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (muted) colors.error.copy(alpha = 0.25f) else colors.surfaceRaised)
                            .testTag("voice_mute_button")
                    ) {
                        Icon(
                            imageVector = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (muted) "Unmute" else "Mute",
                            tint = if (muted) colors.error else colors.textPrimary
                        )
                    }

                    // Stop Session (Large, red-tinted)
                    IconButton(
                        onClick = {
                            VoiceForegroundService.stop(context)
                            onClose()
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(colors.error)
                            .testTag("voice_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Voice Conversation",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Close UI without stopping background session
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceRaised)
                            .testTag("voice_minimize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Minimize",
                            tint = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}
