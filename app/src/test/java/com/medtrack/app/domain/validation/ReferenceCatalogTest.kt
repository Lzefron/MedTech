package com.medtrack.app.domain.validation

import org.junit.Assert.*
import org.junit.Test

class ReferenceCatalogTest {
    @Test fun groupIsRequiredAndSexIsNeverGuessed() {
        assertNull(ReferenceCatalog.suggest("RBC Count", "million/mcL", ""))
        assertNull(ReferenceCatalog.suggest("RBC Count", "million/mcL", "Adult"))
        assertEquals("4.2–5.4", ReferenceCatalog.suggest("RBC Count", "million/mcL", "Adult female")!!.range)
        assertEquals("4.6–6.2", ReferenceCatalog.suggest("RBC Count", "10^12/L", "Adult male")!!.range)
        assertNull(ReferenceCatalog.suggest("WBC Count", "cells/mcL", "Child"))
    }
    @Test fun countAndMassUnitsUseDifferentScales() {
        assertEquals("150–400", ReferenceCatalog.suggest("Platelet Count", "10^9/L", "Adult")!!.range)
        assertEquals("4500–11000", ReferenceCatalog.suggest("WBC Count", "cells/µL", "Adult")!!.range)
        assertEquals("120–160", ReferenceCatalog.suggest("Hemoglobin", "g/L", "Adult female")!!.range)
        assertEquals("53–114.9", ReferenceCatalog.suggest("Serum Creatinine", "umol/L", "Adult")!!.range)
    }
    @Test fun unknownUnitsAndContextDependentTestsHaveNoInventedRange() {
        assertNull(ReferenceCatalog.suggest("WBC Count", "%", "Adult"))
        assertNull(ReferenceCatalog.suggest("Random Blood Glucose", "mg/dL", "Adult"))
        assertNull(ReferenceCatalog.suggest("HbA1c", "%", "Adult"))
        assertNull(ReferenceCatalog.suggest("Custom test", "mg/dL", "Adult"))
    }
    @Test fun everyReferenceIsParsableAndCarriesSource() {
        ReferenceCatalog.entries.forEach { entry ->
            (listOf(entry.unit) + entry.factors.keys).forEach { unit ->
                val result = ReferenceCatalog.suggest(entry.test, unit, entry.group)!!
                assertNotNull(HealthInput.range(result.range))
                assertTrue(result.source.contains("2026-09-22"))
                assertTrue(result.url.startsWith("https://medlineplus.gov/ency/article/"))
            }
        }
    }
    @Test fun unitSuggestionsAllowMisspellingWithoutDroppingChoices() {
        val choices = listOf("mmol/L", "mg/dL", "µmol/L")
        assertEquals("mg/dL", ReferenceCatalog.rank(choices, "mg/dl").first())
        assertEquals(choices.toSet(), ReferenceCatalog.rank(choices, "mg/d").toSet())
        assertTrue(ReferenceCatalog.units("HbA1c").contains("mmol/mol"))
    }
}
