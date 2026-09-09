package com.medtrack.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.app.data.local.entity.ProfileEntity
import com.medtrack.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val allProfiles: StateFlow<List<ProfileEntity>> = profileRepository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            profileRepository.insertProfile(profile)
        }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
        }
    }

    fun setDefaultProfile(profileId: Long) {
        viewModelScope.launch {
            profileRepository.setDefaultProfile(profileId)
        }
    }
}
