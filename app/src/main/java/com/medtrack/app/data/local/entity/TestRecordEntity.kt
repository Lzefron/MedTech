package com.medtrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_records",
    foreignKeys = [
        ForeignKey(
            entity = LabReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reportId"), Index("profileId"), Index("canonicalName")]
)
data class TestRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportId: Long,
    val profileId: Long,
    val testName: String,
    val canonicalName: String,
    val value: String,
    val numericValue: Double? = null,
    val unit: String = "",
    val refRangeLow: Double? = null,
    val refRangeHigh: Double? = null,
    val refRangeText: String? = null,
    val isAbnormal: Boolean = false,
    val confidenceScore: Float = 1.0f,
    val testDate: String,
    val createdAt: Long = System.currentTimeMillis()
)
