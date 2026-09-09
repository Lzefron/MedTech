package com.medtrack.app.domain.ocr

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

    private val commonLabs = listOf(
        "Popular", "Ibn Sina", "Square", "LabAid", "United Hospital",
        "Apollo", "Evercare", "Praava", "BIRDEM", "BSMMU",
        "Quest Diagnostics", "LabCorp", "Metropolis", "Thyrocare"
    )

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
        // Find if line matches any canonical test
        for (test in MedicalDictionary.tests) {
            val allAliases = listOf(test.name) + test.aliases
            val matchAlias = allAliases.firstOrNull { alias ->
                val p = Pattern.compile("\\b" + Pattern.quote(alias) + "\\b", Pattern.CASE_INSENSITIVE)
                p.matcher(line).find()
            }

            if (matchAlias != null) {
                // Line matched this test! Now extract the numeric value, unit, and reference range
                val remainder = line.substring(line.indexOf(matchAlias, ignoreCase = true) + matchAlias.length).trim()
                
                // Extract numbers in remainder
                val numMatcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(remainder)
                val numbers = mutableListOf<Double>()
                while (numMatcher.find()) {
                    val n = numMatcher.group(1)?.toDoubleOrNull()
                    if (n != null) numbers.add(n)
                }

                if (numbers.isEmpty()) continue

                val measuredValue = numbers.first()
                val measuredStr = if (measuredValue % 1.0 == 0.0) measuredValue.toLong().toString() else measuredValue.toString()

                // Check for reference range in the remainder
                var lowBound: Double? = test.normalLow
                var highBound: Double? = test.normalHigh
                var rangeText = if (lowBound != null && highBound != null) "$lowBound - $highBound" else ""

                val rangeMatcher = rangeRegex.matcher(remainder)
                if (rangeMatcher.find()) {
                    val low = rangeMatcher.group(1)?.toDoubleOrNull()
                    val high = rangeMatcher.group(2)?.toDoubleOrNull()
                    if (low != null && high != null) {
                        lowBound = low
                        highBound = high
                        rangeText = "$low - $high"
                    }
                } else {
                    val lessThanMatcher = lessThanRangeRegex.matcher(remainder)
                    if (lessThanMatcher.find()) {
                        val high = lessThanMatcher.group(1)?.toDoubleOrNull()
                        if (high != null) {
                            highBound = high
                            lowBound = 0.0
                            rangeText = "< $high"
                        }
                    }
                }

                // Check if abnormal
                var isAbnormal = false
                if (lowBound != null && measuredValue < lowBound) isAbnormal = true
                if (highBound != null && measuredValue > highBound) isAbnormal = true

                // Detect unit from remainder or fallback to default
                var detectedUnit = test.defaultUnit
                val commonUnits = listOf("mg/dL", "mmol/L", "g/dL", "million/mcL", "cells/mcL", "/mcL", "%", "fL", "pg", "mm/hr", "U/L", "mIU/L", "ng/dL", "pg/mL", "mg/L", "ng/mL")
                for (u in commonUnits) {
                    if (remainder.contains(u, ignoreCase = true)) {
                        detectedUnit = u
                        break
                    }
                }

                // Confidence scoring
                var confidence = 0.95f
                var warning: String? = null

                if (measuredValue < test.physiologicalMin || measuredValue > test.physiologicalMax) {
                    confidence = 0.45f
                    warning = "Value out of realistic physiological range. Please verify with physical report."
                } else if (matchAlias.lowercase() != test.name.lowercase()) {
                    confidence = 0.85f
                }

                return ParsedLabRow(
                    testName = matchAlias,
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
        }
        return null
    }
}
