package com.medtrack.app.domain.ocr

import org.junit.Assert.*
import org.junit.Test

class ClinicalRegexParserTest {

    @Test
    fun testParseCbcReportLine() {
        val line = "Hemoglobin 14.2 g/dL 13.0 - 17.5"
        val parsed = ClinicalRegexParser.parseLine(line)

        assertNotNull(parsed)
        assertEquals("Hemoglobin", parsed!!.canonicalName)
        assertEquals("14.2", parsed.value)
        assertEquals(14.2, parsed.numericValue!!, 0.001)
        assertEquals("g/dL", parsed.unit)
        assertEquals(13.0, parsed.refRangeLow!!, 0.001)
        assertEquals(17.5, parsed.refRangeHigh!!, 0.001)
        assertFalse(parsed.isAbnormal)
    }

    @Test
    fun testParseAbnormalGlucose() {
        val line = "Fasting Blood Glucose: 118 mg/dL (70 - 99)"
        val parsed = ClinicalRegexParser.parseLine(line)

        assertNotNull(parsed)
        assertEquals("Fasting Blood Glucose", parsed!!.canonicalName)
        assertEquals(118.0, parsed.numericValue!!, 0.001)
        assertTrue(parsed.isAbnormal)
    }

    @Test
    fun testParseLessThanRange() {
        val line = "Total Cholesterol 220 mg/dL < 200"
        val parsed = ClinicalRegexParser.parseLine(line)

        assertNotNull(parsed)
        assertEquals("Total Cholesterol", parsed!!.canonicalName)
        assertEquals(220.0, parsed.numericValue!!, 0.001)
        assertTrue(parsed.isAbnormal)
    }

    @Test
    fun testParseFullReport() {
        val rawReport = """
            POPULAR DIAGNOSTIC CENTRE LTD.
            Date: 2026-08-15
            Patient: John Doe

            Hemoglobin 14.5 g/dL 13.0 - 17.5
            Platelet Count 245000 /mcL 150000 - 450000
            Serum Creatinine 0.95 mg/dL 0.7 - 1.3
            Fasting Blood Sugar 106 mg/dL 70 - 99
        """.trimIndent()

        val result = ClinicalRegexParser.parseReportText(rawReport)

        assertEquals("2026-08-15", result.detectedDate)
        assertEquals("Popular", result.detectedLab)
        assertEquals(4, result.extractedRecords.size)

        val glucose = result.extractedRecords.find { it.canonicalName == "Fasting Blood Glucose" }
        assertNotNull(glucose)
        assertTrue(glucose!!.isAbnormal)
    }

    @Test
    fun testHdlNotShadowedByTotalCholesterol() {
        val parsed = ClinicalRegexParser.parseLine("HDL Cholesterol 45 mg/dL 40 - 60")
        assertNotNull(parsed)
        assertEquals("HDL Cholesterol", parsed!!.canonicalName)
        assertEquals(45.0, parsed.numericValue!!, 0.001)
        assertFalse(parsed.isAbnormal)
    }

    @Test
    fun testLdlNotShadowedByTotalCholesterol() {
        val parsed = ClinicalRegexParser.parseLine("LDL Cholesterol 140 mg/dL 0 - 100")
        assertNotNull(parsed)
        assertEquals("LDL Cholesterol", parsed!!.canonicalName)
        assertEquals(140.0, parsed.numericValue!!, 0.001)
        assertTrue(parsed.isAbnormal)
    }

    @Test
    fun testHba1cNotShadowedByHemoglobin() {
        val parsed = ClinicalRegexParser.parseLine("Glycated Hemoglobin 6.2 % 4.0 - 5.6")
        assertNotNull(parsed)
        assertEquals("HbA1c", parsed!!.canonicalName)
        assertEquals(6.2, parsed.numericValue!!, 0.001)
        assertTrue(parsed.isAbnormal)
    }

    @Test
    fun testRangeBeforeValueDoesNotStoreRangeLow() {
        val parsed = ClinicalRegexParser.parseLine("Hemoglobin (13.0 - 17.5) 14.2 g/dL")
        assertNotNull(parsed)
        assertEquals(14.2, parsed!!.numericValue!!, 0.001)
        assertEquals(13.0, parsed.refRangeLow!!, 0.001)
        assertEquals(17.5, parsed.refRangeHigh!!, 0.001)
        assertFalse(parsed.isAbnormal)
    }

    @Test
    fun testLipidPanelKeepsAllFourRecords() {
        val raw = """
            Date: 2026-08-15
            Total Cholesterol 215 mg/dL < 200
            Triglycerides 180 mg/dL < 150
            HDL Cholesterol 45 mg/dL 40 - 60
            LDL Cholesterol 140 mg/dL 0 - 100
        """.trimIndent()
        val result = ClinicalRegexParser.parseReportText(raw)
        assertEquals(4, result.extractedRecords.size)
        assertNotNull(result.extractedRecords.find { it.canonicalName == "HDL Cholesterol" })
        assertNotNull(result.extractedRecords.find { it.canonicalName == "LDL Cholesterol" })
        assertNotNull(result.extractedRecords.find { it.canonicalName == "Total Cholesterol" })
        assertNotNull(result.extractedRecords.find { it.canonicalName == "Triglycerides" })
    }

    @Test
    fun testMmolPerLGlucoseConvertedToMgPerDl() {
        val parsed = ClinicalRegexParser.parseLine("Fasting Blood Sugar 6.0 mmol/L 3.9 - 5.5")
        assertNotNull(parsed)
        assertEquals("Fasting Blood Glucose", parsed!!.canonicalName)
        assertEquals("mg/dL", parsed.unit)
        assertEquals(6.0 * 18.0182, parsed.numericValue!!, 0.01)
        assertEquals(3.9 * 18.0182, parsed.refRangeLow!!, 0.01)
        assertEquals(5.5 * 18.0182, parsed.refRangeHigh!!, 0.01)
        // 6.0 mmol/L (~108 mg/dL) is above the 5.5 mmol/L (~99 mg/dL) upper bound
        assertTrue(parsed.isAbnormal)
    }
}
