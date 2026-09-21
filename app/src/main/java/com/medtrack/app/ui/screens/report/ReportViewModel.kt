package com.medtrack.app.ui.screens.report

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.local.entity.*
import com.medtrack.app.data.model.CanonicalTest
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.domain.ocr.*
import com.medtrack.app.domain.validation.HealthInput
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class OcrUiState {
    object Idle : OcrUiState()
    object Processing : OcrUiState()
    object Success : OcrUiState()
    data class Error(val message: String) : OcrUiState()
}

class ReportViewModel(private val labReportRepository: LabReportRepository) : ViewModel() {
    val ocrState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val reportTitle = MutableStateFlow("")
    val reportDate = MutableStateFlow("")
    val labName = MutableStateFlow("")
    val notes = MutableStateFlow("")
    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val rawText = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    val reviewed = MutableStateFlow(false)
    val extractedRows = mutableStateListOf<ParsedLabRow>()
    private var ocrJob: Job? = null

    fun startOcrProcessing(context: Context, uri: Uri) {
        if (saving.value) return
        ocrJob?.cancel()
        selectedImageUri.value = uri
        extractedRows.clear()
        reportDate.value = ""
        labName.value = ""
        rawText.value = ""
        error.value = null
        reviewed.value = false
        ocrState.value = OcrUiState.Processing
        ocrJob = viewModelScope.launch {
            val analyzer = ReportOcrAnalyzer(context.applicationContext)
            try {
                val result = analyzer.analyzeUri(uri)
                reportDate.value = result.detectedDate ?: ""
                labName.value = result.detectedLab ?: ""
                rawText.value = result.rawText
                extractedRows.addAll(result.extractedRecords)
                ocrState.value = OcrUiState.Success
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { ocrState.value = OcrUiState.Error("Could not read this photo. Try a clear, well-lit JPEG or PNG, or enter results manually.")
            } finally { analyzer.close() }
        }
    }

    fun addManualRow(test: CanonicalTest, value: String, unit: String = test.defaultUnit, reference: String = ""): Boolean {
        val number = HealthInput.number(value)
        if (number == null) { error.value = "Enter a valid number, such as 14.2. Letters, negative values and infinity are not valid."; return false }
        if (unit.isBlank()) { error.value = "Enter the unit printed on the report."; return false }
        val range = HealthInput.range(reference)
        if (range == null) { error.value = "Use a reference range such as 4–10 or < 200, or leave it blank."; return false }
        reviewed.value = false
        extractedRows.add(ParsedLabRow(test.name, test.name, value.trim(), number, unit.trim(), range.low, range.high,
            reference.trim(), range.outside(number), 1f))
        error.value = null
        return true
    }

    fun addCustomRow(name: String, value: String): Boolean {
        if (name.isBlank() || value.isBlank()) { error.value = "Enter both the test name and its written result."; return false }
        reviewed.value = false
        extractedRows.add(ParsedLabRow(name.trim(), name.trim(), value.trim(), null, "", null, null, "", false, 1f, isNumeric = false))
        error.value = null
        return true
    }

    fun updateRow(index: Int, updated: ParsedLabRow) {
        if (index !in extractedRows.indices) return
        reviewed.value = false
        val range = HealthInput.range(updated.refRangeText)
        val number = if (updated.isNumeric) HealthInput.number(updated.value) else null
        extractedRows[index] = updated.copy(numericValue = number, refRangeLow = range?.low, refRangeHigh = range?.high,
            isAbnormal = number != null && range?.outside(number) == true)
    }
    fun removeRow(index: Int) { if (index in extractedRows.indices) { extractedRows.removeAt(index); reviewed.value = false } }

    fun saveReport(profileId: Long, context: Context? = null, onComplete: () -> Unit) {
        if (saving.value || ocrState.value is OcrUiState.Processing) return
        error.value = when {
            profileId <= 0 -> "Select a profile first."
            HealthInput.dateError(reportDate.value) != null -> HealthInput.dateError(reportDate.value)
            extractedRows.isEmpty() -> "Add at least one result before saving."
            selectedImageUri.value != null && !reviewed.value -> "Compare the results with the original photo and confirm your review."
            extractedRows.any { it.value.isBlank() || it.canonicalName.isBlank() } -> "Every result needs a name and value."
            extractedRows.any { it.isNumeric && (it.unit.isBlank() || HealthInput.number(it.value) == null) } -> "Correct invalid numeric results before saving."
            extractedRows.any { HealthInput.range(it.refRangeText) == null } -> "Correct invalid reference ranges before saving."
            else -> null
        }
        if (error.value != null) return
        saving.value = true
        // Snapshot before suspension, so edits cannot change the report mid-save.
        val date = reportDate.value.trim()
        val title = reportTitle.value.trim().ifBlank { "Lab report · " + date }
        val lab = labName.value.trim()
        val reportNotes = notes.value.trim().ifBlank { null }
        val rows = extractedRows.toList()
        val photo = selectedImageUri.value
        viewModelScope.launch {
            var stored: String? = null
            var committed = false
            try {
                withContext(NonCancellable) {
                if (photo != null) {
                    require(context != null) { "Please reopen the photo before saving." }
                    stored = withContext(Dispatchers.IO) { DocumentImages.persist(context.applicationContext, photo) }
                }
                val report = LabReportEntity(profileId = profileId, title = title, testDate = date, labName = lab, imagePath = stored, notes = reportNotes)
                val records = rows.map { row ->
                    val range = HealthInput.range(row.refRangeText)!!
                    val number = if (row.isNumeric) HealthInput.number(row.value) else null
                    TestRecordEntity(reportId = 0, profileId = profileId, testName = row.testName, canonicalName = row.canonicalName,
                        value = row.value.trim(), numericValue = number, unit = row.unit.trim(), refRangeLow = range.low,
                        refRangeHigh = range.high, refRangeText = row.refRangeText.trim(), isAbnormal = number != null && range.outside(number),
                        confidenceScore = row.confidenceScore, testDate = date)
                }
                labReportRepository.saveReportWithRecords(report, records)
                committed = true
                }
                if (viewModelScope.isActive) onComplete()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { error.value = "The report was not saved. Check free storage and try again; your draft is still here."
            } finally {
                if (!committed && context != null) withContext(NonCancellable + Dispatchers.IO) { DocumentImages.deletePrivate(context, stored) }
                saving.value = false
            }
        }
    }
}
