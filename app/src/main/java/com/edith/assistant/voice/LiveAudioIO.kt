package com.edith.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

class LiveAudioIO(
    private val onMicChunk: (ByteArray) -> Unit,
    private val onMicAmplitude: (Float) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var recordThread: Thread? = null

    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null

    companion object {
        fun computeRms(pcm16: ByteArray): Float {
            if (pcm16.isEmpty()) return 0f
            val shorts = ShortArray(pcm16.size / 2)
            ByteBuffer.wrap(pcm16).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            var sumSquare = 0.0
            for (s in shorts) {
                sumSquare += (s * s).toDouble()
            }
            val mean = sumSquare / shorts.size
            val rms = sqrt(mean) / 32768.0
            return rms.toFloat().coerceIn(0f, 1f)
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufSize = max(minBuf, 4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufSize
            )

            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0) {
                try {
                    if (AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}

                try {
                    if (NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}

                try {
                    if (AutomaticGainControl.isAvailable()) {
                        gainControl = AutomaticGainControl.create(sessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}
            }

            audioRecord?.startRecording()
            isRecording = true

            recordThread = Thread({
                val buffer = ByteArray(2048)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val chunk = if (read == buffer.size) buffer.copyOf() else buffer.copyOfRange(0, read)
                        val amp = computeRms(chunk)
                        onMicAmplitude(amp)
                        onMicChunk(chunk)
                    }
                }
            }, "LiveAudioMicThread").apply { start() }

        } catch (e: Exception) {
            isRecording = false
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        recordThread = null

        try { echoCanceler?.release() } catch (_: Exception) {}
        try { noiseSuppressor?.release() } catch (_: Exception) {}
        try { gainControl?.release() } catch (_: Exception) {}
        echoCanceler = null
        noiseSuppressor = null
        gainControl = null
    }

    @Synchronized
    private fun getOrCreateAudioTrack(): AudioTrack? {
        if (audioTrack != null) return audioTrack

        val sampleRate = 24000
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBuf * 4, 8192)

        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            audioTrack = track
            track
        } catch (_: Exception) {
            null
        }
    }

    fun playChunk(bytes: ByteArray): Float {
        val track = getOrCreateAudioTrack() ?: return 0f
        track.write(bytes, 0, bytes.size)
        return computeRms(bytes)
    }

    fun flushPlayback() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (_: Exception) {}
    }

    fun setVolume(vol: Float) {
        try {
            val clamped = vol.coerceIn(0f, 1f)
            audioTrack?.setVolume(clamped)
        } catch (_: Exception) {}
    }

    fun release() {
        stopRecording()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
