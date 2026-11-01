package com.edith.assistant.forge

import android.content.Context
import com.edith.assistant.voice.GeminiTextClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class ForgeStreamEvent {
    data class Chunk(val textChunk: String, val cumulativeHtml: String) : ForgeStreamEvent()
    data class Complete(val finalHtml: String, val savedFile: File) : ForgeStreamEvent()
    data class Error(val message: String) : ForgeStreamEvent()
}

class WebsiteForgeClient(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun streamSite(apiKey: String, prompt: String, isEdit: Boolean = false, currentHtml: String = ""): Flow<ForgeStreamEvent> = flow {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            emit(ForgeStreamEvent.Error("API key is not configured. Please add your Gemini key in Settings."))
            return@flow
        }

        val model = GeminiTextClient.resolveModel(trimmedKey)
        val url = "https://generativelanguage.googleapis.com/v1beta/$model:streamGenerateContent?alt=sse"

        val promptText = if (isEdit) {
            ForgeSystemPrompt.forEdit(currentHtml, prompt)
        } else {
            ForgeSystemPrompt.forNewSite(prompt)
        }

        val requestJson = JSONObject()
        val contents = JSONArray()
        val contentObj = JSONObject()
        contentObj.put("role", "user")
        val parts = JSONArray()
        val part = JSONObject()
        part.put("text", promptText)
        parts.put(part)
        contentObj.put("parts", parts)
        contents.put(contentObj)
        requestJson.put("contents", contents)

        val genConfig = JSONObject()
        if (model.contains("2.5")) {
            val thinkingConfig = JSONObject()
            thinkingConfig.put("thinkingBudget", 0)
            genConfig.put("thinkingConfig", thinkingConfig)
        }
        requestJson.put("generationConfig", genConfig)

        var cumulative = StringBuilder()
        var retryCount = 0
        var success = false

        while (!success && retryCount < 2) {
            cumulative = StringBuilder()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .header("x-goog-api-key", trimmedKey)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    if (response.code == 503 && retryCount == 0) {
                        retryCount++
                        delay(4000)
                        continue
                    }
                    emit(ForgeStreamEvent.Error("Gemini server error (Status ${response.code})"))
                    return@flow
                }

                val stream = response.body?.byteStream()
                if (stream == null) {
                    emit(ForgeStreamEvent.Error("Empty stream received from Gemini"))
                    return@flow
                }

                val reader = BufferedReader(InputStreamReader(stream))
                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data:")) {
                        val dataJson = line.substring(5).trim()
                        if (dataJson.isNotEmpty() && dataJson != "[DONE]") {
                            try {
                                val obj = JSONObject(dataJson)
                                val candidates = obj.optJSONArray("candidates")
                                if (candidates != null && candidates.length() > 0) {
                                    val candidate = candidates.getJSONObject(0)
                                    val cContent = candidate.optJSONObject("content")
                                    val cParts = cContent?.optJSONArray("parts")
                                    if (cParts != null && cParts.length() > 0) {
                                        val chunk = cParts.getJSONObject(0).optString("text", "")
                                        if (chunk.isNotEmpty()) {
                                            cumulative.append(chunk)
                                            emit(ForgeStreamEvent.Chunk(chunk, cumulative.toString()))
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    line = reader.readLine()
                }

                val rawHtml = cumulative.toString()
                val stripped = ForgeHtml.stripFences(rawHtml)

                if (stripped.length < 1500 && retryCount == 0) {
                    retryCount++
                    delay(2000)
                    continue
                }

                val finalHtml = ForgeHtml.withSafetyNet(ForgeHtml.repairTruncated(stripped))

                // Save to filesDir/Websites/website_<timestamp>.html
                val websitesDir = File(context.filesDir, "Websites")
                if (!websitesDir.exists()) websitesDir.mkdirs()
                val outFile = File(websitesDir, "website_${System.currentTimeMillis()}.html")
                outFile.writeText(finalHtml)

                success = true
                emit(ForgeStreamEvent.Complete(finalHtml, outFile))

            } catch (e: Exception) {
                if (retryCount == 0) {
                    retryCount++
                    delay(3000)
                } else {
                    emit(ForgeStreamEvent.Error("Forge network error: ${e.message ?: "Failed stream"}"))
                    return@flow
                }
            }
        }
    }
}
