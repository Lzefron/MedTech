package com.medtrack.app.ui.screens.trends

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medtrack.app.ui.components.TrendChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel,
    profileId: Long
) {
    LaunchedEffect(profileId) {
        viewModel.setProfileId(profileId)
    }

    val availableMetrics by viewModel.availableMetrics.collectAsState()
    val selectedMetric by viewModel.selectedMetric.collectAsState()
    val metricRecords by viewModel.metricRecords.collectAsState()
    val canonicalMeta by viewModel.canonicalMetadata.collectAsState()

    // Default to first available metric if current selected is not in list
    LaunchedEffect(availableMetrics) {
        if (availableMetrics.isNotEmpty() && !availableMetrics.contains(selectedMetric)) {
            viewModel.selectMetric(availableMetrics.first())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Health Trends & Charts", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metric Chips Selector
            item {
                Text(
                    "Select Biomarker",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableMetrics.isEmpty()) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text("No metrics logged yet") }
                        )
                    } else {
                        availableMetrics.forEach { metric ->
                            FilterChip(
                                selected = metric == selectedMetric,
                                onClick = { viewModel.selectMetric(metric) },
                                label = { Text(metric) },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Trend Chart Canvas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                selectedMetric,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                canonicalMeta?.defaultUnit ?: metricRecords.firstOrNull()?.unit ?: "",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        TrendChart(
                            records = metricRecords,
                            refLow = canonicalMeta?.normalLow ?: metricRecords.firstOrNull()?.refRangeLow,
                            refHigh = canonicalMeta?.normalHigh ?: metricRecords.firstOrNull()?.refRangeHigh,
                            unit = canonicalMeta?.defaultUnit ?: metricRecords.firstOrNull()?.unit ?: ""
                        )
                    }
                }
            }

            // Clinical Reference Guide Card
            if (canonicalMeta != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Clinical Context: ${canonicalMeta!!.category}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    canonicalMeta!!.description.ifBlank { "Standard reference ranges may vary slightly between diagnostic laboratories." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Standard Reference Range: ${canonicalMeta!!.normalLow} - ${canonicalMeta!!.normalHigh} ${canonicalMeta!!.defaultUnit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Historical Values Table
            item {
                Text(
                    "Historical Progression (${metricRecords.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(metricRecords.reversed()) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (record.isAbnormal) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(record.testDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Ref Range: ${record.refRangeText ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${record.value} ${record.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (record.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (record.isAbnormal) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("ABNORMAL", color = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
