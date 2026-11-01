package com.edith.assistant.voice

import android.util.Base64
import com.edith.assistant.action.edithToolCatalog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val apiKey: String,
    private val listener: Listener
) {
    interface Listener {
        fun onSetupComplete()
        fun onAudioChunk(bytes: ByteArray)
        fun onInputTranscript(text: String)
        fun onOutputTranscript(text: String)
        fun onToolCall(id: String, name: String, args: JSONObject)
        fun onTurnComplete()
        fun onInterrupted()
        fun onError(message: String)
        fun onClosed()
    }

    companion object {
        const val MODEL = "models/gemini-2.5-flash-live-preview"
        private const val WS_BASE =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key="
    }

    private val deduplicator = ToolCallDeduplicator()
    private var webSocket: WebSocket? = null
    private var isConnected = false

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect(systemInstruction: String, voiceName: String = "Aoede") {
        disconnect()
        deduplicator.clear()

        val url = "$WS_BASE$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                sendSetupMessage(ws, systemInstruction, voiceName)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                handleIncomingMessage(bytes.utf8())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                val msg = t.message ?: "Live connection failed"
                listener.onError(msg)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                listener.onClosed()
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket, systemInstruction: String, voiceName: String) {
        val root = JSONObject()
        val setup = JSONObject()
        setup.put("model", MODEL)

        val genConfig = JSONObject()
        val modalities = JSONArray()
        modalities.put("AUDIO")
        genConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", voiceName.ifBlank { "Aoede" })
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        genConfig.put("speechConfig", speechConfig)

        setup.put("generationConfig", genConfig)

        val sysInst = JSONObject()
        val parts = JSONArray()
        val partText = JSONObject()
        partText.put("text", systemInstruction)
        parts.put(partText)
        sysInst.put("parts", parts)
        setup.put("systemInstruction", sysInst)

        setup.put("inputAudioTranscription", JSONObject())
        setup.put("outputAudioTranscription", JSONObject())

        val toolsArray = JSONArray()
        val toolObject = JSONObject()
        toolObject.put("functionDeclarations", edithToolCatalog.declarations())
        toolsArray.put(toolObject)
        setup.put("tools", toolsArray)

        root.put("setup", setup)
        ws.send(root.toString())
    }

    fun sendAudio(pcm16Bytes: ByteArray) {
        val ws = webSocket ?: return
        if (!isConnected) return

        val base64Data = Base64.encodeToString(pcm16Bytes, Base64.NO_WRAP)
        val root = JSONObject()
        val realtimeInput = JSONObject()
        val audio = JSONObject()
        audio.put("mimeType", "audio/pcm;rate=16000")
        audio.put("data", base64Data)
        realtimeInput.put("audio", audio)
        root.put("realtimeInput", realtimeInput)

        ws.send(root.toString())
    }

    fun sendToolResponse(callId: String, functionName: String, responseData: JSONObject) {
        val ws = webSocket ?: return
        if (!isConnected) return

        val root = JSONObject()
        val toolResponse = JSONObject()
        val functionResponses = JSONArray()
        val fr = JSONObject()
        fr.put("id", callId)
        fr.put("name", functionName)
        fr.put("response", responseData)
        functionResponses.put(fr)
        toolResponse.put("functionResponses", functionResponses)
        root.put("toolResponse", toolResponse)

        ws.send(root.toString())
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)

            // 1. Setup complete
            if (json.has("setupComplete")) {
                listener.onSetupComplete()
                return
            }

            // 2. Tool calls
            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                if (toolCall.has("functionCalls")) {
                    val calls = toolCall.getJSONArray("functionCalls")
                    for (i in 0 until calls.length()) {
                        val call = calls.getJSONObject(i)
                        val id = call.optString("id", "")
                        val name = call.optString("name", "")
                        val args = call.optJSONObject("args") ?: JSONObject()

                        if (deduplicator.isNew(id)) {
                            listener.onToolCall(id, name, args)
                        }
                    }
                }
            }

            // 3. Server content
            if (json.has("serverContent")) {
                val sc = json.getJSONObject("serverContent")

                // Interrupted
                if (sc.optBoolean("interrupted", false)) {
                    listener.onInterrupted()
                }

                // Model turn (audio / text)
                if (sc.has("modelTurn")) {
                    val mt = sc.getJSONObject("modelTurn")
                    val parts = mt.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val data = inlineData.optString("data", "")
                                if (data.isNotEmpty()) {
                                    val bytes = Base64.decode(data, Base64.DEFAULT)
                                    listener.onAudioChunk(bytes)
                                }
                            }
                            if (part.has("text")) {
                                val t = part.optString("text", "")
                                if (t.isNotEmpty()) {
                                    listener.onOutputTranscript(t)
                                }
                            }
                        }
                    }
                }

                // Input audio transcription
                if (sc.has("inputAudioTranscription")) {
                    val it = sc.getJSONObject("inputAudioTranscription")
                    val transcript = it.optString("text", "")
                    if (transcript.isNotEmpty()) {
                        listener.onInputTranscript(transcript)
                    }
                }

                // Output transcription
                if (sc.has("outputAudioTranscription")) {
                    val ot = sc.getJSONObject("outputAudioTranscription")
                    val transcript = ot.optString("text", "")
                    if (transcript.isNotEmpty()) {
                        listener.onOutputTranscript(transcript)
                    }
                }

                // Turn complete
                if (sc.optBoolean("turnComplete", false)) {
                    listener.onTurnComplete()
                }
            }
        } catch (_: Exception) {
            // Silently ignore unrecognized or malformed payloads
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {}
        webSocket = null
        isConnected = false
    }
}
