package com.edith.assistant.ui.screens.quickactions

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.action.ActionExecutor
import com.edith.assistant.forge.ForgeController
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky
import kotlinx.coroutines.launch
import org.json.JSONObject

data class QuickActionTile(
    val title: String,
    val examplePhrase: String,
    val icon: ImageVector,
    val actionType: String,
    val param: String
)

@Composable
fun QuickActionsScreen(
    onBack: (() -> Unit)? = null,
    onOpenForge: () -> Unit
) {
    val context = LocalContext.current
    val colors = NovaTheme.colors
    val scope = rememberCoroutineScope()

    val actions = listOf(
        QuickActionTile(
            title = "Website Forge",
            examplePhrase = "ek cafe ki website banao",
            icon = Icons.Default.Language,
            actionType = "forge",
            param = "ek modern specialty coffee cafe ki stylish 3D website"
        ),
        QuickActionTile(
            title = "Open YouTube",
            examplePhrase = "YouTube kholo",
            icon = Icons.Default.PlayArrow,
            actionType = "app",
            param = "YouTube"
        ),
        QuickActionTile(
            title = "Spotify Music",
            examplePhrase = "Spotify chalao",
            icon = Icons.Default.MusicNote,
            actionType = "app",
            param = "Spotify"
        ),
        QuickActionTile(
            title = "Call Contact",
            examplePhrase = "Call Mum",
            icon = Icons.Default.Call,
            actionType = "call",
            param = "Mum"
        ),
        QuickActionTile(
            title = "WhatsApp Chat",
            examplePhrase = "Rahul ko WhatsApp karo",
            icon = Icons.Default.Message,
            actionType = "message",
            param = "Rahul"
        ),
        QuickActionTile(
            title = "Read Screen",
            examplePhrase = "Screen read karo",
            icon = Icons.Default.Search,
            actionType = "screen",
            param = "read"
        )
    )

    fun handleAction(tile: QuickActionTile) {
        when (tile.actionType) {
            "forge" -> {
                ForgeController.start(context, tile.param)
                onOpenForge()
            }
            "app" -> {
                scope.launch {
                    val args = JSONObject().put("app_name", tile.param)
                    ActionExecutor.execute(context, "open_app", args)
                }
            }
            "call" -> {
                scope.launch {
                    val args = JSONObject().put("contact_name", tile.param)
                    ActionExecutor.execute(context, "call_contact", args)
                }
            }
            "message" -> {
                scope.launch {
                    val args = JSONObject()
                        .put("contact_name", tile.param)
                        .put("message", "Hello! Sending this via edith AI")
                    ActionExecutor.execute(context, "message_contact", args)
                }
            }
            "screen" -> {
                scope.launch {
                    ActionExecutor.execute(context, "read_screen", JSONObject())
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NovaTopBar(title = "Quick Actions", onBack = onBack)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(actions) { tile ->
                    NovaGlassCard(
                        modifier = Modifier
                            .testTag("action_${tile.title.lowercase().replace(" ", "_")}")
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        onClick = { handleAction(tile) }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.accentGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tile.icon,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = tile.title,
                                style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${tile.examplePhrase}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
