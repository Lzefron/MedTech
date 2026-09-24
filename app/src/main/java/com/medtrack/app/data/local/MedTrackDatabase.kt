package com.medtrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.medtrack.app.data.local.dao.LabReportDao
import com.medtrack.app.data.local.dao.MedicineDao
import com.medtrack.app.data.local.dao.ProfileDao
import com.medtrack.app.data.local.dao.TestRecordDao
import com.medtrack.app.data.local.entity.LabReportEntity
import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.data.local.entity.TestRecordEntity

@Database(
    entities = [
        ProfileEntity::class,
        LabReportEntity::class,
        TestRecordEntity::class,
        MedicineEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class MedTrackDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun labReportDao(): LabReportDao
    abstract fun testRecordDao(): TestRecordDao
    abstract fun medicineDao(): MedicineDao

    companion object {
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lab_reports ADD COLUMN recognizedText TEXT")
            }
        }
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE test_records ADD COLUMN referenceSource TEXT")
            }
        }
        @Volatile
        private var INSTANCE: MedTrackDatabase? = null

        // No seed data: the app opens on the profile launcher, and a fresh
        // install starts with an empty vault until the user creates a profile.
        fun getDatabase(context: Context): MedTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedTrackDatabase::class.java,
                    "medtrack_database.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
