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
            Patient: Sajib Ahmed

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
}
