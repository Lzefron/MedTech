package com.medtrack.app.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medtrack.app.data.model.CanonicalTest
import com.medtrack.app.data.model.MedicalDictionary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    viewModel: ReportViewModel,
    profileId: Long,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTest by remember { mutableStateOf<CanonicalTest?>(null) }
    var testValue by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }

    val reportTitle by viewModel.reportTitle.collectAsState()
    val reportDate by viewModel.reportDate.collectAsState()
    val labName by viewModel.labName.collectAsState()

    val filteredTests = remember(searchQuery) {
        MedicalDictionary.search(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Health Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (viewModel.extractedRows.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = {
                            viewModel.saveReport(profileId) {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save ${viewModel.extractedRows.size} Records to Vault", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Report Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = reportTitle,
                            onValueChange = { viewModel.reportTitle.value = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = reportDate,
                                onValueChange = { viewModel.reportDate.value = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = labName,
                                onValueChange = { viewModel.labName.value = it },
                                label = { Text("Laboratory") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Test Search & Autosuggest Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Add Lab Test (with Autosuggest)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Type test name (e.g., Hemoglobin, Glucose, Creatinine)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                showSuggestions = it.isNotBlank()
                            },
                            label = { Text("Search 100+ Canonical Tests") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Suggestions Dropdown List
                        if (showSuggestions && filteredTests.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    filteredTests.take(5).forEach { test ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedTest = test
                                                    searchQuery = test.name
                                                    showSuggestions = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(test.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${test.category} (${test.defaultUnit})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }

                        // When test is selected, show value entry
                        if (selectedTest != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "Selected: ${selectedTest!!.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Standard Unit: ${selectedTest!!.defaultUnit} | Normal Range: ${selectedTest!!.normalLow ?: 0.0} - ${selectedTest!!.normalHigh ?: 0.0}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = testValue,
                                            onValueChange = { testValue = it },
                                            label = { Text("Measured Value") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        Button(
                                            onClick = {
                                                if (testValue.isNotBlank()) {
                                                    viewModel.addManualRow(selectedTest!!, testValue)
                                                    testValue = ""
                                                    searchQuery = ""
                                                    selectedTest = null
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Current Batch Items
            if (viewModel.extractedRows.isNotEmpty()) {
                item {
                    Text(
                        "Batch Test Items (${viewModel.extractedRows.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(viewModel.extractedRows) { index, row ->
                    ExtractedRowCard(
                        row = row,
                        onUpdate = { updated -> viewModel.updateRow(index, updated) },
                        onDelete = { viewModel.removeRow(index) }
                    )
                }
            }
        }
    }
}
