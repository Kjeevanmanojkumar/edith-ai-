package com.edith.assistant.ui.screens.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.ApiKeyStore
import com.edith.assistant.data.LanguagePreference
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaSettingsRow
import com.edith.assistant.ui.components.NovaTextField
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.LocalBottomBarInset
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.NovaOrbState

@Composable
fun SettingsScreen(
    appState: NovaAppState,
    onBack: (() -> Unit)? = null,
    onNavigateToPersonality: () -> Unit,
    onNavigateToOrbStyle: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val colors = NovaTheme.colors
    val bottomInset = LocalBottomBarInset.current

    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(ApiKeyStore.getKey(context)) }

    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(appState.assistantName) }

    var showLanguageDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset + 24.dp)
        ) {
            NovaTopBar(title = "Settings", onBack = onBack)

            Spacer(modifier = Modifier.height(8.dp))

            // Profile Card at top
            NovaGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(colors.accentGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        edithOrb3D(state = NovaOrbState.LISTENING, size = 36.dp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = appState.assistantName,
                            style = MaterialTheme.typography.titleLarge.copy(color = colors.textPrimary)
                        )
                        Text(
                            text = "${appState.personality.displayName} • ${appState.language.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.accent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: AI & Intelligence
            Text(
                text = "Intelligence & Voice",
                style = MaterialTheme.typography.titleMedium.copy(color = colors.accent),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            NovaGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = 6.dp
            ) {
                Column {
                    NovaSettingsRow(
                        title = "Gemini API Key",
                        subtitle = if (ApiKeyStore.hasKey(context)) "Key configured (tap to change)" else "Not set (required for voice & chat)",
                        icon = Icons.Default.Key,
                        onClick = {
                            keyInput = ApiKeyStore.getKey(context)
                            showKeyDialog = true
                        },
                        testTag = "settings_api_key_row"
                    )

                    NovaSettingsRow(
                        title = "Assistant Name",
                        subtitle = appState.assistantName,
                        icon = Icons.Default.Person,
                        onClick = {
                            nameInput = appState.assistantName
                            showNameDialog = true
                        },
                        testTag = "settings_name_row"
                    )

                    NovaSettingsRow(
                        title = "Personality",
                        subtitle = appState.personality.displayName,
                        icon = Icons.Default.Psychology,
                        onClick = onNavigateToPersonality,
                        testTag = "settings_personality_row"
                    )

                    NovaSettingsRow(
                        title = "Voice Language",
                        subtitle = appState.language.displayName,
                        icon = Icons.Default.Language,
                        onClick = { showLanguageDialog = true },
                        testTag = "settings_language_row"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Visuals & Experience
            Text(
                text = "Visuals & Appearance",
                style = MaterialTheme.typography.titleMedium.copy(color = colors.accent),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            NovaGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = 8.dp
            ) {
                Column {
                    NovaSettingsRow(
                        title = "3D Orb Style",
                        subtitle = appState.orbStyle.displayName,
                        icon = Icons.Default.Palette,
                        onClick = onNavigateToOrbStyle,
                        testTag = "settings_orb_style_row"
                    )

                    NovaSettingsRow(
                        title = "Dark Theme",
                        subtitle = "Deep indigo night aesthetic",
                        icon = Icons.Default.DarkMode,
                        checked = appState.themeMode != "LIGHT",
                        onCheckedChange = { isDark ->
                            appState.applyThemeMode(if (isDark) "DARK" else "LIGHT")
                        },
                        testTag = "settings_theme_switch"
                    )

                    NovaSettingsRow(
                        title = "Reduced Motion",
                        subtitle = "Minimize 3D tilts and rotational effects",
                        icon = Icons.Default.Animation,
                        checked = appState.reducedMotion,
                        onCheckedChange = { appState.applyReducedMotion(it) },
                        testTag = "settings_reduced_motion_switch"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: System & Diagnostics
            Text(
                text = "System & Diagnostics",
                style = MaterialTheme.typography.titleMedium.copy(color = colors.accent),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            NovaGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = 10.dp
            ) {
                Column {
                    NovaSettingsRow(
                        title = "Permissions",
                        subtitle = "Microphone, Contacts, Accessibility",
                        icon = Icons.Default.Security,
                        onClick = onNavigateToPermissions,
                        testTag = "settings_permissions_row"
                    )

                    NovaSettingsRow(
                        title = "Voice Pipeline Debugger",
                        subtitle = "Live connection, VAD & latency telemetry",
                        icon = Icons.Default.BugReport,
                        onClick = onNavigateToDebug,
                        testTag = "settings_debugger_row"
                    )

                    NovaSettingsRow(
                        title = "About edith AI",
                        subtitle = "Version 1.0 • System details",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout,
                        testTag = "settings_about_row"
                    )
                }
            }
        }
    }

    // Key Input Dialog
    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Gemini API Key", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "Your key is stored securely on your device only. Never logged or shared.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = "AIzaSy..."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ApiKeyStore.saveKey(context, keyInput)
                        showKeyDialog = false
                    }
                ) {
                    Text("Save Key", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel", color = colors.textTertiary)
                }
            },
            containerColor = colors.surfaceRaised
        )
    }

    // Name Input Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Assistant Name", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("Personalize what you call your AI companion:")
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = "edith"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appState.applyAssistantName(nameInput)
                        showNameDialog = false
                    }
                ) {
                    Text("Save", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = colors.textTertiary)
                }
            },
            containerColor = colors.surfaceRaised
        )
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Voice Language", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguagePreference.entries.forEach { lang ->
                        NovaSettingsRow(
                            title = lang.displayName,
                            subtitle = lang.description,
                            onClick = {
                                appState.applyLanguage(lang)
                                showLanguageDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close", color = colors.accent)
                }
            },
            containerColor = colors.surfaceRaised
        )
    }
}
