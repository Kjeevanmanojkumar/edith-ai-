package com.edith.assistant.forge

import android.content.Context
import com.edith.assistant.data.ApiKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class ForgePhase {
    IDLE,
    BUILDING,
    DONE,
    FAILED,
    CANCELLED
}

data class ForgeMetrics(
    val chars: Int = 0,
    val sections: Int = 0,
    val elapsedSec: Long = 0,
    val tokensPerSec: Float = 0f
)

data class ForgeUiState(
    val phase: ForgePhase = ForgePhase.IDLE,
    val prompt: String = "",
    val isEdit: Boolean = false,
    val stage: ForgeStage = ForgeStage.INITIALIZING,
    val progress: Int = 0,
    val metrics: ForgeMetrics = ForgeMetrics(),
    val sections: List<String> = emptyList(),
    val log: List<String> = emptyList(),
    val buildId: String = "",
    val file: File? = null,
    val error: String? = null,
    val html: String = ""
)

sealed class ForgeStartResult(val message: String) {
    class Started(msg: String = "Opening Website Forge...") : ForgeStartResult(msg)
    class MissingKey(msg: String = "Please add your Gemini API key in Settings first.") : ForgeStartResult(msg)
    class Busy(msg: String = "Forge is currently generating a website.") : ForgeStartResult(msg)
}

object ForgeController {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var buildJob: Job? = null

    private val _uiState = MutableStateFlow(ForgeUiState())
    val uiState: StateFlow<ForgeUiState> = _uiState.asStateFlow()

    private val _pendingOpen = MutableStateFlow(false)
    val pendingOpen: StateFlow<Boolean> = _pendingOpen.asStateFlow()

    fun requestOpen() {
        _pendingOpen.value = true
    }

    fun consumePendingOpen(): Boolean {
        val current = _pendingOpen.value
        _pendingOpen.value = false
        return current
    }

    fun partialHtml(): String = _uiState.value.html

    fun start(context: Context, prompt: String): ForgeStartResult {
        val apiKey = ApiKeyStore.getKey(context)
        if (apiKey.isEmpty()) {
            return ForgeStartResult.MissingKey()
        }
        if (_uiState.value.phase == ForgePhase.BUILDING) {
            return ForgeStartResult.Busy()
        }

        requestOpen()
        val buildId = UUID.randomUUID().toString().take(8)
        _uiState.value = ForgeUiState(
            phase = ForgePhase.BUILDING,
            prompt = prompt,
            isEdit = false,
            stage = ForgeStage.INITIALIZING,
            progress = 5,
            buildId = buildId,
            log = listOf("Initializing Forge Reactor for: $prompt")
        )

        buildJob?.cancel()
        buildJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val client = WebsiteForgeClient(context.applicationContext)
            var lastDetectTime = 0L

            client.streamSite(apiKey, prompt).collect { event ->
                when (event) {
                    is ForgeStreamEvent.Chunk -> {
                        val now = System.currentTimeMillis()
                        val elapsed = (now - startTime) / 1000L
                        val chars = event.cumulativeHtml.length
                        val tokensApprox = chars / 4
                        val rate = if (elapsed > 0) tokensApprox.toFloat() / elapsed else 0f

                        val (stage, prog) = if (now - lastDetectTime > 120) {
                            lastDetectTime = now
                            ForgeStageDetector.detect(event.cumulativeHtml)
                        } else {
                            _uiState.value.stage to _uiState.value.progress
                        }

                        // Extract sections count
                        val sectionMatches = Regex("<section\\b", RegexOption.IGNORE_CASE)
                            .findAll(event.cumulativeHtml).count()

                        _uiState.value = _uiState.value.copy(
                            html = event.cumulativeHtml,
                            stage = stage,
                            progress = prog,
                            metrics = ForgeMetrics(
                                chars = chars,
                                sections = sectionMatches,
                                elapsedSec = elapsed,
                                tokensPerSec = rate
                            )
                        )
                    }

                    is ForgeStreamEvent.Complete -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ForgePhase.DONE,
                            stage = ForgeStage.POLISHED,
                            progress = 100,
                            html = event.finalHtml,
                            file = event.savedFile,
                            log = _uiState.value.log + listOf("Website compiled and saved successfully.")
                        )
                    }

                    is ForgeStreamEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ForgePhase.FAILED,
                            error = event.message,
                            log = _uiState.value.log + listOf("Error: ${event.message}")
                        )
                    }
                }
            }
        }

        return ForgeStartResult.Started()
    }

    fun edit(context: Context, changePrompt: String): ForgeStartResult {
        val apiKey = ApiKeyStore.getKey(context)
        if (apiKey.isEmpty()) return ForgeStartResult.MissingKey()
        val current = _uiState.value.html
        if (current.isEmpty()) return ForgeStartResult.MissingKey("No existing website to edit.")

        requestOpen()
        val buildId = UUID.randomUUID().toString().take(8)
        _uiState.value = _uiState.value.copy(
            phase = ForgePhase.BUILDING,
            prompt = changePrompt,
            isEdit = true,
            stage = ForgeStage.STRUCTURE,
            progress = 30,
            buildId = buildId,
            log = _uiState.value.log + listOf("Applying requested modifications...")
        )

        buildJob?.cancel()
        buildJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val client = WebsiteForgeClient(context.applicationContext)

            client.streamSite(apiKey, changePrompt, isEdit = true, currentHtml = current).collect { event ->
                when (event) {
                    is ForgeStreamEvent.Chunk -> {
                        val (stage, prog) = ForgeStageDetector.detect(event.cumulativeHtml)
                        _uiState.value = _uiState.value.copy(
                            html = event.cumulativeHtml,
                            stage = stage,
                            progress = prog
                        )
                    }
                    is ForgeStreamEvent.Complete -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ForgePhase.DONE,
                            stage = ForgeStage.POLISHED,
                            progress = 100,
                            html = event.finalHtml,
                            file = event.savedFile
                        )
                    }
                    is ForgeStreamEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ForgePhase.FAILED,
                            error = event.message
                        )
                    }
                }
            }
        }
        return ForgeStartResult.Started("Refining website...")
    }

    fun cancel() {
        buildJob?.cancel()
        _uiState.value = _uiState.value.copy(
            phase = ForgePhase.CANCELLED,
            log = _uiState.value.log + listOf("Generation cancelled.")
        )
    }

    fun reset() {
        cancel()
        _uiState.value = ForgeUiState()
    }

    fun openSaved(file: File) {
        if (file.exists()) {
            val html = file.readText()
            _uiState.value = ForgeUiState(
                phase = ForgePhase.DONE,
                stage = ForgeStage.POLISHED,
                progress = 100,
                html = html,
                file = file
            )
            requestOpen()
        }
    }
}
