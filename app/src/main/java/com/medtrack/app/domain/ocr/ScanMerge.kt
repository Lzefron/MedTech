package com.medtrack.app.domain.ocr

/** Fill gaps only. Conflicting readings remain visible for human review, never silently overwrite edits. */
object ScanMerge {
    fun merge(local: List<ParsedLabRow>, remote: List<ParsedLabRow>): List<ParsedLabRow> {
        val result = local.toMutableList()
        remote.forEach { incoming ->
            val matching = result.indices.filter { result[it].canonicalName.equals(incoming.canonicalName, true) }
            val uniqueRemote = remote.count { it.canonicalName.equals(incoming.canonicalName, true) } == 1
            if (matching.size != 1 || !uniqueRemote) { result.add(incoming); return@forEach }
            val index = matching.single(); val old = result[index]
            val conflict = (old.value.isNotBlank() && old.value != incoming.value && incoming.value.isNotBlank()) ||
                (old.unit.isNotBlank() && incoming.unit.isNotBlank() && old.unit != incoming.unit) || old.isNumeric != incoming.isNumeric ||
                (old.refRangeText.isNotBlank() && incoming.refRangeText.isNotBlank() && old.refRangeText != incoming.refRangeText)
            if (conflict) {
                result[index] = old.copy(warningNote = "Readings differ. Kept your current fields. AI suggested: ${incoming.value} ${incoming.unit}; range ${incoming.refRangeText}. Compare the photo and edit if needed.")
            } else {
                val fillValue = old.value.isBlank()
                result[index] = old.copy(value = if (fillValue) incoming.value else old.value,
                    numericValue = if (fillValue) incoming.numericValue else old.numericValue,
                    unit = old.unit.ifBlank { incoming.unit }, refRangeText = old.refRangeText.ifBlank { incoming.refRangeText },
                    warningNote = incoming.warningNote, referenceSource = if (old.refRangeText.isBlank()) incoming.referenceSource else old.referenceSource)
            }
        }
        return result
    }
}
