package com.medtrack.app.domain.ocr

import com.medtrack.app.data.model.CanonicalTest
import com.medtrack.app.data.model.MedicalDictionary
import com.medtrack.app.domain.validation.HealthInput

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
    private val units = (MedicalDictionary.tests.flatMap { com.medtrack.app.domain.validation.ReferenceCatalog.units(it.name) } + listOf("cells/µL", "million/µL", "/µL", "umol/L", "10^3/uL", "10^6/uL")).distinct().sortedByDescending { it.length }
    private val number = """(?:\d{1,3}(?:,\d{3})+|\d+)(?:\.\d+)?|\.\d+"""
    private val pairRange = Regex("""($number)\s*(?:-|–|—|to)\s*($number)""", RegexOption.IGNORE_CASE)
    private val boundRange = Regex("""(?:<=|>=|≤|≥|<|>)\s*(?:$number)""")
    private val measured = Regex("""^\s*[:=()]*\s*($number)(?=\s|[A-Za-zµ%/*)]|$)""")

    fun parseReportText(rawText: String): OcrExtractionResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val date = ReportDateParser.detect(rawText)
        val lab = lines.take(10).firstNotNullOfOrNull { line ->
            listOf("Popular", "Ibn Sina", "Square", "LabAid", "United Hospital", "Apollo", "Evercare", "Praava", "BIRDEM", "BSMMU", "Quest Diagnostics", "LabCorp", "Metropolis", "Thyrocare")
                .firstOrNull { line.contains(it, ignoreCase = true) }
        }
        val title = lines.firstOrNull { it.length <= 100 && Regex("(?i)^(complete blood count|c\\.?b\\.?c\\.?|ha?ematology|lipid profile|biochemistry|thyroid profile|liver function tests?|renal function tests?)$").matches(it) }
        val laboratory = lab ?: lines.take(10).firstOrNull { it.length <= 120 && Regex("(?i)\\b(diagnostic|hospital|laboratory|clinic|medical cent(?:re|er))\\b").containsMatchIn(it) }
        return OcrExtractionResult(rawText, date.date, laboratory, lines.flatMap { line ->
            val found = aliases.flatMap { (test, alias, regex) -> regex.findAll(line).map { Match(test, alias, it.range.first, it.range.last + 1) }.toList() }
                .sortedWith(compareBy<Match> { it.start }.thenByDescending { it.alias.length })
            val nonOverlapping = mutableListOf<Match>()
            found.forEach { if (nonOverlapping.lastOrNull()?.end?.let { end -> it.start >= end } != false) nonOverlapping.add(it) }
            val segments = if (nonOverlapping.size > 1) nonOverlapping.mapIndexed { index, match -> line.substring(match.start, nonOverlapping.getOrNull(index + 1)?.start ?: line.length) } else listOf(line)
            segments.mapNotNull { parseLine(it) ?: partialRow(it.trim()) }
        }, date.explanation, detectedTitle = title)
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
        val rangeMatches = (pairRange.findAll(remainder).toList() + boundRange.findAll(remainder).toList()).sortedBy { it.range.first }
        val rangeMatch = rangeMatches.singleOrNull()
        val reference = rangeMatch?.value ?: ""
        val range = HealthInput.range(reference) ?: return null
        rangeMatches.asReversed().forEach { remainder = remainder.removeRange(it.range).trimEnd() }
        val valueMatch = measured.find(remainder) ?: return null
        val value = HealthInput.number(valueMatch.groupValues[1]) ?: return null
        // Reject a second unlabelled numeric value instead of picking arbitrarily.
        val tail = remainder.substring(valueMatch.range.last + 1).trim()
        val explicitUnit = units.firstOrNull { unit ->
            Regex("(?<![A-Za-z])" + Regex.escape(unit) + "(?![A-Za-z])", RegexOption.IGNORE_CASE).containsMatchIn(tail)
        }
        val nonUnitTail = if (explicitUnit != null && tail.startsWith(explicitUnit, true)) tail.drop(explicitUnit.length).trim() else tail
        if (Regex("""^\d""").containsMatchIn(nonUnitTail)) return null
        // Unit must be confirmed by the user if OCR did not find one.
        // Preserve printed values and units so the editable fields match the photo exactly.
        val unit = explicitUnit ?: ""
        val low = range.low
        val high = range.high
        val referenceText = reference
        val warnings = mutableListOf("Compare the test name, value, unit and range with the original.")
        if (explicitUnit == null) warnings.add("Unit not detected; enter it from the report.")
        if (reference.isBlank()) warnings.add("No printed reference range found.")
        if (rangeMatches.size > 1) warnings.add("Several printed ranges found; choose the applicable range manually.")
        if (unit == match.test.defaultUnit && (value < match.test.physiologicalMin || value > match.test.physiologicalMax))
            warnings.add("Unusual value; check for a reading error.")
        val valueText = java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        return ParsedLabRow(match.alias, match.test.name, valueText, value, unit, low, high, referenceText,
            HealthInput.range(referenceText)?.outside(value) == true, 0.65f, warnings.joinToString(" "))
    }

    private fun partialRow(line: String): ParsedLabRow? {
        if (Regex("(?i)\\b(patient|doctor|consultant|date|age|sex|reference|normal|method|specimen|collected)\\b").containsMatchIn(line)) return null
        val match = aliases.mapNotNull { (test, alias, regex) -> regex.find(line)?.let { Match(test, alias, it.range.first, it.range.last + 1) } }
            .filter { it.start == 0 }.maxByOrNull { it.alias.length }
        val generic = Regex("^([\\p{L}][\\p{L} /()%-]{1,65}?)\\s{2,}(.+)$").matchEntire(line)
        val name = match?.test?.name ?: generic?.groupValues?.get(1)?.trim() ?: return null
        val tail = if (match != null) line.substring(match.end).trim() else generic!!.groupValues[2].trim()
        val written = Regex("(?i)^(negative|positive|non[- ]reactive|reactive|not detected|detected|trace|[<>≤≥]\\s*[\\d.]+)(?=\\s|$)").find(tail)?.value
        val numeric = measured.find(tail)?.groupValues?.get(1)?.takeIf { HealthInput.number(it) != null }
        // Unknown tests need a recognisable result; known but unreadable tests remain blank for review.
        if (match == null && written == null && numeric == null) return null
        val value = written ?: if (match == null) numeric.orEmpty() else ""
        val unit = units.firstOrNull { Regex("(?<![A-Za-z])${Regex.escape(it)}(?![A-Za-z])", RegexOption.IGNORE_CASE).containsMatchIn(tail) }.orEmpty()
        return ParsedLabRow(name, name, value, if (written == null) HealthInput.number(value) else null, unit,
            null, null, "", false, 0.2f, "Partial reading — check and complete this row. Photo text: ${line.take(300)}", isNumeric = written == null)
    }
}
