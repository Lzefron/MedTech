package com.medtrack.app.ui.screens.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.medtrack.app.domain.validation.HealthInput
import com.medtrack.app.domain.validation.ReferenceCatalog

@Composable
fun PickOrTypeField(value: String, onChange: (String) -> Unit, label: String, options: List<String>,
    onPick: (String) -> Unit = onChange, showLabel: String = "Show all units") {
    var expanded by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Column {
        OutlinedTextField(value, { expanded = true; onChange(it) }, label = { Text(label) },
            singleLine = true, modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) expanded = true })
        OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide choices" else showLabel) }
        if (expanded && options.isNotEmpty()) {
            val ranked = ReferenceCatalog.rank(options, value)
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                    ranked.forEach { option ->
                        ListItem(headlineContent = { Text(option) }, modifier = Modifier.clickable { onPick(option); expanded = false; focus.clearFocus() })
                    }
                }
            }
        }
    }
}

@Composable
fun UnitAndRangeFields(test: String, unit: String, onUnit: (String) -> Unit,
    range: String, source: String?, onRange: (String, String?) -> Unit,
    initialGroup: String = "", onGroupChoice: (String) -> Unit = {}) {
    var group by rememberSaveable(test) { mutableStateOf(initialGroup) }
    var showGroups by rememberSaveable(test) { mutableStateOf(false) }
    var usingSuggested by rememberSaveable(test) { mutableStateOf(source != null) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val choices = ReferenceCatalog.units(test).ifEmpty {
        com.medtrack.app.data.model.MedicalDictionary.tests.map { it.defaultUnit }.distinct()
    }
    PickOrTypeField(unit, { next ->
        onUnit(next.take(40))
        // A typed or scanned range cannot silently travel into another unit.
        val suggestion = if (usingSuggested) ReferenceCatalog.suggest(test, next, group) else null
        onRange(suggestion?.range.orEmpty(), suggestion?.source)
    }, "Unit from report *", choices)
    Text("Check the unit against your report. Changing units does not convert your measured value.", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(range, { usingSuggested = false; onRange(it.take(80), null) }, label = { Text("Reference range (optional)") },
        modifier = Modifier.fillMaxWidth(), isError = HealthInput.range(range) == null,
        supportingText = { Text(if (source == null) "Copy your lab's range, use a general reference, or skip." else "General reference, not your lab's range. You can replace it.") })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { usingSuggested = false; onRange("", null); showGroups = false }) { Text("Skip range") }
        OutlinedButton(onClick = { showGroups = !showGroups }) { Text("General reference") }
    }
    if (showGroups) {
        Text("Choose the group at the time of the test. These references are for nonpregnant adults only. Children, pregnancy and treatment targets need the lab's range.", style = MaterialTheme.typography.bodySmall)
        Column {
            listOf("Adult", "Adult male", "Adult female").forEach { choice ->
                FilterChip(selected = group == choice, onClick = {
                    group = choice
                    usingSuggested = true
                    onGroupChoice(choice)
                    val suggestion = ReferenceCatalog.suggest(test, unit, choice)
                    onRange(suggestion?.range.orEmpty(), suggestion?.source)
                    if (suggestion != null) showGroups = false
                }, label = { Text(choice) })
            }
        }
        if (group.isNotBlank() && ReferenceCatalog.suggest(test, unit, group) == null) {
            Text("No verified reference for this test, unit and group. Choose a more specific group, copy the lab's range, or skip.", style = MaterialTheme.typography.bodySmall)
        }
    }
    source?.let {
        Text(it.substringBefore(" · https://"), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            (context as? com.medtrack.app.MainActivity)?.allowExternalReturn()
            runCatching { uriHandler.openUri(it.substringAfterLast(" · ")) }
        }) { Text("Read medical source") }
    }
}

@Composable
fun SkippableField(value: String, onChange: (String) -> Unit, label: String, maxLength: Int) {
    var skipped by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            if (!skipped) OutlinedTextField(value, { onChange(it.take(maxLength)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
            else Text(label.substringBefore(" (optional)") + " skipped")
            OutlinedButton(onClick = { if (!skipped) onChange(""); skipped = !skipped }) {
                Text(if (skipped) "Add this detail" else "Skip " + label.substringBefore(" (optional)").lowercase())
            }
        }
    }
}
