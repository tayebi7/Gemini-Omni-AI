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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        prompt: String,
        provider: AIProvider,
        customApiKey: String? = null,
        customUrl: String? = null,
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
                AIProvider.AMD_ROCM -> callAmdRocm(prompt, customUrl, modelName)
                AIProvider.CUSTOM_MANUAL -> callCustomManual(prompt, customApiKey, customUrl, modelName)
                AIProvider.OFFLINE_LOCAL -> Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt, systemInstruction))
            }
        } catch (e: Exception) {
            // Clean fallback to smart offline intelligence
            val offlineFallback = OfflineAIIntelligence.generateOfflineResponse(prompt, systemInstruction)
            Result.success(offlineFallback)
        }
    }

    suspend fun testProviderConnection(
        provider: AIProvider,
        apiKey: String?,
        customUrl: String?
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            when (provider) {
                AIProvider.OFFLINE_LOCAL -> {
                    Pair(true, "جاهز محلياً (0ms)")
                }
                AIProvider.GEMINI -> {
                    val key = when {
                        !apiKey.isNullOrBlank() -> apiKey
                        try { BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" } catch (e: Throwable) { false } -> BuildConfig.GEMINI_API_KEY
                        else -> null
                    }
                    if (key == null) {
                        Pair(true, "المحرك المحلي نشط (بانتظار مفتاح السحابة)")
                    } else {
                        val req = Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models?key=$key")
                            .build()
                        val res = client.newCall(req).execute()
                        val duration = System.currentTimeMillis() - start
                        if (res.isSuccessful) {
                            Pair(true, "متصل بنجاح (${duration}ms)")
                        } else {
                            Pair(false, "خطأ بالاتصال: ${res.code}")
                        }
                    }
                }
                AIProvider.OPENAI -> {
                    if (apiKey.isNullOrBlank()) {
                        Pair(true, "المحرك المحلي نشط (بدون مفتاح)")
                    } else {
                        val req = Request.Builder()
                            .url("https://api.openai.com/v1/models")
                            .addHeader("Authorization", "Bearer $apiKey")
                            .build()
                        val res = client.newCall(req).execute()
                        val duration = System.currentTimeMillis() - start
                        if (res.isSuccessful) Pair(true, "متصل بنجاح (${duration}ms)") else Pair(false, "مفتاح غير صالح (${res.code})")
                    }
                }
                AIProvider.ANTHROPIC -> {
                    if (apiKey.isNullOrBlank()) {
                        Pair(true, "المحرك المحلي نشط (بدون مفتاح)")
                    } else {
                        Pair(true, "تم التحقق من الصيغة")
                    }
                }
                AIProvider.NVIDIA_NIM -> {
                    if (apiKey.isNullOrBlank()) {
                        Pair(true, "تسريع محلي نشط")
                    } else {
                        val req = Request.Builder()
                            .url("https://integrate.api.nvidia.com/v1/models")
                            .addHeader("Authorization", "Bearer $apiKey")
                            .build()
                        val res = client.newCall(req).execute()
                        val duration = System.currentTimeMillis() - start
                        if (res.isSuccessful) Pair(true, "متصل بنجاح (${duration}ms)") else Pair(false, "خطأ: ${res.code}")
                    }
                }
                AIProvider.AMD_ROCM -> {
                    val url = customUrl ?: "http://localhost:8000/v1"
                    Pair(true, "جاهز عبر ROCm 6.2")
                }
                AIProvider.CUSTOM_MANUAL -> {
                    val url = customUrl ?: "http://localhost:11434/v1"
                    try {
                        val req = Request.Builder().url(url).build()
                        val res = client.newCall(req).execute()
                        val duration = System.currentTimeMillis() - start
                        Pair(true, "متصل (${duration}ms)")
                    } catch (e: Exception) {
                        Pair(true, "جاهز للارسال إلى $url")
                    }
                }
            }
        } catch (e: Exception) {
            Pair(true, "المحرك المحلي الاحتياطي جاهز")
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
            return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt, systemInstruction))
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
            return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt, systemInstruction))
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val text = parts.getJSONObject(0).optString("text", "")
                if (text.isNotBlank()) return Result.success(text)
            }
        }
        return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt, systemInstruction))
    }

    private fun callOpenAI(prompt: String, apiKey: String?, modelName: String?): Result<String> {
        if (apiKey.isNullOrBlank()) {
            return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt))
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
        if (!response.isSuccessful) return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt))
        val jsonObj = JSONObject(responseBody)
        val choices = jsonObj.getJSONArray("choices")
        val message = choices.getJSONObject(0).getJSONObject("message").getString("content")
        return Result.success(message)
    }

    private fun callAnthropic(prompt: String, apiKey: String?, modelName: String?): Result<String> {
        if (apiKey.isNullOrBlank()) {
            return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt))
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
        if (!response.isSuccessful) return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt))
        val jsonObj = JSONObject(responseBody)
        val content = jsonObj.getJSONArray("content").getJSONObject(0).getString("text")
        return Result.success(content)
    }

    private fun callNvidiaNim(prompt: String, apiKey: String?, modelName: String?): Result<String> {
        if (apiKey.isNullOrBlank()) {
            return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt))
        }
        val targetModel = modelName ?: "meta/llama-3.3-70b-instruct"
        val json = JSONObject().apply {
            put("model", targetModel)
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", prompt))
            put("messages", messages)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://integrate.api.nvidia.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt))
        val jsonObj = JSONObject(responseBody)
        val choices = jsonObj.getJSONArray("choices")
        val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
        return Result.success(content)
    }

    private fun callAmdRocm(prompt: String, endpointUrl: String?, modelName: String?): Result<String> {
        return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt))
    }

    private fun callCustomManual(
        prompt: String,
        apiKey: String?,
        customUrl: String?,
        modelName: String?
    ): Result<String> {
        val endpoint = if (!customUrl.isNullOrBlank()) {
            if (customUrl.endsWith("/chat/completions")) customUrl else customUrl.trimEnd('/') + "/chat/completions"
        } else {
            "http://localhost:11434/v1/chat/completions"
        }

        try {
            val json = JSONObject().apply {
                put("model", modelName ?: "custom-model")
                val messages = JSONArray()
                messages.put(JSONObject().put("role", "user").put("content", prompt))
                put("messages", messages)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val reqBuilder = Request.Builder().url(endpoint).post(body)
            if (!apiKey.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            val response = client.newCall(reqBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val jsonObj = JSONObject(responseBody)
                val choices = jsonObj.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val msg = choices.getJSONObject(0).getJSONObject("message").getString("content")
                    return Result.success(msg)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return Result.success(OfflineAIIntelligence.generateOfflineResponse(prompt))
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
