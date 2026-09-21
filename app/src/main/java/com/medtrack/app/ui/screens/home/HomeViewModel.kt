package com.medtrack.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.local.entity.LabReportEntity
import com.medtrack.app.data.local.entity.MedicineEntity
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.data.local.entity.TestRecordEntity
import com.medtrack.app.data.repository.LabReportRepository
import com.medtrack.app.data.repository.MedicineRepository
import com.medtrack.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val labReportRepository: LabReportRepository,
    private val medicineRepository: MedicineRepository
) : ViewModel() {

    val allProfiles: StateFlow<List<ProfileEntity>> = profileRepository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProfileId = MutableStateFlow<Long?>(null)

    /** Profile explicitly picked on the launcher this session; null until then. */
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    val currentProfile: StateFlow<ProfileEntity?> = combine(
        allProfiles,
        _selectedProfileId
    ) { profiles, selectedId ->
        profiles.find { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentReports: StateFlow<List<LabReportEntity>> = currentProfile.flatMapLatest { profile ->
        if (profile != null) {
            labReportRepository.getReportsForProfile(profile.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentAbnormalRecords: StateFlow<List<TestRecordEntity>> = currentProfile.flatMapLatest { profile ->
        if (profile != null) {
            labReportRepository.getRecentAbnormalRecords(profile.id, limit = 8)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMedicines: StateFlow<List<MedicineEntity>> = currentProfile.flatMapLatest { profile ->
        if (profile != null) {
            medicineRepository.getActiveMedicinesForProfile(profile.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearSelection() { _selectedProfileId.value = null }

    fun selectProfile(profileId: Long) {
        _selectedProfileId.value = profileId
    }

    val error = MutableStateFlow<String?>(null)
    fun deleteReport(report: LabReportEntity) {
        viewModelScope.launch {
            try { labReportRepository.deleteReport(report) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { error.value = "Could not delete the report. Please retry." }
        }
    }
}
