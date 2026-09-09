package com.medtrack.app.ui.screens.report

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medtrack.app.domain.ocr.ParsedLabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScanScreen(
    viewModel: ReportViewModel,
    profileId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val ocrState by viewModel.ocrState.collectAsState()
    val reportTitle by viewModel.reportTitle.collectAsState()
    val reportDate by viewModel.reportDate.collectAsState()
    val labName by viewModel.labName.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()

    // Modern Photo Picker (Play Store compliant, zero storage permissions needed)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.startOcrProcessing(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Lab Report (OCR)") },
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
            // Image Picker Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Google ML Kit On-Device Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "100% offline & private. Sensitive medical data stays on your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedImageUri != null) "Reselect Document Photo" else "Select Report Photo")
                        }
                    }
                }
            }

            // OCR Status & Progress
            when (ocrState) {
                is OcrUiState.Processing -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Running On-Device OCR...", fontWeight = FontWeight.Bold)
                                    Text("Extracting clinical metrics, units & reference ranges", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                is OcrUiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                "OCR Error: ${(ocrState as OcrUiState.Error).message}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                else -> {}
            }

            // Report Metadata Form
            if (viewModel.extractedRows.isNotEmpty() || ocrState is OcrUiState.Success) {
                item {
                    Text(
                        "Report Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = reportTitle,
                        onValueChange = { viewModel.reportTitle.value = it },
                        label = { Text("Report Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = reportDate,
                            onValueChange = { viewModel.reportDate.value = it },
                            label = { Text("Test Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = labName,
                            onValueChange = { viewModel.labName.value = it },
                            label = { Text("Diagnostic Lab") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Extracted Tests (${viewModel.extractedRows.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Verify & Edit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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

@Composable
fun ExtractedRowCard(
    row: ParsedLabRow,
    onUpdate: (ParsedLabRow) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (row.isAbnormal) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        row.canonicalName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (row.testName.lowercase() != row.canonicalName.lowercase()) {
                        Text(
                            "Matched from: \"${row.testName}\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (row.confidenceScore >= 0.9f) {
                        Badge(containerColor = Color(0xFF2E7D32)) {
                            Text("HIGH CONF", color = Color.White, modifier = Modifier.padding(2.dp), fontSize = 9.sp)
                        }
                    } else {
                        Badge(containerColor = Color(0xFFF57C00)) {
                            Text("REVIEW", color = Color.White, modifier = Modifier.padding(2.dp), fontSize = 9.sp)
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.value,
                    onValueChange = { newVal ->
                        val nVal = newVal.toDoubleOrNull()
                        var ab = false
                        if (nVal != null) {
                            if (row.refRangeLow != null && nVal < row.refRangeLow) ab = true
                            if (row.refRangeHigh != null && nVal > row.refRangeHigh) ab = true
                        }
                        onUpdate(row.copy(value = newVal, numericValue = nVal, isAbnormal = ab))
                    },
                    label = { Text("Value") },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = row.unit,
                    onValueChange = { onUpdate(row.copy(unit = it)) },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = row.refRangeText,
                    onValueChange = { onUpdate(row.copy(refRangeText = it)) },
                    label = { Text("Ref Range") },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            if (row.warningNote != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "⚠️ ${row.warningNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
