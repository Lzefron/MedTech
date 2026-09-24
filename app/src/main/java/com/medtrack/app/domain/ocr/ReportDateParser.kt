package com.medtrack.app.domain.ocr

import com.medtrack.app.domain.validation.HealthInput
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

object ReportDateParser {
    data class Detection(val date: String?, val explanation: String)
    private val labels = Regex("(?i)\\b(collection(?:\\s+date)?|collected(?:\\s+on)?|sample(?:\\s+date)?|test\\s+date|report(?:ing)?(?:\\s+date)?|reported(?:\\s+on)?|date|dob|date\\s+of\\s+birth|birth)\\s*[:=-]?")
    private val datePattern = Regex("\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4}|\\d{1,2}[- /]+[A-Za-z]{3,9}[- ,/]+\\d{4}|[A-Za-z]{3,9} +\\d{1,2},? +\\d{4}")
    fun detect(text: String): Detection {
        val clean = text.map { if (it in '০'..'৯') '0' + (it - '০') else it }.joinToString("")
        val matches = labels.findAll(clean).toList()
        val candidates = matches.mapIndexedNotNull { index, match ->
            val label = match.groupValues[1].lowercase()
            if (label.contains("birth") || label == "dob") return@mapIndexedNotNull null
            val end = minOf(matches.getOrNull(index + 1)?.range?.first ?: clean.length, match.range.last + 85)
            val raw = datePattern.find(clean.substring(match.range.last + 1, end))?.value ?: return@mapIndexedNotNull null
            val date = parse(raw) ?: return@mapIndexedNotNull null
            val priority = if (label.startsWith("collect") || label.startsWith("sample") || label.startsWith("test")) 0 else if (label.startsWith("report")) 1 else 2
            Triple(priority, date, label)
        }
        val best = candidates.minOfOrNull { it.first } ?: return Detection(null, "Date not read confidently. Enter the test or collection date from the report.")
        val dates = candidates.filter { it.first == best }.map { it.second }.distinct()
        return if (dates.size == 1) Detection(dates.single(), "Filled from the photo. Check the test/collection date; numeric dates use day-month-year.")
        else Detection(null, "Several possible test dates were found: ${dates.joinToString()}. Choose the date on your report.")
    }
    private fun parse(raw: String): String? {
        val normalized = raw.replace(Regex(" +"), " ")
        val formats = listOf("uuuu-M-d", "uuuu/M/d", "uuuu.M.d", "d/M/uuuu", "d-M-uuuu", "d.M.uuuu", "d MMM uuuu", "d MMMM uuuu", "d-MMM-uuuu", "d-MMMM-uuuu", "d/MMM/uuuu", "MMM d, uuuu", "MMMM d, uuuu", "MMM d uuuu")
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching { LocalDate.parse(normalized, DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)).toString() }
                .getOrNull()?.takeIf { HealthInput.dateError(it) == null }
        }
    }
}
