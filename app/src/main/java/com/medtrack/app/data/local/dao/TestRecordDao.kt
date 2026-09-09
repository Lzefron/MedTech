package com.medtrack.app.data.local.dao

import androidx.room.*
import com.medtrack.app.data.local.entity.TestRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestRecordDao {
    @Query("SELECT * FROM test_records WHERE reportId = :reportId ORDER BY id ASC")
    fun getRecordsForReport(reportId: Long): Flow<List<TestRecordEntity>>

    @Query("SELECT * FROM test_records WHERE reportId = :reportId ORDER BY id ASC")
    suspend fun getRecordsForReportSync(reportId: Long): List<TestRecordEntity>

    @Query("SELECT * FROM test_records WHERE profileId = :profileId AND canonicalName = :canonicalName ORDER BY testDate ASC")
    fun getRecordsForMetric(profileId: Long, canonicalName: String): Flow<List<TestRecordEntity>>

    @Query("SELECT DISTINCT canonicalName FROM test_records WHERE profileId = :profileId ORDER BY canonicalName ASC")
    fun getAllDistinctCanonicalNames(profileId: Long): Flow<List<String>>

    @Query("SELECT * FROM test_records WHERE profileId = :profileId AND isAbnormal = 1 ORDER BY testDate DESC LIMIT :limit")
    fun getRecentAbnormalRecords(profileId: Long, limit: Int = 10): Flow<List<TestRecordEntity>>

    @Query("SELECT * FROM test_records WHERE profileId = :profileId ORDER BY testDate DESC")
    fun getAllRecordsForProfile(profileId: Long): Flow<List<TestRecordEntity>>

    @Query("SELECT * FROM test_records WHERE profileId = :profileId ORDER BY testDate DESC")
    suspend fun getAllRecordsForProfileSync(profileId: Long): List<TestRecordEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: TestRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecords(records: List<TestRecordEntity>)

    @Update
    suspend fun updateRecord(record: TestRecordEntity)

    @Delete
    suspend fun deleteRecord(record: TestRecordEntity)

    @Query("DELETE FROM test_records WHERE reportId = :reportId")
    suspend fun deleteRecordsForReport(reportId: Long): Int
}
