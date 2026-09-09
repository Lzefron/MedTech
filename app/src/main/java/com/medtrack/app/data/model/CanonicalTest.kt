package com.medtrack.app.data.model

data class CanonicalTest(
    val name: String,
    val category: String,
    val defaultUnit: String,
    val normalLow: Double?,
    val normalHigh: Double?,
    val aliases: List<String> = emptyList(),
    val physiologicalMin: Double = 0.0,
    val physiologicalMax: Double = 1_000_000.0,
    val description: String = ""
)
