package com.edith.assistant.ui.screens.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.ui.components.NovaButton
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTextField
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.LocalBottomBarInset
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.NovaOrbState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    appState: NovaAppState,
    onBack: (() -> Unit)? = null,
    onStartVoice: () -> Unit
) {
    val colors = NovaTheme.colors
    val bottomInset = LocalBottomBarInset.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = appState.conversations.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.preview.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset)
        ) {
            NovaTopBar(title = "Conversation History", onBack = onBack)

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                NovaTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search saved conversations...",
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.textTertiary
                        )
                    }
                )
            }

            if (filteredList.isEmpty()) {
                // Empty state: dim orb with "No conversations yet"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.alpha(0.55f)) {
                            edithOrb3D(state = NovaOrbState.IDLE, size = 120.dp)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.titleLarge.copy(color = colors.textPrimary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Talk or type to start your first session with ${appState.assistantName}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        NovaButton(
                            text = "Start Talking",
                            onClick = onStartVoice,
                            testTag = "history_start_talking_button"
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        val dateFormatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            .format(Date(item.timestamp))

                        NovaGlassCard(
                            modifier = Modifier
                                .testTag("conversation_${item.id}")
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 3.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
                                    )
                                    Text(
                                        text = item.preview,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = colors.textSecondary,
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textTertiary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
