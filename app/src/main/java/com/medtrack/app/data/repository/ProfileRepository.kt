package com.medtrack.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.medtrack.app.data.local.MedTrackDatabase
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.domain.alarm.MedicineAlarmScheduler
import com.medtrack.app.domain.ocr.DocumentImages
import com.medtrack.app.domain.validation.HealthInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ProfileRepository(private val database: MedTrackDatabase, private val context: Context) {
    private val profileDao = database.profileDao()
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()
    val defaultProfile: Flow<ProfileEntity?> = profileDao.getDefaultProfile()
    fun getProfileById(id: Long) = profileDao.getProfileById(id)
    suspend fun getProfileByIdSync(id: Long) = profileDao.getProfileByIdSync(id)
    suspend fun getDefaultProfileSync() = profileDao.getDefaultProfileSync()
    private fun validate(profile: ProfileEntity) {
        require(profile.name.isNotBlank() && profile.name.length <= 80)
        require(HealthInput.dateError(profile.dob, optional = true) == null)
        require(HealthInput.phoneError(profile.emergencyPhone) == null)
    }
    suspend fun insertProfile(profile: ProfileEntity): Long = database.withTransaction {
        validate(profile)
        if (profile.isDefault) profileDao.clearDefaultProfiles()
        profileDao.insertProfile(profile)
    }
    suspend fun updateProfile(profile: ProfileEntity) = database.withTransaction {
        validate(profile)
        if (profile.isDefault) profileDao.clearDefaultProfiles()
        profileDao.updateProfile(profile)
    }
    suspend fun deleteProfile(profile: ProfileEntity) {
        val medicines = database.medicineDao().getActiveMedicinesForProfileSync(profile.id)
        val reports = database.labReportDao().getReportsForProfile(profile.id).first()
        // Commit the database deletion before removing photos. Failed transactions keep originals.
        database.withTransaction { profileDao.deleteProfile(profile) }
        medicines.forEach { MedicineAlarmScheduler(context).cancelReminders(it) }
        withContext(Dispatchers.IO) { reports.forEach { DocumentImages.deletePrivate(context, it.imagePath) } }
    }
    suspend fun setDefaultProfile(profileId: Long) = database.withTransaction {
        val profile = profileDao.getProfileByIdSync(profileId)
        if (profile != null) {
            profileDao.clearDefaultProfiles()
            profileDao.updateProfile(profile.copy(isDefault = true))
        }
    }
}
