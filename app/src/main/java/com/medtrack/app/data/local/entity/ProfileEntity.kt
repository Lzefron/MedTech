package com.medtrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String = "Self",
    val dob: String = "",
    val gender: String = "Male",
    val bloodGroup: String = "O+",
    val emergencyPhone: String = "",
    val allergies: String = "None",
    val chronicConditions: String = "None",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
