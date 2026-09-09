package com.medtrack.app.data.repository

import com.medtrack.app.data.local.dao.LabReportDao
import com.medtrack.app.data.local.dao.TestRecordDao
import com.medtrack.app.data.local.entity.LabReportEntity
import com.medtrack.app.data.local.entity.TestRecordEntity
import kotlinx.coroutines.flow.Flow

class LabReportRepository(
    private val labReportDao: LabReportDao,
    private val testRecordDao: TestRecordDao
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
    ): Long {
        val reportId = labReportDao.insertReport(report)
        val recordsWithReportId = records.map {
            it.copy(reportId = reportId, profileId = report.profileId, testDate = report.testDate)
        }
        testRecordDao.insertRecords(recordsWithReportId)
        return reportId
    }

    suspend fun deleteReport(report: LabReportEntity) {
        labReportDao.deleteReport(report)
    }

    suspend fun updateTestRecord(record: TestRecordEntity) {
        testRecordDao.updateRecord(record)
    }
}
