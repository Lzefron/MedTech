package com.medtrack.app.domain.ocr

import org.junit.Assert.*
import org.junit.Test

class ScanRecoveryTest {
    @Test fun namedAndMultilineDatesExcludeBirth() {
        assertEquals("2024-09-18", ReportDateParser.detect("DOB: 01/01/1960  Collection Date:\n18-SEP-2024  Report date: 19/09/2024").date)
        assertNull(ReportDateParser.detect("Date of Birth: 18-Sep-1960").date)
        assertNull(ReportDateParser.detect("Test Date: 2024-01-02\nTest Date: 2024-01-03").date)
    }
    @Test fun sixRowsIncludePartialUnknownAndWrittenResults() {
        val text = "Hemoglobin 14 g/dL 12-16\nWBC Count unreadable\nPlatelet Count 250000 /mcL\nRBC Count 4.5 million/mcL\nCustom Marker  12 mg/L\nUnknown Screen  Negative"
        val rows = ClinicalRegexParser.parseReportText(text).extractedRecords
        assertEquals(6, rows.size)
        assertEquals("", rows[1].value)
        assertFalse(rows.last().isNumeric)
        assertEquals("Negative", rows.last().value)
    }
    @Test fun twoPanelsOnSameLineRetainBothTests() {
        val rows = ClinicalRegexParser.parseReportText("Hemoglobin 14 g/dL 12-16    Platelet Count 250000 /mcL 150000-400000").extractedRecords
        assertEquals(2, rows.size)
        assertEquals("250000", rows[1].value)
    }
    @Test fun multiplePatientRangesAreNotGuessed() {
        assertEquals("", ClinicalRegexParser.parseLine("Hemoglobin 14 g/dL Male 13-18 Female 12-16")!!.refRangeText)
    }
    @Test fun layoutFindsEarlierMatchingRowWhenColumnsAreSkewed() {
        val rows = OcrTableLayout.rows(listOf(PositionedText("Hb",0,10,80,30), PositionedText("WBC",0,19,80,23), PositionedText("14",150,18,180,38)))
        assertEquals(listOf("Hb  14", "WBC"), rows)
    }
    @Test fun aiMergeDoesNotReplaceUserCorrectionsOrDropMissingRows() {
        val row = ClinicalRegexParser.parseLine("Hemoglobin 14 g/dL 12-16")!!
        val merged = ScanMerge.merge(listOf(row), listOf(row.copy(value="9", numericValue=9.0), row.copy(testName="Unknown", canonicalName="Unknown")))
        assertEquals("14", merged.first().value)
        assertTrue(merged.first().warningNote!!.contains("Readings differ"))
        assertEquals(2, merged.size)
    }
    @Test fun aiMergeFillsBlankFields() {
        val row = ClinicalRegexParser.parseLine("Hemoglobin 14 g/dL 12-16")!!
        val merged = ScanMerge.merge(listOf(row.copy(value="", numericValue=null, unit="", refRangeText="")), listOf(row)).single()
        assertEquals("14", merged.value); assertEquals("g/dL", merged.unit); assertEquals("12-16", merged.refRangeText)
    }
}
