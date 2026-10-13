package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.model.AppLanguage
import com.example.data.repository.AppRepository
import com.example.ui.screens.*
import com.example.ui.theme.GeminiBlueGlow
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = AppRepository(applicationContext)

        // Seed sample data if database is new
        lifecycleScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        setContent {
            val isDarkMode by repository.isDarkMode.collectAsState()
            val language by repository.language.collectAsState()
            val isArabic = language == AppLanguage.ARABIC

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppHost(
                    repository = repository,
                    isArabic = isArabic
                )
            }
        }
    }
}

enum class NavigationScreen(val labelAr: String, val labelEn: String) {
    CHAT("الدردشة", "Chat"),
    DOCUMENTS("المستندات", "Docs"),
    WORKFLOWS("المنتج وسير العمل", "Workflows"),
    PROFESSIONAL("الوكلاء والوظائف", "Agents"),
    STUDIO("البرمجة والوسائط", "Studio"),
    SETTINGS("الإعدادات", "Settings")
}

@Composable
fun MainAppHost(
    repository: AppRepository,
    isArabic: Boolean
) {
    var currentScreen by remember { mutableStateOf(NavigationScreen.CHAT) }

    // Predictive / System Back navigation handler
    if (currentScreen != NavigationScreen.CHAT) {
        BackHandler {
            currentScreen = NavigationScreen.CHAT
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_navigation_bar")
            ) {
                val screens = listOf(
                    NavigationScreen.CHAT to Icons.Default.ChatBubble,
                    NavigationScreen.DOCUMENTS to Icons.Default.Description,
                    NavigationScreen.WORKFLOWS to Icons.Default.RocketLaunch,
                    NavigationScreen.PROFESSIONAL to Icons.Default.Work,
                    NavigationScreen.STUDIO to Icons.Default.Code,
                    NavigationScreen.SETTINGS to Icons.Default.Settings
                )

                screens.forEach { (screen, icon) ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = if (isArabic) screen.labelAr else screen.labelEn,
                                tint = if (isSelected) GeminiBlueGlow else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = if (isArabic) screen.labelAr else screen.labelEn,
                                fontSize = 10.sp,
                                maxLines = 1,
                                color = if (isSelected) GeminiBlueGlow else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = GeminiBlueGlow.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { screen ->
            when (screen) {
                NavigationScreen.CHAT -> ChatScreen(
                    repository = repository,
                    onNavigateToSettings = { currentScreen = NavigationScreen.SETTINGS },
                    onNavigateToStudio = { currentScreen = NavigationScreen.STUDIO }
                )
                NavigationScreen.DOCUMENTS -> DocumentsScreen(
                    repository = repository
                )
                NavigationScreen.WORKFLOWS -> WorkflowsAndProductScreen(
                    repository = repository
                )
                NavigationScreen.PROFESSIONAL -> ProfessionalAndAgentsScreen(
                    repository = repository,
                    onDirectChatWithAgent = { agent ->
                        currentScreen = NavigationScreen.CHAT
                    }
                )
                NavigationScreen.STUDIO -> CodeAndStudioScreen(
                    repository = repository
                )
                NavigationScreen.SETTINGS -> SettingsAndProvidersScreen(
                    repository = repository
                )
            }
        }
    }
}
