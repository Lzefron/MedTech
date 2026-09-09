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

    val currentProfile: StateFlow<ProfileEntity?> = combine(
        allProfiles,
        _selectedProfileId
    ) { profiles, selectedId ->
        if (profiles.isEmpty()) return@combine null
        if (selectedId != null) {
            profiles.find { it.id == selectedId } ?: profiles.firstOrNull { it.isDefault } ?: profiles.first()
        } else {
            profiles.firstOrNull { it.isDefault } ?: profiles.first()
        }
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

    fun selectProfile(profileId: Long) {
        _selectedProfileId.value = profileId
    }

    fun deleteReport(report: LabReportEntity) {
        viewModelScope.launch {
            labReportRepository.deleteReport(report)
        }
    }
}
