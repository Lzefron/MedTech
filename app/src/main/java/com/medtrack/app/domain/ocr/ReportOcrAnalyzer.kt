package com.medtrack.app.domain.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrExtractionResult(val rawText: String, val detectedDate: String?, val detectedLab: String?, val extractedRecords: List<ParsedLabRow>)

class ReportOcrAnalyzer(private val context: Context) : AutoCloseable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    suspend fun analyzeBitmap(bitmap: Bitmap): OcrExtractionResult = processImage(InputImage.fromBitmap(bitmap, 0))
    suspend fun analyzeUri(uri: Uri): OcrExtractionResult {
        val bitmap = withContext(Dispatchers.IO) { DocumentImages.decode(context, uri) }
        // ML Kit owns access until its task finishes; do not recycle on coroutine cancellation.
        return analyzeBitmap(bitmap)
    }
    private suspend fun processImage(image: InputImage): OcrExtractionResult {
        val text = recognizer.process(image).awaitTask()
        val fragments = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            line.boundingBox?.let { PositionedText(line.text, it.left, it.top, it.right, it.bottom) }
        }
        val rows = OcrTableLayout.rows(fragments)
        return withContext(Dispatchers.Default) { ClinicalRegexParser.parseReportText(rows.joinToString("\n")) }
    }
    override fun close() { recognizer.close() }
    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
}
