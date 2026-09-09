package com.medtrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.medtrack.app.data.local.dao.LabReportDao
import com.medtrack.app.data.local.dao.MedicineDao
import com.medtrack.app.data.local.dao.ProfileDao
import com.medtrack.app.data.local.dao.TestRecordDao
import com.medtrack.app.data.local.entity.LabReportEntity
import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.data.local.entity.TestRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProfileEntity::class,
        LabReportEntity::class,
        TestRecordEntity::class,
        MedicineEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MedTrackDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun labReportDao(): LabReportDao
    abstract fun testRecordDao(): TestRecordDao
    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: MedTrackDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MedTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedTrackDatabase::class.java,
                    "medtrack_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: MedTrackDatabase) {
            val existing = database.profileDao().getDefaultProfileSync()
            if (existing != null) return

            val defaultProfile = ProfileEntity(
                name = "Sajib Ahmed",
                relationship = "Self",
                dob = "1994-08-12",
                gender = "Male",
                bloodGroup = "B+",
                emergencyPhone = "+880 1711-223344",
                allergies = "Penicillin, Sulfa drugs",
                chronicConditions = "Mild Asthma",
                isDefault = true
            )
            val profileId = database.profileDao().insertProfile(defaultProfile)

            // Seed sample lab reports with longitudinal progression
            val report1Id = database.labReportDao().insertReport(
                LabReportEntity(
                    profileId = profileId,
                    title = "Annual Health Screening - Baseline",
                    testDate = "2026-01-15",
                    labName = "Popular Diagnostic Centre",
                    notes = "Routine yearly physical exam."
                )
            )

            database.testRecordDao().insertRecords(
                listOf(
                    TestRecordEntity(
                        reportId = report1Id,
                        profileId = profileId,
                        testName = "Hemoglobin",
                        canonicalName = "Hemoglobin",
                        value = "14.2",
                        numericValue = 14.2,
                        unit = "g/dL",
                        refRangeLow = 13.0,
                        refRangeHigh = 17.5,
                        refRangeText = "13.0 - 17.5",
                        isAbnormal = false,
                        testDate = "2026-01-15"
                    ),
                    TestRecordEntity(
                        reportId = report1Id,
                        profileId = profileId,
                        testName = "Fasting Blood Glucose",
                        canonicalName = "Fasting Blood Glucose",
                        value = "98",
                        numericValue = 98.0,
                        unit = "mg/dL",
                        refRangeLow = 70.0,
                        refRangeHigh = 99.0,
                        refRangeText = "70 - 99",
                        isAbnormal = false,
                        testDate = "2026-01-15"
                    ),
                    TestRecordEntity(
                        reportId = report1Id,
                        profileId = profileId,
                        testName = "Serum Creatinine",
                        canonicalName = "Serum Creatinine",
                        value = "0.95",
                        numericValue = 0.95,
                        unit = "mg/dL",
                        refRangeLow = 0.7,
                        refRangeHigh = 1.3,
                        refRangeText = "0.7 - 1.3",
                        isAbnormal = false,
                        testDate = "2026-01-15"
                    ),
                    TestRecordEntity(
                        reportId = report1Id,
                        profileId = profileId,
                        testName = "Platelet Count",
                        canonicalName = "Platelet Count",
                        value = "240000",
                        numericValue = 240000.0,
                        unit = "/mcL",
                        refRangeLow = 150000.0,
                        refRangeHigh = 450000.0,
                        refRangeText = "150000 - 450000",
                        isAbnormal = false,
                        testDate = "2026-01-15"
                    ),
                    TestRecordEntity(
                        reportId = report1Id,
                        profileId = profileId,
                        testName = "Total Cholesterol",
                        canonicalName = "Total Cholesterol",
                        value = "215",
                        numericValue = 215.0,
                        unit = "mg/dL",
                        refRangeLow = 125.0,
                        refRangeHigh = 200.0,
                        refRangeText = "< 200",
                        isAbnormal = true,
                        testDate = "2026-01-15"
                    )
                )
            )

            val report2Id = database.labReportDao().insertReport(
                LabReportEntity(
                    profileId = profileId,
                    title = "Lipid & Glucose Follow-up",
                    testDate = "2026-05-20",
                    labName = "Ibn Sina Diagnostic",
                    notes = "Follow-up after diet changes."
                )
            )

            database.testRecordDao().insertRecords(
                listOf(
                    TestRecordEntity(
                        reportId = report2Id,
                        profileId = profileId,
                        testName = "Fasting Blood Glucose",
                        canonicalName = "Fasting Blood Glucose",
                        value = "92",
                        numericValue = 92.0,
                        unit = "mg/dL",
                        refRangeLow = 70.0,
                        refRangeHigh = 99.0,
                        refRangeText = "70 - 99",
                        isAbnormal = false,
                        testDate = "2026-05-20"
                    ),
                    TestRecordEntity(
                        reportId = report2Id,
                        profileId = profileId,
                        testName = "Total Cholesterol",
                        canonicalName = "Total Cholesterol",
                        value = "192",
                        numericValue = 192.0,
                        unit = "mg/dL",
                        refRangeLow = 125.0,
                        refRangeHigh = 200.0,
                        refRangeText = "< 200",
                        isAbnormal = false,
                        testDate = "2026-05-20"
                    ),
                    TestRecordEntity(
                        reportId = report2Id,
                        profileId = profileId,
                        testName = "Serum Creatinine",
                        canonicalName = "Serum Creatinine",
                        value = "0.92",
                        numericValue = 0.92,
                        unit = "mg/dL",
                        refRangeLow = 0.7,
                        refRangeHigh = 1.3,
                        refRangeText = "0.7 - 1.3",
                        isAbnormal = false,
                        testDate = "2026-05-20"
                    ),
                    TestRecordEntity(
                        reportId = report2Id,
                        profileId = profileId,
                        testName = "Hemoglobin",
                        canonicalName = "Hemoglobin",
                        value = "14.5",
                        numericValue = 14.5,
                        unit = "g/dL",
                        refRangeLow = 13.0,
                        refRangeHigh = 17.5,
                        refRangeText = "13.0 - 17.5",
                        isAbnormal = false,
                        testDate = "2026-05-20"
                    )
                )
            )

            val report3Id = database.labReportDao().insertReport(
                LabReportEntity(
                    profileId = profileId,
                    title = "Comprehensive Metabolic Panel",
                    testDate = "2026-08-30",
                    labName = "Square Hospital Lab",
                    notes = "Pre-travel health clearance check."
                )
            )

            database.testRecordDao().insertRecords(
                listOf(
                    TestRecordEntity(
                        reportId = report3Id,
                        profileId = profileId,
                        testName = "Fasting Blood Glucose",
                        canonicalName = "Fasting Blood Glucose",
                        value = "104",
                        numericValue = 104.0,
                        unit = "mg/dL",
                        refRangeLow = 70.0,
                        refRangeHigh = 99.0,
                        refRangeText = "70 - 99",
                        isAbnormal = true,
                        testDate = "2026-08-30"
                    ),
                    TestRecordEntity(
                        reportId = report3Id,
                        profileId = profileId,
                        testName = "Serum Creatinine",
                        canonicalName = "Serum Creatinine",
                        value = "0.89",
                        numericValue = 0.89,
                        unit = "mg/dL",
                        refRangeLow = 0.7,
                        refRangeHigh = 1.3,
                        refRangeText = "0.7 - 1.3",
                        isAbnormal = false,
                        testDate = "2026-08-30"
                    ),
                    TestRecordEntity(
                        reportId = report3Id,
                        profileId = profileId,
                        testName = "Total Cholesterol",
                        canonicalName = "Total Cholesterol",
                        value = "185",
                        numericValue = 185.0,
                        unit = "mg/dL",
                        refRangeLow = 125.0,
                        refRangeHigh = 200.0,
                        refRangeText = "< 200",
                        isAbnormal = false,
                        testDate = "2026-08-30"
                    ),
                    TestRecordEntity(
                        reportId = report3Id,
                        profileId = profileId,
                        testName = "Platelet Count",
                        canonicalName = "Platelet Count",
                        value = "255000",
                        numericValue = 255000.0,
                        unit = "/mcL",
                        refRangeLow = 150000.0,
                        refRangeHigh = 450000.0,
                        refRangeText = "150000 - 450000",
                        isAbnormal = false,
                        testDate = "2026-08-30"
                    ),
                    TestRecordEntity(
                        reportId = report3Id,
                        profileId = profileId,
                        testName = "Hemoglobin",
                        canonicalName = "Hemoglobin",
                        value = "14.8",
                        numericValue = 14.8,
                        unit = "g/dL",
                        refRangeLow = 13.0,
                        refRangeHigh = 17.5,
                        refRangeText = "13.0 - 17.5",
                        isAbnormal = false,
                        testDate = "2026-08-30"
                    )
                )
            )

            // Seed medicines
            database.medicineDao().insertMedicine(
                MedicineEntity(
                    profileId = profileId,
                    name = "Montelukast",
                    dosage = "10 mg",
                    frequency = "Once daily at night",
                    takeMorning = false,
                    takeAfternoon = false,
                    takeEvening = false,
                    takeNight = true,
                    nightTime = "21:30",
                    mealTiming = "Before Bed",
                    notes = "For seasonal asthma prevention.",
                    isActive = true
                )
            )

            database.medicineDao().insertMedicine(
                MedicineEntity(
                    profileId = profileId,
                    name = "Vitamin D3 (Cholecalciferol)",
                    dosage = "2000 IU",
                    frequency = "Daily",
                    takeMorning = true,
                    takeAfternoon = false,
                    takeEvening = false,
                    takeNight = false,
                    morningTime = "09:00",
                    mealTiming = "After Meal",
                    notes = "Take with breakfast.",
                    isActive = true
                )
            )
        }
    }
}
