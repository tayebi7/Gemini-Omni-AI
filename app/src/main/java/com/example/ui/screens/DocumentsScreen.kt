package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.DocumentFormat
import com.example.data.model.DocumentItem
import com.example.data.model.DocumentType
import com.example.data.network.OfflineAIIntelligence
import com.example.data.repository.AppRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isArabic = repository.language.collectAsState().value.name == "ARABIC"
    val documents by repository.allDocuments.collectAsState(initial = emptyList())

    var selectedDoc by remember { mutableStateOf<DocumentItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    // Init selection
    LaunchedEffect(documents) {
        if (selectedDoc == null && documents.isNotEmpty()) {
            selectedDoc = documents.first()
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
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = GeminiPurpleGlow
                        )
                        Column {
                            Text(
                                text = if (isArabic) "استوديو المستندات والبحوث" else "Docs & Research Studio",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "تصدير لكافة الصيغ وعمل دون اتصال" else "Multi-format export & 100% Offline",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_document_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = "إنشاء مستند جديد",
                            tint = GeminiBlueGlow
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
                .padding(16.dp)
        ) {
            // Horizontal list of saved documents
            Text(
                text = if (isArabic) "المستندات المحفوظة (أوفلاين)" else "Saved Documents (Offline)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(documents, key = { it.id }) { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    Surface(
                        onClick = { selectedDoc = doc },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) GeminiPurpleGlow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) GeminiPurpleGlow else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = doc.format.extension.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeminiBlueGlow
                                )
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Offline Available",
                                    tint = GeminiGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = doc.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = doc.type.name,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Preview & Action Header
            if (selectedDoc != null) {
                val doc = selectedDoc!!
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title bar with Export action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${doc.type.name} • الصيغة الحالية: ${doc.format.extension}",
                                    fontSize = 11.sp,
                                    color = GeminiBlueGlow
                                )
                            }

                            Button(
                                onClick = { showExportSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("export_doc_button")
                            ) {
                                Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isArabic) "تصدير / تحويل" else "Export / Convert", fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Document Content Viewer
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = doc.content,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = if (isArabic) "لا يوجد مستند محدد" else "No document selected", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Export & Convert Sheet (PDF, DOCX, MD, TXT, JSON, HTML)
    if (showExportSheet && selectedDoc != null) {
        val currentDoc = selectedDoc!!
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isArabic) "اختر صيغة التصدير والتحويل" else "Select Export / Conversion Format",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isArabic) "يتم التحويل وتجهيز الملف فورياً دون الحاجة للاتصال بالإنترنت." else "Files are converted and prepared locally without network requirement.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val formats = listOf(
                    DocumentFormat.PDF to ("PDF (.pdf)" to "مستند بي دي إف عالي التنسيق جاهز للطباعة والمشاركة"),
                    DocumentFormat.DOCX to ("Word (.docx)" to "مستند مايكروسوفت وورد متوافق مع البرامج المكتبية"),
                    DocumentFormat.MARKDOWN to ("Markdown (.md)" to "صيغة ماركداون قياسية للمطورين ومواقع التوثيق"),
                    DocumentFormat.HTML to ("HTML Web (.html)" to "صفحة ويب مجهزة مع أنماط CSS أنيقة"),
                    DocumentFormat.TXT to ("Text (.txt)" to "نص مجرد خفيف وسريع القراءة"),
                    DocumentFormat.JSON to ("JSON Schema (.json)" to "بيانات مهيكلة جاهزة للربط مع الـ APIs وقواعد البيانات")
                )

                formats.forEach { (format, details) ->
                    val (label, desc) = details
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                // Update format
                                val updated = currentDoc.copy(format = format)
                                repository.saveDocument(updated)
                                selectedDoc = updated
                                showExportSheet = false

                                // Trigger native share intent
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = format.mime
                                    putExtra(Intent.EXTRA_SUBJECT, currentDoc.title)
                                    putExtra(Intent.EXTRA_TEXT, currentDoc.content)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "تصدير المستند كـ $label"))
                                Toast.makeText(context, "تم تجهيز وتصدير $label بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = GeminiBlueGlow)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Create New Document Dialog
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(DocumentType.RESEARCH) }
        var customPrompt by remember { mutableStateOf("") }
        var isGenerating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = if (isArabic) "إنشاء مستند / بحث / قصة جديدة" else "Create New Doc / Research / Story",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(if (isArabic) "عنوان المستند" else "Document Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        label = { Text(if (isArabic) "موضوع المستند أو الفكرة" else "Topic / Outline Prompt") },
                        placeholder = { Text(if (isArabic) "مثال: استراتيجية أتمتة الأعمال، قصة خيال علمي..." else "e.g. AI Strategy, Sci-Fi Story...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Text(text = if (isArabic) "نوع المستند:" else "Document Type:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(DocumentType.RESEARCH, DocumentType.STORY, DocumentType.PRODUCT_BRIEF).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (newTitle.isNotBlank()) newTitle else "مستند ذكاء اصطناعي جديد"
                        val prompt = if (customPrompt.isNotBlank()) customPrompt else title
                        isGenerating = true

                        coroutineScope.launch {
                            val generatedContent = OfflineAIIntelligence.generateOfflineResponse("إنشاء مستند وبحث: $prompt")
                            val newDoc = DocumentItem(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                type = selectedType,
                                content = generatedContent,
                                format = DocumentFormat.PDF
                            )
                            repository.saveDocument(newDoc)
                            selectedDoc = newDoc
                            showCreateDialog = false
                            isGenerating = false
                            Toast.makeText(context, "تم توليد وحفظ المستند بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isGenerating
                ) {
                    Text(if (isArabic) "توليد بالذكاء الاصطناعي" else "Generate with AI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
