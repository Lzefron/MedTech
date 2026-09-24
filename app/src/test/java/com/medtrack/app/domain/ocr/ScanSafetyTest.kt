package com.medtrack.app.domain.ocr

import org.junit.Assert.*
import org.junit.Test

class ScanSafetyTest {
    @Test fun missingDateAndRangeRemainUnknown() {
        val result = ClinicalRegexParser.parseReportText("Hemoglobin 14.2 g/dL")
        assertNull(result.detectedDate)
        assertNull(result.extractedRecords.single().refRangeLow)
        assertFalse(result.extractedRecords.single().isAbnormal)
    }
    @Test fun dateOfBirthIsNotUsedAsTestDate() {
        assertNull(ClinicalRegexParser.parseReportText("DOB: 2000-01-02\nHemoglobin 14 g/dL").detectedDate)
    }
    @Test fun invalidDateIsNotRolledIntoNextMonth() {
        assertNull(ClinicalRegexParser.parseReportText("Date: 2024-02-30").detectedDate)
    }
    @Test fun collectionDateTakesPriorityOverReportPrintingDate() {
        assertEquals("2024-01-01", ClinicalRegexParser.parseReportText("Collection date: 2024-01-01\nReport date: 2024-01-02").detectedDate)
    }
    @Test fun commaSeparatedPlateletsAreNotTruncated() {
        val row = ClinicalRegexParser.parseLine("Platelet Count 250,000 /mcL 150,000 - 450,000")!!
        assertEquals(250000.0, row.numericValue!!, 0.0)
        assertEquals(150000.0, row.refRangeLow!!, 0.0)
    }
    @Test fun unsafeValuesAreNotGuessed() {
        assertNull(ClinicalRegexParser.parseLine("Hemoglobin -14 g/dL"))
        assertNull(ClinicalRegexParser.parseLine("Hemoglobin < 3 g/dL"))
        assertNull(ClinicalRegexParser.parseLine("Hemoglobin 14 25 g/dL"))
        assertNull(ClinicalRegexParser.parseLine("Hemoglobin abc g/dL"))
    }
    @Test fun repeatedTestsArePreservedForReview() {
        assertEquals(2, ClinicalRegexParser.parseReportText("Hemoglobin 14 g/dL\nHemoglobin 15 g/dL").extractedRecords.size)
    }
    @Test fun tableLayoutJoinsColumnsWithoutMixingAdjacentRows() {
        val lines = listOf(
            PositionedText("Hemoglobin", 0, 10, 100, 30), PositionedText("14.2", 150, 11, 200, 31),
            PositionedText("g/dL", 220, 12, 270, 32), PositionedText("Platelets", 0, 40, 100, 60),
            PositionedText("250000", 150, 41, 200, 61))
        assertEquals(listOf("Hemoglobin  14.2  g/dL", "Platelets  250000"), OcrTableLayout.rows(lines))
    }
    @Test fun triglyceridesUseTheirOwnConversionFactor() {
        assertEquals(177.14, UnitConverter.normalizeValue("Triglycerides", 2.0, "mmol/L").first, 0.001)
        assertEquals(77.34, UnitConverter.normalizeValue("Total Cholesterol", 2.0, "mmol/L").first, 0.001)
    }
}
