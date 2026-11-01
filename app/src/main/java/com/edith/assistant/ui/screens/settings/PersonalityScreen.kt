package com.edith.assistant.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.data.Personality
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.depthSurface
import com.edith.assistant.ui.theme.nightSky

@Composable
fun PersonalityScreen(
    appState: NovaAppState,
    onBack: () -> Unit
) {
    val colors = NovaTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NovaTopBar(title = "Personality", onBack = onBack)

            // Live status banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(1.2f)
                        .depthSurface(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "• Changes take effect immediately",
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.accent)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(Personality.entries) { item ->
                    val isSelected = appState.personality == item
                    val scale = if (isSelected) 1.04f else 0.98f
                    val alpha = if (isSelected) 1.0f else 0.72f
                    val shape = RoundedCornerShape(20.dp)

                    Box(
                        modifier = Modifier
                            .testTag("personality_${item.name.lowercase()}")
                            .scale(scale)
                            .alpha(alpha)
                            .depthSurface(shape, elevation = if (isSelected) 12.dp else 4.dp)
                            .border(
                                width = if (isSelected) 2.dp else 0.5.dp,
                                color = if (isSelected) colors.accent else colors.surfaceBorder,
                                shape = shape
                            )
                            .clickable {
                                appState.applyPersonality(item)
                            }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (item) {
                                        Personality.NORMAL -> "✨"
                                        Personality.FRIENDLY -> "💛"
                                        Personality.PROFESSIONAL -> "💼"
                                        Personality.FUNNY -> "😄"
                                        Personality.COMPANION -> "🤝"
                                        Personality.GF_MODE -> "💖"
                                        Personality.TEACHER -> "🎓"
                                        Personality.DEVELOPER -> "💻"
                                        Personality.HUNGRY -> "🍕"
                                        Personality.NAUTANKI -> "🎭"
                                    },
                                    fontSize = 24.sp
                                )

                                if (isSelected) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
