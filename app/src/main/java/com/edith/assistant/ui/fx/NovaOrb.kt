package com.edith.assistant.ui.fx

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.edith.assistant.data.NovaOrbStyle
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.voice.NovaOrbState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun NovaOrb(
    state: NovaOrbState,
    size: Dp = 220.dp,
    amplitude: Float = 0f,
    style: NovaOrbStyle = NovaOrbStyle.NOVA,
    modifier: Modifier = Modifier,
    assistantName: String = "edith"
) {
    val reduced = NovaTheme.reducedMotion
    val colors = NovaTheme.colors

    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    // Breathing (4.2 s sine, +-3.5 %)
    val breathing by infiniteTransition.animateFloat(
        initialValue = if (reduced) 1f else 0.965f,
        targetValue = if (reduced) 1f else 1.035f,
        animationSpec = infiniteRepeatable(tween(2100, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    // Rotation speeds per state
    val rotDuration = when (state) {
        NovaOrbState.CONNECTING -> 1100
        NovaOrbState.THINKING -> 2600
        else -> 9000
    }

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(rotDuration, easing = LinearEasing), RepeatMode.Restart),
        label = "rotate"
    )

    // Expanding rings for LISTENING
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "ringPulse"
    )

    // State-based core colors
    val (coreStart, coreEnd, glowColor) = when (state) {
        NovaOrbState.IDLE -> Triple(colors.lotus, colors.surfaceRaised, colors.lotus.copy(alpha = 0.35f))
        NovaOrbState.LISTENING -> Triple(colors.lagoon, Color(0xFF007799), colors.lagoon.copy(alpha = 0.5f))
        NovaOrbState.THINKING -> Triple(colors.lagoon, colors.accent, colors.lagoon.copy(alpha = 0.6f))
        NovaOrbState.SPEAKING -> Triple(colors.accent, colors.lotus, colors.accentSecondary.copy(alpha = 0.65f))
        NovaOrbState.CONNECTING -> Triple(colors.accent, Color(0xFFB45309), colors.accent.copy(alpha = 0.4f))
        NovaOrbState.ERROR -> Triple(colors.error, Color(0xFF7F1D1D), colors.error.copy(alpha = 0.4f))
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$assistantName status: ${state.name.lowercase()}" }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f * breathing
            val ampRadius = baseRadius * (1f + amplitude * 0.28f)

            // 1. Radial outer atmospheric glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = ampRadius * 1.55f
                ),
                radius = ampRadius * 1.55f,
                center = center
            )

            // 2. Expanding fading rings while LISTENING
            if (state == NovaOrbState.LISTENING) {
                val ring1R = baseRadius * (1f + ringPulse * 0.45f)
                val ring1Alpha = (1f - ringPulse).coerceIn(0f, 1f) * 0.6f
                drawCircle(
                    color = colors.lagoon.copy(alpha = ring1Alpha),
                    radius = ring1R,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                val pulse2 = (ringPulse + 0.5f) % 1f
                val ring2R = baseRadius * (1f + pulse2 * 0.45f)
                val ring2Alpha = (1f - pulse2).coerceIn(0f, 1f) * 0.6f
                drawCircle(
                    color = colors.lagoon.copy(alpha = ring2Alpha),
                    radius = ring2R,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            // 3. Rotating 100-degree arc while CONNECTING
            if (state == NovaOrbState.CONNECTING) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(colors.accent, Color.Transparent)),
                    startAngle = rotationAngle,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(ampRadius * 2.2f, ampRadius * 2.2f),
                    topLeft = Offset(center.x - ampRadius * 1.1f, center.y - ampRadius * 1.1f)
                )
            }

            // 4. Orbiting particles
            if (style != NovaOrbStyle.MINIMAL && state != NovaOrbState.ERROR) {
                val particleCount = when (style) {
                    NovaOrbStyle.ENERGY -> 8
                    NovaOrbStyle.PLASMA -> 6
                    NovaOrbStyle.GLASS -> 3
                    else -> 5
                }

                for (i in 0 until particleCount) {
                    val angleRad = (rotationAngle * (if (i % 2 == 0) 1 else -1) + (i * 360f / particleCount)) * (PI / 180.0)
                    val orbitDist = ampRadius * (1.18f + 0.08f * sin(angleRad.toFloat() * 2f))
                    val px = center.x + (orbitDist * cos(angleRad)).toFloat()
                    val py = center.y + (orbitDist * sin(angleRad)).toFloat()

                    drawCircle(
                        color = if (i % 2 == 0) colors.accent else colors.lagoon,
                        radius = 3.5.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            // 5. Archer Style (Fibonacci golden-spiral particles)
            if (style == NovaOrbStyle.ARCHER && !reduced) {
                val totalParticles = 240
                val goldenRatio = 1.6180339887f
                for (n in 0 until totalParticles) {
                    val theta = 2f * PI.toFloat() * n / goldenRatio + (rotationAngle * PI.toFloat() / 180f * 0.2f)
                    val phi = kotlin.math.acos(1f - 2f * (n + 0.5f) / totalParticles)
                    val r = ampRadius * (0.92f + 0.05f * amplitude)
                    val z = r * cos(phi)
                    val depthAlpha = ((z / r + 1f) / 2f).coerceIn(0.15f, 0.95f)

                    val px = center.x + r * sin(phi) * cos(theta)
                    val py = center.y + r * sin(phi) * sin(theta)

                    drawCircle(
                        color = colors.accent.copy(alpha = depthAlpha),
                        radius = (1.5f + depthAlpha * 2f).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            } else {
                // Radial gradient core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(coreStart, coreEnd),
                        center = Offset(center.x - ampRadius * 0.22f, center.y - ampRadius * 0.22f),
                        radius = ampRadius
                    ),
                    radius = ampRadius,
                    center = center
                )

                // White specular highlight for GLASS style
                if (style == NovaOrbStyle.GLASS) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.55f),
                        startAngle = 200f,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(ampRadius * 1.6f, ampRadius * 1.6f),
                        topLeft = Offset(center.x - ampRadius * 0.8f, center.y - ampRadius * 0.8f)
                    )
                }
            }

            // Error pulse ring
            if (state == NovaOrbState.ERROR) {
                drawCircle(
                    color = colors.error.copy(alpha = 0.4f),
                    radius = ampRadius * 1.25f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}
