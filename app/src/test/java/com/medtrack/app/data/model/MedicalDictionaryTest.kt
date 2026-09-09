package com.medtrack.app.data.model

import org.junit.Assert.*
import org.junit.Test

class MedicalDictionaryTest {

    @Test
    fun testFindCanonicalByExactName() {
        val test = MedicalDictionary.findCanonical("Hemoglobin")
        assertNotNull(test)
        assertEquals("g/dL", test!!.defaultUnit)
    }

    @Test
    fun testFindCanonicalByAlias() {
        val test = MedicalDictionary.findCanonical("hgb")
        assertNotNull(test)
        assertEquals("Hemoglobin", test!!.name)

        val fbs = MedicalDictionary.findCanonical("fasting blood sugar")
        assertNotNull(fbs)
        assertEquals("Fasting Blood Glucose", fbs!!.name)
    }

    @Test
    fun testSearchDictionary() {
        val results = MedicalDictionary.search("creatin")
        assertTrue(results.any { it.name == "Serum Creatinine" })
    }
}
