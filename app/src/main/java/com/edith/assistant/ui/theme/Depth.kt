package com.edith.assistant.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalBottomBarInset = compositionLocalOf { 80.dp }

fun Modifier.nightSky(): Modifier = composed {
    val colors = NovaTheme.colors
    val reduced = NovaTheme.reducedMotion

    this
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    colors.backgroundGradientTop,
                    colors.background
                )
            )
        )
        .drawBehind {
            if (!reduced) {
                // Subtle static stars field
                val starPositions = listOf(
                    Offset(size.width * 0.15f, size.height * 0.12f),
                    Offset(size.width * 0.82f, size.height * 0.18f),
                    Offset(size.width * 0.45f, size.height * 0.28f),
                    Offset(size.width * 0.28f, size.height * 0.42f),
                    Offset(size.width * 0.72f, size.height * 0.48f),
                    Offset(size.width * 0.12f, size.height * 0.65f),
                    Offset(size.width * 0.88f, size.height * 0.72f),
                    Offset(size.width * 0.52f, size.height * 0.85f),
                    Offset(size.width * 0.35f, size.height * 0.92f)
                )
                for (pos in starPositions) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = 1.5f,
                        center = pos
                    )
                }
            }
        }
}

fun Modifier.depthSurface(
    shape: Shape,
    elevation: Dp = 6.dp
): Modifier = composed {
    val colors = NovaTheme.colors
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = colors.depthShadow,
            spotColor = colors.accentGlow
        )
        .clip(shape)
        .background(colors.surfaceGlass)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    colors.rimLight,
                    colors.surfaceBorder
                )
            ),
            shape = shape
        )
}
