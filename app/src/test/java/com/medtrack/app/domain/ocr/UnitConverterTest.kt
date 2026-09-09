package com.medtrack.app.domain.ocr

import org.junit.Assert.*
import org.junit.Test

class UnitConverterTest {

    @Test
    fun testGlucoseMmolToMgDl() {
        val (normVal, normUnit) = UnitConverter.normalizeValue("Fasting Glucose", 6.0, "mmol/L")
        assertEquals("mg/dL", normUnit)
        assertEquals(108.1, normVal, 0.5)
    }

    @Test
    fun testCholesterolMmolToMgDl() {
        val (normVal, normUnit) = UnitConverter.normalizeValue("Total Cholesterol", 5.2, "mmol/L")
        assertEquals("mg/dL", normUnit)
        assertEquals(201.0, normVal, 1.0)
    }

    @Test
    fun testHemoglobinGLToGdl() {
        val (normVal, normUnit) = UnitConverter.normalizeValue("Hemoglobin", 145.0, "g/L")
        assertEquals("g/dL", normUnit)
        assertEquals(14.5, normVal, 0.01)
    }
}
