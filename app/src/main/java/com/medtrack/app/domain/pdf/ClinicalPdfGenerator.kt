package com.medtrack.app.domain.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.medtrack.app.data.local.entity.*
import java.io.File
import java.util.UUID

object ClinicalPdfGenerator {
    fun generateClinicalReport(context: Context, profile: ProfileEntity, reports: List<LabReportEntity>,
        allRecords: List<TestRecordEntity>, activeMedicines: List<MedicineEntity>): Uri {
        val document = PdfDocument()
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.rgb(35, 45, 42) }
        val heading = Paint(body).apply { textSize = 16f; isFakeBoldText = true }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 52f
        fun finishPage() {
            page?.let {
                it.canvas.drawText("MedTrack · Personal health record · Page " + pageNumber, 40f, 810f, body)
                document.finishPage(it)
                page = null
            }
        }
        fun newPage() {
            finishPage()
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 52f
        }
        fun line(text: String, paint: Paint = body) {
            for (paragraph in text.lines()) {
                var remaining = paragraph
                do {
                    if (page == null || y > 770f) newPage()
                    if (remaining.isEmpty()) { y += 10f; break }
                    var count = paint.breakText(remaining, true, 510f, null).coerceAtLeast(1)
                    if (count < remaining.length) {
                        val space = remaining.lastIndexOf(' ', count - 1)
                        if (space > count / 2) count = space
                    }
                    page!!.canvas.drawText(remaining.take(count), 40f, y, paint)
                    remaining = remaining.drop(count).trimStart()
                    y += paint.textSize + 6f
                } while (remaining.isNotEmpty())
            }
        }
        val directory = File(context.cacheDir, "reports").apply { mkdirs() }
        val output = File(directory, "MedTrack-" + UUID.randomUUID() + ".pdf")
        try {
            line("Health summary", heading)
            line(profile.name, heading)
            line("Relationship: " + profile.relationship.ifBlank { "Not recorded" })
            line("Birth date: " + profile.dob.ifBlank { "Not recorded" } + " · Sex: " + profile.gender)
            line("Blood group: " + profile.bloodGroup)
            line("Emergency contact: " + profile.emergencyPhone.ifBlank { "Not recorded" })
            line("Allergies: " + profile.allergies.ifBlank { "Not recorded" })
            line("Conditions: " + profile.chronicConditions.ifBlank { "Not recorded" })
            line("")
            line("Active medicines", heading)
            if (activeMedicines.isEmpty()) line("No active medicines recorded.")
            activeMedicines.forEach { med ->
                line(med.name + " · " + med.dosage)
                val times = listOfNotNull(med.morningTime.takeIf { med.takeMorning }, med.afternoonTime.takeIf { med.takeAfternoon },
                    med.eveningTime.takeIf { med.takeEvening }, med.nightTime.takeIf { med.takeNight })
                line(med.frequency + " · " + times.joinToString(", "))
                if (med.mealTiming.isNotBlank()) line("Food instructions: " + med.mealTiming)
                if (med.notes.isNotBlank()) line("Notes: " + med.notes)
                line("")
            }
            line("Report history", heading)
            line("Values are transcribed records, not diagnoses. Missing ranges are not assessed.")
            if (reports.isEmpty()) line("No reports recorded.")
            reports.sortedByDescending { it.testDate }.forEach { report ->
                line("")
                line(report.testDate + " · " + report.title, heading)
                if (report.labName.isNotBlank()) line("Laboratory: " + report.labName)
                report.notes?.takeIf { it.isNotBlank() }?.let { line("Notes: " + it) }
                allRecords.filter { it.reportId == report.id }.forEach { record ->
                    line(record.canonicalName + ": " + record.value + " " + record.unit)
                    val assessed = record.numericValue != null && (record.refRangeLow != null || record.refRangeHigh != null)
                    val status = if (!assessed) "Not assessed" else if (record.isAbnormal) "Outside saved range" else "Within saved range"
                    line("Reference: " + record.refRangeText.orEmpty().ifBlank { "Not recorded" } + " · " + status)
                    record.referenceSource?.let { line(it) }
                }
            }
            finishPage()
            output.outputStream().use { document.writeTo(it) }
        } catch (e: Exception) { output.delete(); throw e
        } finally { document.close() }
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", output)
    }
    fun createShareIntent(context: Context, pdfUri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        putExtra(Intent.EXTRA_SUBJECT, "MedTrack health summary")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
