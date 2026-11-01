package com.edith.assistant.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

object EchoGuard {
    private const val ECHO_TAIL_MS = 450L
    private const val WATCHDOG_TIMEOUT_MS = 60_000L

    private val playbackCounter = AtomicInteger(0)
    @Volatile
    private var isMuted = false

    private val scope = CoroutineScope(Dispatchers.Default)
    private var unmuteJob: Job? = null
    private var watchdogJob: Job? = null

    @Synchronized
    fun notifyPlaybackStarted() {
        val count = playbackCounter.incrementAndGet()
        isMuted = true
        unmuteJob?.cancel()

        // Start 60s watchdog to force-unmute if playback somehow hung
        if (count == 1) {
            watchdogJob?.cancel()
            watchdogJob = scope.launch {
                delay(WATCHDOG_TIMEOUT_MS)
                forceUnmute()
            }
        }
    }

    @Synchronized
    fun notifyPlaybackStopped() {
        val count = playbackCounter.decrementAndGet()
        if (count <= 0) {
            playbackCounter.set(0)
            watchdogJob?.cancel()
            unmuteJob?.cancel()
            unmuteJob = scope.launch {
                delay(ECHO_TAIL_MS)
                isMuted = false
            }
        }
    }

    @Synchronized
    private fun forceUnmute() {
        playbackCounter.set(0)
        isMuted = false
        watchdogJob?.cancel()
        unmuteJob?.cancel()
    }

    fun isMicMuted(): Boolean = isMuted

    @Synchronized
    fun reset() {
        playbackCounter.set(0)
        isMuted = false
        watchdogJob?.cancel()
        unmuteJob?.cancel()
    }
}
