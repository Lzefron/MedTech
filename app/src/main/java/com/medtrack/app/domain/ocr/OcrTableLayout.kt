package com.medtrack.app.domain.ocr

import kotlin.math.abs
import kotlin.math.min

data class PositionedText(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Reassemble table cells split into separate ML Kit blocks. Never join vertically adjacent rows. */
object OcrTableLayout {
    fun rows(fragments: List<PositionedText>): List<String> {
        val rows = mutableListOf<MutableList<PositionedText>>()
        for (fragment in fragments.sortedWith(compareBy({ it.top }, { it.left }))) {
            val height = (fragment.bottom - fragment.top).coerceAtLeast(1)
            val row = rows.lastOrNull()?.takeIf { existing ->
                val anchor = existing.first()
                val otherHeight = (anchor.bottom - anchor.top).coerceAtLeast(1)
                val centerDistance = abs((fragment.top + fragment.bottom) - (anchor.top + anchor.bottom)) / 2.0
                centerDistance <= min(height, otherHeight) * 0.35 &&
                    existing.none { it.left < fragment.right && fragment.left < it.right }
            }
            if (row == null) rows.add(mutableListOf(fragment)) else row.add(fragment)
        }
        return rows.map { row -> row.sortedBy { it.left }.joinToString("  ") { it.text } }
    }
}
