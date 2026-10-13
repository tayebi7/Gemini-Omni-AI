package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppLanguage {
    ARABIC,
    ENGLISH
}

enum class AIProvider(
    val displayName: String,
    val defaultModel: String,
    val availableModels: List<String>,
    val defaultGpuMode: String
) {
    GEMINI(
        displayName = "Google Gemini",
        defaultModel = "gemini-3.5-flash",
        availableModels = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-2.5-flash-image"),
        defaultGpuMode = "Google TPU v5e / Cloud Acceleration"
    ),
    OPENAI(
        displayName = "OpenAI",
        defaultModel = "gpt-4o",
        availableModels = listOf("gpt-4o", "gpt-4o-mini", "o1-preview"),
        defaultGpuMode = "Azure NDv4 GPU Cloud"
    ),
    ANTHROPIC(
        displayName = "Anthropic",
        defaultModel = "claude-3-5-sonnet",
        availableModels = listOf("claude-3-5-sonnet", "claude-3-5-haiku", "claude-3-opus"),
        defaultGpuMode = "AWS Trainium / Claude Cloud"
    ),
    NVIDIA_NIM(
        displayName = "NVIDIA NIM",
        defaultModel = "meta/llama-3.3-70b-instruct",
        availableModels = listOf("meta/llama-3.3-70b-instruct", "mistralai/mistral-nemo", "nvidia/nemotron-4-340b"),
        defaultGpuMode = "NVIDIA TensorRT-LLM FP8 (H100 / L40S)"
    ),
    AMD_ROCM(
        displayName = "AMD ROCm / vLLM",
        defaultModel = "deepseek-ai/DeepSeek-V2.5",
        availableModels = listOf("deepseek-ai/DeepSeek-V2.5", "meta-llama/Llama-3.1-70B", "Qwen/Qwen2.5-Coder"),
        defaultGpuMode = "AMD Instinct MI300X (ROCm 6.2 / vLLM)"
    ),
    OFFLINE_LOCAL(
        displayName = "Offline Neural Engine",
        defaultModel = "gemini-omni-edge-v1",
        availableModels = listOf("gemini-omni-edge-v1", "phi-3-mini-edge", "qwen-coder-local"),
        defaultGpuMode = "On-Device NPU / Local GPU Acceleration"
    )
}

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessagePreview: String = "",
    val providerUsed: String = AIProvider.GEMINI.name
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedImageUri: String? = null,
    val modelUsed: String = "gemini-3.5-flash",
    val providerUsed: String = AIProvider.GEMINI.name,
    val tokensCount: Int = 0,
    val isOfflineGenerated: Boolean = false
)

enum class DocumentFormat(val extension: String, val mime: String) {
    MARKDOWN(".md", "text/markdown"),
    PDF(".pdf", "application/pdf"),
    DOCX(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    TXT(".txt", "text/plain"),
    JSON(".json", "application/json"),
    HTML(".html", "text/html")
}

enum class DocumentType {
    RESEARCH,
    STORY,
    PRODUCT_BRIEF,
    TECHNICAL_SPEC,
    EXECUTIVE_SUMMARY
}

@Entity(tableName = "documents")
data class DocumentItem(
    @PrimaryKey val id: String,
    val title: String,
    val type: DocumentType,
    val content: String,
    val format: DocumentFormat = DocumentFormat.MARKDOWN,
    val createdAt: Long = System.currentTimeMillis(),
    val isOfflineAvailable: Boolean = true,
    val tags: String = ""
)

enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    REVIEW,
    DONE
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Entity(tableName = "workflow_tasks")
data class WorkflowTask(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String, // "Product", "Analytics", "Development", "Marketing"
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assignedAgent: String = "Product Manager AI",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scheduled_posts")
data class ScheduledPost(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val platform: String, // "Instagram", "TikTok", "LinkedIn", "YouTube", "Twitter"
    val scheduledTime: String,
    val status: String = "SCHEDULED", // "SCHEDULED", "PUBLISHED", "DRAFT"
    val hook: String = "",
    val visualPrompt: String = "",
    val engagementRateEstimate: String = "94%"
)

data class AIAgent(
    val id: String,
    val name: String,
    val roleTitle: String,
    val avatarEmoji: String,
    val specialty: String,
    val description: String,
    val systemPrompt: String,
    val isAvailable: Boolean = true
)

data class JobOpportunity(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val matchScore: Int, // e.g. 96%
    val requiredSkills: List<String>,
    val salaryRange: String,
    val description: String,
    val coverLetterTip: String
)

data class CodeProject(
    val id: String,
    val title: String,
    val language: String,
    val code: String,
    val explanation: String,
    val solutions: List<String>
)

data class IntegrationConfig(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val isConnected: Boolean,
    val statusText: String,
    val syncDetails: String
)
