package com.medtrack.app.data.model

import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.data.local.entity.TestRecordEntity
import org.junit.Assert.*
import org.junit.Test

class EmergencySummaryTest {

    @Test
    fun testEmergencyQrPayload() {
        val profile = ProfileEntity(
            id = 1,
            name = "John Doe",
            relationship = "Self",
            dob = "1985-05-20",
            gender = "Male",
            bloodGroup = "B+",
            emergencyPhone = "01700000000",
            allergies = "Penicillin",
            chronicConditions = "Asthma"
        )
        val medicines = listOf(
            MedicineEntity(id = 1, profileId = 1, name = "Montelukast", dosage = "10 mg")
        )
        val abnormalLabs = listOf(
            TestRecordEntity(
                id = 1,
                reportId = 1,
                profileId = 1,
                testName = "Total Cholesterol",
                canonicalName = "Total Cholesterol",
                value = "215",
                unit = "mg/dL",
                isAbnormal = true,
                testDate = "2026-08-15"
            )
        )

        val summary = EmergencySummary(profile, medicines, abnormalLabs)
        val payload = summary.toQrPayload()

        assertTrue(payload.contains("MEDTRACK|EMERGENCY|v1"))
        assertTrue(payload.contains("Blood: B+"))
        assertTrue(payload.contains("Emergency Call: 01700000000"))
        assertTrue(payload.contains("Montelukast 10 mg"))
        assertTrue(payload.contains("Total Cholesterol: 215 mg/dL"))
    }
}
