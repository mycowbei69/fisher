package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherStation
import com.example.ui.viewmodel.FishingViewModel

@Composable
fun WeatherScreen(viewModel: FishingViewModel) {
    val selectedStation by viewModel.selectedStation.collectAsState()
    val isRefreshing by viewModel.isWeatherRefreshing.collectAsState()
    val aiReport by viewModel.aiWeatherReport.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Heading
        Text(
            text = "海洋氣象觀測中心",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("weather_section_title")
        )
        Text(
            text = "串接政府多點海事觀測 Open Data，提供潮汐、浪高、海溫與風力監測數據",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // LazyRow select location
        Text(
            text = "觀測站點切換",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("station_tabs_row")
        ) {
            items(viewModel.weatherStations) { station ->
                val isSelected = selectedStation.name == station.name
                val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                    modifier = Modifier
                        .clickable { viewModel.selectStation(station) }
                        .testTag("station_tab_${station.name}")
                ) {
                    Text(
                        text = (if (station.safetyStatus.contains("危險")) "⚠️" else "") + station.name,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Selected station details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedStation.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GPS 緯經度: ${selectedStation.latitude}, ${selectedStation.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Safety Badge
                    val safetyColor = when (selectedStation.safetyStatus) {
                        "安全" -> Color(0xFF2E7D32)
                        "注意 (浪高風大)" -> Color(0xFFEF6C00)
                        else -> Color(0xFFC62828)
                    }
                    Surface(
                        color = safetyColor,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = selectedStation.safetyStatus,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Grid of 4 stats
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherStatItem(
                            icon = { Icon(Icons.Default.WaterDrop, contentDescription = "潮汐", tint = Color(0xFF1976D2)) },
                            title = "潮位高度",
                            value = "${selectedStation.tideState} (${selectedStation.tideHeight}公尺)",
                            modifier = Modifier.weight(1f)
                        )
                        WeatherStatItem(
                            icon = { Icon(Icons.Default.Waves, contentDescription = "浪高", tint = Color(0xFF00ACC1)) },
                            title = "當前波高",
                            value = "${selectedStation.waveHeight}公尺 (${selectedStation.waveDirection})",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherStatItem(
                            icon = { Icon(Icons.Default.Air, contentDescription = "風速", tint = Color(0xFF78909C)) },
                            title = "平均風力",
                            value = "${selectedStation.windSpeed} km/h (${selectedStation.windDirection})",
                            modifier = Modifier.weight(1f)
                        )
                        WeatherStatItem(
                            icon = { Icon(Icons.Default.Thermostat, contentDescription = "溫度", tint = Color(0xFFD84315)) },
                            title = "海水 / 氣溫",
                            value = "${selectedStation.waterTemperature}°C / ${selectedStation.airTemperature}°C",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "天氣",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "氣象狀況: ${selectedStation.weatherStatus}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = { viewModel.refreshWeather() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("weather_refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "更新", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(text = if (isRefreshing) "同步更新中" else "串接 OpenData 更新")
                    }
                }
            }
        }

        // Gemini AI Consultation Section
        Card(
            modifier = Modifier.fillMaxWidth().testTag("ai_weather_assessment_card"),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI 綜合評估",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "雲端 AI 智慧海釣綜合評估",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "分析特定水溫與潮差交互條件下，今日最佳索餌時辰與防護守則",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                if (aiReport.isEmpty() && !isGeneratingReport) {
                    Text(
                        text = "點擊下方按鈕，啟用雲端大語言模型，即刻評估本觀測海域的海釣安全指數與巨種魚咬餌活性活性。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Button(
                        onClick = { viewModel.generateAiWeatherReport() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("trigger_ai_report_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "生成")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("計算本海域 AI 索餌活性分析")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 實時 AI 分析報告：",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isGeneratingReport) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            // Display response text with nice layout
                            Text(
                                text = aiReport,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp,
                                modifier = Modifier.testTag("ai_report_textbox")
                            )

                            if (!isGeneratingReport) {
                                TextButton(
                                    onClick = { viewModel.generateAiWeatherReport() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.Replay, contentDescription = "重新評估", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("重新計算評估報告", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherStatItem(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
