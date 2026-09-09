package com.medtrack.app.data.local.dao

import androidx.room.*
import com.medtrack.app.data.local.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE profileId = :profileId ORDER BY name ASC")
    fun getMedicinesForProfile(profileId: Long): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE profileId = :profileId AND isActive = 1 ORDER BY name ASC")
    fun getActiveMedicinesForProfile(profileId: Long): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE profileId = :profileId AND isActive = 1 ORDER BY name ASC")
    suspend fun getActiveMedicinesForProfileSync(profileId: Long): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    fun getMedicineById(id: Long): Flow<MedicineEntity?>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineByIdSync(id: Long): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE isActive = 1")
    suspend fun getAllActiveMedicinesSync(): List<MedicineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)
}
