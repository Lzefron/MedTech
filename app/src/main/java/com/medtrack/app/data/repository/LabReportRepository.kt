package com.medtrack.app.data.repository

import com.medtrack.app.data.local.dao.LabReportDao
import com.medtrack.app.data.local.dao.TestRecordDao
import com.medtrack.app.data.local.entity.LabReportEntity
import com.medtrack.app.data.local.entity.TestRecordEntity
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction
import com.medtrack.app.data.local.MedTrackDatabase
import android.content.Context
import com.medtrack.app.domain.ocr.DocumentImages

class LabReportRepository(
    private val labReportDao: LabReportDao,
    private val testRecordDao: TestRecordDao,
    private val database: MedTrackDatabase,
    private val context: Context
) {
    fun getReportsForProfile(profileId: Long): Flow<List<LabReportEntity>> =
        labReportDao.getReportsForProfile(profileId)

    fun getReportById(id: Long): Flow<LabReportEntity?> =
        labReportDao.getReportById(id)

    suspend fun getReportByIdSync(id: Long): LabReportEntity? =
        labReportDao.getReportByIdSync(id)

    fun getRecordsForReport(reportId: Long): Flow<List<TestRecordEntity>> =
        testRecordDao.getRecordsForReport(reportId)

    suspend fun getRecordsForReportSync(reportId: Long): List<TestRecordEntity> =
        testRecordDao.getRecordsForReportSync(reportId)

    fun getRecordsForMetric(profileId: Long, canonicalName: String): Flow<List<TestRecordEntity>> =
        testRecordDao.getRecordsForMetric(profileId, canonicalName)

    fun getAllDistinctCanonicalNames(profileId: Long): Flow<List<String>> =
        testRecordDao.getAllDistinctCanonicalNames(profileId)

    fun getRecentAbnormalRecords(profileId: Long, limit: Int = 10): Flow<List<TestRecordEntity>> =
        testRecordDao.getRecentAbnormalRecords(profileId, limit)

    fun getAllRecordsForProfile(profileId: Long): Flow<List<TestRecordEntity>> =
        testRecordDao.getAllRecordsForProfile(profileId)

    suspend fun getAllRecordsForProfileSync(profileId: Long): List<TestRecordEntity> =
        testRecordDao.getAllRecordsForProfileSync(profileId)

    suspend fun saveReportWithRecords(
        report: LabReportEntity,
        records: List<TestRecordEntity>
    ): Long = database.withTransaction {
        require(report.profileId > 0)
        require(com.medtrack.app.domain.validation.HealthInput.dateError(report.testDate) == null)
        require(records.isNotEmpty())
        require(records.all { it.canonicalName.isNotBlank() && it.value.isNotBlank() &&
            (it.numericValue == null || (it.numericValue.isFinite() && it.numericValue >= 0 && it.unit.isNotBlank())) })
        val reportId = labReportDao.insertReport(report)
        val recordsWithReportId = records.map {
            it.copy(reportId = reportId, profileId = report.profileId, testDate = report.testDate)
        }
        testRecordDao.insertRecords(recordsWithReportId)
        reportId
    }

    suspend fun deleteReport(report: LabReportEntity) {
        labReportDao.deleteReport(report)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { DocumentImages.deletePrivate(context, report.imagePath) }
    }

    suspend fun updateTestRecord(record: TestRecordEntity) {
        testRecordDao.updateRecord(record)
    }
}
