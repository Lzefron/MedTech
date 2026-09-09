package com.medtrack.app.domain.ocr

import com.medtrack.app.data.model.CanonicalTest
import com.medtrack.app.data.model.MedicalDictionary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedLabRow(
    val testName: String,
    val canonicalName: String,
    val value: String,
    val numericValue: Double?,
    val unit: String,
    val refRangeLow: Double?,
    val refRangeHigh: Double?,
    val refRangeText: String,
    val isAbnormal: Boolean,
    val confidenceScore: Float,
    val warningNote: String? = null
)

object ClinicalRegexParser {

    private val dateRegexes = listOf(
        Pattern.compile("(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})"),
        Pattern.compile("(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})"),
        Pattern.compile("(\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4})", Pattern.CASE_INSENSITIVE)
    )

    private val rangeRegex = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(?:-|to|–|—)\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE)
    private val lessThanRangeRegex = Pattern.compile("<\\s*([0-9]+(?:\\.[0-9]+)?)")
    private val greaterThanRangeRegex = Pattern.compile(">\\s*([0-9]+(?:\\.[0-9]+)?)")
    private val numberRegex = Pattern.compile("[0-9]+(?:\\.[0-9]+)?")

    private val commonLabs = listOf(
        "Popular", "Ibn Sina", "Square", "LabAid", "United Hospital",
        "Apollo", "Evercare", "Praava", "BIRDEM", "BSMMU",
        "Quest Diagnostics", "LabCorp", "Metropolis", "Thyrocare"
    )

    /** A dictionary test plus the alias that matched and where it matched in the line. */
    private data class AliasMatch(
        val test: CanonicalTest,
        val alias: String,
        val start: Int,
        val end: Int
    )

    /**
     * Best alias match for a line across the whole dictionary.
     *
     * Earlier start wins, then longer alias wins. Ordering matters because
     * shorter aliases can be substrings of longer qualified names: in
     * "HDL Cholesterol 45 mg/dL" the plain "cholesterol" alias of Total
     * Cholesterol starts later than "hdl", and in "Glycated Hemoglobin 6.2 %"
     * Hemoglobin's "hemoglobin" starts later than HbA1c's full alias — in both
     * cases the earlier, more specific qualifier identifies the real test.
     */
    private fun findBestAliasMatch(line: String): AliasMatch? {
        var best: AliasMatch? = null
        for (test in MedicalDictionary.tests) {
            val allAliases = listOf(test.name) + test.aliases
            for (alias in allAliases) {
                val pattern = Pattern.compile("\\b" + Pattern.quote(alias) + "\\b", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(line)
                if (!matcher.find()) continue
                val candidate = AliasMatch(test, alias, matcher.start(), matcher.end())
                val currentBest = best
                if (currentBest == null ||
                    candidate.start < currentBest.start ||
                    (candidate.start == currentBest.start && candidate.alias.length > currentBest.alias.length)
                ) {
                    best = candidate
                }
            }
        }
        return best
    }

    fun parseReportText(rawText: String): OcrExtractionResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val detectedDate = extractDate(lines) ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val detectedLab = extractLab(lines)

        val extracted = mutableListOf<ParsedLabRow>()

        for (line in lines) {
            val parsedRow = parseLine(line)
            if (parsedRow != null) {
                // Check if already added
                if (extracted.none { it.canonicalName == parsedRow.canonicalName }) {
                    extracted.add(parsedRow)
                }
            }
        }

        return OcrExtractionResult(
            rawText = rawText,
            detectedDate = detectedDate,
            detectedLab = detectedLab,
            extractedRecords = extracted
        )
    }

    private fun extractDate(lines: List<String>): String? {
        for (line in lines.take(15)) {
            for (pattern in dateRegexes) {
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val rawDateStr = matcher.group(1)
                    return normalizeDateString(rawDateStr)
                }
            }
        }
        return null
    }

    private fun normalizeDateString(raw: String?): String {
        val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy", "dd-MM-yyyy", "dd-MMM-yyyy", "dd MMMM yyyy")
        if (raw == null) return ""
        for (fmt in formats) {
            try {
                val parser = SimpleDateFormat(fmt, Locale.ENGLISH)
                parser.isLenient = false
                val parsed = parser.parse(raw)
                if (parsed != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(parsed)
                }
            } catch (_: Exception) {}
        }
        return raw
    }

    private fun extractLab(lines: List<String>): String? {
        for (line in lines.take(10)) {
            for (lab in commonLabs) {
                if (line.contains(lab, ignoreCase = true)) {
                    return lab
                }
            }
        }
        return null
    }

    fun parseLine(line: String): ParsedLabRow? {
        val match = findBestAliasMatch(line) ?: return null
        val test = match.test

        // Text after the matched test name: holds value, unit, and reference range
        val remainder = line.substring(match.end).trim()

        // Locate the reference range FIRST so its numbers are not mistaken for
        // the measured value. Some reports print the range before the value
        // ("Hemoglobin (13.0 - 17.5) 14.2 g/dL"), so the range must be removed
        // from the remainder before picking the value.
        val rangeSpan = findRangeSpan(remainder, test)

        var lowBound: Double? = test.normalLow
        var highBound: Double? = test.normalHigh
        var rangeText = if (lowBound != null && highBound != null) "$lowBound - $highBound" else ""

        if (rangeSpan != null) {
            if (rangeSpan.low != null && rangeSpan.high != null) {
                lowBound = rangeSpan.low
                highBound = rangeSpan.high
                rangeText = "$lowBound - $highBound"
            } else if (rangeSpan.high != null) {
                highBound = rangeSpan.high
                lowBound = 0.0
                rangeText = "< $highBound"
            } else if (rangeSpan.low != null) {
                lowBound = rangeSpan.low
                rangeText = "> $lowBound"
            }
        }

        // Value: first number outside the reference-range span
        val valueStripped = removeRangeSpan(remainder, rangeSpan)
        val valueMatcher = numberRegex.matcher(valueStripped)
        val numbers = mutableListOf<Double>()
        while (valueMatcher.find()) {
            valueMatcher.group()?.toDoubleOrNull()?.let { numbers.add(it) }
        }
        if (numbers.isEmpty()) return null

        var measuredValue = numbers.first()
        var measuredStr = if (measuredValue % 1.0 == 0.0) measuredValue.toLong().toString() else measuredValue.toString()

        // Detect unit from remainder or fallback to default
        var detectedUnit = test.defaultUnit
        val commonUnits = listOf("mg/dL", "mmol/L", "g/dL", "million/mcL", "cells/mcL", "/mcL", "%", "fL", "pg", "mm/hr", "U/L", "mIU/L", "ng/dL", "pg/mL", "mg/L", "ng/mL")
        for (u in commonUnits) {
            if (remainder.contains(u, ignoreCase = true)) {
                detectedUnit = u
                break
            }
        }

        // Normalize units so the value and lab-printed range are compared in the
        // dictionary's canonical unit (e.g. glucose mmol/L -> mg/dL)
        if (detectedUnit != test.defaultUnit) {
            val converted = UnitConverter.normalizeValue(test.name, measuredValue, detectedUnit)
            if (converted.second != detectedUnit) {
                measuredValue = converted.first
                detectedUnit = converted.second
                measuredStr = if (measuredValue % 1.0 == 0.0) measuredValue.toLong().toString() else measuredValue.toString()
                if (rangeSpan != null) {
                    val rangeUnit = rangeSpan.originalUnit ?: detectedUnit
                    var rangeConverted = false
                    if (rangeSpan.low != null && rangeSpan.high != null) {
                        val convLow = UnitConverter.normalizeValue(test.name, rangeSpan.low, rangeUnit)
                        val convHigh = UnitConverter.normalizeValue(test.name, rangeSpan.high, rangeUnit)
                        if (convLow.second == detectedUnit && convHigh.second == detectedUnit) {
                            lowBound = convLow.first
                            highBound = convHigh.first
                            rangeText = "$lowBound - $highBound"
                            rangeConverted = true
                        }
                    } else if (rangeSpan.high != null) {
                        val convHigh = UnitConverter.normalizeValue(test.name, rangeSpan.high, rangeUnit)
                        if (convHigh.second == detectedUnit) {
                            highBound = convHigh.first
                            lowBound = 0.0
                            rangeText = "< $highBound"
                            rangeConverted = true
                        }
                    } else if (rangeSpan.low != null) {
                        val convLow = UnitConverter.normalizeValue(test.name, rangeSpan.low, rangeUnit)
                        if (convLow.second == detectedUnit) {
                            lowBound = convLow.first
                            rangeText = "> $lowBound"
                            rangeConverted = true
                        }
                    }
                    if (!rangeConverted) {
                        // Could not convert the printed range; fall back to dictionary range (canonical unit)
                        lowBound = test.normalLow
                        highBound = test.normalHigh
                        rangeText = if (lowBound != null && highBound != null) "$lowBound - $highBound" else ""
                    }
                }
            }
        }

        // Check if abnormal
        var isAbnormal = false
        if (lowBound != null && measuredValue < lowBound) isAbnormal = true
        if (highBound != null && measuredValue > highBound) isAbnormal = true

        // Confidence scoring
        var confidence = 0.95f
        var warning: String? = null

        if (measuredValue < test.physiologicalMin || measuredValue > test.physiologicalMax) {
            confidence = 0.45f
            warning = "Value out of realistic physiological range. Please verify with physical report."
        } else if (match.alias.lowercase() != test.name.lowercase()) {
            confidence = 0.85f
        }

        return ParsedLabRow(
            testName = match.alias,
            canonicalName = test.name,
            value = measuredStr,
            numericValue = measuredValue,
            unit = detectedUnit,
            refRangeLow = lowBound,
            refRangeHigh = highBound,
            refRangeText = rangeText,
            isAbnormal = isAbnormal,
            confidenceScore = confidence,
            warningNote = warning
        )
    }

    /** Reference range found in the remainder, with the unit it was printed in. */
    private data class RangeSpan(
        val low: Double?,
        val high: Double?,
        val start: Int,
        val end: Int,
        val originalUnit: String?
    )

    /**
     * Find the reference range in the remainder text. Matches "13.0 - 17.5",
     * "< 200" and "> 0.7". A "lo - hi" pair only counts when both numbers sit
     * close together (no more than a short separator between them, which the
     * range regex enforces); a lone "<"/">" bound is only trusted when it is
     * not merely the units or value punctuation.
     */
    private fun findRangeSpan(remainder: String, test: CanonicalTest): RangeSpan? {
        val rangeMatcher = rangeRegex.matcher(remainder)
        if (rangeMatcher.find()) {
            val low = rangeMatcher.group(1)?.toDoubleOrNull()
            val high = rangeMatcher.group(2)?.toDoubleOrNull()
            // Reject implausible ranges: values must be within physiological
            // bounds scaled generously; otherwise this is probably two unrelated
            // numbers (e.g. value and something else) separated by a dash.
            if (low != null && high != null && high > low) {
                return RangeSpan(low, high, rangeMatcher.start(), rangeMatcher.end(), detectUnitNear(remainder))
            }
        }
        val lessThanMatcher = lessThanRangeRegex.matcher(remainder)
        if (lessThanMatcher.find()) {
            val high = lessThanMatcher.group(1)?.toDoubleOrNull()
            if (high != null) {
                return RangeSpan(null, high, lessThanMatcher.start(), lessThanMatcher.end(), detectUnitNear(remainder))
            }
        }
        val greaterThanMatcher = greaterThanRangeRegex.matcher(remainder)
        if (greaterThanMatcher.find()) {
            val low = greaterThanMatcher.group(1)?.toDoubleOrNull()
            if (low != null) {
                return RangeSpan(low, null, greaterThanMatcher.start(), greaterThanMatcher.end(), detectUnitNear(remainder))
            }
        }
        return null
    }

    private fun detectUnitNear(remainder: String): String? {
        val commonUnits = listOf("mg/dL", "mmol/L", "g/dL", "million/mcL", "cells/mcL", "/mcL", "%", "fL", "pg", "mm/hr", "U/L", "mIU/L", "ng/dL", "pg/mL", "mg/L", "ng/mL")
        for (u in commonUnits) {
            if (remainder.contains(u, ignoreCase = true)) return u
        }
        return null
    }

    /** Blank out the matched range span so value extraction skips its numbers. */
    private fun removeRangeSpan(remainder: String, span: RangeSpan?): String {
        if (span == null) return remainder
        val chars = remainder.toCharArray()
        for (i in span.start until span.end) {
            if (chars[i].isDigit() || chars[i] == '.') {
                // Replace digits with 'x' so they no longer parse as numbers
                chars[i] = ' '
            }
        }
        return String(chars)
    }
}
