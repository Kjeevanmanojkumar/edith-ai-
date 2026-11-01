package com.edith.assistant.voice

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatTurn(
    val isUser: Boolean,
    val text: String
)

sealed class ChatReplyResult {
    data class Success(val text: String) : ChatReplyResult()
    data class Error(val message: String) : ChatReplyResult()
}

object GeminiTextClient {
    private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta"

    @Volatile
    private var cachedModelName: String? = null

    internal val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    internal suspend fun resolveModel(apiKey: String): String {
        cachedModelName?.let { return it }

        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) return "models/gemini-2.5-flash"

        val url = "$API_BASE/models?key=$trimmedKey"
        val request = Request.Builder().url(url).get().build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return "models/gemini-2.5-flash"
            }
            val body = response.body?.string() ?: return "models/gemini-2.5-flash"
            val json = JSONObject(body)
            val modelsArray = json.optJSONArray("models") ?: return "models/gemini-2.5-flash"

            var bestModel: String? = null
            var firstGenerativeModel: String? = null

            for (i in 0 until modelsArray.length()) {
                val modelObj = modelsArray.getJSONObject(i)
                val name = modelObj.optString("name", "") // e.g. "models/gemini-2.5-flash"
                val methods = modelObj.optJSONArray("supportedGenerationMethods")
                val supportsGen = methods != null && (0 until methods.length()).any {
                    methods.getString(it) == "generateContent"
                }

                if (supportsGen) {
                    if (firstGenerativeModel == null) {
                        firstGenerativeModel = name
                    }
                    val lower = name.lowercase()
                    val isFlash = lower.contains("flash")
                    val isExcluded = lower.contains("live") || lower.contains("image") || lower.contains("tts")
                    if (isFlash && !isExcluded) {
                        bestModel = name
                        break
                    }
                }
            }

            val chosen = bestModel ?: firstGenerativeModel ?: "models/gemini-2.5-flash"
            cachedModelName = chosen
            chosen
        } catch (_: Exception) {
            "models/gemini-2.5-flash"
        }
    }

    suspend fun reply(
        apiKey: String,
        systemInstruction: String,
        history: List<ChatTurn>
    ): ChatReplyResult {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return ChatReplyResult.Error("Gemini API key is not configured. Please add your key in Settings.")
        }
        if (history.isEmpty()) {
            return ChatReplyResult.Error("No message to send.")
        }

        val model = resolveModel(trimmedKey)
        val url = "$API_BASE/$model:generateContent?key=$trimmedKey"

        val root = JSONObject()

        // systemInstruction
        if (systemInstruction.isNotBlank()) {
            val sysObj = JSONObject()
            val parts = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", systemInstruction)
            parts.put(textPart)
            sysObj.put("parts", parts)
            root.put("systemInstruction", sysObj)
        }

        // contents
        val contentsArray = JSONArray()
        for (turn in history) {
            val contentObj = JSONObject()
            contentObj.put("role", if (turn.isUser) "user" else "model")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", turn.text)
            parts.put(part)
            contentObj.put("parts", parts)
            contentsArray.put(contentObj)
        }
        root.put("contents", contentsArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = root.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val errorReason = try {
                    val errJson = JSONObject(errorBody).optJSONObject("error")
                    errJson?.optString("message", "Status ${response.code}") ?: "Status ${response.code}"
                } catch (_: Exception) {
                    "Status ${response.code}"
                }
                return ChatReplyResult.Error("Gemini API error ($errorReason)")
            }

            val responseString = response.body?.string() ?: ""
            val json = JSONObject(responseString)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return ChatReplyResult.Error("No response generated by Gemini.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                val finishReason = firstCandidate.optString("finishReason", "empty")
                return ChatReplyResult.Error("Response was empty or filtered ($finishReason)")
            }

            val text = parts.getJSONObject(0).optString("text", "").trim()
            if (text.isEmpty()) {
                ChatReplyResult.Error("Response was empty.")
            } else {
                ChatReplyResult.Success(text)
            }
        } catch (e: IOException) {
            ChatReplyResult.Error("Couldn't reach Gemini. Please check your internet connection.")
        } catch (e: Exception) {
            ChatReplyResult.Error("Error: ${e.message ?: "Failed to generate reply"}")
        }
    }
}
