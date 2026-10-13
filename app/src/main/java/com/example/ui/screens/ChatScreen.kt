package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProvider
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.repository.AppRepository
import com.example.ui.components.FormattedMessageBody
import com.example.ui.components.GeminiSparkleIcon
import com.example.ui.components.PromptSuggestionChip
import com.example.ui.components.ProviderBadgeChip
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    repository: AppRepository,
    onNavigateToSettings: () -> Unit,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeProvider by repository.currentProvider.collectAsState()
    val isArabic = repository.language.collectAsState().value.name == "ARABIC"

    val sessions by repository.allSessions.collectAsState(initial = emptyList())
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }

    // Init session if needed
    LaunchedEffect(sessions) {
        if (currentSessionId == null) {
            if (sessions.isNotEmpty()) {
                currentSessionId = sessions.first().id
            } else {
                currentSessionId = repository.createNewSession(if (isArabic) "محادثة جديدة" else "New Chat")
            }
        }
    }

    val messagesFlow = remember(currentSessionId) {
        currentSessionId?.let { repository.getMessagesForSession(it) }
    }
    val messages by (messagesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Android 13+ zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                selectedBitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                Toast.makeText(context, if (isArabic) "تم إرفاق الصورة بنجاح" else "Image attached", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "فشل قراءة الصورة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isAutoRouting by repository.isAutoRouting.collectAsState()

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    fun handleSend(prompt: String = inputText) {
        val query = prompt.trim()
        if (query.isBlank() && selectedBitmap == null) return
        val sessionId = currentSessionId ?: return

        isSending = true
        val tempBitmap = selectedBitmap
        inputText = ""
        selectedImageUri = null
        selectedBitmap = null

        coroutineScope.launch {
            repository.sendMessage(sessionId, query, tempBitmap)
            isSending = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GeminiSparkleIcon(size = 24)
                        Column {
                            Text(
                                text = "Gemini Omni",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isArabic) "يفهم كل شيء تلقائياً" else "Understands everything automatically",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    ProviderBadgeChip(
                        provider = activeProvider,
                        isAutoRouting = isAutoRouting,
                        onClick = onNavigateToSettings,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                currentSessionId?.let { repository.clearMessagesForSession(it) }
                                Toast.makeText(context, if (isArabic) "تم مسح المحادثة" else "Chat cleared", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "مسح المحادثة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("chat_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "سجل المحادثات",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                currentSessionId = repository.createNewSession(if (isArabic) "محادثة جديدة" else "New Chat")
                            }
                        },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "محادثة جديدة",
                            tint = GeminiBlueGlow,
                            modifier = Modifier.size(20.dp)
                        )
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
            // Main Message list or Welcome state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    // Empty state with Gemini greeting & suggestion chips
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GeminiSparkleIcon(size = 54)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isArabic) "مرحباً بك في Gemini Omni" else "Welcome to Gemini Omni",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic)
                                "اطلب أي شيء: تحليل منتج، سيناريو ريلز، كتابة مستند وتصديره، أو حل برمجي فوري."
                            else
                                "Ask anything: product study, viral reel script, export docs, or code sandbox.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Suggestion chips
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PromptSuggestionChip(
                                text = if (isArabic) "📊 دراسة جدوى منتج جديد مع مصفوفة SWOT" else "📊 New Product Feasibility & SWOT",
                                icon = { Icon(Icons.Default.Analytics, contentDescription = null, tint = GeminiBlueGlow, modifier = Modifier.size(16.dp)) },
                                onClick = { handleSend(if (isArabic) "قم بعمل دراسة جدوى متكاملة لمنتج ذكاء اصطناعي مع SWOT وROI" else "Create complete product study with SWOT & ROI") }
                            )
                            PromptSuggestionChip(
                                text = if (isArabic) "🎬 سيناريو ريلز إعلاني فيروسي لزيادة المبيعات" else "🎬 Viral Reels Script for Sales",
                                icon = { Icon(Icons.Default.MovieFilter, contentDescription = null, tint = GeminiPink, modifier = Modifier.size(16.dp)) },
                                onClick = { handleSend(if (isArabic) "اكتب سيناريو ريلز احترافي مع خطاف إعلاني قوي ومواصفات بصرية للمشاهد" else "Write viral reels ad script with strong hook and visual specs") }
                            )
                            PromptSuggestionChip(
                                text = if (isArabic) "📄 إنشاء بحث استراتيجي جاهز للتحويل والتصدير" else "📄 Create Strategic Research for Export",
                                icon = { Icon(Icons.Default.Description, contentDescription = null, tint = GeminiPurpleGlow, modifier = Modifier.size(16.dp)) },
                                onClick = { handleSend(if (isArabic) "أنشئ بحثاً تقنياً متكاملاً عن مستقبل النماذج اللغوية مع ملخص تنفيذي" else "Create technical research on LLMs with executive summary") }
                            )
                            PromptSuggestionChip(
                                text = if (isArabic) "💼 اقتراح وظيفة وتطوير مهني مخصص لـ LinkedIn" else "💼 LinkedIn Job Match & Professional Post",
                                icon = { Icon(Icons.Default.Work, contentDescription = null, tint = GeminiGreen, modifier = Modifier.size(16.dp)) },
                                onClick = { handleSend(if (isArabic) "اقترح وظيفة أحلامي بناء على مهاراتي وصغ منشور احترافي للينكد إن" else "Suggest top job match and draft a professional LinkedIn post") }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageItem(
                                message = msg,
                                isArabic = isArabic,
                                onShare = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, msg.content)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الإجابة"))
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Gemini Message", msg.content)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (isArabic) "تم النسخ بنجاح" else "Copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (isSending) {
                            item {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GeminiSparkleIcon(size = 20)
                                    Text(
                                        text = if (isArabic) "Gemini يفكر ويحلل..." else "Gemini is analyzing...",
                                        fontSize = 13.sp,
                                        color = GeminiBlueGlow
                                    )
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = GeminiBlueGlow
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Attached image preview before send
            if (selectedBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "معاينة الصورة المرفقة",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = if (isArabic) "صورة مرفقة للتحليل متعدد الوسائط" else "Image attached for multimodal analysis",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                selectedImageUri = null
                                selectedBitmap = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "حذف الصورة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Bottom Input Bar - Gemini Glowing Bar
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(GeminiBlueGlow.copy(alpha = 0.5f), GeminiPurpleGlow.copy(alpha = 0.5f))
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("attach_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "إرفاق صورة",
                            tint = GeminiBlueGlow
                        )
                    }

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isArabic) "اكتب رسالة أو استفسار..." else "Ask Gemini anything...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        maxLines = 4
                    )

                    // Voice / speech mock button
                    IconButton(
                        onClick = {
                            inputText = if (isArabic) "حلل هذا المنتج واقترح سيناريو إعلاني ومهام عمل" else "Analyze this product and generate ad script with tasks"
                            Toast.makeText(context, if (isArabic) "تم تسجيل الصوت بنجاح" else "Voice captured", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "الإدخال الصوتي",
                            tint = GeminiPurpleGlow
                        )
                    }

                    IconButton(
                        onClick = { handleSend() },
                        enabled = !isSending && (inputText.isNotBlank() || selectedBitmap != null),
                        modifier = Modifier.testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = if (inputText.isNotBlank() || selectedBitmap != null) GeminiBlueGlow else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    // History Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "سجل المحادثات السابقة" else "Chat History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                currentSessionId = repository.createNewSession(if (isArabic) "محادثة جديدة" else "New Chat")
                                showHistorySheet = false
                            }
                        }
                    ) {
                        Text(if (isArabic) "+ محادثة جديدة" else "+ New Chat")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (sessions.isEmpty()) {
                    Text(
                        text = if (isArabic) "لا توجد محادثات سابقة بعد." else "No previous chats yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxHeight(0.6f)
                    ) {
                        items(sessions, key = { it.id }) { s ->
                            Surface(
                                onClick = {
                                    currentSessionId = s.id
                                    showHistorySheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (s.id == currentSessionId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (s.lastMessagePreview.isNotBlank()) {
                                            Text(
                                                text = s.lastMessagePreview,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteSession(s.id)
                                                if (currentSessionId == s.id) {
                                                    currentSessionId = repository.createNewSession()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف المحادثة",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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
fun ChatMessageItem(
    message: ChatMessage,
    isArabic: Boolean,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                GeminiSparkleIcon(size = 28, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) GeminiBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) GeminiBlueGlow.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FormattedMessageBody(content = message.content)

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✦ ${message.modelUsed}",
                                fontSize = 10.sp,
                                color = GeminiBlueGlow
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = onShare, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GeminiPurpleGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
