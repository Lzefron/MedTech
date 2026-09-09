package com.medtrack.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medtrack.app.data.local.entity.TestRecordEntity
import kotlin.math.max
import kotlin.math.min

@Composable
fun TrendChart(
    records: List<TestRecordEntity>,
    refLow: Double?,
    refHigh: Double?,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No longitudinal test data available for this metric yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Sort by testDate ascending
    val sorted = remember(records) {
        records.filter { it.numericValue != null }.sortedBy { it.testDate }
    }

    if (sorted.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No numeric data points to chart.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Keyed to the data: a stale index from a previous metric (fewer points)
    // must not survive into sorted[activeIndex], which would crash.
    var selectedPointIndex by remember(sorted) { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    // Determine value bounds
    val dataMin = sorted.minOf { it.numericValue!! }
    val dataMax = sorted.maxOf { it.numericValue!! }

    var yMin = dataMin
    var yMax = dataMax

    if (refLow != null) yMin = min(yMin, refLow)
    if (refHigh != null) yMax = max(yMax, refHigh)

    // Add 10% padding
    val span = if (yMax == yMin) 1.0 else (yMax - yMin)
    val plotMin = yMin - (span * 0.15)
    val plotMax = yMax + (span * 0.15)
    val plotSpan = plotMax - plotMin

    Column(modifier = modifier.fillMaxWidth()) {
        // Stats Summary Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatPill(title = "Latest", value = "${sorted.last().value} $unit", isAbnormal = sorted.last().isAbnormal)
            StatPill(title = "Lowest", value = "${dataMin} $unit", isAbnormal = false)
            StatPill(title = "Highest", value = "${dataMax} $unit", isAbnormal = false)
        }

        // Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(sorted) {
                        detectTapGestures { tapOffset ->
                            val width = size.width
                            val spacing = if (sorted.size > 1) width / (sorted.size - 1) else width / 2
                            var closestIdx = 0
                            var minDist = Float.MAX_VALUE
                            sorted.indices.forEach { i ->
                                val ptX = if (sorted.size == 1) width / 2 else i * spacing
                                val dist = kotlin.math.abs(tapOffset.x - ptX)
                                if (dist < minDist) {
                                    minDist = dist
                                    closestIdx = i
                                }
                            }
                            selectedPointIndex = closestIdx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw Shaded Reference Range Band
                if (refLow != null && refHigh != null) {
                    val refTopY = (h - ((refHigh - plotMin) / plotSpan * h)).toFloat().coerceIn(0f, h)
                    val refBottomY = (h - ((refLow - plotMin) / plotSpan * h)).toFloat().coerceIn(0f, h)
                    val bandHeight = refBottomY - refTopY

                    drawRect(
                        color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                        topLeft = Offset(0f, refTopY),
                        size = Size(w, bandHeight)
                    )

                    // Draw reference threshold lines
                    drawLine(
                        color = Color(0xFF4CAF50).copy(alpha = 0.4f),
                        start = Offset(0f, refTopY),
                        end = Offset(w, refTopY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF4CAF50).copy(alpha = 0.4f),
                        start = Offset(0f, refBottomY),
                        end = Offset(w, refBottomY),
                        strokeWidth = 2f
                    )
                }

                val spacing = if (sorted.size > 1) w / (sorted.size - 1) else w / 2
                val points = sorted.mapIndexed { index, record ->
                    val x = if (sorted.size == 1) w / 2 else index * spacing
                    val y = (h - ((record.numericValue!! - plotMin) / plotSpan * h)).toFloat().coerceIn(0f, h)
                    Offset(x, y)
                }

                // Draw Connecting Line Path
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cX = (prev.x + curr.x) / 2
                            cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF1E88E5),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                // Draw Points
                points.forEachIndexed { i, pt ->
                    val isAbnormal = sorted[i].isAbnormal
                    val ptColor = if (isAbnormal) Color(0xFFE53935) else Color(0xFF1E88E5)
                    val isSelected = selectedPointIndex == i

                    if (isSelected) {
                        drawCircle(
                            color = ptColor.copy(alpha = 0.25f),
                            radius = 16f,
                            center = pt
                        )
                    }

                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = pt
                    )
                    drawCircle(
                        color = ptColor,
                        radius = 6f,
                        center = pt
                    )
                }
            }
        }

        // Selected point detail tooltip
        val activeIndex = (selectedPointIndex ?: (sorted.size - 1)).coerceIn(0, sorted.size - 1)
        val activeRecord = sorted[activeIndex]

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (activeRecord.isAbnormal) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Date: ${activeRecord.testDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${activeRecord.value} $unit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (activeRecord.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (activeRecord.isAbnormal) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("ABNORMAL", color = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(4.dp))
                    }
                } else {
                    Badge(containerColor = Color(0xFF2E7D32)) {
                        Text("IN RANGE", color = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(title: String, value: String, isAbnormal: Boolean) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
