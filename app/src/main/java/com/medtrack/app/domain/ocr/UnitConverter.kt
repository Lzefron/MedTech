package com.medtrack.app.domain.ocr

object UnitConverter {
    fun normalizeValue(metricName: String, value: Double, currentUnit: String): Pair<Double, String> {
        val unitClean = currentUnit.trim().lowercase()
        val metricClean = metricName.trim().lowercase()

        // Glucose conversions: mmol/L <-> mg/dL
        if (metricClean.contains("glucose") || metricClean.contains("sugar") || metricClean.contains("fbs") || metricClean.contains("rbs")) {
            if (unitClean == "mmol/l" || unitClean == "mmol") {
                // convert to mg/dL for standard plotting
                return Pair(value * 18.0182, "mg/dL")
            }
        }

        // Cholesterol & Triglycerides: mmol/L <-> mg/dL
        if (metricClean.contains("cholesterol") || metricClean.contains("triglyceride") || metricClean.contains("hdl") || metricClean.contains("ldl")) {
            if (unitClean == "mmol/l" || unitClean == "mmol") {
                return Pair(value * 38.67, "mg/dL")
            }
        }

        // Hemoglobin: g/L <-> g/dL
        if (metricClean.contains("hemoglobin") || metricClean.contains("hb")) {
            if (unitClean == "g/l") {
                return Pair(value / 10.0, "g/dL")
            }
        }

        // Creatinine: umol/L <-> mg/dL
        if (metricClean.contains("creatinine")) {
            if (unitClean.contains("umol") || unitClean.contains("µmol")) {
                return Pair(value / 88.42, "mg/dL")
            }
        }

        return Pair(value, currentUnit)
    }
}
