package com.medtrack.app.ui.screens.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medtrack.app.data.model.*
import com.medtrack.app.domain.validation.HealthInput
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(viewModel: ReportViewModel, profileId: Long, onNavigateBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedName?.let { name -> MedicalDictionary.tests.find { it.name == name }
        ?: CanonicalTest(name, "Custom test", "", null, null) }
    var value by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var referenceSource by rememberSaveable { mutableStateOf<String?>(null) }
    var referenceGroup by rememberSaveable { mutableStateOf("") }
    var written by rememberSaveable { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val saving by viewModel.saving.collectAsState()
    val goBack = { if (!saving) { if (viewModel.extractedRows.isNotEmpty()) discard = true else onNavigateBack() } }
    BackHandler { goBack() }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Add results") }, navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.Default.ArrowBack, "Back") } }) },
        bottomBar = { SaveReportBar(viewModel) { viewModel.saveReport(profileId, onComplete = onNavigateBack) } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                HorizontalDivider()
                Text("Add a test", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !written, onClick = { written = false; value = ""; selectedName = null }, label = { Text("Number") })
                    FilterChip(selected = written, onClick = { written = true; value = ""; selectedName = null }, label = { Text("Written result") })
                }
                PickOrTypeField(query, { query = it.take(100); selectedName = null; reference = ""; referenceSource = null },
                    if (written) "Test name *" else "Search test name *",
                    (MedicalDictionary.search(query) + MedicalDictionary.tests).distinct().map { it.name },
                    onPick = { name ->
                        selectedName = name; query = name; unit = MedicalDictionary.tests.first { it.name == name }.defaultUnit
                        val suggestion = com.medtrack.app.domain.validation.ReferenceCatalog.suggest(name, unit, referenceGroup)
                        reference = suggestion?.range.orEmpty(); referenceSource = suggestion?.source
                    },
                    showLabel = "Show all tests")
                if (!written && selected == null && query.isNotBlank()) {
                    OutlinedButton(onClick = { selectedName = query.trim(); unit = ""; reference = ""; referenceSource = null }) { Text("Use this name as a custom test") }
                }
                if (written || selected != null) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value, { value = it.take(160) }, label = { Text(if (written) "Result * · e.g. Negative" else "Measured value *") },
                        modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = if (written) KeyboardType.Text else KeyboardType.Decimal),
                        isError = !written && value.isNotBlank() && HealthInput.number(value) == null,
                        supportingText = { if (!written && value.isNotBlank() && HealthInput.number(value) == null) Text("Enter a number, such as 14.2 or 250,000.") })
                    if (!written) {
                        UnitAndRangeFields(selectedName.orEmpty(), unit, { unit = it }, reference, referenceSource,
                            { text, source -> reference = text; referenceSource = source }, referenceGroup, { referenceGroup = it })
                    } else Text("Written results are saved exactly as entered and are not interpreted or charted.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        val added = if (written) viewModel.addCustomRow(query, value) else selected?.let { viewModel.addManualRow(it, value, unit, reference, referenceSource) } == true
                        if (added) { value = ""; query = ""; selectedName = null; reference = ""; referenceSource = null; unit = "" }
                    }, enabled = !saving, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Add result") }
                }
            }
            item { ReportFields(viewModel) }
            if (viewModel.extractedRows.isNotEmpty()) item { Text("Review your results", style = MaterialTheme.typography.titleLarge) }
            itemsIndexed(viewModel.extractedRows) { index, row ->
                ExtractedRowCard(row, { viewModel.updateRow(index, it) }, { viewModel.removeRow(index) })
            }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this draft?") },
        text = { Text("These results have not been saved.") }, confirmButton = { TextButton(onClick = onNavigateBack) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
}
