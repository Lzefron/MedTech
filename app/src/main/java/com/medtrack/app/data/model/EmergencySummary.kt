package com.medtrack.app.data.model

import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.data.local.entity.TestRecordEntity

data class EmergencySummary(
    val profile: ProfileEntity,
    val activeMedicines: List<MedicineEntity>,
    val abnormalLabs: List<TestRecordEntity>
) {
    fun toQrPayload(): String {
        val sb = StringBuilder()
        sb.append("MEDTRACK|EMERGENCY|v1\n")
        sb.append("Patient: ${profile.name} (${profile.relationship})\n")
        sb.append("Blood: ${profile.bloodGroup} | Gender: ${profile.gender} | DOB: ${profile.dob}\n")
        sb.append("Emergency Call: ${profile.emergencyPhone}\n")
        sb.append("Allergies: ${profile.allergies}\n")
        sb.append("Chronic Conditions: ${profile.chronicConditions}\n")
        
        if (activeMedicines.isNotEmpty()) {
            sb.append("Active Meds: ")
            sb.append(activeMedicines.joinToString(", ") { "${it.name} ${it.dosage}" })
            sb.append("\n")
        }
        
        if (abnormalLabs.isNotEmpty()) {
            sb.append("Flagged Labs: ")
            sb.append(abnormalLabs.take(5).joinToString("; ") { "${it.canonicalName}: ${it.value} ${it.unit} (${it.testDate})" })
            sb.append("\n")
        }
        
        return sb.toString()
    }
}
