package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.domain.AppUsageEstimate
import com.example.ui.BatteryViewModel
import com.example.ui.BatteryUiState
import com.example.ui.utils.formatDuration
import com.example.ui.utils.formatFloat
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorScreen(viewModel: BatteryViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Monitor", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { /* Help */ }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help")
                    }
                    IconButton(onClick = { /* More */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading || uiState.latestSample == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Collecting battery data...", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Use your phone normally. Battery statistics will become more accurate after several discharge sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    BatteryGaugeSection(uiState)
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .offset(y = (-24).dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DischargeStatusSection(uiState)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                BatteryUsageSection(uiState, Modifier.fillMaxHeight())
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                DischargingSpeedSection(uiState, Modifier.fillMaxHeight())
                            }
                        }
                        ForegroundAppUsageSection(uiState)
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryGauge(
    percent: Float,
    status: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(144.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(144.dp)) {
            val strokeWidth = 6.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = (percent / 100f) * 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percent.toInt()}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = status.uppercase(),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BatteryGaugeSection(uiState: BatteryUiState) {
    val percent = uiState.latestSample?.batteryPercent ?: 0f
    val isCharging = uiState.latestSample?.charging == true
    val status = if (isCharging) "CHARGING" else "DISCHARGING"
    val stats = uiState.usageStats
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(bottom = 48.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BatteryGauge(percent = percent, status = status)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Battery at ${percent.toInt()}% lasts for", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Very simplified estimate based on rates, assuming rate is positive meaning drop/hr
            val combinedHr = if (stats != null && stats.combinedDrainRatePerHr > 0f) percent / stats.combinedDrainRatePerHr else 0f
            val screenOnHr = if (stats != null && stats.screenOnDrainRatePerHr > 0f) percent / stats.screenOnDrainRatePerHr else 0f
            val screenOffHr = if (stats != null && stats.screenOffDrainRatePerHr > 0f) percent / stats.screenOffDrainRatePerHr else 0f
            
            EstimateItem("Combined", combinedHr)
            EstimateItem("Screen On", screenOnHr)
            EstimateItem("Screen Off", screenOffHr)
        }
    }
}

@Composable
fun EstimateItem(label: String, hours: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (hours > 0) {
            Text(formatDuration((hours * 3600000).toLong()), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White)
        } else {
            Text("...", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label.uppercase(), fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), letterSpacing = 0.5.sp)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, badge: String? = null) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CustomCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

@Composable
fun DischargeStatusSection(uiState: BatteryUiState) {
    CustomCard {
        SectionTitle("Discharge status", badge = "Measured")
        val sample = uiState.latestSample
        val stats = uiState.usageStats
        
        val currentStr = if (sample != null && sample.currentMa != 0) {
            val powerW = com.example.utils.BatteryStatsParser.calculatePowerW(sample.voltageMv, sample.currentMa)
            "${formatFloat(powerW)} W / ${sample.currentMa} mA"
        } else "Unavailable"
        
        val avgUsage = if (stats != null && stats.combinedDrainRatePerHr > 0) {
            "-${formatFloat(stats.combinedDrainRatePerHr)}%/h"
        } else "Collecting..."
        
        val tempStr = if (sample != null) "${formatFloat(sample.temperatureC)} °C" else "N/A"
        val voltageStr = if (sample != null) "${sample.voltageMv} mV" else "N/A"
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("Battery current", currentStr)
                MetricCard("Temperature", tempStr)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("Average usage", avgUsage, valueColor = Color(0xFFD32F2F))
                MetricCard("Voltage", voltageStr)
            }
        }
    }
}

@Composable
fun GridCard(title: String, bigText: String, subText: String, subTextColor: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(bigText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f) // placeholder
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(subText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}


@Composable
fun BatteryUsageSection(uiState: BatteryUiState, modifier: Modifier = Modifier) {
    val stats = uiState.usageStats ?: return
    CustomCard(modifier) {
        SectionTitle("Battery usage")
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatFloat(stats.totalPercentConsumed)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stats.totalPercentConsumed.coerceIn(0f, 100f) / 100f)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${stats.totalCapacityMahConsumed.toInt()} mAh in ${formatDuration(stats.durationMs)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ScreenOffUsageSection(uiState: BatteryUiState) {
    // Hidden to match layout
}

@Composable
fun DischargingSpeedSection(uiState: BatteryUiState, modifier: Modifier = Modifier) {
    val stats = uiState.usageStats ?: return
    CustomCard(modifier) {
        SectionTitle("Discharge speed")
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Screen On", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${formatFloat(stats.screenOnDrainRatePerHr)}%/h", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Screen Off", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${formatFloat(stats.screenOffDrainRatePerHr)}%/h", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Average", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${formatFloat(stats.combinedDrainRatePerHr)}%/h", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ForegroundAppUsageSection(uiState: BatteryUiState) {
    CustomCard {
        SectionTitle("Foreground app battery usage", badge = "Estimated")
        
        val apps = uiState.usageStats?.appUsage ?: emptyList()
        if (apps.isEmpty()) {
            Text("No foreground app data available.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                apps.take(10).forEach { app ->
                    AppUsageRow(app)
                }
            }
        }
    }
}

@Composable
fun AppUsageRow(app: AppUsageEstimate) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF222222), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                app.appName.take(1).uppercase(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(app.appName, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
                Text(if (app.usageTimeMs > 0) formatDuration(app.usageTimeMs) else "< 1m", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp))
                Text("${formatFloat(app.percentage)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(app.percentage.coerceIn(0f, 10f) / 10f)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("${formatFloat(app.capacityMah)} mAh", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Right)
    }
}
