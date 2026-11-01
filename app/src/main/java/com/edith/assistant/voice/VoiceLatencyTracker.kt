package com.edith.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceLatencyTracker {
    private val _reconnectCount = MutableStateFlow(0)
    val reconnectCount: StateFlow<Int> = _reconnectCount.asStateFlow()

    private val _lastTurnLatencyMs = MutableStateFlow(0L)
    val lastTurnLatencyMs: StateFlow<Long> = _lastTurnLatencyMs.asStateFlow()

    private var speechEndTimestamp: Long = 0L
    private var firstResponseAudioTimestamp: Long = 0L

    @Synchronized
    fun markUserSpeechEnd(timestamp: Long = System.currentTimeMillis()) {
        speechEndTimestamp = timestamp
        firstResponseAudioTimestamp = 0L
    }

    @Synchronized
    fun markFirstAudioChunk(timestamp: Long = System.currentTimeMillis()): Long {
        if (speechEndTimestamp > 0L && firstResponseAudioTimestamp == 0L && timestamp >= speechEndTimestamp) {
            firstResponseAudioTimestamp = timestamp
            val delta = timestamp - speechEndTimestamp
            _lastTurnLatencyMs.value = delta
            return delta
        }
        return -1L
    }

    @Synchronized
    fun incrementReconnect() {
        _reconnectCount.value += 1
    }

    @Synchronized
    fun resetReconnect() {
        _reconnectCount.value = 0
    }

    @Synchronized
    fun reset() {
        speechEndTimestamp = 0L
        firstResponseAudioTimestamp = 0L
        _lastTurnLatencyMs.value = 0L
        _reconnectCount.value = 0
    }
}
