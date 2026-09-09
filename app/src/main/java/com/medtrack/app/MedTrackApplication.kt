package com.medtrack.app

import android.app.Application
import com.medtrack.app.data.local.MedTrackDatabase
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.data.repository.MedicineRepository
import com.medtrack.app.data.repository.ProfileRepository
import com.medtrack.app.domain.alarm.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedTrackApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { MedTrackDatabase.getDatabase(this, applicationScope) }
    val profileRepository by lazy { ProfileRepository(database.profileDao()) }
    val labReportRepository by lazy { LabReportRepository(database.labReportDao(), database.testRecordDao()) }
    val medicineRepository by lazy { MedicineRepository(database.medicineDao()) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        applicationScope.launch {
            MedTrackDatabase.populateInitialData(database)
        }
    }
}
