package com.edith.assistant.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edith.assistant.data.ApiKeyStore
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.data.Personality
import com.edith.assistant.forge.ForgeController
import com.edith.assistant.forge.ForgeIntent
import com.edith.assistant.ui.components.NovaGlassCard
import com.edith.assistant.ui.components.NovaTextField
import com.edith.assistant.ui.components.NovaTypingDots
import com.edith.assistant.ui.fx.edithOrb3D
import com.edith.assistant.ui.theme.LocalBottomBarInset
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.ui.theme.depthSurface
import com.edith.assistant.ui.theme.nightSky
import com.edith.assistant.voice.ChatReplyResult
import com.edith.assistant.voice.ChatTurn
import com.edith.assistant.voice.GeminiTextClient
import com.edith.assistant.voice.NovaOrbState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

object ChatPromptBuilder {
    fun build(personality: Personality, language: String, assistantName: String): String {
        return """
You are $assistantName, an intelligent and insightful personal AI companion.
This is a typed chat context. Never mention listening, speaking, microphone, or oral voice tone.
${personality.systemInstruction}
Language: Respond naturally in the user's chosen language.
Safety: Never claim to be human or possess a physical body. No explicit content.
""".trimIndent()
    }
}

@Composable
fun ChatScreen(
    appState: NovaAppState,
    onBack: (() -> Unit)? = null,
    onOpenForge: () -> Unit
) {
    val context = LocalContext.current
    val colors = NovaTheme.colors
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Hello! I am ${appState.assistantName}. How can I assist you today?",
                isUser = false
            )
        )
    }

    // TextToSpeech setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.getDefault()
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val query = inputText.trim()
        if (query.isEmpty() || isTyping) return

        messages.add(ChatMessage(text = query, isUser = true))
        inputText = ""

        // Check if user requested to build a website
        val forgePrompt = ForgeIntent.extract(query)
        if (forgePrompt != null) {
            messages.add(
                ChatMessage(
                    text = "Igniting the Website Forge for: \"$forgePrompt\"... Opening the 3D scene now!",
                    isUser = false
                )
            )
            ForgeController.start(context, forgePrompt)
            onOpenForge()
            return
        }

        isTyping = true
        scope.launch {
            val apiKey = ApiKeyStore.getKey(context)
            val systemPrompt = ChatPromptBuilder.build(
                appState.personality,
                appState.language.name,
                appState.assistantName
            )
            val historyTurns = messages.map { ChatTurn(isUser = it.isUser, text = it.text) }

            val result = GeminiTextClient.reply(apiKey, systemPrompt, historyTurns)
            isTyping = false

            when (result) {
                is ChatReplyResult.Success -> {
                    // Word-by-word reveal (20ms per word)
                    val words = result.text.split(" ")
                    val msgId = UUID.randomUUID().toString()
                    val streamingMsg = ChatMessage(id = msgId, text = "", isUser = false, isStreaming = true)
                    messages.add(streamingMsg)

                    val sb = StringBuilder()
                    for (w in words) {
                        sb.append(w).append(" ")
                        val idx = messages.indexOfFirst { it.id == msgId }
                        if (idx != -1) {
                            messages[idx] = streamingMsg.copy(text = sb.toString().trim())
                        }
                        delay(20)
                    }
                    val finalIdx = messages.indexOfFirst { it.id == msgId }
                    if (finalIdx != -1) {
                        messages[finalIdx] = streamingMsg.copy(text = result.text, isStreaming = false)
                    }
                }
                is ChatReplyResult.Error -> {
                    messages.add(
                        ChatMessage(
                            text = result.message,
                            isUser = false,
                            isError = true
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nightSky()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .depthSurface(RoundedCornerShape(0.dp), elevation = 4.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                }

                edithOrb3D(
                    state = if (isTyping) NovaOrbState.THINKING else NovaOrbState.IDLE,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = appState.assistantName,
                        style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary)
                    )
                    Text(
                        text = if (isTyping) "Typing..." else "Here to help",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isTyping) colors.accent else colors.textSecondary
                        )
                    )
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(messages, key = { it.id }) { msg ->
                    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
                    val bubbleBg = if (msg.isUser) colors.accent.copy(alpha = 0.22f) else colors.surfaceGlass
                    val textColor = if (msg.isError) colors.error else colors.textPrimary

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = alignment
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .depthSurface(
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (msg.isUser) 20.dp else 4.dp,
                                        bottomEnd = if (msg.isUser) 4.dp else 20.dp
                                    ),
                                    elevation = 4.dp
                                )
                                .background(bubbleBg)
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = textColor,
                                        lineHeight = 22.sp
                                    )
                                )

                                if (!msg.isUser && !msg.isStreaming && !msg.isError) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = colors.textTertiary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Reply", msg.text))
                                                }
                                        )
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Speak text",
                                            tint = colors.textTertiary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    tts?.speak(msg.text, TextToSpeech.QUEUE_FLUSH, null, null)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isTyping) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        ) {
                            NovaTypingDots()
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                NovaTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = "Type a message or 'ek cafe ki website banao'...",
                    trailingIcon = {
                        IconButton(
                            onClick = { sendMessage() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = colors.onAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
