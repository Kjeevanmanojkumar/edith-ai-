package com.edith.assistant.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.edith.assistant.action.NovaAccessibilityService
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTopBar
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.nightSky

@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = NovaTheme.colors

    var micGranted by remember { mutableStateOf(false) }
    var notifGranted by remember { mutableStateOf(false) }
    var contactsGranted by remember { mutableStateOf(false) }
    var callGranted by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        callGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        accessibilityEnabled = NovaAccessibilityService.isEnabled(context)
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissions()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissions()
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissions()
    }
    val callLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissions()
    }

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
            NovaTopBar(title = "App Permissions", onBack = onBack)

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Microphone
                PermissionCard(
                    title = "Microphone",
                    description = "Required for real-time full duplex conversational voice",
                    isGranted = micGranted,
                    onClick = {
                        if (!micGranted) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    testTag = "perm_mic"
                )

                // Notifications
                PermissionCard(
                    title = "Notifications",
                    description = "Needed for background voice session controls & Website Forge alerts",
                    isGranted = notifGranted,
                    onClick = {
                        if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    testTag = "perm_notif"
                )

                // Contacts
                PermissionCard(
                    title = "Contacts",
                    description = "Allows calling and messaging people by their contact name",
                    isGranted = contactsGranted,
                    onClick = {
                        if (!contactsGranted) contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    testTag = "perm_contacts"
                )

                // Phone Call
                PermissionCard(
                    title = "Phone Calling",
                    description = "Directly places phone calls to contacts when requested",
                    isGranted = callGranted,
                    onClick = {
                        if (!callGranted) callLauncher.launch(Manifest.permission.CALL_PHONE)
                    },
                    testTag = "perm_call"
                )

                // Accessibility
                PermissionCard(
                    title = "Accessibility Navigation",
                    description = "Enables on-screen assistance, reading elements, and automated taps",
                    isGranted = accessibilityEnabled,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    testTag = "perm_accessibility"
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = NovaTheme.colors
    val statusColor = if (isGranted) colors.lagoon else colors.error

    NovaGlassCard(
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGranted) "Granted" else "Grant",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isGranted) colors.lagoon else colors.accent
                )
            )
        }
    }
}
