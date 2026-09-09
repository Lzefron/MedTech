package com.medtrack.app.data.local.dao

import androidx.room.*
import com.medtrack.app.data.local.entity.LabReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabReportDao {
    @Query("SELECT * FROM lab_reports WHERE profileId = :profileId ORDER BY testDate DESC, createdAt DESC")
    fun getReportsForProfile(profileId: Long): Flow<List<LabReportEntity>>

    @Query("SELECT * FROM lab_reports WHERE id = :id LIMIT 1")
    fun getReportById(id: Long): Flow<LabReportEntity?>

    @Query("SELECT * FROM lab_reports WHERE id = :id LIMIT 1")
    suspend fun getReportByIdSync(id: Long): LabReportEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReport(report: LabReportEntity): Long

    @Update
    suspend fun updateReport(report: LabReportEntity)

    @Delete
    suspend fun deleteReport(report: LabReportEntity)
}
