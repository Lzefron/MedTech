package com.medtrack.app

import android.app.Application
import com.medtrack.app.data.local.MedTrackDatabase
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.data.repository.MedicineRepository
import com.medtrack.app.data.repository.ProfileRepository
import com.medtrack.app.domain.alarm.NotificationHelper

class MedTrackApplication : Application() {
    val database by lazy { MedTrackDatabase.getDatabase(this) }
    val profileRepository by lazy { ProfileRepository(database, this) }
    val labReportRepository by lazy { LabReportRepository(database.labReportDao(), database.testRecordDao(), database, this) }
    val medicineRepository by lazy { MedicineRepository(database.medicineDao()) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

    }
}
