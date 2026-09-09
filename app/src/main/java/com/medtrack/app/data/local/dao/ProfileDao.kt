package com.medtrack.app.data.local.dao

import androidx.room.*
import com.medtrack.app.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY isDefault DESC, name ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfileById(id: Long): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileByIdSync(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    fun getDefaultProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfileSync(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isDefault = 0")
    suspend fun clearDefaultProfiles(): Int
}
