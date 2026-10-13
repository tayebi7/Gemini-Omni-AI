package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProvider
import com.example.data.model.AppLanguage
import com.example.data.repository.AppRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndProvidersScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeProvider by repository.currentProvider.collectAsState()
    val isAutoRouting by repository.isAutoRouting.collectAsState()
    val appLanguage by repository.language.collectAsState()
    val isDarkMode by repository.isDarkMode.collectAsState()
    val isArabic = appLanguage == AppLanguage.ARABIC

    val integrations = remember { repository.getIntegrations() }

    var selectedProviderForConfig by remember { mutableStateOf<AIProvider?>(null) }
    val providerStatusMap = remember { mutableStateMapOf<AIProvider, String>() }
    val isTestingMap = remember { mutableStateMapOf<AIProvider, Boolean>() }
    val providerEnabledState = remember { mutableStateMapOf<AIProvider, Boolean>() }

    // Init enabled state
    LaunchedEffect(Unit) {
        AIProvider.values().forEach { p ->
            providerEnabledState[p] = repository.isProviderEnabled(p)
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
                        Icon(Icons.Default.Settings, contentDescription = null, tint = GeminiBlueGlow)
                        Column {
                            Text(
                                text = if (isArabic) "إعدادات المزودين والربط" else "AI Providers & Connectivity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "تفعيل المزودين، التوجيه التلقائي، والمزود اليدوي" else "Multi-provider auto routing & manual mode",
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Auto-Routing Master Banner
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAutoRouting) GeminiBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAutoRouting) GeminiBlueGlow else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoMode, contentDescription = null, tint = GeminiBlueGlow, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (isArabic) "التوجيه التلقائي الذكي للمزودين" else "Auto Provider Routing",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isAutoRouting) GeminiBlueGlow else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic)
                                    "تفعيل كل المزودين معاً والمنصة تختار الأسرع والأفضل تلقائياً"
                                else
                                    "Enables all active providers; platform selects best one automatically",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isAutoRouting,
                            onCheckedChange = { enabled ->
                                repository.setAutoRouting(enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "تم تفعيل التوجيه التلقائي الذكي" else "تم إيقاف التوجيه التلقائي (اختيار يدوي)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("auto_routing_switch")
                        )
                    }
                }
            }

            // Providers Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "قائمة المزودين واختبار الاتصال" else "Providers & Connection Tests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isArabic) "اضغط للاختبار أو الإعداد" else "Tap to test or configure",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Compact Provider Items
            items(AIProvider.values()) { provider ->
                val isEnabled = providerEnabledState[provider] ?: true
                val isCurrentSelected = activeProvider == provider && !isAutoRouting
                val statusText = providerStatusMap[provider]
                val isTesting = isTestingMap[provider] ?: false

                val (color, icon) = when (provider) {
                    AIProvider.GEMINI -> GoogleGeminiColor to Icons.Default.AutoAwesome
                    AIProvider.OPENAI -> OpenAIColor to Icons.Default.Bolt
                    AIProvider.ANTHROPIC -> AnthropicColor to Icons.Default.Psychology
                    AIProvider.NVIDIA_NIM -> NvidiaColor to Icons.Default.Memory
                    AIProvider.AMD_ROCM -> AmdColor to Icons.Default.Speed
                    AIProvider.CUSTOM_MANUAL -> CustomManualColor to Icons.Default.Tune
                    AIProvider.OFFLINE_LOCAL -> OfflineLocalColor to Icons.Default.CloudOff
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrentSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Header row with Icon, Name, and Enable/Disable Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = provider.displayName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isCurrentSelected) {
                                            Text(text = "● نشط", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = provider.defaultModel,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Switch (تفعيل / إيقاف)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isEnabled) (if (isArabic) "مفعل" else "ON") else (if (isArabic) "معطل" else "OFF"),
                                    fontSize = 10.sp,
                                    color = if (isEnabled) GeminiGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { newState ->
                                        providerEnabledState[provider] = newState
                                        repository.setProviderEnabled(provider, newState)
                                        if (newState && !isAutoRouting) {
                                            repository.setProvider(provider)
                                        }
                                    },
                                    modifier = Modifier.testTag("switch_${provider.name.lowercase()}")
                                )
                            }
                        }

                        // Actions & Test Connectivity Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Test button
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        isTestingMap[provider] = true
                                        coroutineScope.launch {
                                            val (ok, msg) = repository.testProvider(provider)
                                            providerStatusMap[provider] = if (ok) "متصل ✓ $msg" else "فشل ✕ $msg"
                                            isTestingMap[provider] = false
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("test_${provider.name.lowercase()}")
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    } else {
                                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = if (isArabic) "اختبار" else "Test", fontSize = 10.sp)
                                    }
                                }

                                if (statusText != null) {
                                    Text(
                                        text = statusText,
                                        fontSize = 10.sp,
                                        color = if (statusText.contains("متصل") || statusText.contains("جاهز")) GeminiGreen else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Edit key / custom URL
                            TextButton(
                                onClick = { selectedProviderForConfig = provider },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (provider == AIProvider.CUSTOM_MANUAL) (if (isArabic) "تعديل الرابط" else "Endpoint") else (if (isArabic) "المفتاح" else "Key"),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // General Preferences (Theme and Language)
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isArabic) "لغة التطبيق" else "Language", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Button(
                                onClick = { repository.toggleLanguage() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(text = if (isArabic) "English 🌐" else "العربية 🌐", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isArabic) "الوضع الداكن" else "Dark Theme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { repository.toggleDarkMode() },
                                modifier = Modifier.testTag("dark_mode_switch")
                            )
                        }
                    }
                }
            }

            // Integrations Section
            item {
                Text(
                    text = if (isArabic) "التكاملات (GitHub, Notion, Slack, WhatsApp)" else "Integrations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(integrations, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = item.iconEmoji, fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(text = item.syncDetails, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = GeminiGreen.copy(alpha = 0.15f)) {
                            Text(
                                text = "مفعل ✓",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeminiGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        var customUrlInput by remember { mutableStateOf(repository.getCustomUrl()) }
        var customModelInput by remember { mutableStateOf(repository.getCustomModel()) }
        var dialogTestResult by remember { mutableStateOf<String?>(null) }
        var isDialogTesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedProviderForConfig = null },
            title = {
                Text(
                    text = if (provider == AIProvider.CUSTOM_MANUAL) "إعدادات المزود اليدوي المخصص" else "إعداد ${provider.displayName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (provider == AIProvider.CUSTOM_MANUAL) {
                        Text(
                            text = "يمكنك ربط أي خادم محلي أو سحابي (مثل Ollama أو vLLM أو LiteLLM أو سيرفرك الخاص):",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            label = { Text("رابط الخادم (Base URL)") },
                            placeholder = { Text("http://localhost:11434/v1") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customModelInput,
                            onValueChange = { customModelInput = it },
                            label = { Text("اسم النموذج (Model Name)") },
                            placeholder = { Text("llama3 أو mistral أو custom") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(if (provider == AIProvider.CUSTOM_MANUAL) "API Key (إن وجد)" else "مفتاح API Key") },
                        placeholder = { Text("اختياري - يترك فارغاً للوضع المحلي") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Test connectivity button inside dialog
                    Button(
                        onClick = {
                            isDialogTesting = true
                            coroutineScope.launch {
                                repository.setApiKeyForProvider(provider, apiKeyInput.trim())
                                if (provider == AIProvider.CUSTOM_MANUAL) {
                                    repository.setCustomUrl(customUrlInput.trim())
                                    repository.setCustomModel(customModelInput.trim())
                                }
                                val (ok, msg) = repository.testProvider(provider)
                                dialogTestResult = if (ok) "نجح الاتصال ✓ ($msg)" else "فشل الاتصال: $msg"
                                isDialogTesting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDialogTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(text = "⚡ اختبار الاتصال الآن", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }

                    if (dialogTestResult != null) {
                        Text(
                            text = dialogTestResult!!,
                            fontSize = 11.sp,
                            color = if (dialogTestResult!!.contains("نجح")) GeminiGreen else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.setApiKeyForProvider(provider, apiKeyInput.trim())
                        if (provider == AIProvider.CUSTOM_MANUAL) {
                            repository.setCustomUrl(customUrlInput.trim())
                            repository.setCustomModel(customModelInput.trim())
                        }
                        selectedProviderForConfig = null
                        Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProviderForConfig = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
