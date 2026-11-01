package com.edith.assistant.voice

import android.content.Context
import com.edith.assistant.action.ActionExecutor
import com.edith.assistant.action.InstalledAppLabelCache
import com.edith.assistant.data.ApiKeyStore
import com.edith.assistant.data.AssistantBrand
import com.edith.assistant.data.LanguagePreference
import com.edith.assistant.data.Personality
import com.edith.assistant.data.PersonalityRepository
import com.edith.assistant.forge.ForgeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

object VoiceSessionManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val connectMutex = Mutex()
    private val reconnectMutex = Mutex()

    private var sessionEpoch = 0L
    private var client: GeminiLiveClient? = null
    private var audioIO: LiveAudioIO? = null
    val memory = ConversationMemory(12)
    val latencyTracker = VoiceLatencyTracker()

    private var reconnectJob: Job? = null
    private var silenceWatchdogJob: Job? = null
    private var consecutiveReconnectFailures = 0

    // Exposed StateFlows
    private val _state = MutableStateFlow(NovaOrbState.IDLE)
    val state: StateFlow<NovaOrbState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _statusText = MutableStateFlow("Ready to talk")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _currentPersonality = MutableStateFlow(Personality.NORMAL)
    val currentPersonality: StateFlow<Personality> = _currentPersonality.asStateFlow()

    private val _currentLanguage = MutableStateFlow(LanguagePreference.AUTO)
    val currentLanguage: StateFlow<LanguagePreference> = _currentLanguage.asStateFlow()

    private val _detectedLanguage = MutableStateFlow(DetectedLanguage.UNKNOWN)
    val detectedLanguage: StateFlow<DetectedLanguage> = _detectedLanguage.asStateFlow()

    private val _vadActive = MutableStateFlow(false)
    val vadActive: StateFlow<Boolean> = _vadActive.asStateFlow()

    fun start(context: Context) {
        if (_isActive.value) return
        _isActive.value = true
        _lastError.value = null
        _state.value = NovaOrbState.CONNECTING
        _statusText.value = "Connecting to Gemini Live..."
        memory.clear()
        latencyTracker.reset()
        consecutiveReconnectFailures = 0

        InstalledAppLabelCache.prewarm(context)

        audioIO?.release()
        audioIO = LiveAudioIO(
            onMicChunk = { pcmChunk ->
                if (_isActive.value && !_muted.value && !EchoGuard.isMicMuted()) {
                    client?.sendAudio(pcmChunk)
                }
            },
            onMicAmplitude = { amp ->
                if (_state.value == NovaOrbState.LISTENING) {
                    _amplitude.value = amp
                    _vadActive.value = amp > 0.08f
                }
            }
        )

        scope.launch {
            connectSession(context, isResuming = false)
        }
    }

    private suspend fun connectSession(context: Context, isResuming: Boolean) {
        connectMutex.withLock {
            val epoch = ++sessionEpoch
            client?.disconnect()

            val apiKey = ApiKeyStore.getKey(context)
            if (apiKey.isEmpty()) {
                _state.value = NovaOrbState.ERROR
                _lastError.value = "API key missing. Enter key in Settings."
                _statusText.value = "API Key not configured"
                return@withLock
            }

            val repo = PersonalityRepository.getInstance(context)
            val personality = repo.getPersonality()
            val language = repo.getLanguage()
            val assistantName = repo.getAssistantName()
            val voiceName = repo.getVoiceName()

            _currentPersonality.value = personality
            _currentLanguage.value = language

            val recap = if (isResuming) memory.recap() else null
            val systemPrompt = PersonalityPromptBuilder.build(
                personality = personality,
                language = language,
                detectedLanguage = _detectedLanguage.value,
                recap = recap,
                assistantName = assistantName
            )

            _statusText.value = if (isResuming) "Reconnecting..." else "Warming up voice connection..."

            val liveListener = object : GeminiLiveClient.Listener {
                override fun onSetupComplete() {
                    if (epoch != sessionEpoch) return
                    consecutiveReconnectFailures = 0
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "$assistantName is listening..."
                    audioIO?.startRecording()
                }

                override fun onAudioChunk(bytes: ByteArray) {
                    if (epoch != sessionEpoch) return
                    val delta = latencyTracker.markFirstAudioChunk()
                    EchoGuard.notifyPlaybackStarted()
                    _state.value = NovaOrbState.SPEAKING
                    _statusText.value = "$assistantName is speaking..."

                    val amp = audioIO?.playChunk(bytes) ?: 0f
                    _amplitude.value = amp

                    // Re-arm 300ms silence watchdog
                    silenceWatchdogJob?.cancel()
                    silenceWatchdogJob = scope.launch {
                        delay(300)
                        if (epoch == sessionEpoch && _state.value == NovaOrbState.SPEAKING) {
                            EchoGuard.notifyPlaybackStopped()
                            _state.value = NovaOrbState.LISTENING
                            _statusText.value = "$assistantName is listening..."
                            _amplitude.value = 0f
                        }
                    }
                }

                override fun onInputTranscript(text: String) {
                    if (epoch != sessionEpoch) return
                    memory.addTurn(MemoryRole.USER, text)
                    latencyTracker.markUserSpeechEnd()
                    val detected = LanguageDetector.detect(text)
                    _detectedLanguage.value = detected
                    _state.value = NovaOrbState.THINKING
                    _statusText.value = "$assistantName is thinking..."
                }

                override fun onOutputTranscript(text: String) {
                    if (epoch != sessionEpoch) return
                    memory.addTurn(MemoryRole.ASSISTANT, text)
                }

                override fun onToolCall(id: String, name: String, args: JSONObject) {
                    if (epoch != sessionEpoch) return
                    _state.value = NovaOrbState.THINKING
                    _statusText.value = "Executing action..."

                    if (name == "build_website") {
                        val prompt = args.optString("prompt", "Awesome modern website")
                        ForgeController.start(context, prompt)
                        val resp = JSONObject().put("result", "forge_started")
                        client?.sendToolResponse(id, name, resp)
                        return
                    }

                    scope.launch {
                        val actionResult = ActionExecutor.execute(context, name, args)
                        if (epoch == sessionEpoch) {
                            client?.sendToolResponse(id, name, actionResult)
                        }
                    }
                }

                override fun onTurnComplete() {
                    if (epoch != sessionEpoch) return
                    silenceWatchdogJob?.cancel()
                    EchoGuard.notifyPlaybackStopped()
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "$assistantName is listening..."
                    _amplitude.value = 0f
                }

                override fun onInterrupted() {
                    if (epoch != sessionEpoch) return
                    silenceWatchdogJob?.cancel()
                    audioIO?.flushPlayback()
                    EchoGuard.notifyPlaybackStopped()
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "$assistantName is listening..."
                    _amplitude.value = 0f
                }

                override fun onError(message: String) {
                    if (epoch != sessionEpoch) return
                    if (_isActive.value) {
                        scheduleReconnect(context)
                    }
                }

                override fun onClosed() {
                    if (epoch != sessionEpoch) return
                    if (_isActive.value) {
                        scheduleReconnect(context)
                    }
                }
            }

            val newClient = GeminiLiveClient(apiKey, liveListener)
            client = newClient
            newClient.connect(systemPrompt, voiceName)
        }
    }

    private fun scheduleReconnect(context: Context) {
        scope.launch {
            reconnectMutex.withLock {
                if (!_isActive.value) return@withLock
                if (reconnectJob?.isActive == true) return@withLock

                consecutiveReconnectFailures++
                latencyTracker.incrementReconnect()

                if (consecutiveReconnectFailures > 5) {
                    _state.value = NovaOrbState.ERROR
                    _lastError.value = "Connection lost - tap Stop and reopen Voice Mode"
                    _statusText.value = "Connection lost"
                    return@withLock
                }

                _state.value = NovaOrbState.CONNECTING
                _statusText.value = "Reconnecting..."

                reconnectJob = scope.launch {
                    delay(1200)
                    connectSession(context, isResuming = true)
                }
            }
        }
    }

    fun refreshInstructions(context: Context) {
        if (_isActive.value) {
            scope.launch {
                connectSession(context, isResuming = true)
            }
        }
    }

    fun toggleMute() {
        _muted.value = !_muted.value
    }

    fun stop() {
        sessionEpoch++
        _isActive.value = false
        reconnectJob?.cancel()
        silenceWatchdogJob?.cancel()
        client?.disconnect()
        client = null
        audioIO?.release()
        audioIO = null
        EchoGuard.reset()
        memory.clear()
        _state.value = NovaOrbState.IDLE
        _statusText.value = "Voice session stopped"
        _amplitude.value = 0f
    }
}
