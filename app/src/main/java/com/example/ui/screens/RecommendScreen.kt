package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FishingSpot
import com.example.data.network.GeminiClient
import com.example.ui.viewmodel.FishingViewModel
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun RecommendScreen(viewModel: FishingViewModel) {
    val coroutineScope = rememberCoroutineScope()
    var selectedSpotIndex by remember { mutableStateOf(0) }
    val spot = viewModel.fishingSpots.getOrNull(selectedSpotIndex) ?: viewModel.fishingSpots[0]

    // AI bait state
    var selectedTargetFish by remember { mutableStateOf("金目鱸") }
    var selectedLureType by remember { mutableStateOf("路亞 (米諾假餌)") }
    var aiBaitAdvice by remember { mutableStateOf("") }
    var isAnalyzingBait by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section title
        item {
            Text(
                text = "最佳海釣時機與釣點推薦",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("recommend_section_title")
            )
            Text(
                text = "綜合昨日捕獲紀錄及當前風浪、溫差指標，計算今日最高索餌活性釣點。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Leaderboard/Podium Layout matching user's illustration!
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("podium_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🏆 今日精選潛力釣點排行榜",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Podium View
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // 2nd Place (Left)
                        PodiumColumn(
                            positionText = "2",
                            spotName = viewModel.fishingSpots[1].name.replace("基隆嶼", "").replace("澎湖", "").replace("萊萊", ""),
                            targetFish = viewModel.fishingSpots[1].targetFish.split("、").first(),
                            stars = viewModel.fishingSpots[1].stars,
                            heightDp = 110.dp,
                            color = Color(0xFFCFD8DC),
                            onClick = { selectedSpotIndex = 1 },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 1st Place (Center)
                        PodiumColumn(
                            positionText = "1",
                            spotName = viewModel.fishingSpots[0].name.replace("基隆嶼", "").replace("澎湖", "").replace("萊萊", ""),
                            targetFish = viewModel.fishingSpots[0].targetFish.split("、").first(),
                            stars = viewModel.fishingSpots[0].stars,
                            heightDp = 140.dp,
                            color = Color(0xFFFFD54F),
                            onClick = { selectedSpotIndex = 0 },
                            modifier = Modifier.weight(1.1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 3rd Place (Right)
                        PodiumColumn(
                            positionText = "3",
                            spotName = viewModel.fishingSpots[2].name.replace("基隆嶼", "").replace("澎湖", "").replace("萊萊", ""),
                            targetFish = viewModel.fishingSpots[2].targetFish.split("、").first(),
                            stars = viewModel.fishingSpots[2].stars,
                            heightDp = 90.dp,
                            color = Color(0xFFFFB74D),
                            onClick = { selectedSpotIndex = 2 },
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                    Text(
                        text = "* 點擊排名柱狀圖可查看該推薦點的實時潮差與主客觀咬度分析",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Details of selected Fishing spot
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("fishing_spot_detail_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = spot.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "隸屬區域: ${spot.locationName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "評價", tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                            Text(
                                text = " ${spot.stars} 分",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "🎯 主要魚種：${spot.targetFish}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = spot.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                    // Peak Bite Times Line Chart
                    Text(
                        text = "📈 實時大咬高潛力時段預測 (潮汐活性曲線)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    BiteTimeChart()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "最佳大咬時段", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "最佳大咬時段: ${spot.majorBiteTimes}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Map preview action - simulate navigation jump
                    TextButton(
                        onClick = { viewModel.jumpToLocation(spot.latitude, spot.longitude) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "導航", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("將模擬 vessel 定位設為此處")
                    }
                }
            }
        }

        // Section: AI Bait Match
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("ai_bait_matcher_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SetMeal,
                            contentDescription = "AI 餌料配方",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "釣餌與釣組 AI 快速配對器",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "選擇目標魚與您擅長之釣法，AI 助手將推薦配重、擬餌規格及釣法操流手勢：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target Fish selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("目標：", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(48.dp))
                        listOf("金目鱸", "黑鯛", "白帶魚", "紅魽").forEach { fishName ->
                            val active = selectedTargetFish == fishName
                            FilterChip(
                                selected = active,
                                onClick = { selectedTargetFish = fishName },
                                label = { Text(fishName) }
                            )
                        }
                    }

                    // Lure Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("釣法：", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(48.dp))
                        listOf("路亞 (米諾/鐵板)", "磯釣 (浮標南極蝦)", "前打").forEach { typeName ->
                            val active = selectedLureType == typeName
                            FilterChip(
                                selected = active,
                                onClick = { selectedLureType = typeName },
                                label = { Text(typeName) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isAnalyzingBait = true
                                aiBaitAdvice = "AI 正在融合今日潮汐波浪係數，計算最佳雙鉤釣棚深度與配方..."
                                val prompt = """
                                    您是海釣世家第三代兼現代智慧海洋科技工程師。
                                    請針對以下配置推薦最頂尖的釣魚方案：
                                    目標魚類: $selectedTargetFish
                                    使用釣法: $selectedLureType
                                    
                                    請務必詳列：
                                    1. 推薦的最具誘惑力擬餌/生餌（如規格、克數、南極蝦誘餌籠份量）
                                    2. 釣竿硬度與母線、子線（號數）配置
                                    3. 水域操竿手法技巧（如何模擬弱生動態或誘餌深度控制）
                                    
                                    請使用有條理的繁體中文，幽默專業且提倡生態永續。
                                """.trimIndent()

                                val response = GeminiClient.getGeminiReport(prompt, "您是專業的釣具專賣店店長和資深老釣手。")
                                aiBaitAdvice = response
                                isAnalyzingBait = false
                            }
                        },
                        enabled = !isAnalyzingBait,
                        modifier = Modifier.fillMaxWidth().testTag("bait_matcher_button")
                    ) {
                        if (isAnalyzingBait) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "適配")
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("為我配對最殺擬餌與技巧")
                    }

                    // Output area
                    AnimatedVisibility(visible = aiBaitAdvice.isNotEmpty()) {
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
                                        text = "🦈 AI 老司機對策報告：",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { aiBaitAdvice = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "關閉", modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = aiBaitAdvice,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.testTag("ai_bait_advice_text")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumColumn(
    positionText: String,
    spotName: String,
    targetFish: String,
    stars: Float,
    heightDp: androidx.compose.ui.unit.Dp,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Label on top
        Text(
            text = spotName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "🐟$targetFish",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Block with number
        Box(
            modifier = Modifier
                .width(55.dp)
                .height(heightDp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Crown or icon on top block
                if (positionText == "1") {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "金冠",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }
                
                Text(
                    text = positionText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun BiteTimeChart() {
    val chartColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        val width = size.width
        val height = size.height
        
        // Draw standard tidal/bite curve line (sine wave peak mimicking morning and afternoon bite times)
        val path = Path()
        
        val margin = 10f
        val plotWidth = width - 2 * margin
        val plotHeight = height - 2 * margin
        
        path.moveTo(margin, plotHeight + margin)
        
        for (x in 0..100) {
            val ratio = x / 100f
            val actualX = margin + ratio * plotWidth
            // Double wave peaks at hours 05:00 and 18:00
            val wave = sin(ratio * 2 * Math.PI.toFloat() * 1.5f - 1.2f) * 0.4f +
                       sin(ratio * 2 * Math.PI.toFloat() * 0.5f) * 0.3f
            
            // Normalize y from -0.7 to 0.7 to plotHeight
            val normalizedY = ((wave + 0.7f) / 1.4f) * plotHeight
            val actualY = height - margin - normalizedY
            
            if (x == 0) {
                path.moveTo(actualX, actualY)
            } else {
                path.lineTo(actualX, actualY)
            }
        }
        
        // Draw Grid Lines (Hours: 00, 06, 12, 18, 24)
        for (i in 0..4) {
            val gridX = margin + (i / 4f) * plotWidth
            drawLine(
                color = gridColor,
                start = Offset(gridX, margin),
                end = Offset(gridX, height - margin),
                strokeWidth = 1f
            )
        }
        
        // Draw the main curve
        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(width = 8f)
        )
        
        // Draw hot regions indicator circles
        // Morning Peak: ~05:30 (around 23% mark)
        val morningX = margin + 0.23f * plotWidth
        val morningY = height - margin - (((sin(0.23f * 2 * Math.PI.toFloat() * 1.5f - 1.2f) * 0.4f + sin(0.23f * 2 * Math.PI.toFloat() * 0.5f) * 0.3f) + 0.7f) / 1.4f) * plotHeight
        drawCircle(
            color = Color(0xFFE53935),
            radius = 12f,
            center = Offset(morningX, morningY)
        )
        
        // Evening Peak: ~18:30 (around 77% mark)
        val eveningX = margin + 0.77f * plotWidth
        val eveningY = height - margin - (((sin(0.77f * 2 * Math.PI.toFloat() * 1.5f - 1.2f) * 0.4f + sin(0.77f * 2 * Math.PI.toFloat() * 0.5f) * 0.3f) + 0.7f) / 1.4f) * plotHeight
        drawCircle(
            color = Color(0xFFE53935),
            radius = 12f,
            center = Offset(eveningX, eveningY)
        )
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("半夜 00:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("清晨 06:00 (大咬)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
        Text("中午 12:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("黃昏 18:00 (大咬)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
        Text("午夜 24:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
