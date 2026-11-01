package com.edith.assistant.ui.screens.forge

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import com.edith.assistant.forge.ForgeController
import com.edith.assistant.forge.ForgePhase
import com.edith.assistant.forge.ForgeStage
import com.edith.assistant.ui.components.NovaButton
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTextField
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.depthSurface
import com.edith.assistant.ui.theme.nightSky
import org.json.JSONObject
import java.io.File

@Composable
fun ForgeScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = NovaTheme.colors
    val uiState by ForgeController.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editPromptInput by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (uiState.phase == ForgePhase.BUILDING) {
            ForgeController.cancel()
        }
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when (uiState.phase) {
            ForgePhase.BUILDING, ForgePhase.IDLE -> {
                // 3D Scene View (WebGL WebView or fallback)
                ForgeWebGlScene(
                    modifier = Modifier.fillMaxSize(),
                    progress = uiState.progress,
                    stage = uiState.stage.label,
                    phase = uiState.phase.name
                )

                // HUD overlay
                ForgeHud(
                    prompt = uiState.prompt,
                    stage = uiState.stage,
                    progress = uiState.progress,
                    chars = uiState.metrics.chars,
                    sections = uiState.metrics.sections,
                    rate = uiState.metrics.tokensPerSec,
                    elapsed = uiState.metrics.elapsedSec,
                    onCancel = {
                        ForgeController.cancel()
                        onBack()
                    }
                )
            }

            ForgePhase.DONE -> {
                // Generated Site Viewer (Sandboxed WebView)
                GeneratedSiteView(
                    html = uiState.html,
                    modifier = Modifier.fillMaxSize()
                )

                // Result Floating Toolbar
                ResultToolbar(
                    onBack = onBack,
                    onShare = {
                        val file = uiState.file
                        if (file != null && file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.forge.provider",
                                file
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/html"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Website"))
                        }
                    },
                    onOpenBrowser = {
                        val file = uiState.file
                        if (file != null && file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.forge.provider",
                                file
                            )
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "text/html")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(viewIntent)
                            } catch (_: Exception) {}
                        }
                    },
                    onEdit = {
                        editPromptInput = ""
                        showEditDialog = true
                    },
                    onRebuild = {
                        ForgeController.start(context, uiState.prompt)
                    },
                    onDelete = {
                        showDeleteDialog = true
                    }
                )
            }

            ForgePhase.FAILED, ForgePhase.CANCELLED -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nightSky()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.phase == ForgePhase.CANCELLED) "Build Cancelled" else "Forge Encountered an Issue",
                            style = MaterialTheme.typography.displayMedium.copy(color = colors.textPrimary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.error ?: "Generation stopped before finishing.",
                            style = MaterialTheme.typography.bodyLarge.copy(color = colors.textSecondary)
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            NovaButton(
                                text = "Retry",
                                onClick = {
                                    ForgeController.start(context, uiState.prompt)
                                }
                            )
                            NovaButton(
                                text = "Back",
                                onClick = onBack
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Website", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("Describe changes to apply to your website:")
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = editPromptInput,
                        onValueChange = { editPromptInput = it },
                        placeholder = "e.g. Change primary color to emerald green..."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        ForgeController.edit(context, editPromptInput)
                    }
                ) {
                    Text("Update", color = colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = colors.textTertiary)
                }
            },
            containerColor = colors.surfaceRaised
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Website?") },
            text = { Text("This will remove the generated website file.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        uiState.file?.delete()
                        ForgeController.reset()
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text("Delete", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.textTertiary)
                }
            },
            containerColor = colors.surfaceRaised
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ForgeWebGlScene(
    modifier: Modifier = Modifier,
    progress: Int,
    stage: String,
    phase: String
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(progress, stage, phase) {
        val stateObj = JSONObject()
        stateObj.put("progress", progress)
        stateObj.put("stage", stage)
        stateObj.put("phase", phase)
        webViewRef?.evaluateJavascript("window.Forge && window.Forge.updateState('${stateObj}');", null)
    }

    AndroidView(
        factory = { ctx ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                setBackgroundColor(0xFF06050E.toInt())

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onReady() {}

                    @JavascriptInterface
                    fun onRevealFinished() {}

                    @JavascriptInterface
                    fun onWebGLFailed() {}
                }, "ForgeBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url ?: return null
                        return assetLoader.shouldInterceptRequest(url)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return true
                    }
                }

                loadUrl("https://appassets.androidplatform.net/assets/forge/forge.html")
                webViewRef = this
            }
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GeneratedSiteView(
    html: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.setGeolocationEnabled(false)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url ?: return false
                        if (url.scheme == "http" || url.scheme == "https") {
                            try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, url))
                            } catch (_: Exception) {}
                            return true
                        }
                        return false
                    }
                }

                loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

@Composable
fun ForgeHud(
    prompt: String,
    stage: ForgeStage,
    progress: Int,
    chars: Int,
    sections: Int,
    rate: Float,
    elapsed: Long,
    onCancel: () -> Unit
) {
    val colors = NovaTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        // Top prompt chip
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 20.dp, end = 20.dp)
                .depthSurface(RoundedCornerShape(24.dp), elevation = 8.dp)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = "⚡ $prompt",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = colors.accent,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
        }

        // Bottom status card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .depthSurface(RoundedCornerShape(28.dp), elevation = 12.dp)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stage.label,
                            style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
                        )
                        Text(
                            text = "Stage ${stage.ordinal + 1} of ${ForgeStage.entries.size}",
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                        )
                    }

                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = colors.accent,
                            fontSize = 32.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Six-segment stage track
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(6) { idx ->
                        val isFilled = progress >= (idx + 1) * 16
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isFilled) colors.accent else colors.surfaceBorder)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${chars / 1024} KB • $sections sections",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = "${rate.toInt()} t/s • ${elapsed}s",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textTertiary)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                NovaButton(
                    text = "Cancel Generation",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "forge_cancel_button"
                )
            }
        }
    }
}

@Composable
fun ResultToolbar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenBrowser: () -> Unit,
    onEdit: () -> Unit,
    onRebuild: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = NovaTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .depthSurface(RoundedCornerShape(32.dp), elevation = 8.dp)
                .background(colors.surfaceRaised.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
            }
            IconButton(onClick = onShare, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Share, "Share", tint = colors.accent)
            }
            IconButton(onClick = onOpenBrowser, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.OpenInBrowser, "Browser", tint = colors.textPrimary)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Edit, "Edit", tint = colors.textPrimary)
            }
            IconButton(onClick = onRebuild, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Refresh, "Rebuild", tint = colors.textPrimary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = colors.error)
            }
        }
    }
}
