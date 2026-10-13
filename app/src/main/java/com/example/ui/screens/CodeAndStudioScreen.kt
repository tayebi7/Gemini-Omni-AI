package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.OfflineAIIntelligence
import com.example.data.repository.AppRepository
import com.example.ui.components.CodeBlockView
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeAndStudioScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isArabic = repository.language.collectAsState().value.name == "ARABIC"

    var selectedSection by remember { mutableIntStateOf(0) }
    // 0: استوديو البرمجة وحلول المواقع (Code & Web Solutions)
    // 1: استوديو الصور والفيديو (Media Studio)
    // 2: ربط واتساب والتطبيقات (WhatsApp & Connect)

    // Code studio state
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    val languages = listOf("Kotlin", "Python", "JavaScript", "HTML/CSS", "SQL", "Rust")
    var codePrompt by remember { mutableStateOf("") }
    var currentCodeSnippet by remember {
        mutableStateOf(
            """
// حل برمجي عالي الأداء مع معالجة غير متزامنة
fun fetchOmniSolutions(query: String): List<String> {
    return listOf(
        "1. تشغيل المحرك العصبي المحلي فورياً بدون انتظار",
        "2. تسريع المعالجة عبر بطاقات NVIDIA TensorRT أو AMD ROCm",
        "3. تصدير المخرجات لـ PDF و Markdown و DOCX بنقرة واحدة"
    )
}
            """.trimIndent()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = GeminiBlueGlow)
                        Column {
                            Text(
                                text = if (isArabic) "استوديو البرمجة والوسائط والحلول" else "Code & Media Solutions Studio",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "برمجة بعدة لغات، فيديو Veo، وربط واتساب" else "Multi-language code, Veo video & WhatsApp",
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
            PrimaryTabRow(
                selectedTabIndex = selectedSection,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeminiBlueGlow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text(if (isArabic) "البرمجة والحلول" else "Code Studio", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text(if (isArabic) "استوديو الصور والفيديو" else "Media Studio", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    text = { Text(if (isArabic) "ربط واتساب" else "WhatsApp", fontSize = 12.sp) }
                )
            }

            when (selectedSection) {
                0 -> {
                    // Code Studio
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(text = if (isArabic) "اختر لغة البرمجة المطلوبة:" else "Select Language:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(languages) { lang ->
                                    FilterChip(
                                        selected = selectedLanguage == lang,
                                        onClick = { selectedLanguage = lang },
                                        label = { Text(lang, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = codePrompt,
                                onValueChange = { codePrompt = it },
                                label = { Text(if (isArabic) "صف المطلوب برمجته أو مشكلة ترغب بحلها" else "Describe feature to code or bug to solve") },
                                placeholder = { Text(if (isArabic) "مثال: واجهة تسجيل دخول، دالة فرز سريعة، كود موقع متجاوب..." else "e.g. fast sort algorithm, responsive web layout...") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    val prompt = if (codePrompt.isNotBlank()) codePrompt else "خوارزمية معالجة سريعة"
                                    coroutineScope.launch {
                                        val fullSolution = OfflineAIIntelligence.generateOfflineResponse("كود وبرمجة بلغة $selectedLanguage: $prompt")
                                        currentCodeSnippet = fullSolution
                                        Toast.makeText(context, "تم توليد الحل البرمجي بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_code_button")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "توليد الكود والحل الشامل ✦" else "Generate Code & Full Solution ✦")
                            }
                        }

                        item {
                            Text(text = if (isArabic) "معاينة الكود والحل البرمجي:" else "Solution Code Output:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            CodeBlockView(
                                codeText = currentCodeSnippet,
                                language = selectedLanguage
                            )
                        }
                    }
                }

                1 -> {
                    // Media Studio (Image & Video)
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
                                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPurpleGlow.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isArabic) "🎨 استوديو هندسة البرومت للصور (Image Studio)" else "🎨 AI Image Prompt Studio",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(text = "8K Cinematic", fontSize = 10.sp, color = GeminiPink, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "\"Hyper-realistic 8K photo of futuristic AI workstation with glowing holographic displays, neon purple and indigo ambient lights, ultra clean minimalist architecture, octane render\"",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GeminiBlueGlow
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Image Prompt", "Hyper-realistic 8k prompt..."))
                                            Toast.makeText(context, "تم نسخ البرومت بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(text = if (isArabic) "نسخ برومت الصورة" else "Copy Image Prompt", fontSize = 11.sp)
                                    }
                                }
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
                                    Text(
                                        text = if (isArabic) "🎥 محرك فيديو Veo 3.1 & Reels Animation" else "🎥 Veo Video Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = GeminiPink
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "• نسبة الأبعاد: 9:16 (Vertically Optimized for Reels)\n• الكاميرا: Dynamic 3D tracking forward with neon flare\n• الإطارات: 60 FPS Cinematic Motion Smoothness",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "تم تجهيز مواصفات الفيديو للمونتاج والتوليد", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPink),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(text = if (isArabic) "توليد ومحاكاة المشهد البصري" else "Simulate Video Motion", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // WhatsApp & Communication Connect
                    var whatsappMessage by remember {
                        mutableStateOf(
                            "مرحباً! أشارك معك هذا التحليل والحل الاستراتيجي الذي تم إعداده عبر منصة Gemini Omni الذكية:\n\n• زيادة المبيعات بنسبة 35%\n• خطة محتوى ريلز أسبوعية مؤتمتة\n• وثيقة العمل بصيغة PDF مرفقة."
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF25D366).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "🟢", fontSize = 32.sp)
                                    Column {
                                        Text(
                                            text = if (isArabic) "ربط واتساب الفوري (WhatsApp Direct Connect)" else "WhatsApp Instant Connect",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF25D366)
                                        )
                                        Text(
                                            text = if (isArabic) "مشاركة الحلول، سيناريوهات الإعلانات، والتقارير مباشرة لعملائك وفرق العمل." else "Share solutions, ad scripts, and reports directly to clients and teams.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = whatsappMessage,
                                onValueChange = { whatsappMessage = it },
                                label = { Text(if (isArabic) "نص الرسالة للمشاركة عبر واتساب" else "Message to Share via WhatsApp") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    try {
                                        val url = "https://api.whatsapp.com/send?text=" + Uri.encode(whatsappMessage)
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback to standard share
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, whatsappMessage)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة عبر واتساب"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_whatsapp_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "إرسال مباشر عبر واتساب" else "Send via WhatsApp Now",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
