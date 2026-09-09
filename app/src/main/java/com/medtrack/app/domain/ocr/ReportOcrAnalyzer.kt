package com.medtrack.app.domain.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrExtractionResult(
    val rawText: String,
    val detectedDate: String?,
    val detectedLab: String?,
    val extractedRecords: List<ParsedLabRow>
)

class ReportOcrAnalyzer(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun analyzeBitmap(bitmap: Bitmap): OcrExtractionResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return processImage(image)
    }

    suspend fun analyzeUri(uri: Uri): OcrExtractionResult {
        val image = InputImage.fromFilePath(context, uri)
        return processImage(image)
    }

    private suspend fun processImage(image: InputImage): OcrExtractionResult {
        val visionText = recognizer.process(image).awaitTask()
        val rawText = visionText.text
        return ClinicalRegexParser.parseReportText(rawText)
    }

    private suspend fun <T> Task<T>.awaitTask(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result -> continuation.resume(result) }
            addOnFailureListener { exception -> continuation.resumeWithException(exception) }
            addOnCanceledListener { continuation.cancel() }
        }
    }
}
