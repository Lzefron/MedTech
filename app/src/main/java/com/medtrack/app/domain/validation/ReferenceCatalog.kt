package com.medtrack.app.domain.validation

import com.medtrack.app.data.model.MedicalDictionary
import java.util.Locale

/** Reviewed 2026-09-22. General adult references, never a substitute for the reporting lab. */
object ReferenceCatalog {
    data class Entry(val test: String, val low: Double, val high: Double, val unit: String,
        val article: String, val group: String = "Adult", val factors: Map<String, Double> = emptyMap())
    data class Suggestion(val range: String, val source: String, val url: String)
    private const val CBC = "003642"
    private const val CMP = "003468"
    val entries = listOf(
        Entry("Hemoglobin", 13.0, 18.0, "g/dL", CBC, "Adult male", mapOf("g/L" to 10.0)),
        Entry("Hemoglobin", 12.0, 16.0, "g/dL", CBC, "Adult female", mapOf("g/L" to 10.0)),
        Entry("RBC Count", 4.6, 6.2, "million/mcL", CBC, "Adult male", mapOf("10^12/L" to 1.0)),
        Entry("RBC Count", 4.2, 5.4, "million/mcL", CBC, "Adult female", mapOf("10^12/L" to 1.0)),
        Entry("Hematocrit (PCV)", 40.0, 55.0, "%", CBC, "Adult male", mapOf("L/L" to 0.01)),
        Entry("Hematocrit (PCV)", 36.0, 48.0, "%", CBC, "Adult female", mapOf("L/L" to 0.01)),
        Entry("WBC Count", 4500.0, 11000.0, "cells/mcL", CBC, factors = mapOf("10^9/L" to 0.001)),
        Entry("Platelet Count", 150000.0, 400000.0, "/mcL", "003647", factors = mapOf("10^9/L" to 0.001)),
        Entry("MCV", 80.0, 100.0, "fL", CBC),
        Entry("MCH", 27.0, 32.0, "pg", CBC),
        Entry("MCHC", 32.0, 36.0, "g/dL", CBC, factors = mapOf("g/L" to 10.0)),
        Entry("Neutrophils", 40.0, 60.0, "%", "003657"),
        Entry("Lymphocytes", 20.0, 40.0, "%", "003657"),
        Entry("Monocytes", 2.0, 8.0, "%", "003657"),
        Entry("Eosinophils", 1.0, 4.0, "%", "003657"),
        Entry("Blood Urea Nitrogen (BUN)", 6.0, 20.0, "mg/dL", CMP),
        Entry("Blood Urea Nitrogen (BUN)", 2.14, 7.14, "mmol/L", CMP),
        Entry("Serum Creatinine", 0.6, 1.3, "mg/dL", CMP),
        Entry("Serum Creatinine", 53.0, 114.9, "µmol/L", CMP),
        Entry("Fasting Blood Glucose", 70.0, 100.0, "mg/dL", CMP),
        Entry("Fasting Blood Glucose", 3.9, 5.6, "mmol/L", CMP),
        Entry("Serum Sodium", 135.0, 145.0, "mmol/L", CMP, factors = mapOf("mEq/L" to 1.0)),
        Entry("Serum Potassium", 3.7, 5.2, "mmol/L", CMP, factors = mapOf("mEq/L" to 1.0)),
        Entry("SGPT (ALT)", 4.0, 36.0, "U/L", CMP),
        Entry("SGPT (ALT)", 0.07, 0.60, "µkat/L", CMP),
        Entry("SGOT (AST)", 8.0, 33.0, "U/L", CMP),
        Entry("SGOT (AST)", 0.13, 0.55, "µkat/L", CMP),
        Entry("Alkaline Phosphatase (ALP)", 20.0, 130.0, "U/L", CMP),
        Entry("Alkaline Phosphatase (ALP)", 0.33, 2.17, "µkat/L", CMP),
        Entry("Total Bilirubin", 0.1, 1.2, "mg/dL", CMP),
        Entry("Total Bilirubin", 2.0, 21.0, "µmol/L", CMP),
        Entry("TSH", 0.4, 4.8, "mIU/L", "003684", factors = mapOf("µIU/mL" to 1.0))
    )
    fun unitKey(unit: String): String = unit.trim().lowercase(Locale.ROOT)
        .replace("μ", "u").replace("µ", "u").replace("mcl", "ul").replace(" ", "")
        .replace("×", "").replace("³", "^3").replace("⁹", "^9")
    fun units(test: String): List<String> {
        val default = MedicalDictionary.tests.firstOrNull { it.name == test }?.defaultUnit
        val alternatives = when (test) {
            "Postprandial Blood Glucose (2h)", "Random Blood Glucose", "Serum Urea", "Total Cholesterol", "Triglycerides", "HDL Cholesterol", "LDL Cholesterol" -> listOf("mg/dL", "mmol/L")
            "Uric Acid" -> listOf("mg/dL", "µmol/L")
            "HbA1c" -> listOf("%", "mmol/mol")
            "Free T4" -> listOf("ng/dL", "pmol/L")
            "Free T3" -> listOf("pg/mL", "pmol/L")
            "C-Reactive Protein (CRP)" -> listOf("mg/L", "mg/dL")
            "Vitamin D (25-OH)" -> listOf("ng/mL", "nmol/L")
            "Vitamin B12" -> listOf("pg/mL", "pmol/L")
            "Serum Ferritin" -> listOf("ng/mL", "µg/L")
            "ESR" -> listOf("mm/hr", "mm/h")
            else -> emptyList()
        }
        return (listOfNotNull(default) + entries.filter { it.test == test }.flatMap { listOf(it.unit) + it.factors.keys } + alternatives)
            .filter { it.isNotBlank() }.distinct()
    }
    fun rank(options: List<String>, query: String): List<String> {
        val needle = unitKey(query)
        fun distance(a: String, b: String): Int {
            var previous = IntArray(b.length + 1) { it }
            for (i in a.indices) {
                val next = IntArray(b.length + 1); next[0] = i + 1
                for (j in b.indices) next[j + 1] = minOf(next[j] + 1, previous[j + 1] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
                previous = next
            }
            return previous[b.length]
        }
        return options.sortedBy { if (needle.isBlank() || unitKey(it).contains(needle)) 0 else 1 + distance(needle, unitKey(it)) }
    }
    fun suggest(test: String, unit: String, group: String): Suggestion? {
        if (group !in listOf("Adult", "Adult male", "Adult female")) return null
        val entry = entries.firstOrNull { it.test == test && (it.group == "Adult" || it.group == group) &&
            (unitKey(it.unit) == unitKey(unit) || it.factors.keys.any { key -> unitKey(key) == unitKey(unit) }) } ?: return null
        val factor = if (unitKey(entry.unit) == unitKey(unit)) 1.0 else entry.factors.entries.first { unitKey(it.key) == unitKey(unit) }.value
        fun number(value: Double) = java.math.BigDecimal.valueOf(value).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        val url = "https://medlineplus.gov/ency/article/" + entry.article + ".htm"
        return Suggestion(number(entry.low * factor) + "–" + number(entry.high * factor),
            "General reference · $group, nonpregnant · MedlinePlus · checked 2026-09-22 · $url", url)
    }
}
