package com.medtrack.app.domain.ocr

import com.medtrack.app.data.model.CanonicalTest
import com.medtrack.app.data.model.MedicalDictionary
import com.medtrack.app.domain.validation.HealthInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

data class ParsedLabRow(
    val testName: String, val canonicalName: String, val value: String, val numericValue: Double?,
    val unit: String, val refRangeLow: Double?, val refRangeHigh: Double?, val refRangeText: String,
    val isAbnormal: Boolean, val confidenceScore: Float, val warningNote: String? = null, val isNumeric: Boolean = true,
    val referenceSource: String? = null
)

object ClinicalRegexParser {
    private data class Match(val test: CanonicalTest, val alias: String, val start: Int, val end: Int)
    private val aliases = MedicalDictionary.tests.flatMap { test ->
        (listOf(test.name) + test.aliases).map { alias ->
            Triple(test, alias, Regex("(?<![\\p{L}\\d])" + Regex.escape(alias) + "(?![\\p{L}\\d])", RegexOption.IGNORE_CASE))
        }
    }
    private val units = listOf("million/mcL", "cells/mcL", "mmol/L", "µmol/L", "umol/L", "mIU/L", "mg/dL", "ng/dL", "g/dL", "pg/mL", "ng/mL", "mg/L", "g/L", "/mcL", "mm/hr", "U/L", "fL", "pg", "%")
    private val number = """(?:\d{1,3}(?:,\d{3})+|\d+)(?:\.\d+)?|\.\d+"""
    private val pairRange = Regex("""($number)\s*(?:-|–|—|to)\s*($number)""", RegexOption.IGNORE_CASE)
    private val boundRange = Regex("""(?:<=|>=|≤|≥|<|>)\s*(?:$number)""")
    private val measured = Regex("""^\s*[:=()]*\s*($number)(?=\s|[A-Za-zµ%/*)]|$)""")

    fun parseReportText(rawText: String): OcrExtractionResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        // A date of birth or an arbitrary page date must not become the report date.
        val dates = lines.filter { Regex("""\b(report|test|collection|sample|date)\b""", RegexOption.IGNORE_CASE).containsMatchIn(it) }
            .filterNot { Regex("""\b(dob|birth)\b""", RegexOption.IGNORE_CASE).containsMatchIn(it) }
            .mapNotNull { extractDate(it) }.distinct()
        val lab = lines.take(10).firstNotNullOfOrNull { line ->
            listOf("Popular", "Ibn Sina", "Square", "LabAid", "United Hospital", "Apollo", "Evercare", "Praava", "BIRDEM", "BSMMU", "Quest Diagnostics", "LabCorp", "Metropolis", "Thyrocare")
                .firstOrNull { line.contains(it, ignoreCase = true) }
        }
        return OcrExtractionResult(rawText, dates.singleOrNull(), lab, lines.mapNotNull { parseLine(it) })
    }

    private fun extractDate(line: String): String? {
        val raw = Regex("""\d{4}[-/.]\d{1,2}[-/.]\d{1,2}|\d{1,2}[-/.]\d{1,2}[-/.]\d{4}|\d{1,2}\s+[A-Za-z]+\s+\d{4}""").find(line)?.value ?: return null
        for (format in listOf("uuuu-M-d", "uuuu/M/d", "uuuu.M.d", "d/M/uuuu", "d-M-uuuu", "d.M.uuuu", "d MMM uuuu", "d MMMM uuuu")) {
            val date = runCatching { LocalDate.parse(raw, DateTimeFormatter.ofPattern(format, Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)) }.getOrNull()
            if (date != null && HealthInput.dateError(date.toString()) == null) return date.toString()
        }
        return null
    }

    fun parseLine(line: String): ParsedLabRow? {
        val clean = line.map { if (it in '০'..'৯') '0' + (it - '০') else it }.joinToString("")
        val match = aliases.mapNotNull { (test, alias, regex) ->
            regex.find(clean)?.let { Match(test, alias, it.range.first, it.range.last + 1) }
        }.minWithOrNull(compareBy<Match> { it.start }.thenByDescending { it.alias.length }) ?: return null
        var remainder = clean.substring(match.end).trim()
        // A leading inequality is a censored result, not a reference range. Leave it for manual entry.
        if (remainder.trimStart(':', ' ').startsWith("<") || remainder.trimStart(':', ' ').startsWith(">") ||
            remainder.startsWith("≤") || remainder.startsWith("≥")) return null
        val rangeMatch = pairRange.find(remainder) ?: boundRange.find(remainder)
        val reference = rangeMatch?.value ?: ""
        val range = HealthInput.range(reference) ?: return null
        if (rangeMatch != null) remainder = remainder.removeRange(rangeMatch.range).trim()
        val valueMatch = measured.find(remainder) ?: return null
        var value = HealthInput.number(valueMatch.groupValues[1]) ?: return null
        // Reject a second unlabelled numeric value instead of picking arbitrarily.
        val tail = remainder.substring(valueMatch.range.last + 1).trim()
        if (Regex("""^\d""").containsMatchIn(tail)) return null
        val explicitUnit = units.firstOrNull { unit ->
            Regex("(?<![A-Za-z])" + Regex.escape(unit) + "(?![A-Za-z])", RegexOption.IGNORE_CASE).containsMatchIn(tail)
        }
        // Unit must be confirmed by the user if OCR did not find one.
        var unit = explicitUnit ?: ""
        var low = range.low
        var high = range.high
        var referenceText = reference
        if (unit.isNotBlank()) {
            val normalized = UnitConverter.normalizeValue(match.test.name, value, unit)
            if (normalized.second != unit) {
                low = low?.let { UnitConverter.normalizeValue(match.test.name, it, unit).first }
                high = high?.let { UnitConverter.normalizeValue(match.test.name, it, unit).first }
                referenceText = when {
                    low != null && high != null -> low.toString() + " - " + high
                    low != null -> (if (range.lowExclusive) "> " else ">= ") + low
                    high != null -> (if (range.highExclusive) "< " else "<= ") + high
                    else -> ""
                }
                value = normalized.first
                unit = normalized.second
            }
        }
        val warnings = mutableListOf("Compare the test name, value, unit and range with the original.")
        if (explicitUnit == null) warnings.add("Unit not detected; enter it from the report.")
        if (reference.isBlank()) warnings.add("No printed reference range found.")
        if (unit == match.test.defaultUnit && (value < match.test.physiologicalMin || value > match.test.physiologicalMax))
            warnings.add("Unusual value; check for a reading error.")
        val valueText = java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        return ParsedLabRow(match.alias, match.test.name, valueText, value, unit, low, high, referenceText,
            HealthInput.range(referenceText)?.outside(value) == true, 0.65f, warnings.joinToString(" "))
    }
}
