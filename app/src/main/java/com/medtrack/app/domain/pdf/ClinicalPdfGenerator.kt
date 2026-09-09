package com.medtrack.app.domain.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.medtrack.app.data.local.entity.LabReportEntity
import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.data.local.entity.TestRecordEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClinicalPdfGenerator {

    fun generateClinicalReport(
        context: Context,
        profile: ProfileEntity,
        reports: List<LabReportEntity>,
        allRecords: List<TestRecordEntity>,
        activeMedicines: List<MedicineEntity>
    ): Uri {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72 dpi
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 50, 110)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(60, 60, 60)
            textSize = 10f
            typeface = Typeface.DEFAULT
        }

        val alertPaint = Paint().apply {
            color = Color.rgb(200, 30, 30)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 45f

        // Document Title
        canvas.drawText("MEDTRACK — CLINICAL HEALTH SUMMARY", 40f, y, titlePaint)
        y += 18f

        val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr | Local-First Medical Vault", 40f, y, bodyPaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Patient Demographics Box
        canvas.drawText("PATIENT DEMOGRAPHICS", 40f, y, headerPaint)
        y += 15f

        canvas.drawText("Name: ${profile.name} (${profile.relationship})", 40f, y, bodyPaint)
        canvas.drawText("Blood Group: ${profile.bloodGroup}", 320f, y, bodyPaint)
        y += 14f

        canvas.drawText("Gender: ${profile.gender} | DOB: ${profile.dob}", 40f, y, bodyPaint)
        canvas.drawText("Emergency Call: ${profile.emergencyPhone}", 320f, y, bodyPaint)
        y += 14f

        canvas.drawText("Allergies: ${profile.allergies}", 40f, y, if (profile.allergies != "None") alertPaint else bodyPaint)
        canvas.drawText("Chronic Conditions: ${profile.chronicConditions}", 320f, y, bodyPaint)
        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Active Medications
        canvas.drawText("ACTIVE MEDICATIONS", 40f, y, headerPaint)
        y += 15f

        if (activeMedicines.isEmpty()) {
            canvas.drawText("No currently active prescribed medications recorded.", 40f, y, bodyPaint)
            y += 16f
        } else {
            for (med in activeMedicines.take(6)) {
                canvas.drawText("• ${med.name} — Dose: ${med.dosage} (${med.frequency}, ${med.mealTiming})", 45f, y, bodyPaint)
                y += 14f
            }
        }
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Historical Laboratory Diagnostics
        canvas.drawText("LONGITUDINAL LABORATORY TEST RESULTS", 40f, y, headerPaint)
        y += 18f

        // Table Header
        val colDate = 40f
        val colTest = 120f
        val colVal = 280f
        val colRef = 390f
        val colStatus = 490f

        canvas.drawText("Date", colDate, y, headerPaint)
        canvas.drawText("Test Name", colTest, y, headerPaint)
        canvas.drawText("Result", colVal, y, headerPaint)
        canvas.drawText("Reference", colRef, y, headerPaint)
        canvas.drawText("Status", colStatus, y, headerPaint)
        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 14f

        // Sort all records descending by date
        val sortedRecords = allRecords.sortedByDescending { it.testDate }.take(22)
        for (rec in sortedRecords) {
            canvas.drawText(rec.testDate, colDate, y, bodyPaint)
            canvas.drawText(rec.canonicalName.take(24), colTest, y, bodyPaint)
            canvas.drawText("${rec.value} ${rec.unit}", colVal, y, if (rec.isAbnormal) alertPaint else bodyPaint)
            canvas.drawText(rec.refRangeText ?: "-", colRef, y, bodyPaint)
            if (rec.isAbnormal) {
                canvas.drawText("ABNORMAL", colStatus, y, alertPaint)
            } else {
                canvas.drawText("NORMAL", colStatus, y, bodyPaint)
            }
            y += 14f
        }

        // Clinician Footer Note
        y = 800f
        canvas.drawLine(40f, y - 10f, 555f, y - 10f, linePaint)
        canvas.drawText("Confidential Health Record — For Medical Professional Evaluation Only", 40f, y, bodyPaint)

        document.finishPage(page)

        // Write to cache file
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(reportsDir, "MedTrack_${profile.name.replace(" ", "_")}_Summary.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
    }

    fun createShareIntent(context: Context, pdfUri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "MedTrack Health Summary")
            putExtra(Intent.EXTRA_TEXT, "Attached is the clinical health summary report generated by MedTrack.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
