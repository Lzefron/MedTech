package com.medtrack.app.ui.screens.report

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.local.entity.LabReportEntity
import com.medtrack.app.data.local.entity.TestRecordEntity
import com.medtrack.app.data.model.CanonicalTest
import com.medtrack.app.data.model.MedicalDictionary
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.domain.ocr.ParsedLabRow
import com.medtrack.app.domain.ocr.ReportOcrAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class OcrUiState {
    object Idle : OcrUiState()
    object Processing : OcrUiState()
    object Success : OcrUiState()
    data class Error(val message: String) : OcrUiState()
}

class ReportViewModel(
    private val labReportRepository: LabReportRepository
) : ViewModel() {

    private val _ocrState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val ocrState: StateFlow<OcrUiState> = _ocrState

    var reportTitle = MutableStateFlow("Comprehensive Lab Report")
    var reportDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    var labName = MutableStateFlow("")
    var notes = MutableStateFlow("")
    var selectedImageUri = MutableStateFlow<Uri?>(null)

    val extractedRows = mutableStateListOf<ParsedLabRow>()

    fun startOcrProcessing(context: Context, uri: Uri) {
        selectedImageUri.value = uri
        _ocrState.value = OcrUiState.Processing
        viewModelScope.launch {
            try {
                val analyzer = ReportOcrAnalyzer(context)
                val result = analyzer.analyzeUri(uri)
                if (result.detectedDate != null) {
                    reportDate.value = result.detectedDate
                }
                if (result.detectedLab != null) {
                    labName.value = result.detectedLab
                    reportTitle.value = "${result.detectedLab} Diagnostic Report"
                }
                extractedRows.clear()
                extractedRows.addAll(result.extractedRecords)
                _ocrState.value = OcrUiState.Success
            } catch (e: Exception) {
                _ocrState.value = OcrUiState.Error(e.localizedMessage ?: "OCR processing failed")
            }
        }
    }

    fun addManualRow(test: CanonicalTest, value: String) {
        val numVal = value.toDoubleOrNull()
        var isAbnormal = false
        if (numVal != null) {
            if (test.normalLow != null && numVal < test.normalLow) isAbnormal = true
            if (test.normalHigh != null && numVal > test.normalHigh) isAbnormal = true
        }

        val refText = if (test.normalLow != null && test.normalHigh != null) "${test.normalLow} - ${test.normalHigh}" else ""
        extractedRows.add(
            ParsedLabRow(
                testName = test.name,
                canonicalName = test.name,
                value = value,
                numericValue = numVal,
                unit = test.defaultUnit,
                refRangeLow = test.normalLow,
                refRangeHigh = test.normalHigh,
                refRangeText = refText,
                isAbnormal = isAbnormal,
                confidenceScore = 1.0f
            )
        )
    }

    fun updateRow(index: Int, updated: ParsedLabRow) {
        if (index in extractedRows.indices) {
            extractedRows[index] = updated
        }
    }

    fun removeRow(index: Int) {
        if (index in extractedRows.indices) {
            extractedRows.removeAt(index)
        }
    }

    fun saveReport(profileId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            val report = LabReportEntity(
                profileId = profileId,
                title = reportTitle.value.ifBlank { "Laboratory Diagnostic Report" },
                testDate = reportDate.value,
                labName = labName.value,
                imagePath = selectedImageUri.value?.toString(),
                notes = notes.value.ifBlank { null }
            )

            val records = extractedRows.map { row ->
                TestRecordEntity(
                    reportId = 0,
                    profileId = profileId,
                    testName = row.testName,
                    canonicalName = row.canonicalName,
                    value = row.value,
                    numericValue = row.numericValue,
                    unit = row.unit,
                    refRangeLow = row.refRangeLow,
                    refRangeHigh = row.refRangeHigh,
                    refRangeText = row.refRangeText,
                    isAbnormal = row.isAbnormal,
                    confidenceScore = row.confidenceScore,
                    testDate = reportDate.value
                )
            }

            labReportRepository.saveReportWithRecords(report, records)
            onComplete()
        }
    }
}
