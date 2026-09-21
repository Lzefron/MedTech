package com.medtrack.app.domain.validation

import java.time.LocalDate
import java.time.format.DateTimeParseException

object HealthInput {
    fun number(raw: String): Double? {
        val text = raw.trim().map { if (it in '০'..'৯') '0' + (it - '০') else it }.joinToString("")
        if (!Regex("""(?:\d+|\d{1,3}(?:,\d{3})+)(?:\.\d+)?|\.\d+""").matches(text)) return null
        return text.replace(",", "").toDoubleOrNull()?.takeIf { it.isFinite() && it <= 1e12 }
    }
    fun dateError(raw: String, optional: Boolean = false): String? {
        if (raw.isBlank()) return if (optional) null else "Enter the date printed on the report."
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(raw.trim())) return "Use YYYY-MM-DD."
        val date = try { LocalDate.parse(raw.trim()) } catch (_: DateTimeParseException) { return "Enter a real calendar date." }
        if (date.isAfter(LocalDate.now())) return "Date cannot be in the future."
        if (date.year < 1900) return "Enter a date from 1900 onwards."
        return null
    }
    fun phoneError(raw: String): String? {
        if (raw.isBlank()) return null
        if (!Regex("""\+?[\d ()-]+""").matches(raw.trim()) || raw.count { it.isDigit() } !in 7..15)
            return "Use 7–15 digits, optionally with a country code."
        return null
    }
    data class Range(val low: Double?, val high: Double?, val lowExclusive: Boolean = false, val highExclusive: Boolean = false) {
        fun outside(value: Double): Boolean =
            (low?.let { if (lowExclusive) value <= it else value < it } ?: false) ||
            (high?.let { if (highExclusive) value >= it else value > it } ?: false)
    }
    fun range(raw: String): Range? {
        val text = raw.trim()
        if (text.isEmpty()) return Range(null, null)
        val pair = Regex("""^([\d.,]+)\s*(?:-|–|—|to)\s*([\d.,]+)$""", RegexOption.IGNORE_CASE).matchEntire(text)
        if (pair != null) {
            val low = number(pair.groupValues[1]) ?: return null
            val high = number(pair.groupValues[2]) ?: return null
            return if (low < high) Range(low, high) else null
        }
        val bound = Regex("""^(<=|>=|<|>|≤|≥)\s*([\d.,]+)$""").matchEntire(text) ?: return null
        val value = number(bound.groupValues[2]) ?: return null
        return when (bound.groupValues[1]) {
            "<" -> Range(null, value, highExclusive = true)
            "<=", "≤" -> Range(null, value)
            ">" -> Range(value, null, lowExclusive = true)
            else -> Range(value, null)
        }
    }
}
