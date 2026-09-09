package com.medtrack.app.data.repository

import com.medtrack.app.data.local.dao.MedicineDao
import com.medtrack.app.data.local.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val medicineDao: MedicineDao) {
    fun getMedicinesForProfile(profileId: Long): Flow<List<MedicineEntity>> =
        medicineDao.getMedicinesForProfile(profileId)

    fun getActiveMedicinesForProfile(profileId: Long): Flow<List<MedicineEntity>> =
        medicineDao.getActiveMedicinesForProfile(profileId)

    suspend fun getActiveMedicinesForProfileSync(profileId: Long): List<MedicineEntity> =
        medicineDao.getActiveMedicinesForProfileSync(profileId)

    fun getMedicineById(id: Long): Flow<MedicineEntity?> =
        medicineDao.getMedicineById(id)

    suspend fun getMedicineByIdSync(id: Long): MedicineEntity? =
        medicineDao.getMedicineByIdSync(id)

    suspend fun getAllActiveMedicinesSync(): List<MedicineEntity> =
        medicineDao.getAllActiveMedicinesSync()

    suspend fun insertMedicine(medicine: MedicineEntity): Long =
        medicineDao.insertMedicine(medicine)

    suspend fun updateMedicine(medicine: MedicineEntity) {
        medicineDao.updateMedicine(medicine)
    }

    suspend fun deleteMedicine(medicine: MedicineEntity) {
        medicineDao.deleteMedicine(medicine)
    }
}
