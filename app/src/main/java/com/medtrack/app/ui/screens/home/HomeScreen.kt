package com.medtrack.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medtrack.app.data.local.entity.LabReportEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel, onNavigateToScan: () -> Unit, onNavigateToManual: () -> Unit,
    onNavigateToReportDetail: (Long) -> Unit, onNavigateToLauncher: () -> Unit, onNavigateToEmergency: () -> Unit
) {
    val selected by viewModel.selectedProfileId.collectAsState()
    val current by viewModel.currentProfile.collectAsState()
    val reportsState by viewModel.recentReports.collectAsState()
    val flagsState by viewModel.recentAbnormalRecords.collectAsState()
    val medsState by viewModel.activeMedicines.collectAsState()
    val error by viewModel.error.collectAsState()
    val profile = current?.takeIf { it.id == selected }
    val reports = reportsState.filter { it.profileId == selected }
    val flags = flagsState.filter { it.profileId == selected }
    val medicines = medsState.filter { it.profileId == selected }
    var query by rememberSaveable(selected) { mutableStateOf("") }
    var showFlags by rememberSaveable(selected) { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<LabReportEntity?>(null) }
    val filtered = reports.filter { (it.title + " " + it.labName + " " + it.testDate).contains(query.trim(), ignoreCase = true) }

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text(profile?.name ?: "Health records", fontWeight = FontWeight.Bold)
                Text("Personal health record", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, navigationIcon = {
            IconButton(onClick = onNavigateToLauncher) { Icon(Icons.Default.PeopleAlt, "Choose another profile") }
        }, actions = {
            IconButton(onClick = onNavigateToEmergency) { Icon(Icons.Default.HealthAndSafety, "Emergency card") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("A clearer picture of your health", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Keep today's results ready for your next appointment.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onNavigateToScan, modifier = Modifier.weight(1f)) { Icon(Icons.Default.DocumentScanner, null); Spacer(Modifier.width(6.dp)); Text("Scan report") }
                            OutlinedButton(onClick = onNavigateToManual, modifier = Modifier.weight(1f)) { Text("Type results") }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile(reports.size.toString(), "Saved reports", Modifier.weight(1f))
                    SummaryTile(medicines.size.toString(), "Active medicines", Modifier.weight(1f))
                }
            }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (flags.isNotEmpty()) item {
                OutlinedCard(onClick = { showFlags = !showFlags }, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Text("  Results to discuss", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Icon(if (showFlags) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }
                        Text("Some recorded values fall outside their saved ranges. This is not a diagnosis.", style = MaterialTheme.typography.bodySmall)
                        if (showFlags) flags.forEach { row ->
                            TextButton(onClick = { onNavigateToReportDetail(row.reportId) }) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(row.canonicalName + ": " + row.value + " " + row.unit, fontWeight = FontWeight.SemiBold)
                                    Text(row.testDate + " · Range: " + row.refRangeText.orEmpty(), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Your reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (reports.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(query, { query = it }, label = { Text("Search title, lab or date") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
                }
            }
            if (reports.isEmpty()) item {
                OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Text("A fresh start", style = MaterialTheme.typography.titleLarge)
                        Text("Add your first report when you're ready. Scanned and manually entered results appear here, newest first.")
                        TextButton(onClick = onNavigateToManual) { Text("Add a result") }
                    }
                }
            } else if (filtered.isEmpty()) item { Text("No reports match this search. Try a different title, lab or date.") }
            filtered.groupBy { it.testDate.take(7) }.forEach { (month, group) ->
                item(key = "month-" + month) { Text(month, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(group, key = { it.id }) { report ->
                    Card(onClick = { onNavigateToReportDetail(report.id) }, shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(report.title, fontWeight = FontWeight.SemiBold)
                                Text(report.testDate, style = MaterialTheme.typography.bodyMedium)
                                if (report.labName.isNotBlank()) Text(report.labName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (report.imagePath != null) "Original photo saved" else "Manually entered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleting = report }) { Icon(Icons.Default.DeleteOutline, "Delete " + report.title) }
                        }
                    }
                }
            }
            item { Text("Saved on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    deleting?.let { report ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this report?") }, text = { Text("The results and saved photo will be permanently removed.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteReport(report); deleting = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep report") } })
    }
}

@Composable
private fun SummaryTile(value: String, label: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
