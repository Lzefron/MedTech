package com.medtrack.app.ui.screens.report

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.medtrack.app.ui.components.DocumentPreview
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScanScreen(viewModel: ReportViewModel, profileId: Long, onNavigateBack: () -> Unit, onManualEntry: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.ocrState.collectAsState()
    val image by viewModel.selectedImageUri.collectAsState()
    val raw by viewModel.rawText.collectAsState()
    val reviewed by viewModel.reviewed.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val cloudBusy by viewModel.cloudBusy.collectAsState()
    val cloudSuggestion by viewModel.cloudSuggestion.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()
    var connectionDialog by remember { mutableStateOf(false) }
    var cloudConsent by remember { mutableStateOf(false) }
    var replacement by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var showText by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { if (image != null || viewModel.extractedRows.isNotEmpty()) replacement = uri else viewModel.startOcrProcessing(context, uri) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { if (image != null || viewModel.extractedRows.isNotEmpty()) replacement = Uri.parse(it) else viewModel.startOcrProcessing(context, Uri.parse(it)) }
    }
    val goBack = { if (!saving && !cloudBusy) { if (image != null) discard = true else onNavigateBack() } }
    BackHandler { goBack() }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Scan a report") }, navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.Default.ArrowBack, "Back") } }) },
        bottomBar = { SaveReportBar(viewModel, enabled = state !is OcrUiState.Processing && !cloudBusy) { viewModel.saveReport(profileId, context) {
            android.widget.Toast.makeText(context, "Report and photo saved on this device", android.widget.Toast.LENGTH_LONG).show()
            onNavigateBack()
        } } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Turn a photo into a clear record", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Take or choose a clear photo. The fields below fill automatically, and you can correct them or add missing tests. Reading starts on your phone; optional AI needs your approval.")
                Spacer(Modifier.height(8.dp))
                Text("Handwriting and unclear text may not be recognized. Confirm every result against the original; never use a guessed medicine or dose.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !saving && !cloudBusy && state !is OcrUiState.Processing, onClick = { (context as? com.medtrack.app.MainActivity)?.allowExternalReturn(); picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Choose photo")
                    }
                    OutlinedButton(enabled = !saving && !cloudBusy && state !is OcrUiState.Processing, onClick = {
                        try {
                            val file = File.createTempFile("report-", ".jpg", File(context.cacheDir, "reports").apply { mkdirs() })
                            cameraUri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file).toString()
                            (context as? com.medtrack.app.MainActivity)?.allowExternalReturn()
                            camera.launch(Uri.parse(cameraUri))
                        } catch (_: Exception) { cameraError = "Camera unavailable. Choose a photo instead." }
                    }) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Camera") }
                }
                cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(enabled = !cloudBusy, onClick = { connectionDialog = true }) { Text("AI connection") }
                if (image == null) TextButton(onClick = onManualEntry) { Text("Enter without a photo") }
            }
            image?.let { uri -> item { DocumentPreview(uri) } }
            if (state is OcrUiState.Processing) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Reading the photo…") }
            if (state is OcrUiState.Error) item { Text((state as OcrUiState.Error).message, color = MaterialTheme.colorScheme.error) }
            if (image != null && state !is OcrUiState.Processing) {
                item {
                    if (scanMessage.isNotBlank()) Text(scanMessage, style = MaterialTheme.typography.bodyMedium)
                    if (viewModel.extractedRows.isEmpty()) {
                        Text("No readable test rows found", style = MaterialTheme.typography.titleLarge)
                        Text("Try a clearer photo, or copy results manually. Handwritten prescriptions need confirmation from the doctor or pharmacist.")
                    } else {
                        Text(viewModel.extractedRows.size.toString() + " results to review", style = MaterialTheme.typography.titleLarge)
                        Text("OCR can miss rows or mix columns. Compare the full photo, and add any missing results manually.")
                    }
                    OutlinedButton(enabled = !saving && !cloudBusy, onClick = { adding = true; viewModel.error.value = null }) { Text("Add a result manually") }
                    OutlinedButton(enabled = !saving && !cloudBusy, onClick = {
                        if (com.medtrack.app.domain.ocr.VisionSettings(context).load() == null) connectionDialog = true else cloudConsent = true
                    }) { Text("Try AI for missing or unclear fields") }
                    if (cloudBusy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("AI is reading the photo. Your current fields are kept…") }
                    cloudSuggestion?.let { suggestion ->
                        OutlinedCard { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("AI found ${suggestion.extractedRecords.size} rows. Fill missing fields and add missing tests; existing entries stay unchanged. Conflicts will be shown for review.")
                            Text("Date: ${suggestion.detectedDate ?: "Not identified"} · Lab: ${suggestion.detectedLab.orEmpty()}")
                            Button(onClick = { viewModel.applyCloudReading() }) { Text("Fill missing fields with AI reading") }
                            TextButton(onClick = { viewModel.cloudSuggestion.value = null }) { Text("Keep current reading") }
                        } }
                    }
                    if (raw.isNotBlank()) {
                        TextButton(onClick = { showText = !showText }) { Text(if (showText) "Hide recognized text" else "Show recognized text") }
                        if (showText) Text(raw, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item { Text("Editable results · not saved yet", style = MaterialTheme.typography.titleLarge) }
                    itemsIndexed(viewModel.extractedRows) { index, row -> ExtractedRowCard(row, { viewModel.updateRow(index, it) }, { viewModel.removeRow(index) }) }
                item { ReportFields(viewModel) }
            }
        }
    }
    if (adding) AddScanResultDialog(viewModel) { adding = false }
    if (connectionDialog) VisionConnectionDialog { connectionDialog = false }
    if (cloudConsent) AlertDialog(onDismissRequest = { cloudConsent = false }, title = { Text("Send this photo for AI reading?") },
        text = { Text("This sends the entire selected report photo, including any patient details visible in it, to your configured gateway and Google Gemini. Only proceed with the patient's permission and a billing-enabled Gemini service. AI can misread handwriting and numbers. You will review the fields before saving. No other saved records are sent. Gateway: ${com.medtrack.app.domain.ocr.VisionSettings(context).load()?.endpoint.orEmpty()}") },
        confirmButton = { Button(onClick = { cloudConsent = false; viewModel.requestCloudReading(context) }) { Text("Send this photo") } },
        dismissButton = { TextButton(onClick = { cloudConsent = false }) { Text("Keep on device") } })
    replacement?.let { uri -> AlertDialog(onDismissRequest = { replacement = null }, title = { Text("Replace this scan?") },
        text = { Text("This replaces the current photo, extracted results, date and laboratory. Unsaved edits to those fields will be lost.") },
        confirmButton = { Button(onClick = { replacement = null; viewModel.startOcrProcessing(context, uri) }) { Text("Replace scan") } },
        dismissButton = { TextButton(onClick = { replacement = null }) { Text("Keep current scan") } }) }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Leave this scan?") },
        text = { Text("This scan has not been saved.") }, confirmButton = { TextButton(onClick = onNavigateBack) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep reviewing") } })
}
