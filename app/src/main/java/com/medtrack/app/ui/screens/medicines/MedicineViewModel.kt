package com.medtrack.app.ui.screens.medicines

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.repository.MedicineRepository
import com.medtrack.app.domain.alarm.MedicineAlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MedicineViewModel(
    private val medicineRepository: MedicineRepository
) : ViewModel() {

    private val _currentProfileId = MutableStateFlow<Long?>(null)

    val medicines: StateFlow<List<MedicineEntity>> = _currentProfileId.flatMapLatest { profileId ->
        if (profileId != null) {
            medicineRepository.getMedicinesForProfile(profileId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setProfileId(profileId: Long) {
        _currentProfileId.value = profileId
    }

    fun addMedicine(context: Context, medicine: MedicineEntity) {
        viewModelScope.launch {
            val id = medicineRepository.insertMedicine(medicine)
            val scheduler = MedicineAlarmScheduler(context)
            scheduler.scheduleReminders(medicine.copy(id = id))
        }
    }

    fun toggleMedicine(context: Context, medicine: MedicineEntity) {
        viewModelScope.launch {
            val updated = medicine.copy(isActive = !medicine.isActive)
            medicineRepository.updateMedicine(updated)
            val scheduler = MedicineAlarmScheduler(context)
            if (updated.isActive) {
                scheduler.scheduleReminders(updated)
            } else {
                scheduler.cancelReminders(updated)
            }
        }
    }

    fun deleteMedicine(context: Context, medicine: MedicineEntity) {
        viewModelScope.launch {
            val scheduler = MedicineAlarmScheduler(context)
            scheduler.cancelReminders(medicine)
            medicineRepository.deleteMedicine(medicine)
        }
    }
}
