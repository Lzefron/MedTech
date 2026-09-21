package com.medtrack.app

import android.graphics.pdf.PdfRenderer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medtrack.app.data.local.entity.*
import com.medtrack.app.domain.pdf.ClinicalPdfGenerator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfExportTest {
    @Test fun longHistoryExportsAcrossPagesWithoutTruncation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val profile = ProfileEntity(name = "Long history review")
        val report = LabReportEntity(id = 1, profileId = 1, title = "Full history", testDate = "2024-01-01")
        val rows = (1..100).map { index ->
            TestRecordEntity(id = index.toLong(), reportId = 1, profileId = 1, testName = "Result " + index,
                canonicalName = "Result " + index, value = "14.2", numericValue = 14.2, unit = "g/dL", testDate = "2024-01-01")
        }
        val uri = ClinicalPdfGenerator.generateClinicalReport(context, profile, listOf(report), rows, emptyList())
        context.contentResolver.openFileDescriptor(uri, "r")!!.use { descriptor ->
            PdfRenderer(descriptor).use { pdf -> assertTrue("All 100 results need multiple pages", pdf.pageCount >= 5) }
        }
    }
}
