package com.medtrack.app.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medtrack.app.data.local.entity.LabReportEntity
import com.medtrack.app.data.local.entity.TestRecordEntity
import com.medtrack.app.data.repository.LabReportRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    reportId: Long,
    repository: LabReportRepository,
    onNavigateBack: () -> Unit
) {
    val report by repository.getReportById(reportId).collectAsState(initial = null)
    val records by repository.getRecordsForReport(reportId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(report?.title ?: "Report Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            report?.let { rep ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                rep.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Date: ${rep.testDate} • Lab: ${rep.labName.ifBlank { "Diagnostic Facility" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            if (!rep.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Notes: ${rep.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Laboratory Diagnostics (${records.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(records) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (rec.isAbnormal) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.canonicalName, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Ref: ${rec.refRangeText ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${rec.value} ${rec.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (rec.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (rec.isAbnormal) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("ABNORMAL", color = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
