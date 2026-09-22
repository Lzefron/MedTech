package com.medtrack.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medtrack.app.data.local.MedTrackDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MedTrackDatabase::class.java)
    @Test fun existingRecordsSurviveSourceColumnMigration() {
        helper.createDatabase("migration-reference-test", 1).apply {
            execSQL("INSERT INTO profiles VALUES (1, 'Migration person', 'Self', '', 'Not specified', 'Unknown', '', '', '', 1, 0)")
            execSQL("INSERT INTO lab_reports (id,profileId,title,testDate,labName,createdAt) VALUES (1,1,'Original report','2024-01-01','Lab',0)")
            execSQL("INSERT INTO test_records (id,reportId,profileId,testName,canonicalName,value,numericValue,unit,refRangeLow,refRangeHigh,refRangeText,isAbnormal,confidenceScore,testDate,createdAt) VALUES (1,1,1,'Hb','Hemoglobin','14',14,'g/dL',12,16,'12-16',0,1,'2024-01-01',0)")
            close()
        }
        helper.runMigrationsAndValidate("migration-reference-test", 2, true, MedTrackDatabase.MIGRATION_1_2).apply {
            query("SELECT value, refRangeText, referenceSource FROM test_records WHERE id = 1").use {
                assertTrue(it.moveToFirst()); assertEquals("14", it.getString(0)); assertEquals("12-16", it.getString(1)); assertTrue(it.isNull(2))
            }
            close()
        }
    }
}
