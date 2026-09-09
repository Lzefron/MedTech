package com.medtrack.app.data.repository

import com.medtrack.app.data.local.dao.ProfileDao
import com.medtrack.app.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()
    val defaultProfile: Flow<ProfileEntity?> = profileDao.getDefaultProfile()

    fun getProfileById(id: Long): Flow<ProfileEntity?> = profileDao.getProfileById(id)

    suspend fun getProfileByIdSync(id: Long): ProfileEntity? = profileDao.getProfileByIdSync(id)

    suspend fun getDefaultProfileSync(): ProfileEntity? = profileDao.getDefaultProfileSync()

    suspend fun insertProfile(profile: ProfileEntity): Long {
        if (profile.isDefault) {
            profileDao.clearDefaultProfiles()
        }
        return profileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: ProfileEntity) {
        if (profile.isDefault) {
            profileDao.clearDefaultProfiles()
        }
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: ProfileEntity) {
        profileDao.deleteProfile(profile)
    }

    suspend fun setDefaultProfile(profileId: Long) {
        profileDao.clearDefaultProfiles()
        val profile = profileDao.getProfileByIdSync(profileId)
        if (profile != null) {
            profileDao.updateProfile(profile.copy(isDefault = true))
        }
    }
}
