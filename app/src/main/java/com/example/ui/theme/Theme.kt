package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GeminiBlueGlow,
    onPrimary = GeminiDeepSpace,
    primaryContainer = GeminiSurfaceVariantDark,
    onPrimaryContainer = GeminiBlueGlow,
    secondary = GeminiPurpleGlow,
    onSecondary = GeminiDeepSpace,
    secondaryContainer = GeminiSurfaceVariantDark,
    onSecondaryContainer = GeminiPurpleGlow,
    tertiary = GeminiCyan,
    background = GeminiDeepSpace,
    onBackground = TextPrimaryDark,
    surface = GeminiSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = GeminiSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = GeminiBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = GeminiBlue,
    onPrimary = GeminiLightBg,
    primaryContainer = GeminiLightSurfaceVariant,
    onPrimaryContainer = GeminiBlue,
    secondary = GeminiPurple,
    onSecondary = GeminiLightBg,
    secondaryContainer = GeminiLightSurfaceVariant,
    onSecondaryContainer = GeminiPurple,
    tertiary = GeminiCyan,
    background = GeminiLightBg,
    onBackground = TextPrimaryLight,
    surface = GeminiLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = GeminiLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = GeminiLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to deep obsidian dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
