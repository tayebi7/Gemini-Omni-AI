package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.network.AIService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AppRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val chatDao = database.chatDao()
    private val documentDao = database.documentDao()
    private val taskDao = database.taskDao()
    val aiService = AIService()

    // Preferences & Settings in Memory with sensible defaults
    private val prefs = context.getSharedPreferences("gemini_omni_prefs", Context.MODE_PRIVATE)

    private val _currentProvider = MutableStateFlow(
        try {
            AIProvider.valueOf(prefs.getString("active_provider", AIProvider.GEMINI.name) ?: AIProvider.GEMINI.name)
        } catch (e: Exception) {
            AIProvider.GEMINI
        }
    )
    val currentProvider = _currentProvider.asStateFlow()

    private val _isAutoRouting = MutableStateFlow(prefs.getBoolean("auto_routing", true))
    val isAutoRouting = _isAutoRouting.asStateFlow()

    private val _language = MutableStateFlow(
        if (prefs.getString("app_language", "AR") == "EN") AppLanguage.ENGLISH else AppLanguage.ARABIC
    )
    val language = _language.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode = _isDarkMode.asStateFlow()

    // Provider Custom Keys & URLs
    fun getApiKeyForProvider(provider: AIProvider): String {
        return prefs.getString("key_${provider.name}", "") ?: ""
    }

    fun setApiKeyForProvider(provider: AIProvider, key: String) {
        prefs.edit().putString("key_${provider.name}", key).apply()
    }

    fun getCustomUrl(): String {
        return prefs.getString("custom_url", "http://localhost:11434/v1") ?: "http://localhost:11434/v1"
    }

    fun setCustomUrl(url: String) {
        prefs.edit().putString("custom_url", url).apply()
    }

    fun getCustomModel(): String {
        return prefs.getString("custom_model", "llama3") ?: "llama3"
    }

    fun setCustomModel(model: String) {
        prefs.edit().putString("custom_model", model).apply()
    }

    fun isProviderEnabled(provider: AIProvider): Boolean {
        // By default, Gemini, OpenAI, Nvidia, AMD, and Offline are enabled
        return prefs.getBoolean("enabled_${provider.name}", true)
    }

    fun setProviderEnabled(provider: AIProvider, enabled: Boolean) {
        prefs.edit().putBoolean("enabled_${provider.name}", enabled).apply()
    }

    fun setAutoRouting(enabled: Boolean) {
        _isAutoRouting.value = enabled
        prefs.edit().putBoolean("auto_routing", enabled).apply()
    }

    fun setProvider(provider: AIProvider) {
        _currentProvider.value = provider
        prefs.edit().putString("active_provider", provider.name).apply()
    }

    suspend fun testProvider(provider: AIProvider): Pair<Boolean, String> {
        val key = getApiKeyForProvider(provider)
        val url = if (provider == AIProvider.CUSTOM_MANUAL) getCustomUrl() else null
        return aiService.testProviderConnection(provider, key, url)
    }

    fun toggleLanguage() {
        val newLang = if (_language.value == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
        _language.value = newLang
        prefs.edit().putString("app_language", if (newLang == AppLanguage.ENGLISH) "EN" else "AR").apply()
    }

    fun toggleDarkMode() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        prefs.edit().putBoolean("is_dark_mode", newMode).apply()
    }

    // Sessions and Messages
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(title: String = "محادثة جديدة"): String {
        val id = UUID.randomUUID().toString()
        val session = ChatSession(
            id = id,
            title = title,
            providerUsed = _currentProvider.value.name
        )
        chatDao.insertSession(session)
        return id
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun clearMessagesForSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
    }

    suspend fun sendMessage(
        sessionId: String,
        userPrompt: String,
        bitmap: Bitmap? = null
    ): Result<String> {
        val userMessageId = UUID.randomUUID().toString()
        val userMsg = ChatMessage(
            id = userMessageId,
            sessionId = sessionId,
            role = "user",
            content = userPrompt,
            providerUsed = _currentProvider.value.name,
            modelUsed = _currentProvider.value.defaultModel
        )
        chatDao.insertMessage(userMsg)

        // Resolve target provider based on Auto Routing or manual selection
        val provider = if (_isAutoRouting.value) {
            // Pick first enabled provider
            AIProvider.values().firstOrNull { isProviderEnabled(it) && it != AIProvider.OFFLINE_LOCAL }
                ?: AIProvider.GEMINI
        } else {
            _currentProvider.value
        }

        val customKey = getApiKeyForProvider(provider)
        val customUrl = if (provider == AIProvider.CUSTOM_MANUAL) getCustomUrl() else null
        val customModel = if (provider == AIProvider.CUSTOM_MANUAL) getCustomModel() else provider.defaultModel

        val result = aiService.generateResponse(
            prompt = userPrompt,
            provider = provider,
            customApiKey = customKey,
            customUrl = customUrl,
            modelName = customModel,
            attachedBitmap = bitmap
        )

        val responseText = result.getOrElse { "عذراً، حدث خطأ أثناء المعالجة: ${it.localizedMessage}" }

        val assistantMessageId = UUID.randomUUID().toString()
        val assistantMsg = ChatMessage(
            id = assistantMessageId,
            sessionId = sessionId,
            role = "assistant",
            content = responseText,
            providerUsed = provider.name,
            modelUsed = customModel,
            tokensCount = responseText.length / 4,
            isOfflineGenerated = provider == AIProvider.OFFLINE_LOCAL
        )
        chatDao.insertMessage(assistantMsg)

        // Update session's last message & title
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            val updatedTitle = if (session.title == "محادثة جديدة" || session.title.isBlank()) {
                userPrompt.take(28) + if (userPrompt.length > 28) "..." else ""
            } else {
                session.title
            }
            chatDao.insertSession(
                session.copy(
                    title = updatedTitle,
                    updatedAt = System.currentTimeMillis(),
                    lastMessagePreview = responseText.take(45),
                    providerUsed = provider.name
                )
            )
        }

        return result
    }

    // Documents
    val allDocuments: Flow<List<DocumentItem>> = documentDao.getAllDocuments()

    suspend fun saveDocument(doc: DocumentItem) {
        documentDao.insertDocument(doc)
    }

    suspend fun deleteDocument(id: String) {
        documentDao.deleteDocument(id)
    }

    // Workflow Tasks
    val allTasks: Flow<List<WorkflowTask>> = taskDao.getAllTasks()

    suspend fun insertTask(task: WorkflowTask) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: WorkflowTask) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(id: String) {
        taskDao.deleteTask(id)
    }

    // Scheduled Posts
    val allScheduledPosts: Flow<List<ScheduledPost>> = taskDao.getAllScheduledPosts()

    suspend fun insertScheduledPost(post: ScheduledPost) {
        taskDao.insertScheduledPost(post)
    }

    suspend fun deleteScheduledPost(id: String) {
        taskDao.deleteScheduledPost(id)
    }

    // Seed sample initial data if empty
    suspend fun seedInitialDataIfEmpty() {
        val initialDoc = DocumentItem(
            id = "doc-1",
            title = "استراتيجية الذكاء الاصطناعي المؤسسي 2026",
            type = DocumentType.RESEARCH,
            content = "# استراتيجية الذكاء الاصطناعي المؤسسي 2026\n\nتكامل الذكاء الاصطناعي مع نماذج الأعمال الحديثة لرفع الإنتاجية بنسبة 50%.\n\n- أتمتة المهام اليومية.\n- دعم العمل دون اتصال بالإنترنت.\n- التكامل مع GitHub وSlack وNotion.",
            format = DocumentFormat.PDF
        )
        documentDao.insertDocument(initialDoc)

        val initialTask = WorkflowTask(
            id = "task-1",
            title = "دراسة ميزات المنتج وتحديد متطلبات السوق",
            description = "تحليل المنافسين وإعداد مصفوفة SWOT لمنتج الذكاء الاصطناعي",
            category = "Product",
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.HIGH,
            assignedAgent = "Product Manager AI"
        )
        taskDao.insertTask(initialTask)

        val task2 = WorkflowTask(
            id = "task-2",
            title = "توليد ريلز تسويقي لزيادة المبيعات",
            description = "صياغة سيناريو إعلان تيك توك وإنستغرام ريلز بنبرة تفاعلية",
            category = "Marketing",
            status = TaskStatus.TODO,
            priority = TaskPriority.MEDIUM,
            assignedAgent = "Viral Reels Agent"
        )
        taskDao.insertTask(task2)

        val samplePost = ScheduledPost(
            id = "post-1",
            title = "ريلز ميزات Gemini Omni الخارقة",
            content = "كيف تنجز عمل أسبوع كامل في ساعتين باستخدام الذكاء الاصطناعي الشامل؟ شاهد الفيديو وطبق الخطوات!",
            platform = "Instagram",
            scheduledTime = "اليوم، 06:30 مساءً",
            hook = "توقف عن إضاعة وقتك في العمل الروتيني!",
            visualPrompt = "Dynamic neon transition showing AI automation",
            engagementRateEstimate = "95.2%"
        )
        taskDao.insertScheduledPost(samplePost)
    }

    fun getAIAgents(): List<AIAgent> {
        return listOf(
            AIAgent(
                id = "agent_arch",
                name = "ألكسندر (Tech Lead)",
                roleTitle = "كبير مهندسي الحلول والبرمجيات",
                avatarEmoji = "👨‍💻",
                specialty = "معمارية النظم، Kotlin، Python، تسريع وحدات GPU",
                description = "متخصص في هندسة التطبيقات السحابية وحلول الأداء العالي.",
                systemPrompt = "أنت كبير مهندسي الحلول، تقدم حلولاً برمجية قوية ومحسنة."
            ),
            AIAgent(
                id = "agent_pm",
                name = "سارة (Product Director)",
                roleTitle = "مديرة دراسة وتطوير المنتجات",
                avatarEmoji = "👩‍💼",
                specialty = "دراسة الجدوى، تحليلات SWOT، خريطة الطريق، ROI",
                description = "تبني خطط المنتجات وتحلل مؤشرات السوق والمنافسين ببراعة.",
                systemPrompt = "أنت مديرة منتجات رائدة، تصيغ استراتيجيات تسعير وخريطة طريق متكاملة."
            ),
            AIAgent(
                id = "agent_viral",
                name = "عمر (Creative Reels Director)",
                roleTitle = "مدير الحملات الإعلانية والريلز",
                avatarEmoji = "🎬",
                specialty = "الريلز الفيروسي، إعلانات تيك توك، نصوص الإقناع والمبيعات",
                description = "يبتكر خطافات بصرية جذابة ترفع معدلات التفاعل والمبيعات.",
                systemPrompt = "أنت خبير إعلانات وريلز تسويقية فيروسية، تركز على جذب الانتباه وزيادة المبيعات."
            ),
            AIAgent(
                id = "agent_talent",
                name = "ليلى (Career & Talent Scout)",
                roleTitle = "مستشارة التوظيف وشبكات LinkedIn",
                avatarEmoji = "🌟",
                specialty = "اقتراح الوظائف، تحسين السير الذاتية، منشورات لينكد إن",
                description = "تربط مهاراتك بأفضل الفرص الوظيفية العالمية وتجهزك للمقابلات.",
                systemPrompt = "أنت مستشارة توظيف وتطوير مهني محترفة على لينكد إن."
            ),
            AIAgent(
                id = "agent_growth",
                name = "ديفيد (Automations & Integrations)",
                roleTitle = "خبير الربط والتكامل التلقائي",
                avatarEmoji = "⚡",
                specialty = "GitHub CI/CD, Slack Bots, Notion Templates, WhatsApp",
                description = "يربط المنصة بجميع أدوات العمل الجماعي لأتمتة سير العمل.",
                systemPrompt = "أنت خبير أتمتة وتكامل أدوات الفرق التقنية."
            )
        )
    }

    fun getSampleJobOpportunities(): List<JobOpportunity> {
        return listOf(
            JobOpportunity(
                id = "job-1",
                title = "Senior AI & Mobile Engineer",
                company = "HyperScale AI Labs",
                location = "عن بُعد (Remote / Global)",
                matchScore = 98,
                requiredSkills = listOf("Kotlin", "Jetpack Compose", "Gemini API", "Edge AI"),
                salaryRange = "$120,000 - $155,000 / سنة",
                description = "بناء تطبيقات ذكاء اصطناعي متعددة النماذج تدعم المعالجة السحابية والمحلية الفورية.",
                coverLetterTip = "أبرز خبرتك في تكامل Jetpack Compose مع نماذج Gemini وتسريع الحوسبة."
            ),
            JobOpportunity(
                id = "job-2",
                title = "Product Strategy & Growth Specialist",
                company = "NextGen Ventures",
                location = "دبي / الرياض / هجين",
                matchScore = 94,
                requiredSkills = listOf("Product Discovery", "SWOT & ROI", "Marketing Reels", "Analytics"),
                salaryRange = "$90,000 - $125,000 / سنة",
                description = "قيادة استراتيجيات نمو منتجات الـ SaaS والذكاء الاصطناعي وتحسين مسارات التحويل والمبيعات.",
                coverLetterTip = "أشر إلى قدرتك على إطلاق حملات محتوى مؤتمتة وتحليل سلوك المستخدمين بدقة."
            ),
            JobOpportunity(
                id = "job-3",
                title = "Full-Stack Automation & Integrations Architect",
                company = "CloudFlow Enterprise",
                location = "عن بُعد (Remote)",
                matchScore = 91,
                requiredSkills = listOf("Slack Apps", "GitHub Actions", "Notion API", "Webhooks"),
                salaryRange = "$100,000 - $135,000 / سنة",
                description = "ربط منصات الفرق وأتمتة مسارات العمل بين أنظمة التطوير والتسويق.",
                coverLetterTip = "وضح أمثلة على أتمتة الإشعارات بين سلاك وجيت هاب وواتساب."
            )
        )
    }

    fun getIntegrations(): List<IntegrationConfig> {
        return listOf(
            IntegrationConfig("gh", "GitHub", "🐙", true, "متصل (Connected)", "مزامنة الـ Repositories ومولد PRs وCommit messages"),
            IntegrationConfig("notion", "Notion", "📓", true, "متصل (Connected)", "مزامنة قواعد البيانات والوثائق وصفحات المشاريع"),
            IntegrationConfig("slack", "Slack", "💬", true, "متصل (Connected)", "إرسال ملخصات الاجتماعات وتنبيهات الفرق اللحظية"),
            IntegrationConfig("whatsapp", "WhatsApp", "🟢", true, "جاهز للمشاركة المباشرة", "مشاركة سيناريوهات الريلز والتقارير عبر رسائل مباشرة"),
            IntegrationConfig("linkedin", "LinkedIn", "💼", true, "متصل (Connected)", "نشر المقالات المهنية واقتناص فرص العمل"),
            IntegrationConfig("skills", "Skills & Certifications", "🎓", true, "مفعل (Active)", "تحليل الفجوة المهارية وتوصية المسارات التعليمية")
        )
    }
}
