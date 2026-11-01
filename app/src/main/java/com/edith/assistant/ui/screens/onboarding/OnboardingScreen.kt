package com.edith.assistant.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.AssistantBrand
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.data.NovaOrbStyle
import com.edith.assistant.ui.components.ButtonVariant
import com.edith.assistant.ui.components.NovaButton
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.NovaOrbState
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val orbState: NovaOrbState,
    val orbStyle: NovaOrbStyle
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    appState: NovaAppState,
    onComplete: () -> Unit
) {
    val colors = NovaTheme.colors
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPageData(
            title = "Meet ${AssistantBrand.FULL_NAME}",
            subtitle = "Your voice-first personal AI companion living on your Android phone, speaking naturally in Hindi, Hinglish, and English.",
            orbState = NovaOrbState.LISTENING,
            orbStyle = NovaOrbStyle.NOVA
        ),
        OnboardingPageData(
            title = "Fluid Conversational Voice",
            subtitle = "Full duplex conversation powered by Google Gemini Live. Interrupt, barge-in, or pause naturally anytime.",
            orbState = NovaOrbState.SPEAKING,
            orbStyle = NovaOrbStyle.AURORA
        ),
        OnboardingPageData(
            title = "The 3D Website Forge",
            subtitle = "Say \"ek cafe ki website banao\" and watch a stunning modern 3D website materialize in real time before your eyes.",
            orbState = NovaOrbState.THINKING,
            orbStyle = NovaOrbStyle.PLASMA
        ),
        OnboardingPageData(
            title = "Phone & App Control",
            subtitle = "Call contacts, prepare messages, launch applications, and explore actions seamlessly on your device.",
            orbState = NovaOrbState.CONNECTING,
            orbStyle = NovaOrbStyle.ENERGY
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = {
                            appState.applyHasOnboarded(true)
                            onComplete()
                        },
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text("Skip", color = colors.textSecondary)
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIdx ->
                val page = pages[pageIdx]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    edithOrb3D(
                        state = page.orbState,
                        size = 230.dp,
                        amplitude = if (page.orbState == NovaOrbState.SPEAKING) 0.35f else 0f,
                        style = page.orbStyle,
                        assistantName = appState.assistantName
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            fontSize = 30.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // Bottom Navigation (Dots + Next/Get Started)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(pages.size) { idx ->
                        val isSelected = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.accent else colors.surfaceRaised)
                        )
                    }
                }

                NovaButton(
                    text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            appState.applyHasOnboarded(true)
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.PRIMARY,
                    testTag = "onboarding_next_button"
                )
            }
        }
    }
}
