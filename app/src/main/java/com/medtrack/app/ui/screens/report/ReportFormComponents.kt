package com.medtrack.app.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medtrack.app.domain.ocr.ParsedLabRow
import com.medtrack.app.domain.validation.HealthInput
import java.time.LocalDate

@Composable
fun ReportFields(viewModel: ReportViewModel) {
    val title by viewModel.reportTitle.collectAsState()
    val date by viewModel.reportDate.collectAsState()
    val lab by viewModel.labName.collectAsState()
    val notes by viewModel.notes.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Report details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(date, { viewModel.reportDate.value = it.take(10); viewModel.reviewed.value = false },
            label = { Text("Test date * · YYYY-MM-DD") }, singleLine = true,
            isError = date.isNotBlank() && HealthInput.dateError(date) != null,
            supportingText = { Text(if (date.isBlank()) "Use the date printed on the report." else HealthInput.dateError(date) ?: "Confirm this is the test date, not the birth date.") },
            modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { viewModel.reportDate.value = LocalDate.now().toString(); viewModel.reviewed.value = false }) { Text("Use today's date") }
        OutlinedTextField(title, { viewModel.reportTitle.value = it.take(120) }, label = { Text("Title (optional)") }, placeholder = { Text("e.g. Annual checkup") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(lab, { viewModel.labName.value = it.take(120) }, label = { Text("Laboratory (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { viewModel.notes.value = it.take(2000) }, label = { Text("Notes for your doctor (optional)") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ExtractedRowCard(row: ParsedLabRow, onUpdate: (ParsedLabRow) -> Unit, onDelete: () -> Unit) {
    val valueError = row.isNumeric && HealthInput.number(row.value) == null
    val rangeError = HealthInput.range(row.refRangeText) == null
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.canonicalName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (row.isNumeric) "Numeric result" else "Written result · not plotted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Close, "Remove " + row.canonicalName) }
            }
            OutlinedTextField(row.value, { onUpdate(row.copy(value = it.take(160))) },
                label = { Text("Result *") }, modifier = Modifier.fillMaxWidth(), isError = valueError,
                supportingText = { if (valueError) Text("Enter a finite, non-negative number.") },
                keyboardOptions = KeyboardOptions(keyboardType = if (row.isNumeric) KeyboardType.Decimal else KeyboardType.Text))
            if (row.isNumeric) {
                OutlinedTextField(row.unit, { onUpdate(row.copy(unit = it.take(40))) }, label = { Text("Unit from report *") }, modifier = Modifier.fillMaxWidth(), isError = row.unit.isBlank())
                OutlinedTextField(row.refRangeText, { onUpdate(row.copy(refRangeText = it.take(80))) },
                    label = { Text("Printed reference range (optional)") }, modifier = Modifier.fillMaxWidth(), isError = rangeError,
                    supportingText = { Text(if (rangeError) "Use 4–10, < 200, or leave blank." else "Blank means no range assessment. Ranges vary by lab and patient.") })
            }
            row.warningNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun SaveReportBar(viewModel: ReportViewModel, enabled: Boolean = true, onSave: () -> Unit) {
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    Surface(shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(onClick = onSave, enabled = enabled && !saving && viewModel.extractedRows.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                Text(if (saving) "Saving on this device…" else "Save " + viewModel.extractedRows.size + " results")
            }
        }
    }
}


@Composable
fun AddScanResultDialog(viewModel: ReportViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var range by remember { mutableStateOf("") }
    var written by remember { mutableStateOf(false) }
    val error by viewModel.error.collectAsState()
    AlertDialog(modifier = Modifier.imePadding(), onDismissRequest = onDismiss, title = { Text("Add a missing result") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(100) }, label = { Text("Test name *") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(written, { written = it; value = "" })
                    Text("Written result, such as Negative or < 0.5")
                }
                OutlinedTextField(value, { value = it.take(160) }, label = { Text("Result *") },
                    keyboardOptions = KeyboardOptions(keyboardType = if (written) KeyboardType.Text else KeyboardType.Decimal))
                if (!written) {
                    OutlinedTextField(unit, { unit = it.take(40) }, label = { Text("Unit from report *") })
                    OutlinedTextField(range, { range = it.take(80) }, label = { Text("Reference range (optional)") })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && value.isNotBlank(), onClick = {
                val test = com.medtrack.app.data.model.MedicalDictionary.tests.firstOrNull { it.name.equals(name.trim(), true) || it.aliases.any { alias -> alias.equals(name.trim(), true) } }
                    ?: com.medtrack.app.data.model.CanonicalTest(name.trim(), "Custom test", "", null, null)
                val added = if (written) viewModel.addCustomRow(name, value) else viewModel.addManualRow(test, value, unit, range)
                if (added) onDismiss()
            }) { Text("Add result") }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

