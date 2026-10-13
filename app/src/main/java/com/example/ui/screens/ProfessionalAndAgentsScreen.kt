package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.example.data.model.AIAgent
import com.example.data.model.JobOpportunity
import com.example.data.network.OfflineAIIntelligence
import com.example.data.repository.AppRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalAndAgentsScreen(
    repository: AppRepository,
    onDirectChatWithAgent: (AIAgent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isArabic = repository.language.collectAsState().value.name == "ARABIC"
    val agents = remember { repository.getAIAgents() }
    val jobs = remember { repository.getSampleJobOpportunities() }

    var selectedTab by remember { mutableIntStateOf(0) }
    // 0: وكلاء Teamily المتخصصين (Multi-Agents)
    // 1: وظائف وتطوير LinkedIn (Job Match & Networking)

    var generatedPostText by remember {
        mutableStateOf(
            "يسعدني مشاركة رؤيتي حول مستقبل الذكاء الاصطناعي التوليدي في 2026: لم يعد الذكاء الاصطناعي مجرد أداة مساعدة، بل شريك عمل استراتيجي يقود القرارات ويسرع الإنجاز. فخور ببناء أنظمة تجمع بين قوة السحابة والخصوصية الفائقة للمحركات المحلية! #AI #Innovation #Leadership"
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
                        Icon(Icons.Default.Work, contentDescription = null, tint = GeminiGreen)
                        Column {
                            Text(
                                text = if (isArabic) "الشبكات المهنية ووكلاء Teamily" else "Professional Hub & Agents",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "خصائص LinkedIn وفرص عمل ووكلاء متخصصون" else "LinkedIn networking, jobs & agent team",
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
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeminiGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isArabic) "وكلاء الذكاء الاصطناعي (Teamily)" else "Teamily AI Agents", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isArabic) "منصة LinkedIn والوظائف" else "LinkedIn & Jobs", fontSize = 12.sp) }
                )
            }

            if (selectedTab == 0) {
                // Teamily Agents List
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
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "🤖", fontSize = 28.sp)
                                Column {
                                    Text(
                                        text = if (isArabic) "فريق العمل الذكي (Inspired by Teamily AI)" else "Multi-Agent Smart Workspace",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isArabic) "وكلاء متخصصون جاهزون لتنفيذ مهام البرمجة، إدارة المنتجات، والتسويق فورياً." else "Autonomous agents for software architecture, product growth, and marketing.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(agents, key = { it.id }) { agent ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(GeminiBlue.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = agent.avatarEmoji, fontSize = 22.sp)
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = agent.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(text = agent.roleTitle, fontSize = 12.sp, color = GeminiBlueGlow)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = GeminiGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "متاح للعمل",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GeminiGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = agent.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "التخصص: ${agent.specialty}", fontSize = 11.sp, color = GeminiPurpleGlow)

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onDirectChatWithAgent(agent) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("chat_agent_${agent.id}")
                                ) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (isArabic) "تفويض مهمة وبدء المحادثة" else "Delegate Task & Chat", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // LinkedIn Networking & Job Recommender
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // LinkedIn Post Generator Box
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBlueGlow.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = GeminiBlueGlow)
                                        Text(
                                            text = if (isArabic) "منشور احترافي لمنصة LinkedIn" else "LinkedIn Thought Leadership Post",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            generatedPostText = OfflineAIIntelligence.generateOfflineResponse("منشور لينكد إن جديد عن القيادة والذكاء الاصطناعي")
                                            Toast.makeText(context, "تمت إعادة الصياغة بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeminiSurfaceVariantDark),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = "توليد جديد ✦", fontSize = 11.sp, color = GeminiBlueGlow)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = generatedPostText,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("LinkedIn Post", generatedPostText))
                                            Toast.makeText(context, "تم نسخ المنشور بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GeminiBlue),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = if (isArabic) "نسخ المنشور" else "Copy Post", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, generatedPostText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة المنشور إلى LinkedIn"))
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GeminiGreen),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = if (isArabic) "مشاركة للشبكة" else "Share to Feed", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Jobs Header
                    item {
                        Text(
                            text = if (isArabic) "فرص عمل ذكية مقترحة لمهاراتك (AI Job Matches)" else "AI Recommended Job Matches",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Job Items
                    items(jobs, key = { it.id }) { job ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = job.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(text = "${job.company} • ${job.location}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = GeminiGreen.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "توافق ${job.matchScore}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = GeminiGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "💰 الراتب المتوقع: ${job.salaryRange}", fontSize = 12.sp, color = GeminiAmber)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = job.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 نصيحة التقديم: ${job.coverLetterTip}",
                                    fontSize = 11.sp,
                                    color = GeminiBlueGlow,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "طلب تقديم: ${job.title}")
                                            putExtra(Intent.EXTRA_TEXT, "أتقدم لشغل وظيفة ${job.title} في ${job.company}.\n${job.coverLetterTip}")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "تقديم للوظيفة عبر رسالة"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GeminiGreen),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = if (isArabic) "تجهيز رسالة التقديم والـ Cover Letter" else "Prepare Cover Letter", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
