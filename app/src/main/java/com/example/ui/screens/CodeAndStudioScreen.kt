package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

    // Media studio state
    var imageTopic by remember { mutableStateOf("واجهة ذكاء اصطناعي مستقبلية بألوان النيون والهولوجرام") }
    var selectedImageStyle by remember { mutableStateOf("8K Photorealistic") }
    val imageStyles = listOf("8K Photorealistic", "Cyberpunk", "3D Octane", "Minimalist Vector", "Cinematic Anime")
    var generatedImagePrompt by remember {
        mutableStateOf(
            "Hyper-realistic 8K photo of futuristic AI holographic interface, glowing purple and neon cyan sparkles, elegant dark obsidian minimalism, raytracing, award winning composition, photorealistic, professional volumetric lighting --ar 16:9 --v 6.0"
        )
    }

    var videoTopic by remember { mutableStateOf("ريلز إعلاني ديناميكي لزيادة المبيعات") }
    var selectedVideoAspect by remember { mutableStateOf("9:16 (Reels & TikTok)") }
    var selectedCameraMotion by remember { mutableStateOf("Dynamic Dolly In") }
    var generatedVideoSpec by remember {
        mutableStateOf(
            "• محرك الفيديو: Google Veo 3.1 Fast\n• الأبعاد: 9:16 عمودي\n• الكاميرا: Dynamic slow tracking dolly forward with soft neon ambient flare\n• الإضاءة: Cinematic studio rim lights, 60 FPS ultra smooth motion\n• المشهد: انتقال ذكي من مشكلة العمل اليدوي إلى الأتمتة الفورية بلمسة زر."
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "برمجة بعدة لغات، فيديو Veo، وربط واتساب" else "Multi-language code, Veo video & WhatsApp",
                                fontSize = 10.sp,
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
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(text = if (isArabic) "اختر لغة البرمجة المطلوبة:" else "Select Language:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(languages) { lang ->
                                    FilterChip(
                                        selected = selectedLanguage == lang,
                                        onClick = { selectedLanguage = lang },
                                        label = { Text(lang, fontSize = 11.sp) }
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
                                    val prompt = if (codePrompt.isNotBlank()) codePrompt else "خوارزمية معالجة سريعة وحل تقني متكامل"
                                    coroutineScope.launch {
                                        val fullSolution = OfflineAIIntelligence.generateOfflineResponse("كود وبرمجة بلغة $selectedLanguage: $prompt")
                                        currentCodeSnippet = fullSolution
                                        Toast.makeText(context, "تم توليد الحل البرمجي بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_code_button")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "توليد الكود والحل الشامل ✦" else "Generate Code & Full Solution ✦", fontSize = 13.sp)
                            }
                        }

                        item {
                            Text(text = if (isArabic) "معاينة الكود والحل البرمجي:" else "Solution Code Output:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            CodeBlockView(
                                codeText = currentCodeSnippet,
                                language = selectedLanguage
                            )
                        }
                    }
                }

                1 -> {
                    // Enhanced Media Studio (Image & Video)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Image Studio Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = GeminiSurfaceVariantDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPurpleGlow.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isArabic) "🎨 استوديو توليد برومت الصور (Image Studio)" else "🎨 AI Image Studio",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(text = selectedImageStyle, fontSize = 10.sp, color = GeminiPink, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = imageTopic,
                                        onValueChange = { imageTopic = it },
                                        label = { Text("موضوع الصورة") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(imageStyles) { style ->
                                            FilterChip(
                                                selected = selectedImageStyle == style,
                                                onClick = { selectedImageStyle = style },
                                                label = { Text(style, fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            generatedImagePrompt = "Cinematic $selectedImageStyle photo of $imageTopic, glowing ambient reflections, ultra clean minimalist geometry, award winning composition, octane render, 8k resolution, raytracing, realistic texture depth --ar 16:9 --v 6.0"
                                            Toast.makeText(context, "تم توليد وتجهيز برومت الصورة الاحترافي", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "توليد برومت ومواصفات الصورة ✦", fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = GeminiDeepSpace,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBorderDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = generatedImagePrompt,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = GeminiBlueGlow,
                                                lineHeight = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Image Prompt", generatedImagePrompt))
                                                    Toast.makeText(context, "تم نسخ برومت الصورة بنجاح", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GeminiSurfaceDark),
                                                modifier = Modifier.align(Alignment.End),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("نسخ البرومت 📋", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Video Studio Card (Veo)
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiPink.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (isArabic) "🎥 استوديو فيديو Google Veo 3.1 & Reels" else "🎥 Veo Video Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GeminiPink
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = videoTopic,
                                        onValueChange = { videoTopic = it },
                                        label = { Text("فكرة أو سيناريو مشهد الفيديو") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("9:16 (Reels)", "16:9 (Landscape)", "1:1 (Square)").forEach { aspect ->
                                            FilterChip(
                                                selected = selectedVideoAspect.startsWith(aspect.take(4)),
                                                onClick = { selectedVideoAspect = aspect },
                                                label = { Text(aspect, fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            generatedVideoSpec = """
• محرك الفيديو: Google Veo 3.1 Fast Preview
• نسبة الأبعاد: $selectedVideoAspect
• فكرة المشهد: $videoTopic
• الكاميرا: $selectedCameraMotion مع تركيز تلقائي وإضاءة حجمية Volumetric
• المخرجات: 60 FPS Cinematic Motion Smoothness جاهز للنشر على الريلز والمنصات.
                                            """.trimIndent()
                                            Toast.makeText(context, "تم تجهيز سيناريو ومواصفات الفيديو بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPink),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "توليد مواصفات المشهد السينمائي 🎥", fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = generatedVideoSpec,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
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
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF25D366).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = "🟢", fontSize = 28.sp)
                                    Column {
                                        Text(
                                            text = if (isArabic) "ربط واتساب الفوري (WhatsApp Direct Connect)" else "WhatsApp Instant Connect",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
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
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, whatsappMessage)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة عبر واتساب"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(16.dp),
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
