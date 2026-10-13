package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProvider
import com.example.data.model.AppLanguage
import com.example.data.repository.AppRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndProvidersScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProvider by repository.currentProvider.collectAsState()
    val appLanguage by repository.language.collectAsState()
    val isDarkMode by repository.isDarkMode.collectAsState()
    val isArabic = appLanguage == AppLanguage.ARABIC

    val integrations = remember { repository.getIntegrations() }

    var selectedProviderForConfig by remember { mutableStateOf<AIProvider?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = GeminiBlueGlow)
                        Column {
                            Text(
                                text = if (isArabic) "إعدادات المزودين والتكاملات" else "AI Providers & Integrations",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "OpenAI، Anthropic، NVIDIA، AMD، وتسريع GPU" else "OpenAI, Anthropic, NVIDIA, AMD & GPU",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Appearance & Language Preferences
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isArabic) "المظهر واللغة (General)" else "General Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GeminiBlueGlow
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Language Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "لغة التطبيق" else "App Language",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isArabic) "العربية (الحالية)" else "English (Active)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { repository.toggleLanguage() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("toggle_language_button")
                            ) {
                                Text(
                                    text = if (isArabic) "English 🌐" else "العربية 🌐",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Theme Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "الوضع الليلي (Dark Mode)" else "Dark Theme",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isDarkMode) "ألوان Obsidian الليلية نشطة" else "الوضع الفاتح نشط",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { repository.toggleDarkMode() },
                                modifier = Modifier.testTag("dark_mode_switch")
                            )
                        }
                    }
                }
            }

            // AI Providers & GPU Acceleration Section
            item {
                Text(
                    text = if (isArabic) "مزودو الذكاء الاصطناعي وبطاقات الرسوميات (AI Providers & GPUs)" else "AI Providers & GPU Acceleration",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            items(AIProvider.values()) { provider ->
                val isActive = activeProvider == provider
                val (color, icon) = when (provider) {
                    AIProvider.GEMINI -> GoogleGeminiColor to Icons.Default.AutoAwesome
                    AIProvider.OPENAI -> OpenAIColor to Icons.Default.Bolt
                    AIProvider.ANTHROPIC -> AnthropicColor to Icons.Default.Psychology
                    AIProvider.NVIDIA_NIM -> NvidiaColor to Icons.Default.Memory
                    AIProvider.AMD_ROCM -> AmdColor to Icons.Default.Speed
                    AIProvider.OFFLINE_LOCAL -> OfflineLocalColor to Icons.Default.CloudOff
                }

                Surface(
                    onClick = { repository.setProvider(provider) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActive) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isActive) 1.5.dp else 1.dp,
                        if (isActive) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isActive) color else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "النموذج: ${provider.defaultModel}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isActive) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = color
                                ) {
                                    Text(
                                        text = "المزود النشط ✓",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ التسريع: ${provider.defaultGpuMode}",
                                fontSize = 11.sp,
                                color = GeminiBlueGlow
                            )

                            TextButton(
                                onClick = { selectedProviderForConfig = provider },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = "إعداد المفتاح ⚙️", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Integrations Section (GitHub, Notion, Slack, Skills)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isArabic) "تكاملات الأدوات والمهارات (Hub Integrations)" else "App & Skills Integrations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            items(integrations, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = item.iconEmoji, fontSize = 26.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = item.syncDetails, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GeminiGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = item.statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeminiGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Provider Configuration Dialog
    if (selectedProviderForConfig != null) {
        val provider = selectedProviderForConfig!!
        var apiKeyInput by remember { mutableStateOf(repository.getApiKeyForProvider(provider)) }

        AlertDialog(
            onDismissRequest = { selectedProviderForConfig = null },
            title = {
                Text(
                    text = "إعدادات ${provider.displayName}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "يمكنك إدخال مفتاح API الخاص بك للاتصال المباشر. في حال ترك الحقل فارغاً، ستستخدم المنصة المحرك العصبي المحلي فائق السرعة تلقائياً.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "تسريع الحوسبة: ${provider.defaultGpuMode}",
                        fontSize = 11.sp,
                        color = GeminiBlueGlow
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.setApiKeyForProvider(provider, apiKeyInput.trim())
                        selectedProviderForConfig = null
                        Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProviderForConfig = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
