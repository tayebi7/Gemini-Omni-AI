package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProvider
import com.example.ui.theme.*

@Composable
fun GeminiSparkleIcon(
    modifier: Modifier = Modifier,
    size: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val glowProgress by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .size((size * glowProgress).dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(GeminiBlueGlow, GeminiPurpleGlow, GeminiPink)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Gemini AI Sparkle",
            tint = Color.White,
            modifier = Modifier.size((size * 0.75).dp)
        )
    }
}

@Composable
fun ProviderBadgeChip(
    provider: AIProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (provider) {
        AIProvider.GEMINI -> GoogleGeminiColor to "✦ Gemini 3.5"
        AIProvider.OPENAI -> OpenAIColor to "⚡ GPT-4o"
        AIProvider.ANTHROPIC -> AnthropicColor to "◆ Claude 3.5"
        AIProvider.NVIDIA_NIM -> NvidiaColor to "🟢 NVIDIA NIM"
        AIProvider.AMD_ROCM -> AmdColor to "🔴 AMD ROCm"
        AIProvider.OFFLINE_LOCAL -> OfflineLocalColor to "💾 Offline NPU"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun CodeBlockView(
    codeText: String,
    language: String = "code",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GeminiDeepSpace,
        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBorderDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GeminiSurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GeminiBlueGlow
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("code", codeText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ الكود بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ الكود",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = codeText,
                modifier = Modifier
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState()),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TextPrimaryDark,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun FormattedMessageBody(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Simple code block detection for clean presentation
        if (content.contains("```")) {
            val parts = content.split("```")
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    // This is a code block
                    val lines = part.trim().lines()
                    val lang = lines.firstOrNull() ?: "code"
                    val actualCode = if (lines.size > 1) lines.drop(1).joinToString("\n") else part
                    CodeBlockView(codeText = actualCode, language = lang)
                } else if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            Text(
                text = content,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PromptSuggestionChip(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon()
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
