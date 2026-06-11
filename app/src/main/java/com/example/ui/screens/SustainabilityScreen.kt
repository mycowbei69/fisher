package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.GeminiClient
import com.example.ui.viewmodel.FishingViewModel
import kotlinx.coroutines.launch

@Composable
fun SustainabilityScreen(viewModel: FishingViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var selectedFishType by remember { mutableStateOf("紅鯛 / 嘉鱲") }
    var aiEcoAdvice by remember { mutableStateOf("") }
    var isLoadingEco by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Heading
        Text(
            text = "智慧海釣與永續海洋",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("sustainability_section_title")
        )
        Text(
            text = "結合雲端 AI 與合理捕撈規範，保護我們的海洋資源，實現年年有巨物、竿竿大咬的永續生態。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Three Educational Banners matching visual guidelines!
        EducateCard(
            icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = "永續", tint = Color(0xFFEF5350), modifier = Modifier.size(28.dp)) },
            title = "友善釣法・抓小放育",
            description = "提倡不帶走小魚（如 20 公分以下之黑鯛、真鯛及石斑），釋放懷卵的雌魚。使用單圈無倒刺鉤，降低對非目標幼魚的創傷，維護海域永續繁殖力。"
        )

        EducateCard(
            icon = { Icon(Icons.Default.Recycling, contentDescription = "環保", tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp)) },
            title = "帶走垃圾・零死角礁岸",
            description = "廢棄物、剪碎的尼龍釣線、鉛錘應帶回岸上處理。割傷礁岩的廢線可能纏繞海鳥、綠蠵龜，破壞珊瑚礁自然生態；讓我們共同守護乾淨的水世界。"
        )

        EducateCard(
            icon = { Icon(Icons.Default.Sailing, contentDescription = "安全", tint = Color(0xFF29B6F6), modifier = Modifier.size(28.dp)) },
            title = "科學氣象・安全出行",
            description = "主動開啟 App 氣象風速警報與安全地理圍欄功能，配戴救生衣與防滑磯釣鞋。避開暴潮與東北季風巨浪，才是資深釣友的智慧選擇。"
        )

        // AI Advisor section
        Card(
            modifier = Modifier.fillMaxWidth().testTag("ai_eco_consultant_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Nature,
                        contentDescription = "生態顧問",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🌲 台灣沿海主要魚種 永續體長與禁捕期 查詢",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "點選您想查詢的海洋底棲/洄游物種，AI 顧問將提供法定或建議的「最小撈捕體長（寸）」及「產卵繁衍季節」：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selectors row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("紅鯛 / 嘉鱲", "黑鯛 / 黑毛", "斑類 / 石斑", "白帶魚", "透抽 / 鎖管").forEach { type ->
                        val active = selectedFishType == type
                        FilterChip(
                            selected = active,
                            onClick = { selectedFishType = type },
                            label = { Text(type, fontSize = 11.sp) }
                        )
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoadingEco = true
                            aiEcoAdvice = "AI 正在查詢農業部漁業署法規，並融合海洋生物多樣性數據庫分析中..."
                            
                            val prompt = """
                                您是政府農業部漁業署、海洋保育署之特派AI科普與保育宣導大使。
                                請針對指定魚種類群：$selectedFishType
                                提供以下科普內容：
                                1. 建議或法規規定的「最小可捕撈體長、寸」（多小以下嚴禁帶走必須即刻釋放）
                                2. 今日該魚類的產卵高峰季/禁漁保護時限說明
                                3. 一個具有感染力、呼籲釣客珍惜海洋的宣導佳句
                                
                                請繁體中文，親和，排版非常精巧整齊。
                            """.trimIndent()

                            val response = GeminiClient.getGeminiReport(prompt, "您是一位海洋保護宣導大師，語氣充滿智慧愛心，並深刻瞭解大洋生態。")
                            aiEcoAdvice = response
                            isLoadingEco = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("eco_advisor_button"),
                    enabled = !isLoadingEco
                ) {
                    if (isLoadingEco) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.FilterVintage, contentDescription = "查詢")
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("查詢永續體長與禁捕時機")
                }

                // AI Response
                AnimatedVisibility(visible = aiEcoAdvice.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🍀 永續海洋專家回復：",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { aiEcoAdvice = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "關閉", modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = aiEcoAdvice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp,
                                modifier = Modifier.testTag("ai_eco_advice_text")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EducateCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
