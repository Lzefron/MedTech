package com.medtrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lab_reports",
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
data class LabReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val title: String,
    val testDate: String,
    val labName: String = "",
    val imagePath: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
