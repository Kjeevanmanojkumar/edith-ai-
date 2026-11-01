package com.edith.assistant.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.AssistantBrand
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.NovaOrbState
import kotlinx.coroutines.delay

object SplashSession {
    var played: Boolean = false
}

@Composable
fun edithSplash3D(onFinish: () -> Unit) {
    if (SplashSession.played) {
        onFinish()
        return
    }

    val scaleAnim = remember { Animatable(0.4f) }
    val alphaAnim = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        delay(800)
        alphaAnim.animateTo(0f, tween(400))
        SplashSession.played = true
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
            .alpha(alphaAnim.value)
            .clickable {
                SplashSession.played = true
                onFinish()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scaleAnim.value)
        ) {
            edithOrb3D(
                state = NovaOrbState.LISTENING,
                size = 180.dp,
                amplitude = 0.2f
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = AssistantBrand.FULL_NAME,
                style = MaterialTheme.typography.displayMedium.copy(
                    color = NovaTheme.colors.textPrimary,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.alpha(textAlpha.value)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = AssistantBrand.TAGLINE,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = NovaTheme.colors.accent
                ),
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
