package com.medtrack.app.ui.screens.trends

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medtrack.app.ui.components.TrendChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(viewModel: TrendsViewModel, profileId: Long) {
    LaunchedEffect(profileId) { viewModel.setProfileId(profileId) }
    val available by viewModel.availableMetrics.collectAsState()
    val selected by viewModel.selectedMetric.collectAsState()
    val state by viewModel.metricRecords.collectAsState()
    val records = state.filter { it.profileId == profileId && it.canonicalName == selected }
    val units = records.filter { it.numericValue?.isFinite() == true }.map { it.unit }.distinct()
    var chosenUnit by remember(selected) { mutableStateOf<String?>(null) }
    val unit = chosenUnit?.takeIf { it in units } ?: units.firstOrNull()
    val chartRecords = records.filter { it.numericValue?.isFinite() == true && it.unit == unit }
    LaunchedEffect(available) { if (available.isNotEmpty() && selected !in available) viewModel.selectMetric(available.first()) }
    Scaffold(topBar = { TopAppBar(title = { Text("Health over time") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (available.isEmpty()) item {
                Text("Your story starts with a result", style = MaterialTheme.typography.headlineSmall)
                Text("Save a report to see its results here. Add the same test over time to build a history.")
            } else {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        available.forEach { metric -> FilterChip(selected == metric, { viewModel.selectMetric(metric) }, label = { Text(metric) }) }
                    }
                    if (units.size > 1) {
                        Text("Different units are shown separately so values are not compared incorrectly.", style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            units.forEach { choice -> FilterChip(unit == choice, { chosenUnit = choice }, label = { Text(choice.ifBlank { "No unit" }) }) }
                        }
                    }
                }
                item {
                    Text(selected, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    // A range band is meaningful only when all plotted records share that printed range.
                    val ranges = chartRecords.map { it.refRangeLow to it.refRangeHigh }.distinct()
                    val range = ranges.singleOrNull()
                    TrendChart(chartRecords, range?.first, range?.second, unit.orEmpty())
                    Text("Points follow report order. Ranges are taken from saved reports; they are not a diagnosis.", style = MaterialTheme.typography.bodySmall)
                }
                item { Text("Recorded results", style = MaterialTheme.typography.titleLarge) }
                items(records.reversed(), key = { it.id }) { record ->
                    OutlinedCard {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(record.testDate, style = MaterialTheme.typography.labelLarge)
                            Text(record.value + " " + record.unit, style = MaterialTheme.typography.titleLarge)
                            Text("Reference: " + record.refRangeText.orEmpty().ifBlank { "Not recorded" })
                            if (record.isAbnormal) Text("Outside saved range", color = MaterialTheme.colorScheme.error)
                            if (record.numericValue == null) Text("Written result · not plotted", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
