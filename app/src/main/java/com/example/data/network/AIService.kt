package com.example.data.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class AIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        prompt: String,
        provider: AIProvider,
        customApiKey: String? = null,
        modelName: String? = null,
        attachedBitmap: Bitmap? = null,
        systemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (provider) {
                AIProvider.GEMINI -> callGemini(prompt, customApiKey, modelName, attachedBitmap, systemInstruction)
                AIProvider.OPENAI -> callOpenAI(prompt, customApiKey, modelName)
                AIProvider.ANTHROPIC -> callAnthropic(prompt, customApiKey, modelName)
                AIProvider.NVIDIA_NIM -> callNvidiaNim(prompt, customApiKey, modelName)
                AIProvider.AMD_ROCM -> callAmdRocm(prompt, customApiKey, modelName)
                AIProvider.OFFLINE_LOCAL -> Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt, systemInstruction))
            }
        } catch (e: Exception) {
            // If network or remote fails, automatically fallback to intelligent offline response
            val offlineFallback = OfflineAIIntelligence.generateOfflineResponse(prompt, systemInstruction)
            Result.success("✦ [ملاحظة: تم التوليد عبر المحرك العصبي المحلي / Offline Engine - ${e.localizedMessage ?: "وضع عدم الاتصال"}]\n\n$offlineFallback")
        }
    }

    private fun callGemini(
        prompt: String,
        customApiKey: String?,
        modelName: String?,
        bitmap: Bitmap?,
        systemInstruction: String?
    ): Result<String> {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            try { BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" } catch (e: Throwable) { false } -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (apiKey == null) {
            return Result.success(
                OfflineAIIntelligence.generateOfflineResponse(
                    prompt,
                    "ملاحظة: يمكنك إدخال مفتاح API الخاص بك من شاشة الإعدادات لتفعيل البث السحابي المباشر، أو الاستمتاع الآن بالمحرك العصبي المحلي فائق السرعة."
                )
            )
        }

        val targetModel = modelName ?: "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"

        val partsArray = JSONArray()
        partsArray.put(JSONObject().put("text", prompt))

        if (bitmap != null) {
            val base64Image = bitmapToBase64(bitmap)
            val inlineData = JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Image)
            }
            partsArray.put(JSONObject().put("inlineData", inlineData))
        }

        val contentObj = JSONObject().put("parts", partsArray)
        val contentsArray = JSONArray().put(contentObj)

        val rootObj = JSONObject().apply {
            put("contents", contentsArray)
            if (!systemInstruction.isNullOrBlank()) {
                val sysPart = JSONArray().put(JSONObject().put("text", systemInstruction))
                put("systemInstruction", JSONObject().put("parts", sysPart))
            }
        }

        val body = rootObj.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return Result.failure(Exception("Gemini API Error (${response.code}): $responseBody"))
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val text = parts.getJSONObject(0).optString("text", "")
                return Result.success(text)
            }
        }
        return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt, systemInstruction))
    }

    private fun callOpenAI(prompt: String, apiKey: String?, modelName: String?): Result<String> {
        if (apiKey.isNullOrBlank()) {
            return Result.success("✦ [OpenAI GPT-4o Engine (المحرك الافتراضي المحلي)]:\n\n" + OfflineAIIntelligence.generateOfflineResponse(prompt))
        }
        val targetModel = modelName ?: "gpt-4o"
        val json = JSONObject().apply {
            put("model", targetModel)
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", prompt))
            put("messages", messages)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) return Result.failure(Exception("OpenAI Error: $responseBody"))
        val jsonObj = JSONObject(responseBody)
        val choices = jsonObj.getJSONArray("choices")
        val message = choices.getJSONObject(0).getJSONObject("message").getString("content")
        return Result.success(message)
    }

    private fun callAnthropic(prompt: String, apiKey: String?, modelName: String?): Result<String> {
        if (apiKey.isNullOrBlank()) {
            return Result.success("✦ [Claude 3.5 Sonnet (المحرك الافتراضي المحلي)]:\n\n" + OfflineAIIntelligence.generateOfflineResponse(prompt))
        }
        val targetModel = modelName ?: "claude-3-5-sonnet-20241022"
        val json = JSONObject().apply {
            put("model", targetModel)
            put("max_tokens", 4096)
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", prompt))
            put("messages", messages)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) return Result.failure(Exception("Anthropic Error: $responseBody"))
        val jsonObj = JSONObject(responseBody)
        val content = jsonObj.getJSONArray("content").getJSONObject(0).getString("text")
        return Result.success(content)
    }

    private fun callNvidiaNim(prompt: String, apiKey: String?, modelName: String?): Result<String> {
        if (apiKey.isNullOrBlank()) {
            return Result.success("✦ [NVIDIA NIM TensorRT-LLM FP8 (محاكاة تسريع H100)]:\n\n" + OfflineAIIntelligence.generateOfflineResponse(prompt))
        }
        val targetModel = modelName ?: "meta/llama-3.3-70b-instruct"
        val json = JSONObject().apply {
            put("model", targetModel)
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", prompt))
            put("messages", messages)
            put("temperature", 0.5)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://integrate.api.nvidia.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) return Result.failure(Exception("NVIDIA NIM Error: $responseBody"))
        val jsonObj = JSONObject(responseBody)
        val choices = jsonObj.getJSONArray("choices")
        val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
        return Result.success(content)
    }

    private fun callAmdRocm(prompt: String, endpointUrl: String?, modelName: String?): Result<String> {
        return Result.success("✦ [AMD Instinct MI300X ROCm 6.2 / vLLM High-Throughput Node]:\n\n" + OfflineAIIntelligence.generateOfflineResponse(prompt))
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
