package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.CatchEntity
import com.example.ui.viewmodel.FishingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CatchScreen(viewModel: FishingViewModel) {
    val catches by viewModel.catchLogs.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val cloudAiResponse by viewModel.cloudAiAnalysisResponse.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "歷史漁獲紀錄與評價",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("catch_section_title")
                    )
                    Text(
                        text = "支援離線暫存記錄，回到有網路區域時一鍵上傳 AWS 雲端 AI 機器學習分析。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sync offline toolbar
            if (unsyncedCount > 0 || isSyncing) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("sync_toolbar_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = "未同步",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "有 $unsyncedCount 筆離線漁獲尚未同步至 AWS 雲端",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Button(
                                onClick = { viewModel.syncCatchesToCloud() },
                                enabled = !isSyncing,
                                modifier = Modifier.testTag("aws_sync_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("同步至 AWS AI 分析")
                            }
                        }
                    }
                }
            }

            // AWS Cloud AI advice report display if loaded
            AnimatedVisibility(visible = cloudAiResponse.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("aws_cloud_ai_response_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDone, contentDescription = "AWS AI", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🤖 AWS 雲端大數據個人化推薦回傳：",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(onClick = { viewModel.clearCloudAiResponse() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "關閉", modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = cloudAiResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Catches List
            if (catches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "無資料",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "尚無任何捕獲日誌",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "按下右下角 ＋ 按鈕以建檔第一筆離線海釣點漁獲！",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("catch_logs_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(catches) { item ->
                        CatchItemRow(item = item, onDelete = { viewModel.deleteCatch(item) })
                    }
                }
            }
        }

        // Add Floating Action Button to show dialog
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp)
                .testTag("add_catch_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "建檔紀錄")
        }

        // Add Catch Dialog
        if (showAddDialog) {
            AddCatchDialog(
                defaultLocation = selectedStation.name,
                defaultTide = selectedStation.tideState,
                defaultWind = selectedStation.windSpeed,
                defaultWave = selectedStation.waveHeight,
                defaultTemp = selectedStation.airTemperature,
                onDismiss = { showAddDialog = false },
                onSave = { name, weight, length, loc, tide, wind, wave, temp, stars, diary ->
                    viewModel.addCatchLog(name, weight, length, loc, tide, wind, wave, temp, stars, diary)
                    showAddDialog = false
                    viewModel.updateUnsyncedCount()
                }
            )
        }
    }
}

@Composable
fun CatchItemRow(item: CatchEntity, onDelete: () -> Unit) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = formatter.format(Date(item.timestamp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Fish Name & Sync status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SetMeal, contentDescription = "魚種", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.fishName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Sync status indicator
                Surface(
                    color = if (item.isSynced) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = "同步",
                            tint = if (item.isSynced) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (item.isSynced) "AWS 已備份" else "離線暫存",
                            color = if (item.isSynced) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "⚖重量: ${item.weight} kg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = "📏長度: ${item.length} cm", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                
                // Stars rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= item.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "星級",
                            tint = if (i <= item.rating) Color(0xFFFFB300) else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 2.dp))

            // Environments Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📍 [${item.locationName}] 當時潮汐: ${item.tide}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🌬氣候參數: 風速 ${item.windSpeed} km/h | 浪高 ${item.waveHeight} m | 溫 ${item.temperature}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            if (item.notes.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "📝 日記點評: ${item.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 14.sp
                    )
                }
            }

            Text(
                text = "登錄時間: $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun AddCatchDialog(
    defaultLocation: String,
    defaultTide: String,
    defaultWind: Double,
    defaultWave: Double,
    defaultTemp: Double,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, String, String, Double, Double, Double, Int, String) -> Unit
) {
    var fishName by remember { mutableStateOf("金目鱸") }
    var weightText by remember { mutableStateOf("1.5") }
    var lengthText by remember { mutableStateOf("45.0") }
    
    var locationName by remember { mutableStateOf(defaultLocation) }
    var tideState by remember { mutableStateOf(defaultTide) }
    var windSpeed by remember { mutableStateOf(defaultWind.toString()) }
    var waveHeight by remember { mutableStateOf(defaultWave.toString()) }
    var temperature by remember { mutableStateOf(defaultTemp.toString()) }
    
    var rating by remember { mutableStateOf(4) }
    var notes by remember { mutableStateOf("") }
    
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("add_catch_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "✍️ 登錄最新智慧海釣漁獲",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "* 本系統會自動調用 ${defaultLocation} 的觀測 OpenData 水溫與潮位為您智能預存，確保數據精確性。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fishName,
                    onValueChange = { fishName = it },
                    label = { Text("捕獲魚種名稱") },
                    modifier = Modifier.fillMaxWidth().testTag("add_fish_name_field")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("重量 (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("add_fish_weight_field")
                    )
                    OutlinedTextField(
                        value = lengthText,
                        onValueChange = { lengthText = it },
                        label = { Text("長度 (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("add_fish_length_field")
                    )
                }

                Text(text = "評價本次咬度 (星級)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "評分 $i 星",
                                tint = if (i <= rating) Color(0xFFFFB300) else Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 2.dp))

                Text(text = "自動預存 OpenData 海象參數 (可手動修正)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("海域點/站點名稱") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tideState,
                    onValueChange = { tideState = it },
                    label = { Text("當時潮汐") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = windSpeed,
                        onValueChange = { windSpeed = it },
                        label = { Text("風速 (km/h)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = waveHeight,
                        onValueChange = { waveHeight = it },
                        label = { Text("浪高 (m)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("詳細點評與水流筆記") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: 1.0
                            val l = lengthText.toDoubleOrNull() ?: 30.0
                            val wind = windSpeed.toDoubleOrNull() ?: defaultWind
                            val wave = waveHeight.toDoubleOrNull() ?: defaultWave
                            val temp = temperature.toDoubleOrNull() ?: defaultTemp
                            onSave(fishName, w, l, locationName, tideState, wind, wave, temp, rating, notes)
                        },
                        modifier = Modifier.testTag("save_catch_button")
                    ) {
                        Text("本機離線保存")
                    }
                }
            }
        }
    }
}
