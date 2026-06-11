package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FishingViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SafetyScreen(viewModel: FishingViewModel) {
    val lat by viewModel.userLatitude.collectAsState()
    val lng by viewModel.userLongitude.collectAsState()
    val speed by viewModel.userSpeed.collectAsState()
    val bearing by viewModel.userBearing.collectAsState()

    val breached by viewModel.isGeofenceBreached.collectAsState()
    val breachMsg by viewModel.breachMessage.collectAsState()
    val breachZoneName by viewModel.breachZoneName.collectAsState()

    // Flashing alarm color animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarAlarm")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlarmAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "釣客安全導航與地理圍欄",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("safety_section_title")
        )
        Text(
            text = "提供實時海洋定位、多點危險海域警報器、禁釣與軍事營區地理邊界主動防護",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Radar Canvas Map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .testTag("radar_canvas_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)) // Dark space/naval colors
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Radar grid lines and target zones drawn in Canvas
                RadarMapDrawing(
                    lat = lat,
                    lng = lng,
                    bearing = bearing,
                    geofenceZones = viewModel.geofenceZones,
                    modifier = Modifier.fillMaxSize()
                )

                // Flash Alarm Overlay if Breached
                if (breached) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFD32F2F).copy(alpha = alphaAnim * 0.25f))
                    )
                }

                // GPS Stats Info Panel
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "🛰️ VESSEL GPS 定位狀態",
                            color = Color(0xFF4FC3F7),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "LAT: $lat", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text(text = "LNG: $lng", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text(text = "航速: ${String.format("%.1f", speed)} 節 ( knots)", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Simple Compass Dial indicator
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(10.dp)
                        .size(60.dp)
                        .align(Alignment.TopEnd)
                        .border(1.dp, Color(0xFF81C784), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${Math.round(bearing)}°\n" + when {
                                bearing in 337.5..360.0 || bearing in 0.0..22.5 -> "北"
                                bearing in 22.5..67.5 -> "東北"
                                bearing in 67.5..112.5 -> "東"
                                bearing in 112.5..157.5 -> "東南"
                                bearing in 157.5..202.5 -> "南"
                                bearing in 202.5..247.5 -> "西南"
                                bearing in 247.5..292.5 -> "西"
                                else -> "西北"
                            },
                            color = Color(0xFF81C784),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active notification alert banner
        Card(
            modifier = Modifier.fillMaxWidth().testTag("safety_alert_banner"),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    breached -> MaterialTheme.colorScheme.errorContainer
                    breachZoneName.contains("接近") -> Color(0xFFFFF3E0)
                    else -> Color(0xFFE8F5E9)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = when {
                                breached -> MaterialTheme.colorScheme.error
                                breachZoneName.contains("接近") -> Color(0xFFEF6C00)
                                else -> Color(0xFF2E7D32)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            breached -> Icons.Default.Campaign
                            breachZoneName.contains("接近") -> Icons.Default.Warning
                            else -> Icons.Default.Shield
                        },
                        contentDescription = "安全哨兵狀態",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            breached -> "⚠️ 越界紅色警戒！"
                            breachZoneName.contains("接近") -> "🔔 黃色靠近安全警示"
                            else -> "安全保衛中"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = when {
                            breached -> MaterialTheme.colorScheme.onErrorContainer
                            breachZoneName.contains("接近") -> Color(0xFFE65100)
                            else -> Color(0xFF1B5E20)
                        }
                    )
                    Text(
                        text = if (breachMsg.isEmpty()) "正在圍繞安全防線進行掃描。主動防止碰撞船舶與跌落島礁岩。" else breachMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Vessel simulation manual pilot movement joystick buttons!
        Card(
            modifier = Modifier.fillMaxWidth().testTag("vessel_joystick_card")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🧭 手動模擬航行控制（靠近或遠離危險岩石區）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coordinates Adjustment Buttons grid
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.moveUserLocation(0.005, 0.0) },
                            modifier = Modifier.size(70.dp, 38.dp).testTag("move_up_btn"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("北 ⬆️", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { viewModel.moveUserLocation(0.0, -0.005) },
                                modifier = Modifier.size(70.dp, 38.dp).testTag("move_left_btn"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("⬅️ 西", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { viewModel.moveUserLocation(0.0, 0.005) },
                                modifier = Modifier.size(70.dp, 38.dp).testTag("move_right_btn"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("東 ➡️", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            onClick = { viewModel.moveUserLocation(-0.005, 0.0) },
                            modifier = Modifier.size(70.dp, 38.dp).testTag("move_down_btn"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("南 ⬇️", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Hot Jump keys
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(text = "快速瞬移:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        
                        FilledTonalButton(
                            onClick = { viewModel.jumpToLocation(25.0118, 122.0003) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("萊萊危險區 🚩", fontSize = 11.sp)
                        }
                        FilledTonalButton(
                            onClick = { viewModel.jumpToLocation(25.1905, 121.7801) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("基隆嶼邊界 🚩", fontSize = 11.sp)
                        }
                        FilledTonalButton(
                            onClick = { viewModel.jumpToLocation(23.6015, 119.5085) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("返回澎湖安全碼頭 ⚓", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadarMapDrawing(
    lat: Double,
    lng: Double,
    bearing: Double,
    geofenceZones: List<com.example.ui.viewmodel.GeofenceZone>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2, height / 2)
        val outerRadius = Math.min(width, height) / 2.5f

        // Draw Radar polar coordinate circles (Background rings)
        for (i in 1..4) {
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = 0.15f),
                radius = outerRadius * (i / 4f),
                center = center,
                style = Stroke(width = 2f)
            )
        }

        // Radar coordinate crosses
        drawLine(
            color = Color(0xFF00E676).copy(alpha = 0.2f),
            start = Offset(center.x - outerRadius, center.y),
            end = Offset(center.x + outerRadius, center.y),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFF00E676).copy(alpha = 0.2f),
            start = Offset(center.x, center.y - outerRadius),
            end = Offset(center.x, center.y + outerRadius),
            strokeWidth = 2f
        )

        // Draw coordinates text headings
        // Scale conversion: 1 degree latitude = 15000 pixels in Radar scale for visibility
        val pixelScale = 12000.0

        for (zone in geofenceZones) {
            // Calculate relative offset in meters/degrees from user's current GPS position
            val dLat = zone.latitude - lat
            val dLng = zone.longitude - lng

            // Convert to Radar canvas coordinates
            val relativeX = center.x + (dLng * pixelScale * Math.cos(Math.toRadians(lat))).toFloat()
            val relativeY = center.y - (dLat * pixelScale).toFloat() // Flip Y since higher screen is lower index

            // Only draw if within bounds of radar
            val distanceToCircle = Math.sqrt(((relativeX - center.x).pow(2) + (relativeY - center.y).pow(2)).toDouble()).toFloat()
            if (distanceToCircle <= outerRadius * 1.5f) {
                // Zone center point (Dangerous reef red marker)
                drawCircle(
                    color = Color(0xFFE53935).copy(alpha = 0.6f),
                    radius = 10f,
                    center = Offset(relativeX, relativeY)
                )

                // Geofence Circle radius
                // 1 meter in GPS coordinates is about pixel scale factor
                val renderRadius = (zone.radiusMeters * 0.28).toFloat() // estimate scale factor
                drawCircle(
                    color = Color(0xFFE53935).copy(alpha = 0.2f),
                    radius = if (renderRadius < 15f) 30f else renderRadius,
                    center = Offset(relativeX, relativeY),
                    style = Stroke(width = 3f)
                )

                // Dangerous Label
                // Simplification for canvas text is skipping name directly or rendering a custom dot
            }
        }

        // Draw User boat vessel pointer inside the center (Always center on radar!)
        val pointerLength = 30f
        val angleRad = Math.toRadians(bearing - 90.0) // Rotate coordinate system so 0 deg is North
        val headX = center.x + pointerLength * Math.cos(angleRad).toFloat()
        val headY = center.y + pointerLength * Math.sin(angleRad).toFloat()

        // Draw boat hull direction vector (Green/Cyan Arrow)
        drawLine(
            color = Color(0xFF29B6F6),
            start = center,
            end = Offset(headX, headY),
            strokeWidth = 6f
        )
        drawCircle(
            color = Color(0xFF29B6F6),
            radius = 12f,
            center = center
        )
    }
}

// Math power helper extension
fun Float.pow(n: Int): Float {
    var res = 1f
    for (i in 1..n) {
        res *= this
    }
    return res
}
