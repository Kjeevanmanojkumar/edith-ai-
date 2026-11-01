package com.edith.assistant.ui.screens.profile

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.license.AccessKeyManager
import com.edith.assistant.ui.components.NovaButton
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTextField
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky

@Composable
fun ProfileScreen(
    appState: NovaAppState,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = NovaTheme.colors
    val licenseInfo by AccessKeyManager.licenseInfo.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(appState.userName) }

    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }

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
            NovaTopBar(title = "Your Profile", onBack = onBack)

            Spacer(modifier = Modifier.height(20.dp))

            // Avatar & Name Card
            NovaGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(colors.accentGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Avatar",
                            tint = colors.accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appState.userName,
                            style = MaterialTheme.typography.titleLarge.copy(color = colors.textPrimary)
                        )
                        Text(
                            text = "Companion: ${appState.assistantName}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                        )
                    }

                    IconButton(
                        onClick = {
                            nameInput = appState.userName
                            showEditNameDialog = true
                        },
                        modifier = Modifier.testTag("edit_user_name_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Access Key & License Section
            Text(
                text = "Licensing & Access Tier",
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = colors.accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = licenseInfo.plan,
                                style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
                            )
                            Text(
                                text = licenseInfo.statusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (licenseInfo.active) colors.lagoon else colors.error
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NovaButton(
                        text = if (licenseInfo.hasRememberedKey) "Change Pro Access Key" else "Activate Pro Key",
                        onClick = { showKeyDialog = true },
                        testTag = "activate_access_key_button"
                    )
                }
            }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Your Name") },
            text = {
                NovaTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = "Your name"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appState.applyUserName(nameInput)
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = colors.textTertiary)
                }
            },
            containerColor = colors.surfaceRaised
        )
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Enter Direct Access Key") },
            text = {
                Column {
                    Text(
                        "Activate VIP or Pro features with your access key:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = "EDITH-VIP-..."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AccessKeyManager.activate(context, keyInput)
                        showKeyDialog = false
                    }
                ) {
                    Text("Activate", color = colors.accent)
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
}
