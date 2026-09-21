package com.medtrack.app.ui.screens.medicines

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.repository.MedicineRepository
import com.medtrack.app.domain.alarm.MedicineAlarmScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MedicineViewModel(private val medicineRepository: MedicineRepository) : ViewModel() {
    private val currentProfileId = MutableStateFlow<Long?>(null)
    val error = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    val medicines: StateFlow<List<MedicineEntity>> = currentProfileId.flatMapLatest { id ->
        if (id != null) medicineRepository.getMedicinesForProfile(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun setProfileId(profileId: Long) { currentProfileId.value = profileId }

    fun saveMedicine(context: Context, medicine: MedicineEntity, onSaved: () -> Unit) {
        if (saving.value) return
        if (medicine.name.isBlank() || medicine.dosage.isBlank()) { error.value = "Medicine name and prescribed dose are required."; return }
        val times = listOfNotNull(
            medicine.morningTime.takeIf { medicine.takeMorning }, medicine.afternoonTime.takeIf { medicine.takeAfternoon },
            medicine.eveningTime.takeIf { medicine.takeEvening }, medicine.nightTime.takeIf { medicine.takeNight })
        if (times.isEmpty() || times.any { !Regex("""(?:[01]\d|2[0-3]):[0-5]\d""").matches(it) } || times.distinct().size != times.size) {
            error.value = "Choose at least one reminder time. Use distinct 24-hour times (HH:mm)."; return
        }
        saving.value = true
        error.value = null
        viewModelScope.launch {
            try {
                val scheduler = MedicineAlarmScheduler(context)
                val id = if (medicine.id == 0L) medicineRepository.insertMedicine(medicine) else {
                    medicineRepository.updateMedicine(medicine)
                    scheduler.cancelReminders(medicine)
                    medicine.id
                }
                try { scheduler.scheduleReminders(medicine.copy(id = id)) }
                catch (_: Exception) { error.value = "Medicine saved, but reminders could not be scheduled. Check Android alarm settings." }
                onSaved()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { error.value = "Could not save the medicine. Please retry."
            } finally { saving.value = false }
        }
    }
    fun toggleMedicine(context: Context, medicine: MedicineEntity) {
        viewModelScope.launch {
            try {
                val updated = medicine.copy(isActive = !medicine.isActive)
                medicineRepository.updateMedicine(updated)
                val scheduler = MedicineAlarmScheduler(context)
                if (updated.isActive) scheduler.scheduleReminders(updated) else scheduler.cancelReminders(updated)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { error.value = "Could not update reminders. Check Android settings and retry." }
        }
    }
    fun deleteMedicine(context: Context, medicine: MedicineEntity) {
        viewModelScope.launch {
            try {
                medicineRepository.deleteMedicine(medicine)
                MedicineAlarmScheduler(context).cancelReminders(medicine)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { error.value = "Could not remove the medicine. Please retry." }
        }
    }
}
