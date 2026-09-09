package com.medtrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medicines",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val dosage: String,
    val frequency: String = "Daily",
    val takeMorning: Boolean = true,
    val takeAfternoon: Boolean = false,
    val takeEvening: Boolean = true,
    val takeNight: Boolean = false,
    val morningTime: String = "08:00",
    val afternoonTime: String = "13:00",
    val eveningTime: String = "18:00",
    val nightTime: String = "22:00",
    val mealTiming: String = "After Meal",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
