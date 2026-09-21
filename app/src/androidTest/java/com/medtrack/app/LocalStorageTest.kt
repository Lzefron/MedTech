package com.medtrack.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medtrack.app.data.local.MedTrackDatabase
import com.medtrack.app.data.local.entity.*
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.domain.ocr.DocumentImages
import com.medtrack.app.domain.ocr.ReportOcrAnalyzer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalStorageTest {
    private lateinit var database: MedTrackDatabase
    private lateinit var context: Context
    private lateinit var reports: LabReportRepository
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MedTrackDatabase::class.java).build()
        reports = LabReportRepository(database.labReportDao(), database.testRecordDao(), database, context)
    }
    @After fun close() { database.close() }

    @Test fun failedBatchRollsBackReportAndResults() = runBlocking {
        val profile = database.profileDao().insertProfile(ProfileEntity(name = "Test person"))
        val report = LabReportEntity(profileId = profile, title = "Test", testDate = "2024-01-01")
        val row = TestRecordEntity(id = 7, reportId = 0, profileId = profile, testName = "Hb", canonicalName = "Hemoglobin", value = "14", numericValue = 14.0, unit = "g/dL", testDate = "2024-01-01")
        val failed = runCatching { reports.saveReportWithRecords(report, listOf(row, row)) }.isFailure
        assertTrue(failed)
        assertTrue(reports.getReportsForProfile(profile).first().isEmpty())
        assertTrue(reports.getAllRecordsForProfile(profile).first().isEmpty())
    }
    @Test fun profilesKeepRecordsSeparateAndDeleteCascades() = runBlocking {
        val first = database.profileDao().insertProfile(ProfileEntity(name = "Person one"))
        val second = database.profileDao().insertProfile(ProfileEntity(name = "Person two"))
        val report = LabReportEntity(profileId = first, title = "Test", testDate = "2024-01-01")
        val row = TestRecordEntity(reportId = 0, profileId = first, testName = "Hb", canonicalName = "Hemoglobin", value = "14", numericValue = 14.0, unit = "g/dL", testDate = "2024-01-01")
        reports.saveReportWithRecords(report, listOf(row))
        assertEquals(1, reports.getReportsForProfile(first).first().size)
        assertTrue(reports.getReportsForProfile(second).first().isEmpty())
        database.profileDao().deleteProfile(database.profileDao().getProfileByIdSync(first)!!)
        assertTrue(reports.getAllRecordsForProfile(first).first().isEmpty())
        assertNotNull(database.profileDao().getProfileByIdSync(second))
    }
    @Test fun photoSurvivesSourceDeletionAndOcrReadsPrintedTable() = runBlocking {
        val bitmap = Bitmap.createBitmap(1400, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 42f }
        canvas.drawText("Report date: 2024-01-15", 40f, 80f, paint)
        canvas.drawText("Hemoglobin", 40f, 180f, paint)
        canvas.drawText("14.2", 550f, 180f, paint)
        canvas.drawText("g/dL", 800f, 180f, paint)
        canvas.drawText("13.0 - 17.5", 1050f, 180f, paint)
        canvas.drawText("Platelet Count", 40f, 270f, paint)
        canvas.drawText("250000", 550f, 270f, paint)
        canvas.drawText("/mcL", 800f, 270f, paint)
        val source = File(context.cacheDir, "synthetic-ocr-test.png")
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val saved = DocumentImages.persist(context, Uri.fromFile(source))
        source.delete()
        assertTrue(File(Uri.parse(saved).path!!).exists())
        val analyzer = ReportOcrAnalyzer(context)
        try {
            val result = analyzer.analyzeUri(Uri.parse(saved))
            assertEquals("2024-01-15", result.detectedDate)
            assertEquals(14.2, result.extractedRecords.first { it.canonicalName == "Hemoglobin" }.numericValue!!, 0.01)
            assertEquals(250000.0, result.extractedRecords.first { it.canonicalName == "Platelet Count" }.numericValue!!, 0.01)
        } finally { analyzer.close(); DocumentImages.deletePrivate(context, saved) }
    }
}
