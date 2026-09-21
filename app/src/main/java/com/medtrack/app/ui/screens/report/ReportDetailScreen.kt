package com.medtrack.app.ui.screens.report

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.ui.components.DocumentPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(reportId: Long, repository: LabReportRepository, profileId: Long, onNavigateBack: () -> Unit) {
    val reportFlow = remember(reportId) { repository.getReportById(reportId) }
    val recordsFlow = remember(reportId) { repository.getRecordsForReport(reportId) }
    val report by reportFlow.collectAsState(initial = null)
    val stored by recordsFlow.collectAsState(initial = emptyList())
    val records = stored.filter { it.profileId == profileId }
    var onlyFlagged by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Report details") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val rep = report?.takeIf { it.profileId == profileId }
            if (rep == null) {
                item { Text("Loading report… If it was deleted, return to your records.") }
            } else {
                item {
                    Text(rep.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(rep.testDate + if (rep.labName.isNotBlank()) " · " + rep.labName else "")
                    rep.notes?.let { Text(it, Modifier.padding(top = 8.dp)) }
                    Spacer(Modifier.height(12.dp))
                    Text(records.size.toString() + " results · " + records.count { it.isAbnormal } + " outside saved ranges", style = MaterialTheme.typography.titleSmall)
                    Text("Ranges depend on the laboratory and patient. Review results with your clinician.", style = MaterialTheme.typography.bodySmall)
                }
                rep.imagePath?.let { path -> item { DocumentPreview(Uri.parse(path)) } }
                item { FilterChip(selected = onlyFlagged, onClick = { onlyFlagged = !onlyFlagged }, label = { Text("Outside range only") }) }
                val visible = if (onlyFlagged) records.filter { it.isAbnormal } else records
                if (visible.isEmpty()) item { Text(if (onlyFlagged) "No results outside saved ranges." else "No results in this report.") }
                items(visible, key = { it.id }) { record ->
                    val assessed = record.numericValue != null && (record.refRangeLow != null || record.refRangeHigh != null)
                    val status = when {
                        !assessed -> "No range assessment"
                        record.isAbnormal -> "Outside saved range"
                        else -> "Within saved range"
                    }
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(record.canonicalName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(record.value + " " + record.unit, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(status, color = if (record.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Reference: " + record.refRangeText?.takeIf { it.isNotBlank() }.orEmpty().ifBlank { "Not recorded" }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
