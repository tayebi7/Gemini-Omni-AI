package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScheduledPost
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.WorkflowTask
import com.example.data.network.OfflineAIIntelligence
import com.example.data.repository.AppRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowsAndProductScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isArabic = repository.language.collectAsState().value.name == "ARABIC"

    val tasks by repository.allTasks.collectAsState(initial = emptyList())
    val scheduledPosts by repository.allScheduledPosts.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    // 0: دراسة المنتج وسير العمل (Product & Kanban)
    // 1: صانع الريلز والإعلانات (Viral Reels & Ads)
    // 2: الجدولة التلقائية والتنبيهات (Social Scheduler & Alerts)

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showReelGeneratorDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = GeminiBlueGlow)
                        Column {
                            Text(
                                text = if (isArabic) "دراسة المنتج وإدارة سير العمل" else "Product Discovery & Workflows",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "تحليلات، ريلز تسويقية، وجدولة تلقائية" else "Analytics, viral reels & smart scheduler",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Secondary Segmented Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeminiBlueGlow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isArabic) "سير العمل والمهام" else "Workflows", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isArabic) "استوديو الريلز" else "Viral Reels", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(if (isArabic) "الجدولة الذكية" else "Scheduler", fontSize = 12.sp) }
                )
            }

            when (selectedTab) {
                0 -> ProductAndWorkflowsTab(
                    tasks = tasks,
                    isArabic = isArabic,
                    onAddTask = { showAddTaskDialog = true },
                    onStatusChange = { task, newStatus ->
                        coroutineScope.launch {
                            repository.updateTask(task.copy(status = newStatus))
                        }
                    },
                    onDeleteTask = { id ->
                        coroutineScope.launch { repository.deleteTask(id) }
                    }
                )
                1 -> ViralReelsStudioTab(
                    isArabic = isArabic,
                    onGenerateReel = { showReelGeneratorDialog = true }
                )
                2 -> SocialSchedulerTab(
                    posts = scheduledPosts,
                    isArabic = isArabic,
                    onSchedulePost = { showScheduleDialog = true },
                    onDeletePost = { id ->
                        coroutineScope.launch { repository.deleteScheduledPost(id) }
                    }
                )
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var taskCategory by remember { mutableStateOf("Product") }
        var taskPriority by remember { mutableStateOf(TaskPriority.HIGH) }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text(if (isArabic) "إضافة مهمة جديدة لسير العمل" else "Add New Workflow Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text(if (isArabic) "عنوان المهمة" else "Task Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = taskCategory,
                        onValueChange = { taskCategory = it },
                        label = { Text(if (isArabic) "القسم (Product, Marketing, Analytics)" else "Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            coroutineScope.launch {
                                repository.insertTask(
                                    WorkflowTask(
                                        id = UUID.randomUUID().toString(),
                                        title = taskTitle,
                                        description = "مهمة مؤتمتة عبر وكيل الذكاء الاصطناعي",
                                        category = taskCategory,
                                        status = TaskStatus.TODO,
                                        priority = taskPriority,
                                        assignedAgent = "Automated Agent"
                                    )
                                )
                                showAddTaskDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (isArabic) "إضافة" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }

    // Reel Generator Dialog
    if (showReelGeneratorDialog) {
        var productTopic by remember { mutableStateOf("") }
        var targetPlatform by remember { mutableStateOf("Instagram Reels") }
        var isGenerating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showReelGeneratorDialog = false },
            title = { Text(if (isArabic) "توليد ريلز وإعلان فيروسي مخصص" else "Generate Viral Reel & Ad Script") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = productTopic,
                        onValueChange = { productTopic = it },
                        label = { Text(if (isArabic) "المنتج أو الفكرة المراد ترويجها" else "Product or Topic") },
                        placeholder = { Text(if (isArabic) "مثال: تطبيق ذكاء اصطناعي لزيادة المبيعات" else "e.g. AI tool for sales") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val topic = if (productTopic.isNotBlank()) productTopic else "زيادة المبيعات والأتمتة"
                        isGenerating = true
                        coroutineScope.launch {
                            val script = OfflineAIIntelligence.generateOfflineResponse("ريلز وإعلان: $topic على $targetPlatform")
                            repository.insertScheduledPost(
                                ScheduledPost(
                                    id = UUID.randomUUID().toString(),
                                    title = "ريلز $topic",
                                    content = script,
                                    platform = targetPlatform,
                                    scheduledTime = "غداً، 07:00 م (وقت الذروة)",
                                    hook = "أسرع طريقة لمضاعفة نتائجك في دقائق!",
                                    engagementRateEstimate = "96.4%"
                                )
                            )
                            isGenerating = false
                            showReelGeneratorDialog = false
                            selectedTab = 2 // Switch to scheduler tab to view it
                            Toast.makeText(context, "تم توليد سيناريو الريلز وجدولته بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isGenerating
                ) {
                    Text(if (isArabic) "توليد وجدولة فورية" else "Generate & Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReelGeneratorDialog = false }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }

    // Schedule Dialog
    if (showScheduleDialog) {
        var postTitle by remember { mutableStateOf("") }
        var postPlatform by remember { mutableStateOf("Instagram") }
        var postTime by remember { mutableStateOf("اليوم، 08:00 مساءً") }

        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text(if (isArabic) "جدولة منشور جديد تلقائياً" else "Schedule New Social Post") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = postTitle,
                        onValueChange = { postTitle = it },
                        label = { Text(if (isArabic) "عنوان المنشور / الريلز" else "Post Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = postPlatform,
                        onValueChange = { postPlatform = it },
                        label = { Text(if (isArabic) "المنصة (Instagram, TikTok, LinkedIn)" else "Platform") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = postTime,
                        onValueChange = { postTime = it },
                        label = { Text(if (isArabic) "التوقيت المقترح للنشر" else "Scheduled Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postTitle.isNotBlank()) {
                            coroutineScope.launch {
                                repository.insertScheduledPost(
                                    ScheduledPost(
                                        id = UUID.randomUUID().toString(),
                                        title = postTitle,
                                        content = "محتوى إعلاني تفاعلي مجدول لتحقيق أعلى انتشار",
                                        platform = postPlatform,
                                        scheduledTime = postTime,
                                        hook = "سر النجاح التسويقي الذي لا يخبرك به أحد",
                                        engagementRateEstimate = "95.8%"
                                    )
                                )
                                showScheduleDialog = false
                                Toast.makeText(context, "تمت جدولة المنشور بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(if (isArabic) "جدولة" else "Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
fun ProductAndWorkflowsTab(
    tasks: List<WorkflowTask>,
    isArabic: Boolean,
    onAddTask: () -> Unit,
    onStatusChange: (WorkflowTask, TaskStatus) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Analytics & SWOT Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GeminiSurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBlueGlow.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "📊 تحليلات المنتج التنافسية (AI SWOT)" else "📊 AI Competitive Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GeminiGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ROI: +380%",
                                color = GeminiGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic)
                            "• نقاط القوة: استجابة فورية بدون اتصال، دعم البطاقات الرسومية NVIDIA و AMD.\n• الفرص: التوسع في أسواق الخليج والشرق الأوسط عبر أتمتة الواتساب والسوشيال ميديا."
                        else
                            "• Strengths: Instant offline fallback, GPU acceleration via NVIDIA & AMD.\n• Opportunities: Growth in MENA markets with WhatsApp & Social Media automations.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Kanban Tasks Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "مهام سير العمل (Kanban Tasks)" else "Workflow Kanban Tasks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Button(
                    onClick = onAddTask,
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_task_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "مهمة جديدة" else "Add Task", fontSize = 12.sp)
                }
            }
        }

        // Task Items
        items(tasks, key = { it.id }) { task ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[${task.category}]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiPurpleGlow
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (task.status) {
                                    TaskStatus.DONE -> GeminiGreen.copy(alpha = 0.2f)
                                    TaskStatus.IN_PROGRESS -> GeminiBlue.copy(alpha = 0.2f)
                                    else -> GeminiAmber.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = task.status.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (task.status) {
                                        TaskStatus.DONE -> GeminiGreen
                                        TaskStatus.IN_PROGRESS -> GeminiBlueGlow
                                        else -> GeminiAmber
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteTask(task.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = task.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = task.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "الوكيل: ${task.assignedAgent}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (task.status != TaskStatus.IN_PROGRESS) {
                                TextButton(onClick = { onStatusChange(task, TaskStatus.IN_PROGRESS) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("بدء التنفيذ", fontSize = 11.sp)
                                }
                            }
                            if (task.status != TaskStatus.DONE) {
                                TextButton(onClick = { onStatusChange(task, TaskStatus.DONE) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("اكتمال ✓", fontSize = 11.sp, color = GeminiGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViralReelsStudioTab(
    isArabic: Boolean,
    onGenerateReel: () -> Unit
) {
    val sampleReelScript = remember {
        OfflineAIIntelligence.generateOfflineResponse("ريلز وإعلان لزيادة المبيعات والوصول")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Button(
                onClick = onGenerateReel,
                colors = ButtonDefaults.buttonColors(containerColor = GeminiPink),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_reel_button")
            ) {
                Icon(Icons.Default.MovieFilter, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "🎬 توليد ريلز وسيناريو إعلاني فيروسي جديد" else "🎬 Generate New Viral Reel Script",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPink.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "سيناريو ريلز نشط (Viral Production Blueprint)" else "Active Reels Production Script",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GeminiPink
                        )
                        Text(
                            text = "معدل الوصول: 96.2%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiGreen
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Text(
                        text = sampleReelScript,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SocialSchedulerTab(
    posts: List<ScheduledPost>,
    isArabic: Boolean,
    onSchedulePost: () -> Unit,
    onDeletePost: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GeminiSurfaceVariantDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GeminiAmber, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "نظام التنبيهات الذكي لنشر المحتوى" else "Smart Publishing & Sales Alert System",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isArabic) "يقوم النظام بدراسة أوقات ذروة تفاعل المتابعين وإرسال إشعارات لتأكيد النشر وزيادة المبيعات." else "Analyzes audience peak activity and triggers notifications to boost conversions.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "جدول المنشورات والريلز القادمة" else "Upcoming Content Schedule",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Button(
                    onClick = onSchedulePost,
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_schedule_button")
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "جدولة جديدة" else "Schedule", fontSize = 12.sp)
                }
            }
        }

        items(posts, key = { it.id }) { post ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GeminiBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = post.platform,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeminiBlueGlow,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "معدل التفاعل: ${post.engagementRateEstimate}", fontSize = 11.sp, color = GeminiGreen)
                            IconButton(onClick = { onDeletePost(post.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = post.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "📅 الموعد: ${post.scheduledTime}", fontSize = 12.sp, color = GeminiAmber)

                    if (post.hook.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "🎯 الخطاف الإعلاني: \"${post.hook}\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
