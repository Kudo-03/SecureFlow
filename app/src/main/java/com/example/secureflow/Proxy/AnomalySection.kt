package com.example.secureflow.ui.sections

import Anomaly
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.example.secureflow.Proxy.AnomalyViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.collections.forEachIndexed

@Composable
fun AnomalySection(
    viewModel: AnomalyViewModel,
    userId: String
) {
    // Collect states from ViewModel
    val anomalies by viewModel.anomalies.collectAsState()
    val loading by viewModel.isLoading.collectAsState()

    // Reload anomalies when user changes
    LaunchedEffect(userId) {
        viewModel.loadUserAnomalies(userId)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ────────────────────────────────
            Text(
                text = "Recent Anomalies",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            LiveIndicator()




            // ── Loading State ─────────────────────────
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp)
                )
                return@Column
            }

            // ── Empty State ───────────────────────────
            if (anomalies.isEmpty()) {
                Text(
                    text = "No anomalies found for your account.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
                return@Column
            }

            // ── Summary Row ───────────────────────────
            val recentAnomalies = anomalies.takeLast(10)
            val total = recentAnomalies.size
            val detected = recentAnomalies.count { it.anomaly }
            val percentage = if (total > 0) (detected.toFloat() / total) * 100 else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Detected: $detected / $total",
                    fontSize = 14.sp,
                    color = if (detected > 0) Color.Red else Color(0xFF00C853)
                )
                Text(
                    text = "Rate: %.1f%%".format(percentage),
                    fontSize = 14.sp,
                    color = if (percentage > 30) Color.Red else Color(0xFF00C853)
                )
            }

            // ── Mini Chart ─────────────────────────────
            MiniAnomalyChart(recentAnomalies)

            // ── Detailed List ─────────────────────────
            recentAnomalies.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Err: %.5f".format(item.error),
                        fontSize = 14.sp,
                        color = if (item.anomaly) Color.Red else Color.Gray
                    )
                    Text(
                        text = if (item.anomaly) "⚠️ Anomaly" else "✓ Normal",
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.anomaly) Color.Red else Color(0xFF00C853)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniAnomalyChart(anomalies: List<Anomaly>) {
    val maxError = anomalies.maxOfOrNull { it.error } ?: 1.0
    val animatedHeights = remember(anomalies) {
        anomalies.map { it.error / maxError }
    }

    // 🔹 Create animated heights OUTSIDE Canvas
    val animatedValues = animatedHeights.map { value ->
        animateFloatAsState(
            targetValue = value.toFloat(),
            animationSpec = tween(durationMillis = 500)
        ).value
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(top = 4.dp)
    ) {
        val count = anomalies.size.coerceAtLeast(1)
        val barWidth = size.width / count

        anomalies.forEachIndexed { index, item ->
            val barHeight = animatedValues[index] * size.height
            val color = if (item.anomaly) Color.Red else Color(0xFF00C853)

            drawRect(
                color = color,
                topLeft = Offset(index * barWidth, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(
                    barWidth - 4.dp.toPx(),
                    barHeight
                )
            )
        }

        drawLine(
            color = Color.Gray.copy(alpha = 0.4f),
            start = Offset(0f, size.height - 2.dp.toPx()),
            end = Offset(size.width, size.height - 2.dp.toPx()),
            strokeWidth = 2f
        )
    }
}


@Composable
fun LiveIndicator() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            visible = !visible
            delay(500)
        }
    }
    if (visible) {
        Text("🟢 LIVE", color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
    } else {
        Text("   ", color = Color.Transparent)
    }
}
