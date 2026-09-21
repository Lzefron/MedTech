package com.medtrack.app.domain.validation

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HealthInputTest {
    @Test fun rejectsInvalidNumbersWithoutCoercion() {
        listOf("", "abc", "12abc", "NaN", "Infinity", "1e309", "-4", "12,34", "4.2.3", "1 000").forEach { assertNull(it, HealthInput.number(it)) }
    }
    @Test fun acceptsDecimalsGroupedAndBanglaDigits() {
        assertEquals(250000.0, HealthInput.number("250,000")!!, 0.0)
        assertEquals(14.2, HealthInput.number(" 14.2 ")!!, 0.0)
        assertEquals(14.2, HealthInput.number("১৪.২")!!, 0.0)
        assertEquals(0.5, HealthInput.number(".5")!!, 0.0)
        assertEquals(0.0, HealthInput.number("0")!!, 0.0)
    }
    @Test fun validatesRealDatesAndFutureDates() {
        assertNotNull(HealthInput.dateError("2025-02-29"))
        assertNotNull(HealthInput.dateError("2024-02-30"))
        assertNull(HealthInput.dateError("2024-02-29"))
        assertNotNull(HealthInput.dateError(LocalDate.now().plusDays(1).toString()))
        assertNull(HealthInput.dateError("", optional = true))
        assertNotNull(HealthInput.dateError(""))
        assertNotNull(HealthInput.dateError("2024-1-1"))
    }
    @Test fun rangesDoNotInventBounds() {
        assertEquals(HealthInput.Range(null, null), HealthInput.range(""))
        assertNull(HealthInput.range("10-4"))
        assertNull(HealthInput.range("normal"))
        assertNull(HealthInput.range("NaN-8"))
        assertTrue(HealthInput.range("< 200")!!.outside(200.0))
        assertFalse(HealthInput.range("<= 200")!!.outside(200.0))
        assertTrue(HealthInput.range("> 40")!!.outside(40.0))
        assertFalse(HealthInput.range("4–10")!!.outside(7.0))
    }
    @Test fun phoneIsOptionalButValidatedWhenEntered() {
        assertNull(HealthInput.phoneError(""))
        assertNull(HealthInput.phoneError("+880 1700-000000"))
        assertNotNull(HealthInput.phoneError("call me"))
        assertNotNull(HealthInput.phoneError("123"))
    }
}
